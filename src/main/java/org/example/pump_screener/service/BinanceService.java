package org.example.pump_screener.service;

import com.binance.connector.client.SpotClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.example.pump_screener.socket.BollingerBands;
import org.example.pump_screener.socket.Candlestick;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
@Data
public class BinanceService {
    private final SpotClient spotClient;
    private final List<BigDecimal> kValues = new ArrayList<>(); // Хранение значений %K
    // Константы для периодов
    private static final int DEFAULT_STOCHASTIC_PERIOD = 14;
    private static final int DEFAULT_D_PERIOD = 3;

    public BinanceService(SpotClient spotClient) {
        this.spotClient = spotClient;
    }

        /*
        Все методы которые есть
        spotClient.createBlvt();
        spotClient.createTrade();
        spotClient.createBswap();
        spotClient.createC2C();
        spotClient.createConvert();
        spotClient.createCryptoLoans();
        spotClient.createFiat();
        spotClient.createFutures();
        spotClient.createGiftCard();
        spotClient.createMargin();
        spotClient.createMarket();
        spotClient.createMining();
        spotClient.createNFT();
        spotClient.createPay();
        spotClient.createPortfolioMargin();
        spotClient.createRebate();
        spotClient.createSavings();
        spotClient.createStaking();
        spotClient.createSubAccount();
        spotClient.createUserData();
        spotClient.createWallet();
        spotClient.setProxy();
        spotClient.setShowLimitUsage();
        spotClient.unsetProxy();
         */

    // Метод для получения последних свечей
    public List<Candlestick> getLatestCandlesticks(String symbol) {
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("symbol", symbol);
        parameters.put("interval", "1h"); // Используйте 1 минута
        parameters.put("limit", 50); // Получить последние 50 свечей

        String response = spotClient.createMarket().klines(parameters);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode;
        List<Candlestick> candlesticks = new ArrayList<>();
        try {
            rootNode = objectMapper.readTree(response);
            for (JsonNode candlestickData : rootNode) {
                long openTime = candlestickData.get(0).asLong();
                String open = candlestickData.get(1).asText();
                String high = candlestickData.get(2).asText();
                String low = candlestickData.get(3).asText();
                String close = candlestickData.get(4).asText();
                String volume = candlestickData.get(5).asText();
                long closeTime = candlestickData.get(6).asLong();

                Candlestick candlestick = new Candlestick(openTime, open, high, low, close, volume, closeTime);
                candlesticks.add(candlestick);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return candlesticks;
    }

    public BigDecimal calculateRSI(String symbol, int periods) {
        List<Candlestick> candlesticks = getLatestCandlesticks(symbol);

        if (candlesticks.size() < periods) {
            throw new RsiCalculationException("Недостаточно данных для расчета RSI. Требуется " + periods + " свечей, но получено " + candlesticks.size() + ".");
        }

        BigDecimal totalGain = BigDecimal.ZERO;
        BigDecimal totalLoss = BigDecimal.ZERO;

        for (int i = 1; i <= periods; i++) {
            BigDecimal change = new BigDecimal(candlesticks.get(i).getClose())
                    .subtract(new BigDecimal(candlesticks.get(i - 1).getClose()));
            if (change.compareTo(BigDecimal.ZERO) > 0) {
                totalGain = totalGain.add(change);
            } else {
                totalLoss = totalLoss.add(change.abs());
            }
        }

        // Использование метода Уайлдера для средней прибыли и убытка
        BigDecimal averageGain = totalGain.divide(BigDecimal.valueOf(periods), 4, RoundingMode.HALF_UP);
        BigDecimal averageLoss = totalLoss.divide(BigDecimal.valueOf(periods), 4, RoundingMode.HALF_UP);

        for (int i = periods; i < candlesticks.size(); i++) {
            BigDecimal change = new BigDecimal(candlesticks.get(i).getClose())
                    .subtract(new BigDecimal(candlesticks.get(i - 1).getClose()));

            if (change.compareTo(BigDecimal.ZERO) > 0) {
                averageGain = (averageGain.multiply(BigDecimal.valueOf(periods - 1)).add(change)).divide(BigDecimal.valueOf(periods), 4, RoundingMode.HALF_UP);
                averageLoss = averageLoss.multiply(BigDecimal.valueOf(periods - 1)).divide(BigDecimal.valueOf(periods), 4, RoundingMode.HALF_UP); // Убыток остается неизменным
            } else {
                averageLoss = (averageLoss.multiply(BigDecimal.valueOf(periods - 1)).add(change.abs())).divide(BigDecimal.valueOf(periods), 4, RoundingMode.HALF_UP);
                averageGain = averageGain.multiply(BigDecimal.valueOf(periods - 1)).divide(BigDecimal.valueOf(periods), 4, RoundingMode.HALF_UP); // Прибыль остается неизменной
            }
        }

        if (averageLoss.compareTo(BigDecimal.ZERO) == 0 && averageGain.compareTo(BigDecimal.ZERO) == 0) {
            // Это исключительный случай, когда и прибыль, и убыток равны нулю
            throw new RsiCalculationException("И средняя прибыль, и средняя убыток равны нулю, не возможно рассчитать RSI.");
        }

        if (averageLoss.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(100); // Если потерь нет, RSI = 100
        }

        BigDecimal rs = averageGain.divide(averageLoss, 4, RoundingMode.HALF_UP);
        BigDecimal rsi = BigDecimal.valueOf(100).subtract(BigDecimal.valueOf(100).divide(BigDecimal.ONE.add(rs), 4, RoundingMode.HALF_UP));

        return rsi;
    }

    public BigDecimal calculateWilliamsR(List<BigDecimal> closingPrices, int n) {
        // Проверка на наличие достаточных данных
        if (closingPrices == null || closingPrices.isEmpty()) {
            throw new IllegalArgumentException("Список цен закрытия не должен быть пустым.");
        }
        if (closingPrices.size() < n) {
            throw new IllegalArgumentException("Недостаточно данных для расчета Williams %R. Требуется " + n + " значений, но получено " + closingPrices.size() + ".");
        }

        BigDecimal highestHigh = closingPrices.get(0);
        BigDecimal lowestLow = closingPrices.get(0);
        BigDecimal currentClose = closingPrices.get(closingPrices.size() - 1); // Последняя цена закрытия

        // Проходим по последним n ценам, чтобы найти наивысшую и наименьшую цены
        for (int i = closingPrices.size() - n; i < closingPrices.size(); i++) {
            BigDecimal price = closingPrices.get(i);
            if (price.compareTo(highestHigh) > 0) {
                highestHigh = price;
            }
            if (price.compareTo(lowestLow) < 0) {
                lowestLow = price;
            }
        }

        if (highestHigh.compareTo(lowestLow) == 0) {
            return BigDecimal.ZERO; // Избегаем деления на ноль
        }

        BigDecimal williamsR = highestHigh.subtract(currentClose)
                .divide(highestHigh.subtract(lowestLow), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(-100));

        return williamsR;
    }

    public BigDecimal[] calculateStochastic(List<BigDecimal> closingPrices, List<BigDecimal> highPrices, List<BigDecimal> lowPrices, int n) {
        // Проверка на достаточное количество данных
        if (closingPrices.size() < n || highPrices.size() < n || lowPrices.size() < n) {
            return new BigDecimal[]{null, null}; // Недостаточно данных
        }

        BigDecimal latestClose = closingPrices.get(closingPrices.size() - 1);
        BigDecimal highestHigh = highPrices.subList(highPrices.size() - n, highPrices.size()).stream().max(BigDecimal::compareTo).orElse(null);
        BigDecimal lowestLow = lowPrices.subList(lowPrices.size() - n, lowPrices.size()).stream().min(BigDecimal::compareTo).orElse(null);

        // Избежание деления на ноль
        if (highestHigh == null || lowestLow == null || highestHigh.compareTo(lowestLow) == 0) {
            return new BigDecimal[]{null, null}; // Недостаточно данных для расчета
        }

        // Расчет %K
        BigDecimal stochasticK = (latestClose.subtract(lowestLow)).divide(highestHigh.subtract(lowestLow), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));

        // Добавляем новое значение %K в список
        kValues.add(stochasticK);

        // Ограничиваем размер списка до 14 для %K
        if (kValues.size() > DEFAULT_STOCHASTIC_PERIOD) {
            kValues.remove(0);
        }

        // Проверяем наличие достаточных значений для вычисления %D
        BigDecimal stochasticD = kValues.size() < DEFAULT_D_PERIOD ? null : calculateSMA(kValues, DEFAULT_D_PERIOD);

        return new BigDecimal[]{stochasticK, stochasticD}; // Возвращаем возможные n/a вместо 0 для D
    }

    public BollingerBands calculateBollingerBands(String symbol, int period, int k) {
        if (period <= 0 || k <= 0) {
            throw new IllegalArgumentException("Период и K должны быть положительными.");
        }

        List<Candlestick> candlesticks = getLatestCandlesticks(symbol);
        if (candlesticks.size() < period) {
            return null; // Недостаточно данных
        }

        List<BigDecimal> closingPrices = candlesticks.stream()
                .map(candlestick -> new BigDecimal(candlestick.getClose()))
                .toList();

        // Вычисление SMA и стандартного отклонения в одном цикле
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal varianceSum = BigDecimal.ZERO;

        for (int i = closingPrices.size() - period; i < closingPrices.size(); i++) {
            total = total.add(closingPrices.get(i));
        }

        BigDecimal sma = total.divide(BigDecimal.valueOf(period), 4, RoundingMode.HALF_UP);

        for (int i = closingPrices.size() - period; i < closingPrices.size(); i++) {
            BigDecimal deviation = closingPrices.get(i).subtract(sma);
            varianceSum = varianceSum.add(deviation.multiply(deviation));
        }

        BigDecimal stdDev = BigDecimal.valueOf(Math.sqrt(varianceSum.divide(BigDecimal.valueOf(period), 10, RoundingMode.HALF_UP).doubleValue()));

        // Вычисление верхней и нижней границ
        BigDecimal upperBand = sma.add(stdDev.multiply(BigDecimal.valueOf(k)));
        BigDecimal lowerBand = sma.subtract(stdDev.multiply(BigDecimal.valueOf(k)));

        return new BollingerBands(sma, upperBand, lowerBand); // возвращаем объект BollingerBands
    }

    // Метод для расчета SMA за n периодов на основе K
    private BigDecimal calculateSMA(List<BigDecimal> kValues, int n) {
        if (kValues.size() < n) {
            return null; // Недостаточно данных
        }

        BigDecimal total = BigDecimal.ZERO;
        for (int i = kValues.size() - n; i < kValues.size(); i++) {
            total = total.add(kValues.get(i));
        }

        return total.divide(BigDecimal.valueOf(n), 4, RoundingMode.HALF_UP);
    }

    public class RsiCalculationException extends RuntimeException {
        public RsiCalculationException(String message) {
            super(message);
        }
    }
}

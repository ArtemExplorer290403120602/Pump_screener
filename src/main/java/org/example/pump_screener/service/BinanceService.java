package org.example.pump_screener.service;

import com.binance.connector.client.SpotClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
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
            return null; // Недостаточно данных для расчета RSI
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

        BigDecimal averageGain = totalGain.divide(BigDecimal.valueOf(periods), 4, RoundingMode.HALF_UP);
        BigDecimal averageLoss = totalLoss.divide(BigDecimal.valueOf(periods), 4, RoundingMode.HALF_UP);

        if (averageLoss.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(100); // Если потерь нет, RSI = 100
        }

        BigDecimal rs = averageGain.divide(averageLoss, 4, RoundingMode.HALF_UP);
        BigDecimal rsi = BigDecimal.valueOf(100).subtract(BigDecimal.valueOf(100).divide(BigDecimal.ONE.add(rs), 4, RoundingMode.HALF_UP));

        return rsi;
    }

    public BigDecimal calculateWilliamsR(List<BigDecimal> closingPrices, int n) {
        if (closingPrices.size() < n) {
            return null; // Недостаточно данных
        }

        BigDecimal highestHigh = closingPrices.subList(closingPrices.size() - n, closingPrices.size())
                .stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal lowestLow = closingPrices.subList(closingPrices.size() - n, closingPrices.size())
                .stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal currentClose = closingPrices.get(closingPrices.size() - 1); // Последняя цена закрытия

        if (highestHigh.compareTo(lowestLow) == 0) {
            return BigDecimal.ZERO; // Избегаем деления на ноль
        }

        BigDecimal williamsR = highestHigh.subtract(currentClose)
                .divide(highestHigh.subtract(lowestLow), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(-100));

        return williamsR;
    }

    public BigDecimal[] calculateStochastic(List<BigDecimal> closingPrices, List<BigDecimal> highPrices, List<BigDecimal> lowPrices, int n) {
        if (closingPrices.size() < n || highPrices.size() < n || lowPrices.size() < n) {
            return new BigDecimal[]{BigDecimal.ZERO, null}; // Недостаточно данных
        }

        BigDecimal latestClose = closingPrices.get(closingPrices.size() - 1);
        BigDecimal highestHigh = highPrices.subList(highPrices.size() - n, highPrices.size()).stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal lowestLow = lowPrices.subList(lowPrices.size() - n, lowPrices.size()).stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

        if (highestHigh.compareTo(lowestLow) == 0) {
            return new BigDecimal[]{BigDecimal.ZERO, null}; // Избегаем деления на ноль
        }

        BigDecimal stochasticK = (latestClose.subtract(lowestLow)).divide(highestHigh.subtract(lowestLow), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));

        // Добавляем новое значение %K в список
        kValues.add(stochasticK);

        // Ограничиваем размер списка до 14 для %K
        if (kValues.size() > 14) {
            kValues.remove(0);
        }

        // Проверяем наличие достаточных значений для вычисления %D
        BigDecimal stochasticD = kValues.size() < 3 ? null : calculateSMA(kValues, 3);

        return new BigDecimal[]{stochasticK, stochasticD}; // Возвращаем возможные n/a вместо 0 для D
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
}

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

    public BigDecimal[] calculateMACD(String symbol) {
        List<Candlestick> candlesticks = getLatestCandlesticks(symbol);
        if (candlesticks.size() < 26) {
            return new BigDecimal[]{null, null, null}; // Недостаточно данных для расчета MACD
        }

        // Получаем цены закрытия
        List<BigDecimal> closingPrices = new ArrayList<>();
        for (Candlestick candlestick : candlesticks) {
            closingPrices.add(new BigDecimal(candlestick.getClose()));
        }

        BigDecimal ema12 = calculateEMA(closingPrices, 12);
        BigDecimal ema26 = calculateEMA(closingPrices, 26);
        BigDecimal macdLine = ema12.subtract(ema26);
        BigDecimal signalLine = calculateEMA(List.of(macdLine), 9); // Здесь нужно будет хранить историю MACD для расчета сигнальной линии

        return new BigDecimal[]{macdLine, signalLine, macdLine.subtract(signalLine)}; // Возвращаем MACD, сигнальную линию и гистограмму
    }

    private BigDecimal calculateEMA(List<BigDecimal> prices, int period) {
        BigDecimal multiplier = BigDecimal.valueOf(2).divide(BigDecimal.valueOf(period + 1), RoundingMode.HALF_UP);
        BigDecimal ema = prices.get(0); // Начальное значение EMA можно взять как первую цену

        for (int i = 1; i < prices.size(); i++) {
            ema = (prices.get(i).subtract(ema)).multiply(multiplier).add(ema);
        }

        return ema;
    }
}

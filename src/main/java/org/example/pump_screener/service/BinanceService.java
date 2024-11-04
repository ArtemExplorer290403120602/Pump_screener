package org.example.pump_screener.service;

import com.binance.connector.client.SpotClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.example.pump_screener.adapters.binance.BinanceAPI;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
@Data
public class BinanceService implements BinanceAPI {
    private final SpotClient spotClient;

    public BinanceService(SpotClient spotClient) {
        this.spotClient = spotClient;
    }

    @Override
    public String getAccountStatus() {
        try {
            // Задаем торговую пару
            String symbol = "BTCUSDT";  // Замените на нужную вам пару

            HashMap<String, Object> parameters = new HashMap<>();
            parameters.put("symbol", symbol);
            parameters.put("recvWindow", "10000"); // Устанавливаем recvWindow в 10 секунд


            // Получаем список ордеров для указанной торговой пары
            String response = spotClient.createTrade().getOrders(parameters);
            return "Список ордеров по символу " + symbol + ": " + response; // Отправляем ответ пользователю
        } catch (IllegalArgumentException e) {
            return "Ошибка подключения к Binance: " + e.getMessage();
        } catch (Exception e) {
            return "Ошибка: " + e.getMessage();
        }
    }

    @Override
    public List<String> getAllBitcoinPairs() {
        List<String> bitcoinPairs = new ArrayList<>();
        try {
            // Получаем все символы
            HashMap<String, Object> parameters = new HashMap<>();
            String response = spotClient.createMarket().exchangeInfo(parameters);

            // Предположим, что вы используете библиотеку для работы с JSON
            // Пример: Jackson
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode symbolsNode = rootNode.path("symbols");

            // Фильтруем символы, чтобы оставить только биткойн-пары
            for (JsonNode symbolInfo : symbolsNode) {
                String symbol = symbolInfo.path("symbol").asText();
                if (symbol.endsWith("BTC")) {
                    bitcoinPairs.add(symbol);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitcoinPairs;
    }

    @Override
    public List<String> getAllUsdtPairs() {
        List<String> usdtPairs = new ArrayList<>();
        try {
            HashMap<String, Object> parameters = new HashMap<>();
            String response = spotClient.createMarket().exchangeInfo(parameters);
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode symbolsNode = rootNode.path("symbols");

            for (JsonNode symbolInfo : symbolsNode) {
                String symbol = symbolInfo.path("symbol").asText();
                if (symbol.endsWith("USDT")) {
                    usdtPairs.add(symbol);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return usdtPairs;
    }

    public BigDecimal get1MinutePriceChange(String symbol) {
        try {
            HashMap<String, Object> parameters = new HashMap<>();
            parameters.put("symbol", symbol);

            String response = spotClient.createMarket().ticker24H(parameters);
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(response);

            return new BigDecimal(rootNode.path("priceChangePercent").asText()); // % изменение за 1 минуту
        } catch (Exception e) {
            e.printStackTrace();
            return BigDecimal.ZERO;
        }
    }

    public BigDecimal get1MinuteVolume(String symbol) {
        try {
            HashMap<String, Object> parameters = new HashMap<>();
            parameters.put("symbol", symbol);

            String response = spotClient.createMarket().ticker24H(parameters);
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(response);

            return new BigDecimal(rootNode.path("volume").asText()); // Объем за 1 минуту
        } catch (Exception e) {
            e.printStackTrace();
            return BigDecimal.ZERO;
        }
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

    // Измененный метод для получения последних свечей
    public List<Candlestick> getLatestCandlesticks(String symbol) {
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("symbol", symbol);
        parameters.put("interval", "1m"); // Интервал 1 минута
        parameters.put("limit", 1); // Получаем только последнюю свечу

        String response = spotClient.createMarket().klines(parameters); // Используем klines вместо candlestick

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

    @Data
    // Добавляем класс Candlestick
    public class Candlestick {
        private long openTime;
        private String open;
        private String high;
        private String low;
        private String close;
        private String volume;
        private long closeTime;

        public Candlestick(long openTime, String open, String high, String low, String close, String volume, long closeTime) {
            this.openTime = openTime;
            this.open = open;
            this.high = high;
            this.low = low;
            this.close = close;
            this.volume = volume;
            this.closeTime = closeTime;
        }

        // Getters и toString() для удобного вывода информации
    }
}

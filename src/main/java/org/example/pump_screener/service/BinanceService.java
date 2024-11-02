package org.example.pump_screener.service;

import com.binance.connector.client.SpotClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.pump_screener.adapters.binance.BinanceAPI;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
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
}

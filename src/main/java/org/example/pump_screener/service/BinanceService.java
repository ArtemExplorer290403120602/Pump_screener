package org.example.pump_screener.service;

import com.binance.connector.client.SpotClient;
import org.example.pump_screener.adapters.binance.BinanceAPI;
import org.springframework.stereotype.Service;

import java.util.HashMap;
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
}

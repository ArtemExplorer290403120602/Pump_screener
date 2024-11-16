package org.example.pump_screener.socket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.websocket.*;
import org.example.pump_screener.adapters.CandlestickEventImpl;
import org.example.pump_screener.adapters.binance.CandlestickEvent;
import org.example.pump_screener.service.BinanceService;
import org.example.pump_screener.service.BotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.util.HashMap;
import java.util.List;

import static javax.management.remote.JMXConnectorFactory.connect;

@Component
@ClientEndpoint
public class WebSocketClient {
    private final BinanceService binanceService;
    private final BotService botService;
    private Session userSession = null;

    private static final String BINANCE_CANDLESTICK_URL = "wss://stream.binance.com:9443/ws/";
    private final HashMap<String, BigDecimal> lastPriceChanges = new HashMap<>();

    @Autowired
    public WebSocketClient(BinanceService binanceService, BotService botService) {
        this.binanceService = binanceService;
        this.botService = botService;
    }

    @PostConstruct
    public void start() {
        connect();
    }

    private void connect() {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        try {
            List<String> symbolsToTrack = List.of("BTCUSDT", "DOGEUSDT", "TROYUSDT", "WLDUSDT", "SUIUSDT");
            for (String symbol : symbolsToTrack) {
                String endpoint = BINANCE_CANDLESTICK_URL + symbol.toLowerCase() + "@kline_1m";
                container.connectToServer(this, URI.create(endpoint));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @OnOpen
    public void onOpen(Session userSession) {
        this.userSession = userSession;
        System.out.println("WebSocket Opened");
    }

    @OnMessage
    public void onMessage(String message) {
        CandlestickEvent event = parseMessage(message);
        if (event != null) {
            // Передаем событие в метод для обработки изменения цены
            processPriceChange(event);
        }
    }

    @OnClose
    public void onClose(Session userSession, CloseReason reason) {
        this.userSession = null;
        System.out.println("WebSocket Closed: " + reason);
    }

    @OnError
    public void onError(Session userSession, Throwable throwable) {
        System.out.println("WebSocket Error: " + throwable.getMessage());
    }

    private CandlestickEvent parseMessage(String message) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode jsonNode = objectMapper.readTree(message);
            String symbol = jsonNode.get("s").asText();
            JsonNode kline = jsonNode.get("k");

            // Получаем данные из свечи
            long openTime = kline.get("t").asLong();
            long closeTime = kline.get("T").asLong();
            String open = kline.get("o").asText();
            String close = kline.get("c").asText();

            Candlestick candlestick = new Candlestick(
                    openTime, open, kline.get("h").asText(), kline.get("l").asText(), close, kline.get("v").asText(), closeTime);

            return new CandlestickEventImpl(symbol, candlestick);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void processPriceChange(CandlestickEvent event) {
        String symbol = event.getSymbol();
        BigDecimal openPrice = new BigDecimal(event.getCandlestick().getOpen());
        BigDecimal closePrice = new BigDecimal(event.getCandlestick().getClose());
        BigDecimal priceChangePercent = closePrice.subtract(openPrice)
                .divide(openPrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        //Получение объема
        BigDecimal volume = new BigDecimal(event.getCandlestick().getVolume());

        // Форматирование объема до 2 знаков после запятой
        BigDecimal formattedVolume = volume.setScale(2, RoundingMode.HALF_UP);

        // Обчисление суммы в долларах (при этом closePrice - это цена закрытия)
        BigDecimal totalValueInUSD = closePrice.multiply(formattedVolume).setScale(2, RoundingMode.HALF_UP);

        // Проверяем, изменилось ли значение
        if (priceChangePercent.abs().compareTo(BigDecimal.valueOf(0.05)) >= 0) {
            BigDecimal lastChange = lastPriceChanges.getOrDefault(symbol, BigDecimal.ZERO);

            if (lastChange.compareTo(priceChangePercent) != 0) {
                lastPriceChanges.put(symbol, priceChangePercent);
                String direction;
                String emoji;

                if (priceChangePercent.compareTo(BigDecimal.ZERO) > 0) {
                    direction = "Pump";
                    emoji = "\uD83D\uDCC8"; // Зеленая стрелка вверх
                } else {
                    direction = "Dump";
                    emoji = "\uD83D\uDCC9"; // Красная стрелка вниз
                }
                // Формируем ссылку на график Binance
                String tradingUrl = String.format("https://www.binance.com/en/trade/%s?ref=396823681", symbol); // Изменение URL
                String message = String.format("❗️❗️❗️❗️❗️\n\n`%s` %s\n\n%s изменение цены: %.2f%% 🔥\n\n\uD83E\uDD11Объем: %s\uD83E\uDD11 \n\n\uD83D\uDCB5Сумма в долларах: %s\uD83D\uDCB5\n\n \uD83D\uDC49\uD83C\uDFFD[Торгуй сейчас!](%s)✅", symbol, direction, emoji, priceChangePercent,formattedVolume, totalValueInUSD,tradingUrl);
                List<Candlestick> latestCandlesticks = binanceService.getLatestCandlesticks(symbol);
                botService.sendMessageToAllUsers(message,symbol,latestCandlesticks);  // Отправляем сообщение в бот
            }
        }
    }
}

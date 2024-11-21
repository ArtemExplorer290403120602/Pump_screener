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
            List<String> symbolsToTrack = List.of("BTCUSDT", "DOGEUSDT", "TROYUSDT", "WLDUSDT", "SUIUSDT", "TIAUSDT", "ADAUSDT",
                    "ETHUSDT", "BNBUSDT", "PENDLEUSDT", "AVAXUSDT", "AAVEUSDT", "WIFUSDT", "XRPUSDT", "TURBOUSDT",
                    "LINKUSDT", "SAGAUSDT", "DOGSUSDT", "OPUSDT", "PIXELUSDT", "JASMYUSDT", "ZKUSDT", "ARBUSDT",
                    "CATIUSDT", "FILUSDT", "DOTUSDT", "BCHUSDT", "EOSUSDT", "LTCUSDT", "TRXUSDT", "ETCUSDT", "XLMUSDT", "XMRUSDT",
                    "DASHUSDT", "ZECUSDT", "XTZUSDT", "ATOMUSDT", "ONTUSDT", "IOTAUSDT", "BATUSDT", "VETUSDT", "NEOUSDT", "QTUMUSDT",
                    "IOSTUSDT", "THETAUSDT", "ALGOUSDT", "ZILUSDT", "KNCUSDT", "ZRXUSDT", "COMPUSDT", "OMGUSDT", "SXPUSDT", "KAVAUSDT",
                    "BANDUSDT", "RLCUSDT", "MKRUSDT", "SNXUSDT", "CELRUSDT", "YFIUSDT", "BALUSDT", "CRVUSDT", "TRBUSDT", "RUNEUSDT",
                    "SUSHIUSDT", "EGLDUSDT", "SOLUSDT", "ICXUSDT", "STORJUSDT", "BLZUSDT", "UNIUSDT", "FTMUSDT", "ENJUSDT", "FLMUSDT",
                    "RENUSDT", "KSMUSDT", "NEARUSDT", "RSRUSDT", "LRCUSDT", "BELUSDT", "AXSUSDT", "ALPHAUSDT", "ZENUSDT", "SKLUSDT",
                    "1INCHUSDT", "CHZUSDT", "SANDUSDT", "ANKRUSDT", "LITUSDT", "REEFUSDT", "RVNUSDT", "SFPUSDT", "XEMUSDT", "COTIUSDT",
                    "CHRUSDT", "MANAUSDT", "ALICEUSDT", "HBARUSDT", "ONEUSDT", "LINAUSDT", "STMXUSDT", "DENTUSDT", "HOTUSDT", "MTLUSDT",
                    "OGNUSDT", "NKNUSDT", "GMXUSDT", "CFXUSDT", "STXUSDT", "BAKEUSDT", "GTCUSDT", "BNXUSDT", "IOTXUSDT", "C98USDT",
                    "MASKUSDT", "ATAUSDT", "DYDXUSDT", "GALAUSDT", "CELOUSDT", "ARUSDT", "ARPAUSDT", "CTSIUSDT", "LPTUSDT", "ENSUSDT", "PEOPLEUSDT",
                    "ROSEUSDT", "DUSKUSDT", "FLOWUSDT", "IMXUSDT", "API3USDT", "GMTUSDT", "APEUSDT", "WOOUSDT", "ASTRUSDT", "DARUSDT", "PHBUSDT", "INJUSDT",
                    "STGUSDT", "SPELLUSDT", "ACHUSDT", "LDOUSDT", "ICPUSDT", "APTUSDT", "QNTUSDT", "FETUSDT", "FXSUSDT", "HOOKUSDT", "MAGICUSDT", "TUSDT",
                    "HIGHUSDT", "MINAUSDT", "SSVUSDT", "CKBUSDT", "PERPUSDT", "TRUUSDT", "LQTYUSDT", "USDCUSDT", "IDUSDT", "RDNTUSDT", "JOEUSDT",
                    "TLMUSDT", "AMBUSDT", "LEVERUSDT", "BICOUSDT", "HFTUSDT", "XVSUSDT", "ARKMUSDT", "BLURUSDT", "EDUUSDT", "LOOMUSDT", "AGLDUSDT",
                    "UMAUSDT", "KEYUSDT", "COMBOUSDT", "NMRUSDT", "MAVUSDT", "XVGUSDT", "AIUSDT", "BONDUSDT", "YGGUSDT", "XAIUSDT", "BNTUSDT",
                    "OXTUSDT", "SEIUSDT", "CYBERUSDT", "HIFIUSDT", "ARKUSDT", "MANTAUSDT", "WAXPUSDT", "OMUSDT", "RIFUSDT", "POLYXUSDT", "GASUSDT", "POWRUSDT",
                    "MOVRUSDT", "CAKEUSDT", "MEMEUSDT", "TWTUSDT", "LSKUSDT", "ORDIUSDT", "STEEMUSDT", "BADGERUSDT", "ILVUSDT", "NTRNUSDT", "ALTUSDT", "BEAMXUSDT",
                    "JUPUSDT", "PYTHUSDT", "SUPERUSDT", "USTCUSDT", "ONGUSDT", "STRKUSDT", "JTOUSDT", "1000SATSUSDT", "AUCTIONUSDT", "RONINUSDT",
                    "ACEUSDT", "DYMUSDT", "AXLUSDT", "GLMUSDT", "PORTALUSDT", "TONUSDT", "METISUSDT", "AEVOUSDT", "VANRYUSDT", "BOMEUSDT", "ETHFIUSDT", "ENAUSDT",
                    "WUSDT", "TNSRUSDT", "TAOUSDT", "OMNIUSDT", "REZUSDT", "BBUSDT", "NOTUSDT", "IOUSDT", "LISTAUSDT", "ZROUSDT", "RENDERUSDT", "BANANAUSDT",
                    "RAREUSDT", "GUSDT", "SYNUSDT", "VOXELUSDT", "ALPACAUSDT", "SUNUSDT", "VIDTUSDT", "NULSUSDT", "MBOXUSDT", "CHESSUSDT", "FLUXUSDT", "BSWUSDT",
                    "QUICKUSDT", "RPLUSDT", "AERGOUSDT", "POLUSDT", "1MBABYDOGEUSDT", "NEIROUSDT", "KDAUSDT", "FIDAUSDT", "FIOUSDT", "GHSTUSDT",
                    "LOKAUSDT", "HMSTRUSDT", "REIUSDT", "COSUSDT", "EIGENUSDT", "DIAUSDT", "SCRUSDT","SANTOSUSDT");
            for (String symbol : symbolsToTrack) {
                String endpoint = BINANCE_CANDLESTICK_URL + symbol.toLowerCase() + "@kline_4h";
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

        // Получение объема
        BigDecimal volume = new BigDecimal(event.getCandlestick().getVolume());

        // Форматирование объема до 2 знаков после запятой
        BigDecimal formattedVolume = volume.setScale(2, RoundingMode.HALF_UP);

        // Обчисление суммы в долларах (при этом closePrice - это цена закрытия)
        BigDecimal totalValueInUSD = closePrice.multiply(formattedVolume).setScale(2, RoundingMode.HALF_UP);

        // Проверяем, изменилось ли значение
        if (priceChangePercent.abs().compareTo(BigDecimal.valueOf(4.00)) >= 0 && volume.compareTo(BigDecimal.valueOf(5_000_000)) > 0) {
            BigDecimal lastChange = lastPriceChanges.getOrDefault(symbol, BigDecimal.ZERO);

            if (lastChange.compareTo(priceChangePercent) != 0) {
                lastPriceChanges.put(symbol, priceChangePercent);
                String direction;
                String emoji;

                // Оставим обработку только для "pump"
                if (priceChangePercent.compareTo(BigDecimal.ZERO) > 0) {
                    direction = "Pump";
                    emoji = "\uD83D\uDCC8"; // Зеленая стрелка вверх

                    // Формируем ссылку на график Binance
                    String tradingUrl = String.format("https://www.binance.com/en/trade/%s?ref=396823681", symbol); // Изменение URL
                    String message = String.format("❗️❗️❗️❗️❗️\n\n`%s` %s\n\n%s изменение цены: %.2f%% 🔥\n\n\uD83E\uDD11Объем: %s\uD83E\uDD11 \n\n\uD83D\uDCB5Сумма в долларах: %s\uD83D\uDCB5\n\n \uD83D\uDC49\uD83C\uDFFD[Торгуй сейчас!](%s)✅", symbol, direction, emoji, priceChangePercent, formattedVolume, totalValueInUSD, tradingUrl);
                    List<Candlestick> latestCandlesticks = binanceService.getLatestCandlesticks(symbol);
                    botService.sendMessageToAllUsers(message, symbol, latestCandlesticks);  // Отправляем сообщение в бот
                }
                // Мы просто не делаем ничего для "dump"
            }
        }
    }
}
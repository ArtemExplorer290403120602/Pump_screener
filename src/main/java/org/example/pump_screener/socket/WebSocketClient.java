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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static javax.management.remote.JMXConnectorFactory.connect;

@Component
@ClientEndpoint
public class WebSocketClient {
    private final BinanceService binanceService;
    private final BotService botService;
    private Session userSession = null;

    private static final String BINANCE_CANDLESTICK_URL = "wss://stream.binance.com:9443/ws/";
    private final HashMap<String, BigDecimal> lastPriceChanges = new HashMap<>();
    // Добавьте это в начале вашего класса WebSocketClient
    private final ConcurrentHashMap<String, Long> lastNotificationTime = new ConcurrentHashMap<>();
    private static final long NOTIFICATION_DELAY = TimeUnit.MINUTES.toMillis(10); // 10 минут в

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
            List<String> symbolsToTrack = List.of("BTCUSDT", "THEUSDT", "SCRTUSDT", "DOGEUSDT", "TROYUSDT", "WLDUSDT", "SUIUSDT", "TIAUSDT", "ADAUSDT",
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
                    "LOKAUSDT", "HMSTRUSDT", "REIUSDT", "COSUSDT", "EIGENUSDT", "DIAUSDT", "SCRUSDT", "SANTOSUSDT");
            for (String symbol : symbolsToTrack) {
                String endpoint = BINANCE_CANDLESTICK_URL + symbol.toLowerCase() + "@kline_1h";
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
        System.err.println("WebSocket Error: " + throwable.getMessage());
        throwable.printStackTrace(); // Дополнительная информация об ошибке
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
        // Проверяем время последнего уведомления для этого символа
        long currentTime = System.currentTimeMillis();
        long lastTime = lastNotificationTime.getOrDefault(symbol, 0L);

        // Если задержка еще не прошла, выходим из метода
        if (currentTime - lastTime < NOTIFICATION_DELAY) {
            return; // Пропускаем отправку уведомления
        }
        BigDecimal openPrice = new BigDecimal(event.getCandlestick().getOpen());
        BigDecimal closePrice = new BigDecimal(event.getCandlestick().getClose());
        BigDecimal priceChangePercent = closePrice.subtract(openPrice)
                .divide(openPrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        // Получение объема
        BigDecimal volume = new BigDecimal(event.getCandlestick().getVolume());

        // Форматирование объема до 2 знаков после запятой
        BigDecimal formattedVolume = volume.setScale(2, RoundingMode.HALF_UP);

        BinanceService.MACD macd = binanceService.calculateMACD(symbol, 12, 26, 9); // 12, 26, 9 - это стандартные параметры для MACD

        // Получение предыдущей свечи для расчета дельты и роста объемов
        BigDecimal lastClosePrice = lastPriceChanges.getOrDefault(symbol + "_lastClose", BigDecimal.ZERO);
        BigDecimal lastVolume = lastPriceChanges.getOrDefault(symbol + "_lastVolume", BigDecimal.ZERO);

        BigDecimal currentPrice = binanceService.getCurrentPrice(symbol); // Получаем текущую цену

        BigDecimal rsi = binanceService.calculateRSI(symbol, 14); // 14 периодов для RSI

        List<Candlestick> latestCandlesticks = binanceService.getLatestCandlesticks(symbol);
        // Собираем здесь последние 14 закрывающих цен для Williams %R
        List<BigDecimal> closingPrices = latestCandlesticks.stream()
                .map(candlestick -> new BigDecimal(candlestick.getClose()))
                .toList();
        List<BigDecimal> highPrices = latestCandlesticks.stream().map(candlestick -> new BigDecimal(candlestick.getHigh())).toList();
        List<BigDecimal> lowPrices = latestCandlesticks.stream().map(candlestick -> new BigDecimal(candlestick.getLow())).toList();

        BigDecimal williamsR = binanceService.calculateWilliamsR(closingPrices, 14); // предположим, что мы используем 14 периодов
        // Расчет Стохастика
        BigDecimal[] stochasticValues = binanceService.calculateStochastic(closingPrices, highPrices, lowPrices, 14);

        BigDecimal adx = binanceService.calculateADX(symbol, 14); // Используем 14 периодов для ADX

        BigDecimal mfi = binanceService.calculateMFI(symbol, 14); // Расчет MFI

        BigDecimal adLine = binanceService.calculateADLine(symbol);

        // Получение значений %K и %D
        BigDecimal stochasticK = stochasticValues[0];
        BigDecimal stochasticD = stochasticValues[1];

        // Получение значений индикатора Боллинджера
        BollingerBands bollingerBands = binanceService.calculateBollingerBands(symbol, 14, 2);
        BigDecimal sma = bollingerBands.getSma();
        BigDecimal upperBand = bollingerBands.getUpperBand();
        BigDecimal lowerBand = bollingerBands.getLowerBand();

        // Расчет вероятности "пампинга"
        BigDecimal pumpProbability = binanceService.calculatePumpProbability(
                priceChangePercent,
                rsi,
                macd.getMacd(),
                sma,
                stochasticK,
                stochasticD,
                williamsR,
                upperBand,
                lowerBand,
                adx, // Передаем ADX
                mfi, // Передаем MFI
                adLine, // Передаем AD Line
                symbol // Передаем символ
        );

        // Проверяем наличие значения
        String williamsRString = williamsR != null ? williamsR.setScale(2, RoundingMode.HALF_UP).toString() : "N/A";
        String stochasticKString = stochasticK != null ? stochasticK.setScale(2, RoundingMode.HALF_UP).toString() : "N/A";
        String stochasticDString = stochasticD != null ? stochasticD.setScale(2, RoundingMode.HALF_UP).toString() : "N/A";

        // Форматирование Боллинджера
        String smaString = sma != null ? sma.setScale(2, RoundingMode.HALF_UP).toString() : "N/A";
        String upperBandString = upperBand != null ? upperBand.setScale(2, RoundingMode.HALF_UP).toString() : "N/A";
        String lowerBandString = lowerBand != null ? lowerBand.setScale(2, RoundingMode.HALF_UP).toString() : "N/A";
        String mfiString = mfi != null ? mfi.setScale(2, RoundingMode.HALF_UP).toString() : "N/A"; // Форматирование MFI

        // Форматирование MACD для отправки
        String macdString = macd.getMacd().setScale(2, RoundingMode.HALF_UP).toString();
        String signalString = macd.getSignal().setScale(2, RoundingMode.HALF_UP).toString();
        String histogramString = macd.getHistogram().setScale(2, RoundingMode.HALF_UP).toString();

        // Сохраняем последние значения
        lastPriceChanges.put(symbol + "_lastClose", closePrice);
        lastPriceChanges.put(symbol + "_lastVolume", volume);

        // Проверяем условия для отправки уведомления
        if (pumpProbability.compareTo(BigDecimal.valueOf(80)) > 0 && stochasticK.compareTo(BigDecimal.valueOf(88)) > 0) {

            // Формируем сообщение для бота
            String direction = "Pump";
            String emoji = "\uD83D\uDCC8"; // Зеленая стрелка вверх
            String tradingUrl = String.format("https://www.binance.com/en/trade/%s?ref=396823681", symbol);

            // Формируем сообщение для бота
            String message = String.format("❗️❗️❗️❗️❗️`%s` %s\n %s изменение цены: %.2f%% 🔥\n Текущая цена: %s\n ADX: %.2f\n MACD: %s\n Signal: %s \n Histogram: %s \n RSI: %s\n MFI: %s\n Williams (Индикатор моментума) R: %s\n Стохастик K: %s\n D: %s\n SMA: %s\n Боллинджер (верхняя граница): %s\n Боллинджер (нижняя граница): %s\n Вероятность пампинга: %.2f%%\n Объем: %s\uD83E\uDD11 \n AD Line: %.2f\n [Торгуй сейчас!](%s)✅",
            symbol, direction, emoji, priceChangePercent, currentPrice.setScale(2, RoundingMode.HALF_UP), adx.setScale(2, RoundingMode.HALF_UP), macdString, signalString, histogramString, rsi, mfiString, williamsRString, stochasticKString, stochasticDString, smaString, upperBandString, lowerBandString, pumpProbability, formattedVolume, adLine.setScale(2, RoundingMode.HALF_UP), tradingUrl);

            botService.sendMessageToAllUsers(message, symbol, latestCandlesticks); // Отправляем сообщение в бот

            // Обновляем время последнего уведомления
            lastNotificationTime.put(symbol, currentTime);
        }
    }
}
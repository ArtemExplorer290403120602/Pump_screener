package org.example.pump_screener.adapters;


import com.binance.connector.client.exceptions.BinanceClientException;
import lombok.Setter;
import org.example.pump_screener.service.BinanceService;
import org.example.pump_screener.service.BotService;
import org.example.pump_screener.socket.Candlestick;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class PriceVolumeWatcher {
    private final BinanceService binanceService;
    private final ApplicationEventPublisher eventPublisher;
    private final HashMap<String, BigDecimal> lastPriceChanges = new HashMap<>();

    @Setter
    private boolean monitoringTwoPercentActive = false;

    @Autowired
    public PriceVolumeWatcher(BinanceService binanceService, ApplicationEventPublisher eventPublisher) {
        this.binanceService = binanceService;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(fixedRate = 60000) // периодичность сообщений
    public void monitorTwoPercentGrowth() {
        if (monitoringTwoPercentActive) {
            System.out.println("Мониторинг 2% активен");

            // Объявляем список символов, которые необходимо отслеживать
            String[] symbolsToTrack = {"BTCUSDT", "DOGEUSDT", "TROYUSDT", "WLDUSDT", "SUIUSDT", "TIAUSDT", "ADAUSDT",
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
                    "LOKAUSDT", "HMSTRUSDT", "REIUSDT", "COSUSDT", "EIGENUSDT", "DIAUSDT", "SCRUSDT","SANTOSUSDT"};

            // Создание и запуск CompletableFuture для каждого символа
            CompletableFuture[] futures = Arrays.stream(symbolsToTrack)
                    .map(symbol -> CompletableFuture.runAsync(() -> processSymbol(symbol)))
                    .toArray(CompletableFuture[]::new);

            // ожидание завершения всех задач
            CompletableFuture.allOf(futures).join();
        }
    }

    private void processSymbol(String symbol) {
        try {
            List<Candlestick> latestCandlesticks = binanceService.getLatestCandlesticks(symbol);
            if (!latestCandlesticks.isEmpty()) {
                Candlestick candlestick = latestCandlesticks.get(0);


                BigDecimal openPrice = new BigDecimal(candlestick.getOpen());
                BigDecimal closePrice = new BigDecimal(candlestick.getClose());
                BigDecimal priceChangePercent = (closePrice.subtract(openPrice)).divide(openPrice, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));


                if (priceChangePercent.abs().compareTo(BigDecimal.valueOf(1.30)) >= 0) {
                    // Проверка, изменилось ли значение с момента последнего уведомления
                    BigDecimal lastChange = lastPriceChanges.getOrDefault(symbol, BigDecimal.ZERO);
                    if (lastChange.compareTo(priceChangePercent) != 0) {
                        lastPriceChanges.put(symbol, priceChangePercent);
                        String direction = priceChangePercent.compareTo(BigDecimal.ZERO) > 0 ? "выросла" : "упала";
                        String message = String.format("Цена %s за последнюю минуту %s на %.2f%%", symbol, direction, priceChangePercent);
                        System.out.println("Отправка сообщения: " + message);
                        eventPublisher.publishEvent(new PriceAlertEventImpl(symbol, priceChangePercent, BigDecimal.ZERO));
                    }
                }
            } else {
                System.out.println("Нет данных для символа: " + symbol);
            }
        } catch (BinanceClientException e) {
            System.err.println("Ошибка для символа " + symbol + ": " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Ошибка обработки символа " + symbol + ": " + e.getMessage());
        }
    }
}
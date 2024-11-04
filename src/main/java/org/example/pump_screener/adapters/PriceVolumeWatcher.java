package org.example.pump_screener.adapters;


import lombok.Setter;
import org.example.pump_screener.service.BinanceService;
import org.example.pump_screener.service.BotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class PriceVolumeWatcher {
    private final BinanceService binanceService;
    private final ApplicationEventPublisher eventPublisher;

    @Setter
    private boolean monitoringActive = false;
    @Setter
    private boolean monitoringStarted = false;
    @Setter
    private boolean monitoringTwoPercentActive = false;

    @Autowired
    public PriceVolumeWatcher(BinanceService binanceService, ApplicationEventPublisher eventPublisher) {
        this.binanceService = binanceService;
        this.eventPublisher = eventPublisher;
    }

    // Новый метод для получения текущего статуса мониторинга
    public boolean isMonitoringActive() {
        return monitoringActive;
    }


    @Scheduled(fixedRate = 60000)
    public void monitorPrices() {
        boolean monitoringActive1 = false;
        if (monitoringActive1) {
            System.out.println("Мониторинг цен активен");
            List<String> usdtPairs = binanceService.getAllUsdtPairs();
            for (String symbol : usdtPairs) {
                BigDecimal priceChange = binanceService.get1MinutePriceChange(symbol);
                BigDecimal volume = binanceService.get1MinuteVolume(symbol);

                System.out.println("Обработка символа: " + symbol + " - Изменение цены: " + priceChange + ", Объем: " + volume);

                // Упростите временно условие для тестирования
                if (priceChange.compareTo(BigDecimal.ZERO) > 0 && volume.compareTo(new BigDecimal("10")) > 0) {
                    System.out.println("Условия выполнены для: " + symbol);
                    PriceAlertEventImpl priceAlert = new PriceAlertEventImpl(symbol, priceChange, volume);
                    eventPublisher.publishEvent(priceAlert);
                } else {
                    System.out.println("Условия не выполнены для: " + symbol);
                }
            }
        }
    }

    @Scheduled(fixedRate = 60000)
    public void scheduleCandlestickUpdates() {
        if (monitoringActive) {
            System.out.println("Мониторинг свечей активен");
            String[] trackedPairs = {"BTCUSDT", "ETHUSDT", "SOLUSDT", "DOGEUSDT"}; // Фиксированный список пар

            for (String symbol : trackedPairs) {
                List<BinanceService.Candlestick> latestCandlesticks = binanceService.getLatestCandlesticks(symbol);
                if (!latestCandlesticks.isEmpty()) {
                    BinanceService.Candlestick candlestick = latestCandlesticks.get(0);
                    String message = String.format("Последняя свеча для %s:\nОткрытие: %s, Закрытие: %s, Макс.: %s, Мин.: %s, Объем: %s",
                            symbol, candlestick.getOpen(), candlestick.getClose(), candlestick.getHigh(), candlestick.getLow(), candlestick.getVolume());
                    System.out.println("Отправка сообщения: " + message);
                    eventPublisher.publishEvent(new CandlestickEventImpl(symbol, candlestick));
                } else {
                    System.out.println("Нет данных для символа: " + symbol);
                }
            }
        }
    }

    @Scheduled(fixedRate = 60000)
    public void scheduleCandlestickUpdatesAll() {
        if (monitoringStarted) {
            System.out.println("Мониторинг свечей активен");

            // Получаем все пары с USDT
            List<String> usdtPairs = binanceService.getAllUsdtPairs();
            for (String symbol : usdtPairs) {
                List<BinanceService.Candlestick> latestCandlesticks = binanceService.getLatestCandlesticks(symbol);
                if (!latestCandlesticks.isEmpty()) {
                    BinanceService.Candlestick candlestick = latestCandlesticks.get(0);
                    String message = String.format("Последняя свеча для %s:\nОткрытие: %s, Закрытие: %s, Макс.: %s, Мин.: %s, Объем: %s",
                            symbol, candlestick.getOpen(), candlestick.getClose(), candlestick.getHigh(), candlestick.getLow(), candlestick.getVolume());
                    System.out.println("Отправка сообщения: " + message);
                    eventPublisher.publishEvent(new CandlestickEventImpl(symbol, candlestick));
                } else {
                    System.out.println("Нет данных для символа: " + symbol);
                }
            }
        }
    }

    @Scheduled(fixedRate = 60000) // периодичность сообщений
    public void monitorTwoPercentGrowth() {
        if (monitoringTwoPercentActive) {
            System.out.println("Мониторинг 0.5% активен");

            String symbol = "BTCUSDT"; // Фокусируемся на Bitcoin фьючерсах
            List<BinanceService.Candlestick> latestCandlesticks = binanceService.getLatestCandlesticks(symbol);
            if (!latestCandlesticks.isEmpty()) {
                BinanceService.Candlestick candlestick = latestCandlesticks.get(0);

                // Логика для проверки роста на 0.05%
                BigDecimal openPrice = new BigDecimal(candlestick.getOpen());
                BigDecimal closePrice = new BigDecimal(candlestick.getClose());
                BigDecimal priceChangePercent = (closePrice.subtract(openPrice)).divide(openPrice, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));

                // Если изменение более 0.05%, отправляем уведомление
                if (priceChangePercent.abs().compareTo(BigDecimal.valueOf(0.05)) >= 0) {
                    String direction = priceChangePercent.compareTo(BigDecimal.ZERO) > 0 ? "выросла" : "упала";
                    String message = String.format("Цена %s за последнюю минуту %s на %.2f%%", symbol, direction, priceChangePercent);
                    System.out.println("Отправка сообщения: " + message);
                    eventPublisher.publishEvent(new PriceAlertEventImpl(symbol, priceChangePercent, BigDecimal.ZERO));
                }
            } else {
                System.out.println("Нет данных для символа: " + symbol);
            }
        }
    }
}

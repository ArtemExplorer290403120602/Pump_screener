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
import java.util.List;

@Component
public class PriceVolumeWatcher {
    private final BinanceService binanceService;
    private final ApplicationEventPublisher eventPublisher;

    @Setter
    private boolean monitoringActive = false;
    @Setter
    private boolean monitoringStarted = false;

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
}

package org.example.pump_screener.service;

import org.example.pump_screener.adapters.PriceVolumeWatcher;
import org.example.pump_screener.adapters.binance.CandlestickEvent;
import org.example.pump_screener.adapters.binance.PriceAlertEvent;
import org.example.pump_screener.config.BotConfig;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;


import java.util.ArrayList;
import java.util.List;

@Service
public class BotService extends TelegramLongPollingBot {
    private final BotConfig config;
    private final BinanceService binanceService;
    private final PriceVolumeWatcher priceVolumeWatcher;
    private List<Long> chatIds = new ArrayList<>();

    public BotService(BotConfig config, BinanceService binanceService, PriceVolumeWatcher priceVolumeWatcher) {
        this.config = config;
        this.binanceService = binanceService;
        this.priceVolumeWatcher = priceVolumeWatcher;

        List<BotCommand> listOfBotCommands = new ArrayList<>();
        listOfBotCommands.add(new BotCommand("/start", "Приветствует пользователя и объясняет, что делает бот."));
        listOfBotCommands.add(new BotCommand("/check_binance", "Проверяет статус вашего аккаунта на Binance."));
        listOfBotCommands.add(new BotCommand("/list_bitcoin_pairs", "Выводит список доступных биткойн-пар."));
        listOfBotCommands.add(new BotCommand("/list_usdt_pairs", "Выводит список доступных пар с USDT."));
        listOfBotCommands.add(new BotCommand("/start_monitoring", "Запускает мониторинг цен и объема."));
        listOfBotCommands.add(new BotCommand("/stop_monitoring", "Останавливает мониторинг цен и объема."));
        try {
            execute(new SetMyCommands(listOfBotCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBot_name();
    }

    @Override
    public String getBotToken() {
        return config.getBot_token();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            switch (messageText) {
                case "/start":
                    try {
                        startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                    break;
                case "/check_binance":
                    String result = binanceService.getAccountStatus();
                    sendMessageSafely(chatId, result);
                    break;
                case "/list_bitcoin_pairs":
                    List<String> bitcoinPairs = binanceService.getAllBitcoinPairs();
                    String result1 = "Доступные биткойн-пары:\n" + String.join("\n", bitcoinPairs);
                    sendMessageSafely(chatId, result1);
                    break;
                case "/list_usdt_pairs":
                    List<String> usdtPairs = binanceService.getAllUsdtPairs();
                    String result2 = usdtPairs.isEmpty() ? "Нет доступных USDT пар." : "Доступные USDT пары:\n" + String.join("\n", usdtPairs);
                    sendMessageSafely(chatId, result2);
                    break;
                case "/start_monitoring":
                    priceVolumeWatcher.setMonitoringActive(true);
                    sendMessageSafely(chatId, "Мониторинг цен и объема запущен.");
                    break;
                case "/stop_monitoring":
                    priceVolumeWatcher.setMonitoringActive(false);
                    sendMessageSafely(chatId, "Мониторинг цен и объема остановлен.");
                    break;
                default:
                    sendMessageSafely(chatId, "Пока ничего не придумал");
            }
        }
    }

    private void sendMessageSafely(long chatId, String message) {
        try {
            sendMessage(chatId, message);
        } catch (TelegramApiException e) {
            // Логируем ошибку отправки сообщения
            System.err.println("Ошибка отправки сообщения для chatId: " + chatId + ", сообщение: " + message + ", ошибка: " + e.getMessage());
        }
    }

    public void sendMessageToAllUsers(String message) {
        System.out.println("Отправка сообщения всем пользователям: " + message);
        for (Long chatId : chatIds) {
            try {
                sendMessage(chatId, message);
            } catch (TelegramApiException e) {
                // Логируем ошибку отправки сообщения для конкретного пользователя
                System.err.println("Ошибка отправки сообщения для chatId: " + chatId + ", ошибка: " + e.getMessage());
            }
        }
    }

    private void sendMessage(long chatId, String sendText) throws TelegramApiException {
        final int maxMessageLength = 4096;
        if (sendText.length() > maxMessageLength) {
            int start = 0;
            while (start < sendText.length()) {
                int end = Math.min(sendText.length(), start + maxMessageLength);
                String part = sendText.substring(start, end);
                SendMessage message = new SendMessage();
                message.setChatId(String.valueOf(chatId));
                message.setText(part);
                execute(message);
                start = end;
            }
        } else {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText(sendText);
            execute(message);
        }
    }

    private void startCommandReceived(long chatId, String name) throws TelegramApiException {
        if (!chatIds.contains(chatId)) {
            chatIds.add(chatId);
            System.out.println("Добавлен новый пользователь с chatId: " + chatId);
        }
        String answer = "Привет " + name + ", это бот поможет тебе заработать огромные бабки!";
        sendMessage(chatId, answer);
    }

    @EventListener
    public void handlePriceAlert(PriceAlertEvent event) {
        String message = String.format("%s (изменение)\nОбъем: %.2f $\nИзменение цены: %.2f%%",
                event.getSymbol(), event.getVolume(), event.getPriceChange());
        System.out.println("Отправка сообщения: " + message);
        sendMessageToAllUsers(message);
    }

    @EventListener
    public void handleCandlestickEvent(CandlestickEvent event) {
        String symbol = event.getSymbol();
        BinanceService.Candlestick candlestick = event.getCandlestick();
        String message = String.format("Последняя свеча для %s: Открытие: %s, Закрытие: %s, Макс.: %s, Мин.: %s, Объем: %s",
                symbol, candlestick.getOpen(), candlestick.getClose(), candlestick.getHigh(), candlestick.getLow(), candlestick.getVolume());
        System.out.println("Обработка события свечи: " + message);
        sendMessageToAllUsers(message);
    }
}
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


import java.math.BigDecimal;
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
        listOfBotCommands.add(new BotCommand("/start", "Перезапуск бота и добавление пользователя в бота"));
        listOfBotCommands.add(new BotCommand("/start_monitoring_two_procent", "Запускает мониторинг цен с условием роста на 2%."));
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
                case "/start_monitoring_two_procent":
                    priceVolumeWatcher.setMonitoringTwoPercentActive(true);
                    sendMessageSafely(chatId, "Мониторинг цен с условием роста на 2% запущен.");
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
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));

        // Устанавливаем режим Markdown для форматирования
        message.setText(sendText);
        message.setParseMode("Markdown"); // Указываем режим форматирования

        if (sendText.length() > maxMessageLength) {
            int start = 0;
            while (start < sendText.length()) {
                int end = Math.min(sendText.length(), start + maxMessageLength);
                String part = sendText.substring(start, end);
                message.setText(part);
                execute(message);
                start = end;
            }
        } else {
            execute(message);
        }
    }

    private void startCommandReceived(long chatId, String name) throws TelegramApiException {
        if (!chatIds.contains(chatId)) {
            chatIds.add(chatId);
            System.out.println("Добавлен новый пользователь с chatId: " + chatId);
        }
        String answer = "Привет " + name + ", это бот поможет тебе отслеживать изменение цены свечи на графике ( время 1 минута) !";
        sendMessage(chatId, answer);
    }

    @EventListener
    public void handlePriceAlert(PriceAlertEvent event) {
        String pumpOrDump = event.getPriceChange().compareTo(BigDecimal.ZERO) > 0 ? "Pump" : "Dump";

        // Определяем emoji для Pump и Dump
        String directionEmoji = pumpOrDump.equals("Pump") ? "📈" : "📉"; // Зеленая стрелка вверх (Pump) или красная стрелка вниз (Dump)
        String fireEmoji = "🔥"; // Emoji огня, который будет под сообщением
        String alertEmoji = "❗️❗️❗️❗️❗️"; // Три восклицательных знака (emoji) для предупреждения

        // Формируем сообщение с emoji, добавляя символ в обратные апострофы
        String message = String.format("%s\n\n %s ` %s ` (%s)\n изменение цены: %.2f%% %s",
                alertEmoji, directionEmoji, event.getSymbol(), pumpOrDump,
                event.getPriceChange(), fireEmoji);

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
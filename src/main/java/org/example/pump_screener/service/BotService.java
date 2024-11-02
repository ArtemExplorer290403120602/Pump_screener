package org.example.pump_screener.service;

import org.example.pump_screener.config.BotConfig;
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


    public BotService(BotConfig config, BinanceService binanceService) {
        this.config = config;
        this.binanceService = binanceService;
        List<BotCommand> listOfBotCommands = new ArrayList<>();
        listOfBotCommands.add(new BotCommand("/start", "Приветствует пользователя и объясняет, что делает бот."));
        listOfBotCommands.add(new BotCommand("/check_binance", "Проверяет статус вашего аккаунта на Binance."));
        listOfBotCommands.add(new BotCommand("/list_bitcoin_pairs", "Выводит список доступных биткойн-пар."));
        listOfBotCommands.add(new BotCommand("/list_usdt_pairs", "Выводит список доступных пар с USDT."));
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
                        throw new RuntimeException(e);
                    }
                    break;
                case "/check_binance":
                    String result = binanceService.getAccountStatus();
                    try {
                        sendMessage(chatId, result);
                    } catch (TelegramApiException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case "/list_bitcoin_pairs":
                    List<String> bitcoinPairs = binanceService.getAllBitcoinPairs();
                    String result1 = "Доступные биткойн-пары:\n" + String.join("\n", bitcoinPairs);
                    try {
                        sendMessage(chatId, result1);
                    } catch (TelegramApiException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case "/list_usdt_pairs":  // Обработка новой команды
                    List<String> usdtPairs = binanceService.getAllUsdtPairs();
                    if (usdtPairs.isEmpty()) {
                        try {
                            sendMessage(chatId, "Нет доступных USDT пар.");
                        } catch (TelegramApiException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        String result2 = "Доступные USDT пары:\n" + String.join("\n", usdtPairs);
                        try {
                            sendMessage(chatId, result2);
                        } catch (TelegramApiException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    break;
                default:
                    try {
                        sendMessage(chatId, "Пока ничего не придумал");
                    } catch (TelegramApiException e) {
                        throw new RuntimeException(e);
                    }
            }
        }
    }

    private void sendMessage(long chatId, String sendText) throws TelegramApiException {
        // Максимальная длина сообщения в Telegram
        final int maxMessageLength = 4096;

        // Если длина сообщения превышает максимальную, разбиваем его на части
        if (sendText.length() > maxMessageLength) {
            // Разбиваем сообщение на части по 4096 символов
            int start = 0;
            while (start < sendText.length()) {
                int end = Math.min(sendText.length(), start + maxMessageLength);
                String part = sendText.substring(start, end);
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(String.valueOf(chatId));
                sendMessage.setText(part);
                execute(sendMessage);
                start = end; // Переходим к следующей части
            }
        } else {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(String.valueOf(chatId));
            sendMessage.setText(sendText);
            execute(sendMessage);
        }
    }

    private void startCommandReceived(long chatId, String name) throws TelegramApiException {
        String answer = "Привет " + name + ", это бот поможет тебе заработать огромные бабки!";
        sendMessage(chatId, answer);
    }
}
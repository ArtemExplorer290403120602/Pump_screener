package org.example.pump_screener.service;

import org.example.pump_screener.config.BotConfig;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

@Service
public class BotService extends TelegramLongPollingBot {
    private final BotConfig config;
    private final BinanceService binanceService;

    public BotService(BotConfig config, BinanceService binanceService) {
        this.config = config;
        this.binanceService = binanceService;
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
                    String result = binanceService.getAccountStatus(); // Используем новый метод
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
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(sendText);
        execute(sendMessage);
    }

    private void startCommandReceived(long chatId, String name) throws TelegramApiException {
        String answer = "Привет " + name + ", это бот поможет тебе заработать огромные бабки!";
        sendMessage(chatId, answer);
    }
}
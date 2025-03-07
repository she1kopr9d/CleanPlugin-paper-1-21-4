package com.example.telegramtest;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.bukkit.Bukkit;
import org.telegram.telegrambots.meta.api.objects.User;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class TelegramBot extends TelegramLongPollingBot {
    private final MainPlugin plugin;
    private final String botToken;
    private final String botUsername;

    public TelegramBot(MainPlugin plugin, String botToken, String botUsername) {
        this.plugin = plugin;
        this.botToken = botToken;
        this.botUsername = botUsername;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String message = update.getMessage().getText();
            User user = update.getMessage().getFrom();
            String username = user.getUserName() != null ? user.getUserName() : user.getFirstName();

            // Отправляем сообщение в игру асинхронно
            Bukkit.getScheduler().runTask(plugin, () -> {
                // Создаем компонент сообщения с форматированием
                Component telegramMessage = Component.text("[TG] ")
                        .color(NamedTextColor.BLUE)
                        .append(Component.text(username + ": ")
                                .color(NamedTextColor.YELLOW))
                        .append(Component.text(message)
                                .color(NamedTextColor.WHITE));
                
                // Отправляем сообщение всем игрокам
                Bukkit.broadcast(telegramMessage);
            });
        }
    }
} 
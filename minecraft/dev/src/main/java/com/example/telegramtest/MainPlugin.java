package com.example.telegramtest;

import org.bukkit.plugin.java.JavaPlugin;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class MainPlugin extends JavaPlugin {
    private TelegramBot telegramBot;
    private AuthManager authManager;

    @Override
    public void onEnable() {
        // Инициализируем менеджер авторизации
        authManager = new AuthManager(this);
        
        // Создаем и регистрируем бота
        String botToken = getConfig().getString("bot-token");
        String botUsername = getConfig().getString("bot-username");
        
        if (botToken == null || botUsername == null) {
            getLogger().severe("Bot token or username not found in config.yml!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        telegramBot = new TelegramBot(this, botToken, botUsername);
        
        // Регистрируем слушатель
        getServer().getPluginManager().registerEvents(
            new AuthListener(this, authManager, telegramBot), 
            this
        );

        // Инициализируем и запускаем бота
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(telegramBot);
            getLogger().info("Telegram bot successfully started!");
        } catch (TelegramApiException e) {
            getLogger().severe("Failed to start Telegram bot: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info("Plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("Plugin has been disabled!");
    }

    public AuthManager getAuthManager() {
        return authManager;
    }
}
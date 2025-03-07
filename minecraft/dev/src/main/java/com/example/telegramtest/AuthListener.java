package com.example.telegramtest;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuthListener implements Listener {
    private final MainPlugin plugin;
    private final AuthManager authManager;
    private final TelegramBot bot;
    private final Map<UUID, Location> joinLocations = new HashMap<>();
    private final Map<UUID, BukkitTask> kickTasks = new HashMap<>();

    public AuthListener(MainPlugin plugin, AuthManager authManager, TelegramBot bot) {
        this.plugin = plugin;
        this.authManager = authManager;
        this.bot = bot;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();
        UUID playerUuid = player.getUniqueId();

        if (!authManager.isRegistered(playerName)) {
            player.kick(net.kyori.adventure.text.Component.text("Вы не зарегистрированы! Используйте /reg в Telegram боте"));
            return;
        }

        // Сохраняем позицию и замораживаем игрока
        joinLocations.put(playerUuid, player.getLocation());
        authManager.setPendingAuth(playerUuid);

        // Отправляем сообщение в Telegram
        Long telegramId = authManager.getTelegramId(playerName);
        if (telegramId != null) {
            bot.sendTelegramMessage(telegramId, 
                "⚠️ Обнаружен вход в игру!\n" +
                "Отправьте /confirm для подтверждения входа\n" +
                "У вас есть 60 секунд, иначе вы будете отключены");
        }

        // Запускаем таймер на кик
        BukkitTask kickTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!authManager.isAuthConfirmed(playerUuid)) {
                player.kick(net.kyori.adventure.text.Component.text("Время подтверждения входа истекло!"));
                authManager.removeAuthStatus(playerUuid);
                joinLocations.remove(playerUuid);
            }
        }, 20 * 60); // 60 секунд

        kickTasks.put(playerUuid, kickTask);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerUuid = event.getPlayer().getUniqueId();
        
        // Отменяем таск кика если есть
        BukkitTask kickTask = kickTasks.remove(playerUuid);
        if (kickTask != null) {
            kickTask.cancel();
        }

        // Очищаем данные
        authManager.removeAuthStatus(playerUuid);
        joinLocations.remove(playerUuid);
    }
} 
package com.example.telegramtest;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.Location;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import java.time.Duration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuthListener implements Listener {
    private final MainPlugin plugin;
    private final AuthManager authManager;
    private final TelegramBot bot;
    private final Map<UUID, Location> joinLocations = new HashMap<>();
    private final Map<UUID, BukkitTask> kickTasks = new HashMap<>();

    private final Title.Times TITLE_TIMES = Title.Times.times(
        Duration.ofMillis(500),
        Duration.ofMillis(3000),
        Duration.ofMillis(500)
    );

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

        // // Проверяем сессию
        String ip = player.getAddress().getAddress().getHostAddress();
        // if (authManager.isSessionValid(ip)) {
        //     return; // Пропускаем авторизацию если сессия активна
        // }

        if (!authManager.isRegistered(playerName)) {
            if (plugin.getConfig().getBoolean("show-registration-instructions", true)) {
                String instructions = plugin.getConfig().getString("registration-instructions", "")
                    .replace("{player}", playerName)
                    .replace("{bot-link}", plugin.getConfig().getString("bot-link", ""))
                    .replace("&", "§");
                player.sendMessage(instructions);
            }
            player.kick(Component.text("Вы не зарегистрированы! Используйте /reg в Telegram боте"));
            return;
        }

        // Сохраняем позицию и замораживаем игрока
        joinLocations.put(playerUuid, player.getLocation());
        authManager.setPendingAuth(playerUuid);

        // Показываем сообщение игроку
        player.showTitle(Title.title(
            Component.text("Подтвердите вход", NamedTextColor.GOLD),
            Component.text("Отправьте /confirm в Telegram бота", NamedTextColor.YELLOW),
            TITLE_TIMES
        ));

        // Отправляем сообщение в Telegram
        Long telegramId = authManager.getTelegramId(playerName);
        if (telegramId != null) {
            bot.sendTelegramMessage(telegramId, 
                String.format("""
                    ⚠️ Обнаружен вход в игру!
                    📍 IP-адрес: %s
                    
                    Если это вы:
                    /confirm - подтвердить вход
                    /cancel - отменить вход
                    
                    Если это не вы:
                    /block - заблокировать IP-адрес
                    
                    ⏰ У вас есть 60 секунд на ответ
                    """, ip));
        }

        // Запускаем таймер на кик
        BukkitTask kickTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!authManager.isAuthConfirmed(playerUuid)) {
                player.kick(Component.text("Время подтверждения входа истекло!"));
                authManager.removeAuthStatus(playerUuid);
                joinLocations.remove(playerUuid);
            }
        }, 20 * 60); // 60 секунд

        kickTasks.put(playerUuid, kickTask);

        // Запускаем таймер для повторяющихся сообщений
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!authManager.isAuthConfirmed(playerUuid) && player.isOnline()) {
                player.sendActionBar(Component.text("Ожидание подтверждения входа...", NamedTextColor.RED));
            }
        }, 0L, 20L); // Каждую секунду
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!authManager.isAuthConfirmed(player.getUniqueId())) {
            Location from = event.getFrom();
            Location to = event.getTo();
            // Разрешаем только поворот головы
            if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (!authManager.isAuthConfirmed(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("⛔ Сначала подтвердите вход!", NamedTextColor.RED));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            if (!authManager.isAuthConfirmed(player.getUniqueId()) && !authManager.isSessionValid(player.getAddress().getAddress().getHostAddress())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemDrop(PlayerDropItemEvent event) {
        if (!authManager.isAuthConfirmed(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!authManager.isAuthConfirmed(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!authManager.isAuthConfirmed(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!authManager.isAuthConfirmed(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
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
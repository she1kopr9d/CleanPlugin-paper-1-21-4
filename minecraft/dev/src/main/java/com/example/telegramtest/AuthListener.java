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
import org.bukkit.configuration.file.FileConfiguration;

public class AuthListener implements Listener {
    private final MainPlugin plugin;
    private final AuthManager authManager;
    private final TelegramBot bot;
    private final Map<UUID, Location> joinLocations = new HashMap<>();
    private final Map<UUID, BukkitTask> kickTasks = new HashMap<>();

    private final Title.Times TITLE_TIMES;

    public AuthListener(MainPlugin plugin, AuthManager authManager, TelegramBot bot) {
        this.plugin = plugin;
        this.authManager = authManager;
        this.bot = bot;

        // Load configuration
        FileConfiguration config = plugin.getConfig();
        long fadeIn = config.getLong("title-display-time.fade-in");
        long stay = config.getLong("title-display-time.stay");
        long fadeOut = config.getLong("title-display-time.fade-out");
        TITLE_TIMES = Title.Times.times(Duration.ofMillis(fadeIn), Duration.ofMillis(stay), Duration.ofMillis(fadeOut));
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
            player.kick(Component.text(plugin.getConfig().getString("kick-timeout-message")));
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
                    
                    ⏰ У вас есть %d секунд на ответ
                    """, ip, plugin.getConfig().getInt("confirmation-timeout")));
        }

        // Запускаем таймер на кик
        BukkitTask kickTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!authManager.isAuthConfirmed(playerUuid)) {
                player.kick(Component.text(plugin.getConfig().getString("kick-timeout-message")));
                authManager.removeAuthStatus(playerUuid);
                joinLocations.remove(playerUuid);
            }
        }, 20 * plugin.getConfig().getInt("confirmation-timeout")); // Use configurable timeout

        kickTasks.put(playerUuid, kickTask);

        // Запускаем таймер для повторяющихся сообщений
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!authManager.isAuthConfirmed(playerUuid) && player.isOnline()) {
                player.sendActionBar(Component.text(plugin.getConfig().getString("waiting-confirmation-message"), NamedTextColor.RED));
            }
        }, 0L, 20L); // Every second
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
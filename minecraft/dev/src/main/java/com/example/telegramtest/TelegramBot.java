package com.example.telegramtest;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.telegram.telegrambots.meta.api.objects.User;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.ConsoleCommandSender;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class TelegramBot extends TelegramLongPollingBot {
    private final MainPlugin plugin;
    private final String botToken;
    private final String botUsername;
    private final AuthManager authManager;

    public TelegramBot(MainPlugin plugin, String botToken, String botUsername) {
        this.plugin = plugin;
        this.botToken = botToken;
        this.botUsername = botUsername;
        this.authManager = plugin.getAuthManager();
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
            long chatId = update.getMessage().getChatId();
            User user = update.getMessage().getFrom();
            String username = user.getUserName() != null ? user.getUserName() : user.getFirstName();

            // Обработка команды /exec
            if (message.startsWith("/exec ")) {
                if (!isUserAllowed(username)) {
                    sendTelegramMessage(chatId, "У вас нет прав для использования этой команды!");
                    return;
                }

                String command = message.substring(6).trim(); // Убираем "/exec "
                handleExecCommand(chatId, command);
                return;
            }

            // Обработка команды /inv
            if (message.startsWith("/inv ")) {
                String playerName = message.substring(5).trim();
                handleInventoryCommand(chatId, playerName);
                return;
            }

            // Обработка команды /reg
            if (message.startsWith("/reg ")) {
                String minecraftUsername = message.substring(5).trim();
                handleRegistration(chatId, minecraftUsername);
                return;
            }
            
            // Обработка команды /start
            if (message.equals("/start")) {
                handleStart(chatId);
                return;
            }

            // Обработка команды /help
            if (message.equals("/help")) {
                handleHelp(chatId);
                return;
            }

            // Обработка команды /info
            if (message.equals("/info")) {
                handleInfo(chatId);
                return;
            }

            // Обработка команды /unreg
            if (message.equals("/unreg")) {
                handleUnregistration(chatId);
                return;
            }

            // Обработка подтверждения входа
            if (message.equals("/confirm")) {
                handleConfirmation(chatId);
                return;
            }

            // Обычное сообщение
            // Отправляем сообщение в игру асинхронно
            Bukkit.getScheduler().runTask(plugin, () -> {
                Component telegramMessage = Component.text("[TG] ")
                        .color(NamedTextColor.BLUE)
                        .append(Component.text(username + ": ")
                                .color(NamedTextColor.YELLOW))
                        .append(Component.text(message)
                                .color(NamedTextColor.WHITE));
                
                Bukkit.broadcast(telegramMessage);
            });
        }
    }

    private boolean isUserAllowed(String username) {
        File opsFile = new File("ops.json");
        if (!opsFile.exists()) {
            plugin.getLogger().warning("ops.json not found!");
            return false;
        }

        try (FileReader reader = new FileReader(opsFile)) {
            Gson gson = new Gson();
            JsonArray opsArray = gson.fromJson(reader, JsonArray.class);

            for (int i = 0; i < opsArray.size(); i++) {
                JsonObject op = opsArray.get(i).getAsJsonObject();
                String name = op.get("name").getAsString();
                if (username.equalsIgnoreCase(name)) {
                    return true;
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Error reading ops.json: " + e.getMessage());
        }

        return false;
    }

    private void handleExecCommand(long chatId, String command) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
                boolean success = Bukkit.dispatchCommand(console, command);
                
                if (success) {
                    sendTelegramMessage(chatId, "✅ Команда выполнена: /" + command);
                } else {
                    sendTelegramMessage(chatId, "❌ Ошибка выполнения команды: /" + command);
                }
            } catch (Exception e) {
                sendTelegramMessage(chatId, "Произошла ошибка: " + e.getMessage());
                plugin.getLogger().warning("Error executing command from Telegram: " + e.getMessage());
            }
        });
    }

    private void handleInventoryCommand(long chatId, String playerName) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(playerName);
            if (player == null) {
                sendTelegramMessage(chatId, "Игрок " + playerName + " не найден или не в сети");
                return;
            }

            StringBuilder inventory = new StringBuilder();
            inventory.append("Инвентарь игрока ").append(playerName).append(":\n\n");

            // Основной инвентарь
            ItemStack[] items = player.getInventory().getContents();
            for (int i = 0; i < items.length; i++) {
                ItemStack item = items[i];
                if (item != null) {
                    String itemName = getItemName(item);
                    int amount = item.getAmount();
                    inventory.append(String.format("Слот %d: %s x%d\n", i, itemName, amount));
                }
            }

            // Броня
            inventory.append("\nБроня:\n");
            ItemStack[] armor = player.getInventory().getArmorContents();
            String[] armorSlots = {"Ботинки", "Поножи", "Нагрудник", "Шлем"};
            for (int i = 0; i < armor.length; i++) {
                if (armor[i] != null) {
                    String itemName = getItemName(armor[i]);
                    inventory.append(String.format("%s: %s\n", armorSlots[i], itemName));
                }
            }

            // Предмет в левой руке
            ItemStack offHand = player.getInventory().getItemInOffHand();
            if (offHand.getType().isAir() == false) {
                inventory.append("\nЛевая рука: ").append(getItemName(offHand));
            }

            sendTelegramMessage(chatId, inventory.toString());
        });
    }

    private String getItemName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return item.getType().toString().toLowerCase().replace('_', ' ');
    }

    public void sendTelegramMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            plugin.getLogger().warning("Failed to send Telegram message: " + e.getMessage());
        }
    }

    private void handleRegistration(long chatId, String minecraftUsername) {
        try {
            if (authManager.isRegistered(minecraftUsername)) {
                sendTelegramMessage(chatId, "❌ Этот аккаунт Minecraft уже зарегистрирован!");
                return;
            }

            authManager.registerPlayer(minecraftUsername, chatId);
            sendTelegramMessage(chatId, "✅ Регистрация успешна! Теперь вы можете войти на сервер.\n" +
                    "При входе вам придет запрос на подтверждение.");
        } catch (IllegalStateException e) {
            sendTelegramMessage(chatId, "❌ " + e.getMessage());
        }
    }

    private void handleInfo(long chatId) {
        String username = authManager.getMinecraftUsername(chatId);
        if (username != null) {
            sendTelegramMessage(chatId, "ℹ️ Ваш Telegram привязан к нику: " + username);
        } else {
            sendTelegramMessage(chatId, "❌ У вас нет привязанного аккаунта Minecraft");
        }
    }

    private void handleUnregistration(long chatId) {
        String username = authManager.getMinecraftUsername(chatId);
        if (username != null) {
            authManager.unregisterPlayer(chatId);
            sendTelegramMessage(chatId, "✅ Аккаунт успешно отвязан");
        } else {
            sendTelegramMessage(chatId, "❌ У вас нет привязанного аккаунта");
        }
    }

    private void handleConfirmation(long chatId) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                String playerName = player.getName();
                Long registeredChatId = authManager.getTelegramId(playerName);
                
                if (registeredChatId != null && registeredChatId == chatId) {
                    authManager.confirmAuth(player.getUniqueId());
                    sendTelegramMessage(chatId, "✅ Вход подтвержден!");
                    return;
                }
            }
            sendTelegramMessage(chatId, "❌ Нет ожидающих подтверждения входов для вашего аккаунта");
        });
    }

    private void handleStart(long chatId) {
        String welcomeMessage = """
            �� Добро пожаловать в бот авторизации Minecraft!
            
            🔐 Этот бот используется для:
            • Привязки аккаунта Minecraft к Telegram
            • Подтверждения входа на сервер
            • Защиты вашего аккаунта
            
            📝 Чтобы начать, используйте команду:
            /reg <ваш_ник> - для привязки аккаунта
            
            ❓ Для просмотра всех команд используйте /help
            """;
        
        sendTelegramMessage(chatId, welcomeMessage);
    }

    private void handleHelp(long chatId) {
        String helpMessage = """
            📋 Доступные команды:
            
            🔐 Авторизация:
            /reg <ник> - привязать аккаунт Minecraft
            /unreg - отвязать аккаунт
            /info - информация о привязанном аккаунте
            /confirm - подтвердить вход на сервер
            
            🛠️ Управление сервером (только для админов):
            /exec <команда> - выполнить команду на сервере
            /inv <игрок> - просмотреть инвентарь игрока
            
            ❓ Помощь:
            /start - приветственное сообщение
            /help - список всех команд
            """;

        sendTelegramMessage(chatId, helpMessage);
    }
} 
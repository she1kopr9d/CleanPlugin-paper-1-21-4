package com.example.untitled1;


import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.bukkit.command.CommandExecutor;

public abstract class ArtelCommand implements CommandExecutor {
    protected final MainPlugin plugin;

    // Конструктор получает ссылку на основной класс плагина
    public ArtelCommand(MainPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Базовая логика обработки команд
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cЭта команда может быть использована только игроком!");
            return true;
        }

        Player player = (Player) sender;
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        // Делегируем выполнение конкретных команд в Artel1
        return executeCommand(player, args);
    }

    // Абстрактный метод, который будет реализован в Artel1
    protected abstract boolean executeCommand(Player player, String[] args);

    // Базовые утилитные методы
    protected void sendHelp(Player player) {
        player.sendMessage("§6=== Помощь по командам артели ===");
        player.sendMessage("§f/artel create <название> §7- Создать новую артель");
        player.sendMessage("§f/artel info §7- Открыть информацию о вашей статистике и артели");
        player.sendMessage("§f/artel rename <имя> §7- Переименовать артель");
        player.sendMessage("§f/artel add <игрок> §7- Добавить игрока в артель");
        player.sendMessage("§f/artel kick <игрок> §7- Выгнать игрока из артели");
        player.sendMessage("§f/artel leave §7- Покинуть артель");
        player.sendMessage("§f/artel delete §7- Удалить артель");
        player.sendMessage("§f/artel dop <игрок> §7- Передать права главы артели другому игроку");
    }
}

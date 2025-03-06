package com.example.untitled1;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;
import java.util.Arrays;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

public class Artel1 extends ArtelCommand {
    public Artel1(MainPlugin plugin) {
        super(plugin);
    }

    @Override
    protected boolean executeCommand(Player player, String[] args) {
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "create":
                if (args.length < 2) {
                    player.sendMessage("§cИспользование: /artel create <Назване Артеля>");
                    return true;
                }
                if (args.length > 2) {
                    player.sendMessage("§cИмя артеля не должно содержать пробелы");
                    return true;
                }

                createArtel(player, Arrays.copyOfRange(args, 0, args.length));
                break;

            case "add":
                if (args.length < 2) {
                    player.sendMessage("§cИспользование: /artel add <Имя_Игрока>");
                    return true;
                }
                addPlayerToArtel(player, args[1]);
                break;

            case "kick":
                if (args.length < 2) {
                    player.sendMessage("§cИспользование: /artel kick <Имя_Игрока>");
                    return true;
                }
                removePlayerFromArtel(player, args[1]);
                break;

            case "info":
                infoArtel(player);
                break;

            case "rename":
                if (args.length < 2) {
                    player.sendMessage("§cИспользование: /artel rename <новое_имя_Артеля>");
                    return true;
                }
                renameArtel(player, Arrays.copyOfRange(args, 1, args.length));
                break;

            case "delete":
                if (args.length < 2) {
                    player.sendMessage("Осторожно! Данная команда полностью удалит артель!\n"
                    + "§cДля использоования пропишите: /artel delete delte");
                    return true;
                }
                deleteArtel(player, args[1]);
                break;
            
            case "dop":
                if (args.length < 2) {
                    player.sendMessage("§cИспользование: /artel dop <Имя_Игрока>");
                    return true;
                }
                deligateOfPermissions(player, args[1]);
                break;

            case "leave":
                leaveArtel(player);
                break;

            default:
                sendHelp(player);
                break;
        }
        return true;
    }

    // создание артели
    private void createArtel(Player player, String[] args) {
        String artelName = args[1];
        if (!artelName.matches("^[a-zA-Z0-9_-]+$")) {
            player.sendMessage("§cИмя артели может содержать только буквы, цифры, - и _");
            return;
        }

        if (player.getScoreboardTags().contains("arteler")) {
            player.sendMessage("§aВы уже состоите в артели!");
            return;
        }
        
        player.addScoreboardTag("arteler");
        player.addScoreboardTag(artelName);
        player.addScoreboardTag("bossArtel");

        int balance = plugin.getPlayerData(player);
        
        plugin.setPlayerData(player, balance);
        plugin.setArtelData(artelName, 1);
        plugin.saveData();

        player.sendMessage("§aДанные успешно сохранены!");
    }

    private void infoArtel(Player player) {
        if (!player.getScoreboardTags().contains("arteler")) {
            player.sendMessage("§cВы не состоите в артели");
            return;
        }

        String artelName = findArtelTag(player);
        int artelReputation = plugin.getArtelData(artelName);
        int playerBalance = plugin.getPlayerData(player);
        int membersCount = countAllPlayersWithTag(artelName);
        boolean isBoss = player.getScoreboardTags().contains("bossArtel");

        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        
        meta.setTitle("Информация об артели");
        meta.setAuthor("Система артелей");
        
        String page = String.format(
            "§l§6Информация об артели§r\n\n" +
            "§3Название артели:§r %s\n" +
            "§3Репутация артели:§r %d\n" +
            "§3Количество участников:§r %d\n" +
            "\n§l§6Ваша информация§r\n\n" +
            "§3Ваш баланс:§r %d\n" +
            "§3Ваша роль:§r %s\n" +
            "\n§8Используйте команду /artel help для просмотра всех команд",
            artelName,
            artelReputation,
            membersCount,
            playerBalance,
            isBoss ? "Глава артели" : "Участник артели"
        );

        meta.addPage(page);
        book.setItemMeta(meta);
        
        player.openBook(book);
    }

    private void addPlayerToArtel(Player player, String playerName) {
        if (!player.getScoreboardTags().contains("bossArtel")) { 
            player.sendMessage("§cУ вас нет прав для добавления игроков в Артель");
            return;
        }
        
        Player newMember = Bukkit.getPlayer(playerName);
        if (newMember == null) {
            player.sendMessage("§cИгрок не найден или не в сети");
            return;
        }
        if (newMember.getScoreboardTags().contains("arteler")) {
            player.sendMessage("§cИгрок уже состоит в артели!");
            return;
        }

        String artelTag = findArtelTag(player);
        int balance = plugin.getPlayerData(newMember);
        plugin.setPlayerData(newMember, balance);
        newMember.addScoreboardTag("arteler");
        newMember.addScoreboardTag(artelTag);

        player.sendMessage("§aИгрок " + playerName + " успешно добавлен в артель");
        newMember.sendMessage("§aВы были добавлены в артель " + artelTag);
        plugin.saveData();
    }

    // находит название артеля, через тег игрока
    private String findArtelTag(Player player) {
        for (String tag : player.getScoreboardTags()) {
            if (tag.equals("bossArtel")) continue;
            if (tag.equals("arteler")) continue;
            if (plugin.getArtelData(tag) != 0) return tag;
        }
        return null;
    }

    // переименование артели
    private void renameArtel(Player player, String[] args) {
        // проверка на права(только босс может переименовывать артель)
        if (!player.getScoreboardTags().contains("bossArtel")) { 
            player.sendMessage("§cУ вас нет прав для переименования Артели");
            return;
        }

        String oldName = findArtelTag(player);

        String newName = args[0];
        //проверка совпадения имен
        if (plugin.getArtelData(newName) != 0) { 
            player.sendMessage("§cАртель с таким именем уже существует");
            return;
        }
        //передача данных, удаление старого имени, добавление нового имени + сохранение данных
        plugin.setArtelData(newName, plugin.getArtelData(oldName)); 
        plugin.removeArtelData(oldName);
        player.removeScoreboardTag(oldName);
        player.addScoreboardTag(newName);
        plugin.saveData();
        player.sendMessage("§aАртель успешно переименована в " + newName);
    }

    private void removePlayerFromArtel(Player player, String playerName) {
        if (!player.getScoreboardTags().contains("bossArtel")) {
            player.sendMessage("§cУ вас нет прав для удаления Артели");
            return;
        }

        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            player.sendMessage("§cИгрок не найден или не в сети");
            return;
        }

        String artelTag = findArtelTag(player);
        target.removeScoreboardTag(artelTag);
        target.removeScoreboardTag("arteler");
        plugin.saveData();
        
        player.sendMessage("§aИгрок " + playerName + " был исключен из артели");
        target.sendMessage("§cВы были исключены из артели " + artelTag);
    }    

    private void deligateOfPermissions(Player player, String playerName) {
        if (!player.getScoreboardTags().contains("bossArtel")) {
            player.sendMessage("§cУ вас нет прав для передачи прав");
            return;
        }
        Player newBoss = Bukkit.getPlayer(playerName);
        String artelTag = findArtelTag(player);
        if (artelTag != findArtelTag(newBoss)) {
            player.sendMessage("§cИгрок должен состоять в артели");
            return;
        }
        
        newBoss.addScoreboardTag("bossArtel");
        player.removeScoreboardTag("bossArtel");
        player.sendMessage("§aВы передали права новому главе Артели");
        newBoss.sendMessage("§aВы стали новой главой Артели");
        plugin.saveData();
    }

    private void deleteArtel(Player player, String confim) {
        if (!confim.equals("delete")) {
            player.sendMessage("§cИспользование: /artel delete delete");
            return;
        }
        if (!player.getScoreboardTags().contains("bossArtel")) {
            player.sendMessage("§cУ вас нет прав для удаления Артели");
            return;
        }
        int count = countAllPlayersWithTag(findArtelTag(player));
        if (count > 1) {
            player.sendMessage("§cВ артели не должно оставастя игроков(помимо вас), перед ее удалением" +
            "Осталось: " + (count-1) + " игроков");
            return;
        }

        plugin.removeArtelData(findArtelTag(player));
        player.removeScoreboardTag("bossArtel");
        player.removeScoreboardTag("arteler");
        player.sendMessage("§aАртель успешно удалена"); //метод незавершен
        plugin.saveData();
    }

    private int countAllPlayersWithTag(String someTag) {
        int count = 0;
        // Проверяем только онлайн игроков
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.getScoreboardTags().contains(someTag)) {
                count++;
            }
        }
        return count;
    }

    private void leaveArtel(Player player) {
        if (!player.getScoreboardTags().contains("arteler")) {
            player.sendMessage("§cВы не состоите в артели");
            return;
        }
        if (player.getScoreboardTags().contains("bossArtel")) {
            player.sendMessage("§cВы не можете покинуть артель, пока являетесь главой" +
            "Можете передать права другому игроку командой: /artel dop <Имя_Игрока>");
            return;
        }
        player.removeScoreboardTag("arteler");
        player.removeScoreboardTag(findArtelTag(player));
        player.sendMessage("§aВы покинули артель");
        plugin.saveData();
    }
}

package com.example.untitled1;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MainPlugin extends JavaPlugin {
    // Хранит данные игроков в памяти (ключ - UUID игрока, значение - числовые данные)
    private Map<UUID, Integer> playerData;
    private Map<String, Integer> artelData;
    // Ссылка на файл data.yml
    private File dataFilePlayer;
    private File dataFileArtel;

    // Объект для работы с YAML конфигурацией
    private FileConfiguration dataConfigPlayer;
    private FileConfiguration dataConfigArtel;

    @Override
    public void onEnable() {
        // Инициализируем HashMap при запуске плагина
        playerData = new HashMap<>();
        artelData = new HashMap<>();

        // Создаем/проверяем файл данных
        createDataFile();

        // Загружаем существующие данные из файла
        loadData();

        // Запускаем автосохранение каждые 5 минут (6000 тиков = 5 минут)
        Bukkit.getScheduler().runTaskTimer(this, this::saveData, 6000L, 6000L);

        // Регистрируем команду
        getCommand("artel").setExecutor(new Artel1(this));
    }

    // Методы для работы с данными игроков
    public void setPlayerData(@NotNull Player player, Integer balance) {
        playerData.put(player.getUniqueId(), balance);
    }
    public Integer getPlayerData(Player player) {
        return playerData.getOrDefault(player.getUniqueId(), 0);
    }
    // для артелей
    public void setArtelData(String nameArtel, int reputation) {
        artelData.put(nameArtel, reputation);
    }
    public int getArtelData(String nameArtel) {
        return artelData.getOrDefault(nameArtel, 0);
    }
    public void removeArtelData(String nameArtel) {
        artelData.remove(nameArtel);
    }

    // Создание файла данных
    private void createDataFile() {
        dataFilePlayer = new File(getDataFolder(), "dataPlayer.yml");
        dataFileArtel = new File(getDataFolder(), "dataArtel.yml");
        if (!dataFilePlayer.exists()) {
            // Создаем директорию плагина, если её нет
            dataFilePlayer.getParentFile().mkdirs();
            try {
                // Создаем сам файл
                dataFilePlayer.createNewFile();
            } catch (IOException e) {
                getLogger().severe("Не удалось создать файл dataPlayer.yml!");
                e.printStackTrace();
            }
        }
        if (!dataFileArtel.exists()) { // Проверяем, существует ли файл dataArtel.yml
            dataFileArtel.getParentFile().mkdirs();
            try {
                // Создаем сам файл
                dataFileArtel.createNewFile();
            } catch (IOException e) {
                getLogger().severe("Не удалось создать файл dataArtel.yml!");
                e.printStackTrace();
            }
        }
        // Загружаем конфигурацию из файла
        dataConfigPlayer = YamlConfiguration.loadConfiguration(dataFilePlayer); // Загружаем конфигурацию для игроков
        dataConfigArtel = YamlConfiguration.loadConfiguration(dataFileArtel); // Загружаем конфигурацию для артелей
    }

    // Загрузка данных из файла в HashMap
    private void loadData() {
        for (String uuidString : dataConfigPlayer.getKeys(false)) {
            playerData.put(UUID.fromString(uuidString),
                    dataConfigPlayer.getInt(uuidString));
        }
        for (String uuidString : dataConfigArtel.getKeys(false)) {
            artelData.put(uuidString,
                    dataConfigArtel.getInt(uuidString));
        }
    }

    // Сохранение данных из HashMap в файл
    public void saveData() {
        for (Map.Entry<UUID, Integer> entry : playerData.entrySet()) {
            dataConfigPlayer.set(entry.getKey().toString(), entry.getValue());
        }
        try {
            dataConfigPlayer.save(dataFilePlayer);
        } catch (IOException e) {
            getLogger().severe("Не удалось сохранить данные об игроках!");
            e.printStackTrace();
        }

        for (Map.Entry<String, Integer> entry : artelData.entrySet()) {
            dataConfigArtel.set(entry.getKey().toString(), entry.getValue());
        }
        try {
            dataConfigArtel.save(dataFileArtel);
        } catch (IOException e) {
            getLogger().severe("Не удалось сохранить данные об артелях!");
            e.printStackTrace();
        }
    }
}
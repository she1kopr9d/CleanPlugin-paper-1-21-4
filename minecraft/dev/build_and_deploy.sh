#!/bin/bash

# Путь к папке plugins сервера (относительно корневой папки minecraft)
SERVER_PLUGINS_DIR="../paper_server/plugins"

# Проверяем наличие Maven
if ! command -v mvn &> /dev/null; then
    echo "Maven не установлен. Устанавливаем..."
    if [ -f "../install_maven.sh" ]; then
        bash ../install_maven.sh
    elif [ -f "../../install_maven.sh" ]; then
        bash ../../install_maven.sh
    else
        echo "Скрипт установки Maven не найден!"
        echo "Убедитесь, что install_maven.sh находится в родительской директории"
        exit 1
    fi
fi

# Проверяем версию Java
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" != "21" ]; then
    echo "Требуется Java 21. Текущая версия: $JAVA_VERSION"
    echo "Пожалуйста, установите Java 21"
    exit 1
fi

# Собираем проект
echo "Сборка проекта..."
if ! mvn clean package; then
    echo "Ошибка при сборке проекта!"
    exit 1
fi

# Создаем директорию plugins, если её нет
mkdir -p "$SERVER_PLUGINS_DIR"

# Копируем собранный плагин
echo "Копирование плагина в папку plugins..."
cp target/paper-plugin-1.0-SNAPSHOT.jar "$SERVER_PLUGINS_DIR/"

# Проверяем успешность копирования
if [ $? -eq 0 ]; then
    echo "Плагин успешно скопирован в папку plugins!"
    echo "Перезапустите сервер для применения изменений"
else
    echo "Ошибка при копировании плагина!"
    exit 1
fi 
#!/bin/bash

# Создаем структуру директорий
mkdir -p src/main/java/com/example/paperplugin
mkdir -p src/main/resources
mkdir -p src/test/java

# Копируем файлы из текущей директории
if [ -f "PaperPlugin.java" ]; then
    mv PaperPlugin.java src/main/java/com/example/paperplugin/
fi

if [ -f "plugin.yml" ]; then
    mv plugin.yml src/main/resources/
fi

# Проверяем наличие Maven
if ! command -v mvn &> /dev/null; then
    echo "Maven не установлен. Устанавливаем..."
    if [ -f "../install_maven.sh" ]; then
        bash ../install_maven.sh
    else
        echo "Скрипт установки Maven не найден!"
        exit 1
    fi
fi

# Проверяем версию Java
JAVA_VERSION=$("$JAVA_HOME/bin/java" -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" != "21" ]; then
    echo "Требуется Java 21. Текущая версия: $JAVA_VERSION"
    echo "Пожалуйста, установите Java 21"
    exit 1
fi

# Компилируем проект
mvn clean package

echo "Проект инициализирован!"
echo "Собранный плагин находится в: target/paper-plugin-1.0-SNAPSHOT.jar" 
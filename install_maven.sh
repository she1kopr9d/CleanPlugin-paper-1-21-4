#!/bin/bash

# Определяем версию Maven для установки
MAVEN_VERSION="3.9.6"

# Проверяем, установлен ли Homebrew
if ! command -v brew &> /dev/null; then
    echo "Homebrew не установлен. Устанавливаем Homebrew..."
    /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
fi

# Проверяем, установлена ли Java
if ! command -v java &> /dev/null; then
    echo "Java не установлена. Устанавливаем OpenJDK..."
    brew install openjdk
fi

# Устанавливаем Maven через Homebrew
echo "Устанавливаем Apache Maven..."
brew install maven

# Проверяем установку
echo "Проверяем установку Maven..."
mvn -version

echo "Maven успешно установлен!"

# Выводим информацию о путях
echo -e "\nПути Maven:"
echo "Maven исполняемый файл: $(which mvn)"
echo "Maven домашняя директория: $(brew --prefix maven)" 
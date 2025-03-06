#!/bin/bash

# Проверяем, установлен ли Homebrew
if ! command -v brew &> /dev/null; then
    echo "Homebrew не установлен. Устанавливаем Homebrew..."
    /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
    
    # Добавляем Homebrew в PATH
    echo 'eval "$(/opt/homebrew/bin/brew shellenv)"' >> ~/.zprofile
    eval "$(/opt/homebrew/bin/brew shellenv)"
fi

# Удаляем старые версии Java если установлены
if brew list openjdk@17 &>/dev/null; then
    echo "Удаляем Java 17..."
    brew uninstall openjdk@17
fi

# Устанавливаем Java 21
echo "Устанавливаем Java 21..."
brew install openjdk@21

# Настраиваем Java 21 как системную Java
echo "Настраиваем Java 21..."
sudo ln -sfn $(brew --prefix)/opt/openjdk@21/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-21.jdk

# Добавляем переменные окружения Java в профиль
echo 'export PATH="$(brew --prefix)/opt/openjdk@21/bin:$PATH"' >> ~/.zprofile
echo 'export JAVA_HOME="$(brew --prefix)/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"' >> ~/.zprofile

# Применяем изменения в текущей сессии
export PATH="$(brew --prefix)/opt/openjdk@21/bin:$PATH"
export JAVA_HOME="$(brew --prefix)/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"

# Проверяем версию Java
echo "Проверяем версию Java..."
java -version

# Устанавливаем Maven
echo "Устанавливаем Maven..."
brew install maven

# Проверяем версию Maven
echo "Проверяем версию Maven..."
mvn -version

echo "Установка завершена!"
echo "Java 21 и Maven успешно установлены!"
echo "Чтобы изменения вступили в силу, может потребоваться перезапустить терминал" 
#!/bin/bash

# Проверяем, установлен ли Homebrew
if ! command -v brew &> /dev/null; then
    echo "Homebrew не установлен. Устанавливаем Homebrew..."
    /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
fi

# Удаляем Java 17 если установлена
if brew list openjdk@17 &>/dev/null; then
    echo "Удаляем Java 17..."
    brew uninstall openjdk@17
fi

# Устанавливаем Java 21
echo "Устанавливаем Java 21..."
brew install openjdk@21

# Получаем путь к Java 21
JAVA_PATH="$(brew --prefix)/opt/openjdk@21/bin/java"

# Проверяем версию Java
echo "Проверяем версию Java..."
"$JAVA_PATH" -version

# Создаем директорию для сервера
mkdir -p paper_server
cd paper_server

# Скачиваем последнюю версию Paper 1.21.1
echo "Скачиваем Paper 1.21.1..."
curl -o paper.jar "https://api.papermc.io/v2/projects/paper/versions/1.21.1/builds/latest/downloads/paper-1.21.1-latest.jar"

# Создаем скрипт для запуска с оптимизированными настройками Java
cat > start.sh << EOF
#!/bin/bash

# Путь к Java 21
JAVA_PATH="$(brew --prefix)/opt/openjdk@21/bin/java"

# Проверяем наличие Java 21
if [ ! -f "\$JAVA_PATH" ]; then
    echo "Java 21 не найдена. Пожалуйста, переустановите сервер."
    exit 1
fi

# Минимальное количество RAM
MIN_RAM="1G"
# Максимальное количество RAM
MAX_RAM="4G"

# Запуск сервера с оптимизированными параметрами
"\$JAVA_PATH" -Xms\${MIN_RAM} -Xmx\${MAX_RAM} \\
    -XX:+UseG1GC \\
    -XX:+ParallelRefProcEnabled \\
    -XX:MaxGCPauseMillis=200 \\
    -XX:+UnlockExperimentalVMOptions \\
    -XX:+DisableExplicitGC \\
    -XX:+AlwaysPreTouch \\
    -XX:G1NewSizePercent=30 \\
    -XX:G1MaxNewSizePercent=40 \\
    -XX:G1HeapRegionSize=8M \\
    -XX:G1ReservePercent=20 \\
    -XX:G1HeapWastePercent=5 \\
    -XX:G1MixedGCCountTarget=4 \\
    -XX:InitiatingHeapOccupancyPercent=15 \\
    -XX:G1MixedGCLiveThresholdPercent=90 \\
    -XX:G1RSetUpdatingPauseTimePercent=5 \\
    -XX:SurvivorRatio=32 \\
    -XX:+PerfDisableSharedMem \\
    -XX:MaxTenuringThreshold=1 \\
    -jar paper.jar nogui
EOF

# Делаем скрипт запуска исполняемым
chmod +x start.sh

# Создаем eula.txt с принятием условий
echo "eula=true" > eula.txt

# Создаем базовый server.properties с оптимизированными настройками
cat > server.properties << 'EOF'
spawn-protection=16
max-tick-time=60000
query.port=25565
generator-settings={}
sync-chunk-writes=true
force-gamemode=false
allow-nether=true
enforce-whitelist=false
gamemode=survival
broadcast-console-to-ops=true
enable-query=false
player-idle-timeout=0
difficulty=easy
spawn-monsters=true
broadcast-rcon-to-ops=true
op-permission-level=4
pvp=true
entity-broadcast-range-percentage=100
snooper-enabled=true
level-type=default
hardcore=false
enable-command-block=false
network-compression-threshold=256
max-players=20
max-world-size=29999984
resource-pack-sha1=
function-permission-level=2
rcon.port=25575
server-port=25565
debug=false
server-ip=
spawn-npcs=true
allow-flight=false
level-name=world
view-distance=10
resource-pack=
spawn-animals=true
white-list=false
rcon.password=
generate-structures=true
online-mode=true
max-build-height=256
level-seed=
prevent-proxy-connections=false
use-native-transport=true
enable-rcon=false
motd=A Minecraft Server
EOF

echo "Установка завершена!"
echo "Для запуска сервера выполните: ./start.sh" 
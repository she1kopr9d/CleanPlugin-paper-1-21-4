#!/bin/bash

# Путь к Java 21
JAVA_PATH="/opt/homebrew/opt/openjdk@21/bin/java"

# Проверяем наличие Java 21
if [ ! -f "$JAVA_PATH" ]; then
    echo "Java 21 не найдена. Пожалуйста, переустановите сервер."
    exit 1
fi

# Минимальное количество RAM
MIN_RAM="1G"
# Максимальное количество RAM
MAX_RAM="4G"

# Запуск сервера с оптимизированными параметрами
"$JAVA_PATH" -Xms${MIN_RAM} -Xmx${MAX_RAM} \
    -XX:+UseG1GC \
    -XX:+ParallelRefProcEnabled \
    -XX:MaxGCPauseMillis=200 \
    -XX:+UnlockExperimentalVMOptions \
    -XX:+DisableExplicitGC \
    -XX:+AlwaysPreTouch \
    -XX:G1NewSizePercent=30 \
    -XX:G1MaxNewSizePercent=40 \
    -XX:G1HeapRegionSize=8M \
    -XX:G1ReservePercent=20 \
    -XX:G1HeapWastePercent=5 \
    -XX:G1MixedGCCountTarget=4 \
    -XX:InitiatingHeapOccupancyPercent=15 \
    -XX:G1MixedGCLiveThresholdPercent=90 \
    -XX:G1RSetUpdatingPauseTimePercent=5 \
    -XX:SurvivorRatio=32 \
    -XX:+PerfDisableSharedMem \
    -XX:MaxTenuringThreshold=1 \
    -jar paper.jar nogui

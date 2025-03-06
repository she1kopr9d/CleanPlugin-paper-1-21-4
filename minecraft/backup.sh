#!/bin/bash

# Директория для бэкапов
BACKUP_DIR="backups"
WORLD_DIR="paper_server/world"

# Создаем директорию для бэкапов, если её нет
mkdir -p "$BACKUP_DIR"

# Создаем имя файла бэкапа с текущей датой
BACKUP_FILE="$BACKUP_DIR/world_$(date +%Y%m%d_%H%M%S).tar.gz"

# Создаем бэкап
tar -czf "$BACKUP_FILE" "$WORLD_DIR"

echo "Бэкап создан: $BACKUP_FILE" 
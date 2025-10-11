#!/bin/bash

set -e

# List of image names
IMAGES=(
  "c-image"
  "cpp-image"
  "javascript-image"
  "java-image"
  "rust-image"
  "python-image"
  "go-image"
  "php-image"
  "csharp-image"
  "ubuntu-novnc"
)

echo "[*] Stopping and removing containers..."
for img in "${IMAGES[@]}"; do
    CONTAINERS=$(sudo docker ps -a -q --filter "ancestor=$img")
    if [ -n "$CONTAINERS" ]; then
        sudo docker stop $CONTAINERS
        sudo docker rm $CONTAINERS
        echo "    Removed containers for $img"
    fi
done

echo "[*] Removing images..."
for img in "${IMAGES[@]}"; do
    if sudo docker images -q $img >/dev/null; then
        sudo docker rmi -f $img || true
        echo "    Removed $img"
    fi
done

echo "[*] Docker system prune (optional)..."
sudo docker system prune -f

echo "[✓] Cleanup complete."
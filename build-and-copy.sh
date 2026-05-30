#!/bin/bash

echo "===== BUILD + COPY JARS ====="

BASE_DIR="$(pwd)"
OUTPUT_DIR="$BASE_DIR/PluginsCompilados"

# ✅ criar pasta
mkdir -p "$OUTPUT_DIR"

echo ""
echo "👉 Compilar OBM-Core (install)"
cd "$BASE_DIR/OBM-Core" || exit
mvn clean install

echo ""
echo "👉 Compilar plugins em paralelo"

PLUGINS=("OBM-Inventory" "OBM-Lobby" "OBM-UHC" "OBM-SMP")

# ✅ array de processos
PIDS=()

for plugin in "${PLUGINS[@]}"
do
(
    echo "👉 [THREAD] Compilar $plugin"

    cd "$BASE_DIR/$plugin" || exit
    mvn clean package

    JAR_FILE=$(find target -name "*.jar" ! -name "*original*")

    echo "✔ Copiar $plugin"
    cp "$JAR_FILE" "$OUTPUT_DIR"

) &

PIDS+=($!) # guarda PID
done

# ✅ esperar todos
for pid in "${PIDS[@]}"
do
    wait $pid
done

echo ""
echo "👉 Copiar OBM-Core também"
CORE_JAR=$(find "$BASE_DIR/OBM-Core/target" -name "*.jar" ! -name "*original*")
cp "$CORE_JAR" "$OUTPUT_DIR"

echo ""
echo "✅ BUILD COMPLETO"

# ✅ abrir pasta (Windows via Git Bash / WSL)
echo "👉 Abrir pasta de output"
explorer.exe "$(cygpath -w "$OUTPUT_DIR")"

echo ""
echo "📂 Pasta aberta: $OUTPUT_DIR"
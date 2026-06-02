#!/bin/bash

echo "===== COPY JARS ====="

BASE_DIR="$(pwd)"
OUTPUT_DIR="$BASE_DIR/PluginsCompilados"

mkdir -p "$OUTPUT_DIR"

echo ""
echo "👉 A copiar JARs..."

MODULES=("OBM-Core" "OBM-SMP" "OBM-Lobby" "OBM-Inventory" "OBM-UHC")

for module in "${MODULES[@]}"
do
    echo "👉 $module"

    JAR_FILE=$(find "$BASE_DIR/$module/target" -name "*.jar" ! -name "*original*")

    if [ -z "$JAR_FILE" ]; then
        echo "❌ JAR não encontrado para $module (compila primeiro!)"
        continue
    fi

    echo "✔ Copiado $module"
    cp "$JAR_FILE" "$OUTPUT_DIR"
done

echo ""
echo "✅ TODOS OS JARS COPIADOS"

echo "👉 Abrir pasta"
explorer.exe "$(cygpath -w "$OUTPUT_DIR")"

echo ""
echo "📂 Pasta aberta: $OUTPUT_DIR"
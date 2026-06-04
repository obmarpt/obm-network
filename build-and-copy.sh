#!/bin/bash

PROJECT_DIR="/d/PluginsMinecraft/OMD Network"
SERVER_PLUGIN_DIR="/d/PluginsMinecraft/OMD Network/PluginsCompilados"

echo "🧹 Limpando build..."
cd "$PROJECT_DIR" || exit
mvn clean

echo "📦 Compilando TODOS os módulos..."
mvn clean install || exit

echo "🧹 Limpando pasta PluginsCompilados..."
mkdir -p "$SERVER_PLUGIN_DIR"
rm -f "$SERVER_PLUGIN_DIR"/*.jar

echo "📂 Copiando JARs..."

cp OBM-Core/target/OBM-Core-*.jar "$SERVER_PLUGIN_DIR/OBM-Core.jar"
cp OBM-Lobby/target/OBM-Lobby-*.jar "$SERVER_PLUGIN_DIR/OBM-Lobby.jar"
cp OBM-SMP/target/OBM-SMP-*.jar "$SERVER_PLUGIN_DIR/OBM-SMP.jar"
cp OBM-TierSpace/target/OBM-TierSpace-*.jar "$SERVER_PLUGIN_DIR/OBM-TierSpace.jar"
cp OBM-UHC/target/OBM-UHC-*.jar "$SERVER_PLUGIN_DIR/OBM-UHC.jar"
cp OBM-Inventory/target/OBM-Inventory-*.jar "$SERVER_PLUGIN_DIR/OBM-Inventory.jar"

echo "✅ Build completo + todos os módulos!"
echo "📦 Local: $SERVER_PLUGIN_DIR"
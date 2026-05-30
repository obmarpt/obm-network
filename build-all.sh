#!/bin/bash

echo "===== COMPILAR OBM NETWORK ====="

BASE_DIR="$(pwd)"

echo ""
echo "➡️  Compilar OBM-Core (install)"
cd "$BASE_DIR/OBM-Core" || exit
mvn clean install

echo ""
echo "➡️  Compilar OBM-Inventory"
cd "$BASE_DIR/OBM-Inventory" || exit
mvn clean package

echo ""
echo "➡️  Compilar OBM-Lobby"
cd "$BASE_DIR/OBM-Lobby" || exit
mvn clean package

echo ""
echo "➡️  Compilar OBM-UHC"
cd "$BASE_DIR/OBM-UHC" || exit
mvn clean package

echo ""
echo "➡️  Compilar OBM-SMP"
cd "$BASE_DIR/OBM-SMP" || exit
mvn clean package

echo ""
echo "✅ BUILD COMPLETO TERMINADO!"
#!/bin/bash

echo "==============================="
echo "🚀 MineSpace Build Script"
echo "==============================="

BASE_DIR="/d/PluginsMinecraft/OMD Network"
OUTPUT_DIR="$BASE_DIR/PluginsCompilados"
INFO_FILE="$BASE_DIR/build-info.txt"

cd "$BASE_DIR" || exit

echo "🔧 Running Maven build..."
mvn clean install -DskipTests

if [ $? -ne 0 ]; then
  echo "❌ BUILD FAILED"
  exit 1
fi

echo "✅ Build successful"

echo "🧹 Cleaning output folder..."
rm -f "$OUTPUT_DIR"/*.jar

echo "📦 Copying JARs..."

cp OBM-Core/target/OBM-Core-*.jar "$OUTPUT_DIR/OBM-Core.jar"
cp OBM-SMP/target/OBM-SMP-*.jar "$OUTPUT_DIR/OBM-SMP.jar"
cp OBM-TierSpace/target/OBM-TierSpace-*.jar "$OUTPUT_DIR/OBM-TierSpace.jar"
cp OBM-Lobby/target/OBM-Lobby-*.jar "$OUTPUT_DIR/OBM-Lobby.jar"
cp OBM-UHC/target/OBM-UHC-*.jar "$OUTPUT_DIR/OBM-UHC.jar"
cp OBM-Inventory/target/OBM-Inventory-*.jar "$OUTPUT_DIR/OBM-Inventory.jar"

echo "✅ JARs copied"

---

echo "📝 Generating build info..."

DATE=$(date)
GIT_BRANCH=$(git rev-parse --abbrev-ref HEAD 2>/dev/null)
GIT_COMMIT=$(git rev-parse --short HEAD 2>/dev/null)

cat <<EOF > "$INFO_FILE"
===============================
MineSpace Build Info
===============================

Date: $DATE
Branch: $GIT_BRANCH
Commit: $GIT_COMMIT

Modules:
- OBM-Core
- OBM-SMP
- OBM-TierSpace
- OBM-Lobby
- OBM-UHC
- OBM-Inventory

Status: SUCCESS ✅

JAR Location:
$OUTPUT_DIR

===============================
EOF

echo "✅ Build info created at: $INFO_FILE"

echo "🎉 DONE!"
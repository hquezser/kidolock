#!/usr/bin/env bash
# Build sans Gradle : uniquement les outils du SDK Android déjà installés.
#   ./build.sh            → build/mode-enfant.apk
#   ./build.sh install    → build + installation via adb
set -euo pipefail
cd "$(dirname "$0")"

SDK="${ANDROID_HOME:-/opt/homebrew/share/android-sdk}"
BT="$SDK/build-tools/36.0.0"
PLATFORM="$SDK/platforms/android-36/android.jar"
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17}"
export PATH="$JAVA_HOME/bin:$PATH"

# Surchargeables : VERSION_CODE=2 VERSION_NAME=0.2 ./build.sh
VERSION_CODE="${VERSION_CODE:-1}"
VERSION_NAME="${VERSION_NAME:-0.1}"

rm -rf build
mkdir -p build/gen build/classes build/dex

echo "→ ressources"
"$BT/aapt2" compile --dir res -o build/res.zip
"$BT/aapt2" link -I "$PLATFORM" --manifest AndroidManifest.xml \
    --min-sdk-version 26 --target-sdk-version 34 \
    --version-code "$VERSION_CODE" --version-name "$VERSION_NAME" \
    --java build/gen -o build/unaligned.apk build/res.zip

echo "→ java"
javac -source 8 -target 8 -bootclasspath "$PLATFORM" -classpath "$BT/core-lambda-stubs.jar" \
    -Xlint:-options -encoding UTF-8 \
    -d build/classes $(find src build/gen -name '*.java')

echo "→ dex"
"$BT/d8" --release --min-api 26 --lib "$PLATFORM" --output build/dex $(find build/classes -name '*.class')
(cd build/dex && zip -q ../unaligned.apk classes.dex)

echo "→ signature"
"$BT/zipalign" -f -p 4 build/unaligned.apk build/aligned.apk
"$BT/apksigner" sign --ks "$HOME/.android/debug.keystore" --ks-pass pass:android \
    --ks-key-alias androiddebugkey --out build/mode-enfant.apk build/aligned.apk

ls -lh build/mode-enfant.apk

if [[ "${1:-}" == "install" ]]; then
    adb install -r build/mode-enfant.apk
fi

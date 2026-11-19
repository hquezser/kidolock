#!/usr/bin/env bash
# Build via Gradle. Usage :
#   ./build.sh              → assembleDebug
#   ./build.sh install      → assembleDebug + installDebug
#   ./build.sh release      → bundleRelease (AAB signé si keystore.properties présent)
set -euo pipefail
cd "$(dirname "$0")"

case "${1:-}" in
    install) exec ./gradlew installDebug ;;
    release) exec ./gradlew bundleRelease ;;
    *) exec ./gradlew assembleDebug ;;
esac

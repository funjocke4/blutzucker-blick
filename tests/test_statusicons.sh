#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
TEMP=$(mktemp -d)
trap 'rm -rf "$TEMP"' EXIT
kotlinc android/app/src/main/java/de/blutzuckerblick/ReadingStatusIcons.kt tests/ReadingStatusIconsTest.kt -include-runtime -d "$TEMP/test.jar"
java -jar "$TEMP/test.jar"

#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
TMP=$(mktemp -d); trap 'rm -rf "$TMP"' EXIT
kotlinc android/app/src/main/java/de/blutzuckerblick/DigitMotionTracker.kt tests/DigitMotionTrackerTest.kt -include-runtime -d "$TMP/check.jar"
java -jar "$TMP/check.jar"

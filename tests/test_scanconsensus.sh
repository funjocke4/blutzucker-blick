#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
TEMP=$(mktemp -d)
trap 'rm -rf "$TEMP"' EXIT
kotlinc android/app/src/main/java/de/blutzuckerblick/ScanConsensus.kt tests/ScanConsensusTest.kt -include-runtime -d "$TEMP/tests.jar"
java -jar "$TEMP/tests.jar"

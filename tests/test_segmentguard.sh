#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT
kotlinc "$ROOT/android/app/src/main/java/de/blutzuckerblick/LcdSegmentGuard.kt" \
  "$ROOT/tests/LcdSegmentGuardTest.kt" -include-runtime -d "$WORK/check.jar"
java -jar "$WORK/check.jar"

#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
jar="$(mktemp --suffix=.jar)"
trap 'rm -f "$jar"' EXIT
kotlinc \
  "$root/android/app/src/main/java/de/blutzuckerblick/GlucoseOcrCandidates.kt" \
  "$root/tests/GlucoseOcrCandidatesTest.kt" \
  -include-runtime -d "$jar"
java -jar "$jar"

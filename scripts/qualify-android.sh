#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
printf '%s\n' 'COMMAND: bash ./gradlew testDebugUnitTest --stacktrace'
bash ./gradlew testDebugUnitTest --stacktrace
printf '%s\n' 'COMMAND: bash ./gradlew assembleDebug --stacktrace'
bash ./gradlew assembleDebug --stacktrace
printf '%s\n' 'COMMAND: bash ./gradlew lintDebug --stacktrace'
bash ./gradlew lintDebug --stacktrace
APK=app/build/outputs/apk/debug/app-debug.apk
test -s "$APK"
shasum -a 256 "$APK"

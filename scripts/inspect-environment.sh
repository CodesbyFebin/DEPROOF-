#!/usr/bin/env bash
set -uo pipefail
cd "$(dirname "$0")/.."
printf 'Observed environment (UTC): '; date -u
uname -a
df -h .
java -version
go version
node --version
printf 'SDK platforms/build tools:\n'
ls "${ANDROID_HOME:-$HOME/Library/Android/sdk}/platforms"
ls "${ANDROID_HOME:-$HOME/Library/Android/sdk}/build-tools"
printf 'ADB device query (may be blocked):\n'
adb devices -l
printf 'Docker runtime query (may be blocked):\n'
docker version
printf 'Dependency network probe (no credentials):\n'
curl -I --max-time 10 https://dl.google.com/android/repository/repository2-1.xml

#!/usr/bin/env bash
set -euo pipefail
umask 077
RUN="$(date -u +%Y%m%dT%H%M%SZ)-$$"
OUT="/export/$RUN"
mkdir -p "$OUT"
stage=initialization
trap 'code=$?; if test "$code" != 0; then printf "%s FAIL (exit %s)\n" "$stage" "$code" >> "$OUT/results.txt"; fi' EXIT
test -w /cache/gradle && test -w "$HOME" || { echo 'Container cache ownership mismatch; use UID-specific named volumes.' >&2; exit 1; }
cp -R /source/. /workspace/
cd /workspace
printf 'sdk.dir=/opt/android-sdk\n' > local.properties
java -version > "$OUT/toolchain.txt" 2>&1
uname -a >> "$OUT/toolchain.txt"
/opt/android-sdk/cmdline-tools/pinned/bin/sdkmanager --version >> "$OUT/toolchain.txt" 2>&1
result=0
for task in testDebugUnitTest lintDebug assembleDebug; do
  stage="$task"
  printf 'COMMAND: bash ./gradlew --no-daemon --max-workers=2 %s --stacktrace\n' "$task" >> "$OUT/commands.txt"
  if bash ./gradlew --no-daemon --max-workers=2 "$task" --stacktrace > "$OUT/$task.log" 2>&1; then
    printf '%s PASS\n' "$task" >> "$OUT/results.txt"
  else
    printf '%s FAIL\n' "$task" >> "$OUT/results.txt"
    result=1
    break
  fi
done
if test -d app/build/reports; then cp -R app/build/reports "$OUT/"; fi
if test -d app/build/test-results; then cp -R app/build/test-results "$OUT/"; fi
if test "$result" = 0 && test -s app/build/outputs/apk/debug/app-debug.apk; then
 cp app/build/outputs/apk/debug/app-debug.apk "$OUT/app-debug.apk"
 (cd "$OUT" && find . -type f ! -name artifact-checksums.txt -print0 | sort -z | xargs -0 sha256sum > artifact-checksums.txt)
 printf '%s\n' "$RUN" > /export/latest-successful-run.txt
else
 result=1
fi
printf 'Android container build exit: %s; artifacts: %s\n' "$result" "$OUT"
exit "$result"

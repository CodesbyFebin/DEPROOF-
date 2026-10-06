#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
SDK_LOCATION="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
test -d "$SDK_LOCATION" || { echo 'BLOCKED: set ANDROID_HOME to an installed SDK' >&2; exit 1; }
if ! test -e local.properties; then
  python3 - "$SDK_LOCATION" <<'PY'
import sys,pathlib
pathlib.Path('local.properties').write_text('sdk.dir='+sys.argv[1].replace('\\','\\\\').replace(':','\\:')+'\n')
PY
fi
printf 'SDK configured without overwriting existing local.properties.\n'
printf 'Required: JDK 17, SDK platform 37.0, build tools 36.0.0.\n'

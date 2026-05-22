#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
"$ROOT/scripts/build.sh"
CP="out"
if compgen -G "lib/*.jar" > /dev/null; then
  for jar in lib/*.jar; do
    CP="$CP:$jar"
  done
else
  echo "警告：找不到 lib/*.jar。建議改用 Maven：mvn clean compile exec:java"
  echo "若要用此腳本，請將 sqlite-jdbc jar 放入 lib/ 資料夾。"
fi
java -cp "$CP" com.gymapp.App "$@"

#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
rm -rf out
mkdir -p out
find src/main/java -name "*.java" > sources.txt
javac -encoding UTF-8 -d out @sources.txt
cp -R src/main/resources/* out/
rm sources.txt
echo "Build completed: $ROOT/out"

#!/usr/bin/env bash
# Run explicitly to commit and push; setup never invokes this script.
set -euo pipefail
project_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
cd "$project_dir"
bash scripts/maven.sh -B verify
if ! git rev-parse --git-dir >/dev/null 2>&1; then git init -b main; fi
if ! git remote get-url origin >/dev/null 2>&1; then
  git remote add origin "${1:-https://github.com/jpggames2001-web/Minecraft-java.git}"
fi
git add .gitignore pom.xml README.md LICENSE docs scripts src .github downloads
if ! git diff --cached --quiet; then git commit -m 'Implement procedural Java Skyblock game and validation'; fi
# Explicit destination supports the cloud checkout's unborn branch named work.
git push -u origin HEAD:main

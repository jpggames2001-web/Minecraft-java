#!/usr/bin/env bash
set -euo pipefail
project_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
tools_dir=$(dirname -- "$project_dir")/.minecraft-tools
cd "$project_dir"
if [[ ! -f target/skyblock-classic-0.1.0.jar ]]; then bash scripts/maven.sh -B verify; fi
java_bin=java
if [[ -x "$tools_dir/jdk/bin/java" ]]; then java_bin="$tools_dir/jdk/bin/java"; fi
exec "$java_bin" -jar target/skyblock-classic-0.1.0.jar "$@"

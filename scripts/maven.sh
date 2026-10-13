#!/usr/bin/env bash
set -euo pipefail
project_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
tools_dir=$(dirname -- "$project_dir")/.minecraft-tools
cd "$project_dir"
if [[ -x "$tools_dir/jdk/bin/javac" && -x "$tools_dir/apache-maven-3.9.9/bin/mvn" ]]; then
  export JAVA_HOME="$tools_dir/jdk"
  args=(-Dmaven.repo.local="$tools_dir/m2")
  if [[ -f "$tools_dir/maven-settings.xml" ]]; then args+=(-s "$tools_dir/maven-settings.xml"); fi
  exec "$tools_dir/apache-maven-3.9.9/bin/mvn" "${args[@]}" "$@"
fi
command -v javac >/dev/null || { echo 'Java 21 JDK is required.' >&2; exit 1; }
command -v mvn >/dev/null || { echo 'Maven 3.9+ is required.' >&2; exit 1; }
exec mvn "$@"

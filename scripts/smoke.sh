#!/usr/bin/env bash
set -euo pipefail
project_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
tools_dir=$(dirname -- "$project_dir")/.minecraft-tools
cd "$project_dir"
bash scripts/maven.sh -B verify
mkdir -p target/runtime target/mesa-cache
chmod 700 target/runtime
export XDG_RUNTIME_DIR="$project_dir/target/runtime"
export MESA_SHADER_CACHE_DIR="$project_dir/target/mesa-cache"
export LIBGL_ALWAYS_SOFTWARE=1
xvfb_bin=$(command -v Xvfb || true)
if [[ -z "$xvfb_bin" ]]; then xvfb_bin="$tools_dir/root/usr/bin/Xvfb"; fi
[[ -x "$xvfb_bin" ]] || { echo 'Xvfb required for the Linux graphical smoke check.' >&2; exit 1; }
# Xvfb allocates a free display number through a file descriptor. Only our process is stopped.
rm -f target/display-number
"$xvfb_bin" -displayfd 3 -screen 0 1280x720x24 -nolisten tcp 3>target/display-number >target/xvfb.log 2>&1 &
xvfb_pid=$!
trap 'kill "$xvfb_pid" 2>/dev/null || true; wait "$xvfb_pid" 2>/dev/null || true' EXIT
for attempt in {1..50}; do
  if [[ -s target/display-number ]]; then break; fi
  kill -0 "$xvfb_pid" 2>/dev/null || { cat target/xvfb.log >&2; exit 1; }
  sleep 0.1
done
[[ -s target/display-number ]] || { echo 'Xvfb did not become ready.' >&2; exit 1; }
export DISPLAY=":$(cat target/display-number)"
bash scripts/run.sh --smoke 120 --no-audio --screenshot target/smoke-game.png
bash scripts/run.sh --smoke 35 --no-audio --screenshot target/smoke-inventory.png
bash scripts/run.sh --smoke 120 --smoke-menus --no-audio --screenshot target/smoke-menu-final.png

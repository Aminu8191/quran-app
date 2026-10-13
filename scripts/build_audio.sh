#!/usr/bin/env bash
# usage: build_audio.sh <folder with original NNN.m4a> <folder with extra files> <output folder>
set -euo pipefail
SRC="$1"; EXTRA="$2"; OUT="$3"
mkdir -p "$OUT"

# files that are correct as they are
for f in "$SRC"/*.m4a; do
  n=$(basename "$f" .m4a)
  v=$((10#$n))
  if [ "$v" -le 75 ] || { [ "$v" -ge 81 ] && [ "$v" -le 98 ]; }; then
    cp "$f" "$OUT/$n.m4a"
  fi
done

# shifted files, renamed using audio_map.txt
while read -r dst src; do
  case "$dst" in ''|\#*) continue ;; esac
  cp "$SRC/$src.m4a" "$OUT/$dst.m4a"
done < audio_map.txt

# extra files (e.g. 049.m4a)
cp "$EXTRA"/*.m4a "$OUT"/

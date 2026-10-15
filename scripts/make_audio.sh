#!/usr/bin/env bash
# Makes ~/storage/shared/QuranFinal/NNN.opus  (Opus 16 kbps mono)
# NNN is the REAL surah number (the shift of surahs 77-115 is fixed here using audio_map.txt).
set -u
ORIG="$HOME/storage/shared/Sheikh Usman Birnin Kebbi Full Quran"
DL="$HOME/storage/shared/Download"
OLD="$HOME/storage/shared/QuranOpus16"   # optional: files already converted earlier (original numbering)
OUT="$HOME/storage/shared/QuranFinal"
MAP="$(cd "$(dirname "$0")/.." && pwd)/audio_map.txt"
mkdir -p "$OUT"

# surah number -> number of the original file that contains it
src_for() {
  local n=$((10#$1))
  if [ "$n" -le 75 ] || { [ "$n" -ge 81 ] && [ "$n" -le 98 ]; }; then
    printf '%03d' "$n"
    return
  fi
  awk -v d="$1" '$1==d {print $2}' "$MAP"
}

convert() {
  ffmpeg -y -loglevel error -nostdin -i "$1" -vn -ac 1 -ar 24000 -c:a libopus -b:a 16k -f ogg "$2.tmp" && mv "$2.tmp" "$2"
}

NO_AUDIO=" 076 086 088 089 094 099 110 "
for i in $(seq 1 115); do
  n=$(printf '%03d' "$i")
  case "$NO_AUDIO" in *" $n "*) continue ;; esac
  [ -s "$OUT/$n.opus" ] && continue
  s=$(src_for "$n")
  if [ -z "$s" ]; then echo "BABU TASWIRA: $n"; continue; fi
  if [ -s "$OLD/$s.opus" ]; then
    cp "$OLD/$s.opus" "$OUT/$n.opus" && echo "an kwafa $n"
    continue
  fi
  f=""
  if [ "$s" = "049" ]; then f=$(ls "$DL"/049*Hujurat*.m4a 2>/dev/null | head -1); fi
  if [ -z "$f" ]; then f=$(ls "$ORIG"/"$s"*.m4a "$ORIG"/"$s"*.mp3 2>/dev/null | head -1); fi
  if [ -z "$f" ]; then echo "BABU FAYIL: $s (don sura $n)"; continue; fi
  convert "$f" "$OUT/$n.opus" && echo "an gama $n"
done
echo KAMMALA
echo "fayiloli: $(ls "$OUT"/*.opus 2>/dev/null | wc -l)  (ya kamata 108)"
du -sh "$OUT"

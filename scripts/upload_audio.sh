#!/usr/bin/env bash
# Uploads ~/storage/shared/QuranFinal/*.opus to the GitHub release audio-v2 (one file per surah).
# Safe to run again and again: it only uploads what is missing.
R=Aminu8191/quran-app
T=audio-v2
DIR="$HOME/storage/shared/QuranFinal"

gh release view "$T" --repo "$R" >/dev/null 2>&1 || \
  gh release create "$T" --repo "$R" --title "Audio Opus16" --notes "Opus 16kbps mono, one file per surah (NNN.opus)"

for pass in 1 2 3 4 5 6; do
  ID=$(gh api "repos/$R/releases/tags/$T" -q .id)
  DONE=$(gh api --paginate "repos/$R/releases/$ID/assets?per_page=100" -q '.[] | select(.state=="uploaded") | .name')
  missing=0
  for f in "$DIR"/*.opus; do
    b=$(basename "$f")
    if echo "$DONE" | grep -qx "$b"; then continue; fi
    missing=$((missing+1))
    gh release delete-asset "$T" "$b" --repo "$R" -y >/dev/null 2>&1
    echo "ina lodawa $b"
    gh release upload "$T" "$f" --repo "$R" || sleep 3
  done
  echo "zagaye $pass: an yi ƙoƙarin loda $missing"
  [ "$missing" -eq 0 ] && break
done
ID=$(gh api "repos/$R/releases/tags/$T" -q .id)
echo "jimla da suka hau: $(gh api --paginate "repos/$R/releases/$ID/assets?per_page=100" -q '.[] | select(.state=="uploaded") | .name' | grep -c opus)  (ya kamata 108)"

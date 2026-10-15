# Full Quran – Sheikh Usman Birnin Kebbi (Android)

Quran audio player (Kotlin + Jetpack Compose + Media3 + AdMob).

- The APK is small. Each surah is downloaded once from the GitHub release `audio-v2`
  (`001.opus` ... `115.opus`) and then works offline.
- Build: **Actions -> Build APK -> Run workflow**. The APK appears in release `apk-latest`.
- Audio files are made with `scripts/make_audio.sh` and uploaded with `scripts/upload_audio.sh` (Termux).
- Ads: see `ADS_GUIDE.md`. Defaults are in `ads.conf`.

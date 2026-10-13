# Ads: how to change them without rebuilding

The app reads `ads.json` from this repo every time it starts:
`https://raw.githubusercontent.com/Aminu8191/quran-app/main/ads.json`

## Which unit id goes with which format

| "format"            | Full-screen ad? | Banner? | Unit ids used                                   |
|---------------------|-----------------|---------|-------------------------------------------------|
| `interstitial`      | yes             | no      | `unit_first` (launch) + `unit_return` (return)  |
| `banner`            | no              | yes     | `unit_banner`                                   |
| `app_open`          | yes (App Open)  | no      | `unit_app_open`                                 |
| `banner+interstitial` | yes           | yes     | `unit_banner` + `unit_first` + `unit_return`    |
| `none`              | no              | no      | (nothing)                                       |

`"enabled": false` turns ALL ads off whatever the format is.
The AdMob **App ID** is never remote: it lives in `ads.conf` (`admob_app_id`) and needs a rebuild to change.

## Example ads.json

```json
{
  "format": "banner",
  "enabled": true,
  "unit_first": "ca-app-pub-3940256099942544/8691691433",
  "unit_return": "ca-app-pub-3940256099942544/1033173712",
  "unit_banner": "ca-app-pub-3940256099942544/9214589741",
  "unit_app_open": "ca-app-pub-3940256099942544/9257395921",
  "return_min_gap_sec": 20,
  "first_wait_ms": 5000
}
```
(The ids above are Google's TEST ids. Replace with your own AdMob unit ids when ready.)

Rules: every unit id must look like `ca-app-pub-1234567890123456/1234567890`; bad ids and unknown formats are ignored.
Each AdMob unit must be of the right type (a banner id will not work as interstitial and vice versa).

## Edit it from your phone browser

1. Open `github.com/Aminu8191/quran-app` and sign in.
2. Tap `ads.json`.
3. Tap the pencil icon (Edit this file).
4. Change only the values (keep the quotes and commas), e.g. `"format": "interstitial"`.
5. Tap **Commit changes...** then **Commit changes** again.
6. Wait about 5 minutes (GitHub caches the raw link), then close and reopen the app.

If the JSON has a typo the app simply ignores it and keeps the last good copy.

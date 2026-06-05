# Publishing to F-Droid

> English (this file) · [العربية](../FDROID.md)

How to get **GT-TAHAKOM** listed on the free **F-Droid** store. Everything needed is
already in the repo; the one remaining action only the project owner can do is
**opening the inclusion request from their own GitLab account**.

> ✅ **Submission status:** RFP opened on 2026-06-04 — track it at
> **https://gitlab.com/fdroid/rfp/-/work_items/3972** (#3972). Now awaiting F-Droid volunteers.

> 📘 **For other projects:** this file is GT-TAHAKOM–specific; the **reusable, project-agnostic**
> guide (placeholders + a machine-actionable section) lives in
> [guides/en/FDROID_PLAYBOOK.md](guides/en/FDROID_PLAYBOOK.md).

## Why the app qualifies (verified)
- **Free license:** GPLv3 (`LICENSE`).
- **No proprietary deps or trackers:** no Google Play Services, no Firebase, no non-free
  SDK — only AndroidX/Compose/Hilt/Room/OkHttp/Coroutines (all free).
- **Builds from source without keys:** signing is conditional on a local
  `keystore.properties`; when absent (as on F-Droid's servers) it builds an unsigned
  `release` and F-Droid signs it with its own key. Proven via CI.
- **All permissions justified:** `INTERNET` + `ACCESS_NETWORK_STATE` +
  `ACCESS_WIFI_STATE` + `CHANGE_WIFI_MULTICAST_STATE` (mDNS/SSDP discovery) +
  `TRANSMIT_IR` (IR blaster).
- **No anti-features:** fully offline for core features; online search is optional and
  fetches free data from GitHub only.

## What's ready in the repo
| Item | Location |
|---|---|
| F-Droid build recipe (metadata) | [`fdroid/com.gnutux.tahakom.yml`](../../fdroid/com.gnutux.tahakom.yml) |
| Title/description/changelogs (ar + en) | `fastlane/metadata/android/{ar,en-US}/` |
| Icon + 4 screenshots | `fastlane/metadata/android/en-US/images/` |
| Tag the build is cut from | `v1.0.0` (versionCode 30) |

> **Note:** the fastlane metadata layout we use is exactly what F-Droid reads straight
> from the repo to pull the description and screenshots automatically after inclusion —
> no manual upload needed.

---

## Two ways to submit — pick one

### Route (A) — RFP: Request For Packaging (simplest, recommended first time)
F-Droid volunteers write/review the recipe for you.

1. Sign in to GitLab and open: **https://gitlab.com/fdroid/rfp/-/issues/new**
2. Choose the **`Request For Packaging`** template and paste the text below.
3. Submit, then follow the comments (a small clarification may be asked).

**Request text (paste as-is):**
```
App name: GT-TAHAKOM (تَحَكُّمْ)
Package ID: com.gnutux.tahakom
Source code: https://github.com/SalehGNUTUX/GT-TAHAKOM
License: GPL-3.0-only
Latest tag/release: v1.0.0 (versionCode 30)
Website: https://salehgnutux.github.io/GT-TAHAKOM/

Description:
A universal remote for TVs and electronics over WiFi, Infrared (IR), and a
WiFi-IR bridge. Offline-first, bilingual (Arabic/English) with RTL. Abstract
Transport layer (Roku ECP, LG webOS SSAP, Samsung Tizen, IR via Pronto, plus
experimental Android TV & Broadlink). Local IR database + on-device online
search with on-phone Pronto conversion.

FOSS check:
- No Google Play Services / Firebase / non-free SDKs.
- Builds from source with no keystore (signing is conditional; F-Droid signs).
- Permissions: INTERNET, ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE,
  CHANGE_WIFI_MULTICAST_STATE (mDNS/SSDP discovery), TRANSMIT_IR (IR blaster).
- No anti-features; fully offline for core features.

A ready F-Droid build recipe is in the repo at fdroid/com.gnutux.tahakom.yml
and fastlane metadata is under fastlane/metadata/android/.
```

### Route (B) — direct Merge Request to fdroiddata (faster, more control)
Our recipe is ready, so this is quick if you have an `fdroidserver` environment.

```bash
# 1) Fork fdroiddata on GitLab, then clone your fork
git clone https://gitlab.com/<you>/fdroiddata.git
cd fdroiddata

# 2) Drop the recipe in place (from our repo root)
cp /path/to/GT-TAHAKOM/fdroid/com.gnutux.tahakom.yml metadata/com.gnutux.tahakom.yml

# 3) Lint, then actually try the build
fdroid lint com.gnutux.tahakom
fdroid build -v -l com.gnutux.tahakom     # needs Docker / fdroidserver

# 4) Push and open a Merge Request on gitlab.com/fdroid/fdroiddata
git checkout -b add-com.gnutux.tahakom
git add metadata/com.gnutux.tahakom.yml
git commit -m "New app: GT-TAHAKOM (com.gnutux.tahakom)"
git push -u origin add-com.gnutux.tahakom
```
Then open an MR from your branch to `master` on `gitlab.com/fdroid/fdroiddata`.

---

## After acceptance
- The app appears on F-Droid in the next build cycle, signed with **F-Droid's key**
  (different from ours — so installing the F-Droid build requires uninstalling our build
  first, or vice versa).
- **Updates are automatic:** `UpdateCheckMode: Tags` + `AutoUpdateMode: Version v%v`.
  After each release just:
  1. Bump `versionCode`/`versionName` in `app/build.gradle.kts`.
  2. Add `fastlane/metadata/android/{ar,en-US}/changelogs/<versionCode>.txt`.
  3. Push a `vX.Y.Z` tag — F-Droid's bot picks it up and builds automatically.

## References
- Inclusion How-To: https://f-droid.org/docs/Inclusion_How-To/
- Build metadata reference: https://f-droid.org/docs/Build_Metadata_Reference/
- Data repo: https://gitlab.com/fdroid/fdroiddata

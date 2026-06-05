# Publishing an Android app to F-Droid — reusable playbook

> English · [العربية](../FDROID_PLAYBOOK.md)

A **project-agnostic** guide to getting an Android app (Kotlin/Java + Gradle) onto the free
**F-Droid** store, from eligibility check to acceptance and maintenance. Designed to be followed
**manually** or by an **AI model**. Replace the `<…>` placeholders with your project's values.

> Worked example of this playbook applied: [docs/FDROID.md](../../FDROID.md) and
> [fdroid/com.gnutux.tahakom.yml](../../../fdroid/com.gnutux.tahakom.yml).

## Placeholders (fill these first)
| Variable | Meaning | Example |
|---|---|---|
| `<APP_ID>` | Package id (applicationId) | `com.example.app` |
| `<REPO_URL>` | Public repo URL (HTTPS) | `https://github.com/user/app` |
| `<REPO_GIT>` | git clone URL | `https://github.com/user/app.git` |
| `<LICENSE>` | SPDX license id | `GPL-3.0-only`, `MIT`, `Apache-2.0` |
| `<TAG>` | Release tag to build from | `v1.0.0` |
| `<VNAME>` | versionName | `1.0.0` |
| `<VCODE>` | versionCode (monotonic integer) | `30` |
| `<SUBDIR>` | App module dir | `app` |
| `<GL_USER>` | Your GitLab username | `username` |

---

## Stage 0 — Eligibility (check before anything)
F-Droid accepts **free software only** that **builds from source** on its servers. Confirm **all**:

- [ ] **Approved free license** (OSI/FSF) with a `LICENSE` file; note the SPDX id (`<LICENSE>`).
- [ ] **No proprietary deps/libraries:** no Google Play Services, no Firebase/Crashlytics/Analytics,
      no closed-source SDK. (These cause rejection or an "anti-feature" tag.)
- [ ] **No trackers** and no ads. (Check Exodus Privacy if unsure.)
- [ ] **Builds from source with no secrets:** the build must succeed **without** any keystore/keys (Stage 2).
- [ ] **No prebuilt blobs:** no bundled `.jar`/`.aar`/`.so` without source; all deps from public free Maven repos.
- [ ] **Permissions justified** and clearly explained.
- [ ] **versionCode is a monotonic integer** that increases every release.

> If a non-free part exists (e.g. a GMS flavor), create a **fully-free flavor** and have F-Droid
> build it, or remove the dependency. Otherwise it's rejected or tagged with an Anti-Feature
> (e.g. `NonFreeDep`/`NonFreeNet`).

---

## Stage 1 — Prepare store metadata (fastlane)
F-Droid reads the description and screenshots **automatically** from a fastlane tree in your repo
(if present). Create it:

```
fastlane/metadata/android/
├── en-US/
│   ├── title.txt                 # app name (one line)
│   ├── short_description.txt      # short description (≤ 80 chars preferred)
│   ├── full_description.txt       # full description (light Markdown)
│   ├── changelogs/
│   │   └── <VCODE>.txt            # changelog for this release (filename = versionCode)
│   └── images/
│       ├── icon.png              # 512×512 icon
│       └── phoneScreenshots/
│           ├── 1.png
│           └── 2.png
└── <lang>/                       # extra languages (e.g. ar), same structure (optional)
```
- The changelog filename **is the `<VCODE>` number** (not the version name).
- `en-US` is the fallback; other languages inherit what they're missing.
- Screenshots are **optional** but greatly improve the listing.

---

## Stage 2 — Prepare the build config (most important)
Goal: `assembleRelease` succeeds **with no keys**, so F-Droid builds it and signs with its own key.

**1) Make signing conditional** on a local (gitignored) keystore file:
```kotlin
// app/build.gradle.kts
val keystorePropsFile = rootProject.file("keystore.properties")
android {
    signingConfigs {
        if (keystorePropsFile.exists()) {
            create("release") { /* reads from keystore.properties */ }
        }
    }
    buildTypes {
        release {
            // sign only if the file exists; otherwise leave unsigned (F-Droid signs).
            if (keystorePropsFile.exists()) signingConfig = signingConfigs.getByName("release")
        }
    }
}
```
**2) Gitignore secrets** — `.gitignore`:
```
keystore.properties
*.keystore
*.jks
```
**3) No non-free Maven repos** and no `jcenter()`; use only `google()` and `mavenCentral()`.
**4) Verify locally** that the build works without keys:
```bash
rm -f keystore.properties && ./gradlew clean assembleRelease
```

---

## Stage 3 — Write the F-Droid recipe (metadata file)
A single file named `<APP_ID>.yml` (placed under `metadata/` in the fdroiddata repo). Keep a copy
in your project at `fdroid/<APP_ID>.yml`:

```yaml
Categories:
  - Connectivity            # or System, Internet, Multimedia, Games...
License: <LICENSE>          # SPDX id
AuthorName: <your name>
AuthorEmail: <your email>
WebSite: <REPO_URL>
SourceCode: <REPO_URL>
IssueTracker: <REPO_URL>/issues
Changelog: <REPO_URL>/blob/HEAD/CHANGELOG.md

AutoName: <app name>

RepoType: git
Repo: <REPO_GIT>

Builds:
  - versionName: <VNAME>
    versionCode: <VCODE>
    commit: <TAG>           # tag or hash to build from (a signed tag is preferred)
    subdir: <SUBDIR>
    gradle:
      - yes                 # default flavor; or put the free flavor's name

AutoUpdateMode: Version v%v # on a new vX.Y.Z tag, auto-creates a build (%v=name, %c=code)
UpdateCheckMode: Tags       # watches git tags; alternatives: Tags <regex>, RepoManifest, HTTP, None
CurrentVersion: <VNAME>
CurrentVersionCode: <VCODE>
```
- **`AutoUpdateMode` + `UpdateCheckMode: Tags`** = lifelong automatic updates: just push a new tag.
- If `commit` has no `v` prefix, use `AutoUpdateMode: Version %v`.
- Useful fields when needed: `AntiFeatures: [NonFreeNet]`, `MaintainerNotes`, and inside a build
  entry: `scandelete`/`scanignore` (to exclude files the scanner rejects), `rm`, `prebuild`.

---

## Stage 4 — Local verification (optional, speeds acceptance)
Requires `fdroidserver` (and sometimes Docker). If unavailable, skip to Stage 5 (RFP route).
```bash
# inside a clone of fdroiddata:
fdroid rewritemeta <APP_ID>        # normalize the recipe formatting
fdroid lint <APP_ID>               # check fields and syntax
fdroid build -v -l <APP_ID>        # actual build (needs Docker/fdroidserver)
fdroid checkupdates <APP_ID>       # simulate tag-based update detection
```

---

## Stage 5 — Submit (pick a route)

### Route (A) — RFP: Request For Packaging (simplest, recommended first time)
F-Droid volunteers write/review the recipe. Open an issue at
**https://gitlab.com/fdroid/rfp/-/issues/new** and fill the "Request For Packaging" template. Ready text:
```
App name: <app name>
Package ID: <APP_ID>
Source code: <REPO_URL>
License: <LICENSE>
Latest tag/release: <TAG> (versionCode <VCODE>)

Description:
<brief functional description>

FOSS check:
- No Google Play Services / Firebase / non-free SDKs.
- Builds from source with no keystore (signing is conditional; F-Droid signs).
- Permissions: <list each permission and why>.
- No anti-features.

A build recipe is ready at fdroid/<APP_ID>.yml and fastlane metadata under fastlane/metadata/android/.
```

**Open the RFP via `glab`** (after `glab auth login`):
```bash
glab issue create --repo fdroid/rfp \
  --title "<app name> (<APP_ID>)" \
  --description "$(cat rfp_body.txt)"
```
**Or a pre-filled URL** (no tooling): use `?issue[title]=…&issue[description]=…` on
`…/rfp/-/issues/new` after URL-encoding.

### Route (B) — direct Merge Request to fdroiddata (faster, more control)
```bash
# 1) fork the data repo to your account
glab repo fork fdroid/fdroiddata --remote=false
# 2) shallow clone (fdroiddata is huge)
git clone --depth 1 https://gitlab.com/<GL_USER>/fdroiddata.git && cd fdroiddata
# 3) drop the recipe in place
cp /path/to/<APP_ID>.yml metadata/<APP_ID>.yml
# 4) (if available) verify
fdroid lint <APP_ID>
# 5) branch + commit + push
git checkout -b add-<APP_ID>
git add metadata/<APP_ID>.yml
git commit -m "New app: <app name> (<APP_ID>)"
git push -u origin add-<APP_ID>
# 6) open an MR to master on fdroid/fdroiddata
glab mr create --repo fdroid/fdroiddata \
  --source-branch "<GL_USER>:add-<APP_ID>" --target-branch master \
  --title "New app: <app name> (<APP_ID>)" \
  --description "FOSS, builds from source. Closes fdroid/rfp#<RFP_ID> (if any)." --yes
```

---

## Stage 6 — After acceptance & maintenance
- The app builds and appears in the index in the next build cycle, **signed with F-Droid's key**
  (different from yours → installing the F-Droid build requires uninstalling your signed build,
  and vice versa).
- **Updates are automatic** with `UpdateCheckMode: Tags`. For each new release:
  1. Bump `versionCode`/`versionName`.
  2. Add `fastlane/metadata/android/<lang>/changelogs/<VCODE>.txt`.
  3. Push a `v<VNAME>` tag → F-Droid's bot picks it up and builds automatically.
- (Advanced/optional) **Reproducible builds** + `AllowedAPKSigningKeys` to match your signature to
  F-Droid's (allows updating between the two without uninstalling).

---

## Common rejection reasons (avoid these)
- A non-free dependency/service (GMS, closed Google Maps, non-free fonts/assets) → remove or free flavor.
- Build fails because it relies on a keystore/secrets or on the dead jcenter.
- Prebuilt blobs without source (the scanner flags them) → `scandelete`/build from source.
- `versionCode` not increasing, or the tag missing at `commit`.
- A non-free network service as a core function → at least a `NonFreeNet` tag.

---

## Machine-actionable quick reference (for an AI model)
Execute in order; stop at the first failure and request human input:
1. **Check eligibility** (Stage 0). If any non-free part → report and stop.
2. **Verify keyless build:** `rm -f keystore.properties && ./gradlew clean assembleRelease`.
   Failure → fix conditional signing (Stage 2) before continuing.
3. **Extract** `<APP_ID>`, `<VNAME>`, `<VCODE>` from `build.gradle(.kts)`, `<LICENSE>` from `LICENSE`,
   and `<TAG>` from `git tag --list 'v*' | tail -1`.
4. **Create** `fdroid/<APP_ID>.yml` (Stage 3) and (if missing) the fastlane tree (Stage 1).
5. **Submission is human-authorized:** opening the RFP/MR needs the user's GitLab account — **do not
   do it without explicit permission**. Prepare the ready RFP text and `glab` command and present
   them, or generate the pre-filled URL.
6. **After acceptance:** remind that the update mechanism = push a new tag + a changelog file named `<VCODE>`.

## Official links
- Inclusion How-To: https://f-droid.org/docs/Inclusion_How-To/
- Build metadata reference: https://f-droid.org/docs/Build_Metadata_Reference/
- Data repo: https://gitlab.com/fdroid/fdroiddata
- Requests for packaging (RFP): https://gitlab.com/fdroid/rfp
- fdroidserver tools: https://f-droid.org/docs/Installing_the_Server_and_Repo_Tools/

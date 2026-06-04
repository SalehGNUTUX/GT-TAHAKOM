# Field test notes — GT-TAHAKOM

> English · [العربية](../TEST_NOTES.md)

A log of real-device user tests, to build a full picture before each release cycle.
Each entry: device + transport + what happened + analysis + next-release actions.

> **Why here?** The experimental transports (Android TV, Broadlink) are unverified on
> hardware — field tests are our only way to tune them. See "Remaining" in [STATUS.md](STATUS.md).

---

## Quick index
| # | Device | Transport | Version | Result | State |
|---|---|---|---|---|---|
| 1 | SENIC (Android receiver) | AndroidTvTransport (experimental) | 1.0.0 | Code-entry field appeared, but no code shown on the TV | Under analysis |
| 2 | LG (webOS) @192.168.1.17 | WebosTransport (SSAP) | 1.0.0 | Basic control works; apps/smart-menu and navigation/touchpad don't; general scan misses it | Diagnosed (two app bugs) |
| 3 | General (UX test) | Add-from-scan + list ordering | 1.0.0 | Add-from-scan didn't open the remote; newest not shown on top | ✅ Fixed |

---

## #1 — SENIC (Android-based receiver) · AndroidTvTransport · v1.0.0

**Date:** 2026-06-04
**Device:** SENIC — id/MAC: `C7:90:32:4A:B4:00`
**Transport:** WiFi → `AndroidTvTransport` (tagged "experimental").
**Screen context during the test:** the TV was playing a **fullscreen YouTube video**.

### Observation
After tapping "Pair", the **PIN entry field appeared in the app**, but **no code showed on
the TV screen**, so pairing could not be completed.

![App screenshot during the SENIC pairing attempt](../../screenshots/test/01-senic-androidtv-v1.0.0.png)

### Analysis (from reading the code)
- In `AndroidTvPairViewModel.kt`, the code-entry field (`PairStage.ENTER_CODE`) **only appears
  if `AndroidTvPairing.start()` returns `true`**.
- And `start()` (in `AndroidTvPairing.kt`) returns `true` only after: a successful TLS handshake
  on port **6467** + exchanging the three polo messages (request → option → **configuration**)
  and reading a non-null reply to each.
- **Conclusion:** the field appearing proves the connection reached the step where the TV is
  supposed to display the code. So the problem is not reaching the device.

**Two likely causes (not mutually exclusive):**
1. **No response-status check:** `start()` only checks the reply is **non-null** (`AtvFrames.read
   != null`); it does **not** verify `status == 200`. If the device replied with an **error** to
   the configuration message (our polo field numbers are from the spec, unverified on hardware),
   the code treats it as "success" and jumps to code entry **without the TV ever being asked to
   show a code**. ← **Most likely; fully explains the symptom.**
2. **Immersive video hides the system overlay:** the pairing code is drawn by the Android system
   service above apps; some systems (especially generic Android boxes, not certified Google TV)
   may not draw the system dialog over a fullscreen/immersive video. ← Plausible; cheapest to rule out.

**Helper notes:**
- The MAC starts with `C7` (locally-administered bit set) → a "locally administered/random"
  address, common for privacy on modern Android — **not diagnostic** of device class by itself.
- An "Android-based receiver" may be **plain Android (AOSP)** rather than **certified Android
  TV/Google TV**; the former may lack the "Remote Service" (Remote v2) that shows the code even
  though it answered on 6467.

### Next-release actions
- [ ] **Verify the response status** in `start()` at each polo step (`status == 200`) instead of
      just null-checking, so the code field only appears once the device truly accepts configuration.
- [ ] **Log raw response bytes/status** at each step (request/option/configuration) for diagnosis.
- [ ] **Retest from the device home screen** (exit YouTube) to rule out immersive-overlay suppression.
- [ ] **Confirm device class:** does SENIC pair via Google's official "Google TV"/"Android TV
      Remote" app? If a code shows there → the fault is in our impl (polo field numbers/sequence);
      if not → the device doesn't support Remote v2 and this path doesn't apply.
- [ ] Surface a clear error code distinguishing "device rejected configuration" from "timed out
      waiting for input".

### Related files
- `core/transport/impl/androidtv/AndroidTvPairing.kt` (polo sequence + port 6467)
- `core/transport/impl/androidtv/AndroidTvCrypto.kt` (TLS + secret computation)
- `feature/androidtv/AndroidTvPairViewModel.kt` (pairing stages)

---

## #2 — LG (webOS) · WebosTransport (SSAP) · v1.0.0

**Date:** 2026-06-04
**Device:** LG TV running webOS — address `192.168.1.17`.
**Transport:** WiFi → `WebosTransport` (SSAP, WebSocket on port 3000).

### Observations (three)
1. **Discovery:** "Search for nearby devices" (general scan/radar) **does not find the TV**;
   but "Add by name/model → network → LG (webOS)" **does** (LG @192.168.1.17).
2. **Pairing:** after adding, the accept prompt appeared with "No" as default; moving to "Yes"
   and tapping, then it returning to "No", repeated once or twice, then disappeared and control
   became possible.
3. **Partial control:** **power + volume + channel change + media play/pause** work; but
   **apps/smart-menu/home/settings** and **navigation (D-pad) and the touchpad** don't work at all.

### Analysis (from reading the code) — two confirmed app bugs

**Pairing succeeded** — the proof is that any control works at all means the TV accepted the
registration and a `client-key` was saved in `WebosKeyStore`. The repeated "No/Yes" is the TV's
accept prompt (its default is decline, so you move to "Yes"); the repetition comes from the
**"one WebSocket session per command"** model: each attempt before the key is saved opens a new
session and re-triggers the prompt. So the problem is **not pairing and not the TV**.

**Bug (A) — app-launch commands are built wrong.** In `WebosTransport.toSsap()`, the commands
that work are **parameterless**: `ssap://system/turnOff`, `ssap://audio/volumeUp`,
`ssap://tv/channelUp`, `ssap://media.controls/play` — that's why they **work**. But
apps/smart/home/settings are built with a query string:
`ssap://com.webos.applicationManager/launch?id=...`. **SSAP ignores `?id=`**; the id must be sent
in a separate JSON `payload` field, not in the URI. And `send()` only sends `uri` **with no
`payload`** → every launch command fails. ← Fully explains the apps failure.
- **Fix:** send `{"type":"request","uri":"ssap://system.launcher/launch","payload":{"id":"<appId>"}}`.
  Common ids: YouTube `youtube.leanback.v4`, Netflix `netflix`; home via the home button not
  launch; app list via `ssap://com.webos.applicationManager/listLaunchPoints` or open
  `com.webos.app.discovery`.

**Bug (B) — the pointer socket doesn't deliver (navigation + touchpad).** All `NAV_*`/`OK` keys
and the **touchpad** go through `sendPointer()` (a second socket after `getPointerInputSocket`),
not through `runSession`. In `sendPointer`, the moment the second socket opens we send
`type:button…` then **complete `done` immediately, so both sockets are closed right away**
(`pointerWs.close()` + `main.close()`) — the frame may be dropped before delivery. Also the
session-per-press model reopens a WebSocket+register for every drag step (far too slow for a
touchpad). ← Explains "navigation/touchpad don't work".
- **Fix:** a **persistent WebSocket connection** (open once, keep the pointer socket open, send
  through it); delay the close until send is confirmed; and add `type:move` support for the
  touchpad instead of converting drags into NAV steps.

**Bug (C, secondary) — the general scan misses webOS.** The general scan broadcasts M-SEARCH once
with an `MX:2` window (`SsdpDiscovery`) and includes ST `urn:lge-com:service:webos-second-screen:1`;
yet it didn't find the TV while the targeted path did. Likely causes: short/single broadcast,
a lost UDP reply, or the scan stopping early. ← Needs investigation (see actions).

### Next-release actions
- [ ] **(A)** Rebuild launch commands via JSON `payload`, not a query string (apps/smart/settings/home).
- [ ] **(B)** A **persistent** WebSocket connection + keep the pointer socket open + delay close
      after send; add `type:move` (and maybe `click`) for the touchpad instead of NAV steps.
- [ ] **A "re-pair / forget key" UI** (delete from `WebosKeyStore`) — currently missing.
- [ ] **(C)** Improve general webOS discovery: repeat M-SEARCH + longer window; compare to the targeted path.
- [ ] Expand the list of common app ids (YouTube/Netflix/browser…) + fetch the actually-installed list.

### Screenshots
![Control screen — LG webOS](../../screenshots/test/02-lg-webos-remote-v1.0.0.png)
![General scan finds nothing](../../screenshots/test/02-lg-webos-scan-empty-v1.0.0.png)
![Targeted path finds LG @192.168.1.17](../../screenshots/test/02-lg-webos-targeted-found-v1.0.0.png)

### Related files
- `core/transport/impl/WebosTransport.kt` — `toSsap()` (bug A), `sendPointer()` (bug B),
  `runSession()`/`registerPayload()` (pairing).
- `core/store/WebosKeyStore.kt` — stores `client-key` (no delete UI yet).
- `core/discovery/SsdpDiscovery.kt` — ST queries (bug C).
- `feature/remote/RemoteScreen.kt` — `Touchpad` (converts drags into NAV_* steps).

---

## #3 — UX: add-from-scan + "My devices" ordering · v1.0.0 · ✅ Fixed

**Date:** 2026-06-04

### Two observations
1. **Add-from-scan doesn't open the remote:** in "Search for devices on the network", finding a
   device and adding it **stayed on the search page** instead of going straight to the device's
   remote control.
2. **Ordering:** newly-added devices should appear at the **top** of "My devices".

### Cause (from reading the code)
1. Every other add path (manual/network/online/IR/import) calls `adopt()` in `MainActivity` =
   **save + open `Screen.Remote`**; the scan path was `onAdopt = { devicesVm.save(...) }` which
   **only saves** (with an intentional "keep the page to add more" comment). Inconsistent with the rest.
2. `SavedDevicesRepository.add()` appended to the **end**: `current + device`.

### Fix (applied in this build)
1. `MainActivity`: the scan path is now `onAdopt = { adopt(it.toDevice()) }` → opens the remote
   directly like every other path.
2. `SavedDevicesRepository.add()`: now prepends → `listOf(device) + current.filterNot {…}`
   (newest first, for all add paths). Manual drag-reorder is unaffected.

### Related files
- `MainActivity.kt` — `adopt()` and the `ScanScreen.onAdopt` wiring.
- `core/store/SavedDevicesRepository.kt` — `add()` (head instead of tail).
- `feature/devices/ScanScreen.kt` — `onAdopt` is invoked on card/"+" tap.

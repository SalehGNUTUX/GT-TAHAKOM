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

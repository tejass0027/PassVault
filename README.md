# PassVault

A local, offline Android password manager. Everything is stored only on your phone,
encrypted, protected by fingerprint/face + a pattern lock, with security questions as a
recovery option if you ever forget your pattern or move to a new phone.

## How to open and run it

This project was generated without access to Android Studio/Gradle/Java, so it hasn't been
compiled yet. To build and run it:

1. Install **Android Studio** (this includes the Android SDK and a bundled JDK).
2. Open Android Studio → **Open** → select this `password` folder.
3. Let Gradle sync finish (it will download dependencies automatically the first time -
   needs an internet connection just for this step; the app itself never uses the internet).
4. If Android Studio complains about a missing Gradle wrapper jar, click **"Trust and sync
   project"** / let it re-download the wrapper - this is expected since the binary wrapper
   jar wasn't generated here. Alternatively, if you have Gradle installed separately, run
   `gradle wrapper --gradle-version 8.7` once inside this folder.
5. Connect an Android phone (USB debugging enabled) or start an emulator, then click **Run ▶**.

If the first build fails with a compile error, that's expected risk of generating a project
without a compiler in the loop - copy the exact error text back and it can be fixed directly.

## Manual test checklist

1. First launch → Welcome screen → draw a pattern (4+ dots) → confirm it → set 3 security
   questions and answers → optionally enable fingerprint/face unlock.
2. App lands on the vault list, unlocked.
3. Tap the lock icon (top-left) → app returns to the login screen.
4. Log back in: fingerprint/face prompt (if enabled) → draw your pattern → back in the vault.
5. Add a password entry (title/username/password required); try the dice icon to generate one.
6. Open the entry → reveal/hide the password → copy it → check it auto-clears from the
   clipboard after 30 seconds.
7. Background the app (Home button), wait past your auto-lock setting, reopen → should be
   locked again.
8. Settings → Export encrypted backup → set a backup password → save the file somewhere.
9. Settings → Erase all data → confirm → back at the Welcome screen with nothing left.
10. Onboard again, then Settings → Import backup → enter the backup password → pick the
    file you saved → confirm replace → your entries are back.
11. From the login screen, tap "Forgot pattern?" → answer your 3 security questions → draw
    and confirm a new pattern → back in the vault with the same data.
12. On the login screen, draw a wrong pattern on purpose a couple of times, then log in
    correctly → a red banner appears on the vault list ("Someone tried to open PassVault
    2 times") → tap "View activity" (or Settings → Login activity) → see the timestamped
    list of incorrect and successful attempts.
13. From the vault list, tap the photo icon (top bar) → tap the add-photo icon → pick an
    image from your gallery → it appears in the grid → tap it → confirm it opens full-size
    → delete it and confirm it disappears from the grid.

## Security model

- **Data Encryption Key (DEK)**: one random 256-bit key generated once, used to encrypt the
  vault (AES-256-GCM). It's never derived from your pattern or biometrics directly.
- **Two wrapped copies of the DEK** are kept: one wrapped under a key derived from your
  pattern (PBKDF2-HMAC-SHA256, salted, 210,000 iterations), one wrapped under a key derived
  the same way from your security question answers. Either path unlocks the same vault.
- **Biometric unlock is a device-level gate**, not a key source - Android can't reliably turn
  a fingerprint/face scan into a stable secret, so it's used to gate the pattern step rather
  than to derive the encryption key.
- **At rest**: a single `vault.dat` file in app-private storage, AES-GCM encrypted. Metadata
  (salts, wrapped DEK copies, security question text) lives in `EncryptedSharedPreferences`,
  itself backed by an Android Keystore hardware key.
- **No network access at all** - the app has no INTERNET permission, and Android's automatic
  cloud backup is disabled (`allowBackup="false"`) so none of this ever leaves the device
  except via the manual, password-protected export you trigger yourself.
- Password fields use `FLAG_SECURE` (blocks screenshots/recent-apps thumbnail), and copied
  passwords auto-clear from the clipboard after 30 seconds.
- **Login activity**: every pattern, biometric, and recovery attempt (success or failure) is
  timestamped and kept in `EncryptedSharedPreferences` (last 50). If any failed attempts
  happened before you last logged in successfully, the vault list surfaces a banner ("Someone
  tried to open PassVault N times") the first time you open it afterward; the full history is
  under Settings → Login activity.
- **Photo vault**: a separate section (photo icon in the vault list's top bar) for storing
  photos unrelated to any specific password - e.g. a picture of a physical key, an ID, or
  backup codes. Each photo is encrypted individually with the same DEK as the passwords (a
  small metadata index lists them; the actual image bytes live in their own encrypted file
  each, so adding/removing one photo never touches the others). Photos are picked using
  Android's built-in Photo Picker, which needs no storage/gallery permission at all - keeping
  the app's zero-extra-permissions design intact.

## Project layout

- `crypto/` - AES-GCM helpers and PBKDF2 key derivation.
- `auth/` - pattern, biometric, and security-question managers; encrypted prefs storage.
- `data/` - the `Credential`/`VaultPhoto` models, the encrypted vault and photo stores, and backup export/import.
- `ui/` - Jetpack Compose screens, grouped by onboarding / login / vault / settings.
- `navigation/NavGraph.kt` - screen routing and the auto-lock logic.

# PassVault

A local, offline Android password manager. Everything is stored only on your phone,
encrypted, protected by fingerprint/face + a pattern lock, with security questions as a
recovery option if you ever forget your pattern or move to a new phone.

## What it does

- Stores your passwords (title, username, password, URL, notes) in an encrypted vault, unlocked
  by drawing your pattern (or fingerprint/face, if you enable it).
- Generates strong random passwords and shows a strength meter while you type your own.
- Copies a password to the clipboard and auto-clears it after 30 seconds.
- Keeps a separate encrypted photo vault for pictures you want stored the same secure way,
  picked straight from your gallery.
- Logs login activity, including failed attempts, and shows a banner if someone tried to get in.
- Lets you export/import an encrypted backup file (`.pvbk`) - the same format the Windows app
  uses, so you can move your passwords between your phone and PC.
- Light/Dark/System theme toggle.
- No network access at all - nothing leaves your phone except a backup file you explicitly export.

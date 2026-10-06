# Privacy and retention

No Deproof account or custom server is required. Wallet transaction keys stay in the wallet. Auth tokens remain only in adapter memory; no logs, backup or export include them. App backup is disabled. Private files, task notes and receipts remain in app-private storage until deleted or explicitly exported.

RPC providers receive observed addresses, signatures and user-approved devnet transaction bytes. They can correlate requests and IP addresses. Camera launches require an explicit user action; system camera controls its permissions. Cancelled capture creates no photo record. The app requests INTERNET only; it does not collect location, request wallet seeds, upload evidence or call an AI provider. Optional AI/cloud backup are not configured or implemented.

Receipt export includes public verification material and user-selected notes/metadata. Exported notes can be sensitive; sharing remains the user's explicit OS file-picker action. Portable files contain stable IDs and digests, not private absolute paths. Deleting app records cannot remove already exported copies. Evidence private keys cannot be exported/recovered; old signatures remain verifiable with public keys after device loss.

Tasks/receipt/file deletion controls exist in source but require device testing. Automatic retention policy, orphan-file cleanup, encrypted backup and complete process-death capture recovery are unfinished. Uninstalling clears local app data; back up intentional portable records first. This is not wallet recovery guidance or a service promising transaction reversal.

Node identities use software files with restrictive permissions. Sharing defaults off; explicit fixed-endpoint consent is required. Qualification transfers contain generated zero bytes only. Node logs/usage receipts reveal endpoints, timing and local counters. Public export excludes private node keys. No external reward system, witness export, relay, location or mobile-data sharing is enabled. Restart revokes sessions/disables consent; stops do not reset reserved usage.

The independent tools/bundle.py now provides explicit portable bundle export and AES-256-GCM encrypted backups. Private file inclusion requires a separate --include-private-files flag. The backup key is a fresh owner-controlled 32-byte key in a 0600 file; retain a separate offline copy. It is not a wallet or hardware evidence private key, and hardware evidence private keys are never packaged. Lost backup keys cannot be recovered. Decrypt authenticates ciphertext and independently verifies preserved raw receipt/file material before writing. Android backup UI integration remains incomplete.

## Node-control credentials and crash records

Pairing codes are single-use and remain in memory. Node-control seeds are AES-GCM encrypted under a separate Android Keystore key. Automatic cloud/device-transfer backup excludes application files, databases and preferences. Public node fingerprints and exact signed contribution receipts can be exported deliberately; encrypted seeds and pairing codes are excluded. Recovery journals retain uncertain operation identities without replaying network effects. Local sample proof jobs use public inputs and are not private evidence upload.

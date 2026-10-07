# Deproof — Android Permission Explanations

## Declared Permissions

| Permission | Reason |
|------------|--------|
| `INTERNET` | Required for node-agent communication and Solana RPC queries (when live RPC is enabled) |
| `ACCESS_FINE_LOCATION` | Required for location-based DePIN proof observations (e.g. GEODNET, Hivemapper adapters) |
| `ACCESS_COARSE_LOCATION` | Fallback location for proof observations |
| `FOREGROUND_SERVICE` | Node monitoring service runs in the foreground to maintain observation continuity |
| `POST_NOTIFICATIONS` | Required (Android 13+) to display node status and payment review notifications |
| `CAMERA` | Used for visual proof capture (Hivemapper adapter) — requested on demand |
| `READ_EXTERNAL_STORAGE` | Required on Android 9–12 for exporting portable receipt files |
| `WRITE_EXTERNAL_STORAGE` | Required on Android 9–9 for exporting portable receipt files |

## Wallet-Related Behavior

Deproof uses the Solana Mobile Wallet Adapter (MWA) protocol to launch an installed wallet app for transaction signing. It does not access wallet keys directly. The wallet app (Phantom, Solflare, etc.) handles all key operations.

## No Network Access to Keys

Deproof does not transmit signing keys or wallet credentials over the network. All transaction construction occurs locally; only the finalized unsigned transaction is passed to the wallet app for signing.

## Data Retention

- Contribution receipts are stored in the app's local database (Room)
- No analytics, crash reporting, or usage data is collected
- No data is shared with third parties

## Reviewer Notes

- Location permission prompt occurs the first time a location-based proof observation is requested
- Camera permission prompt occurs the first time a visual proof capture is initiated
- Wallet connection occurs on user tap; no background wallet access

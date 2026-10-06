# Local workflow contracts

Deproof / DEPR — Built by CodesbyFebin. These implementations do not establish device qualification, provider integration, physical presence or production readiness.

## F058 — optional consented location

Tasks/Evidence offers an unchecked opt-in and a separate collection button. Only that action requests foreground approximate location permission. There is no fine/background location permission or continuous tracking. AndroidX LocationManagerCompat performs a cancellable single observation with a 30-second timeout. Permission denial/revocation, disabled providers, absent/offline result, future/stale timestamps and malformed coordinates refuse observation. Provider elapsed time must be within 120 seconds; wall-clock discrepancies fail closed. Network or GPS availability is not assumed. GPS-only devices without a provider usable under approximate permission may be unavailable.

Changing tasks, leaving the component, cancelling or disabling consent cancels the request and clears the pending location. Generation checks reject late callbacks. App restart does not restore consent or start collection. Mock observations are explicitly labeled; they do not prove presence. Before creating a manifest, permission is checked again. Export is explicit; no private location goes to RPC, AI, the node or website.

The optional `location` extension to deproof-evidence-v2 contains decimal-string latitude/longitude/accuracy, provider, observation/collection UTC times, permission precision and a mock boolean. There is no inferred physical-truth field, altitude, device ID or absolute path. Legacy v2 manifests without the extension remain byte-identical and verifiable; older strict verifiers may refuse the new extension rather than silently ignore it. Both verifiers validate provenance/ranges and hash the entire manifest. Golden fixture: `fixtures/location-golden.json` is synthetic. Disabling collection does not erase already saved receipts; use explicit receipt deletion/export controls.

## F105 — mapping task planner

Within a persisted task, enter an ordered list `label|latitude|longitude` (one per line), bounded to 100 points, 200-character labels/title and valid coordinate ranges. Persisted private `mapping:<taskId>` drafts survive database reopening. A save replaces that task's plan atomically; invalid edits do not replace a valid plan. Explicit export emits canonical deproof-mapping-plan-v1 JSON with task linkage and ordered points. Exported coordinates are private content; share only deliberately.

This is a user-entered offline plan. It does not fetch map tiles, route-provider coverage, claim completed travel, compute rewards or integrate Hivemapper. Manual device editing/reopening/export and accessibility remain NOT_RUN.

## F115 — local reminders and preferences

Reminders require explicit per-task consent, a future UTC time within one year and Android notification availability. A stable per-task ID and fresh generation replace previous schedules; queued old-generation broadcasts refuse delivery. Persist the record before arming an inexact AlarmManager alarm. No exact-alarm permission is requested. Global default is disabled; scheduling explicitly enables local reminders, and Disable all cancels schedules and posted reminder notifications. Cancel task removes only that task's notification/alarm. Lock-screen text is generic and excludes task titles, notes, coordinates and evidence.

PENDING/SCHEDULED are local state, not delivery proof. POST_REQUESTED is persisted before asking Android to notify and is never replayed automatically after a crash. PERMISSION_DENIED, SCHEDULE_FAILED, EXPIRED and CANCELLED are distinct. Boot, package replacement and clock/timezone broadcasts recover future schedules; startup also reconciles them. Past missed reminders expire rather than implying delivery. Force-stop suppresses Android broadcasts until relaunch. A process-wide mutex orders scheduling, cancellation and delivery; Room writes remain transactional. Restart and physical notification delivery are NOT_RUN. Reminders never authorize a protocol claim or transaction; E010 still needs current protocol data and external acceptance.

## F117 — localization

English and Spanish UI catalogs have matching resource IDs, positional format contracts and singular/plural waypoint forms. The persisted DataStore language preference changes Compose resources; initial selection follows Spanish system language where applicable. Private content, canonical bytes, protocol codes, addresses, exact counters and signed observations are not rewritten or translated. Notification labels follow the selected language. Original UTC records remain exact.

This is an initial locale support decision: the blueprint specifies complete translation coverage but does not name locales. Other languages, independent Spanish linguistic/security review, domain-generated decoder/account-effect explanation localization, remaining fallback text, RTL/large-font/screen-reader/device locale/restart acceptance remain unresolved. Catalog parity is not full F117 acceptance. `scripts/qualify-localization.py` records this distinction explicitly.

Primary platform sources consulted on 2026-10-06: [LocationManager](https://developer.android.com/reference/android/location/LocationManager), [LocationManagerCompat](https://developer.android.com/reference/androidx/core/location/LocationManagerCompat), [location permissions](https://developer.android.com/develop/sensors-and-location/location/permissions), [inexact alarms](https://developer.android.com/develop/background-work/services/alarms), [notification permissions](https://developer.android.com/develop/ui/views/notifications/notification-permission), [language resources](https://developer.android.com/guide/topics/resources/app-languages). Existing pinned AndroidX dependencies are reused; no provider, map SDK or scheduling service dependency was added.

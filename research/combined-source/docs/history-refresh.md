# Historical observation persistence

Deproof / DEPR — Built by CodesbyFebin.

F030 requires status refresh without duplicate records. Opening a historical
transaction now uses `RecordsDao.saveHistoricalObservation` instead of inserting
a new receipt on every tap. One Room transaction finds an existing OBSERVED event
by cluster and signature, checks the exact serialized-message digest, retains
its original payload and ID, and appends a separately timestamped observation.
Different clusters remain separate. A changed message digest is refused before
any write. RPC unavailability never becomes a successful status or a zero value.

Repeated status observations are intentional records of independent reads;
they do not create new receipts, replace original review context, authorize
signing, or replay transaction submission. Existing duplicate receipts from
older application versions remain preserved; refresh selects the earliest
matching original event. No database schema or migration changes are needed.

`HistoricalObservationTest` exercises the actual current Room database under
Robolectric: repeated status changes, concurrent opens, cluster separation,
conflicting bytes with unchanged storage, and unavailable observations with the
original known status retained. The Android history route is wired to this DAO
method. Device interaction and live account-history acceptance remain NOT_RUN.
Local tests do not establish physical-device wallet behavior.

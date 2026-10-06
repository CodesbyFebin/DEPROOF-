# F079 status mapping (receipt-v2 schema)

Authoritative source: `contracts/receipt-v2.schema.json`. The registry wording for F079 ("pending, confirmed, finalized, failed, unavailable") does not name schema values. This document maps those words to the schema without changing either.

## Where each concept lives

| Concept | Schema field | Values |
|---|---|---|
| Local submission state | `submission.state` | NOT_SUBMITTED, SUBMITTING, SUBMITTED, SUBMISSION_UNKNOWN |
| Chain status (last known) | `chainObservation.lastKnownStatus` | UNKNOWN, PROCESSED, CONFIRMED, FINALIZED, FAILED |
| Observation availability | `chainObservation.availability` | AVAILABLE, UNAVAILABLE, NOT_QUERIED |

## F079 word to schema

| Word | Schema representation | Notes |
|---|---|---|
| pending (Deproof-submitted) | outcome `SUBMITTED`, `submission.state` `SUBMITTED`, `lastKnownStatus` `UNKNOWN` or `PROCESSED` | `SUBMITTED` outcome requires signature, broadcast, submittedByDeproof and `rpcAcceptedAt`. `UNKNOWN` means not yet observed; `PROCESSED` means observed but not final. |
| pending (historical observation) | outcome `OBSERVED`, `lastKnownStatus` `PROCESSED` | `OBSERVED` requires `submission.state` `NOT_SUBMITTED`. Deproof did not submit it. |
| SUBMITTING | `submission.state` `SUBMITTING` | Not accepted on a persisted `SUBMITTED` receipt. It is in-flight local state only. |
| confirmed | `lastKnownStatus` `CONFIRMED` | |
| finalized | `lastKnownStatus` `FINALIZED` | |
| failed | `lastKnownStatus` `FAILED` | |
| unavailable | `availability` `UNAVAILABLE` | Recorded as its own observation. It never overwrites the last known status and is never read as a failed transaction. |
| not queried | `availability` `NOT_QUERIED` | Used when no observation was attempted. |

## Rules enforced

- A status word is never stored as availability, and an availability word is never stored as a status.
- A refresh that is unavailable keeps the last known status (F030 tests).
- Values outside the schema enums are refused with `BAD_CHAIN_STATUS`, `BAD_OBSERVATION` or `BAD_SUBMISSION`.

## Tests

`app/src/test/java/com/example/data/ObservationStatusTest.kt`:
- `schemaStatusAndAvailabilityEnumsAreExactlyWhatTheValidatorAccepts` reads the schema enums and checks them against the validator.
- `pendingForADeproofSubmissionIsSubmittedWithUnknownOrProcessedChainStatus` checks the pending mapping.
- `submittingIsNotAPersistedReceiptState` checks the SUBMITTING rule.
- `statusesOutsideTheSchemaEnumAreRefused`, `unavailabilityIsRecordedSeparatelyFromTheLastKnownStatus` and `everySchemaStatusIsTrackedAsItsOwnObservation`.

These are local JVM tests. They do not establish live chain observation or device behaviour.

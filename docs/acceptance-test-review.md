# Acceptance test review

Deproof / DEPR — Built by CodesbyFebin.

Review the acceptance text, implementation and existing assertions together. An empty registry `tests` field is a linkage gap until source/test inspection establishes otherwise. Generic code coverage or a passing gate does not establish acceptance.

| Requirement | Existing meaningful assertions | Remaining acceptance |
|---|---|---|
| F040 | InspectionTest checks ordered independent per-instruction verdicts, reversed order, multi-instruction memo refusal and empty-message rejection; CoreTest covers unsupported layouts | Actual Android review of supported/unsupported messages; no device pass |
| F041 | InspectionTest asserts exact allowed transfer/memo summary and refused Approve summary | Android presentation/accessibility and complete explanation localization |
| F045–F048 | CoreTest and ReviewBindingTest test serialized-byte mutations, account/cluster/time/fee/block-height context, wallet-account mismatch and immutable hashes | Observe recomputation and refusal at real wallet handoff; local methods alone do not prove UI ordering |
| FN042 | LookupTableTest validates legacy static keys and refuses versioned lookup/unresolved account references | Supported scope is refusal, not live address-table resolution |
| C095 | PolicyTest limits rebuilding to one and proves rebuilt bytes cannot inherit approval | Real expired-blockhash flow and fresh wallet authorization |
| FN062 | PolicyTest refuses unqualified/software key levels; NodeProtocolTest verifies raw-digest/domain-separated signature contracts | Observe Android Keystore KeyInfo, exact returned signature and independent verification on physical hardware |

All existing domain *Test.kt classes are executed by the direct JVM runner as well as Gradle. Compilation alone is not test execution. F040/F041's dedicated six-test run is recorded in evidence/qualification/f040-f041-result.json and f040-f041-tests.log.

Remaining registry records retain their scoped status. A missing named linkage does not become MISSING_TESTS automatically. Device, Docker and external provider gates remain NOT_RUN/BLOCKED until actually observed. Priority follows security dependencies and available environments, never an arbitrary test count.

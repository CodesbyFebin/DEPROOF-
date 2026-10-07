# Deproof — Qualification Results

**Gate**: SKR Integration (skr-integration)
**Score**: 16/16 PASS
**Source**: `evidence/qualification/skr-integration.json`

## Automated Checks (All PASS)

| ID | Description | Evidence |
|----|-------------|---------|
| SKR-MINT-01 | SKR mint address matches in Gradle build config | `evidence/qualification/skr-integration/gradle-mint.txt` |
| SKR-MINT-02 | SKR mint address matches in Go constants | `evidence/qualification/skr-integration/go-mint.txt` |
| SKR-MINT-03 | SKR mint address matches in Kotlin constants | `evidence/qualification/skr-integration/kotlin-mint.txt` |
| SKR-PROG-01 | SPL Token Program address consistent across Go/Kotlin/Gradle | `evidence/qualification/skr-integration/token-prog-consistency.txt` |
| SKR-DEC-01 | SKR decimals (6) correct in Go | `evidence/qualification/skr-integration/go-decimals.txt` |
| SKR-DEC-02 | SKR decimals (6) correct in Kotlin | `evidence/qualification/skr-integration/kotlin-decimals.txt` |
| SKR-TEST-01 | Go TestBuildSKRPaymentJob tests pass | `evidence/qualification/skr-integration/go-tests.txt` |
| SKR-TEST-02 | Go TestDuplicate payment rejection passes | `evidence/qualification/skr-integration/dup-test.txt` |
| SKR-TEST-03 | Go TestChanged bytes rejection passes | `evidence/qualification/skr-integration/changed-test.txt` |
| SKR-TEST-04 | Go TestParse round-trip passes | `evidence/qualification/skr-integration/parse-test.txt` |
| SKR-SCHEMA-01 | `skr-payment-job-v1.schema.json` schema present and valid | `evidence/qualification/skr-integration/payment-schema.txt` |
| SKR-AUTH-01 | AUTHORIZATION=CONSTRUCTION_ONLY present in Go | `evidence/qualification/skr-integration/auth-go.txt` |
| SKR-AUTH-02 | AUTHORIZATION=CONSTRUCTION_ONLY present in Kotlin | `evidence/qualification/skr-integration/auth-kotlin.txt` |
| SKR-FMT-01 | Decimal formatting boundary values pass | `evidence/qualification/skr-integration/fmt-test.txt` |
| SKR-KT-01 | Kotlin JVM SkrPaymentJobTest coverage present | `evidence/qualification/skr-integration/kotlin-jvm-tests.txt` |
| SKR-KT-RUN-01 | NOT_APPLICABLE: Run Kotlin JVM tests requires Android SDK (manual) | — |

## Manual Gates (BLOCKED — require external infrastructure)

| ID | Reason |
|----|--------|
| SKR-MAN-01 | On-chain SPL Token Program and SKR mint verification: requires live Solana RPC |
| SKR-MAN-02 | End-to-end payment submission: requires devnet + funded wallet + physical device |
| SKR-MAN-03 | SKR oracle receipt round-trip: requires running oracle endpoint |

## Additional Test Metrics (This Session)

- Kotlin JVM unit tests: **125 PASS, 0 FAIL** (testDebugUnitTest)
- Go unit tests: **46 PASS, 0 FAIL** (internal/agent + cmd)
- Includes SkrPaymentJobTest: **34 PASS**

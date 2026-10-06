# Deproof P1 MVP - Requirements Matrix (281 Items)

**Status:** Qualifying Implementation  
**Total Requirements:** 281  
**Verified (CI):** 15  
**Remaining:** 266  

---

## Phase 1: Unit Tests (60 items) - Locally Actionable

### Formatter Tests (12 items)
- [ ] SolFormatter.formatSol() with various precisions
- [ ] SolFormatter edge cases (0 SOL, 1e-9 SOL, large amounts)
- [ ] SkrFormatter.formatSkr() basic and edge cases
- [ ] SkrFormatter decimal formatting
- [ ] Timestamp formatter with various zones
- [ ] Timestamp edge cases (epoch, future dates)
- [ ] Address formatter truncation logic
- [ ] Signature formatter display length
- [ ] Currency formatter rounding modes
- [ ] BigDecimal conversion safety
- [ ] Null input handling
- [ ] Internationalization considerations

### Validator Tests (15 items)
- [ ] PublicKeyValidator accepts valid base58 addresses
- [ ] PublicKeyValidator rejects invalid base58
- [ ] PublicKeyValidator rejects wrong-length keys
- [ ] MintValidator accepts token mint addresses
- [ ] MintValidator validates decimals (0-18)
- [ ] SignatureValidator accepts valid signatures
- [ ] SignatureValidator rejects malformed signatures
- [ ] AmountValidator accepts decimal amounts
- [ ] AmountValidator rejects negative amounts
- [ ] AmountValidator handles very large amounts
- [ ] DecimalValidator enforces precision
- [ ] URLValidator accepts valid RPC endpoints
- [ ] URLValidator rejects invalid protocols
- [ ] EmailValidator accepts common formats
- [ ] EmailValidator rejects malformed emails

### Crypto Utility Tests (16 items)
- [ ] Base58Encoder encodes bytes correctly
- [ ] Base58Encoder handles edge lengths
- [ ] Base58Decoder decodes valid strings
- [ ] Base58Decoder rejects invalid characters
- [ ] BinaryParser reads u8 correctly
- [ ] BinaryParser reads u16 little-endian
- [ ] BinaryParser reads u32 correctly
- [ ] BinaryParser reads u64 correctly
- [ ] BinaryParser reads strings with length prefix
- [ ] BinaryParser reads arrays
- [ ] BinaryParser offset tracking
- [ ] BinaryParser bounds checking
- [ ] MessageBinding.createCanonicalMessage() consistency
- [ ] MessageBinding.calculateMessageHash() SHA-256 correctness
- [ ] MessageBinding.verifyMessageIntegrity() validation
- [ ] MessageBinding tamper detection on modified messages

### Instruction Decoder Tests (12 items)
- [ ] InstructionDecoder.decodeInstruction() basic transfer
- [ ] InstructionDecoder handles TransferChecked instruction
- [ ] InstructionDecoder extracts mint correctly
- [ ] InstructionDecoder validates decimals
- [ ] InstructionDecoder verdict determination: Payable
- [ ] InstructionDecoder verdict determination: DoNotSign
- [ ] InstructionDecoder verdict determination: Unknown
- [ ] InstructionDecoder rejects malformed instructions
- [ ] InstructionDecoder handles missing accounts
- [ ] InstructionDecoder handles missing data
- [ ] InstructionDecoder decimal validation (6 for SKR)
- [ ] InstructionDecoder authority validation

### ViewModel State Tests (5 items)
- [ ] NowViewModel.uiState emits correct initial state
- [ ] NowViewModel balance updates trigger state change
- [ ] ReviewViewModel instruction loading updates state
- [ ] ReceiptsViewModel receipt list emission
- [ ] TasksViewModel completion tracking

---

## Phase 2: Device/Emulator Tests (45 items) - Requires Android Environment

### Screen Navigation (8 items)
- [ ] App starts to Now screen
- [ ] Bottom nav shows all 5 tabs
- [ ] Tab switching works (Now → Review → Receipts → Tasks → Nodes)
- [ ] Back button in each screen
- [ ] Back button from Now screen exits app
- [ ] State preserved on tab switch and back
- [ ] Deep linking to Review screen (if supported)
- [ ] Navigation transitions are smooth

### Now Screen (8 items)
- [ ] Wallet connection card displays
- [ ] "Connect Wallet" button clickable
- [ ] SOL balance displays after mock connection
- [ ] SKR balance displays after mock connection
- [ ] Balance formatting with correct decimals
- [ ] Transaction history list appears
- [ ] Scroll through transaction list
- [ ] Refresh button triggers balance update

### Review Screen (10 items)
- [ ] Instruction input field accepts data
- [ ] Decode button triggers instruction parsing
- [ ] Instruction displays parsed fields
- [ ] Verdict determination correct for test instructions
- [ ] Message binding shows hash
- [ ] Tamper detection alerts if hash modified
- [ ] Approve button clickable
- [ ] Reject button clickable
- [ ] Action results display feedback
- [ ] Error handling for invalid instructions

### Receipts Screen (8 items)
- [ ] Receipt list displays approved transactions
- [ ] Receipt detail view opens
- [ ] JSON export button works
- [ ] Copy to clipboard functionality
- [ ] Timestamp formatting correct
- [ ] Filter/search if implemented
- [ ] Empty state when no receipts
- [ ] Scroll performance with many receipts

### Tasks & Nodes Screens (5 items)
- [ ] Tasks screen displays task list
- [ ] Task completion toggles
- [ ] Nodes screen shows network status
- [ ] Progress indicators update
- [ ] Evidence collection (if implemented)

### UI Polish & Responsiveness (6 items)
- [ ] Portrait orientation layout
- [ ] Landscape orientation layout
- [ ] Keyboard doesn't cover input fields
- [ ] Text sizes readable on various screen sizes
- [ ] Colors consistent with theme
- [ ] Dark mode switching works (if implemented)

---

## Phase 3: RPC Integration Tests (70 items) - Requires Network Access

### Balance Query Service (15 items)
- [ ] RpcRepository.getBalance() returns SOL amount
- [ ] RpcRepository.getTokenBalance() returns SKR amount
- [ ] RpcRepository handles devnet endpoint
- [ ] RpcRepository handles testnet endpoint
- [ ] RpcRepository caches results (if implemented)
- [ ] RpcRepository handles network timeout
- [ ] RpcRepository handles 503 service unavailable
- [ ] RpcRepository handles 429 rate limit (retry)
- [ ] RpcRepository handles malformed response
- [ ] RpcRepository handles missing account
- [ ] RpcRepository decimal precision matches blockchain
- [ ] RpcRepository parses lamports correctly
- [ ] RpcRepository handles zero balance
- [ ] RpcRepository handles very large balances
- [ ] RpcRepository updates state on success

### Transaction Retrieval (12 items)
- [ ] RpcRepository.getTransaction() returns tx data
- [ ] RpcRepository parses transaction structure
- [ ] RpcRepository extracts instructions array
- [ ] RpcRepository decodes base64 transaction
- [ ] RpcRepository handles failed transactions
- [ ] RpcRepository handles pending transactions
- [ ] RpcRepository handles not-found transaction
- [ ] RpcRepository timestamp parsing
- [ ] RpcRepository fee extraction
- [ ] RpcRepository signer extraction
- [ ] RpcRepository handles network errors gracefully
- [ ] RpcRepository timeout handling

### Instruction Processing (18 items)
- [ ] Instruction.decode() parses program ID
- [ ] Instruction.decode() extracts accounts
- [ ] Instruction.decode() extracts data payload
- [ ] Transfer instruction decoding accuracy
- [ ] TransferChecked instruction decoding
- [ ] SetAuthority instruction decoding
- [ ] Mint instruction decoding
- [ ] Burn instruction decoding
- [ ] MintTo instruction decoding
- [ ] CloseAccount instruction decoding
- [ ] Unknown instruction handling
- [ ] Instruction data length validation
- [ ] Account count validation
- [ ] Signer detection accuracy
- [ ] Authority chain validation
- [ ] Token mint validation (6 decimals)
- [ ] Amount precision validation
- [ ] Fee structure detection

### Verdict Determination (10 items)
- [ ] Verdict.Payable for standard transfers
- [ ] Verdict.Payable for token transfers
- [ ] Verdict.DoNotSign for suspicious authority
- [ ] Verdict.DoNotSign for unknown programs
- [ ] Verdict.DoNotSign for amount anomalies
- [ ] Verdict.Unknown for edge cases
- [ ] Verdict consistency with rules
- [ ] Verdict explanation text generation
- [ ] Verdict persistence to Receipt
- [ ] Verdict audit trail logging

### Wallet Connection (12 items)
- [ ] MWA handshake with Phantom
- [ ] MWA handshake with Backpack
- [ ] MWA request signing capabilities
- [ ] MWA wallet returns public key
- [ ] MWA transaction signing request
- [ ] MWA sign rejection handling
- [ ] MWA timeout handling
- [ ] MWA session persistence
- [ ] MWA disconnect flow
- [ ] MWA re-connection after disconnect
- [ ] MWA Android app requirement
- [ ] MWA version compatibility

### Error Recovery (3 items)
- [ ] Network error → retry with backoff
- [ ] Rate limit → exponential backoff
- [ ] Timeout → user-facing message and retry option

---

## Phase 4: Security & Hardening (40 items) - Code Review & Local Testing

### Input Validation (10 items)
- [ ] Address input validated before use
- [ ] Signature input validated
- [ ] Amount input bounds checked
- [ ] Transaction data length validated
- [ ] Instruction format validated
- [ ] JSON payload validated
- [ ] URL scheme validated (https only)
- [ ] String length limits enforced
- [ ] Null pointer checks throughout
- [ ] Type safety via sealed classes

### Cryptographic Security (8 items)
- [ ] SHA-256 used correctly (not MD5, SHA-1)
- [ ] Message binding prevents modification
- [ ] Base58 encoding/decoding doesn't lose entropy
- [ ] Random UUID generation secure
- [ ] Timestamp precision adequate (milliseconds)
- [ ] No hardcoded secrets in code
- [ ] No logging of sensitive data
- [ ] Crypto library versions current

### Database Security (8 items)
- [ ] Room database encrypted at rest (if sensitive data stored)
- [ ] SQL injection impossible (parameterized queries)
- [ ] Backup exclusion configured
- [ ] File permissions restrict access
- [ ] No plaintext passwords in database
- [ ] Transaction integrity maintained
- [ ] Concurrent access handled safely
- [ ] Database version migrations tested

### API Security (8 items)
- [ ] RPC endpoints validated (https)
- [ ] No API keys hardcoded
- [ ] Rate limiting respected
- [ ] User-Agent header present
- [ ] TLS certificate validation enabled
- [ ] Proxy support if needed
- [ ] Timeout configured (no infinite waits)
- [ ] Error messages don't expose internals

### Network Security (6 items)
- [ ] HTTPS enforced for all requests
- [ ] Certificate pinning (optional, stricter security)
- [ ] DNS resolution safe
- [ ] No plaintext transmission of sensitive data
- [ ] Proxy tunneling for mitmproxy testing capability
- [ ] Offline mode graceful degradation

---

## Phase 5: Documentation (30 items) - Locally Actionable

### User Documentation (8 items)
- [ ] README.md with quick start
- [ ] Screenshot tour of all 5 screens
- [ ] Wallet setup instructions (Phantom, Backpack)
- [ ] Troubleshooting guide (common issues)
- [ ] FAQ (frequently asked questions)
- [ ] Video tutorial link (if created)
- [ ] Glossary of Solana terms
- [ ] Accessibility notes

### Developer Documentation (10 items)
- [ ] Architecture overview diagram
- [ ] MVVM pattern explanation
- [ ] Dependency injection setup
- [ ] StateFlow reactive patterns used
- [ ] Room database schema explanation
- [ ] Solana RPC client integration guide
- [ ] Adding new screens (template)
- [ ] Adding new validators (example)
- [ ] Error handling patterns
- [ ] Testing strategy and examples

### Configuration & Setup (8 items)
- [ ] Local.properties example template
- [ ] Android SDK version requirements
- [ ] Gradle configuration explanation
- [ ] Maven repository configuration
- [ ] Build variants (debug, release)
- [ ] Signing configuration
- [ ] ProGuard/R8 configuration
- [ ] Android Studio project setup

### API & Integration (4 items)
- [ ] RPC endpoint configuration
- [ ] Wallet adapter protocol
- [ ] Token mint addresses (mainnet, testnet)
- [ ] Error code reference

---

## Phase 6: CI/CD & DevOps (36 items) - P2-P5 Phases

### Build Pipeline (12 items)
- [ ] Gradle caching configured
- [ ] Parallel task execution
- [ ] Artifact staging and versioning
- [ ] APK signing automated
- [ ] Build artifact retention policy
- [ ] Build timeout configuration
- [ ] Incremental builds optimized
- [ ] Dependency lock file (if using)
- [ ] Build performance metrics
- [ ] Failure notification
- [ ] Build history tracking
- [ ] Release note generation

### Testing Pipeline (10 items)
- [ ] Unit test execution (Phase 1)
- [ ] Instrumentation test execution (Phase 2)
- [ ] Code coverage reporting (target: 70%)
- [ ] Coverage trend tracking
- [ ] Test failure reporting
- [ ] Flaky test detection
- [ ] Test parallelization
- [ ] Test environment setup
- [ ] Emulator provisioning (if using)
- [ ] Test artifact collection

### Security Pipeline (8 items)
- [ ] SAST (Static Application Security Testing)
- [ ] Dependency vulnerability scanning
- [ ] Secret scanning (no API keys committed)
- [ ] License compliance checking
- [ ] Code signing verification
- [ ] APK size analysis
- [ ] Proguard output verification
- [ ] Security policy updates

### Deployment (6 items)
- [ ] Play Store alpha release workflow
- [ ] Internal testing track
- [ ] Beta release process
- [ ] Production rollout gates
- [ ] Crash reporting (Crashlytics)
- [ ] Performance monitoring (Firebase)

---

## Dependency Order Summary

```
Phase 1: Unit Tests (60 items)
    ├─ Run locally
    ├─ No external dependencies
    └─ Enables Phase 2

Phase 2: Device Tests (45 items)
    ├─ Requires emulator/device
    ├─ Depends on Phase 1 results
    └─ Enables Phase 3

Phase 3: RPC Integration (70 items)
    ├─ Requires network access
    ├─ Depends on Phase 2 passing
    └─ Enables Phase 4

Phase 4: Security (40 items)
    ├─ Code review focused
    ├─ Parallel with Phase 3
    └─ Critical for release

Phase 5: Documentation (30 items)
    ├─ Locally actionable
    ├─ Parallel with other phases
    └─ Enables external communication

Phase 6: CI/CD (36 items)
    ├─ Automation focused
    ├─ Parallel with phases 4-5
    └─ Enables production readiness
```

---

## Success Metrics

| Phase | Complete When | Acceptance Criteria |
|-------|---|---|
| Phase 1 | All 60 tests pass | ≥80% code coverage, 0 failures |
| Phase 2 | Device tests pass | All 45 tests on real/emulated device, video evidence |
| Phase 3 | RPC integration | Real wallet connection, successful transaction review |
| Phase 4 | Security review | No high/critical findings, code review approval |
| Phase 5 | Documentation | All docs complete, no TODOs, examples tested |
| Phase 6 | CI/CD ready | Automated pipeline green, reliable artifact production |

---

**Next Action:** Begin Phase 1 unit tests (start with formatters and validators)

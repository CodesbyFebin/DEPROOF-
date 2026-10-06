# Phase 2 Implementation Plan - Device/Emulator UI Testing

**Status:** Planned  
**Priority:** High (user-facing features)  
**Requirements:** 45 items  
**Environment:** Android Emulator or Physical Device  
**Target SDK:** API 34 (targetSdk)  
**Min SDK:** API 28 (minSdk)

---

## Overview

Phase 2 validates all user-facing screens and interactions through UI testing. The 45 items are organized into 6 areas:

1. **Screen Navigation** (8 items) - Bottom nav, tab switching, deep linking
2. **Now Screen** (8 items) - Wallet connection, balance display, transaction history
3. **Review Screen** (10 items) - Instruction input, parsing, verdict display, approval
4. **Receipts Screen** (8 items) - Receipt list, detail view, export, search
5. **Tasks & Nodes Screens** (5 items) - Task completion, network status, progress
6. **UI Polish & Responsiveness** (6 items) - Orientation, keyboard, fonts, dark mode

---

## Testing Strategy

### Environment Options

**Option A: Android Emulator (Cloud-Capable)**
- Setup: Requires KVM/virtualization support
- Automation: Full CI/CD pipeline possible
- Cost: Free, runs in cloud container
- Status: Requires Linux container with GPU passthrough

**Option B: Physical Device (Requires Hardware)**
- Setup: Connect via USB or wireless adb
- Automation: Limited CI/CD (must have device attached)
- Cost: Hardware dependent
- Status: Blocked until device available

**Option C: Google Cloud Test Lab (Recommended for CI)**
- Setup: Push to Firebase Test Lab
- Automation: Full CI/CD integration
- Cost: $2–10 per test run
- Status: Requires Google Cloud setup

### Phase 2 Approach (Hybrid)

1. **Foundation (Cloud):** Write all UI tests using Compose testing + Espresso
2. **Local Testing:** Run on emulator when available
3. **CI/CD:** Execute in cloud container or GitHub Actions with test matrix
4. **Device Testing:** Run on physical device when available
5. **Documentation:** Detailed setup and execution guides

---

## Test Organization

### Compose UI Testing Framework

Tests use Jetpack Compose testing APIs for native Compose screens:

```kotlin
@RunWith(ComposeTestRule::class)
class NowScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun walletConnectionCardDisplays() {
        composeTestRule.setContent {
            DepRoofTheme {
                NowScreen(rpcRepository, navController)
            }
        }
        
        composeTestRule.onNodeWithText("Connect Wallet").assertIsDisplayed()
    }
}
```

### Espresso Testing Framework

Tests for navigation, intents, and system interactions:

```kotlin
@RunWith(AndroidJUnit4::class)
class NavigationTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)
    
    @Test
    fun bottomNavShowsAllTabs() {
        onView(withId(R.id.bottom_nav)).check(matches(isDisplayed()))
        // Verify all 5 tabs present
    }
}
```

---

## Test Categories & Coverage

### 1. Navigation Tests (8 items)

**Location:** `app/src/androidTest/kotlin/com/deproof/ui/NavigationTest.kt`

```
✓ App starts to Now screen (verify MainActivity launches NowScreen as default)
✓ Bottom nav shows all 5 tabs (verify BottomNavigationBar has 5 items)
✓ Tab switching works (click each tab, verify navigation)
✓ Back button in each screen (verify back stack works)
✓ Back button from Now screen exits app (verify onBackPressed closes MainActivity)
✓ State preserved on tab switch and back (verify ViewModel state retained)
✓ Deep linking to Review screen (verify intent filter works)
✓ Navigation transitions are smooth (verify animation timing)
```

### 2. Now Screen Tests (8 items)

**Location:** `app/src/androidTest/kotlin/com/deproof/presentation/ui/screen/NowScreenTest.kt`

```
✓ Wallet connection card displays (verify Card composable visible)
✓ "Connect Wallet" button clickable (verify Button is enabled)
✓ SOL balance displays after mock connection (verify balance text)
✓ SKR balance displays after mock connection (verify token balance)
✓ Balance formatting with correct decimals (verify formatSol()/formatSkr())
✓ Transaction history list appears (verify LazyColumn renders)
✓ Scroll through transaction list (verify scrolling works)
✓ Refresh button triggers balance update (verify onClick handler)
```

### 3. Review Screen Tests (10 items)

**Location:** `app/src/androidTest/kotlin/com/deproof/presentation/ui/screen/ReviewScreenTest.kt`

```
✓ Instruction input field accepts data (verify TextField input)
✓ Decode button triggers instruction parsing (verify onClick calls decode)
✓ Instruction displays parsed fields (verify decoded instruction renders)
✓ Verdict determination correct for test instructions (verify verdict logic)
✓ Message binding shows hash (verify hash display)
✓ Tamper detection alerts if hash modified (verify alert shows on modification)
✓ Approve button clickable (verify approve action)
✓ Reject button clickable (verify reject action)
✓ Action results display feedback (verify snackbar/toast)
✓ Error handling for invalid instructions (verify error message)
```

### 4. Receipts Screen Tests (8 items)

**Location:** `app/src/androidTest/kotlin/com/deproof/presentation/ui/screen/ReceiptsScreenTest.kt`

```
✓ Receipt list displays approved transactions (verify LazyColumn with receipts)
✓ Receipt detail view opens (verify navigation to detail screen)
✓ JSON export button works (verify export file creation)
✓ Copy to clipboard functionality (verify clipboard content)
✓ Timestamp formatting correct (verify date/time display)
✓ Filter/search if implemented (verify search field if present)
✓ Empty state when no receipts (verify empty message)
✓ Scroll performance with many receipts (verify smooth scrolling with 100+ items)
```

### 5. Tasks & Nodes Screens Tests (5 items)

**Location:** `app/src/androidTest/kotlin/com/deproof/presentation/ui/screen/TasksScreenTest.kt` & `NodesScreenTest.kt`

```
✓ Tasks screen displays task list (verify LazyColumn renders)
✓ Task completion toggles (verify checkbox/button toggle state)
✓ Nodes screen shows network status (verify status indicator)
✓ Progress indicators update (verify animation/progress bar)
✓ Evidence collection (if implemented - verify data collection)
```

### 6. UI Polish & Responsiveness Tests (6 items)

**Location:** `app/src/androidTest/kotlin/com/deproof/ui/UiPolishTest.kt`

```
✓ Portrait orientation layout (verify layout constraint with portrait)
✓ Landscape orientation layout (verify layout with landscape)
✓ Keyboard doesn't cover input fields (verify scrolling with keyboard)
✓ Text sizes readable on various screen sizes (verify font sizes)
✓ Colors consistent with theme (verify color values match)
✓ Dark mode switching works (verify dark theme applies if implemented)
```

---

## Test Fixtures & Mocking

### Mock Data Factory

Create consistent test data for all screens:

```kotlin
object MockDataFactory {
    fun createMockReceipt(): Receipt = Receipt(
        id = "receipt-1",
        transactionHash = "..." ,
        instruction = "Transfer 1 SOL",
        verdict = Verdict.Payable,
        timestamp = System.currentTimeMillis(),
        approved = true
    )
    
    fun createMockTransaction(): Transaction = Transaction(
        id = "tx-1",
        hash = "...",
        amount = BigDecimal("1.0"),
        token = "SOL",
        timestamp = System.currentTimeMillis()
    )
}
```

### Test Repositories

Mock repository implementations:

```kotlin
class TestRpcRepository : RpcRepository {
    override suspend fun getBalance(address: String): Result<BigDecimal> {
        return Result.success(BigDecimal("100.0"))
    }
    
    override suspend fun getTokenBalance(address: String, mint: String): Result<BigDecimal> {
        return Result.success(BigDecimal("50.0"))
    }
}
```

### Test Navigation Controller

```kotlin
class TestNavController : NavController(mock()) {
    var lastNavigated: String? = null
    
    override fun navigate(route: String) {
        lastNavigated = route
    }
}
```

---

## Test Execution

### Local Emulator

```bash
# Start emulator
emulator -avd Pixel_4_API_34 &

# Wait for boot
adb wait-for-device

# Run all UI tests
./gradlew connectedAndroidTest

# Run specific test class
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.deproof.ui.NavigationTest

# Run with coverage
./gradlew connectedAndroidTest -Pandroid.enableTestCoverage=true
```

### GitHub Actions CI/CD

```yaml
name: Phase 2 UI Tests

on: [push, pull_request]

jobs:
  ui-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Setup Android SDK
        uses: android-actions/setup-android@v2
      - name: Create AVD
        run: |
          echo no | android create avd --force -n test -t "android-34" -b default
      - name: Start emulator
        run: |
          emulator -avd test -no-audio -no-window &
          adb wait-for-device
      - name: Run UI tests
        run: ./gradlew connectedAndroidTest
      - name: Upload results
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: ui-test-results
          path: app/build/reports/androidTests/
```

### Firebase Test Lab (Recommended)

```bash
# Build APK and test APK
./gradlew assembleDebug assembleAndroidTest

# Run on Firebase Test Lab
gcloud firebase test android run \
  --app=app/build/outputs/apk/debug/app-debug.apk \
  --test=app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk \
  --device-ids=Pixel4,Pixel5 \
  --os-versions=30,31,32,33,34 \
  --locales=en
```

---

## Implementation Roadmap

### Week 1: Foundation & Navigation (8 items)
1. Set up test infrastructure
2. Create navigation tests
3. Verify bottom nav structure
4. Test deep linking

### Week 2: Now Screen & Review Screen (18 items)
5. Implement Now screen tests
6. Implement Review screen tests
7. Create mock data factory
8. Add verdict verification

### Week 3: Receipts & Tasks/Nodes (13 items)
9. Implement Receipts screen tests
10. Implement Tasks/Nodes tests
11. Add export/clipboard tests
12. Verify state management

### Week 4: Polish & CI/CD (6 items + setup)
13. Test dark mode
14. Test orientation changes
15. Verify keyboard handling
16. Set up GitHub Actions
17. Configure Firebase Test Lab

---

## Success Criteria

✅ **All 45 UI tests pass** on emulator/device  
✅ **Coverage:** 80%+ of UI code  
✅ **Performance:** Tests complete in <5 minutes  
✅ **Automation:** CI/CD pipeline runs on every commit  
✅ **Documentation:** Setup and execution guides complete  
✅ **Devices:** Tested on API 28 (min) and API 34 (target)  

---

## Known Blockers

🔴 **Android Emulator:** Requires KVM or nested virtualization in cloud  
🔴 **Physical Device:** Requires USB connection or wireless adb  
🟡 **Firebase Test Lab:** Requires Google Cloud project setup  

---

## Next Steps

1. **Immediate:** Write navigation test cases
2. **Parallel:** Set up mock data factory and test repositories
3. **Week 1:** Get tests compiling and running locally
4. **Week 2:** Add CI/CD pipeline
5. **Final:** Execute full test suite on all target devices

---

**Owner:** Phase 2 Implementation  
**Target Completion:** 4 weeks (with device access)  
**Dependencies:** Phase 1 ✅ Complete  
**Blockers:** Emulator setup in cloud environment

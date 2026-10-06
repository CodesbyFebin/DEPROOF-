# Deproof Design Architecture

**Tagline:** Review. Sign. Verify.\
**Creator:** Febin Francis — CodesbyFebin\
**Visual Identity:** DE monogram, gradient cyan→purple, glowing effects

---

## 1. Brand Identity

### Logo

- **DE monogram:** Rounded, modern letterform
- **Primary gradient:** Cyan (#7FE7F0) → Purple (#8B7CFF)
- **Secondary gradient:** Mint (#3DDC97) → Cyan
- **Glow effect:** Subtle luminous halo in all contexts
- **Launcher icon:** DE monogram centered on obsidian background with glow

### Color Palette

**Dark Mode (Default)**

- Background: Obsidian `#0B1020`
- Surface: Navy `#121A2B`
- Text: `#E7EEF8`
- Primary accent: Mint `#3DDC97`
- Secondary accent: Cyan `#7FE7F0`
- Tertiary accent: Purple `#8B7CFF`
- Error/Refusal: `#FF6B6B`
- Success/Payable: `#51CF66`

**Light Mode**

- Background: Pearl `#F7F4EF`
- Surface: White `#FFFFFF`
- Text: Ink `#1A1714`
- Primary accent: Mint `#1F8A62`
- Secondary accent: Cyan `#0099CC`
- Tertiary accent: Lavender `#6E62C9`
- Error: `#E63946`
- Success: `#06A77D`

### Typography

- **Display (large titles):** Sans-serif, 28-32px, bold, letter spacing -0.5px
- **Headline (screen titles):** Sans-serif, 20-24px, semi-bold
- **Body (primary text):** Sans-serif, 14-16px, regular
- **Label (UI labels):** Sans-serif, 12-14px, medium
- **Monospace (hashes, addresses):** Monospace, 12px, `font-variant-numeric: tabular-nums`

---

## 2. Layout Structure

### Three Screens (Bottom Navigation)

#### Screen 1: Now (Home)

**Purpose:** Account observation, wallet status, transaction history\
**Navigation tab icon:** House

**Top section:**

- Cluster selector (devnet/mainnet-beta) — dropdown in top-right
- Status badges (wallet connected/disconnected, RPC status)

**Main content:**

- Address paste input with validate button
- OR "Connect wallet" button (mint/cyan gradient) if no wallet session

**Account summary card:**

- SOL balance (9 decimals, formatted)
- SKR balance (6 decimals, formatted)
- Slot from last read
- Observation time + queried address label
- Refresh button or pull-to-refresh

**Last receipt card (collapsed):**

- Status: Signed/Rejected/Observed
- Summary one-liner
- Timestamp
- Tap to expand → navigate to Receipts

**Transaction history:**

- Last 10 signatures in list
- Each row:
  - Confirmation status (dot + text: Finalized/Confirmed/Processed/Failed)
  - Summary (program, amount, direction)
  - Timestamp
  - Explorer link icon
- Tap signature → opens Review screen

**Empty/error states:**

- No wallet + no address: "Paste a mainnet address or connect wallet"
- Loading: Spinner + "Fetching account..."
- Error: Error message with error code + retry button
- No history: "No transactions yet"
- Stale data (after refresh failure): Label "Stale" or "Last read X minutes ago"

---

#### Screen 2: Review

**Purpose:** Decode transaction, display verdict, bind approval\
**Navigation tab icon:** Document with checkmark

**Top section:**

- Transaction summary one-liner (e.g., "Transfer 1000 SKR to 7xK2...")
- Back button to Now

**Main content:**

**Verdict card (large, prominent):**

- **If Payable:** Green background, checkmark icon, "Payable" title
- **If Do not sign:** Red background, X icon, "Do not sign" title
- Reason: One sentence explaining verdict
- Warnings (if applicable):
  - "⚠️ Changes wallet authority" (SetAuthority)
  - "⚠️ Not official SKR" (wrong mint)
  - "⚠️ Decimals mismatch" (wrong decimals)
  - "⚠️ Cannot decode" (unknown program)
  - "⚠️ SOL stake, not SKR. Do not sign." (native stake)

**Instruction details panel:**

- Program ID (full base58, copy button, explorer link)
- Account section:
  - Source (shortened: 4+...+4, tap to expand full key)
  - Mint (shortened, expandable)
  - Destination (shortened, expandable)
  - Authority (shortened, expandable)
- Amount (formatted: "1,000.000000" if SKR, "1234567890 lamports" if SOL)
- Decimals (shown as "6 decimals" or "⚠️ Observed: 5 decimals")
- Fee (from transaction or "unknown")
- Cluster (mainnet-beta or devnet)
- dApp identity ("Deproof" / "[https://deproof.app](https://deproof.app/)")

**Message hash display:**

- Label: "Message hash"
- Value: Truncated hash (first 12 chars + "...")
- Copy button
- Full hash visible on tap

**Tamper detection (if MESSAGE_CHANGED):**

- Red banner: "MESSAGE_CHANGED"
- Text: "This transaction has been modified. You must review it again before approving."
- Approve button disabled (grayed out)

**Button section (bottom):**

- **Approve button:**
  - Enabled only if: verdict is Payable AND wallet session exists AND memo gate passed (for mainnet SKR) AND message hash unchanged
  - Text: "Approve" (mint gradient, bold)
  - Disabled state: Grayed out with tooltip explaining why
- **Reject button:**
  - Always enabled (dark red/error color)
  - Text: "Reject"

**Evidence UI (hidden if verdict is Do not sign):**

- Expandable section: "Capture evidence (optional)"
- File import button (+ icon)
- Note input (text field)
- Capture timestamp display
- File list with delete buttons

---

#### Screen 3: Receipts

**Purpose:** View and export local transaction history\
**Navigation tab icon:** Receipt/document

**Top section:**

- Title: "Receipts"
- Search/filter button (optional for phase 2)

**Receipt list:**

- Most recent at top
- Each receipt row:
  - Status badge (Signed = green checkmark, Rejected = red X)
  - Summary one-liner
  - Timestamp (relative: "2m ago", "1h ago")
  - Tap to expand detail view

**Receipt detail (expand/modal):**

- Status: "Signed" or "Rejected" with icon
- Summary (full)
- Message hash (truncated + copy)
- Signature (base58, if exists, with copy button)
- Slot (if broadcast)
- Block time (if available)
- Network (mainnet-beta or devnet)
- `broadcast: true/false` (if applicable)
- `submittedByClearance: true/false` (false until broadcast)
- Close detail button

**Copy receipt button (on detail):**

- Text: "Copy as JSON"
- On success: Toast "Copied to clipboard"
- On failure: Show text in modal box with copy-from-text fallback

**Empty state:**

- "No receipts yet"
- Hint: "Review and reject transactions to create receipts"

---

## 3. Component System

### Buttons

- **Primary (Approve, Connect wallet):** Mint or cyan gradient, rounded, 48px height, bold text
- **Secondary (Reject, Cancel):** Dark navy background, border 1px error color, rounded
- **Tertiary (Expand, Copy, Explorer link):** Text link, no background
- **Icon buttons:** 44px min tap target, circular background on hover

### Cards

- Border radius: 12-16px
- Background: Navy surface on dark
- Border: Optional 1px, cyan/purple accent
- Padding: 16-20px
- Shadow: Subtle (opacity 20%, blur 12px)

### Input Fields

- Border radius: 8px
- Border: 1px `#3DDC97` (mint, active) or `#121A2B` (navy, inactive)
- Padding: 12px 16px
- Focus: Cyan border glow, placeholder text fades
- Error state: Red border, error text below

### Status Indicators

- **Finalized:** Green dot + "Finalized"
- **Confirmed:** Cyan dot + "Confirmed"
- **Processed:** Purple dot + "Processed"
- **Failed:** Red dot + "Failed"
- **Pending:** Animated spinning dot + "Pending"
- Text always paired with color (never color alone)

### Badges

- **Wallet connected:** Green/mint badge "Connected"
- **Wallet disconnected:** Gray badge "Not connected"
- **RPC status:** Cyan dot + "Mainnet" or "Devnet"

### Modals/Sheets

- Dark overlay (60% opacity)
- Bottom sheet or center modal, rounded top
- Close button (X) in top-right or center-bottom
- Escape key closes (if possible)

---

## 4. Interaction Patterns

### Pull-to-refresh

- Pull down from top of Now screen
- Spinner appears
- Releases to refresh SOL + SKR + signatures independently
- On error: Show error, keep previous data visible, label "Stale"

### Tap to expand/collapse

- Full keys, hashes, details collapse by default
- Tap to expand full value
- Copy icon appears on expand
- Tap copy → toast confirmation

### Long press

- Copy address/hash on long press (alternative to button)
- Haptic feedback

### Wallet connection flow

- Tap "Connect wallet"
- MWA authorization request appears
- User selects wallet + account
- Account address displays
- Session saved until disconnect

### Approve/Reject flow

- Tap Approve → MWA sign request
- Wallet signature received → receipt created
- Navigate to Receipts automatically
- OR tap Reject → receipt created immediately, no signature needed

### Error recovery

- RPC error: Show full error string + retry button
- Stale data: Label "Stale (last read 5m ago)" + refresh button
- MESSAGE_CHANGED: Red banner, Approve disabled, prompt to review again

---

## 5. States & Transitions

### Data states

- **Loading:** Spinner + placeholder text
- **Empty:** Appropriate empty state message
- **Error:** Error message + error code + recovery action
- **Unavailable:** "Service temporarily unavailable" (timeout)
- **Success:** Data displayed, no error message
- **Stale:** Data displayed with "Stale" label + option to refresh

### Wallet states

- **Not connected:** "No wallet connected" message, Connect button shown
- **Connecting:** Spinner, "Requesting authorization..."
- **Connected:** Address displayed, Disconnect option in settings
- **Disconnected:** Clears session, returns to "No wallet connected"

### Approval states

- **Can approve:** Button enabled, description of what will happen
- **Cannot approve:** Button disabled, tooltip explaining why (e.g., "No wallet session" or "Message changed")
- **Submitted:** Toast "Signature submitted" or navigate to Receipts

---

## 6. Accessibility

### Text alternatives

- Every color/icon paired with text label
- Alt text for images/icons
- Error messages in text, not just red
- Status never indicated by color alone

### Touch targets

- Minimum 44×44 dp (device-independent pixels)
- Buttons, links, icons all 44dp+ minimum
- Spacing between targets ≥ 8dp

### Contrast

- Text: WCAG AA minimum (4.5:1 for normal, 3:1 for large)
- Dark mode: Light text on dark background
- Light mode: Dark text on light background

### Keyboard navigation

- Tab order logical (top to bottom, left to right)
- Focus visible (blue/cyan outline)
- Escape closes modals
- Enter submits forms

### Screen readers

- Semantic HTML structure
- `contentDescription` on all interactive elements
- List items announced with position ("1 of 10")
- State changes announced ("Approved", "Error: Network unavailable")

---

## 7. Dark & Light Mode

### Dark mode (default)

- Obsidian background (#0B1020)
- Navy surfaces (#121A2B)
- Light text (#E7EEF8)
- Cyan/mint accents
- Shadows soft and subtle

### Light mode

- Pearl background (#F7F4EF)
- White surfaces
- Ink text (#1A1714)
- Mint/cyan accents
- Shadows slightly stronger for depth

### Transition

- Smooth fade between themes (200-300ms)
- All color values interpolated
- Images/icons adapt automatically

---

## 8. Responsive Layout

### Phone (360-414dp width)

- Single column, full width with side gutters (16dp)
- Bottom navigation always visible
- Modals full height, bottom sheet from bottom
- Scrollable content areas

### Tablet (600+dp width)

- Same three-screen bottom nav (or top tabs)
- Wider cards with max-width constraints
- Landscape: side-by-side panels if beneficial (e.g., Now + Review)

### Orientation changes

- Smooth reflow
- No data loss
- Scroll position preserved where possible

---

## 9. Launch Flow

**App launch:**

1. Splash screen (1s): DE logo centered, fade in/out
2. Home screen (Now tab)
3. If first time: Tutorial overlay optional (skip button available)

**First interaction:**

- Paste address OR Connect wallet
- Fetch balances
- Show results

---

## 10. Visual Hierarchy

**High emphasis (user action required):**

- Verdict card (large, colored background)
- Approve/Reject buttons (gradient, bold)
- Error messages (red, bold, top-level)

**Medium emphasis (important info):**

- Account summary card (bordered, highlighted)
- Transaction list (each item scannable)
- Instruction details (organized sections)

**Low emphasis (supporting info):**

- Slot numbers, timestamps (smaller text, secondary color)
- RPC status badge (small, corner position)
- Exploratory links (text link color, optional tap)

---

## 11. Micro-interactions

### Button feedback

- Tap: Slight scale down (98%), opacity fade to 80%
- Release: Scale back up, snap
- Disabled: No response to tap, cursor change (if web/desktop)

### Loading

- Spinner: Rotating circle, cyan color, 2s rotation
- Pulse: Optional fade pulse on list items during load

### Copy to clipboard

- Toast appears (bottom-center, dark background)
- Text: "Copied to clipboard"
- Duration: 2-3 seconds, fade out
- Haptic: Brief vibration (if available)

### Navigation transitions

- Swipe back (if supported): Slide right, fade out previous screen
- Tap navigation tab: Fade transition between screens
- Push modal: Slide up from bottom
- Close modal: Slide down, fade out

---

## 12. Prototype Preview Notes

**From design concept:**

- "Your workspace" headline emphasizes personal control
- Three core actions highlighted: Connect wallet, Review transaction, Capture evidence
- Recent activity shows real evidence capture event with timestamp
- Visual balance between wallet state, transaction review, and evidence tracking
- Prominent gradient button (Connect wallet) draws focus
- Hash display establishes trust via transparency
- Evidence captured confirmation reinforces integrity

**Implementation priority:**

1. Core layout (Now, Review, Receipts tabs) + navigation
2. Account reading (balances, slot, address)
3. Transaction history list
4. Verdict card + instruction details
5. Approve/Reject buttons
6. Receipts list + copy JSON
7. Refinement pass: animations, states, accessibility

---

## 13. Out-of-Scope Design Elements

These are marked **LATER** in the UI:

- DePIN integrations (Helium, Grass)
- Liquid staking ecosystem
- DeFi routing interfaces
- Multi-wallet switcher
- Decentralized storage UI
- Marketplace or remittance flows
- System-wide interceptor UI
- Biometric unlock
- Localization dropdown
- Backup/restore flows

Each LATER feature includes source code comment: `// LATER: <reason>`

---

## 14. References

- **Color references:** Dark obsidian (#0B1020), Navy (#121A2B), Mint (#3DDC97), Cyan (#7FE7F0), Purple (#8B7CFF)
- **Typography:** Sans-serif for all text, monospace for hashes/addresses only
- **Spacing:** 8dp grid system, 16dp gutters, 12-20px padding on cards
- **Border radius:** 8dp for inputs, 12-16dp for cards, 44dp for icon buttons
- **Shadows:** Subtle (6-12px blur, 20% opacity)
- **Animations:** 200-300ms transitions, ease-out timing
- **Icons:** Outline style, 24dp base size, strokeWidth 2dp

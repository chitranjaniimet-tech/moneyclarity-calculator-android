# MoneyClarity Calc

[![Android build](https://github.com/chitranjaniimet-tech/moneyclarity-calculator-android/actions/workflows/android-build.yml/badge.svg)](https://github.com/chitranjaniimet-tech/moneyclarity-calculator-android/actions/workflows/android-build.yml)

An offline Android financial calculator for working out what borrowed money
actually costs—and for checking common saving, tax and planning calculations.

Package: `com.moneyclarity.calc` · Version 1.2.1 · Kotlin · Jetpack Compose · Material 3
· Android 8–16+ (API 26–36)

---

## What it does

| Screen | Purpose |
|---|---|
| **Effective cost** | Converts a flat quote, processing fees, bundled insurance and advance instalments into the annual rate actually being paid. Includes a separate mode for plans advertised as interest free. |
| Instalment | Solve for EMI, tenure, rate or eligible amount from the other three figures. |
| Prepayment | Simulates a lump sum or a recurring extra, and compares cutting the tenure against cutting the instalment. Reports the instalment number at which the outlay is recovered. |
| Compare quotes | Up to three quotes ranked on total money paid out rather than on instalment size. |
| Schedule | Compact repayment tables by month or Indian financial year, actual first-period interest, and a complete dated CSV export. |
| Saved | Loan plans and result snapshots kept on the device. |
| Settings | Appearance and app details. |

The app declares **no permissions at all**, including no internet permission. Nothing can leave the phone.

It is a calculator. It performs arithmetic on figures the user types in. It does not offer credit, name lenders, or recommend a course of action.

## Version 1.2.1 presentation

- Removed the permanent bottom bar and replaced it with a compact elevated
  publisher chip floating over the content.
- The chip automatically leaves the screen while typing and whenever a
  calculated result or repayment schedule is visible.
- Added a native Android launch splash, refreshed app icon and short animated
  MoneyClarity brand reveal.

## Version 1.2.0 refinement

- Replaced the sepia/green monochrome interface with a clean neutral canvas,
  blue actions, violet findings, category accents and a purpose-built dark
  palette.
- Corrected reverse-tenure rounding: a whole-rupee EMI that represents a
  240-month loan now resolves to 240 months, not 241.
- Made that rounding rule flow through the repayment schedule so the final
  balance still reconciles exactly.
- Replaced repayment pills with compact aligned tables for monthly and
  financial-year views.
- Added Save result to every calculator flow; Saved now supports both reopenable
  loans and general result snapshots.
- Rebuilt the publisher footer as a compact pill and selected its light/dark
  wordmark from the app theme, not the phone theme.
- Removed the remaining hidden sliders. Figures use direct entry and precise
  step controls throughout.

## Version 1.1.0 reliability work

- Rebuilt the repayment schedule as one lazy mobile list, eliminating the
  zero-height nested table that could make calculated rows invisible.
- Corrected the CSV schema and added the missing payment column.
- Preserved a user-entered EMI when tenure is solved instead of silently
  deriving a different EMI from a rounded month count.
- Added correct broken-period schedules for both standard and fixed-payment
  loans.
- Hid the publisher footer while typing and made the content consume IME
  insets, keeping focused fields above the Android keyboard.
- Added Done actions to shared numeric fields.
- Corrected prepayment charge clamping and break-even arithmetic.
- Added unit tests for schedule reconciliation, zero-rate loans, reverse
  solvers, broken-period interest, financial-year grouping and prepayments.
- Upgraded compile and target SDK to Android 16 (API 36).

---

## Building it without a computer

Everything runs on GitHub's servers. You need only a browser.

### 1. Create the repository

Make a new empty repository on GitHub, then upload the contents of this zip. Keep the folder structure exactly as it is — `app/` and `.github/` must sit at the repository root, next to `settings.gradle.kts`.

### 2. First build

Every pull request and push to `main` runs the **Build** workflow. It produces a
debug APK under the run's **Artifacts** section for installation and testing.

There is no Gradle wrapper committed to this repository. The workflow generates it on the runner, which keeps the repo free of binary files so it stays editable from a phone.

### 3. Create your signing key — once, and only once

Go to **Actions → Create keystore → Run workflow**. Give it an alias and a password you will not forget.

Download the artifact it produces. It contains two files:

- `release.jks` — **store this somewhere permanent.** If you lose it you can never publish an update to the same Play listing again.
- `keystore_base64.txt` — the same key as text.

Then go to **Settings → Secrets and variables → Actions** and add four repository secrets:

| Secret | Value |
|---|---|
| `KEYSTORE_BASE64` | the entire contents of `keystore_base64.txt` |
| `KEYSTORE_PASSWORD` | the password you chose |
| `KEY_ALIAS` | the alias you chose |
| `KEY_PASSWORD` | the same password |

Delete the workflow run afterwards so the artifact does not linger.

### 4. Produce a release build for Play

Create a tag beginning with `v`, for example `v1.2.0`. The Build workflow then produces a signed `.aab` under Artifacts. That is the file Play Console accepts.

To raise the version later, edit `versionCode` and `versionName` in `app/build.gradle.kts` and tag again. `versionCode` must increase with every upload.

---

## Verification

The calculation engine in `app/src/main/java/com/moneyclarity/calc/engine/` was compiled and executed independently of the app, and its output cross-checked against a separate implementation. Confirmed:

- The amortisation schedule closes to a balance of exactly zero, and principal repaid equals the amount borrowed to within a rounding paisa.
- Instalment × count equals principal plus total interest.
- A flat 8% over five years resolves to 14.125% on reducing balance.
- A zero rate falls back to simple division rather than dividing by zero.
- Cutting the tenure always saves more interest than cutting the instalment, for the same lump sum.
- With no extra payment entered, the prepayment simulator reports zero saved and the original tenure.
- Financial year interest sums back to the schedule total.

---

## Notes on the design

The palette uses a cool neutral canvas with blue for actions, violet for
findings, emerald for positive movement and distinct category accents. The
MoneyClarity green is reserved for the publisher wordmark, so the entire
interface no longer reads as a single monochrome brand block.

No font files are bundled, so the repository stays entirely text and can be edited from a phone. Figures are set with tabular numerals so digits do not shift as values change.

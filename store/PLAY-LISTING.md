# MoneyClarity Calc — Play Console submission pack

Everything below is ready to paste. Character counts are checked against Play's
limits. Where I am not certain of the current form wording, I say so rather than
guessing — read the actual options in Console and pick the honest one.

---

## 1. Store listing

**App name** (limit 30) — 17 characters

```
MoneyClarity Calc
```

**Short description** (limit 80) — pick one

```
See the rate you are really paying. 28 calculators, zero permissions.
```
69 characters. Leads with the flagship.

```
Loan and investment maths, worked out offline. No account, no permissions.
```
74 characters. Leads with the privacy position.

I would use the first. The effective-cost angle is the thing no competitor
says, and the permissions line already has the whole "No permissions. Not one."
section of the long description behind it.

**Full description** — 2,699 of 4,000 characters. In `full-description.txt`,
reproduced here:

---

MoneyClarity Calc answers the question most calculators skip. Not "what is my instalment", but "what is this actually costing me".

THE RATE BEHIND THE QUOTE

A quote of "8% flat" is not 8%. Enter the flat rate, the processing fee, any bundled insurance and any instalments taken in advance, and the app resolves all of it into the annual reducing-balance rate you are really paying. A typical 8% flat vehicle quote comes out a little over 14%.

LOAN TOOLS

- Effective cost: the real annual rate behind a flat quote, with fees and add-ons folded in. Handles no-cost EMI offers too.
- Instalment: solve for whichever figure you do not know. Fix the amount, rate and tenure to get the instalment. Or fix the instalment to get the tenure, the interest rate, or how much that instalment would actually borrow.
- Prepayment: cut the tenure or cut the instalment, with the break-even point between them.
- Compare quotes: ranked on total outgo, not on whose instalment looks smallest.
- Schedule: a dated month-by-month table, a financial-year view, and CSV export.

28 MORE CALCULATORS

Investing: monthly plan, target-first planning, lump sum, withdrawal plan, CAGR, step-up, financial independence.
Deposits: FD, RD, PPF, girl child scheme, savings certificate, monthly income schemes.
Work: provident fund, gratuity, HRA exemption, NPS, retirement corpus, take-home pay.
Tax: income tax new against old for tax year 2026-27, capital gains.
Borrowing: borrowing capacity, credit card payoff, renting against buying.
Everyday: simple and compound interest, GST, inflation.

BUILT FOR INDIAN FIGURES

Amounts group the Indian way, so 5000000 reads as 50,00,000 instead of something you have to count on the screen. Tenures read in years and months. Schedules follow the April to March financial year.

NO PERMISSIONS. NOT ONE.

The app declares no permissions at all, including no internet access, so Android itself stops it from opening a network connection. There is no account, no sign-in, no analytics, no tracking and no advertising. You do not have to take that on trust: open App permissions on this page and it reads "No permissions required".

WHAT THIS APP IS NOT

This is a calculator, not an adviser. It does arithmetic on figures you type in. It does not offer credit, arrange or facilitate a loan, introduce you to any lender, or tell you whether to borrow, invest or save. No lender is named anywhere in it. Results are estimates for your own information, and the figures a provider actually quotes will govern any real agreement.

WORKING PRACTICE

Every formula is executed and cross-checked against published figures before it ships, rather than assumed correct because it looks right.

---

The "WHAT THIS APP IS NOT" section is not filler. A home loan calculator being
flagged under the Financial Services policy is a documented occurrence, and if a
reviewer opens your listing that paragraph is the fastest way for them to see
which side of the line you are on.

---

## 2. Graphics

| Asset | File | Spec |
|---|---|---|
| App icon | `icon-512.png` | 512×512, 32-bit PNG, no alpha |
| Feature graphic | `feature-graphic-1024x500.png` | 1024×500, no alpha |

Both are drawn from the same coordinates as the launcher vector in the repo, so
the store icon and the installed icon cannot drift apart. No rounded corners or
shadow are baked in — Play applies its own masking and would double it up.

**Screenshots you still have to capture** (2 minimum, 8 maximum, phone).
Take them on your own device once the closed-test build is installed:

1. Effective cost, with a flat quote resolved — this is the whole pitch.
2. All calculators, showing the new grid.
3. Instalment with the Tenure or Rate mode selected — proves the four-way
   solving that nothing else in the category does.
4. Schedule, financial year view.
5. Settings, so the no-permissions claim is visible in-app.

Capture in both light and dark if you want to show the theme, but keep the set
consistent — do not alternate.

---

## 3. App content declarations

**Privacy policy URL.** Host `privacy-policy.html` on your domain and give Play
that address. Replace `REPLACE_WITH_YOUR_EMAIL` in the file first. Suggested
location: `https://moneyclaritytech.com/privacy-policy.html`

**Data safety.**

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **No** |

That is the whole form. Saved calculations and your theme preference live in the
app's private storage and never leave the device, and Google's definition of
collection is data *transmitted off the device*. Local-only storage is not
collection. There is no follow-up section once you answer No.

**Ads.** Contains ads: **No**.

**Content rating questionnaire.** Category: *Utility, Productivity,
Communication or Other*. Then No to every content question — violence, sexuality,
language, controlled substances, crude humour, gambling, horror. Also No to:
shares location, allows user interaction, allows purchases, contains
user-generated content. Expected outcome: Everyone / PEGI 3 / IARC 3+.

**Target audience.** Select **18 and over only**. Ticking any bracket under 18
pulls you into the Families programme with its own extra requirements, and there
is no reason for a loan calculator to be there.

**News app:** No. **COVID-19 app:** No. **Government app:** No.

**Financial features declaration.** You must complete this — it applies to apps
on closed testing, not just production.

My reading: this app has no financial features to declare. Play defines
financial products and services as those related to the *management or
investment* of money, including personalised advice. Yours manages no money,
holds no account, moves no funds, and gives no advice. Critically, it is neither
a *personal loan direct lender* nor a *loan facilitator*, which are the two
categories that would drag in licensing documentation.

Two honest caveats. First, I cannot see the current form, and the option
wording changes — read what is actually on screen and choose the accurate
answer rather than the one I predicted. Second, calculators in this category do
sometimes get flagged anyway. If that happens, the reply writes itself: no
lender is named in the app, there is no apply path, no offer, no referral, no
network access at all, and no money can move through it.

You work at a bank. Whatever internal disclosure or outside-activity approval
that implies is your call, not mine, but it is worth settling before the listing
is public rather than after.

---

## 4. Release sequence

**Step 1 — create the signing key.** Actions → *Create keystore* → Run workflow.
Alias `moneyclarity`. It no longer asks for a password: I changed it to generate
one on the runner, because a `workflow_dispatch` input is shown in the run
summary and your signing password would have sat in the Actions history for
anyone with repo access to read.

Download the artifact. It contains `release.jks`, `keystore_base64.txt`, and
`SECRETS-READ-ME.txt` with the generated password.

**Store `release.jks` somewhere permanent and private before doing anything
else.** Losing it means you can never update this listing again — not a new
version, not a bug fix, ever. A new key means a new listing and no way to carry
existing installs across. Put it somewhere that survives losing your phone.

**Step 2 — add four repository secrets.** Settings → Secrets and variables →
Actions → New repository secret:

- `KEYSTORE_BASE64` — the entire contents of `keystore_base64.txt`, one line
- `KEYSTORE_PASSWORD` — from the read-me
- `KEY_ALIAS` — `moneyclarity`
- `KEY_PASSWORD` — the same password

Then delete the downloaded artifact from the Actions run.

**Step 3 — tag the release.** Tagging `v1.1.0` triggers the signed bundle:

```
git tag v1.1.0 && git push origin v1.1.0
```

The `.aab` appears under that run's Artifacts. `versionCode` is 1 and
`versionName` is 1.1.0 and `versionCode` is 2. Increase the code for every later Play upload.

**Step 4 — upload.** Play Console → Test and release → Closed testing → create
track → upload the `.aab` → release notes → add testers → submit.

---

## 5. What the closed-testing gate actually requires

If your developer account is a **personal** account created after
13 November 2023, you need at least **12 testers opted in continuously for
14 days** before you can apply for production access. Organisation accounts
registered to a legal business entity are exempt. Check which yours is now — it
determines whether your launch is two weeks out or a few days.

Points that catch people:

- The 14-day clock starts only once the release is approved *and* 12 testers
  have opted in. Not when you upload.
- Only closed testing counts. Internal testing does nothing for this, however
  many people you add.
- Testers must be distinct Google accounts on real devices, joined through your
  opt-in link. Emulators and duplicate accounts do not count.
- Recruit more than 12. If someone opts out and you drop below the threshold,
  the streak is affected, so a buffer of 15 to 20 is worth the effort.
- Review of the closed-testing release itself takes roughly 1 to 3 days on a
  new account, on top of the 14.

Sources are Play Console Help on app testing requirements; the specifics have
moved before (it was 20 testers until December 2024), so confirm against
Console rather than against this document if anything looks off.

---

## 6. Still outstanding

- Screenshots — needs a device, only you can do this.
- `REPLACE_WITH_YOUR_EMAIL` in the privacy policy.
- Privacy policy actually hosted and reachable at a public URL.
- Not tested on a real phone beyond debug installs. Closed testing is the right
  call; I would not go straight to production on a build neither of us has run.
- PDF export still deferred. Schedule exports CSV only.
- Tax slabs are TY 2026-27. Recheck every February after the Union Budget.

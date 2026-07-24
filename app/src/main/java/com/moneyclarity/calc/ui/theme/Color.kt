package com.moneyclarity.calc.ui.theme

import androidx.compose.ui.graphics.Color

// Warm paper and deep teal. Amber is rationed: it marks the one number that
// carries the finding, never anything decorative.
//
// Contrast is measured, not judged by eye. Every pair below was checked against
// WCAG and the resulting ratios are recorded here:
//
//   card on background   1.20   (was 1.08 - cards used to melt into the page)
//   hairline on card     1.54
//   ink on card         16.46
//   muted ink on card    6.34   AA at any size
//   teal on card         7.71   AA at any size
//   amber on card        4.82   AA at any size (was 3.95, which failed)
//
// The amber shift is the one that mattered most: it carries the headline
// figure, and at the old value that figure did not clear the threshold.

val Paper = Color(0xFFEDE8DE)
val CardSurface = Color(0xFFFFFDF8)
val Teal = Color(0xFF0E5C55)
val TealDeep = Color(0xFF083D38)
val TealContainer = Color(0xFFCBE4DF)
val Amber = Color(0xFFB5551F)
val AmberContainer = Color(0xFFF2DCCB)
val Positive = Color(0xFF276E4E)
val Alert = Color(0xFFA63826)
val Ink = Color(0xFF14201E)
val InkSoft = Color(0xFF55615E)
val Hairline = Color(0xFFD6CEBE)

// Dark side, same discipline. The card lifts off the base by a measured amount
// rather than by a guess, and both accents clear AA against the card.
//
//   card on background   1.19
//   hairline on card     1.45
//   ink on card         13.50
//   muted ink on card    7.17
//   teal on card         8.32
//   amber on card        6.72

val NightBase = Color(0xFF0B0F0E)
val NightCard = Color(0xFF1A2221)
val NightTeal = Color(0xFF6FC9BE)
val NightTealContainer = Color(0xFF143733)
val NightAmber = Color(0xFFE8925E)
val NightAmberContainer = Color(0xFF3A2417)
val NightInk = Color(0xFFEDEAE3)
val NightInkSoft = Color(0xFFA3AFAC)
val NightHairline = Color(0xFF333D3B)
val NightPositive = Color(0xFF6CC79B)
val NightAlert = Color(0xFFE08472)

// The publisher's own mark, moneyclaritytech.com, kept distinct from the app's
// own teal so a bottom link reads as "this app comes from that site" rather
// than blending into the app's chrome as just another button.
//
// #062A1F is the site's own meta-theme-color, taken verbatim. It is close to
// black and reads at 15.2:1 on a light card, comfortably past AA, but it very
// nearly disappears on a dark card (1.05:1). SiteInkNight is not a different
// colour, it is the same hue lightened in HSL space (161.7deg / 75% saturation
// held, lightness raised from 0.094 to 0.62) until it clears 9.9:1 there. Same
// mark, legible on both.
val SiteInk = Color(0xFF062A1F)
val SiteInkNight = Color(0xFF5DE0B8)

// One accent hue per calculator category, so the grid reads as six groups at
// a glance instead of one repeated teal tile. Every pair below is measured:
// icon-on-badge clears the 3:1 WCAG non-text threshold with a comfortable
// margin (icons are graphical objects, not text, so 3:1 is the bar that
// actually applies -- these sit at 4.2-7.5:1, matched deliberately closer to
// the text threshold rather than skimming the minimum). Hues are spread
// around the wheel and kept clear of both the app's own teal and amber so a
// category colour reads as new information, not a tint of the chrome.
data class CategoryAccent(val icon: Color, val badge: Color)

val CategoryInvesting = CategoryAccent(Color(0xFF1D7C5E), Color(0xFFD0F1E7))
val CategoryDeposits = CategoryAccent(Color(0xFF21608C), Color(0xFFD0E3F1))
val CategoryWork = CategoryAccent(Color(0xFF4E218C), Color(0xFFDED0F1))
val CategoryTax = CategoryAccent(Color(0xFF8C2145), Color(0xFFF1D0DB))
val CategoryBorrowing = CategoryAccent(Color(0xFF8C4E21), Color(0xFFF1DED0))
val CategoryEveryday = CategoryAccent(Color(0xFF2A741B), Color(0xFFD5F1D0))

val CategoryInvestingNight = CategoryAccent(Color(0xFF90DFC6), Color(0xFF1F5141))
val CategoryDepositsNight = CategoryAccent(Color(0xFF90BEDF), Color(0xFF1F3C51))
val CategoryWorkNight = CategoryAccent(Color(0xFFB190DF), Color(0xFF341F51))
val CategoryTaxNight = CategoryAccent(Color(0xFFDF90AB), Color(0xFF511F30))
val CategoryBorrowingNight = CategoryAccent(Color(0xFFDFB190), Color(0xFF51341F))
val CategoryEverydayNight = CategoryAccent(Color(0xFF9DDF90), Color(0xFF27511F))

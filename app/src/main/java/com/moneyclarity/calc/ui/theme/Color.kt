package com.moneyclarity.calc.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * A contemporary neutral canvas with blue as the action colour, violet for
 * findings and emerald for positive movement. The MoneyClarity green remains
 * in the publisher wordmark instead of tinting every control on every screen.
 */
val Paper = Color(0xFFF4F7FC)
val CardSurface = Color(0xFFFFFFFF)
val Teal = Color(0xFF2563EB)
val TealDeep = Color(0xFF173A73)
val TealContainer = Color(0xFFE7EFFF)
val Amber = Color(0xFF7C3AED)
val AmberContainer = Color(0xFFF0E9FF)
val Positive = Color(0xFF0F8A68)
val Alert = Color(0xFFBA1A1A)
val Ink = Color(0xFF172033)
val InkSoft = Color(0xFF5C667A)
val Hairline = Color(0xFFDCE3EE)

val NightBase = Color(0xFF0B1020)
val NightCard = Color(0xFF141C2F)
val NightTeal = Color(0xFF8AB4FF)
val NightTealContainer = Color(0xFF203C6E)
val NightAmber = Color(0xFFC5A6FF)
val NightAmberContainer = Color(0xFF38265F)
val NightInk = Color(0xFFF3F6FC)
val NightInkSoft = Color(0xFFADB7CA)
val NightHairline = Color(0xFF2D3850)
val NightPositive = Color(0xFF63D7B2)
val NightAlert = Color(0xFFFFB4AB)

val SiteInk = Color(0xFF062A1F)
val SiteInkNight = Color(0xFF5DE0B8)

data class CategoryAccent(val icon: Color, val badge: Color)

val CategoryInvesting = CategoryAccent(Color(0xFF087F5B), Color(0xFFD9F7EC))
val CategoryDeposits = CategoryAccent(Color(0xFF2563EB), Color(0xFFE4EDFF))
val CategoryWork = CategoryAccent(Color(0xFF7C3AED), Color(0xFFF0E7FF))
val CategoryTax = CategoryAccent(Color(0xFFC0265E), Color(0xFFFFE4EE))
val CategoryBorrowing = CategoryAccent(Color(0xFFCF5B16), Color(0xFFFFE9DA))
val CategoryEveryday = CategoryAccent(Color(0xFF397A25), Color(0xFFE5F5DE))

val CategoryInvestingNight = CategoryAccent(Color(0xFF70DFBC), Color(0xFF173E35))
val CategoryDepositsNight = CategoryAccent(Color(0xFF8AB4FF), Color(0xFF203C6E))
val CategoryWorkNight = CategoryAccent(Color(0xFFC5A6FF), Color(0xFF38265F))
val CategoryTaxNight = CategoryAccent(Color(0xFFFF9ABA), Color(0xFF572039))
val CategoryBorrowingNight = CategoryAccent(Color(0xFFFFB77E), Color(0xFF56301C))
val CategoryEverydayNight = CategoryAccent(Color(0xFFA6D88F), Color(0xFF294424))

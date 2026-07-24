package com.moneyclarity.calc.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Elderly
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.isSystemInDarkTheme
import com.moneyclarity.calc.data.CalculatorSpec
import com.moneyclarity.calc.data.Calculators
import com.moneyclarity.calc.ui.components.Eyebrow
import com.moneyclarity.calc.ui.components.haptics
import com.moneyclarity.calc.ui.theme.CategoryAccent
import com.moneyclarity.calc.ui.theme.CategoryBorrowing
import com.moneyclarity.calc.ui.theme.CategoryBorrowingNight
import com.moneyclarity.calc.ui.theme.CategoryDeposits
import com.moneyclarity.calc.ui.theme.CategoryDepositsNight
import com.moneyclarity.calc.ui.theme.CategoryEveryday
import com.moneyclarity.calc.ui.theme.CategoryEverydayNight
import com.moneyclarity.calc.ui.theme.CategoryInvesting
import com.moneyclarity.calc.ui.theme.CategoryInvestingNight
import com.moneyclarity.calc.ui.theme.CategoryTax
import com.moneyclarity.calc.ui.theme.CategoryTaxNight
import com.moneyclarity.calc.ui.theme.CategoryWork
import com.moneyclarity.calc.ui.theme.CategoryWorkNight
import com.moneyclarity.calc.ui.theme.NumberSmall

/**
 * Twenty-eight entries is past the point where a stacked list of full-width
 * rows works. Each row carried a title and a sentence, so roughly six fitted on
 * a screen, every one of them the same shape, and finding anything meant
 * reading rather than looking.
 *
 * The grid trades the descriptive sentence for a mark and a shorter scan line.
 * Twelve tiles reach the eye at once instead of six, the icon gives each entry
 * a silhouette to recognise on the second visit, and the sentence that used to
 * sit on the tile is still reachable: it is what the search box matches on, and
 * it is printed at the top of the calculator once opened.
 */

private val icons: Map<String, ImageVector> = mapOf(
    "sip" to Icons.Filled.Savings,
    "sip_target" to Icons.Filled.Flag,
    "lumpsum" to Icons.Filled.Paid,
    "swp" to Icons.Filled.TrendingDown,
    "cagr" to Icons.Filled.ShowChart,
    "stepup" to Icons.Filled.TrendingUp,
    "fire" to Icons.Filled.Star,
    "fd" to Icons.Filled.AccountBalance,
    "rd" to Icons.Filled.CalendarMonth,
    "ppf" to Icons.Filled.Shield,
    "ssy" to Icons.Filled.ChildCare,
    "nsc" to Icons.Filled.Description,
    "payout" to Icons.Filled.Payments,
    "epf" to Icons.Filled.Work,
    "gratuity" to Icons.Filled.Redeem,
    "hra" to Icons.Filled.Home,
    "nps" to Icons.Filled.Elderly,
    "retirement" to Icons.Filled.PieChart,
    "takehome" to Icons.Filled.AccountBalanceWallet,
    "tax" to Icons.Filled.ReceiptLong,
    "capgains" to Icons.Filled.Balance,
    "eligibility" to Icons.Filled.Calculate,
    "card" to Icons.Filled.CreditCard,
    "rentbuy" to Icons.Filled.HomeWork,
    "simple" to Icons.Filled.Percent,
    "compound" to Icons.Filled.Timeline,
    "gst" to Icons.Filled.ShoppingCart,
    "inflation" to Icons.Filled.TrendingUp
)

private fun iconFor(id: String): ImageVector = icons[id] ?: Icons.Filled.Calculate

/** One accent per group, so tiles read as six colour-coded categories. */
@Composable
private fun accentFor(group: String): CategoryAccent {
    val dark = isSystemInDarkTheme()
    return when (group) {
        "Investing" -> if (dark) CategoryInvestingNight else CategoryInvesting
        "Deposits" -> if (dark) CategoryDepositsNight else CategoryDeposits
        "Work" -> if (dark) CategoryWorkNight else CategoryWork
        "Tax" -> if (dark) CategoryTaxNight else CategoryTax
        "Borrowing" -> if (dark) CategoryBorrowingNight else CategoryBorrowing
        else -> if (dark) CategoryEverydayNight else CategoryEveryday
    }
}

@Composable
fun CalculatorsScreen(onOpen: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val tap = haptics()

    val needle = query.trim().lowercase()
    val matches = remember(needle) {
        if (needle.isEmpty()) emptyList()
        else Calculators.all.filter {
            it.title.lowercase().contains(needle) ||
                it.blurb.lowercase().contains(needle) ||
                it.group.lowercase().contains(needle)
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(2) }, key = "search") {
            SearchField(query, { query = it }) { query = ""; tap.select() }
        }

        if (needle.isEmpty()) {
            Calculators.groups.forEach { group ->
                val specs = Calculators.all.filter { it.group == group }
                item(span = { GridItemSpan(2) }, key = "head_$group") {
                    GroupHeader(group, specs.size)
                }
                items(specs, key = { it.id }) { spec ->
                    CalculatorTile(spec, accentFor(spec.group)) { tap.select(); onOpen(spec.id) }
                }
            }
        } else if (matches.isEmpty()) {
            item(span = { GridItemSpan(2) }, key = "empty") {
                Column(Modifier.padding(top = 28.dp)) {
                    Text(
                        "Nothing matches \"${query.trim()}\".",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Try a plainer word: deposit, tax, rent, retirement, interest.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            item(span = { GridItemSpan(2) }, key = "head_results") {
                GroupHeader(if (matches.size == 1) "1 match" else "${matches.size} matches", null)
            }
            items(matches, key = { it.id }) { spec ->
                CalculatorTile(spec, accentFor(spec.group)) { tap.select(); onOpen(spec.id) }
            }
        }
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit, onClear: () -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        placeholder = {
            Text(
                "Search 28 calculators",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingIcon = {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Clear search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedBorderColor = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun GroupHeader(label: String, count: Int?) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Eyebrow(label)
        if (count != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                count.toString(),
                style = NumberSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Fixed height so every row lines up. The title runs to three lines because a
 * few of them genuinely need it, and clipping the name of a calculator to fit a
 * layout would be the wrong way round.
 */
@Composable
private fun CalculatorTile(spec: CalculatorSpec, accent: CategoryAccent, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .height(138.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(accent.badge),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    iconFor(spec.id),
                    contentDescription = null,
                    tint = accent.icon,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                spec.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

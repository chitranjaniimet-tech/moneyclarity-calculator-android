package com.moneyclarity.calc.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.moneyclarity.calc.engine.monthsToTenure
import com.moneyclarity.calc.engine.percent
import com.moneyclarity.calc.engine.rupees
import com.moneyclarity.calc.engine.scaleHint
import com.moneyclarity.calc.ui.theme.EyebrowStyle
import com.moneyclarity.calc.ui.theme.NumberHero
import com.moneyclarity.calc.ui.theme.NumberLarge
import com.moneyclarity.calc.ui.theme.NumberMedium
import com.moneyclarity.calc.ui.theme.NumberSmall

@Composable
fun Eyebrow(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text.uppercase(), style = EyebrowStyle, color = color)
}

@Composable
fun SectionCard(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 0.5.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.78f)
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            if (title != null) {
                Eyebrow(title)
                Spacer(Modifier.height(12.dp))
            }
            content()
        }
    }
}

@Composable
fun ToggleRow(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Feedback is fired here rather than at each call site, so every segmented
    // control in the app behaves identically and no caller can forget it.
    val tap = haptics()
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.8f),
                RoundedCornerShape(14.dp)
            )
            .padding(4.dp)
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { tap.select(); onSelect(index) }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    option,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun HeroResult(
    label: String,
    value: String,
    caption: String? = null,
    accent: Boolean = false
) {
    val color = if (accent) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
    Column(Modifier.fillMaxWidth()) {
        Eyebrow(label)
        Spacer(Modifier.height(6.dp))
        Text(value, style = NumberHero, color = color)
        if (caption != null) {
            Spacer(Modifier.height(4.dp))
            Text(caption, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun StatRow(
    label: String,
    value: String,
    emphasis: Boolean = false,
    valueColor: Color? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = if (emphasis) NumberLarge else NumberMedium,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun HairlineDivider() {
    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
}

/**
 * The signature element: quoted rate on the left in quiet ink, the rate actually
 * being paid on the right in amber, and a bar whose filled portion is the gap
 * between them. It is the one place amber is allowed to appear.
 */
@Composable
fun RevealBar(
    quoted: Double,
    actual: Double?,
    modifier: Modifier = Modifier
) {
    val target = if (actual == null || actual <= 0.0) 0f
    else ((actual - quoted) / actual).toFloat().coerceIn(0f, 1f)
    val fill by animateFloatAsState(targetValue = target, animationSpec = tween(600), label = "gap")

    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Eyebrow("They quoted")
                Spacer(Modifier.height(4.dp))
                Text(percent(quoted), style = NumberLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Eyebrow("You actually pay", MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (actual == null) "—" else percent(actual),
                    style = NumberHero,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(1f - fill)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        if (actual != null && actual - quoted > 0.05) {
            Spacer(Modifier.height(10.dp))
            Text(
                "The gap is ${percent(actual - quoted)} a year.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

/** Stacked bar showing how much of the total outgo is principal and how much is interest. */
@Composable
fun SplitBar(principal: Double, interest: Double) {
    val total = (principal + interest).coerceAtLeast(1.0)
    val pShare by animateFloatAsState(
        targetValue = (principal / total).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(500),
        label = "split"
    )
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
        ) {
            Box(
                Modifier
                    .weight(pShare.coerceAtLeast(0.001f))
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary)
            )
            Box(
                Modifier
                    .weight((1f - pShare).coerceAtLeast(0.001f))
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.secondary)
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            LegendDot("Principal", MaterialTheme.colorScheme.primary, rupees(principal))
            LegendDot("Interest", MaterialTheme.colorScheme.secondary, rupees(interest))
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color, value: String) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(2.dp))
        Text(value, style = NumberMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}


/**
 * The submit affordance every calculator screen now shares: enter figures,
 * press this, see the working underneath. The label switches to "Recalculate"
 * once a result exists, so it is honest about what a second press will do
 * rather than repeating the same word for two different states.
 *
 * Deliberately a filled button and not outlined -- everything else on the
 * input card is outlined or plain text, so this is the one shape on the
 * screen that clearly says "this is the action to take."
 */
@Composable
fun CalculateButton(hasResult: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tap = haptics()
    val keyboard = LocalSoftwareKeyboardController.current
    Button(
        onClick = {
            tap.commit()
            keyboard?.hide()
            onClick()
        },
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Icon(
            Icons.Filled.Calculate,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            if (hasResult) "Recalculate" else "Calculate",
            style = MaterialTheme.typography.titleMedium
        )
    }
}

/** One consistent Save + Share row for every completed calculation. */
@Composable
fun ResultActions(
    saved: Boolean,
    onSave: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tap = haptics()
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = {
                tap.commit()
                onSave()
            },
            enabled = !saved,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(if (saved) "Saved" else "Save result")
        }
        OutlinedButton(
            onClick = {
                tap.select()
                onShare()
            },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Share")
        }
    }
}

/**
 * Sits where a result would go before the first calculation. Point of this
 * card rather than leaving blank space: an empty gap below the button reads
 * as a broken layout, not as "nothing to show yet."
 */
@Composable
fun CalculatePrompt(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.78f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(28.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Filled.Calculate,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Enter your figures and tap Calculate",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

package com.moneyclarity.calc.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.moneyclarity.calc.R

/**
 * The publisher's own mark: moneyclaritytech.com, not the app.
 *
 * These are the real logo files from the MoneyClarityTech brand kit, not a
 * redrawn approximation. Two are bundled -- one lettered for a light card, one
 * for a dark card -- because the wordmark's ink is close to black and
 * disappears against a dark surface if only one version is shipped. There is
 * no need for a bitmap per density bucket: this renders once, small, in a
 * single fixed place, so a single source scaled by Compose is indistinguishable
 * from a tiled set here and keeps the APK smaller.
 *
 * Deliberately used in exactly one place: the bottom bar. The brief was to
 * keep it minimal, and a mark repeated across every screen stops reading as
 * a signature and starts reading as decoration.
 */
@Composable
private fun wordmark() =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) R.drawable.mct_wordmark_on_dark
    else R.drawable.mct_wordmark_on_light

@Composable
fun BrandFooterBar(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tap = haptics()
    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.75f)
            ),
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .clickable { tap.select(); onClick() }
        ) {
            Row(
                Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(wordmark()),
                    contentDescription = "moneyclaritytech.com",
                    modifier = Modifier.height(17.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Free tools ↗",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Compact form for placement inside a card, e.g. the Settings About section. */
@Composable
fun BrandPill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tap = haptics()
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { tap.select(); onClick() }
    ) {
        Box(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Image(
                painter = painterResource(wordmark()),
                contentDescription = "moneyclaritytech.com",
                modifier = Modifier.height(20.dp)
            )
        }
    }
}

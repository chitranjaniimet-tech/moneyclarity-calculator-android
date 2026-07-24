package com.moneyclarity.calc.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.moneyclarity.calc.R

/** A short brand reveal after Android's native launch splash. */
@Composable
fun LaunchReveal() {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val scale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.82f,
        animationSpec = tween(650),
        label = "launch-mark"
    )
    val alpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(500),
        label = "launch-copy"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF081126),
                        Color(0xFF143A71),
                        Color(0xFF472477)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(300.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0x337CB5FF), Color.Transparent)
                    )
                )
        )
        Column(
            Modifier.padding(horizontal = 32.dp).alpha(alpha),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.splash_mark),
                contentDescription = null,
                modifier = Modifier.size(132.dp).scale(scale)
            )
            Spacer(Modifier.height(18.dp))
            Text(
                "MoneyClarity",
                color = Color.White,
                fontSize = 32.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.8).sp
            )
            Text(
                "CALC",
                color = Color(0xFFAFCBFF),
                fontSize = 12.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            )
            Spacer(Modifier.height(22.dp))
            Text(
                "Clarity is the best interest rate.",
                color = Color(0xFFD8E5FF),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

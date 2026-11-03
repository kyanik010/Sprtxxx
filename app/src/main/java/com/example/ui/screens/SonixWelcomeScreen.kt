package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

@Composable
fun SonixWelcomeScreen(modifier: Modifier = Modifier) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    val logoAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(1400, easing = FastOutSlowInEasing),
        label = "sonixLogoAlpha"
    )
    val logoScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.88f,
        animationSpec = tween(1600, easing = FastOutSlowInEasing),
        label = "sonixLogoScale"
    )
    val progress by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(4200, easing = FastOutSlowInEasing),
        label = "sonixWelcomeProgress"
    )
    val floatTransition = rememberInfiniteTransition(label = "sonixLogoFloat")
    val floatY by floatTransition.animateFloat(
        initialValue = 0f,
        targetValue = -7f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), repeatMode = androidx.compose.animation.core.RepeatMode.Reverse),
        label = "sonixFloatY"
    )
    val gold = Color(0xFFD4AF37)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF211A08), Color(0xFF090806), Color(0xFF030303)),
                    radius = 1050f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // A restrained gold halo behind the original, unmodified brand artwork.
        Box(
            Modifier
                .size(300.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x33D4AF37), Color(0x14D4AF37), Color.Transparent)
                    )
                )
        )
        Image(
            painter = painterResource(id = R.drawable.sonix_logo),
            contentDescription = "SONIX TV",
            modifier = Modifier
                .sizeIn(maxWidth = 300.dp, maxHeight = 300.dp)
                .fillMaxWidth(0.78f)
                .aspectRatio(1f)
                .offset(y = floatY.dp)
                .scale(logoScale)
                .alpha(logoAlpha)
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 76.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .width(196.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.10f))
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .align(Alignment.CenterEnd)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF997A00), gold, Color(0xFFFCE08B))
                            )
                        )
                )
            }
            Spacer(Modifier.height(15.dp))
            Text(
                text = "INITIALIZING SONIX TV",
                color = gold.copy(alpha = 0.78f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 3.sp
            )
        }
    }
}

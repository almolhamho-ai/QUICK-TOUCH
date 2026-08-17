package com.example.quickgestures.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AutoCalibrationScreen(
    onCalibrationComplete: (Float) -> Unit
) {
    var isCalibrating by remember { mutableStateOf(false) }
    var detectedValue by remember { mutableFloatStateOf(0f) }
    
    // أنميشن نبض دائرية للمعايرة
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isCalibrating) 1.3f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "المعايرة التلقائية والحساسية",
            style = MaterialTheme.typography.headlineSmall
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(200.dp)
                .scale(scale)
                .background(
                    color = if (isCalibrating) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape
                )
        ) {
            Text(
                text = if (isCalibrating) "قم بهز الهاتف الآن..." else "اضغط ابدأ المعايرة",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Button(
            onClick = {
                isCalibrating = true
                // محاكاة التقاط الحركة وتحديد الحساسية المناسبة
                detectedValue = 14.5f 
                onCalibrationComplete(detectedValue)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isCalibrating) "جاري المعايرة..." else "بدء المعايرة")
        }
    }
}

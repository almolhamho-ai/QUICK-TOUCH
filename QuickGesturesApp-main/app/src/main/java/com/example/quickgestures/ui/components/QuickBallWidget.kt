package com.example.quickgestures.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun QuickBallWidget() {
    var isExpanded by remember { mutableStateOf(false) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.padding(16.dp)
    ) {
        // الكرة الرئيسية
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable { isExpanded = !isExpanded },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FlashOn,
                contentDescription = "Quick Ball",
                tint = Color.White
            )
        }

        // قائمة الاختصارات المنبثقة (MIUI Style)
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.8f), CircleShape)
                    .padding(8.dp)
            ) {
                IconButton(onClick = { /* لقطة شاشة */ }) {
                    Icon(Icons.Default.Crop, contentDescription = "Screenshot", tint = Color.White)
                }
                IconButton(onClick = { /* قفل الشاشة */ }) {
                    Icon(Icons.Default.Lock, contentDescription = "Lock", tint = Color.White)
                }
                IconButton(onClick = { /* تشغيل الواي فاي */ }) {
                    Icon(Icons.Default.Wifi, contentDescription = "Wifi", tint = Color.White)
                }
            }
        }
    }
}

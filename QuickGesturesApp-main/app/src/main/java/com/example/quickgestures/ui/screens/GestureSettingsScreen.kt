package com.example.quickgestures.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun GestureSettingsScreen() {
    var shakeSensitivity by remember { mutableFloatStateOf(12f) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("إعدادات أزرار التحكم والإيماءات", style = MaterialTheme.typography.titleLarge)
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("حساسية هز الهاتف")
                    Slider(
                        value = shakeSensitivity,
                        onValueChange = { shakeSensitivity = it },
                        valueRange = 5f..25f
                    )
                }
            }
        }

        item {
            GestureItem(title = "النقر مرتين على ظهر الهاتف", action = "لقطة شاشة")
            GestureItem(title = "النقر ثلاث مرات على ظهر الهاتف", action = "تسجيل صوتي خفي")
            GestureItem(title = "النقر مرتين على زر خفض الصوت", action = "تشغيل الفلاش")
            GestureItem(title = "النقر مطولا على زر رفع الصوت", action = "قفل الشاشة")
            GestureItem(title = "النقر مرتين على زر الطاقة", action = "فتح التطبيق المفضّل")
        }
    }
}

@Composable
fun GestureItem(title: String, action: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(action, color = MaterialTheme.colorScheme.primary)
        }
    }
}

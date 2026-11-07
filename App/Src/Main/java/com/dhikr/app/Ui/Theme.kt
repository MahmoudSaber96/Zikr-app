package com.dhikr.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import java.text.NumberFormat
import java.util.Locale

val Gold = Color(0xFFC9A24B)
private val Light = lightColorScheme(
    primary = Color(0xFF1F6F5C), onPrimary = Color.White,
    background = Color(0xFFF6F1E7), onBackground = Color(0xFF1E2B28),
    surface = Color(0xFFFCF9F2), onSurface = Color(0xFF1E2B28),
    surfaceVariant = Color(0xFFEDE6D6), onSurfaceVariant = Color(0xFF5B6B66),
    outlineVariant = Color(0xFFDCD3BE), primaryContainer = Color(0xFFD5EBE2))
private val Dark = darkColorScheme(
    primary = Color(0xFF5FC4A8), onPrimary = Color(0xFF06241D),
    background = Color(0xFF0F1B1A), onBackground = Color(0xFFE8EFEA),
    surface = Color(0xFF172726), onSurface = Color(0xFFE8EFEA),
    surfaceVariant = Color(0xFF1F3331), onSurfaceVariant = Color(0xFF9DB3AC),
    outlineVariant = Color(0xFF2C4540), primaryContainer = Color(0xFF1B4A3F))

@Composable fun DhikrTheme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)

/** Arabic-Indic numerals: ٣٣ */
fun Number.ar(): String = NumberFormat.getInstance(Locale("ar", "EG")).format(this)

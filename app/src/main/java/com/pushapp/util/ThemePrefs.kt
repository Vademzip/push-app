package com.pushapp.util

import android.content.Context
import androidx.compose.ui.graphics.Color

data class AccentOption(val key: String, val label: String, val color: Color, val dim: Color)

object ThemePrefs {
    private const val PREFS = "theme_prefs"
    private const val KEY_ACCENT = "accent_key"

    val options = listOf(
        AccentOption("lime",    "Цвет Шрека",      Color(0xFFC9F135), Color(0xFF1E2D07)),
        AccentOption("pink",    "Цвет поросёнка",  Color(0xFFFFB3D1), Color(0xFF2D0718)),
        AccentOption("blue",    "Цвет спокойствия",Color(0xFF5B9CF6), Color(0xFF071A2D)),
        AccentOption("orange",  "Цвет козявки",    Color(0xFFFF9F43), Color(0xFF2D1507)),
        AccentOption("purple",  "Шёлковый путь",   Color(0xFFBB86FC), Color(0xFF1A0730)),
        AccentOption("teal",    "Цвет Тиффани",    Color(0xFF00D2D3), Color(0xFF071F20)),
    )

    fun getKey(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ACCENT, "lime") ?: "lime"

    fun setKey(context: Context, key: String) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_ACCENT, key).apply()

    fun getOption(context: Context): AccentOption =
        options.find { it.key == getKey(context) } ?: options[0]
}

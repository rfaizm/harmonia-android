package com.rfaizm.harmoniamusic.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.rfaizm.harmoniamusic.R

// Latin subset; non-Latin scripts fall back to the system font.
val Nunito = FontFamily(
    Font(R.font.nunito_400, FontWeight.Normal),
    Font(R.font.nunito_500, FontWeight.Medium),
    Font(R.font.nunito_600, FontWeight.SemiBold),
    Font(R.font.nunito_700, FontWeight.Bold),
    Font(R.font.nunito_800, FontWeight.ExtraBold),
    Font(R.font.nunito_900, FontWeight.Black),
)

private fun TextStyle.n() = copy(fontFamily = Nunito)

val Typography = with(Typography()) {
    Typography(
        displayLarge.n(), displayMedium.n(), displaySmall.n(),
        headlineLarge.n(), headlineMedium.n(), headlineSmall.n(),
        titleLarge.n(), titleMedium.n(), titleSmall.n(),
        bodyLarge.n(), bodyMedium.n(), bodySmall.n(),
        labelLarge.n(), labelMedium.n(), labelSmall.n(),
    )
}

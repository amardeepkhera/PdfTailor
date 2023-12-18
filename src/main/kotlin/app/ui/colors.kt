package app.ui

import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color

val graySurface = Color(0xFF2A2A2A)
val lightGray = Color(0xFFD3D3D3)
val green700 = Color(0xff388e3c)
val slackBlack = Color(0xff1E2228)

val DarkColorPalette = darkColors(
    primary = green700,
    primaryVariant = green700,
    secondary = Color.White,
    background = slackBlack,
    surface = slackBlack,
    onPrimary = slackBlack,
    onSecondary = lightGray,
    onBackground = Color.White,
    onSurface = Color.White,
    error = Color.Red,
)

val LightColorPalette = lightColors(
    primary = green700,
    primaryVariant = green700,
    secondary = Color.DarkGray,
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = graySurface,
    onBackground = slackBlack,
    onSurface = slackBlack
)
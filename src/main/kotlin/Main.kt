// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import app.ui.DarkColorPalette
import app.ui.LightColorPalette
import app.ui.homeScreen


fun main() = application {
    Window(
        title = "PDF Tailor",
        alwaysOnTop = false,
        onCloseRequest = ::exitApplication,
        state = WindowState(size = DpSize(875.dp, 475.dp)),
        resizable = false
    ) {

        MaterialTheme(colors = if (isSystemInDarkTheme()) DarkColorPalette else LightColorPalette) {
            homeScreen()
        }
    }
}
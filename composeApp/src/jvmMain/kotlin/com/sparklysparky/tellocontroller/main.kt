package com.sparklysparky.tellocontroller

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    val state = rememberWindowState(
        width = 1200.dp,
        height = 1000.dp,
    )

    Window(
        state = state,
        onCloseRequest = ::exitApplication,
        title = "Tello Controller",
    ) {
        App()
    }
}
package app

import java.awt.Desktop
import java.io.File


fun File.nameWithoutExtension() = name.substringBefore(".$extension")

fun File.appendToName(string: String) = "${nameWithoutExtension()}_$string.$extension"

fun File.open() = runCatching {
    if (Desktop.isDesktopSupported())
        Desktop.getDesktop().open(this)
}.getOrDefault(Unit)

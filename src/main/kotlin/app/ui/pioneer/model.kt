package app.ui.pioneer

import java.io.File

sealed class Element {
    data class Text(val value: String) : Element()
    data class Image(val ref: String) : Element()
}

data class Question(
    val no: String,
    val elements: List<Element>,
    val options: List<Element>,
    val sourceFile: File
) {
    fun hasImage() = elements.filterIsInstance<Element.Image>().any()
}
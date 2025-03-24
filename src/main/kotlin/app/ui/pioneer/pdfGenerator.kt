package app.ui.pioneer

import androidx.compose.ui.res.useResource
import app.withoutExtension
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.math.BigDecimal
import java.math.MathContext
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

private const val fontSize = 16f

private const val MAX_CHARS_IN_LINE = 60
private const val MAX_ROWS_IN_PAGE = 50
private val separator = StringBuilder("").run {
    repeat((1..MAX_CHARS_IN_LINE.plus(10)).count()) { append("-") }
    toString()
}

private data class DocumentState(
    val pdf: PDDocument = PDDocument(),
    var page: PDPage = pdf.addPage(),
    private val fontInputStream: InputStream,
    val font: PDType0Font = PDType0Font.load(pdf, fontInputStream),
    var contentStream: PDPageContentStream = newContentStream(pdf, page, font),
    val rowCounter: AtomicInteger = AtomicInteger(0),
) {
    fun addPageIfPageSizeIsExceeded() {
        if (rowCounter.get() > MAX_ROWS_IN_PAGE) {
            addPage()
        }
    }

    fun addPageIfPageSizeIsExceededOrQuestionHasAnImage(question: Question) {
        if (rowCounter.get() > MAX_ROWS_IN_PAGE || question.hasImage()) {
            addPage()
        }
    }

    private fun addPage() {
        page = pdf.addPage()
        contentStream.destroy()
        rowCounter.set(0)
        contentStream = newContentStream(pdf, page, font)
    }
}

private suspend fun <R> withNewDocument(d: suspend DocumentState.() -> R) =
    useResource("Roboto-VariableFont_wdth,wght.ttf") {
        val documentState = DocumentState(fontInputStream = it)
        d(documentState)
    }

suspend fun List<Question>.toPdf(): PDDocument = withNewDocument {
    forEach { question ->
        addPageIfPageSizeIsExceededOrQuestionHasAnImage(question)
        contentStream.showText(question.no + ") ")
        question.elements.forEach {
            when (it) {
                is Element.Text -> it.paragraph().forEach {
                    addPageIfPageSizeIsExceeded()
                    if (System.lineSeparator().equals(it)) {
                        contentStream.newLine(rowCounter)
                    } else {
                        contentStream.write(it, rowCounter)
                    }
                }

                is Element.Image -> it.load(question.sourceFile, contentStream, pdf, rowCounter)

            }
        }
        question.options.forEach {
            addPageIfPageSizeIsExceeded()
            val text = it as Element.Text
            if (it.value.isBlank()) {
                contentStream.newLine(rowCounter)
            } else {
                text.print(contentStream, rowCounter)
                contentStream.newLine(rowCounter)
            }
        }
        with(contentStream) {
            newLine(rowCounter)
            newLine(rowCounter)
            showText(separator)
            newLine(rowCounter)
            newLine(rowCounter)
        }
    }
    contentStream.destroy()
    pdf
}


//suspend fun List<Question>.toPdf(): PDDocument = useResource("Roboto-VariableFont_wdth,wght.ttf") {
//    val pdf = PDDocument()
//    val font = PDType0Font.load(pdf, it)
//    var page: PDPage? = null
//    var contentStream: PDPageContentStream? = null
//    val rowCounter = AtomicInteger(0)
//
//    forEach { question ->
//        when {
//            pdf.pages.count == 0 -> {
//                page = pdf.addPage()
//                contentStream = newContentStream(pdf, page!!, font)
//            }
//
//            rowCounter.get() >= MAX_ROWS_IN_PAGE || question.hasImage() -> {
//                page = pdf.addPage()
//                contentStream!!.destroy()
//                contentStream = newContentStream(pdf, page!!, font)
//                rowCounter.set(0)
//            }
//        }
//        contentStream!!.showText(question.no + ") ")
//        question.elements.forEach {
//            when (it) {
//                is Element.Text -> it.paragraph().forEach {
//                    newPageIfRequired(
//                        pdf,
//                        page!!,
//                        contentStream!!,
//                        font,
//                        rowCounter
//                    ) { pdPage: PDPage, pdPageContentStream: PDPageContentStream ->
//                        page = pdPage
//                        contentStream = pdPageContentStream
//                    }
//                    if (System.lineSeparator().equals(it)) {
//                        contentStream!!.newLine(rowCounter)
//                    } else {
//                        contentStream!!.write(it, rowCounter)
//                    }
//                }
//
//                is Element.Image -> it.load(question.sourceFile, contentStream!!, pdf, rowCounter)
//
//            }
//        }
//        question.options.forEach {
//            if (rowCounter.get() > MAX_ROWS_IN_PAGE) {
//                page = pdf.addPage()
//                contentStream!!.destroy()
//                contentStream = newContentStream(pdf, page!!, font)
//                rowCounter.set(0)
//            }
//            val text = it as Element.Text
//            if (it.value.isBlank()) {
//                contentStream!!.newLine(rowCounter)
//            } else {
//                text.print(contentStream!!, rowCounter)
//                contentStream!!.newLine(rowCounter)
//            }
//        }
//        with(contentStream!!) {
//            newLine(rowCounter)
//            newLine(rowCounter)
//            showText(separator)
//            newLine(rowCounter)
//            newLine(rowCounter)
//        }
//    }
//    contentStream!!.destroy()
//    pdf
//}

private fun newContentStream(pdf: PDDocument, page: PDPage, font: PDType0Font) =
    PDPageContentStream(pdf, page, PDPageContentStream.AppendMode.APPEND, true, true).apply {
        beginText()
        setFont(font, fontSize)
        setLeading(14.5f)
        newLineAtOffset(25f, page.mediaBox.height - 50)
    }

private fun PDPageContentStream.destroy() = runCatching {
    endText()
    close()
}.recover { close() }

private fun PDDocument.addPage() = PDPage().also { addPage(it) }

private fun Element.Text.paragraph(): List<String> {
    val wrappedText = mutableListOf<String>()
    when {
        value.isBlank() -> wrappedText.add(System.lineSeparator())
        value.length < MAX_CHARS_IN_LINE -> {
            wrappedText.add(value)
        }

        else -> {
            wrap(value, wrappedText)
        }
    }
    return wrappedText.toList()
}

private fun Element.Text.print(contentStream: PDPageContentStream, rowCounter: AtomicInteger) {
    when {
        value.isBlank() -> contentStream.newLine(rowCounter)
        value.length < 50 -> {
            contentStream.showText(value)
            contentStream.newLine(rowCounter)
        }

        else -> {
            val wrappedText = mutableListOf<String>()
            wrap(value, wrappedText)
            wrappedText.forEach {
                contentStream.showText(it)
                contentStream.newLine(rowCounter)
            }
        }
    }
}

private fun wrap(text: String, list: MutableList<String>) {
    if (text.length <= MAX_CHARS_IN_LINE) {
        list.add(text)
        return
    }
    var index = MAX_CHARS_IN_LINE
    while (index > 0 && text[index].isWhitespace().not()) {
        index--
    }

    list.add(text.substring(0, index))
    wrap(text.substring(index + 1, text.length), list)
}

private suspend fun Element.Image.load(
    sourceFile: File,
    contentStream: PDPageContentStream,
    pdf: PDDocument,
    rowCounter: AtomicInteger
) {
    val imageFile = if (ref.startsWith("http")) {
        sourceFile
            .withoutExtension()
            .plus("_files/")
            .plus(UUID.randomUUID().toString())
            .plus(".png").also {
                downloadImage(ref).run {
                    FileOutputStream(it).use {
                        it.write(this)
                    }
                }
            }
    } else {
        sourceFile.parent.plus(ref)
    }

    contentStream.endText()
    val image = PDImageXObject.createFromFile(imageFile, pdf)
    var height = image.height
    var width = image.width
    val aspectRatio = BigDecimal(height).divide(BigDecimal(width), MathContext(2))

    if (width > 500) {
        width = 500
        height = aspectRatio.multiply(BigDecimal(width)).toInt()
    } else if (height > 300) {
        height = 300
        width = BigDecimal(height).divide(aspectRatio, MathContext(2)).toInt()
    }
    contentStream.drawImage(
        image,
        50f,
        (700 - rowCounter.get().times(10) - height).toFloat(),
        width.toFloat(),
        height.toFloat()
    )
    contentStream.beginText()
    contentStream.newLineAtOffset(25f, (700 - rowCounter.get().times(10) - height - 100).toFloat())
    rowCounter.addAndGet((height + 100).div(10))
}

private fun PDPageContentStream.write(text: String, rowCounter: AtomicInteger) = runCatching {
    showText(text)
    newLine(rowCounter)
}.recover {
    extractMissingGlyph(it)?.let { getMissingGlyphReplacement(it) }?.let {
        showText("$it ${text.substringAfter(" ")}")
        newLine(rowCounter)
    }
}

private fun PDPageContentStream.newLine(rowCounter: AtomicInteger) {
    newLine()
    rowCounter.incrementAndGet()
}
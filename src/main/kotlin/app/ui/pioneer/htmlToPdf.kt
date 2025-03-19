package app.ui.pioneer

import androidx.compose.ui.res.useResource
import app.open
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.File
import java.math.BigDecimal
import java.math.MathContext
import java.util.concurrent.atomic.AtomicInteger


private const val fontSize = 16f

private const val MAX_CHARS_IN_LINE = 60
private const val MAX_ROWS_IN_PAGE = 50
private val separator = StringBuilder("").run {
    repeat((1..MAX_CHARS_IN_LINE.plus(10)).count()) { append("-") }
    toString()
}

fun convert(files: Set<File>, onComplete: () -> Unit) {
    files
        .asSequence()
        .flatMap { toQuestions(it) }
        .sortedBy { it.no.toInt() }
        .toList()
        .toPdf()
        .run {
            val file = files.first().parent.plus("/Test.pdf")
            save(file)
            close()
            File(file).open()
        }
    onComplete()
}

private fun toQuestions(file: File): List<Question> {
    val document = Jsoup.parse(file)
    return document.body()
        .getElementsByClass("elevation-2")
        .asSequence()
        .filter {
            it.hasClass("sticky").not() && it.hasAttr("style").not()
        }.map { it.toQuestion(file) }
        .toList()
}

private fun Element.toQuestion(file: File): Question {
    val questionNo = getElementsByClass("v-chip theme--light primary").text()
    val questionElements = mutableListOf<app.ui.pioneer.Element>()
    val options = StringBuilder("")
    getElementsByTag("p")
        .asSequence()
        .forEach {
            when {
                options.isNotBlank() && it.text().isNotEmpty() -> options.append(it.text()).append("()")
                options.isBlank() && it.text().startsWith("a") -> options.append(it.text()).append("()")
                it.getElementsByTag("img").isNotEmpty() -> file.parent.plus(
                    it.getElementsByTag("img").first()!!
                        .attr("src").removePrefix(".")
                ).run { questionElements += app.ui.pioneer.Element.Image(this) }

                else -> questionElements += app.ui.pioneer.Element.Text(it.text())
            }
        }
    return Question(
        no = questionNo,
        elements = questionElements.toList(),
        options = options
            .removeSuffix("a()b()c()d()e()")
            .removeSuffix("()").toString()
            .split("()")
            .map { app.ui.pioneer.Element.Text(it.replace("()", "")) }.toList()
    )
}

private fun PDDocument.addPage() = PDPage().also { addPage(it) }

private fun List<Question>.toPdf(): PDDocument = useResource("Roboto-VariableFont_wdth,wght.ttf") {
    val pdf = PDDocument()
    val font = PDType0Font.load(pdf, it)
    var page: PDPage
    var contentStream: PDPageContentStream? = null
    val rowCounter = AtomicInteger(0)
    this.forEach {
        runCatching {
            when {
                pdf.pages.count == 0 -> {
                    page = pdf.addPage()
                    contentStream = newContentStream(pdf, page, font)
                }

                rowCounter.get() >= MAX_ROWS_IN_PAGE || it.hasImage() -> {
                    page = pdf.addPage()
                    contentStream!!.destroy()
                    contentStream = newContentStream(pdf, page, font)
                    rowCounter.set(0)
                }
            }
            contentStream!!.showText(it.no + ") ")
            it.elements.forEach {
                when (it) {
                    is app.ui.pioneer.Element.Text -> it.paragraph().forEach {
                        if (rowCounter.get() > MAX_ROWS_IN_PAGE) {
                            page = pdf.addPage()
                            contentStream!!.destroy()
                            contentStream = newContentStream(pdf, page, font)
                            rowCounter.set(0)
                        }
                        if (System.lineSeparator().equals(it)) {
                            contentStream!!.newLine(rowCounter)
                        } else {
                            contentStream!!.showText(it)
                            contentStream!!.newLine(rowCounter)
                        }
                    }

                    is app.ui.pioneer.Element.Image -> it.load(contentStream!!, pdf, rowCounter)
                }
            }
            it.options.forEach {
                if (rowCounter.get() > MAX_ROWS_IN_PAGE) {
                    page = pdf.addPage()
                    contentStream!!.destroy()
                    contentStream = newContentStream(pdf, page, font)
                    rowCounter.set(0)
                }
                val text = it as app.ui.pioneer.Element.Text
                if (it.value.isBlank()) {
                    contentStream!!.newLine(rowCounter)
                } else {
                    text.print(contentStream!!, rowCounter)
                    contentStream!!.newLine(rowCounter)
                }
            }
            with(contentStream!!) {
                newLine(rowCounter)
                newLine(rowCounter)
                showText(separator)
                newLine(rowCounter)
                newLine(rowCounter)
            }
        }.recover {
            it.printStackTrace()
        }
    }
    contentStream!!.destroy()
    pdf
}

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

private fun app.ui.pioneer.Element.Text.paragraph(): List<String> {
    val wrappedText = mutableListOf<String>()
    when {
        value.isBlank() -> wrappedText.add(System.lineSeparator())
        value.length < 50 -> {
            wrappedText.add(value)
        }

        else -> {
            wrap(value, wrappedText)
        }
    }
    return wrappedText.toList()
}

private fun app.ui.pioneer.Element.Text.print(contentStream: PDPageContentStream, rowCounter: AtomicInteger) {
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
    if (text.length < MAX_CHARS_IN_LINE) {
        list.add(text)
        return
    }
    list.add(text.substring(0, MAX_CHARS_IN_LINE - 1))
    wrap(text.substring(MAX_CHARS_IN_LINE - 1, text.length - 1), list)
}

private fun app.ui.pioneer.Element.Image.load(
    contentStream: PDPageContentStream,
    pdf: PDDocument,
    rowCounter: AtomicInteger
) {
    contentStream.endText()
    val image = PDImageXObject.createFromFile(ref, pdf)
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

private fun PDPageContentStream.newLine(rowCounter: AtomicInteger) {
    newLine()
    rowCounter.incrementAndGet()
}
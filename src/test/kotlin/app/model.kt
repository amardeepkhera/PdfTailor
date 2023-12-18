package app

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.text.PDFTextStripper
import java.io.File

const val DOC_1 = "Doc1.pdf"
const val DOC_2 = "Doc2.pdf"
const val FINAL_DOC = "Final.pdf"

val tempFolder = File("src/test/resources/tempFolder")

data class Page(val text: String) {
    companion object {
        val BLANK = Page("")
    }
}

data class Document(val name: String, val pages: List<Page>) {
    private var pDDocument: PDDocument? = null

    fun create(): PDDocument {
        val document = PDDocument()

        pages.forEach { page ->
            PDPage().apply {
                PDPageContentStream(document, this).use {
                    it.beginText()
                    it.setFont(PDType1Font.TIMES_ROMAN, 102f)
                    it.showText(page.text)
                    it.endText()
                }
            }.run { document.addPage(this) }
        }
        return document.also { pDDocument = it }
    }

    fun load(): PDDocument = PDDocument.load(File("${tempFolder.path}/$name"))
}

fun PDDocument.getText(pageNo: Int): String =
    PDFTextStripper().apply {
        startPage = pageNo
        endPage = pageNo
    }.getText(this).removeSuffix("\n")

fun String.toFile() = File("${tempFolder.path}/$this")
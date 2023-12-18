package app

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import app.MergeAction.AddBlankPage
import app.MergeAction.AddBlankPageAtLast
import app.MergeAction.DeletePage
import org.apache.pdfbox.io.MemoryUsageSetting
import org.apache.pdfbox.multipdf.PDFMergerUtility
import org.apache.pdfbox.multipdf.Splitter
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.encryption.AccessPermission
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import org.apache.pdfbox.rendering.PDFRenderer
import java.io.File

fun blankPageImage() = PDDocument().use {
    it.addPage(blankPage())
    PDFRenderer(it).renderImageWithDPI(0, 60f).toComposeImageBitmap()
}

fun loadDocumentAsImages(file: File) = file.loadDocumentAndExecute { document ->
    if (document.numberOfPages > 0) {
        val renderer = PDFRenderer(document)
        (0 until document.pages.count)
            .map { renderer.renderImageWithDPI(it, 60f).toComposeImageBitmap() }
    } else throw EmptyDocumentException()
}

fun loadDocumentsAsImages(files: Set<File>): List<ImageBitmap> = files.flatMap { loadDocumentAsImages(it) }

fun splitAndSave(file: File, directory: File) = file.loadDocumentAndExecute { document ->
    Splitter().split(document).forEachIndexed { index, split ->
        split.save(fileName = file.appendToName("page_${index.plus(1)}"), directory = directory)
    }
}

fun splitAndSave(file: File, directory: File, range: String) = file.loadDocumentAndExecute { document ->
    range.split(",").forEach { splitRange ->
        val startAndEnd = splitRange.split("-")
        val splitter = Splitter()

        splitter.setStartPage(startAndEnd.first().toInt())

        if (startAndEnd.size == 2)
            splitter.setSplitAtPage(startAndEnd.last().toInt().minus(startAndEnd.first().toInt()).plus(1))

        val splitDocuments = splitter.split(document)
        splitDocuments.first().save(fileName = file.appendToName(splitRange), directory = directory)

        (1 until splitDocuments.size).forEach {
            splitDocuments[it].close()
        }
    }
}

fun mergeAndSave(mergeRequest: MergeRequest) = with(mergeRequest) {
    val utility = PDFMergerUtility()

    utility.destinationFileName = mergeRequest.mergedFile.absolutePath
    files.forEach { utility.addSource(it) }
    utility.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly())
    if (mergeRequest.actions.isNotEmpty()) {
        mergeRequest.mergedFile.loadDocumentAndExecute { document ->
            mergeRequest.actions.forEach {
                when (it) {
                    is DeletePage -> document.deletePage(it.pageNo)
                    is AddBlankPage -> document.addBlankPage(it.pageNo)
                    AddBlankPageAtLast -> document.addBlankPageAtLast()
                }
            }
            document.save(mergeRequest.mergedFile)
        }
    }
}

fun mergeAndSave(mergeRequest: MergeAndLockRequest) = with(mergeRequest) {
    val utility = PDFMergerUtility()

    utility.destinationFileName = mergeRequest.mergedFile.absolutePath
    files.forEach { utility.addSource(it) }
    utility.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly())

    mergeRequest.mergedFile.loadDocumentAndExecute { document ->
        mergeRequest.actions.execute(document)
        document.lock(mergeRequest.password)
        document.save(mergeRequest.mergedFile)
    }
}

fun lock(file: File, lockedFile: File, password: String) = file.loadDocumentAndExecute {
    it.lock(password = password)
    it.save(lockedFile)
}

fun unlock(file: File, unlockedFile: File, password: String) = file.loadDocumentAndExecute(password) {
    it.isAllSecurityToBeRemoved = true
    it.save(unlockedFile)
}

private fun List<MergeAction>.execute(document: PDDocument) = forEach {
    when (it) {
        is DeletePage -> document.deletePage(it.pageNo)
        is AddBlankPage -> document.addBlankPage(it.pageNo)
        AddBlankPageAtLast -> document.addBlankPageAtLast()
    }
}

private fun PDDocument.deletePage(pageNo: Int) {
    val pages = documentCatalog.pages
    pages.remove(pageNo.minus(1))
}

private fun PDDocument.addBlankPage(pageNo: Int) {
    val pages = documentCatalog.pages
    pages.insertBefore(blankPage(), pages.get(pageNo.minus(1)))
}

private fun PDDocument.addBlankPageAtLast() {
    val pages = documentCatalog.pages
    pages.insertAfter(blankPage(), pages.last())
}

private fun blankPage(size: PDRectangle = PDRectangle.A4) = PDPage(size)

private fun <R> File.loadDocumentAndExecute(fn: (PDDocument) -> R) = runCatching {
    PDDocument.load(this).use(fn)
}.getOrElse {
    if (it is InvalidPasswordException)
        throw PasswordProtectedDocumentException()
    else throw it
}

private fun <R> File.loadDocumentAndExecute(password: String, fn: (PDDocument) -> R) = runCatching {
    PDDocument.load(this, password).use(fn)
}.getOrElse {
    if (it is InvalidPasswordException)
        throw IncorrectPasswordException()
    else throw it
}


private fun PDDocument.lock(password: String) {
    val accessPermission = AccessPermission()
    StandardProtectionPolicy(password, password, accessPermission).apply {
        encryptionKeyLength = 128
        permissions = accessPermission
    }.run { this@lock.protect(this) }
}

private fun PDDocument.save(fileName: String, directory: File) = use {
    save(File(directory.absolutePath + File.separator + fileName))
}
package app.ui.pioneer

import app.open
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit


suspend fun convert(files: Set<File>, onComplete: () -> Unit) {
    files
        .asSequence()
        .flatMap { toQuestions(it) }
        .distinctBy { it.no }
        .sortedBy { it.no.toInt() }
        .toList()
        .toPdf()
        .run {
            val file =
                files.first().parent.plus(
                    "/Test-${
                        LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
                            .format(DateTimeFormatter.ofPattern("YYYY-mm-dd_hh:mm"))
                    }.pdf"
                )
            save(file)
            close()
            File(file).open()
        }
    onComplete()
}






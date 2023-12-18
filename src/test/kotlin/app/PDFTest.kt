package app

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled
class PDFTest {

    @Test
    fun `load pdf`() {
        Document("Merged-123.pdf", emptyList())
            .load().use {
                it.pages.forEachIndexed { index, pdPage ->
                }
            }
    }
}
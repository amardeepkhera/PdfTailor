package app.db

import java.util.UUID

object PdfRepo {
    val getAllPdfs: () -> List<Pdf> = {
        val pdfs = mutableListOf<Pdf>()
        DBConnection.get().prepareStatement("select p.id,p.name from PDF p")
            .use {
                it.executeQuery()
                    .use {
                        while (it.next()) {
                            pdfs += Pdf(id = UUID.fromString(it.getString("id")), name = it.getString("name"))
                        }
                    }
            }
        pdfs

    }

}
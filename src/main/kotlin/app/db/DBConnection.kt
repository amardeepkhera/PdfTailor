package app.db

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

object DBConnection {

    private val createSchema: (Connection) -> Unit = {
        runCatching {
            it.createStatement().execute(
                """
                CREATE TABLE PDF (
                id varchar(36) PRIMARY KEY,
                name VARCHAR(255)
            )""".trimIndent()
            )
            Unit
        }.recover {
            if (it is SQLException && "X0Y32".equals(it.sqlState, true)) println("Schema already exists.") else throw it
        }
    }

    //    private val createData: (Connection) -> Unit = {
//        it.prepareStatement("INSERT INTO PDF (id,name) values (?,?)")
//            .apply {
//                setString(1, UUID.randomUUID().toString())
//                setString(2, "PDF")
//            }.executeUpdate()
//
//    }
    private val connection =
        DriverManager.getConnection("jdbc:derby:/Users/amardeep/dev/pdftailor_new/MyDB;create=true").also {
            it.autoCommit = false
            createSchema(it)
//            createData(it)
        }

    fun get() = connection!!

    fun close() {
        runCatching {
            DriverManager.getConnection("jdbc:derby:/Users/amardeep/dev/pdftailor_new/MyDB;shutdown=true")
        }.recover {
            if (it is SQLException && "08006".equals(
                    it.sqlState,
                    true
                )
            ) println("DB shutdown successful") else throw it
        }
    }
}


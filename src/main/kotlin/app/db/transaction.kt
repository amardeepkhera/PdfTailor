package app.db

import java.sql.Connection

fun transaction(fn: Connection.() -> Unit) {
    val connection = DBConnection.get()
    fn(connection)
    connection.commit()
}
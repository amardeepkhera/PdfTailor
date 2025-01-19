package app.db

import com.mongodb.client.MongoClients
object MongoDBConnection {

    private const val CONNECTION_STRING = "<YOUR_CONNECTION_STRING>"

    fun getMongoClient(): com.mongodb.client.MongoClient {
        return MongoClients.create(CONNECTION_STRING)
    }
}
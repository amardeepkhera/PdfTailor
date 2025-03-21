package app.ui.pioneer

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes


private val httpClient = HttpClient(CIO)


suspend fun downloadImage(url: String) = httpClient.get(urlString = url).bodyAsBytes()
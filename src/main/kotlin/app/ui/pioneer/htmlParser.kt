package app.ui.pioneer

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.File

fun toQuestions(file: File): List<Question> {
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
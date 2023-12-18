package app.ui

private val RANGE_REGEX: (Int) -> Regex = { noOfPages ->
    Regex("^(([1-$noOfPages]+(-[1-$noOfPages]+)?(,|\$))+)\$")
}

fun isPageRangeValid(range: String, noOfPages: Int) =
    if (range.isBlank()) true else RANGE_REGEX(noOfPages).matches(range)
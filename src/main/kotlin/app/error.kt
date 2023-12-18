package app

object EmptyException : RuntimeException()

data class EmptyDocumentException(override val message: String = "Cannot open an empty document") :
    RuntimeException(message)

data class PasswordProtectedDocumentException(override val message: String = "One or more documents are password protected. Please unlock and try again.") :
    RuntimeException(message)

data class IncorrectPasswordException(override val message: String = "Incorrect password provided") :
    RuntimeException(message)

fun Throwable.isNotEmpty() = (this is EmptyException).not()

private val knownErrors =
    listOf(EmptyDocumentException::class, PasswordProtectedDocumentException::class, IncorrectPasswordException::class)

fun Throwable.isKnown() = knownErrors.any { it == this::class }
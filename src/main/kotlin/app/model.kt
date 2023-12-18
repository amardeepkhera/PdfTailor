package app

import java.io.File

sealed class MergeAction {
    data class DeletePage(val pageNo: Int) : MergeAction()
    data class AddBlankPage(val pageNo: Int) : MergeAction()
    object AddBlankPageAtLast : MergeAction()
}

data class MergeRequest(
    val files: Set<File>,
    val mergedFile: File,
    val actions: List<MergeAction>,
)

data class MergeAndLockRequest(
    val files: Set<File>,
    val mergedFile: File,
    val actions: List<MergeAction>,
    val password: String
)
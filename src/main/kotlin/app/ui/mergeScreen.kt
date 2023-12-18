@file:OptIn(ExperimentalMaterialApi::class)

package app.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import app.EmptyException
import app.MergeAction
import app.MergeAction.AddBlankPage
import app.MergeAction.DeletePage
import app.MergeRequest
import app.blankPageImage
import app.isNotEmpty
import app.loadDocumentsAsImages
import app.mergeAndSave
import app.open
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Cursor
import java.awt.FileDialog
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun mergeScreen(
    files: Set<File>,
    workflowCompleted: (Boolean) -> Unit,
    updateFiles: (Set<File>) -> Unit,
    images: List<ImageBitmap>,
    mergeActions: MutableList<MergeAction>
) {

    var isCancelButtonPressed by remember { mutableStateOf(false) }

    var showPasswordScreen by remember { mutableStateOf(false) }

    var stateFullImages by remember { mutableStateOf(images) }

    var error by remember { mutableStateOf<Throwable>(EmptyException) }

    if (images.isEmpty()) {
        LaunchedEffect(files) {
            stateFullImages = withContext(Dispatchers.Default) {
                runCatching {
                    loadDocumentsAsImages(files)
                }.getOrElse {
                    error = it
                    emptyList()
                }
            }
        }
    }


    when {
        error.isNotEmpty() -> errorScreen(error)

        stateFullImages.isEmpty() -> progressIndicator()

        isCancelButtonPressed -> homeScreen()

        showPasswordScreen -> passwordScreenOnMerge(
            files = files,
            mergeActions = mergeActions,
            workflowCompleted = workflowCompleted,
            updateFiles = updateFiles,
            images = stateFullImages
        )

        else -> mergeScreen(
            files = files,
            workflowCompleted = workflowCompleted,
            updateFiles = updateFiles,
            mergeActions = mergeActions,
            stateFullImages = stateFullImages,
            stateFullImagesFn = {
                stateFullImages = it
            },
            onCancel = {
                isCancelButtonPressed = it
            },
            showPasswordScreen = {
                showPasswordScreen = it
            },
            error = {
                error = it
            }
        )
    }
}

@Composable
private fun mergeScreen(
    files: Set<File>,
    workflowCompleted: (Boolean) -> Unit,
    updateFiles: (Set<File>) -> Unit,
    mergeActions: MutableList<MergeAction>,
    stateFullImages: List<ImageBitmap>,
    stateFullImagesFn: (MutableList<ImageBitmap>) -> Unit,
    onCancel: (Boolean) -> Unit,
    showPasswordScreen: (Boolean) -> Unit,
    error: (Throwable) -> Unit
) {
    val blankPageImage by remember { mutableStateOf(blankPageImage()) }
    val stateFullMergeActions by remember { mutableStateOf(mergeActions) }
    val selectedPageNo = remember { mutableStateOf(0) }

    Row(
        Modifier.background(
            color = MaterialTheme.colors.background,
        ).fillMaxSize()
            .then(Modifier.clickable { selectedPageNo.value = 0 })
    ) {
        Column {
            Text(
                modifier = Modifier.padding(start = 100.dp, top = 10.dp),
                color = MaterialTheme.colors.primary,
                text = with(stateFullImages.size) { if (this == 1) "$this Page" else "$this Pages" },
                fontFamily = FontFamily.Cursive,
                fontWeight = FontWeight.Bold,
                fontSize = TextUnit(20f, TextUnitType.Sp)
            )
            LazyColumn(
                modifier = Modifier.padding(5.dp)
            ) {
                itemsIndexed(stateFullImages) { i: Int, bitMap: ImageBitmap ->
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Card(
                            onClick = {
                                selectedPageNo.value = i.plus(1)
                            }, modifier = Modifier
                                .border(
                                    width = if (isPageSelected(i, selectedPageNo.value)) 8.dp else 0.dp,
                                    color = if (isPageSelected(
                                            i,
                                            selectedPageNo.value
                                        )
                                    ) MaterialTheme.colors.primary
                                    else Color.Transparent,
                                    shape = RoundedCornerShape(7.dp)
                                ).pointerHoverIcon(icon = PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                        ) {
                            Image(
                                modifier = Modifier.clip(RoundedCornerShape(7.dp)),
                                bitmap = bitMap,
                                contentDescription = "",
                            )
                        }

                        Text(
                            i.plus(1).toString(),
                            color = MaterialTheme.colors.secondary,
                            modifier = Modifier.padding(vertical = 5.dp)
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(start = 40.dp, top = 50.dp)) {
            Row(modifier = Modifier.padding(start = 50.dp)) {
                button(
                    text = "Delete Page",
                    isEnabled = selectedPageNo.value > 0 && stateFullImages.size > 1,
                    onClick = {
                        stateFullMergeActions.add(DeletePage(selectedPageNo.value))
                        stateFullImages.toMutableList().also { it.removeAt(selectedPageNo.value.minus(1)) }
                            .let { stateFullImagesFn(it) }
                        selectedPageNo.value = 0
                    }
                )

                button(
                    text = "Add Blank Page",
                    isEnabled = selectedPageNo.value > 0,
                    modifier = Modifier.padding(start = 50.dp),
                    onClick = {
                        mergeActions.add(AddBlankPage(selectedPageNo.value))
                        stateFullImages.toMutableList()
                            .also { it.add(selectedPageNo.value.minus(1), blankPageImage) }
                            .let { stateFullImagesFn(it) }
                        selectedPageNo.value = 0
                    }
                )

            }
            Row(modifier = Modifier.padding(top = 40.dp, start = 50.dp)) {
                button(
                    text = "Add Blank Page At End",
                    onClick = {
                        mergeActions.add(MergeAction.AddBlankPageAtLast)
                        stateFullImages.toMutableList().also { it.add(blankPageImage) }
                            .let { stateFullImagesFn(it) }
                        selectedPageNo.value = 0
                    }
                )

                button(
                    text = "Lock With Password",
                    modifier = Modifier.padding(start = 50.dp),
                    onClick = { showPasswordScreen(true) }
                )
            }
            Row(
                modifier = Modifier.padding(top = 40.dp, start = 50.dp),
            ) {
                button(
                    isEnabled = true,
                    text = "Cancel",
                    onClick = { onCancel(true) }
                )
                button(
                    text = "Submit",
                    modifier = Modifier.padding(start = 50.dp),
                    onClick = {
                        FileDialog(ComposeWindow(), "Save", FileDialog.SAVE).apply {
                            val fileName = "Merged-${LocalDate.now().format(DateTimeFormatter.ISO_DATE)}.pdf"
                            this.file = fileName
                            isVisible = true
                            directory?.runCatching {
                                val request = MergeRequest(
                                    files = files,
                                    mergedFile = File("$this/${this@apply.file}"),
                                    actions = mergeActions
                                )
                                mergeAndSave(request)
                                workflowCompleted(true)
                                updateFiles(emptySet())
                                File(directory).open()
                            }?.getOrElse { error(it) }
                        }
                    }
                )
            }
        }
    }
}

private fun isPageSelected(currentIndex: Int, selectPageNo: Int) = currentIndex.plus(1) == selectPageNo

@Composable
fun button(
    isEnabled: Boolean = true,
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    width: Int = 140,
    height: Int = 55
) {
    Button(
        shape = AbsoluteRoundedCornerShape(10.dp),
        modifier = modifier.then(Modifier.width(width.dp).height(height.dp)),
        onClick = onClick,
        enabled = isEnabled
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.CenterVertically),
        )
    }
}

@Preview
@Composable
private fun previewMergeScreen() {
    mergeScreen(
        files = emptySet(),
        workflowCompleted = {},
        updateFiles = {},
        images = emptyList(),
        mergeActions = mutableListOf()
    )
}


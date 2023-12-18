@file:OptIn(ExperimentalFoundationApi::class, ExperimentalFoundationApi::class)

package app.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import app.EmptyException
import app.MergeAction
import app.MergeAndLockRequest
import app.appendToName
import app.isNotEmpty
import app.lock
import app.mergeAndSave
import app.open
import app.unlock
import java.awt.Cursor
import java.awt.FileDialog
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun passwordScreenOnMerge(
    files: Set<File>,
    mergeActions: MutableList<MergeAction>,
    workflowCompleted: (Boolean) -> Unit,
    updateFiles: (Set<File>) -> Unit,
    images: List<ImageBitmap>,
) {

    var isCancelButtonPressed by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Throwable>(EmptyException) }

    when {
        error.isNotEmpty() -> errorScreen(error)

        isCancelButtonPressed -> mergeScreen(
            files = files,
            workflowCompleted = workflowCompleted,
            updateFiles = updateFiles,
            images = images,
            mergeActions = mergeActions
        )

        else -> passwordScreen(
            placeHolderText = "Enter Password To Lock PDF",
            onCancel = { isCancelButtonPressed = it },
            onSubmit = { password ->
                FileDialog(ComposeWindow(), "Save", FileDialog.SAVE).apply {
                    val fileName = "MergedAndLocked-${LocalDate.now().format(DateTimeFormatter.ISO_DATE)}.pdf"
                    this.file = fileName
                    isVisible = true
                    directory?.runCatching {
                        val request = MergeAndLockRequest(
                            files = files,
                            mergedFile = File("$this/${this@apply.file}"),
                            actions = mergeActions,
                            password = password
                        )
                        mergeAndSave(request)
                        workflowCompleted(true)
                        updateFiles(emptySet())
                        File(this).open()
                    }?.getOrElse { error = it }
                }
            }
        )
    }
}

@Composable
fun passwordScreenOnLock(
    file: File,
    workflowCompleted: (Boolean) -> Unit,
    updateFile: (String) -> Unit
) {

    var isCancelButtonPressed by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Throwable>(EmptyException) }

    when {
        isCancelButtonPressed -> homeScreen()

        else -> passwordScreen(
            placeHolderText = "Enter Password To Lock PDF",
            onCancel = { isCancelButtonPressed = it },
            onSubmit = { password ->
                FileDialog(ComposeWindow(), "Save", FileDialog.SAVE).apply {
                    val fileName = file.appendToName("Locked")
                    this.file = fileName
                    isVisible = true
                    directory?.runCatching {
                        lock(file, File("$this/${this@apply.file}"), password)
                        workflowCompleted(true)
                        updateFile("")
                        File(this).open()
                    }?.getOrElse { error = it }
                }
            }
        )
    }
}

@Composable
fun passwordScreenOnUnlock(
    file: File,
    workflowCompleted: (Boolean) -> Unit,
    updateFile: (String) -> Unit,
) {

    var isCancelButtonPressed by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Throwable>(EmptyException) }

    when {
        error.isNotEmpty() -> errorScreen(error)

        isCancelButtonPressed -> homeScreen()

        else -> passwordScreen(
            placeHolderText = "Enter Password To Unlock PDF",
            onCancel = { isCancelButtonPressed = it },
            onSubmit = { password ->
                FileDialog(ComposeWindow(), "Save", FileDialog.SAVE).apply {
                    val fileName = file.appendToName("Unlocked")
                    this.file = fileName
                    isVisible = true
                    directory?.runCatching {
                        unlock(file, File("$this/${this@apply.file}"), password)
                        workflowCompleted(true)
                        updateFile("")
                        File(this).open()
                    }?.getOrElse { error = it }
                }
            }
        )
    }
}

@Composable
private fun passwordScreen(
    placeHolderText: String,
    onCancel: (Boolean) -> Unit,
    onSubmit: (String) -> Unit
) {
    var password by remember { mutableStateOf(TextFieldValue("")) }
    var showPassword by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.background(color = MaterialTheme.colors.background)
            .fillMaxSize()
    ) {
        singleLineTextBox(
            modifier = Modifier.focusRequester(focusRequester),
            isEnabled = true,
            text = password,
            textOnChange = {
                password = it
            },
            placeHolderText = placeHolderText,
            isErrorSupplier = { false },
            trailingIcon = @Composable {
                IconButton(
                    modifier = Modifier.pointerHoverIcon(icon = PointerIcon(Cursor(Cursor.HAND_CURSOR))),
                    onClick = {
                        showPassword = !showPassword
                    }
                ) {
                    Icon(
                        imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = ""
                    )
                }
            },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation()
        )
        Row(modifier = Modifier.padding(top = 50.dp)) {
            Button(
                onClick = { onCancel(true) }
            ) {
                Text("Cancel")
            }

            Button(
                enabled = shouldEnableSubmitButton(password.text),
                modifier = Modifier.padding(start = 100.dp),
                onClick = { onSubmit(password.text) }
            ) {
                Text("Submit")
            }
        }
    }
}

private fun shouldEnableSubmitButton(password: String) = password.isNotEmpty() && password.length < 21

@Preview
@Composable
fun previewPasswordScreen() {
    passwordScreenOnMerge(
        files = emptySet(),
        mergeActions = mutableListOf(),
        workflowCompleted = {},
        updateFiles = {},
        images = emptyList(),
    )
}
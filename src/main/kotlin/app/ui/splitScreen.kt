package app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import app.EmptyException
import app.appendToName
import app.db.DBConnection
import app.isNotEmpty
import app.loadDocumentAsImages
import app.open
import app.splitAndSave
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.io.File
import java.util.UUID.randomUUID

private const val FIRST_RADIO_BUTTON = "1"
private const val SECOND_RADIO_BUTTON = "2"

@Composable
fun splitScreen(file: File, workflowCompleted: (Boolean) -> Unit, updateFile: (String) -> Unit) {

    var images by remember { mutableStateOf(emptyList<ImageBitmap>()) }
    var isCancelButtonPressed by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Throwable>(EmptyException) }


    LaunchedEffect(file) {
        images = withContext(Dispatchers.Default) {
            runCatching {
                loadDocumentAsImages(file)
            }.getOrElse {
                error = it
                emptyList()
            }
        }
    }
    when {
        error.isNotEmpty() -> errorScreen(error)

        images.isEmpty() -> progressIndicator()

        isCancelButtonPressed -> homeScreen()

        else -> splitScreen(
            file = file,
            workflowCompleted = workflowCompleted,
            updateFile = updateFile,
            images = images,
            onCancel = { isCancelButtonPressed = it },
            onError = { error = it }
        )
    }
}

@Composable
private fun splitScreen(
    file: File,
    workflowCompleted: (Boolean) -> Unit,
    updateFile: (String) -> Unit,
    images: List<ImageBitmap>,
    onCancel: (Boolean) -> Unit,
    onError: (Throwable) -> Unit
) {

    val selectedRadioButton = remember { mutableStateOf(FIRST_RADIO_BUTTON) }
    var customRangeText by remember { mutableStateOf(TextFieldValue("")) }
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    var enableSplitButton by remember { mutableStateOf(true) }

    Row(
        Modifier
            .background(color = MaterialTheme.colors.background)
            .fillMaxSize()
    ) {
        Column {
            Text(
                modifier = Modifier.padding(start = 100.dp, top = 10.dp),
                color = MaterialTheme.colors.primary,
                text = with(images.size) { if (this == 1) "$this Page" else "$this Pages" },
                fontFamily = FontFamily.Cursive,
                fontWeight = FontWeight.Bold,
                fontSize = TextUnit(20f, TextUnitType.Sp)
            )
            LazyColumn(modifier = Modifier.padding(20.dp)) {
                itemsIndexed(images) { i: Int, bitMap: ImageBitmap ->
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Card(shape = RoundedCornerShape(7.dp)) {
                            Image(bitmap = bitMap, contentDescription = "")
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

        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colors.primary),
                    selected = selectedRadioButton.value == FIRST_RADIO_BUTTON,
                    onClick = {
                        selectedRadioButton.value = FIRST_RADIO_BUTTON
                        customRangeText = TextFieldValue("")
                    }
                )
                Text(
                    "Split all pages", color = MaterialTheme.colors.primary,
                    fontSize = MaterialTheme.typography.subtitle1.fontSize,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                RadioButton(
                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colors.primary),
                    selected = selectedRadioButton.value == SECOND_RADIO_BUTTON,
                    onClick = {
                        selectedRadioButton.value = SECOND_RADIO_BUTTON
                        scope.launch {
                            delay(100)
                            focusRequester.requestFocus()
                        }
                    }
                )
                Text(
                    text = "Split pages by custom range",
                    color = MaterialTheme.colors.primary,
                    fontSize = MaterialTheme.typography.subtitle1.fontSize,
                    fontWeight = FontWeight.Bold
                )
                customRangeTextBox(
                    noOfPages = images.size,
                    isEnabled = selectedRadioButton.value == SECOND_RADIO_BUTTON,
                    customRangeText = customRangeText,
                    customRangeTextOnChange = {
                        customRangeText = it
                    },
                    focusRequester = focusRequester,
                    isRangeValid = { enableSplitButton = it }
                )
            }
            Row(
                modifier = Modifier.padding(15.dp),
            ) {
                Button(
                    onClick = { onCancel(true) }
                ) {
                    Text("Cancel")
                }
                Button(
                    enabled = enableSplitButton,
                    modifier = Modifier.padding(start = 40.dp),
                    onClick = {
                        FileDialog(ComposeWindow(), "Save", FileDialog.SAVE).apply {
                            this.file = file.appendToName(randomUUID().toString())
                            isVisible = true
                            directory?.runCatching {
                                when (selectedRadioButton.value) {
                                    FIRST_RADIO_BUTTON -> splitAndSave(file = file, directory = File(this))
                                    SECOND_RADIO_BUTTON -> splitAndSave(
                                        file = file,
                                        directory = File(this),
                                        range = customRangeText.text.trim()
                                    )
                                }
                                workflowCompleted(true)
                                updateFile("")
                                File(this).open()
                            }?.getOrElse { onError(it) }
                        }
                    }
                ) {
                    Text("Split")
                }

            }
        }
    }
}

@Composable
fun customRangeTextBox(
    noOfPages: Int,
    isEnabled: Boolean,
    customRangeText: TextFieldValue,
    customRangeTextOnChange: (TextFieldValue) -> Unit,
    isRangeValid: (Boolean) -> Unit,
    focusRequester: FocusRequester
) {
    singleLineTextBox(
        modifier = Modifier.padding(5.dp)
            .size(width = 200.dp, height = 50.dp)
            .border(
                width = 0.1.dp,
                color = Color.DarkGray,
                shape = RoundedCornerShape(50)
            ).focusRequester(focusRequester),
        isEnabled = isEnabled,
        text = customRangeText,
        textOnChange = customRangeTextOnChange,
        placeHolderText = "1,2-4,5-8 or 1-4,5-8",
        isErrorSupplier = {
            isPageRangeValid(range = customRangeText.text, noOfPages = noOfPages)
                .also { isRangeValid(it) }
                .not()
        }
    )
}




package app.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.RemoveRedEye
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation

@Composable
fun singleLineTextBox(
    modifier: Modifier,
    isEnabled: Boolean,
    text: TextFieldValue,
    textOnChange: (TextFieldValue) -> Unit,
    placeHolderText: String? = null,
    isErrorSupplier: () -> Boolean,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    OutlinedTextField(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        textStyle = TextStyle(
            fontSize = MaterialTheme.typography.body2.fontSize,
            color = MaterialTheme.colors.primary,
        ),
        value = text,
        isError = isErrorSupplier(),
        onValueChange = {
            textOnChange(it)
        },
        enabled = isEnabled,
        singleLine = true,
        placeholder = placeHolderText?.let {
            {
                Text(
                    fontSize = MaterialTheme.typography.body2.fontSize,
                    text = it,
                    color = MaterialTheme.colors.primary,
                )
            }
        },
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation

    )
}

@Preview
@Composable
fun previewSingleLineTextBox() {
    singleLineTextBox(
        modifier = Modifier,
        isEnabled = true,
        text = TextFieldValue(""),
        isErrorSupplier = { false },
        textOnChange = {},
        trailingIcon = @Composable {
            IconButton(
                onClick = { }
            ) {
                Icon(
                    imageVector = Icons.TwoTone.RemoveRedEye,
                    contentDescription = "Show Password"
                )
            }
        }
    )
}
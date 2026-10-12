package com.example.feature.checkout.presentation.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.core.ui.asString
import com.example.core.ui.theme.ShopTheme
import com.example.feature.checkout.presentation.ui.model.SelectableRowUi
import com.example.feature.checkout.presentation.ui.preview.CheckoutPreviewData

// The radio button is not clickable on its own, so it gets no minimum touch target:
// its 20.dp icon plus 2.dp of padding on each side.
private val RadioButtonWidth = 24.dp
private val RadioButtonToTextSpacing = 12.dp

/** Where a row's text column starts, for content that belongs under the text rather than under the radio button. */
internal val SelectableRowTextInset = RadioButtonWidth + RadioButtonToTextSpacing

@Composable
fun SelectableRow(row: SelectableRowUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(selected = row.selected, enabled = row.enabled, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = row.selected, onClick = null, enabled = row.enabled)
        Column(modifier = Modifier.weight(1f).padding(start = RadioButtonToTextSpacing)) {
            Text(text = row.title.asString(), style = MaterialTheme.typography.bodyLarge)
            row.subtitle?.let { subtitle ->
                Text(
                    text = subtitle.asString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        row.trailing?.let { trailing ->
            Text(text = trailing.asString(), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SelectableRowPreview() {
    ShopTheme {
        SelectableRow(row = CheckoutPreviewData.shipping.rows.first(), onClick = {})
    }
}

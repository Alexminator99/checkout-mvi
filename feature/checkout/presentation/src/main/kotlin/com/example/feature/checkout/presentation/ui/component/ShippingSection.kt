package com.example.feature.checkout.presentation.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.core.ui.asString
import com.example.core.ui.theme.ShopTheme
import com.example.feature.checkout.presentation.R
import com.example.feature.checkout.presentation.ui.model.ShippingSectionUi
import com.example.feature.checkout.presentation.ui.preview.CheckoutPreviewData

@Composable
fun ShippingSection(
    shipping: ShippingSectionUi,
    onSelect: (optionId: String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionHeader(text = stringResource(R.string.checkout_section_shipping))
        when (shipping) {
            ShippingSectionUi.Hidden -> Text(
                text = stringResource(R.string.checkout_shipping_unavailable),
                style = MaterialTheme.typography.bodyMedium,
            )
            ShippingSectionUi.Loading -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
            is ShippingSectionUi.Error -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = shipping.message.asString(),
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.error,
                )
                TextButton(onClick = onRetry) { Text(text = stringResource(R.string.action_retry)) }
            }
            is ShippingSectionUi.Options -> shipping.rows.forEach { row ->
                SelectableRow(row = row, onClick = { onSelect(row.id) })
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ShippingSectionPreview() {
    ShopTheme { ShippingSection(shipping = CheckoutPreviewData.shipping, onSelect = {}, onRetry = {}) }
}

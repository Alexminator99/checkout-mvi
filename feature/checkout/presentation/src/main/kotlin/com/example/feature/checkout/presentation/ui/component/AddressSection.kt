package com.example.feature.checkout.presentation.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.core.ui.asString
import com.example.core.ui.theme.ShopTheme
import com.example.feature.checkout.presentation.R
import com.example.feature.checkout.presentation.ui.model.AddressSectionUi
import com.example.feature.checkout.presentation.ui.model.AddressStatusUi
import com.example.feature.checkout.presentation.ui.preview.CheckoutPreviewData

@Composable
fun AddressSection(
    address: AddressSectionUi,
    onSelect: (addressId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionHeader(text = stringResource(R.string.checkout_section_address))
        address.rows.forEach { row -> SelectableRow(row = row, onClick = { onSelect(row.id) }) }
        AddressStatus(status = address.status)
    }
}

@Composable
private fun AddressStatus(status: AddressStatusUi) {
    when (status) {
        AddressStatusUi.None -> Unit
        AddressStatusUi.Checking -> Text(
            text = stringResource(R.string.checkout_address_checking),
            style = MaterialTheme.typography.bodySmall,
        )
        AddressStatusUi.Deliverable -> Text(
            text = stringResource(R.string.checkout_address_deliverable),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
        is AddressStatusUi.Warning -> Text(
            text = status.message.asString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AddressSectionPreview() {
    ShopTheme { AddressSection(address = CheckoutPreviewData.address, onSelect = {}) }
}

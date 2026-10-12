package com.example.feature.checkout.presentation.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.core.ui.theme.ShopTheme
import com.example.feature.checkout.presentation.R
import com.example.feature.checkout.presentation.ui.model.PaymentSectionUi
import com.example.feature.checkout.presentation.ui.preview.CheckoutPreviewData

@Composable
fun PaymentSection(
    payment: PaymentSectionUi,
    onSelect: (paymentMethodId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionHeader(text = stringResource(R.string.checkout_section_payment))
        payment.rows.forEach { row -> SelectableRow(row = row, onClick = { onSelect(row.id) }) }
    }
}

@Preview(showBackground = true)
@Composable
private fun PaymentSectionPreview() {
    ShopTheme { PaymentSection(payment = CheckoutPreviewData.payment, onSelect = {}) }
}

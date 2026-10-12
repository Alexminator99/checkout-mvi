package com.example.feature.checkout.presentation.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.core.ui.asString
import com.example.core.ui.theme.ShopTheme
import com.example.feature.checkout.presentation.R
import com.example.feature.checkout.presentation.ui.model.CartLineUi
import com.example.feature.checkout.presentation.ui.model.CartSectionUi
import com.example.feature.checkout.presentation.ui.preview.CheckoutPreviewData

@Composable
fun CartSection(
    cart: CartSectionUi,
    onQuantityChange: (sku: String, quantity: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(text = cart.title.asString())
        if (cart.isEmpty) {
            Text(text = stringResource(R.string.checkout_cart_empty), style = MaterialTheme.typography.bodyMedium)
        }
        cart.lines.forEach { line ->
            CartLineRow(line = line, onQuantityChange = { quantity -> onQuantityChange(line.sku, quantity) })
        }
    }
}

@Composable
private fun CartLineRow(line: CartLineUi, onQuantityChange: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = line.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        IconButton(onClick = { onQuantityChange(line.quantity - 1) }, enabled = line.canDecrement) {
            if (line.decrementRemovesLine) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.checkout_cd_remove, line.name),
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_remove),
                    contentDescription = stringResource(R.string.checkout_cd_decrease, line.name),
                )
            }
        }
        Text(text = line.quantity.toString(), style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = { onQuantityChange(line.quantity + 1) }, enabled = line.canIncrement) {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = stringResource(R.string.checkout_cd_increase, line.name),
            )
        }
        Text(
            text = line.lineTotal,
            modifier = Modifier.widthIn(min = 72.dp),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CartSectionPreview() {
    ShopTheme { CartSection(cart = CheckoutPreviewData.cart, onQuantityChange = { _, _ -> }) }
}

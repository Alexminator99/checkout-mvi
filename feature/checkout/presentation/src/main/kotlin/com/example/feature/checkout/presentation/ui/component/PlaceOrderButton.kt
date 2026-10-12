package com.example.feature.checkout.presentation.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.core.ui.asString
import com.example.core.ui.theme.ShopTheme
import com.example.feature.checkout.presentation.ui.model.PlaceOrderButtonUi
import com.example.feature.checkout.presentation.ui.preview.CheckoutPreviewData

@Composable
fun PlaceOrderButton(button: PlaceOrderButtonUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = onClick, enabled = button.enabled, modifier = modifier.fillMaxWidth()) {
        if (button.loading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = LocalContentColor.current, strokeWidth = 2.dp)
        } else {
            Text(text = button.label.asString())
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaceOrderButtonPreview() {
    ShopTheme { PlaceOrderButton(button = CheckoutPreviewData.placeOrder, onClick = {}) }
}

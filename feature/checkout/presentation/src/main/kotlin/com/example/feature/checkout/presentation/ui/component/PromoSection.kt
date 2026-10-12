package com.example.feature.checkout.presentation.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.core.ui.asString
import com.example.core.ui.theme.ShopTheme
import com.example.feature.checkout.presentation.R
import com.example.feature.checkout.presentation.ui.model.PromoSectionUi
import com.example.feature.checkout.presentation.ui.preview.CheckoutPreviewData

@Composable
fun PromoSection(
    promo: PromoSectionUi,
    onInputChange: (String) -> Unit,
    onApply: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = promo.input,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                enabled = promo.inputEnabled,
                label = { Text(text = stringResource(R.string.checkout_promo_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onApply() }),
            )
            if (promo.applied != null) {
                TextButton(onClick = onRemove, enabled = promo.removeEnabled) { Text(text = stringResource(R.string.action_remove)) }
            } else {
                TextButton(onClick = onApply, enabled = promo.applyEnabled) {
                    if (promo.isRedeeming) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(text = stringResource(R.string.action_apply))
                    }
                }
            }
        }
        promo.applied?.let { applied ->
            Text(text = applied.asString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
        promo.error?.let { error ->
            Text(text = error.asString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PromoSectionPreview() {
    ShopTheme { PromoSection(promo = CheckoutPreviewData.promo, onInputChange = {}, onApply = {}, onRemove = {}) }
}

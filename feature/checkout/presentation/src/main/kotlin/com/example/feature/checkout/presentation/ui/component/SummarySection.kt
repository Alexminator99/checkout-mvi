package com.example.feature.checkout.presentation.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
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
import com.example.feature.checkout.presentation.ui.model.SummaryUi
import com.example.feature.checkout.presentation.ui.preview.CheckoutPreviewData

@Composable
fun SummarySection(summary: SummaryUi, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(text = stringResource(R.string.checkout_section_summary))
        HorizontalDivider()
        summary.rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = row.label.asString(), style = MaterialTheme.typography.bodyLarge)
                Text(text = row.value.asString(), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SummarySectionPreview() {
    ShopTheme { SummarySection(summary = CheckoutPreviewData.summary) }
}

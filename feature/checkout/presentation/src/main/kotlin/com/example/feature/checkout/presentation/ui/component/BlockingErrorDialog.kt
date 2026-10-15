package com.example.feature.checkout.presentation.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.core.ui.UiText
import com.example.core.ui.asString
import com.example.feature.checkout.presentation.R

@Composable
fun BlockingErrorDialog(
    message: UiText,
    onDismiss: () -> Unit,
    dismissEnabled: Boolean = true,
    retry: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = { if (dismissEnabled) onDismiss() },
        confirmButton = {
            TextButton(onClick = onDismiss, enabled = dismissEnabled) {
                Text(text = stringResource(if (retry) R.string.action_retry else R.string.action_ok))
            }
        },
        text = { Text(text = message.asString()) },
    )
}

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
fun BlockingErrorDialog(message: UiText, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.action_ok)) }
        },
        text = { Text(text = message.asString()) },
    )
}

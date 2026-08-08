package org.jellyfin.mobile.onboarding.composable

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun StepText(title: String, description: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.h4,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = description,
        style = MaterialTheme.typography.body1,
        textAlign = TextAlign.Center
    )
}

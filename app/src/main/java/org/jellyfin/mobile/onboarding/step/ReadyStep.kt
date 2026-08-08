package org.jellyfin.mobile.onboarding.step

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.jellyfin.mobile.R
import org.jellyfin.mobile.onboarding.composable.StepText

@Composable
fun ReadyStep() {
    StepText(
        title = stringResource(R.string.onboarding_ready_title),
        description = stringResource(R.string.onboarding_ready_description)
    )
}

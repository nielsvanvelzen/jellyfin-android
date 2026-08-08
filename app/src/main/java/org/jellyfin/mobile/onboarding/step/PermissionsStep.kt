package org.jellyfin.mobile.onboarding.step

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.jellyfin.mobile.R
import org.jellyfin.mobile.onboarding.OnboardingViewModel
import org.jellyfin.mobile.onboarding.composable.StepText
import org.jellyfin.mobile.utils.AndroidVersion
import org.jellyfin.mobile.utils.requestPermission

@Composable
fun PermissionsStep(viewModel: OnboardingViewModel) {
    val context = LocalContext.current
    val activity = context.findActivity()

    StepText(
        title = stringResource(R.string.onboarding_permissions_title),
        description = stringResource(R.string.onboarding_permissions_description)
    )
    Spacer(modifier = Modifier.height(24.dp))
    Button(onClick = {
        if (AndroidVersion.isAtLeastS && activity != null) {
            activity.requestPermission(Manifest.permission.BLUETOOTH_CONNECT) {
                viewModel.nextStep()
            }
        } else {
            viewModel.nextStep()
        }
    }) {
        Text(text = stringResource(R.string.onboarding_grant_permission))
    }
}

private fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

package org.jellyfin.mobile.onboarding.step

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.jellyfin.mobile.R
import org.jellyfin.mobile.app.StorageManager
import org.jellyfin.mobile.onboarding.OnboardingViewModel
import org.jellyfin.mobile.onboarding.composable.StepText

@Composable
fun DownloadsStep(storageManager: StorageManager, viewModel: OnboardingViewModel) {
    val storageLocationPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            storageManager.changeStorageLocation(uri)
            viewModel.nextStep()
        }
    }

    StepText(
        title = stringResource(R.string.onboarding_downloads_title),
        description = stringResource(R.string.onboarding_downloads_description)
    )
    Spacer(modifier = Modifier.height(24.dp))
    Button(onClick = { storageLocationPicker.launch(null) }) {
        Text(text = stringResource(R.string.onboarding_choose_location))
    }
}

package org.jellyfin.mobile.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.jellyfin.mobile.MainViewModel
import org.jellyfin.mobile.R
import org.jellyfin.mobile.app.StorageManager
import org.jellyfin.mobile.onboarding.step.DownloadsStep
import org.jellyfin.mobile.onboarding.step.PermissionsStep
import org.jellyfin.mobile.onboarding.step.ReadyStep
import org.jellyfin.mobile.onboarding.step.ServerSelectionStep
import org.jellyfin.mobile.onboarding.step.WelcomeStep
import org.jellyfin.mobile.ui.ComposeFragment
import org.jellyfin.mobile.ui.screens.connect.LogoHeader
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel
import org.koin.compose.viewmodel.koinViewModel

class OnboardingFragment : ComposeFragment() {
    @Composable
    override fun Content() {
        val mainViewModel = koinActivityViewModel<MainViewModel>()
        val viewModel = koinViewModel<OnboardingViewModel>()
        val storageManager = koinInject<StorageManager>()

        val step by viewModel.currentStep.collectAsState()

        BackHandler(enabled = step != OnboardingStep.WELCOME) {
            viewModel.previousStep()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LogoHeader()

            Crossfade(
                targetState = step,
                modifier = Modifier.weight(1f),
                label = "OnboardingStep"
            ) { step ->
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (step) {
                        OnboardingStep.WELCOME -> WelcomeStep()
                        OnboardingStep.PERMISSIONS -> PermissionsStep(viewModel)
                        OnboardingStep.SERVER_SELECTION -> ServerSelectionStep(viewModel)
                        OnboardingStep.DOWNLOADS -> DownloadsStep(storageManager, viewModel)
                        OnboardingStep.COMPLETED -> ReadyStep()
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (step != OnboardingStep.WELCOME && step != OnboardingStep.COMPLETED) {
                    TextButton(onClick = { viewModel.previousStep() }) {
                        Text(text = "Back")
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (step == OnboardingStep.COMPLETED) {
                    Button(onClick = {
                        viewModel.completeOnboarding()
                        mainViewModel.completeOnboarding()
                    }) {
                        Text(text = stringResource(R.string.onboarding_finish))
                    }
                } else if (step == OnboardingStep.WELCOME) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { viewModel.nextStep() }
                    ) {
                        Text(text = stringResource(R.string.onboarding_get_started))
                    }
                } else if (step != OnboardingStep.SERVER_SELECTION) {
                    Row {
                        TextButton(onClick = { viewModel.nextStep() }) {
                            Text(text = stringResource(R.string.onboarding_skip))
                        }
                        Button(onClick = { viewModel.nextStep() }) {
                            Text(text = stringResource(R.string.onboarding_next))
                        }
                    }
                }
            }
        }
    }
}

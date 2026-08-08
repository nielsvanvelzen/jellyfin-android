package org.jellyfin.mobile.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jellyfin.mobile.MainViewModel
import org.jellyfin.mobile.ui.ComposeFragment
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class OnboardingFragment : ComposeFragment() {
    private val mainViewModel: MainViewModel by activityViewModel()

    @Composable
    override fun Content() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Button(onClick = { mainViewModel.completeOnboarding() }) {
                // TODO
                Text(text = "Todo: Onboarding (Click to complete)")
            }
        }
    }
}

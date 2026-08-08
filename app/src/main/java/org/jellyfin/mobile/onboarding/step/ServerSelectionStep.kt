package org.jellyfin.mobile.onboarding.step

import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jellyfin.mobile.R
import org.jellyfin.mobile.onboarding.OnboardingViewModel
import org.jellyfin.mobile.ui.screens.connect.StyledTextButton
import org.jellyfin.mobile.ui.state.CheckUrlState
import org.jellyfin.mobile.ui.utils.CenterRow

@Composable
fun ServerSelectionStep(viewModel: OnboardingViewModel) {
    val servers by viewModel.discoveredServers.collectAsState()
    val isDiscoveryFinished by viewModel.isDiscoveryFinished.collectAsState()
    val manualUrlState by viewModel.manualUrlState.collectAsState()
    var hostname by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.startDiscovery()
    }

    Text(
        text = stringResource(R.string.available_servers_title),
        style = MaterialTheme.typography.h5,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(16.dp))

    Crossfade(targetState = isDiscoveryFinished && servers.isEmpty()) { showManual ->
        if (showManual) {
            ManualServerInput(
                hostname = hostname,
                onHostnameChange = {
                    hostname = it
                    viewModel.resetManualUrlState()
                },
                urlState = manualUrlState,
                onSubmit = { viewModel.checkManualUrl(hostname) }
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (!isDiscoveryFinished && servers.isEmpty()) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Searching for servers...")
                } else if (servers.isNotEmpty()) {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(servers) { server ->
                            StyledTextButton(
                                text = "${server.name} (${server.address})",
                                onClick = {
                                    viewModel.nextStep()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedButton(onClick = { viewModel.nextStep() }) {
        Text(text = "Skip for now")
    }
}

@Composable
private fun ManualServerInput(
    hostname: String,
    onHostnameChange: (String) -> Unit,
    urlState: CheckUrlState,
    onSubmit: () -> Unit
) {
    val errorText = (urlState as? CheckUrlState.Error)?.message

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "No servers found automatically. Please enter your server address manually.")
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = hostname,
            onValueChange = onHostnameChange,
            modifier = Modifier
                .fillMaxWidth()
                .onKeyEvent { keyEvent ->
                    if (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER) {
                        onSubmit()
                        true
                    } else false
                },
            label = { Text(text = stringResource(R.string.host_input_hint)) },
            isError = errorText != null,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Go
            ),
            keyboardActions = KeyboardActions(onGo = { onSubmit() }),
            singleLine = true
        )

        AnimatedVisibility(visible = errorText != null) {
            Text(
                text = errorText.orEmpty(),
                color = MaterialTheme.colors.error,
                style = MaterialTheme.typography.caption,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (urlState is CheckUrlState.Pending) {
            CenterRow {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        } else {
            Button(
                onClick = onSubmit,
                enabled = hostname.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.connect_button_text))
            }
        }
    }
}

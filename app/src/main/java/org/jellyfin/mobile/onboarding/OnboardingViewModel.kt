package org.jellyfin.mobile.onboarding

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.mobile.app.AppPreferences
import org.jellyfin.mobile.app.StorageManager
import org.jellyfin.mobile.setup.ConnectionHelper
import org.jellyfin.mobile.ui.state.CheckUrlState
import org.jellyfin.mobile.utils.AndroidVersion
import org.jellyfin.sdk.model.api.ServerDiscoveryInfo

class OnboardingViewModel(
    application: Application,
    private val appPreferences: AppPreferences,
    private val connectionHelper: ConnectionHelper,
    private val storageManager: StorageManager
) : AndroidViewModel(application) {
    private val _currentStep = MutableStateFlow(OnboardingStep.WELCOME)
    val currentStep: StateFlow<OnboardingStep> = _currentStep.asStateFlow()

    private val _discoveredServers = MutableStateFlow<List<ServerDiscoveryInfo>>(emptyList())
    val discoveredServers: StateFlow<List<ServerDiscoveryInfo>> = _discoveredServers.asStateFlow()

    private val _isDiscoveryFinished = MutableStateFlow(false)
    val isDiscoveryFinished: StateFlow<Boolean> = _isDiscoveryFinished.asStateFlow()

    private val _manualUrlState = MutableStateFlow<CheckUrlState>(CheckUrlState.Unchecked)
    val manualUrlState: StateFlow<CheckUrlState> = _manualUrlState.asStateFlow()

    private val history = mutableListOf<OnboardingStep>()

    private var discoveryJob: Job? = null

    /**
     * Move to the next step in the onboarding process, skipping completed ones.
     */
    fun nextStep() {
        val origin = _currentStep.value
        var next = getNextStep(origin)

        while (next != OnboardingStep.COMPLETED && shouldSkip(next)) {
            next = getNextStep(next)
        }

        history.add(origin)
        _currentStep.value = next
    }

    /**
     * Move to the previous step in the onboarding process.
     */
    fun previousStep() {
        if (history.isNotEmpty()) {
            _currentStep.value = history.removeAt(history.size - 1)
        }
    }

    private fun getNextStep(step: OnboardingStep): OnboardingStep {
        return when (step) {
            OnboardingStep.WELCOME -> OnboardingStep.PERMISSIONS
            OnboardingStep.PERMISSIONS -> OnboardingStep.SERVER_SELECTION
            OnboardingStep.SERVER_SELECTION -> OnboardingStep.DOWNLOADS
            OnboardingStep.DOWNLOADS -> OnboardingStep.COMPLETED
            OnboardingStep.COMPLETED -> OnboardingStep.COMPLETED
        }
    }

    private fun shouldSkip(step: OnboardingStep): Boolean {
        return when (step) {
            OnboardingStep.PERMISSIONS -> !requiresBluetoothForDiscovery() || isBluetoothPermissionGranted()
            OnboardingStep.DOWNLOADS -> isStorageLocationSet()
            else -> false
        }
    }

    /**
     * Complete the onboarding process.
     */
    fun completeOnboarding() {
        appPreferences.isOnboardingCompleted = true
    }

    /**
     * Check if the Bluetooth permission is granted.
     * Required for local network discovery on Android 17+.
     */
    fun isBluetoothPermissionGranted(): Boolean {
        if (!AndroidVersion.isAtLeastS) return true
        return ContextCompat.checkSelfPermission(
            getApplication(),
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Start local server discovery with an 8-second time limit.
     */
    fun startDiscovery() {
        if (discoveryJob?.isActive == true) return

        _isDiscoveryFinished.value = false
        discoveryJob = viewModelScope.launch {
            val discovery = launch {
                connectionHelper.discoverServersAsFlow().collect { server ->
                    val current = _discoveredServers.value.toMutableList()
                    if (current.none { it.address == server.address }) {
                        current.add(server)
                        _discoveredServers.value = current
                    }
                }
            }

            delay(DISCOVERY_TIMEOUT_MS)
            discovery.cancel()
            _isDiscoveryFinished.value = true
        }
    }

    /**
     * Check a manually entered server URL.
     */
    fun checkManualUrl(url: String) {
        if (_manualUrlState.value == CheckUrlState.Pending) return

        _manualUrlState.value = CheckUrlState.Pending
        viewModelScope.launch {
            val result = connectionHelper.checkServerUrl(url)
            _manualUrlState.value = result
            if (result is CheckUrlState.Success) {
                // If success, we can move to next step
                nextStep()
            }
        }
    }

    /**
     * Reset the manual URL state.
     */
    fun resetManualUrlState() {
        _manualUrlState.value = CheckUrlState.Unchecked
    }

    /**
     * Stop local server discovery and clear the list.
     */
    fun stopDiscovery() {
        discoveryJob?.cancel()
        _discoveredServers.value = emptyList()
        _isDiscoveryFinished.value = false
    }

    /**
     * Check if a storage location for downloads has been set.
     */
    fun isStorageLocationSet(): Boolean {
        return storageManager.getStorageLocation() != null
    }

    /**
     * Whether the device is running Android 17 or higher.
     * Used for specific local network permission requirements.
     */
    fun isAtLeastAndroid17(): Boolean {
        // Placeholder for Android 17 (API 37)
        return Build.VERSION.SDK_INT >= 37
    }

    /**
     * Whether the local network discovery requires the Bluetooth permission on the current device.
     */
    fun requiresBluetoothForDiscovery(): Boolean {
        return isAtLeastAndroid17()
    }

    companion object {
        private const val DISCOVERY_TIMEOUT_MS = 8000L
    }
}

package com.sparklysparky.tellocontroller.classes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sparklysparky.tellocontroller.TelemetryData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val _client = UDPClient()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _telemetry = MutableStateFlow(TelemetryData())
    val telemetry: StateFlow<TelemetryData> = _telemetry.asStateFlow()

    fun connect() {
        viewModelScope.launch {
            val success = _client.connect()
            _isConnected.value = success

            if (success) {
                // Start listening for telemetry updates
                _client.startTelemetryListener { telemetryData ->
                    _telemetry.value = telemetryData
                }
            }
        }
    }

    fun sendCommand(command: CommandType) {
        viewModelScope.launch {
            _client.send(command)
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            _client.close()
        }
    }
}
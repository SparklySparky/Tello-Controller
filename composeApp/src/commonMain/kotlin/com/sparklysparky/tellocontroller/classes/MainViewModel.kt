package com.sparklysparky.tellocontroller.classes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sparklysparky.tellocontroller.TelemetryData
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel: ViewModel() {
    private val _client = UDPClient()
    val client get() = _client

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _telemetry = MutableStateFlow(TelemetryData())
    val telemetry: StateFlow<TelemetryData> = _telemetry.asStateFlow()

    private var telemetryJob: Job? = null

    fun connect() {
        viewModelScope.launch {
            _isConnected.value = _client.connect()
        }
    }

    fun startVideoStream() {
        viewModelScope.launch {
            while(true) {
                _client.getVideoFrame()

                delay(5000L)
            }
        }
    }

    fun sendCommand(command: CommandType) {
        viewModelScope.launch {
            _client.send(command)
        }
    }

    fun updateTelemetry() {
        telemetryJob?.cancel()

        telemetryJob = viewModelScope.launch {
            while(true) {
                if(_isConnected.value) {
                    val data = _client.getTelemetryData(telemetry.value)
                    _telemetry.value = data
                }

                delay(5000L)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            _client.close()
        }
    }
}
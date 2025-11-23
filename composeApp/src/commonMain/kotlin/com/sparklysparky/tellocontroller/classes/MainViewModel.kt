package com.sparklysparky.tellocontroller.classes

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sparklysparky.tellocontroller.TelemetryData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val _client = UDPClient()
    private val _decoder = VideoDecoder()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _telemetry = MutableStateFlow(TelemetryData())
    val telemetry: StateFlow<TelemetryData> = _telemetry.asStateFlow()

    // Video streaming
    private val _currentFrame = MutableStateFlow<ImageBitmap?>(null)
    val currentFrame: StateFlow<ImageBitmap?> = _currentFrame.asStateFlow()

    private val _isVideoStreaming = MutableStateFlow(false)
    val isVideoStreaming: StateFlow<Boolean> = _isVideoStreaming.asStateFlow()

    private val _movementIncrement = MutableStateFlow(20) // cm
    val movementIncrement: StateFlow<Int> = _movementIncrement.asStateFlow()

    private val _rotationIncrement = MutableStateFlow(90) // degrees
    val rotationIncrement: StateFlow<Int> = _rotationIncrement.asStateFlow()

    fun connect() {
        viewModelScope.launch {
            val success = _client.connect()
            _isConnected.value = success

            if (success) {
                _client.startTelemetryListener { telemetryData ->
                    _telemetry.value = telemetryData
                }
            }
        }
    }

    fun sendCommand(command: CommandType, payload: String) {
        viewModelScope.launch {
            _client.send(command, payload)
        }
    }

    fun toggleVideoStream() {
        viewModelScope.launch {
            if (_isVideoStreaming.value) {
                // Stop streaming
                _client.send(CommandType.STREAM_OFF, "")
                _decoder.stop()
                _client.stopVideoStream()
                _currentFrame.value = null
                _isVideoStreaming.value = false
            } else {
                // Start streaming
                _decoder.start(
                    onFrame = { bitmap ->
                        _currentFrame.value = bitmap
                    },
                )

                _client.send(CommandType.STREAM_ON, "")
                _client.startVideoStream { packet ->
                    _decoder.feedPacket(packet)
                }
                _isVideoStreaming.value = true
            }
        }
    }

    fun setMovementIncrement(value: Int) {
        _movementIncrement.value = value
    }

    fun setRotationIncrement(value: Int) {
        _rotationIncrement.value = value
    }


    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            _decoder.stop()
            _client.close()
        }
    }
}
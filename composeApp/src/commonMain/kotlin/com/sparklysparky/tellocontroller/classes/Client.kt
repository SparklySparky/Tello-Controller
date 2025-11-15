package com.sparklysparky.tellocontroller.classes

import com.sparklysparky.tellocontroller.TelemetryData
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

private const val DEFAULT_TELLO_IP = "192.168.10.1"
private const val DEFAULT_TELLO_PORT = 8889        // Command port
private const val LOCAL_COMMAND_PORT = 9000         // Local port for commands
private const val TELEMETRY_PORT = 8890             // Tello state port
private const val VIDEO_PORT = 11111                // Video stream port
private const val RECEIVE_TIMEOUT_MS = 3000

interface Client {
    suspend fun connect(): Boolean
    suspend fun send(message: CommandType)
    fun startTelemetryListener(onUpdate: (TelemetryData) -> Unit)
    fun stopTelemetryListener()
    suspend fun close()
}

class UDPClient : Client {
    private var commandSocket: DatagramSocket? = null
    private var telemetrySocket: DatagramSocket? = null
    private val telloAddress by lazy { InetAddress.getByName(DEFAULT_TELLO_IP) }

    private var telemetryJob: Job? = null
    private var isTelemetryListening = false

    // Command mapping
    private val commandMap = mapOf(
        CommandType.TAKEOFF to "takeoff",
        CommandType.LAND to "land",
        CommandType.EMERGENCY to "emergency",
        CommandType.UP to "up 20",
        CommandType.DOWN to "down 20",
        CommandType.LEFT to "left 20",
        CommandType.RIGHT to "right 20",
        CommandType.FORWARD to "forward 20",
        CommandType.BACKWARD to "back 20",
        CommandType.ROTATE_CLOCKWISE to "cw 90",
        CommandType.ROTATE_COUNTERCLOCKWISE to "ccw 90",
        CommandType.FLIP_FORWARD to "flip f",
        CommandType.FLIP_BACKWARD to "flip b",
        CommandType.FLIP_LEFT to "flip l",
        CommandType.FLIP_RIGHT to "flip r",
        CommandType.GET_BATTERY to "battery?",
        CommandType.GET_SPEED to "speed?",
        CommandType.SET_SPEED to "speed 50",
        CommandType.GET_TIME to "time?",
        CommandType.GET_HEIGHT to "height?",
        CommandType.GET_TEMPERATURE to "temp?",
        CommandType.GET_ACCELERATION to "acceleration?",
        CommandType.GET_TOF to "tof?",
        CommandType.GET_BAROMETER to "baro?",
        CommandType.CONNECT to "command",
        CommandType.STOP to "stop",
        CommandType.STREAM_ON to "streamon",
        CommandType.STREAM_OFF to "streamoff"
    )

    override suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            closeCommandSocket()

            // Create command socket
            commandSocket = DatagramSocket(LOCAL_COMMAND_PORT).apply {
                reuseAddress = true
                soTimeout = RECEIVE_TIMEOUT_MS
            }

            logMsg("✓ Command socket bound to port $LOCAL_COMMAND_PORT")

            // Send SDK mode command
            sendRaw("command")

            val response = receiveCommandResponse()
            val connected = response?.contains("ok", ignoreCase = true) == true

            if (connected) {
                logMsg("✓ Connected to Tello")
            } else {
                logMsg("✗ Connection failed: ${response ?: "timeout"}")
                closeCommandSocket()
            }

            connected
        } catch (e: Exception) {
            logMsg("✗ Connection error: ${e.message}")
            closeCommandSocket()
            false
        }
    }

    override suspend fun send(message: CommandType) = withContext(Dispatchers.IO) {
        val command = commandMap[message] ?: run {
            logMsg("✗ Unknown command: $message")
            return@withContext
        }

        if (commandSocket == null) {
            logMsg("✗ Not connected")
            return@withContext
        }

        try {
            sendRaw(command)
            logMsg("→ $command")
        } catch (e: Exception) {
            logMsg("✗ Send failed: ${e.message}")
        }
    }

    override fun startTelemetryListener(onUpdate: (TelemetryData) -> Unit) {
        if (isTelemetryListening) {
            logMsg("Telemetry listener already running")
            return
        }

        isTelemetryListening = true

        telemetryJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                // Create telemetry socket on port 8890
                telemetrySocket = DatagramSocket(TELEMETRY_PORT).apply {
                    reuseAddress = true
                    soTimeout = 1000 // 1 second timeout for telemetry
                }

                logMsg("✓ Telemetry listener started on port $TELEMETRY_PORT")

                val buffer = ByteArray(2048)

                while (isTelemetryListening) {
                    try {
                        val packet = DatagramPacket(buffer, buffer.size)
                        telemetrySocket?.receive(packet)

                        val data = String(packet.data, 0, packet.length, Charsets.UTF_8).trim()

                        // Parse the telemetry data
                        val telemetry = parseTelemetryState(data)
                        onUpdate(telemetry)

                    } catch (_: SocketTimeoutException) {
                        // Timeout is normal, continue listening
                        continue
                    } catch (e: Exception) {
                        if (isTelemetryListening) {
                            logMsg("Telemetry receive error: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                logMsg("✗ Telemetry listener failed: ${e.message}")
            } finally {
                telemetrySocket?.close()
                telemetrySocket = null
                logMsg("Telemetry listener stopped")
            }
        }
    }

    override fun stopTelemetryListener() {
        isTelemetryListening = false
        telemetryJob?.cancel()
        telemetryJob = null
        telemetrySocket?.close()
        telemetrySocket = null
    }

    override suspend fun close() = withContext(Dispatchers.IO) {
        stopTelemetryListener()
        closeCommandSocket()
        logMsg("✓ Disconnected")
    }

    // Private helpers

    private fun closeCommandSocket() {
        commandSocket?.close()
        commandSocket = null
    }

    private fun sendRaw(command: String) {
        val buffer = command.toByteArray(Charsets.UTF_8)
        val packet = DatagramPacket(buffer, buffer.size, telloAddress, DEFAULT_TELLO_PORT)
        commandSocket?.send(packet)
    }

    private fun receiveCommandResponse(): String? = try {
        val buffer = ByteArray(1518)
        val packet = DatagramPacket(buffer, buffer.size)
        commandSocket?.receive(packet)
        String(packet.data, 0, packet.length, Charsets.UTF_8).trim()
    } catch (_: SocketTimeoutException) {
        null
    } catch (e: Exception) {
        logMsg("Receive error: ${e.message}")
        null
    }

    /**
     * Parse Tello state string into TelemetryData
     * Format: "pitch:%d;roll:%d;yaw:%d;vgx:%d;vgy:%d;vgz:%d;templ:%d;temph:%d;tof:%d;h:%d;bat:%d;baro:%.2f;time:%d;agx:%.2f;agy:%.2f;agz:%.2f;\r\n"
     */
    private fun parseTelemetryState(data: String): TelemetryData {
        try {
            val params = data.split(";")
                .mapNotNull {
                    val parts = it.split(":")
                    if (parts.size == 2) parts[0] to parts[1] else null
                }
                .toMap()

            return TelemetryData(
                pitch = params["pitch"]?.toIntOrNull() ?: 0,
                roll = params["roll"]?.toIntOrNull() ?: 0,
                yaw = params["yaw"]?.toIntOrNull() ?: 0,
                vgx = params["vgx"]?.toIntOrNull() ?: 0,
                vgy = params["vgy"]?.toIntOrNull() ?: 0,
                vgz = params["vgz"]?.toIntOrNull() ?: 0,
                tempLow = params["templ"]?.toIntOrNull() ?: 0,
                tempHigh = params["temph"]?.toIntOrNull() ?: 0,
                tof = params["tof"]?.toIntOrNull() ?: 0,
                height = params["h"]?.toIntOrNull() ?: 0,
                battery = params["bat"]?.toIntOrNull() ?: 0,
                baro = params["baro"]?.toFloatOrNull() ?: 0f,
                flightTime = params["time"]?.toIntOrNull() ?: 0,
                agx = params["agx"]?.toFloatOrNull() ?: 0f,
                agy = params["agy"]?.toFloatOrNull() ?: 0f,
                agz = params["agz"]?.toFloatOrNull() ?: 0f
            )
        } catch (e: Exception) {
            logMsg("Parse error: ${e.message}")
            return TelemetryData()
        }
    }
}
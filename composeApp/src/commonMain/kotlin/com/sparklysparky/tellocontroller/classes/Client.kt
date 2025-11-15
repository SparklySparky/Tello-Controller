package com.sparklysparky.tellocontroller.classes

import com.sparklysparky.tellocontroller.TelemetryData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

const val DEFAULT_TELLO_IP = "192.168.10.1"
const val DEFAULT_TELLO_PORT = 8889
const val LOCAL_PORT = 9000    // local port to bind and receive replies
const val VIDEO_PORT = 11111    // local port to bind and receive replies
const val RECEIVE_TIMEOUT_MS = 3000

interface Client {
    suspend fun connect(): Boolean
    suspend fun send(message: CommandType)
    suspend fun getTelemetryData(oldTelemetryData: TelemetryData): TelemetryData
    suspend fun getVideoFrame(): ByteArray
    suspend fun close()
}

class UDPClient(): Client {
    private var udpSocket: DatagramSocket? = null

    var wifiAlreadyAsked = false
    var sdkAlreadyAsked = false
    var serialAlreadyAsked = false

    override suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            // close any existing socket (defensive)
            udpSocket?.close()

            udpSocket = DatagramSocket(LOCAL_PORT).apply {
                reuseAddress = true
                soTimeout = RECEIVE_TIMEOUT_MS
            }

            logMsg("Socket bound to local port $LOCAL_PORT")

            // send "command" to enter SDK mode
            val cmd = "command"
            val sendBuf = cmd.toByteArray(Charsets.UTF_8)
            val sendPacket = DatagramPacket(sendBuf, sendBuf.size, InetAddress.getByName(DEFAULT_TELLO_IP), DEFAULT_TELLO_PORT)
            udpSocket?.send(sendPacket)
            logMsg("Sent: $cmd")

            val resp = receiveOnce(true)
            logMsg("Connect receive: ${resp ?: "<timeout/no response>"}")

            // Accept if contains "ok" (some packets may be state data; sometimes ok appears later)
            if (resp != null && resp.contains("ok", ignoreCase = true)) {
                logMsg("✓ Connected (ok received)")
                true
            } else {
                logMsg("✗ No 'ok' received")
                false
            }
        } catch (e: Exception) {
            logMsg("Connection failed: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    override suspend fun getVideoFrame(): ByteArray = withContext(Dispatchers.IO) {
        try {
            val actualCommand = "streamon"

            udpSocket = DatagramSocket(LOCAL_PORT).apply {
                reuseAddress = true
                soTimeout = RECEIVE_TIMEOUT_MS
            }

            logMsg("Socket bound to local port $LOCAL_PORT")

            try {
                val buffer = actualCommand.toByteArray(Charsets.UTF_8)
                val packet = DatagramPacket(buffer, buffer.size, InetAddress.getByName(DEFAULT_TELLO_IP), DEFAULT_TELLO_PORT)
                udpSocket?.send(packet)
                logMsg("→ Sent: $actualCommand")
            } catch (e: Exception) {
                logMsg("Failed to send '$actualCommand': ${e.message}")
            }

            val resp = receiveOnce(false)
            logMsg("Connect receive: ${resp ?: "<timeout/no response>"}")

            // Accept if contains "ok" (some packets may be state data; sometimes ok appears later)
            resp?.toByteArray(Charsets.UTF_8) ?: ByteArray(0)
        } catch (e: Exception) {
            logMsg("Connection failed: ${e.message}")
            e.printStackTrace()
            ByteArray(0)
        }
    }

    override suspend fun send(message: CommandType) = withContext(Dispatchers.IO) {
        val actualCommand = when(message) {
            CommandType.TAKEOFF -> "takeoff"
            CommandType.LAND -> "land"
            CommandType.EMERGENCY -> "emergency"
            CommandType.UP -> "up 20"
            CommandType.DOWN -> "down 20"
            CommandType.LEFT -> "left 20"
            CommandType.RIGHT -> "right 20"
            CommandType.FORWARD -> "forward 20"
            CommandType.BACKWARD -> "back 20"
            CommandType.ROTATE_CLOCKWISE -> "cw 90"
            CommandType.ROTATE_COUNTERCLOCKWISE -> "ccw 90"
            CommandType.FLIP_FORWARD -> "flip f"
            CommandType.FLIP_BACKWARD -> "flip b"
            CommandType.FLIP_LEFT -> "flip l"
            CommandType.FLIP_RIGHT -> "flip r"
            CommandType.GET_BATTERY -> "battery?"
            CommandType.GET_SPEED -> "speed?"
            CommandType.SET_SPEED -> "speed 50"
            CommandType.GET_TIME -> "time?"
            CommandType.GET_HEIGHT -> "height?"
            CommandType.GET_TEMPERATURE -> "temp?"
            CommandType.GET_ACCELERATION -> "acceleration?"
            CommandType.GET_TOF -> "tof?"
            CommandType.GET_BAROMETER -> "baro?"
            CommandType.CONNECT -> "command"
            CommandType.STOP -> "stop"
            CommandType.STREAM_ON -> "streamon"
            CommandType.STREAM_OFF -> "streamoff"
        }

        if (udpSocket == null) {
            logMsg("Socket not connected: call connect() first")
            return@withContext
        }

        try {
            val buffer = actualCommand.toByteArray(Charsets.UTF_8)
            val packet = DatagramPacket(buffer, buffer.size, InetAddress.getByName(DEFAULT_TELLO_IP), DEFAULT_TELLO_PORT)
            udpSocket?.send(packet)
            logMsg("→ Sent: $actualCommand")
        } catch (e: Exception) {
            logMsg("Failed to send '$actualCommand': ${e.message}")
        }
    }

    override suspend fun getTelemetryData(oldTelemetryData: TelemetryData): TelemetryData = withContext(Dispatchers.IO) {
        if (udpSocket == null) {
            logMsg("getTelemetryData called but socket is not connected")
            return@withContext TelemetryData()
        }

        val telemetryCommands = mutableMapOf(
            "battery?" to "battery",
            "speed?" to "speed",
            "time?" to "time",
            "wifi?" to "wifi",
            "sdk?" to "sdk",
            "sn?" to "sn"
        )

        if(wifiAlreadyAsked){
            telemetryCommands.remove("wifi?")
        }
        if(sdkAlreadyAsked){
            telemetryCommands.remove("sdk?")
        }
        if(serialAlreadyAsked){
            telemetryCommands.remove("sn?")
        }

        try {
            var finalTelemetryData = oldTelemetryData

            telemetryCommands.forEach { (cmd, type) ->
                val buf = cmd.toByteArray(Charsets.UTF_8)
                val packet = DatagramPacket(buf, buf.size, InetAddress.getByName(DEFAULT_TELLO_IP), DEFAULT_TELLO_PORT)
                udpSocket?.send(packet)
                logMsg("Sent telemetry request: $cmd")

                var responseReceived = false

                delay(1000)

                val resp = receiveOnce(false)

                if (resp != null && resp.isNotEmpty()) {
                    when (type) {
                        "battery" -> {
                            finalTelemetryData = finalTelemetryData.copy(battery = resp.toInt())
                            responseReceived = true
                            logMsg("✓ Battery: $resp%")
                        }
                        "speed" -> {
                            finalTelemetryData = finalTelemetryData.copy(speed = resp)
                            responseReceived = true
                            logMsg("✓ Speed: $resp cm/s")
                        }
                        "time" -> {
                            finalTelemetryData = finalTelemetryData.copy(flightTime = resp)
                            responseReceived = true
                            logMsg("✓ Flight time: $resp seconds")
                        }
                        "wifi" -> {
                            finalTelemetryData = finalTelemetryData.copy(wifi = resp)
                            responseReceived = true
                            logMsg("✓ WiFi: $resp")
                            responseReceived = true
                            wifiAlreadyAsked = true
                        }
                        "sdk" -> {
                            finalTelemetryData = finalTelemetryData.copy(sdk = resp)
                            responseReceived = true
                            logMsg("✓ SDK: $resp")
                            responseReceived = true
                            sdkAlreadyAsked = true
                        }
                        "sn" -> {
                            finalTelemetryData = finalTelemetryData.copy(serialNumber = resp)
                            responseReceived = true
                            logMsg("✓ Serial: $resp")
                            responseReceived = true
                            serialAlreadyAsked = true
                        }
                    }
                }
            }

            finalTelemetryData
        } catch (e: Exception) {
            logMsg("getTelemetryData failed: ${e.message}")
            oldTelemetryData
        }
    }

    override suspend fun close() = withContext(Dispatchers.IO) {
        udpSocket?.close()
        udpSocket = null
        logMsg("Socket closed")
    }

    private fun receiveOnce(connection: Boolean): String? {
        if(connection) {
            try {
                val receiveBuffer = ByteArray(1518)
                val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
                udpSocket?.receive(receivePacket)
                val response = String(receivePacket.data, 0, receivePacket.length, Charsets.UTF_8).trim()
                return response
            } catch (e: SocketTimeoutException) {
                return null
            } catch (e: Exception) {
                logMsg("receiveOnce error: ${e.message}")
                return null
            }
        } else {
            try {
                var response = "ok"

                while(response.contains("ok") || response.contains("error")) {
                    val receiveBuffer = ByteArray(1518)
                    val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
                    udpSocket?.receive(receivePacket)
                    response = String(receivePacket.data, 0, receivePacket.length, Charsets.UTF_8).trim()
                }

                return response
            } catch (e: SocketTimeoutException) {
                return null
            } catch (e: Exception) {
                logMsg("receiveOnce error: ${e.message}")
                return null
            }
        }
    }
}
package com.sparklysparky.tellocontroller.classes

import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

const val DEFAULT_TELLO_IP = "192.168.10.1"
const val DEFAULT_TELLO_PORT = 8889

interface Client {
    suspend fun connect(): Boolean
    suspend fun send(message: CommandType)
    suspend fun close()
}

class UDPClient(): Client {
    private var udpSocket: DatagramSocket? = null

    override suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            udpSocket = DatagramSocket(9000)
            udpSocket?.soTimeout = 5000 // 5 second timeout

            // Send "command"
            val buffer = "command".toByteArray()
            val packet = DatagramPacket(
                buffer,
                buffer.size,
                InetAddress.getByName(DEFAULT_TELLO_IP),
                DEFAULT_TELLO_PORT
            )
            udpSocket?.send(packet)

            // Wait for "ok" response
            val receiveBuffer = ByteArray(1518)
            val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
            udpSocket?.receive(receivePacket)

            val response = String(receivePacket.data, 0, receivePacket.length).trim()
            logMsg("Response: $response")

            response.equals("ok", ignoreCase = true)
        } catch (e: Exception) {
            logMsg("Connection failed: ${e.message}")
            false
        }
    }

    override suspend fun send(message: CommandType) {
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

        logMsg(actualCommand)

        val buffer = actualCommand.toByteArray()
        val packet = DatagramPacket(
            buffer,
            buffer.size,
            InetAddress.getByName(DEFAULT_TELLO_IP),
            DEFAULT_TELLO_PORT
        )

        withContext(Dispatchers.IO) {
            udpSocket?.send(packet)
        }
    }

    override suspend fun close() {
        withContext(Dispatchers.IO) {
            udpSocket?.close()
            udpSocket = null
        }
    }
}
package com.sparklysparky.tellocontroller.classes

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.*
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Java2DFrameConverter
import org.jetbrains.skia.Image as SkiaImage
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.net.ServerSocket
import java.net.Socket
import javax.imageio.ImageIO



class VideoDecoder {
    private var isRunning = false
    private val frameBuffer = ByteArrayOutputStream()
    private var frameCount = 0
    private var totalBytes = 0L
    private var decodedFrames = 0

    private var onFrameCallback: ((ImageBitmap) -> Unit)? = null
    private var onStatsCallback: ((Int, Long, Int) -> Unit)? = null

    // TCP server for feeding H.264 to FFmpeg
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var transmitterJob: Job? = null
    private var decoderJob: Job? = null
    private val frameConverter = Java2DFrameConverter()

    private val h264Frames = mutableListOf<ByteArray>()
    private val frameLock = Any()

    // Frame skipping for lower latency
    private var frameSkipCounter = 0
    private val frameSkipNumber = 2 // Process every 2nd frame (15 FPS instead of 30)

    init {
        avutil.av_log_set_level(avutil.AV_LOG_QUIET)
    }

    fun start(
        onFrame: (ImageBitmap) -> Unit,
    ) {
        isRunning = true
        onFrameCallback = onFrame

        startTransmitter()
        startDecoder()

        logMsg("✓ Video decoder started")
    }

    private fun startTransmitter() {
        transmitterJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                serverSocket = ServerSocket(12345).apply {
                    reuseAddress = true
                    soTimeout = 0 // Non-blocking
                }
                logMsg("✓ H.264 transmitter listening on port 12345")

                while (isRunning) {
                    clientSocket = serverSocket?.accept()
                    logMsg("✓ FFmpeg connected to transmitter")

                    val outputStream = clientSocket?.getOutputStream()

                    while (isRunning && clientSocket?.isConnected == true) {
                        try {
                            val frame = synchronized(frameLock) {
                                if (h264Frames.isNotEmpty()) {
                                    h264Frames.removeAt(0)
                                } else {
                                    null
                                }
                            }

                            if (frame != null) {
                                outputStream?.write(frame)
                                outputStream?.flush()
                            } else {
                                delay(1) // Very short wait
                            }
                        } catch (e: Exception) {
                            if (isRunning) {
                                logMsg("Transmitter error: ${e.message}")
                            }
                            break
                        }
                    }

                    clientSocket?.close()
                }
            } catch (e: Exception) {
                logMsg("Transmitter server error: ${e.message}")
            }
        }
    }

    private fun startDecoder() {
        decoderJob = CoroutineScope(Dispatchers.IO).launch {
            delay(500) // Shorter wait

            try {
                val grabber = FFmpegFrameGrabber("tcp://127.0.0.1:12345")

                // Set format and dimensions
                grabber.format = "h264"
                grabber.imageWidth = 960
                grabber.imageHeight = 720

                grabber.setOption("fflags", "nobuffer")
                grabber.setOption("flags", "low_delay")
                grabber.setOption("probesize", "32")
                grabber.setOption("analyzeduration", "0")
                grabber.setOption("sync", "ext")

                grabber.start()
                logMsg("✓ FFmpeg decoder connected")

                while (isRunning) {
                    try {
                        val frame = grabber.grab()

                        if (frame != null && frame.image != null) {
                            decodedFrames++

                            // Skip frames for lower latency
                            frameSkipCounter++
                            if (frameSkipCounter % frameSkipNumber != 0) {
                                continue // Skip this frame
                            }

                            val bufferedImage: BufferedImage? = frameConverter.convert(frame)

                            if (bufferedImage != null) {
                                val bitmap = bufferedImageToImageBitmap(bufferedImage)

                                withContext(Dispatchers.Main) {
                                    onFrameCallback?.invoke(bitmap)
                                }
                            }
                        }
                    } catch (_: Exception) {
                    }
                }

                grabber.stop()
                grabber.release()
            } catch (_: Exception) {
                if (isRunning) {
                    delay(2000) // Shorter retry delay
                    startDecoder()
                }
            }
        }
    }

    private fun bufferedImageToImageBitmap(bufferedImage: BufferedImage): ImageBitmap {
        val outputStream = ByteArrayOutputStream()
        ImageIO.write(bufferedImage, "PNG", outputStream)
        val pngBytes = outputStream.toByteArray()

        val skiaImage = SkiaImage.makeFromEncoded(pngBytes)
        return skiaImage.toComposeImageBitmap()
    }

    fun feedPacket(packet: ByteArray) {
        if (!isRunning) return

        try {
            frameBuffer.write(packet)

            // Frame complete when packet size != 1460
            if (packet.size != 1460) {
                val completeFrame = frameBuffer.toByteArray()
                val frameSize = completeFrame.size
                frameBuffer.reset()
                frameCount++
                totalBytes += frameSize

                synchronized(frameLock) {
                    h264Frames.add(completeFrame)

                    while (h264Frames.size > 5) {
                        h264Frames.removeAt(0)
                    }
                }

                // Update stats
                onStatsCallback?.invoke(frameCount, totalBytes, frameSize)

            }
        } catch (e: Exception) {
            logMsg("Feed error: ${e.message}")
        }
    }

    fun stop() {
        isRunning = false

        transmitterJob?.cancel()
        decoderJob?.cancel()

        try {
            clientSocket?.close()
            serverSocket?.close()
        } catch (_: Exception) {
            // Ignore
        }

        frameBuffer.reset()
        synchronized(frameLock) {
            h264Frames.clear()
        }

        logMsg("Video decoder stopped - $frameCount frames received, $decodedFrames decoded (${totalBytes / 1024} KB)")
        frameCount = 0
        totalBytes = 0
        decodedFrames = 0
        frameSkipCounter = 0
    }
}
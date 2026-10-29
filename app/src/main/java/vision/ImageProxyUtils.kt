package com.aira.accessibility.vision

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

object ImageProxyUtils {

    fun imageProxyToBitmap(
        imageProxy: ImageProxy
    ): Bitmap? {

        val image = imageProxy.image
            ?: return null

        if (image.format != ImageFormat.YUV_420_888) {
            return null
        }

        return try {

            val nv21 = yuv420888ToNv21(imageProxy)

            val yuvImage = YuvImage(
                nv21,
                ImageFormat.NV21,
                image.width,
                image.height,
                null
            )

            val outputStream =
                ByteArrayOutputStream()

            yuvImage.compressToJpeg(
                Rect(
                    0,
                    0,
                    image.width,
                    image.height
                ),
                90,
                outputStream
            )

            val jpegBytes =
                outputStream.toByteArray()

            BitmapFactory.decodeByteArray(
                jpegBytes,
                0,
                jpegBytes.size
            )

        } catch (e: Exception) {

            e.printStackTrace()
            null
        }
    }

    private fun yuv420888ToNv21(
        image: ImageProxy
    ): ByteArray {

        val width = image.width
        val height = image.height

        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        yBuffer.rewind()
        uBuffer.rewind()
        vBuffer.rewind()

        val ySize = width * height
        val uvSize = width * height / 2

        val nv21 =
            ByteArray(ySize + uvSize)

        var position = 0

        val yRowStride =
            yPlane.rowStride

        val yPixelStride =
            yPlane.pixelStride

        val yRow =
            ByteArray(yRowStride)

        for (row in 0 until height) {

            val bytesToRead =
                minOf(
                    yRowStride,
                    yBuffer.remaining()
                )

            yBuffer.get(
                yRow,
                0,
                bytesToRead
            )

            var col = 0

            while (col < width) {

                val index =
                    col * yPixelStride

                if (index < bytesToRead) {
                    nv21[position++] =
                        yRow[index]
                }

                col++
            }
        }

        val uRowStride =
            uPlane.rowStride

        val uPixelStride =
            uPlane.pixelStride

        val vRowStride =
            vPlane.rowStride

        val vPixelStride =
            vPlane.pixelStride

        val uRow =
            ByteArray(uRowStride)

        val vRow =
            ByteArray(vRowStride)

        val chromaHeight =
            height / 2

        val chromaWidth =
            width / 2

        for (row in 0 until chromaHeight) {

            val uBytes =
                minOf(
                    uRowStride,
                    uBuffer.remaining()
                )

            val vBytes =
                minOf(
                    vRowStride,
                    vBuffer.remaining()
                )

            uBuffer.get(
                uRow,
                0,
                uBytes
            )

            vBuffer.get(
                vRow,
                0,
                vBytes
            )

            for (col in 0 until chromaWidth) {

                val uIndex =
                    col * uPixelStride

                val vIndex =
                    col * vPixelStride

                if (
                    uIndex < uBytes &&
                    vIndex < vBytes
                ) {

                    nv21[position++] =
                        vRow[vIndex]

                    nv21[position++] =
                        uRow[uIndex]
                }
            }
        }

        return nv21
    }
}
package com.aira.accessibility.vision

import android.content.Context
import android.util.Log

import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectorResult

class ObjectDetectorHelper(
    private val context: Context,
    private val onResult: (ObjectDetectorResult) -> Unit,
    private val onError: (String) -> Unit
) {

    companion object {
        private const val TAG = "AIRA_OBJECT"
        private const val MODEL_NAME =
            "efficientdet_lite0.tflite"
    }

    private var objectDetector: ObjectDetector? = null

    init {
        setupObjectDetector()
    }

    private fun setupObjectDetector() {

        try {

            Log.d(
                TAG,
                "Starting MediaPipe Object Detector..."
            )

            val baseOptions =
                BaseOptions.builder()
                    .setModelAssetPath(MODEL_NAME)
                    .build()

            val options =
                ObjectDetector.ObjectDetectorOptions
                    .builder()
                    .setBaseOptions(baseOptions)
                    .setRunningMode(
                        RunningMode.LIVE_STREAM
                    )
                    .setScoreThreshold(0.25f)
                    .setMaxResults(10)
                    .setResultListener { result, timestamp ->

                        Log.d(
                            TAG,
                            "Detection callback received"
                        )

                        Log.d(
                            TAG,
                            "Timestamp: $timestamp"
                        )

                        Log.d(
                            TAG,
                            "Total detections: " +
                                    result.detections().size
                        )

                        /*
                         * Print EVERY detected object
                         * into Logcat.
                         */
                        result.detections()
                            .forEachIndexed { index, detection ->

                                val category =
                                    detection
                                        .categories()
                                        .firstOrNull()

                                if (category != null) {

                                    Log.d(
                                        TAG,
                                        "Object #$index"
                                    )

                                    Log.d(
                                        TAG,
                                        "Label: " +
                                                category
                                                    .categoryName()
                                    )

                                    Log.d(
                                        TAG,
                                        "Confidence: " +
                                                category.score()
                                    )

                                    Log.d(
                                        TAG,
                                        "Bounding box: " +
                                                detection
                                                    .boundingBox()
                                    )
                                }
                            }

                        /*
                         * Send result to VisionScreen.
                         */
                        onResult(result)
                    }
                    .setErrorListener { error ->

                        val message =
                            error.message
                                ?: "Unknown MediaPipe error"

                        Log.e(
                            TAG,
                            "MediaPipe error: $message"
                        )

                        onError(message)
                    }
                    .build()

            objectDetector =
                ObjectDetector.createFromOptions(
                    context,
                    options
                )

            Log.d(
                TAG,
                "MediaPipe Object Detector initialized successfully"
            )

            Log.d(
                TAG,
                "Model: $MODEL_NAME"
            )

            Log.d(
                TAG,
                "Confidence threshold: 0.25"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Object detector initialization failed",
                e
            )

            onError(
                e.message
                    ?: "Failed to initialize object detector"
            )
        }
    }

    fun detect(
        bitmap: android.graphics.Bitmap,
        timestamp: Long
    ) {

        try {

            if (objectDetector == null) {

                Log.e(
                    TAG,
                    "Detector is not initialized"
                )

                return
            }

            /*
             * Convert Android Bitmap into
             * MediaPipe MPImage.
             */
            val mpImage: MPImage =
                BitmapImageBuilder(bitmap)
                    .build()

            Log.d(
                TAG,
                "Sending frame to MediaPipe: " +
                        "${bitmap.width}x${bitmap.height}"
            )

            /*
             * LIVE_STREAM mode requires
             * detectAsync().
             */
            objectDetector?.detectAsync(
                mpImage,
                timestamp
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Detection failed",
                e
            )

            onError(
                e.message
                    ?: "Detection failed"
            )
        }
    }

    fun close() {

        try {

            Log.d(
                TAG,
                "Closing MediaPipe Object Detector"
            )

            objectDetector?.close()

            objectDetector = null

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Error closing detector",
                e
            )
        }
    }
}
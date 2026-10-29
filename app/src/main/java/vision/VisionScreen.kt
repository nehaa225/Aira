package com.aira.accessibility.vision

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectorResult

import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun VisionScreen(
    onBack: () -> Unit
) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var cameraPermissionGranted by remember {

        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            cameraPermissionGranted = granted
        }

    LaunchedEffect(Unit) {

        if (!cameraPermissionGranted) {

            permissionLauncher.launch(
                Manifest.permission.CAMERA
            )
        }
    }

    if (!cameraPermissionGranted) {

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "Camera permission is required",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                Button(
                    onClick = {
                        permissionLauncher.launch(
                            Manifest.permission.CAMERA
                        )
                    }
                ) {

                    Text("Allow Camera")
                }

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Button(
                    onClick = onBack
                ) {

                    Text("Back")
                }
            }
        }

        return
    }

    var objectResult by remember {

        mutableStateOf(
            "Looking for objects..."
        )
    }

    var ocrResult by remember {

        mutableStateOf(
            "No text detected"
        )
    }

    var status by remember {

        mutableStateOf(
            "AIRA Vision is ready"
        )
    }

    val tts = remember {

        TextToSpeech(context) { result ->

            if (result == TextToSpeech.SUCCESS) {

                Log.d(
                    "AIRA_TTS",
                    "Text to speech initialized"
                )
            }
        }
    }

    val detector = remember {

        ObjectDetectorHelper(
            context = context,

            onResult = { result ->

                objectResult =
                    createObjectDescription(result)

                status =
                    if (result.detections().isEmpty()) {

                        "No known objects detected"

                    } else {

                        "${result.detections().size} object(s) detected"
                    }
            },

            onError = { error ->

                Log.e(
                    "AIRA_VISION",
                    error
                )

                status =
                    "Vision error: $error"
            }
        )
    }

    DisposableEffect(Unit) {

        onDispose {

            detector.close()

            tts.stop()
            tts.shutdown()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        AndroidView(

            factory = { ctx ->

                PreviewView(ctx).apply {

                    layoutParams =
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                    scaleType =
                        PreviewView.ScaleType.FILL_CENTER
                }
            },

            modifier = Modifier.fillMaxSize(),

            update = { previewView ->

                startCamera(
                    context = context,
                    previewView = previewView,
                    lifecycleOwner = lifecycleOwner,
                    detector = detector,

                    onOcrResult = { text ->

                        ocrResult =
                            if (text.isBlank()) {

                                "No text detected"

                            } else {

                                text
                            }
                    }
                )
            }
        )

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(12.dp)
        ) {

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier = Modifier.padding(16.dp)
                ) {

                    Text(
                        text = "👁 AIRA Vision",
                        style =
                            MaterialTheme.typography.titleLarge
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = status
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = objectResult,
                        style =
                            MaterialTheme.typography.bodyLarge
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = "OCR: $ocrResult",
                        style =
                            MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                Button(
                    onClick = {

                        speak(
                            tts,
                            createSpeechText(objectResult)
                        )
                    }
                ) {

                    Text("🔊 Speak")
                }

                Button(
                    onClick = onBack
                ) {

                    Text("Back")
                }
            }
        }
    }
}


private fun startCamera(
    context: Context,
    previewView: PreviewView,
    lifecycleOwner: LifecycleOwner,
    detector: ObjectDetectorHelper,
    onOcrResult: (String) -> Unit
) {

    val cameraProviderFuture =
        ProcessCameraProvider.getInstance(context)

    cameraProviderFuture.addListener({

        try {

            val cameraProvider =
                cameraProviderFuture.get()

            val preview =
                Preview.Builder()
                    .build()

            preview.setSurfaceProvider(
                previewView.surfaceProvider
            )

            val analysis =
                ImageAnalysis.Builder()
                    .setBackpressureStrategy(
                        ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                    )
                    .build()

            val executor =
                Executors.newSingleThreadExecutor()

            val lastDetectionTime =
                longArrayOf(0L)

            val lastOcrTime =
                longArrayOf(0L)

            val ocrBusy =
                AtomicBoolean(false)

            val recognizer =
                TextRecognition.getClient(
                    TextRecognizerOptions.DEFAULT_OPTIONS
                )

            analysis.setAnalyzer(executor) { imageProxy ->

                try {

                    val now =
                        System.currentTimeMillis()

                    /*
                     * MediaPipe object detection
                     */
                    if (
                        now - lastDetectionTime[0] >= 700
                    ) {

                        lastDetectionTime[0] = now

                        val bitmap =
                            ImageProxyUtils.imageProxyToBitmap(
                                imageProxy
                            )

                        if (bitmap != null) {

                            detector.detect(
                                bitmap,
                                now
                            )
                        }
                    }

                    /*
                     * OCR
                     */
                    if (
                        now - lastOcrTime[0] >= 2000 &&
                        !ocrBusy.get()
                    ) {

                        val bitmap =
                            ImageProxyUtils.imageProxyToBitmap(
                                imageProxy
                            )

                        if (bitmap != null) {

                            lastOcrTime[0] = now

                            ocrBusy.set(true)

                            val inputImage =
                                InputImage.fromBitmap(
                                    bitmap,
                                    0
                                )

                            recognizer
                                .process(inputImage)
                                .addOnSuccessListener { result ->

                                    val text =
                                        result.text.trim()

                                    if (text.isNotBlank()) {

                                        onOcrResult(text)
                                    }
                                }
                                .addOnFailureListener { error ->

                                    Log.e(
                                        "AIRA_OCR",
                                        "OCR failed",
                                        error
                                    )
                                }
                                .addOnCompleteListener {

                                    ocrBusy.set(false)
                                }
                        }
                    }

                } catch (e: Exception) {

                    Log.e(
                        "AIRA_CAMERA",
                        "Frame analysis error",
                        e
                    )

                } finally {

                    imageProxy.close()
                }
            }

            cameraProvider.unbindAll()

            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis
            )

        } catch (e: Exception) {

            Log.e(
                "AIRA_CAMERA",
                "Camera startup failed",
                e
            )
        }

    }, ContextCompat.getMainExecutor(context))
}


private fun createObjectDescription(
    result: ObjectDetectorResult
): String {

    val detections =
        result.detections()

    if (detections.isEmpty()) {

        return "No known objects detected."
    }

    val descriptions =
        mutableListOf<String>()

    for (detection in detections) {

        val category =
            detection.categories().firstOrNull()
                ?: continue

        val label =
            category.categoryName()

        val score =
            category.score()

        val box =
            detection.boundingBox()

        /*
         * Bounding box center.
         *
         * We compare it with the image width
         * to determine left / center / right.
         */
        val centerX =
            box.centerX()

        val imageWidth =
            box.right - box.left

        /*
         * MediaPipe bounding boxes use image
         * coordinates, so we cannot use a fixed
         * 0.33 / 0.66 value here.
         *
         * For now, estimate position from
         * the bounding box location.
         */
        val position =
            when {

                centerX < 300 ->
                    "on the left"

                centerX > 700 ->
                    "on the right"

                else ->
                    "in the center"
            }

        val readableLabel =
            label
                .replace("_", " ")
                .trim()

        val confidence =
            (score * 100).toInt()

        descriptions.add(
            "$readableLabel $position " +
                    "with $confidence percent confidence"
        )
    }

    return descriptions.joinToString("\n")
}


private fun createSpeechText(
    objectResult: String
): String {

    if (
        objectResult.isBlank() ||
        objectResult.contains(
            "No known objects",
            ignoreCase = true
        )
    ) {

        return "I don't detect any known objects."
    }

    val lines =
        objectResult
            .split("\n")
            .filter {
                it.isNotBlank()
            }

    return if (lines.size == 1) {

        "I detect ${lines[0]}."

    } else {

        "I detect ${lines.take(4).joinToString(", ")}."
    }
}


private fun speak(
    tts: TextToSpeech,
    text: String
) {

    if (text.isBlank()) {
        return
    }

    tts.language = Locale.US

    tts.speak(
        text,
        TextToSpeech.QUEUE_FLUSH,
        null,
        "AIRA_VISION"
    )
}
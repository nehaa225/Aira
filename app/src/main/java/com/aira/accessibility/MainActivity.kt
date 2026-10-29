package com.aira.accessibility

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import androidx.core.content.ContextCompat

import com.aira.accessibility.vision.VisionScreen

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import java.util.Locale


class MainActivity : ComponentActivity() {

    companion object {

        private const val TAG = "AIRA_VOICE"
    }


    // =========================================
    // Speech Recognition
    // =========================================

    private var speechRecognizer: SpeechRecognizer? = null


    // =========================================
    // Text To Speech
    // =========================================

    private var textToSpeech: TextToSpeech? = null


    // =========================================
    // UI State
    // =========================================

    private var listeningState =
        mutableStateOf(false)

    private var recognizedText =
        mutableStateOf("")

    private var resultText =
        mutableStateOf("")

    private var currentScreen =
        mutableStateOf("dashboard")


    // =========================================
    // Microphone Permission
    // =========================================

    private val microphonePermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {

                startListening()

            } else {

                val message =
                    "Microphone permission is required for AIRA voice control."

                resultText.value =
                    message

                speak(message)
            }
        }


    // =========================================
    // Activity Created
    // =========================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )


        initializeTextToSpeech()


        setContent {

            MaterialTheme {

                when (currentScreen.value) {

                    "dashboard" -> {

                        AiraDashboard(

                            recognizedText =
                                recognizedText.value,

                            resultText =
                                resultText.value,

                            isListening =
                                listeningState.value,

                            onTalkClick = {

                                startVoiceAssistant()
                            },

                            onVoiceServiceStart = {

                                startAiraVoiceService()
                            },

                            onVoiceServiceStop = {

                                stopAiraVoiceService()
                            },

                            onVisionClick = {

                                currentScreen.value =
                                    "vision"
                            }
                        )
                    }


                    "vision" -> {

                        VisionScreen(

                            onBack = {

                                currentScreen.value =
                                    "dashboard"
                            }
                        )
                    }
                }
            }
        }
    }


    // =========================================
    // Text To Speech Initialization
    // =========================================

    private fun initializeTextToSpeech() {

        textToSpeech =
            TextToSpeech(
                this
            ) { status ->

                if (
                    status ==
                    TextToSpeech.SUCCESS
                ) {

                    textToSpeech?.language =
                        Locale.US

                    textToSpeech?.setSpeechRate(
                        0.95f
                    )

                    Log.d(
                        TAG,
                        "Text to Speech initialized"
                    )
                }
            }
    }


    // =========================================
    // Start Voice Assistant
    // =========================================

    private fun startVoiceAssistant() {

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            microphonePermissionLauncher.launch(
                Manifest.permission.RECORD_AUDIO
            )

            return
        }


        startListening()
    }


    // =========================================
    // Start Speech Recognition
    // =========================================

    private fun startListening() {

        if (
            !SpeechRecognizer.isRecognitionAvailable(
                this
            )
        ) {

            val message =
                "Speech recognition is not available on this phone."

            resultText.value =
                message

            speak(message)

            return
        }


        speechRecognizer?.destroy()


        speechRecognizer =
            SpeechRecognizer.createSpeechRecognizer(
                this
            )


        speechRecognizer?.setRecognitionListener(

            object : RecognitionListener {


                override fun onReadyForSpeech(
                    params: Bundle?
                ) {

                    listeningState.value =
                        true

                    recognizedText.value =
                        "Listening..."

                    resultText.value =
                        ""

                    Log.d(
                        TAG,
                        "AIRA listening"
                    )
                }


                override fun onBeginningOfSpeech() {

                    listeningState.value =
                        true

                    recognizedText.value =
                        "Listening..."
                }


                override fun onRmsChanged(
                    rmsdB: Float
                ) {
                }


                override fun onBufferReceived(
                    buffer: ByteArray?
                ) {
                }


                override fun onEndOfSpeech() {

                    listeningState.value =
                        false

                    Log.d(
                        TAG,
                        "Speech ended"
                    )
                }


                override fun onError(
                    error: Int
                ) {

                    listeningState.value =
                        false

                    val message =
                        speechErrorMessage(
                            error
                        )

                    Log.e(
                        TAG,
                        "Speech error: $message"
                    )

                    resultText.value =
                        message

                    if (
                        error !=
                        SpeechRecognizer.ERROR_NO_MATCH
                    ) {

                        speak(message)
                    }
                }


                override fun onResults(
                    results: Bundle?
                ) {

                    listeningState.value =
                        false


                    val matches =
                        results?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )


                    val request =
                        matches
                            ?.firstOrNull()
                            ?.trim()


                    if (
                        request.isNullOrEmpty()
                    ) {

                        val message =
                            "I couldn't understand you."

                        resultText.value =
                            message

                        speak(message)

                        return
                    }


                    recognizedText.value =
                        request


                    Log.d(
                        TAG,
                        "User said: $request"
                    )


                    processVoiceRequest(
                        request
                    )
                }


                override fun onPartialResults(
                    partialResults: Bundle?
                ) {

                    val partial =
                        partialResults
                            ?.getStringArrayList(
                                SpeechRecognizer.RESULTS_RECOGNITION
                            )
                            ?.firstOrNull()


                    if (
                        !partial.isNullOrEmpty()
                    ) {

                        recognizedText.value =
                            partial
                    }
                }


                override fun onEvent(
                    eventType: Int,
                    params: Bundle?
                ) {
                }
            }
        )


        val intent =
            Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            ).apply {

                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,

                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )


                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,

                    Locale.getDefault()
                )


                putExtra(
                    RecognizerIntent.EXTRA_PARTIAL_RESULTS,

                    true
                )


                putExtra(
                    RecognizerIntent.EXTRA_MAX_RESULTS,

                    5
                )
            }


        speechRecognizer?.startListening(
            intent
        )
    }


    // =========================================
    // Send Voice Request To AIRA Agent
    // =========================================

    private fun processVoiceRequest(
        request: String
    ) {

        val service =
            AiraAccessibilityService.getInstance()


        if (service == null) {

            val message =
                "AIRA Accessibility Service is not connected."

            resultText.value =
                message

            speak(message)

            return
        }


        resultText.value =
            "AIRA is thinking..."


        CoroutineScope(
            Dispatchers.Main
        ).launch {

            try {

                val result =
                    service.processAiraRequest(
                        request
                    )


                Log.d(
                    TAG,
                    "Agent result: $result"
                )


                resultText.value =
                    result


                speak(
                    result
                )


            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Agent processing failed",
                    e
                )


                val message =
                    "I couldn't complete that request."


                resultText.value =
                    message


                speak(
                    message
                )
            }
        }
    }


    // =========================================
    // START BACKGROUND VOICE SERVICE
    // =========================================

    private fun startAiraVoiceService() {

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            microphonePermissionLauncher.launch(
                Manifest.permission.RECORD_AUDIO
            )

            return
        }


        val intent =
            Intent(
                this,
                AiraVoiceService::class.java
            ).apply {

                action =
                    AiraVoiceService.ACTION_START
            }


        if (
            android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.O
        ) {

            startForegroundService(
                intent
            )

        } else {

            startService(
                intent
            )
        }


        resultText.value =
            "AIRA background voice service started."

        Log.d(
            TAG,
            "Background voice service started"
        )
    }


    // =========================================
    // STOP BACKGROUND VOICE SERVICE
    // =========================================

    private fun stopAiraVoiceService() {

        val intent =
            Intent(
                this,
                AiraVoiceService::class.java
            ).apply {

                action =
                    AiraVoiceService.ACTION_STOP
            }


        startService(
            intent
        )


        resultText.value =
            "AIRA background voice service stopped."

        Log.d(
            TAG,
            "Background voice service stopped"
        )
    }


    // =========================================
    // Text To Speech
    // =========================================

    private fun speak(
        text: String
    ) {

        if (
            text.isBlank()
        ) {
            return
        }


        textToSpeech?.speak(

            text,

            TextToSpeech.QUEUE_FLUSH,

            null,

            "AIRA_RESPONSE"
        )
    }


    // =========================================
    // Speech Error Messages
    // =========================================

    private fun speechErrorMessage(
        error: Int
    ): String {

        return when (error) {

            SpeechRecognizer.ERROR_AUDIO ->
                "There was an audio recording problem."


            SpeechRecognizer.ERROR_CLIENT ->
                "There was a speech recognition problem."


            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                "Microphone permission is not available."


            SpeechRecognizer.ERROR_NETWORK ->
                "The speech recognition network is unavailable."


            SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                "Speech recognition timed out."


            SpeechRecognizer.ERROR_NO_MATCH ->
                "I couldn't understand what you said."


            SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                "Speech recognition is busy."


            SpeechRecognizer.ERROR_SERVER ->
                "The speech recognition service failed."


            SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                "I didn't hear anything."


            else ->
                "I couldn't understand the voice request."
        }
    }


    // =========================================
    // Activity Destroyed
    // =========================================

    override fun onDestroy() {

        speechRecognizer?.destroy()


        textToSpeech?.stop()

        textToSpeech?.shutdown()


        super.onDestroy()
    }
}


/* ========================================================= */
/* AIRA DASHBOARD                                             */
/* ========================================================= */

@Composable
fun AiraDashboard(

    recognizedText: String,

    resultText: String,

    isListening: Boolean,

    onTalkClick: () -> Unit,

    onVoiceServiceStart: () -> Unit,

    onVoiceServiceStop: () -> Unit,

    onVisionClick: () -> Unit
) {

    Column(

        modifier =
            Modifier
                .fillMaxSize()
                .padding(20.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Top
    ) {


        // =========================================
        // AIRA TITLE
        // =========================================

        Text(

            text = "AIRA",

            style =
                MaterialTheme
                    .typography
                    .headlineLarge
        )


        Text(

            text =
                "AI Accessibility Assistant",

            style =
                MaterialTheme
                    .typography
                    .bodyLarge
        )


        Spacer(
            modifier =
                Modifier.height(24.dp)
        )


        // =========================================
        // VOICE CARD
        // =========================================

        Card(

            modifier =
                Modifier.fillMaxWidth()
        ) {

            Column(

                modifier =
                    Modifier.padding(20.dp),

                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {


                Text(

                    text =

                        if (isListening)

                            "🎤 Listening..."

                        else

                            "🎤 Talk to AIRA",

                    style =
                        MaterialTheme
                            .typography
                            .titleLarge
                )


                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )


                Text(
                    text =
                        "Natural-language AI phone control"
                )


                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )


                // =================================
                // TALK BUTTON
                // =================================

                Button(

                    onClick =
                        onTalkClick
                ) {

                    Text(

                        if (isListening)

                            "Listening..."

                        else

                            "Talk to AIRA"
                    )
                }


                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )


                // =================================
                // START BACKGROUND SERVICE
                // =================================

                Button(

                    onClick =
                        onVoiceServiceStart
                ) {

                    Text(
                        "Start Background AIRA"
                    )
                }


                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )


                // =================================
                // STOP BACKGROUND SERVICE
                // =================================

                Button(

                    onClick =
                        onVoiceServiceStop
                ) {

                    Text(
                        "Stop Background AIRA"
                    )
                }


                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )


                // =================================
                // RECOGNIZED SPEECH
                // =================================

                if (
                    recognizedText.isNotBlank()
                ) {

                    Text(

                        text =
                            "You said:\n$recognizedText"
                    )


                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )
                }


                // =================================
                // RESULT
                // =================================

                if (
                    resultText.isNotBlank()
                ) {

                    Text(

                        text =
                            resultText
                    )
                }
            }
        }


        Spacer(
            modifier =
                Modifier.height(20.dp)
        )


        // =========================================
        // VISION + ISL
        // =========================================

        AiraFeatureRow(

            leftTitle =
                "👁 Vision",

            rightTitle =
                "🤟 ISL",

            onLeftClick =
                onVisionClick,

            onRightClick = {}
        )


        Spacer(
            modifier =
                Modifier.height(12.dp)
        )


        // =========================================
        // COMMUNICATION + EMERGENCY
        // =========================================

        AiraFeatureRow(

            leftTitle =
                "💬 Communication",

            rightTitle =
                "🚨 Emergency",

            onLeftClick = {},

            onRightClick = {}
        )


        Spacer(
            modifier =
                Modifier.height(12.dp)
        )


        // =========================================
        // NAVIGATION + LANGUAGE
        // =========================================

        AiraFeatureRow(

            leftTitle =
                "📍 Navigation",

            rightTitle =
                "🌐 Language",

            onLeftClick = {},

            onRightClick = {}
        )
    }
}


/* ========================================================= */
/* FEATURE ROW                                                */
/* ========================================================= */

@Composable
fun AiraFeatureRow(

    leftTitle: String,

    rightTitle: String,

    onLeftClick: () -> Unit,

    onRightClick: () -> Unit
) {

    Row(

        modifier =
            Modifier.fillMaxWidth(),

        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {


        Button(

            onClick =
                onLeftClick,

            modifier =
                Modifier
                    .width(165.dp)
                    .height(70.dp)
        ) {

            Text(
                leftTitle
            )
        }


        Button(

            onClick =
                onRightClick,

            modifier =
                Modifier
                    .width(165.dp)
                    .height(70.dp)
        ) {

            Text(
                rightTitle
            )
        }
    }
}
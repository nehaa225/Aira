package com.aira.accessibility

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Locale

class AiraVoiceService : Service() {

    companion object {

        private const val TAG = "AIRA_VOICE_SERVICE"
        private const val WAKE_TAG = "AIRA_WAKE_WORD"
        private const val SPEECH_TAG = "AIRA_SPEECH"
        private const val AGENT_TAG = "AIRA_AGENT"

        private const val CHANNEL_ID = "aira_voice_assistant"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START =
            "com.aira.accessibility.START_VOICE"

        const val ACTION_STOP =
            "com.aira.accessibility.STOP_VOICE"

        private const val WAKE_PHRASE = "hey aira"
    }

    private var speechRecognizer: SpeechRecognizer? = null

    private var textToSpeech: TextToSpeech? = null

    private var ttsReady = false

    private var serviceRunning = false

    private var listeningForWakeWord = false

    private var listeningForCommand = false

    private var commandProcessing = false

    private var restartingRecognizer = false

    private val mainHandler =
        Handler(Looper.getMainLooper())

    private val serviceScope =
        CoroutineScope(
            SupervisorJob() + Dispatchers.Main
        )

    // =========================================================
    // SERVICE CREATE
    // =========================================================

    override fun onCreate() {

        super.onCreate()

        Log.d(
            TAG,
            "AIRA Voice Service created"
        )

        createNotificationChannel()

        initializeTextToSpeech()
    }

    // =========================================================
    // START COMMAND
    // =========================================================

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        when (intent?.action) {

            ACTION_STOP -> {

                Log.d(
                    TAG,
                    "Stop command received"
                )

                stopVoiceService()

                return START_NOT_STICKY
            }

            ACTION_START -> {

                Log.d(
                    TAG,
                    "Start command received"
                )

                startVoiceService()
            }

            else -> {

                Log.d(
                    TAG,
                    "Starting AIRA voice service"
                )

                startVoiceService()
            }
        }

        return START_STICKY
    }

    // =========================================================
    // START VOICE SERVICE
    // =========================================================

    private fun startVoiceService() {

        if (serviceRunning) {

            Log.d(
                TAG,
                "Voice service already running"
            )

            if (
                !listeningForWakeWord &&
                !listeningForCommand &&
                !commandProcessing
            ) {
                startWakeWordListening()
            }

            return
        }

        try {

            val notification =
                createNotification()

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q
            ) {

                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo
                        .FOREGROUND_SERVICE_TYPE_MICROPHONE
                )

            } else {

                startForeground(
                    NOTIFICATION_ID,
                    notification
                )
            }

            serviceRunning = true

            Log.d(
                TAG,
                "========================================"
            )

            Log.d(
                TAG,
                "AIRA VOICE ASSISTANT RUNNING"
            )

            Log.d(
                TAG,
                "Wake phrase: Hey AIRA"
            )

            Log.d(
                TAG,
                "========================================"
            )

            startWakeWordListening()

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Failed to start voice service",
                e
            )
        }
    }

    // =========================================================
    // WAKE WORD LISTENER
    // =========================================================

    private fun startWakeWordListening() {

        if (!serviceRunning) {
            return
        }

        if (commandProcessing) {
            return
        }

        if (listeningForWakeWord) {

            Log.d(
                WAKE_TAG,
                "Already listening for Hey AIRA"
            )

            return
        }

        if (listeningForCommand) {

            Log.d(
                WAKE_TAG,
                "Already listening for command"
            )

            return
        }

        if (
            !SpeechRecognizer
                .isRecognitionAvailable(this)
        ) {

            Log.e(
                WAKE_TAG,
                "Speech recognition is unavailable"
            )

            return
        }

        Log.d(
            WAKE_TAG,
            "========================================"
        )

        Log.d(
            WAKE_TAG,
            "STARTING AIRA WAKE LISTENER"
        )

        Log.d(
            WAKE_TAG,
            "Say: Hey AIRA"
        )

        Log.d(
            WAKE_TAG,
            "========================================"
        )

        destroySpeechRecognizer()

        speechRecognizer =
            createRecognizer()

        if (speechRecognizer == null) {

            Log.e(
                WAKE_TAG,
                "Could not create speech recognizer"
            )

            scheduleWakeRestart()

            return
        }

        listeningForWakeWord = true

        setupWakeListener()

        val intent =
            createRecognitionIntent()

        try {

            speechRecognizer?.startListening(
                intent
            )

            Log.d(
                WAKE_TAG,
                "Wake listener started"
            )

        } catch (e: Exception) {

            Log.e(
                WAKE_TAG,
                "Failed to start wake listener",
                e
            )

            listeningForWakeWord = false

            scheduleWakeRestart()
        }
    }

    // =========================================================
    // CREATE SPEECH RECOGNIZER
    // =========================================================

    private fun createRecognizer(): SpeechRecognizer? {

        return try {

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.S &&
                SpeechRecognizer
                    .isOnDeviceRecognitionAvailable(this)
            ) {

                Log.d(
                    TAG,
                    "Using Android on-device speech recognizer"
                )

                SpeechRecognizer
                    .createOnDeviceSpeechRecognizer(
                        this
                    )

            } else {

                Log.d(
                    TAG,
                    "Using Android speech recognizer"
                )

                SpeechRecognizer
                    .createSpeechRecognizer(
                        this
                    )
            }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Recognizer creation failed",
                e
            )

            null
        }
    }

    // =========================================================
    // WAKE LISTENER
    // =========================================================

    private fun setupWakeListener() {

        speechRecognizer?.setRecognitionListener(

            object : RecognitionListener {

                override fun onReadyForSpeech(
                    params: Bundle?
                ) {

                    Log.d(
                        WAKE_TAG,
                        "Wake listener READY"
                    )
                }

                override fun onBeginningOfSpeech() {

                    Log.d(
                        WAKE_TAG,
                        "Voice detected"
                    )
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

                    Log.d(
                        WAKE_TAG,
                        "Wake speech ended"
                    )
                }

                override fun onError(
                    error: Int
                ) {

                    Log.e(
                        WAKE_TAG,
                        "Wake recognizer error: $error"
                    )

                    listeningForWakeWord = false

                    if (
                        serviceRunning &&
                        !commandProcessing
                    ) {

                        scheduleWakeRestart()
                    }
                }

                override fun onResults(
                    results: Bundle?
                ) {

                    listeningForWakeWord = false

                    val matches =
                        results?.getStringArrayList(
                            SpeechRecognizer
                                .RESULTS_RECOGNITION
                        )

                    val spokenText =
                        matches
                            ?.firstOrNull()
                            ?.trim()
                            ?: ""

                    Log.d(
                        WAKE_TAG,
                        "Recognizer heard: $spokenText"
                    )

                    handleWakeResult(
                        spokenText
                    )
                }

                override fun onPartialResults(
                    partialResults: Bundle?
                ) {

                    val matches =
                        partialResults
                            ?.getStringArrayList(
                                SpeechRecognizer
                                    .RESULTS_RECOGNITION
                            )

                    val text =
                        matches
                            ?.firstOrNull()
                            ?.trim()
                            ?: ""

                    if (text.isNotBlank()) {

                        Log.d(
                            WAKE_TAG,
                            "Partial: $text"
                        )

                        if (
                            containsWakePhrase(text, fromPartial = true)
                        ) {

                            Log.d(
                                WAKE_TAG,
                                "HEY AIRA DETECTED IN PARTIAL RESULT"
                            )

                            triggerWake(text)
                        }
                    }
                }

                override fun onEvent(
                    eventType: Int,
                    params: Bundle?
                ) {
                }
            }
        )
    }

    // =========================================================
    // HANDLE WAKE RESULT
    // =========================================================

    private fun handleWakeResult(
        spokenText: String
    ) {

        if (commandProcessing) {
            return
        }

        if (
            containsWakePhrase(
                spokenText
            )
        ) {

            triggerWake(
                spokenText
            )

        } else {

            Log.d(
                WAKE_TAG,
                "No Hey AIRA phrase detected"
            )

            scheduleWakeRestart()
        }
    }

    // =========================================================
    // CHECK WAKE PHRASE
    // =========================================================

    private fun containsWakePhrase(
        text: String,
        fromPartial: Boolean = false
    ): Boolean {

        val normalized = normalizeSpeechText(text)
        if (normalized.isBlank()) return false

        val words = normalized.split(" ")
        // Avoid activating from a single ambiguous partial word such as "hair".
        if (fromPartial && words.size < 2) return false

        val wakeAliases = setOf(
            "aira",
            "ayra",
            "ira",
            "hair",
            "hera"
        )

        // Accept the intended phrase and common speech-to-text variations,
        // but only when the wake phrase appears at the beginning.
        if (words.size >= 2 && words[0] == "hey" && words[1] in wakeAliases) {
            return true
        }

        return words[0] in wakeAliases
    }

    private fun normalizeSpeechText(text: String): String {
        return text
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    // =========================================================
    // TRIGGER WAKE
    // =========================================================

    private fun triggerWake(
        spokenText: String
    ) {

        if (commandProcessing) {
            return
        }

        commandProcessing = true

        listeningForWakeWord = false

        Log.d(
            WAKE_TAG,
            "========================================"
        )

        Log.d(
            WAKE_TAG,
            "HEY AIRA DETECTED!"
        )

        Log.d(
            WAKE_TAG,
            "Heard: $spokenText"
        )

        Log.d(
            WAKE_TAG,
            "========================================"
        )

        destroySpeechRecognizer()

        val command =
            extractCommandAfterWakePhrase(
                spokenText
            )

        if (command.isNotBlank()) {

            Log.d(
                SPEECH_TAG,
                "Command included with wake phrase:"
            )

            Log.d(
                SPEECH_TAG,
                command
            )

            processCommandWithAgent(
                command
            )

        } else {

            speakReadyForCommand()
        }
    }

    // =========================================================
    // EXTRACT COMMAND
    // =========================================================

    private fun extractCommandAfterWakePhrase(
        text: String
    ): String {

        val normalized = normalizeSpeechText(text)
        if (normalized.isBlank()) return ""

        val words = normalized.split(" ")
        val wakeAliases = setOf(
            "aira",
            "ayra",
            "ira",
            "hair",
            "hera"
        )

        var wordsToSkip = 0

        if (
            words.size >= 2 &&
            words[0] == "hey" &&
            words[1] in wakeAliases
        ) {
            wordsToSkip = 2
        } else if (words[0] in wakeAliases) {
            wordsToSkip = 1
        }

        if (wordsToSkip == 0 || words.size <= wordsToSkip) {
            return ""
        }

        // Return normalized command text so recognition variations like
        // "hair go to home" still deliver "go to home" to the agent.
        return words.drop(wordsToSkip).joinToString(" ").trim()
    }

    // =========================================================
    // ASK FOR COMMAND
    // =========================================================

    private fun speakReadyForCommand() {

        Log.d(
            SPEECH_TAG,
            "Waiting for user command"
        )

        speak(
            "Yes, how can I help?"
        ) {

            startCommandListening()
        }
    }

    // =========================================================
    // COMMAND LISTENER
    // =========================================================

    private fun startCommandListening() {

        if (!serviceRunning) {
            return
        }

        listeningForCommand = true

        destroySpeechRecognizer()

        speechRecognizer =
            createRecognizer()

        if (speechRecognizer == null) {

            listeningForCommand = false

            speakAndResume(
                "I couldn't start voice recognition."
            )

            return
        }

        speechRecognizer?.setRecognitionListener(

            object : RecognitionListener {

                override fun onReadyForSpeech(
                    params: Bundle?
                ) {

                    Log.d(
                        SPEECH_TAG,
                        "COMMAND LISTENER READY"
                    )
                }

                override fun onBeginningOfSpeech() {

                    Log.d(
                        SPEECH_TAG,
                        "User started command"
                    )
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

                    Log.d(
                        SPEECH_TAG,
                        "User finished command"
                    )
                }

                override fun onError(
                    error: Int
                ) {

                    listeningForCommand = false

                    Log.e(
                        SPEECH_TAG,
                        "Command recognition error: $error"
                    )

                    speakAndResume(
                        commandErrorMessage(
                            error
                        )
                    )
                }

                override fun onResults(
                    results: Bundle?
                ) {

                    listeningForCommand = false

                    val matches =
                        results?.getStringArrayList(
                            SpeechRecognizer
                                .RESULTS_RECOGNITION
                        )

                    val request =
                        matches
                            ?.firstOrNull()
                            ?.trim()
                            ?: ""

                    Log.d(
                        SPEECH_TAG,
                        "========================================"
                    )

                    Log.d(
                        SPEECH_TAG,
                        "COMMAND RECEIVED"
                    )

                    Log.d(
                        SPEECH_TAG,
                        request
                    )

                    Log.d(
                        SPEECH_TAG,
                        "========================================"
                    )

                    if (request.isBlank()) {

                        speakAndResume(
                            "I didn't hear your command."
                        )

                        return
                    }

                    processCommandWithAgent(
                        request
                    )
                }

                override fun onPartialResults(
                    partialResults: Bundle?
                ) {
                }

                override fun onEvent(
                    eventType: Int,
                    params: Bundle?
                ) {
                }
            }
        )

        val intent =
            createRecognitionIntent()

        try {

            speechRecognizer?.startListening(
                intent
            )

            Log.d(
                SPEECH_TAG,
                "Command recognition started"
            )

        } catch (e: Exception) {

            Log.e(
                SPEECH_TAG,
                "Could not start command recognition",
                e
            )

            listeningForCommand = false

            speakAndResume(
                "I couldn't start voice recognition."
            )
        }
    }

    // =========================================================
    // RECOGNITION INTENT
    // =========================================================

    private fun createRecognitionIntent(): Intent {

        return Intent(
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
    }

    // =========================================================
    // AIRA AGENT
    // =========================================================

    private fun processCommandWithAgent(
        request: String
    ) {

        Log.d(
            AGENT_TAG,
            "========================================"
        )

        Log.d(
            AGENT_TAG,
            "AIRA AGENT REQUEST"
        )

        Log.d(
            AGENT_TAG,
            request
        )

        Log.d(
            AGENT_TAG,
            "========================================"
        )

        val accessibilityService =
            AiraAccessibilityService
                .getInstance()

        if (
            accessibilityService == null
        ) {

            Log.e(
                AGENT_TAG,
                "Accessibility Service is not connected"
            )

            speakAndResume(
                "AIRA accessibility control is not connected."
            )

            return
        }

        serviceScope.launch(
            Dispatchers.IO
        ) {

            try {

                val result =
                    accessibilityService
                        .processAiraRequest(
                            request
                        )

                Log.d(
                    AGENT_TAG,
                    "Agent result: $result"
                )

                mainHandler.post {

                    speakAndResume(
                        result
                    )
                }

            } catch (e: Exception) {

                Log.e(
                    AGENT_TAG,
                    "Agent processing failed",
                    e
                )

                mainHandler.post {

                    speakAndResume(
                        "I couldn't complete that request."
                    )
                }
            }
        }
    }

    // =========================================================
    // TEXT TO SPEECH
    // =========================================================

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

                    ttsReady = true

                    Log.d(
                        TAG,
                        "TTS initialized"
                    )

                } else {

                    ttsReady = false

                    Log.e(
                        TAG,
                        "TTS initialization failed"
                    )
                }
            }
    }

    // =========================================================
    // SPEAK
    // =========================================================

    private fun speak(
        message: String,
        onFinished: (() -> Unit)? = null
    ) {

        Log.d(
            TAG,
            "AIRA says: $message"
        )

        if (!ttsReady) {

            onFinished?.invoke()

            return
        }

        val utteranceId =
            "AIRA_${System.currentTimeMillis()}"

        textToSpeech?.setOnUtteranceProgressListener(

            object :
                UtteranceProgressListener() {

                override fun onStart(
                    utteranceId: String?
                ) {
                }

                override fun onDone(
                    utteranceId: String?
                ) {

                    mainHandler.post {

                        onFinished?.invoke()
                    }
                }

                override fun onError(
                    utteranceId: String?
                ) {

                    mainHandler.post {

                        onFinished?.invoke()
                    }
                }
            }
        )

        textToSpeech?.speak(
            message,
            TextToSpeech.QUEUE_FLUSH,
            null,
            utteranceId
        )
    }

    // =========================================================
    // SPEAK AND RESUME
    // =========================================================

    private fun speakAndResume(
        message: String
    ) {

        speak(
            message
        ) {

            mainHandler.postDelayed(
                {

                    commandProcessing = false

                    listeningForCommand = false

                    if (serviceRunning) {

                        startWakeWordListening()
                    }

                },
                500L
            )
        }
    }

    // =========================================================
    // RESTART WAKE LISTENER
    // =========================================================

    private fun scheduleWakeRestart() {

        if (!serviceRunning) {
            return
        }

        if (commandProcessing) {
            return
        }

        if (restartingRecognizer) {
            return
        }

        restartingRecognizer = true

        mainHandler.postDelayed(
            {

                restartingRecognizer = false

                if (
                    serviceRunning &&
                    !commandProcessing &&
                    !listeningForWakeWord &&
                    !listeningForCommand
                ) {

                    Log.d(
                        WAKE_TAG,
                        "Restarting wake listener"
                    )

                    startWakeWordListening()
                }

            },
            700L
        )
    }

    // =========================================================
    // DESTROY RECOGNIZER
    // =========================================================

    private fun destroySpeechRecognizer() {

        try {

            speechRecognizer?.cancel()

        } catch (_: Exception) {
        }

        try {

            speechRecognizer?.destroy()

        } catch (_: Exception) {
        }

        speechRecognizer = null
    }

    // =========================================================
    // ERROR MESSAGE
    // =========================================================

    private fun commandErrorMessage(
        error: Int
    ): String {

        return when (error) {

            SpeechRecognizer.ERROR_AUDIO ->
                "There was an audio problem."

            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                "Microphone permission is not available."

            SpeechRecognizer.ERROR_NETWORK ->
                "The speech recognition network is unavailable."

            SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                "Speech recognition timed out."

            SpeechRecognizer.ERROR_NO_MATCH ->
                "I didn't understand you."

            SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                "Voice recognition is busy."

            SpeechRecognizer.ERROR_SERVER ->
                "The speech recognition service failed."

            SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                "I didn't hear anything."

            else ->
                "I couldn't understand your command."
        }
    }

    // =========================================================
    // STOP SERVICE
    // =========================================================

    private fun stopVoiceService() {

        Log.d(
            TAG,
            "Stopping AIRA voice service"
        )

        serviceRunning = false

        listeningForWakeWord = false

        listeningForCommand = false

        commandProcessing = false

        restartingRecognizer = false

        destroySpeechRecognizer()

        textToSpeech?.stop()

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.N
        ) {

            stopForeground(
                STOP_FOREGROUND_REMOVE
            )

        } else {

            @Suppress("DEPRECATION")
            stopForeground(true)
        }

        stopSelf()
    }

    // =========================================================
    // NOTIFICATION
    // =========================================================

    private fun createNotification(): Notification {

        return NotificationCompat.Builder(
            this,
            CHANNEL_ID
        )
            .setSmallIcon(
                android.R.drawable.ic_btn_speak_now
            )
            .setContentTitle(
                "AIRA Voice Assistant"
            )
            .setContentText(
                "Say Hey AIRA to activate AIRA"
            )
            .setOngoing(true)
            .setCategory(
                NotificationCompat.CATEGORY_SERVICE
            )
            .setPriority(
                NotificationCompat.PRIORITY_LOW
            )
            .build()
    }

    // =========================================================
    // NOTIFICATION CHANNEL
    // =========================================================

    private fun createNotificationChannel() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "AIRA Voice Assistant",
                    NotificationManager.IMPORTANCE_LOW
                )

            channel.description =
                "AIRA background voice assistant"

            val manager =
                getSystemService(
                    NotificationManager::class.java
                )

            manager.createNotificationChannel(
                channel
            )
        }
    }

    // =========================================================
    // BIND
    // =========================================================

    override fun onBind(
        intent: Intent?
    ): IBinder? {

        return null
    }

    // =========================================================
    // DESTROY
    // =========================================================

    override fun onDestroy() {

        Log.d(
            TAG,
            "AIRA voice service destroyed"
        )

        serviceRunning = false

        listeningForWakeWord = false

        listeningForCommand = false

        commandProcessing = false

        destroySpeechRecognizer()

        textToSpeech?.stop()

        textToSpeech?.shutdown()

        textToSpeech = null

        serviceScope.cancel()

        super.onDestroy()
    }
}

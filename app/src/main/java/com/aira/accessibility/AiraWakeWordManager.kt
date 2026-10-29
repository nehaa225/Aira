package com.aira.accessibility

import android.content.Context
import android.util.Log
import com.openwakeword.OpenWakeWord

class AiraWakeWordManager(
    private val context: Context,
    private val onWakeWordDetected: () -> Unit
) {

    companion object {
        private const val TAG = "AIRA_WAKE_WORD"
    }

    private var detector: OpenWakeWord? = null
    private var isRunning = false

    fun start() {
        if (isRunning) {
            Log.d(TAG, "Wake-word detector is already running")
            return
        }

        try {
            Log.d(TAG, "Starting AIRA wake-word detector...")

            detector = OpenWakeWord.Builder(context)
                .setModel(OpenWakeWord.BuiltInModel.HEY_JARVIS)
                .setThreshold(0.5f)
                .setDebounceMs(2500L)
                .build()

            detector?.start { score ->

                Log.d(
                    TAG,
                    "Wake word detected! score=$score"
                )

                onWakeWordDetected()
            }

            isRunning = true

            Log.d(
                TAG,
                "Wake-word detector is listening for: Hey Jarvis"
            )

        } catch (e: Exception) {
            Log.e(
                TAG,
                "Failed to start wake-word detector",
                e
            )
        }
    }

    fun stop() {
        try {
            detector?.stop()
            isRunning = false

            Log.d(
                TAG,
                "Wake-word detector stopped"
            )

        } catch (e: Exception) {
            Log.e(
                TAG,
                "Error stopping wake-word detector",
                e
            )
        }
    }

    fun release() {
        try {
            detector?.stop()
            detector?.release()
            detector = null
            isRunning = false

            Log.d(
                TAG,
                "Wake-word detector released"
            )

        } catch (e: Exception) {
            Log.e(
                TAG,
                "Error releasing wake-word detector",
                e
            )
        }
    }

    fun isRunning(): Boolean {
        return isRunning
    }
}
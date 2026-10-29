package com.aira.accessibility

import android.content.Context
import android.util.Log
import com.aira.accessibility.vision.VisionManager

class AiraCore(
    private val context: Context
) {

    companion object {
        private const val TAG = "AIRA_CORE"
    }

    private lateinit var visionManager: VisionManager

    fun initialize() {

        visionManager = VisionManager(context)

        Log.d(
            TAG,
            "AIRA Core initialized"
        )

        Log.d(
            TAG,
            "Vision module initialized"
        )
    }

    fun getStatus(): String {

        return "AIRA Core is running"
    }

    fun hasCameraPermission(): Boolean {

        return visionManager.hasCameraPermission()
    }
}
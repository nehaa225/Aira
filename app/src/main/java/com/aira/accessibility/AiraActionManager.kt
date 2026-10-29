package com.aira.accessibility

import android.util.Log

class AiraActionManager(
    private val accessibilityService:
    AiraAccessibilityService
) {

    companion object {
        private const val TAG = "AIRA_ACTION"
    }

    fun goBack(): Boolean {
        Log.d(TAG, "GO_BACK")
        return accessibilityService.goBack()
    }

    fun goHome(): Boolean {
        Log.d(TAG, "GO_HOME")
        return accessibilityService.goHome()
    }

    fun notifications(): Boolean {
        return accessibilityService.openNotifications()
    }

    fun quickSettings(): Boolean {
        return accessibilityService.openQuickSettings()
    }

    fun recents(): Boolean {
        return accessibilityService.openRecents()
    }

    fun findElement(
        description: String
    ): Boolean {
        return accessibilityService
            .findElement(description) != null
    }

    fun clickElement(
        description: String
    ): Boolean {
        return accessibilityService
            .clickElement(description)
    }

    fun typeText(
        text: String
    ): Boolean {
        return accessibilityService
            .typeText(text)
    }

    fun scroll(
        direction: String
    ): Boolean {
        return accessibilityService
            .scroll(direction)
    }
}
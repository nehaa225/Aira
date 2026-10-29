package com.aira.accessibility.agent

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

class OpenAppTool(
    private val context: Context
) : AiraTool {

    override val name: String =
        "open_app"

    override val description: String =
        "Open an installed Android application by its name."

    override suspend fun execute(
        parameters: Map<String, String>
    ): AiraToolResult {

        val requestedName =
            parameters["app"]
                ?.trim()

        if (requestedName.isNullOrEmpty()) {

            return AiraToolResult(
                success = false,
                message =
                    "Please specify which app to open."
            )
        }

        val packageManager =
            context.packageManager

        /*
         * Find all activities that appear
         * in the Android launcher.
         */
        val launchIntent =
            Intent(
                Intent.ACTION_MAIN
            ).apply {

                addCategory(
                    Intent.CATEGORY_LAUNCHER
                )
            }

        val activities =
            packageManager.queryIntentActivities(
                launchIntent,
                PackageManager.MATCH_ALL
            )

        /*
         * Find an app whose visible launcher
         * name matches the requested name.
         */
        val matchingActivity =
            activities.firstOrNull { resolveInfo ->

                val label =
                    resolveInfo
                        .loadLabel(
                            packageManager
                        )
                        .toString()
                        .trim()

                label.equals(
                    requestedName,
                    ignoreCase = true
                )
            }

        if (matchingActivity == null) {

            return AiraToolResult(
                success = false,
                message =
                    "I couldn't find " +
                            requestedName +
                            " on this phone."
            )
        }

        return try {

            val packageName =
                matchingActivity
                    .activityInfo
                    .packageName

            val appLaunchIntent =
                packageManager
                    .getLaunchIntentForPackage(
                        packageName
                    )

            if (appLaunchIntent == null) {

                return AiraToolResult(
                    success = false,
                    message =
                        "$requestedName cannot be launched."
                )
            }

            appLaunchIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
            )

            context.startActivity(
                appLaunchIntent
            )

            AiraToolResult(
                success = true,
                message =
                    "Opening $requestedName."
            )

        } catch (e: Exception) {

            AiraToolResult(
                success = false,
                message =
                    "I couldn't open " +
                            requestedName +
                            "."
            )
        }
    }
}
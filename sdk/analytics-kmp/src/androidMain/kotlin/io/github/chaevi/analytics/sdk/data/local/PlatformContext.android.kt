package io.github.chaevi.analytics.sdk.data.local

import android.os.Build
import io.github.chaevi.analytics.sdk.domain.model.EventContext
import java.util.Locale
import java.util.TimeZone

actual fun collectPlatformContext(): EventContext =
    EventContext(
        osName = "Android",
        osVersion = Build.VERSION.RELEASE,
        deviceModel = Build.MODEL,
        deviceManufacturer = Build.MANUFACTURER,
        locale = Locale.getDefault().toLanguageTag(),
        timezone = TimeZone.getDefault().id,
        screenWidth = null,
        screenHeight = null,
        networkType = null,
    )

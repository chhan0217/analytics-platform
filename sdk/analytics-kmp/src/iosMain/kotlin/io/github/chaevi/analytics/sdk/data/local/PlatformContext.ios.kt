package io.github.chaevi.analytics.sdk.data.local

import io.github.chaevi.analytics.sdk.domain.model.EventContext
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.Foundation.localTimeZone
import platform.Foundation.regionCode
import platform.UIKit.UIDevice
import platform.UIKit.UIScreen

@OptIn(ExperimentalForeignApi::class)
actual fun collectPlatformContext(): EventContext {
    val device = UIDevice.currentDevice
    val screen = UIScreen.mainScreen
    val locale = NSLocale.currentLocale
    val langTag =
        buildString {
            append(locale.languageCode)
            locale.regionCode?.let { append("_$it") }
        }

    val (screenW, screenH) =
        screen.bounds.useContents {
            val scale = screen.scale.toInt()
            Pair(size.width.toInt() * scale, size.height.toInt() * scale)
        }

    return EventContext(
        osName = "iOS",
        osVersion = device.systemVersion,
        deviceModel = device.model,
        deviceManufacturer = "Apple",
        locale = langTag,
        timezone = NSTimeZone.localTimeZone.name,
        screenWidth = screenW,
        screenHeight = screenH,
        networkType = null,
    )
}

package io.github.chaevi.analytics.sdk.data.local

import io.github.chaevi.analytics.sdk.domain.model.EventContext
import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.Foundation.localTimeZone
import platform.Foundation.name
import platform.Foundation.regionCode
import platform.UIKit.UIDevice
import platform.UIKit.UIScreen

actual fun collectPlatformContext(): EventContext {
    val device = UIDevice.currentDevice
    val screen = UIScreen.mainScreen
    val locale = NSLocale.currentLocale
    val langTag =
        buildString {
            append(locale.languageCode)
            locale.regionCode?.let { append("_$it") }
        }

    return EventContext(
        osName = "iOS",
        osVersion = device.systemVersion,
        deviceModel = device.model,
        deviceManufacturer = "Apple",
        locale = langTag,
        timezone = NSTimeZone.localTimeZone.name,
        screenWidth = (screen.bounds.size.width * screen.scale).toInt(),
        screenHeight = (screen.bounds.size.height * screen.scale).toInt(),
        networkType = null,
    )
}

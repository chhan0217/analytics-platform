package io.github.chaevi.analytics.sdk.data.local

import io.github.chaevi.analytics.sdk.domain.model.EventContext

expect fun collectPlatformContext(): EventContext

package io.github.chaevi.analytics.compose

import kotlin.test.Test
import kotlin.test.assertEquals

class StripRouteParametersTest {
    @Test
    fun `파라미터 없는 route는 그대로 반환`() {
        assertEquals("onboard", stripRouteParameters("onboard"))
        assertEquals("main/home", stripRouteParameters("main/home"))
        assertEquals("main/my-page/setting", stripRouteParameters("main/my-page/setting"))
    }

    @Test
    fun `쿼리 파라미터를 제거한다`() {
        assertEquals("login", stripRouteParameters("login?email={email}&isForPorsche={isForPorsche}"))
        assertEquals("register", stripRouteParameters("register?socialDataJson={socialDataJson}"))
        assertEquals(
            "main/my-page/event",
            stripRouteParameters("main/my-page/event?eventKey={eventKey}&eventItem={eventItem}"),
        )
    }

    @Test
    fun `경로 파라미터를 제거한다`() {
        assertEquals(
            "main/my-page/announcement/detail",
            stripRouteParameters("main/my-page/announcement/detail/{announcementItem}"),
        )
        assertEquals("term", stripRouteParameters("term/{termsType}"))
        assertEquals(
            "main/home/coupon-notice",
            stripRouteParameters("main/home/coupon-notice/{noticeId}/{type}"),
        )
    }

    @Test
    fun `쿼리 파라미터와 경로 파라미터가 혼합된 경우`() {
        assertEquals(
            "main/membership/auth-user",
            stripRouteParameters(
                "main/membership/auth-user/{cardUid}/{upstream}?credit={credit}&targetCardUid={targetCardUid}",
            ),
        )
        assertEquals(
            "main/charge-station/search",
            stripRouteParameters(
                "main/charge-station/search/{showChaeviNearby}?tabEnabled={tabEnabled}&filters={filters}",
            ),
        )
    }

    @Test
    fun `실제 수집 데이터에서 발견된 패턴`() {
        assertEquals(
            "main/membership/manage-credit/expiring-credit",
            stripRouteParameters("main/membership/manage-credit/expiring-credit?cardUid={cardUid}"),
        )
        assertEquals(
            "main/home/charging-info",
            stripRouteParameters("main/home/charging-info?historyId={historyId}&intervalSec={intervalSec}"),
        )
    }
}

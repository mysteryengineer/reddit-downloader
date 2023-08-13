package io.vinicius.rmd.helper

import com.mixpanel.mixpanelapi.ClientDelivery
import com.mixpanel.mixpanelapi.MessageBuilder
import com.mixpanel.mixpanelapi.MixpanelAPI
import org.json.JSONObject
import java.util.UUID

object Telemetry {
    private val mixpanel = MixpanelAPI()
    private val messageBuilder = MessageBuilder("5be99deb1f8e8bdd9cca5e8e0a1a15a8")
    private val trackingId = UUID.randomUUID().toString()

    fun trackDownloadStart(
        user: String,
        limit: Int,
        parallel: Int,
        convertImages: Boolean,
        convertVideos: Boolean
    ) {
        val props = JSONObject(mapOf(
            "user" to user,
            "limit" to limit,
            "parallel" to parallel,
            "convertImages" to convertImages,
            "convertVideos" to convertVideos
        ))

        val event = messageBuilder.event(trackingId, "Download Start", props)
        val delivery = ClientDelivery().apply { addMessage(event) }
        mixpanel.deliver(delivery)
    }

    fun trackDownloadEnd(
        user: String,
        postsFound: Int,
        failedDownloads: Int,
        duplicated: Int
    ) {
        val props = JSONObject(mapOf(
            "user" to user,
            "postsFound" to postsFound,
            "failedDownloads" to failedDownloads,
            "duplicated" to duplicated
        ))

        val event = messageBuilder.event(trackingId, "Download End", props)
        val delivery = ClientDelivery().apply { addMessage(event) }
        mixpanel.deliver(delivery)
    }
}
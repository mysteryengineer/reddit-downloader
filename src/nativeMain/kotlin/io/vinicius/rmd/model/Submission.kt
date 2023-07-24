package io.vinicius.rmd.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class Submission(
    val data: Data
) {
    @Serializable
    data class Data(
        val domain: String,
        val url: String,

        @SerialName("post_hint")
        val postHint: String? = null,

        @SerialName("created_utc")
        @Serializable(with = DoubleToLongSerializer::class)
        val created: Long
    )

    override fun equals(other: Any?): Boolean = other is Submission && data.url == other.data.url

    override fun hashCode(): Int = data.url.hashCode()

    object DoubleToLongSerializer : KSerializer<Long> {
        override val descriptor: SerialDescriptor = Long.serializer().descriptor

        override fun deserialize(decoder: Decoder): Long {
            return decoder.decodeDouble().toLong()
        }

        override fun serialize(encoder: Encoder, value: Long) {
            encoder.encodeDouble(value.toDouble())
        }
    }
}
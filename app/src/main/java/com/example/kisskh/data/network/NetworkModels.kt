package com.example.kisskh.data.network

import com.google.gson.annotations.SerializedName

// SEARCH
data class SearchResponse(
    @SerializedName("data") val data: List<SearchItem>? = null
)

data class SearchItem(
    @SerializedName("id") val id: Int?,
    @SerializedName("title") val title: String?,
    @SerializedName("thumbnail") val image: String?
)

// DRAMA DETAILS (Episodes)
data class DramaDetailResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("thumbnail") val image: String?,
    @SerializedName("country") val country: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("episodes") val episodes: List<EpisodeItem>?
)

data class EpisodeItem(
    @SerializedName("id") val id: Int,
    @SerializedName("number") val number: Float,
    @SerializedName("sub") val sub: Int? // Subtitle count?
)

// STREAM SOURCE
data class StreamResponse(
    @SerializedName("Video") val videoUrlCap: String?,
    @SerializedName("video") val videoUrl: String?,
    @SerializedName("url") val url_alt: String?, // fallback
    @SerializedName("Type") val type: String?
) {
    val url: String
        get() = videoUrlCap ?: videoUrl ?: url_alt ?: ""
}

package com.example.kisskh.data.network

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface KissKhApi {

    @GET("DramaList/Search")
    suspend fun search(@Query("q") query: String, @Query("type") type: Int = 1): List<SearchItem>
    // Note: The Python code implied it returns a list directly or a wrapped object. 
    // Usually generic APIs return a list for search. I'll assume List<SearchItem> for now based on 'response.json()' parsing.

    @GET("DramaList/Drama/{id}?isq=false")
    suspend fun getDramaDetails(@Path("id") id: String): DramaDetailResponse

    // The png extension is a trick used by the server, but it returns JSON
    @GET("DramaList/Episode/{id}.png?err=false&ts=&time=")
    suspend fun getStreamUrl(@Path("id") id: String): StreamResponse

}

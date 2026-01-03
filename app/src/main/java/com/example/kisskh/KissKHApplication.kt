package com.example.kisskh

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import okhttp3.OkHttpClient

class KissKHApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        com.example.kisskh.data.LocalStorage.init(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient {
                com.example.kisskh.data.network.UnsafeOkHttpClient.getUnsafeOkHttpClient()
                    .addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                            .header("Referer", "https://kisskh.co/")
                            .build()
                        chain.proceed(request)
                    }
                    .build()
            }
            .build()
    }
}

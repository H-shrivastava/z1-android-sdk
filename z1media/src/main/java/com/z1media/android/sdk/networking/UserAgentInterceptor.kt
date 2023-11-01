package com.z1media.android.sdk.networking

import android.content.Context
import com.z1media.android.sdk.utils.Z1KUtils
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

/* This interceptor adds a custom User-Agent. */
class UserAgentInterceptor(private val context: Context) : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Chain): Response {
//        val unsafeHeader: Headers = Headers.Builder()
//            .addUnsafeNonAscii("User-Agent", Z1KUtils.getUserAgent(context))
//            .build()
        val originalRequest: Request = chain.request()
        val requestWithUserAgent = originalRequest.newBuilder()
            .header("User-Agent", Z1KUtils.getUserAgent(context))
            .build()
        return chain.proceed(requestWithUserAgent)
    }

    private fun getSafeValue(value: String, defaultValue: String): String {
        for (i in value.indices) {
            val c = value[i]
            if ((c == '\t' || c in '\u0020'..'\u007e').not()) {
                return defaultValue
            }
        }
        return value
    }

}
package com.z1media.android.sdk.networking

import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitHelper {

    private const val PIXEL_URL = "https://analytics1.vdo.ai/"
    private const val TAG_CONFIG_URL = "https://targetingv1.vdo.ai/"
    private const val ERROR_URL= "https://a.vdo.ai/"

//    lateinit var tagConfigService: RetrofitService
//    private lateinit var logPixelServices: RetrofitService


    private fun getOkHttpClient(context: Context): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(UserAgentInterceptor(context))
            .build()
    }

    @JvmStatic
    fun getTagConfigServices(context: Context) : RetrofitService {
//            if (this::tagConfigService.isInitialized.not()) {

        val retrofit = Retrofit.Builder()
            .client(getOkHttpClient(context))
            .baseUrl(TAG_CONFIG_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
//                tagConfigService = retrofit.create(RetrofitService::class.java)
//            }
        return retrofit.create(RetrofitService::class.java)
    }

    @JvmStatic
    fun getLogPixelServices(context: Context) : RetrofitService {
//            if (this::logPixelServices.isInitialized.not()) {

        val retrofit = Retrofit.Builder()
            .client(getOkHttpClient(context))
            .baseUrl(PIXEL_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
//                logPixelServices = retrofit.create(RetrofitService::class.java)
//            }
//            return logPixelServices
        return retrofit.create(RetrofitService::class.java)
    }

    @JvmStatic
    fun getErrorLogServices(context: Context) : RetrofitService {
//            if (this::logPixelServices.isInitialized.not()) {

        val retrofit = Retrofit.Builder()
            .client(getOkHttpClient(context))
            .baseUrl(ERROR_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
//                logPixelServices = retrofit.create(RetrofitService::class.java)
//            }
//            return logPixelServices
        return retrofit.create(RetrofitService::class.java)
    }

}
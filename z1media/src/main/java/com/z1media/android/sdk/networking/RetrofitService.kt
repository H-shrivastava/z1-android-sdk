package com.z1media.android.sdk.networking

import android.content.Context
import com.z1media.android.sdk.models.GetTagConfigDto
import com.z1media.android.sdk.models.PixelDto
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface RetrofitService {

//    https://a.vdo.ai/core/in-app-rto-vehicle-n-Z1/adframe.json
//    @GET("core/{tag}/adframe.json")
//    @GET("allowed_url.php?type=json&url={APP_ID}&tag={TAGNAME}&platform={platform}")
//    @Throws(Exception::class)
//    suspend fun configApi(@Path("APP_ID") APP_ID:String, @Path("TAGNAME") tagname:String,  @Path("platform") platform:String): Response<GetTagConfigDto>

    @GET("allowed_url.php?type=json")
    @Throws(Exception::class)
    suspend fun   configApi(@Query("url") appId:String, @Query("tag") tadName:String, @Query("platform") platform:String): Response<GetTagConfigDto>

    @POST("logger")
    @Throws(Exception::class)
    suspend fun logPixel(@Body pixelDto: PixelDto): Response<Unit>

    @GET("core/logger.php")
    @Throws(Exception::class)
    suspend fun errorLog(@Query("msg") msg: String, @Query("tag") tag: String/* @Query("code") code: Int?,
                       @Query("url") url: String?,  @Query("func") func: String?*/): Response<Unit>


}
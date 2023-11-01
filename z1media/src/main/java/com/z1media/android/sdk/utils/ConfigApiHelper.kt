package com.z1media.android.sdk.utils

import android.util.Log
import com.google.android.gms.common.internal.ApiExceptionUtil
import com.z1media.android.sdk.models.GetTagConfigDto
import com.z1media.android.sdk.networking.NetworkState
import com.z1media.android.sdk.networking.RetrofitService
import com.z1media.android.sdk.networking.parseResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal object ConfigApiHelper {

    fun getConfig( retrofitService: RetrofitService, packageName: String, tagName:String, listener: ConfigApiListener? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = retrofitService.configApi(packageName, tagName, Z1KUtils.PLATFORM)
                withContext(Dispatchers.Main) {
                    when (val result = response.parseResponse()) {
                        is NetworkState.Success -> {
                            listener?.onSuccess(result.data)
                        }
                        is NetworkState.Error -> {

                            Z1KUtils.getMyHandler().post {
                                listener?.onFailure(result.response?.code()?:0,result.response?.message()?:"")
                            }
                        }
                    }
                }
            } catch(e:Exception){
                e.printStackTrace()
                var msg = e.message
                if (msg.isNullOrEmpty()) {
                    msg = Log.getStackTraceString(e)
                }
                Z1KUtils.getMyHandler().post {
                    listener?.onFailure(0, msg)
                }
            }
        }
    }

    interface ConfigApiListener {
        fun onSuccess(tagConfigDto: GetTagConfigDto)
        fun onFailure(code:Int, errorMessage:String?)
    }
}
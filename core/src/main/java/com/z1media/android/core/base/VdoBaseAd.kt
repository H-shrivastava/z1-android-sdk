package com.z1media.android.core.base

import android.util.Log
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.admanager.AdManagerInterstitialAdLoadCallback
import com.z1media.android.core.models.GetTagConfigDto
import com.z1media.android.core.models.VdoAdError
import com.z1media.android.core.networking.RetrofitService
import com.z1media.android.core.utils.ConfigApiHelper
import com.z1media.android.core.utils.FailureType

abstract class VdoBaseAd : AdListener() {

    protected open val TAG: String get() = VdoBaseAd::class.java.simpleName
    var tagConfigDto: GetTagConfigDto?= null

    abstract fun invokeOnSuccess()

    abstract fun invokeOnFailure(vdoAdError: VdoAdError)

    protected fun getTagConfig(tagConfigService: RetrofitService, mPackageName:String, mTagName:String) {
        ConfigApiHelper.getConfig(tagConfigService, mPackageName,
            mTagName, object : ConfigApiHelper.ConfigApiListener{

                override fun onSuccess(tagConfigDto: GetTagConfigDto) {
                    this@VdoBaseAd.tagConfigDto = tagConfigDto
                    invokeOnSuccess()
                }

                override fun onFailure(code: Int, errorMessage: String?) {
                    Log.e(TAG, "onFailure >>>>> $errorMessage")
                    val adError = VdoAdError(code,"", errorMessage,0, "", FailureType.API)
//                    mListener.onAdFailedToLoad(adError)
                    invokeOnFailure(adError)
                }
            })
    }


}
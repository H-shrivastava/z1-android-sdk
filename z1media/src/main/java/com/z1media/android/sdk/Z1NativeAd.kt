package com.z1media.android.sdk

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import android.util.Log
import androidx.annotation.ColorRes
import androidx.annotation.NonNull
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxError
import com.applovin.mediation.nativeAds.MaxNativeAdListener
import com.applovin.mediation.nativeAds.MaxNativeAdLoader
import com.applovin.mediation.nativeAds.MaxNativeAdView
import com.applovin.sdk.AppLovinSdkConfiguration
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.z1media.android.sdk.listeners.OnApplovinInitializeSdkListener
import com.z1media.android.sdk.listeners.Z1NativeAdsI
import com.z1media.android.sdk.manager.Z1MediaManager
import com.z1media.android.sdk.models.AdUnitsItem
import com.z1media.android.sdk.models.ErrorLogDto
import com.z1media.android.sdk.models.GetTagConfigDto
import com.z1media.android.sdk.models.Z1AdError
import com.z1media.android.sdk.nativeAd.NativeTemplateStyle
import com.z1media.android.sdk.nativeAd.TemplateView
import com.z1media.android.sdk.utils.*
import kotlin.properties.Delegates

/**
 *  created by Ashish Saini at 30th Jan 2023
 *
 **/

class Z1NativeAd(builder: Builder) {

    private val TAG = Z1NativeAd::class.java.simpleName
    private val mActivity : Activity = builder.activity
    private val mPackageName : String = builder.packageName
    private val mEnvironment : String = builder.mEnvironment
    private val mTagName : String = builder.mTagName
    private val mApplovinAdUnitId : String? = builder.mApplovinAdUnitId
    private val mTemplateView : TemplateView? = builder.mTemplateView
    private val mNativeAdOptions :NativeAdOptions = builder.mNativeAdOptions
    private val mListener : Z1NativeAdsI = builder.mListener
    private val errorLogService = builder.errorLogService
    private val logPixelService = builder.logPixelService
    private val tagConfigService = builder.tagConfigService
    private var tagConfigDto: GetTagConfigDto?=null
    private val background :Int = builder.background
    private var adUnitItem: AdUnitsItem?= null
    private var nativeAd: MaxAd? = null
    private var isMediationIsAllowed: Boolean =builder.mIsMediationAllowed
    private var refreshAllowed :Boolean =builder.mRefreshAllowed
    private var mIsPageViewLogged :Boolean = builder.mIsPageViewLogged
    private var mIsPageViewMatchLogged :Boolean = builder.mIsPageViewMatchLogged


    init {
        PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, Z1KUtils.getPixelDto(packageName = mPackageName, pageUrl = "", tagName = mTagName, event = Z1EventNames.LOADED))
        Z1MediaManager.initializeAdsSdk(mActivity)
        getTagConfig()
    }

    class Builder(context: Activity) : Z1BaseBuilder(context){
        var mTemplateView: TemplateView?= null
        lateinit var mNativeAdOptions: NativeAdOptions
        lateinit var mListener: Z1NativeAdsI
        var background by Delegates.notNull<Int>()

        fun setTemplateView(templateView: TemplateView?): Builder {
            this.mTemplateView = templateView
            return this
        }
        fun setMediation(mediationFlag:Boolean): Builder {
            this.mIsMediationAllowed=mediationFlag
            return this
        }
        fun setAllowRefresh(refresh:Boolean) : Builder {
            this.mRefreshAllowed = refresh
            return this
        }
        fun setEnvironment(environment: String): Builder {
            this.mEnvironment = environment
            return this
        }

        fun setTagName(tagName: String): Builder {
            this.mTagName = tagName
            return this
        }

        fun setApplovinAdUnitId(applovinAdUnitId: String?): Builder {
            this.mApplovinAdUnitId = applovinAdUnitId
            return this
        }

        fun setMediaAspectRatio( aspectRatio: Z1MediaAspectRatio): Builder {
            mNativeAdOptions = NativeAdOptions.Builder()
                .setVideoOptions(VideoOptions.Builder().setStartMuted(true).build())
                .setMediaAspectRatio(aspectRatio.ratio).build()
            return this
        }

        fun setBackgroundColor(@ColorRes color: Int): Builder{
            this.background = color
            return this
        }

        fun setListener(@NonNull listener: Z1NativeAdsI): Builder {
            this.mListener = listener
            return this
        }

        fun build(): Z1NativeAd {
            return Z1NativeAd(this)
        }
    }

    private fun getTagConfig() {
        ConfigApiHelper.getConfig(tagConfigService, mPackageName,
            mTagName, object : ConfigApiHelper.ConfigApiListener{

                override fun onSuccess(tagConfigDto: GetTagConfigDto) {
                   this@Z1NativeAd.tagConfigDto = tagConfigDto
                    loadNativeAd()
                }
                override fun onFailure(code: Int, errorMessage: String?) {
                    Log.e(TAG, "onFailure >>>>> $errorMessage")
                    setErrorLog(null, ErrorFilterType.API_FAILURE, errorMessage)
                    val adError = Z1AdError(code,"", errorMessage,0, "", FailureType.API)
                    mListener.onAdFailedToLoad(adError)
                }
        })
    }

    fun loadNativeAd() {
        try{
            if (tagConfigDto == null)
                return

            if (Z1KUtils.isAppInForegrounded()){
                if (!Z1KUtils.isConfigAllowed(tagConfigDto)){

                    adUnitItem = tagConfigDto?.adunits?.get(0)
                    adUnitItem?.adUrl?.let {
                        if (!mIsPageViewLogged){
                            mIsPageViewLogged = true
                            PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, Z1KUtils.getPixelDto(packageName = mPackageName, pageUrl = "", tagName = mTagName, event = Z1EventNames.PAGE_VIEW))
                        }
                        val adLoader: AdLoader = AdLoader.Builder(mActivity, it)
                            .forNativeAd { nativeAd ->
                                Log.e(TAG, "GAM forNativeAd >>>>>>>>>>>")

                                if (isMediationIsAllowed){
                                    mListener.forNativeAd(nativeAd)
                                }else{
                                    val background = ColorDrawable(mActivity.resources.getColor(background))
                                    val styles: NativeTemplateStyle = NativeTemplateStyle.Builder()
                                        .withMainBackgroundColor(background).build()

                                    mTemplateView?.setStyles(styles)
                                    mTemplateView?.setNativeAd(nativeAd)
                                }
                            }
                            .withAdListener(object : AdListener() {

                                override fun onAdFailedToLoad(adError: LoadAdError) {
                                    Log.e(TAG, "GAM Ad failed to load $adError")

                                    if(mApplovinAdUnitId.isNullOrEmpty().not()){
                                        Z1MediaManager.initializeApplovinSdk(mActivity, object : OnApplovinInitializeSdkListener {
                                            override fun onSdkInitialized(configuration: AppLovinSdkConfiguration) {
                                                loadApplovinAd()
                                            }
                                        })
                                    }else{
                                        reloadNativeAd(Z1KUtils.Ad_FAILED_REFRESH_TIME)
                                        mListener.onAdFailedToLoad(Z1KUtils.getAdError(adError))
                                    }
                                }

                                override fun onAdLoaded() {
                                    super.onAdLoaded()
                                    Log.d(TAG, "GAM Ad was loaded")
                                    mListener.onAdLoaded()
                                }

                                override fun onAdImpression() {
                                    super.onAdImpression()
                                    Log.d(TAG, "GAM Ad was impression")
                                    eventAdLoaded()
                                    eventAdImpression()
                                }

                                override fun onAdClosed() {
                                    super.onAdClosed()
                                    Log.d(TAG, "GAM Ad was clicked.")
                                    mListener.onAdClicked()
                                }
                            }).withNativeAdOptions(mNativeAdOptions).build()

                        adLoader.loadAd(Z1MediaManager.getAdManagerAdRequest())
                    }
                }else{
                    PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, Z1KUtils.getPixelDto(packageName = mPackageName, pageUrl = "", tagName = mTagName , event = Z1EventNames.BLOCK_APP, reason = tagConfigDto?.reason))
                }
            }else{
                Log.d(TAG, "Ads is not showing due to app is in background ")
                reloadNativeAd(Z1KUtils.Ad_FAILED_REFRESH_TIME)
            }
        }catch (e:Exception){
            setErrorLog(e, ErrorFilterType.LOAD)
        }
    }

    private fun reloadNativeAd(seconds:Long){
        if(refreshAllowed){ Z1KUtils.getMyHandler().postDelayed(runnable, seconds * 1000) }
    }

    private val runnable:Runnable = Runnable {
        loadNativeAd()
    }


    private fun loadApplovinAd(){

        val nativeAdLoader = MaxNativeAdLoader( mApplovinAdUnitId, mActivity )
        nativeAdLoader.setNativeAdListener(object : MaxNativeAdListener() {

            override fun onNativeAdLoaded(nativeAdView: MaxNativeAdView?, ad: MaxAd) {
                Log.d(TAG, "Applovin onNativeAdLoaded >>>>>>>>> ")
                if(nativeAd != null) {
                    nativeAdLoader.destroy( nativeAd )
                }
                nativeAd = ad
//                val nativeAd = ad.nativeAd
//                if (nativeAd != null) {
//                    val aspectRatio = nativeAd.mediaContentAspectRatio
//                }
                // Add ad view to view.
                mTemplateView?.removeAllViews()
                mTemplateView?.addView(nativeAdView)
                eventAdLoaded()
            }

            override fun onNativeAdLoadFailed(adUnitId: String, error: MaxError) {
                Log.e(TAG, "Applovin onNativeAdLoadFailed >>>>>>>>> $error")
                reloadNativeAd(Z1KUtils.Ad_FAILED_REFRESH_TIME)
                mListener.onAdFailedToLoad(Z1KUtils.getAdError(error))
            }

            override fun onNativeAdClicked(ad: MaxAd) {
                // Optional click callback
                Log.d(TAG, "Applovin onNativeAdClicked >>>>>>>>> ")
                mListener.onAdClicked()
            }
        })
        nativeAdLoader.loadAd()
    }

    private fun eventAdLoaded(){
        val eventDataDto = Z1KUtils.getEventData(0, adUnitItem?.partner, 0.5, Z1KUtils.TYPE_VIDEO)
        if (!mIsPageViewMatchLogged){
            mIsPageViewMatchLogged = true
            PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, Z1KUtils.getPixelDto(packageName = mPackageName, pageUrl = "",
            tagName = mTagName, event = Z1EventNames.PAGE_VIEW_MATCH, eventDataDto= eventDataDto))
        }
        PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, Z1KUtils.getPixelDto(packageName = mPackageName, pageUrl = "",
            tagName = mTagName, event = Z1EventNames.AD_MATCH, eventDataDto= eventDataDto))
    }

    private fun eventAdImpression(){

        val eventDataDto = Z1KUtils.getEventData(0, adUnitItem?.partner, 0.5)
        PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, Z1KUtils.getPixelDto(packageName = mPackageName, pageUrl = "",
            tagName = mTagName, event = Z1EventNames.VIEWABLE_IMPRESSION, eventDataDto= eventDataDto))

        mListener.onAdImpression()
    }

    private fun setErrorLog(e:Exception?, errorType:ErrorFilterType, errorMessage:String?=null){
        var message:String = if (e != null) {
            Log.getStackTraceString(e)
        }else{
            errorMessage?:""
        }
//        message = "filterType.code + message"
        PixelApiHelper.logError(mEnvironment, errorLogService, ErrorLogDto(message, mTagName))
        PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, Z1KUtils.getPixelDto(packageName = mPackageName, pageUrl = "",
            tagName = mTagName , event = Z1EventNames.ERROR, errorCode = errorType.code))
    }

    fun destroy(){
        Z1KUtils.getMyHandler().removeCallbacks(runnable)
    }
}
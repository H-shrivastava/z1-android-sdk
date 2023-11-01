package com.z1media.android.sdk.manager

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.ads.MaxAppOpenAd
import com.applovin.sdk.AppLovinSdk
import com.applovin.sdk.AppLovinSdkConfiguration
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.OfferwallListener
import com.z1media.android.sdk.Z1AppOpenAd
import com.z1media.android.sdk.listeners.OnApplovinInitializeSdkListener
import com.z1media.android.sdk.listeners.OnShowAdCompleteListener
import com.z1media.android.sdk.models.AdUnitsItem
import com.z1media.android.sdk.models.ErrorLogDto
import com.z1media.android.sdk.models.GetTagConfigDto
import com.z1media.android.sdk.networking.RetrofitService
import com.z1media.android.sdk.utils.*
import com.z1media.android.sdk.utils.ConfigApiHelper
import com.z1media.android.sdk.utils.PixelApiHelper
import com.z1media.android.sdk.utils.Z1EventNames
import java.util.*

internal class Z1AppOpenAdManager(val mContext:Context, private val z1AppOpenAd:Z1AppOpenAd,
                                  private val tagConfigService : RetrofitService,
                                  private val logPixelService :RetrofitService,
                                  private val errorLogService:RetrofitService) {

    private val TAG  = Z1AppOpenAdManager::class.java.simpleName
    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    var isShowingAd = false

    private var loadTime: Long = 0

    private var tagConfigDto: GetTagConfigDto?=null
    private var adUnitItem: AdUnitsItem?= null
    lateinit var activity: Activity
    private var mOnShowAdCompleteListener: OnShowAdCompleteListener?= null
    private lateinit var appOpenManager: ApplovinAppOpenManager

    fun showAdIfAvailable(activity: Activity, onShowAdCompleteListener: OnShowAdCompleteListener) {
        this.mOnShowAdCompleteListener = onShowAdCompleteListener
        this.activity = activity
        getTagConfig()
    }

    private fun getTagConfig() {
        ConfigApiHelper.getConfig(tagConfigService, z1AppOpenAd.mPackageName,
            z1AppOpenAd.mTagName, object : ConfigApiHelper.ConfigApiListener{

                override fun onSuccess(tagConfigDto: GetTagConfigDto) {
                    this@Z1AppOpenAdManager.tagConfigDto = tagConfigDto
                    loaAppOpenAd(activity)
                }

                override fun onFailure(code: Int, errorMessage: String?) {
                    Log.e(TAG, "onFailure >>>>> $errorMessage")
                    setErrorLog(null, ErrorFilterType.API_FAILURE, errorMessage)
                    mOnShowAdCompleteListener?.onShowAdComplete()
                }
            })
    }

    private fun loaAppOpenAd(activity: Activity) {
        try{
            if (tagConfigDto == null){
                return
            }

            if (!Z1KUtils.isConfigAllowed(tagConfigDto)) {

                adUnitItem = tagConfigDto?.adunits?.get(0)
                if (adUnitItem?.adUrl.isNullOrEmpty().not()){

                    PixelApiHelper.logPixel(mContext, z1AppOpenAd.mEnvironment, logPixelService, Z1KUtils.getPixelDto(packageName = z1AppOpenAd.mPackageName, pageUrl = "", tagName = z1AppOpenAd.mTagName, event = Z1EventNames.PAGE_VIEW))

                    isLoadingAd = true
                    val request = AdRequest.Builder().build()
                    AppOpenAd.load(activity, adUnitItem?.adUrl!!, request, AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                        object : AppOpenAd.AppOpenAdLoadCallback() {

                            override fun onAdLoaded(ad: AppOpenAd) {
                                Log.d(TAG, "GAM Ad was loaded")
                                appOpenAd = ad
                                isLoadingAd = false
                                loadTime = Date().time

                                eventAdLoaded()
                                showAppOpenAd()
                            }

                            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                                isLoadingAd = false
                                Log.d(TAG, "GAM Ad was failed to load. $loadAdError")

                                if(z1AppOpenAd.mApplovinAdUnitId.isNullOrEmpty().not()){
                                    Z1MediaManager.initializeApplovinSdk(mContext, object : OnApplovinInitializeSdkListener {
                                        override fun onSdkInitialized(configuration: AppLovinSdkConfiguration) {
                                            appOpenManager = ApplovinAppOpenManager(this@Z1AppOpenAdManager,z1AppOpenAd)
                                        }
                                    })
                                }else if (z1AppOpenAd.mIronSourceApiKey.isNullOrEmpty().not() && z1AppOpenAd.mIronSourceAdUnitId.isNullOrEmpty().not()){
                                    loadIronSourceAd()
                                }else{
                                    mOnShowAdCompleteListener?.onShowAdComplete()
                                    z1AppOpenAd.mListener.onAdFailedToLoad(Z1KUtils.getAdError(loadAdError))
                                }
                            }
                        }
                    )
                }
            }else{
                PixelApiHelper.logPixel(mContext, z1AppOpenAd.mEnvironment, logPixelService, Z1KUtils.getPixelDto(packageName = z1AppOpenAd.mPackageName, pageUrl = "", tagName = z1AppOpenAd.mTagName , event = Z1EventNames.BLOCK_APP, reason = tagConfigDto?.reason))
                mOnShowAdCompleteListener?.onShowAdComplete()
            }
        }catch (e:Exception){
            setErrorLog(e, ErrorFilterType.LOAD)
        }
    }

    fun showAppOpenAd(){
        try{
            appOpenAd?.apply {
                fullScreenContentCallback = object : FullScreenContentCallback() {

                    override fun onAdDismissedFullScreenContent() {
                        // Set the reference to null so isAdAvailable() returns false.
                        appOpenAd = null
                        isShowingAd = false
                        Log.d(TAG, "GAM Ad was dismiss full screen")

                        eventAdDismiss()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        appOpenAd = null
                        isShowingAd = false
                        Log.d(TAG, "GAM Ad failed to show full screen $adError")
                        mOnShowAdCompleteListener?.onShowAdComplete()
                        z1AppOpenAd.mListener.onAdFailedToShowFullScreenContent(Z1KUtils.getAdError(adError))
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "GAM Ad was show full screen.")
                        eventShowedFullScreen()
                    }
                }
                isShowingAd = true
                show(activity)
            }
        }catch (e:Exception){
            setErrorLog(e, ErrorFilterType.SHOW)
        }
    }

    private class ApplovinAppOpenManager(val appOpenAdManager: Z1AppOpenAdManager,private val z1AppOpenAd:Z1AppOpenAd ) : LifecycleObserver, MaxAdListener {
        private lateinit var appOpenAd: MaxAppOpenAd
        private var context: Context

        init {
            ProcessLifecycleOwner.get().lifecycle.addObserver(this)

            context = appOpenAdManager.mContext

            appOpenAd = MaxAppOpenAd(appOpenAdManager.z1AppOpenAd.mApplovinAdUnitId!!, context)
            appOpenAd.setListener(this)
            appOpenAd.loadAd()
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        fun onStart() {
            showAdIfReady()
        }

        private fun showAdIfReady() {
            Log.d(appOpenAdManager.TAG, "Applovin onStart .... showAdIfReady >>>>>>>>> ")
            if (this::appOpenAd.isInitialized.not() || AppLovinSdk.getInstance(context).isInitialized.not())
                return

            if(appOpenAd.isReady){
                appOpenAd.showAd(appOpenAdManager.z1AppOpenAd.mApplovinAdUnitId)
            } else {
                appOpenAd.loadAd()
            }
        }

        override fun onAdLoaded(ad: MaxAd) {
            Log.d(appOpenAdManager.TAG, "Applovin onAdLoaded >>>>>>>>>")
            appOpenAdManager.eventAdLoaded()
        }

        override fun onAdDisplayed(ad: MaxAd) {
            Log.d(appOpenAdManager.TAG, "Applovin onAdDisplayed >>>>>>>>>")
            appOpenAdManager.eventShowedFullScreen()
        }

        override fun onAdDisplayFailed(ad: MaxAd, adError: MaxError) {
            Log.d(appOpenAdManager.TAG, "Applovin onAdDisplayFailed >>>>>>>>> $adError")
            if (z1AppOpenAd.mIronSourceApiKey.isNullOrEmpty().not() && z1AppOpenAd.mIronSourceAdUnitId.isNullOrEmpty().not()){
                appOpenAdManager.loadIronSourceAd()
            }else {
                appOpenAdManager.mOnShowAdCompleteListener?.onShowAdComplete()
                appOpenAdManager.z1AppOpenAd.mListener.onAdFailedToShowFullScreenContent(Z1KUtils.getAdError(adError))
            }
        }

        override fun onAdLoadFailed(adUnitId: String, adError: MaxError) {
            Log.d(appOpenAdManager.TAG, "Applovin onAdLoadFailed >>>>>>>>> $adError")
            if (z1AppOpenAd.mIronSourceAdUnitId.isNullOrEmpty().not()){
                appOpenAdManager.loadIronSourceAd()
            }else {
                appOpenAdManager.mOnShowAdCompleteListener?.onShowAdComplete()
                appOpenAdManager.z1AppOpenAd.mListener.onAdFailedToLoad(Z1KUtils.getAdError(adError))
            }
        }

        override fun onAdHidden(ad: MaxAd) {
            Log.d(appOpenAdManager.TAG, "Applovin onAdHidden >>>>>>>>>")
            appOpenAdManager.mOnShowAdCompleteListener?.onShowAdComplete()
//            mOnShowAdCompleteListener?.onShowAdComplete()
        }

        override fun onAdClicked(ad: MaxAd) {
            Log.d(appOpenAdManager.TAG, "Applovin onAdClicked >>>>>>>>>")
        }
    }

    fun loadIronSourceAd(){
        IronSource.setUserId(IronSource.getAdvertiserId(activity)?:Z1KUtils.getRandomString(64))
        IronSource.init(activity, z1AppOpenAd.mIronSourceApiKey, IronSource.AD_UNIT.OFFERWALL)

        IronSource.setOfferwallListener(object :OfferwallListener{

            override fun onOfferwallAvailable(p0: Boolean) {
                Log.d(TAG, "IronSource onOfferWallAvailable >>>>>>>>>")
                if(IronSource.isOfferwallAvailable()){
                    IronSource.showOfferwall()
                    eventShowedFullScreen()
                }
                eventAdLoaded()
            }

            override fun onOfferwallOpened() {
                Log.d(TAG, "IronSource onOfferWallOpened >>>>>>>>>")
            }

            override fun onOfferwallShowFailed(ironSourceError: IronSourceError?) {
                Log.d(TAG, "IronSource onOfferWallShowFailed ${ironSourceError}>>>>>>>>>")
                mOnShowAdCompleteListener?.onShowAdComplete()
                z1AppOpenAd.mListener.onAdFailedToShowFullScreenContent(Z1KUtils.getAdError(ironSourceError))
            }

            override fun onOfferwallAdCredited(credits: Int, totalCredits: Int, totalCreditsFlag: Boolean): Boolean {
                Log.d(TAG, "IronSource onOfferWallAdCredited credits:$credits totalCredits:$totalCredits totalCreditsFlag:$totalCreditsFlag >>>>>>>>>")
                return false
            }

            override fun onGetOfferwallCreditsFailed(ironSourceError: IronSourceError?) {
                Log.d(TAG, "IronSource onGetOfferWallCreditsFailed ${ironSourceError}>>>>>>>>>")
            }

            override fun onOfferwallClosed() {
                Log.d(TAG, "IronSource onOfferWallClosed >>>>>>>>>")
                eventAdDismiss()
            }
        })
    }

    fun eventAdLoaded(){
        val eventDataDto = Z1KUtils.getEventData(0, adUnitItem?.partner, 0.5, Z1KUtils.TYPE_VIDEO)

        PixelApiHelper.logPixel(mContext, z1AppOpenAd.mEnvironment, logPixelService, Z1KUtils.getPixelDto(packageName = z1AppOpenAd.mPackageName, pageUrl = "",
            tagName = z1AppOpenAd.mTagName, event = Z1EventNames.PAGE_VIEW_MATCH, eventDataDto= eventDataDto))

        PixelApiHelper.logPixel(mContext, z1AppOpenAd.mEnvironment, logPixelService, Z1KUtils.getPixelDto(packageName = z1AppOpenAd.mPackageName, pageUrl = "",
            tagName = z1AppOpenAd.mTagName, event = Z1EventNames.AD_MATCH, eventDataDto= eventDataDto))

        z1AppOpenAd.mListener.onAdLoaded()
    }

    private fun eventShowedFullScreen(){
        val eventDataDto = Z1KUtils.getEventData(0, adUnitItem?.partner, 0.5)

        PixelApiHelper.logPixel(mContext, z1AppOpenAd.mEnvironment, logPixelService, Z1KUtils.getPixelDto(packageName = z1AppOpenAd.mPackageName, pageUrl = "",
            tagName = z1AppOpenAd.mTagName, event = Z1EventNames.VIEWABLE_IMPRESSION, eventDataDto= eventDataDto))

        z1AppOpenAd.mListener.onAdShowedFullScreenContent()
    }

    private fun eventAdDismiss(){
        PixelApiHelper.logPixel(mContext, z1AppOpenAd.mEnvironment, logPixelService, Z1KUtils.getPixelDto(packageName = z1AppOpenAd.mPackageName, pageUrl = "",
            tagName = z1AppOpenAd.mTagName, event = Z1EventNames.CROSS_CLICKED))

        z1AppOpenAd.mListener.onAdDismissedFullScreenContent()
        mOnShowAdCompleteListener?.onShowAdComplete()
    }

    private fun setErrorLog(e:Exception?, errorType:ErrorFilterType, errorMessage:String?=null){
        var message:String = if (e != null) {
            Log.getStackTraceString(e)
        }else{
            errorMessage?:""
        }
//        message = "filterType.code + message"
        PixelApiHelper.logError(z1AppOpenAd.mEnvironment, errorLogService, ErrorLogDto(message, z1AppOpenAd.mTagName))
        PixelApiHelper.logPixel(mContext,z1AppOpenAd.mEnvironment , logPixelService, Z1KUtils.getPixelDto(packageName = z1AppOpenAd.mPackageName, pageUrl = "",
            tagName = z1AppOpenAd.mTagName , event = Z1EventNames.ERROR, errorCode = errorType.code))
    }

}
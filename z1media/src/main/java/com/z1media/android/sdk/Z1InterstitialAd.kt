package com.z1media.android.sdk

import android.app.Activity
import android.util.Log
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdViewAdListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.ads.MaxInterstitialAd
import com.applovin.sdk.AppLovinSdkConfiguration
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.admanager.AdManagerInterstitialAd
import com.google.android.gms.ads.admanager.AdManagerInterstitialAdLoadCallback
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.LevelPlayInterstitialListener
import com.z1media.android.sdk.listeners.OnApplovinInitializeSdkListener
import com.z1media.android.sdk.listeners.Z1AdManagerInterstitialI
import com.z1media.android.sdk.manager.Z1MediaManager
import com.z1media.android.sdk.models.AdUnitsItem
import com.z1media.android.sdk.models.ErrorLogDto
import com.z1media.android.sdk.models.GetTagConfigDto
import com.z1media.android.sdk.models.Z1AdError
import com.z1media.android.sdk.utils.*


/**
*  created by Ashish Saini at 6th Feb 2023
*
**/
class Z1InterstitialAd(builder : Builder) {

    private val TAG = Z1InterstitialAd::class.java.simpleName
    private val mActivity: Activity = builder.activity
    private val mEnvironment : String = builder.mEnvironment
    private val mListener: Z1AdManagerInterstitialI = builder.mListener
    private val mTagName:String= builder.mTagName
    private val mApplovinAdUnitId : String? = builder.mApplovinAdUnitId
    private val mIronSourceApiKey : String = builder.mIronSourceApiKey?:""
    private val mIronSourceAdUnitId:String? = builder.mIronSourceAdUnitId
    private val mPackageName : String = builder.packageName
    private val errorLogService = builder.errorLogService
    private val logPixelService = builder.logPixelService
    private val tagConfigService = builder.tagConfigService
    private var tagConfigDto: GetTagConfigDto?=null
    private var mInterstitialAd: AdManagerInterstitialAd?= null
    private var adUnitItem: AdUnitsItem?= null
    private var isMediationAllowed : Boolean = builder.mIsMediationAllowed
    private var mIsPageViewLogged :Boolean = builder.mIsPageViewLogged
    private var mIsPageViewMatchLogged :Boolean = builder.mIsPageViewMatchLogged
    private var refreshAllowed :Boolean =builder.mRefreshAllowed

    init {
        PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, Z1KUtils.getPixelDto(packageName = mPackageName, pageUrl = "", tagName = mTagName, event = Z1EventNames.LOADED))
        Z1MediaManager.initializeAdsSdk(mActivity)
        getTagConfig()
    }

    class Builder(context: Activity) : Z1BaseBuilder(context){
        lateinit var mListener: Z1AdManagerInterstitialI

        fun setListener(listener: Z1AdManagerInterstitialI): Builder{
            this.mListener = listener
            return this
        }

        fun setEnvironment(environment: String): Builder {
            this.mEnvironment = environment
            return this
        }

        fun setMediation(mediationStatus:Boolean): Builder {
            this.mIsMediationAllowed=mediationStatus
            return this
        }

        fun setAllowRefresh(refresh:Boolean) : Builder {
            this.mRefreshAllowed = refresh
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

        fun setIronSourceParams(ironSourceApiKey:String?, ironSourceAdUnitId: String?): Builder {
            this.mIronSourceApiKey = ironSourceApiKey
            this.mIronSourceAdUnitId = ironSourceAdUnitId
            return this
        }

        fun build(): Z1InterstitialAd {
            return Z1InterstitialAd(this)
        }
    }

    private fun getTagConfig() {
        ConfigApiHelper.getConfig(tagConfigService, mPackageName,
            mTagName, object : ConfigApiHelper.ConfigApiListener{

                override fun onSuccess(tagConfigDto: GetTagConfigDto) {
                    this@Z1InterstitialAd.tagConfigDto = tagConfigDto
                    loadInterstitialAd()
                }

                override fun onFailure(code: Int, errorMessage: String?) {
                    Log.e(TAG, "onFailure >>>>> $errorMessage")
                    setErrorLog(null, ErrorFilterType.API_FAILURE, errorMessage)
                    val adError = Z1AdError(code,"", errorMessage,0, "", FailureType.API)
                    mListener.onAdFailedToLoad(adError)
                }
            })
    }

    fun loadInterstitialAd() {
        try{
            if (tagConfigDto == null){
                return
            }

            if (Z1KUtils.isAppInForegrounded()){
                if (!Z1KUtils.isConfigAllowed(tagConfigDto)){
                    adUnitItem = tagConfigDto?.adunits?.get(0)
                    adUnitItem?.adUrl?.let {

                        if (!mIsPageViewLogged){
                            mIsPageViewLogged = true
                            PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, Z1KUtils.getPixelDto(packageName = mPackageName, pageUrl = "", tagName = mTagName, event = Z1EventNames.PAGE_VIEW))
                        }

                        val adRequest = Z1MediaManager.getAdManagerAdRequest()

                        if (mInterstitialAd == null){
                            AdManagerInterstitialAd.load(mActivity, it, adRequest, object : AdManagerInterstitialAdLoadCallback() {

                                override fun onAdLoaded(interstitialAd: AdManagerInterstitialAd) {
                                    Log.d(TAG, "GAM Ad was loaded")
                                    mInterstitialAd = interstitialAd
                                    mListener.onAdLoaded()
                                    showInterstitial()
                                }

                                override fun onAdFailedToLoad(adError: LoadAdError) {
                                    mInterstitialAd = null
                                    Log.e(TAG, "GAM Ad was failed to load. $adError")

                                    if (mApplovinAdUnitId.isNullOrEmpty().not()){
                                        Z1MediaManager.initializeApplovinSdk(mActivity, object : OnApplovinInitializeSdkListener {
                                            override fun onSdkInitialized(configuration: AppLovinSdkConfiguration) {
                                                loadApplovinAd()
                                            }
                                        })
                                    }else if (mIronSourceApiKey.isNullOrEmpty().not() && mIronSourceAdUnitId.isNullOrEmpty().not()){
                                        loadIronSourceAd()
                                    }else{
                                            reloadInterstitialAd(Z1KUtils.Ad_FAILED_REFRESH_TIME)
                                            mListener.onAdFailedToLoad(Z1KUtils.getAdError(adError))
                                    }
                                }
                            })
                        }else{
                            showInterstitial()
                        }
                    }
                }else{
                    PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, Z1KUtils.getPixelDto(packageName = mPackageName, pageUrl = "", tagName = mTagName , event = Z1EventNames.BLOCK_APP, reason = tagConfigDto?.reason))
                }
            }else{
                if (!isMediationAllowed){
                    Log.d(TAG, "Ads is not showing due to app is in background ")
                    reloadInterstitialAd(Z1KUtils.Ad_FAILED_REFRESH_TIME)
                }

            }
        }catch (e:Exception){
            setErrorLog(e, ErrorFilterType.LOAD)
        }
    }

    private fun showInterstitial() {
        try {
            // Show the ad if it's ready. Otherwise toast and restart the game.
            if (mInterstitialAd == null) {
                Log.e(TAG, "GAM interstitial ad wasn't ready yet.")
                return
            }

            mInterstitialAd?.apply {
                fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d(TAG, "GAM Ad was dismiss full screen")
                        mInterstitialAd = null
                        eventAdDismiss()
                    }

                    override fun onAdImpression() {
                        super.onAdImpression()
                        Log.d(TAG, "GAM Ad impression")
                        eventAdLoaded(true)
                        eventAdImpression()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        mInterstitialAd = null
                        Log.e(TAG, "GAM Ad failed to show full screen $adError")

                        if (mApplovinAdUnitId.isNullOrEmpty().not()) {
                            Z1MediaManager.initializeApplovinSdk(
                                mActivity,
                                object : OnApplovinInitializeSdkListener {
                                    override fun onSdkInitialized(configuration: AppLovinSdkConfiguration) {
                                        loadApplovinAd()
                                    }
                                })
                        } else if (mIronSourceApiKey.isNullOrEmpty()
                                .not() && mIronSourceAdUnitId.isNullOrEmpty().not()
                        ) {
                            loadIronSourceAd()
                        } else {
                            mListener.onAdFailedToShowFullScreenContent(Z1KUtils.getAdError(adError))
                        }
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "GAM Ad show full screen")
                        mListener.onAdShowedFullScreenContent()

                    }
                }
                show(mActivity)
            }
        }catch (e:Exception){
            setErrorLog(e, ErrorFilterType.SHOW)
        }
    }

    private fun reloadInterstitialAd(seconds:Long){
        if(refreshAllowed){
            Z1KUtils.getMyHandler().postDelayed(runnable, seconds * 1000)
        }
    }

    private val runnable:Runnable = Runnable {
        loadInterstitialAd()
    }


    private fun loadApplovinAd() {
        val interstitialAd = MaxInterstitialAd( mApplovinAdUnitId, mActivity )
        interstitialAd.setExtraParameter( "container_view_ads", "true" )
        interstitialAd.setListener( object : MaxAdViewAdListener {
            override fun onAdLoaded(p0: MaxAd?) {
                Log.d(TAG, "Applovin onAdLoaded >>>>>>>>> isReady ${interstitialAd.isReady}")
                eventAdLoaded()

                if (interstitialAd.isReady){
                    interstitialAd.showAd()
                }
            }

            override fun onAdDisplayed(p0: MaxAd?) {
                Log.d(TAG, "Applovin onAdDisplayed >>>>>>>>> ")
                eventAdImpression()
                mListener.onAdShowedFullScreenContent()
            }

            override fun onAdDisplayFailed(p0: MaxAd?, adError: MaxError?) {
                Log.e(TAG, "Applovin onAdDisplayFailed >>>>>>>>> $adError")
                if (mIronSourceApiKey.isNullOrEmpty().not() && mIronSourceAdUnitId.isNullOrEmpty().not()){
                    loadIronSourceAd()
                }else{
                    reloadInterstitialAd(Z1KUtils.Ad_FAILED_REFRESH_TIME)
                    mListener.onAdFailedToShowFullScreenContent(Z1KUtils.getAdError(adError))
                }
            }

            override fun onAdLoadFailed(p0: String?, adError: MaxError?) {
                Log.e(TAG, "Applovin onAdLoadFailed >>>>>>>>> $adError")
                if (mIronSourceApiKey.isNullOrEmpty().not() && mIronSourceAdUnitId.isNullOrEmpty().not()){
                    loadIronSourceAd()
                }else{
                    reloadInterstitialAd(Z1KUtils.Ad_FAILED_REFRESH_TIME)
                    mListener.onAdFailedToLoad(Z1KUtils.getAdError(adError))
                }
            }

            override fun onAdHidden(p0: MaxAd?) {
                Log.d(TAG, "Applovin onAdHidden >>>>>>>>> ")
                eventAdDismiss()
            }

            override fun onAdClicked(p0: MaxAd?) {
                Log.d(TAG, "Applovin onAdClicked >>>>>>>>> ")
                mListener.onAdClicked()
            }

            override fun onAdExpanded(p0: MaxAd?) {
                Log.d(TAG, "Applovin onAdExpanded >>>>>>>>> ")
            }

            override fun onAdCollapsed(p0: MaxAd?) {
                Log.d(TAG, "Applovin onAdCollapsed >>>>>>>>> ")
            }

        })
        interstitialAd.loadAd()
    }

    private fun loadIronSourceAd(){
        IronSource.init(mActivity, mIronSourceApiKey, IronSource.AD_UNIT.INTERSTITIAL)
        IronSource.setLevelPlayInterstitialListener(object : LevelPlayInterstitialListener {

            override fun onAdReady(adInfo: AdInfo?) {
                Log.d(TAG, "IronSource onAdReady >>>>>>>>> ${IronSource.isInterstitialReady()}")
                eventAdLoaded()
                if(IronSource.isInterstitialReady()){
                    IronSource.showInterstitial(mIronSourceAdUnitId)
                }
            }

            override fun onAdLoadFailed(adError: IronSourceError) {
                Log.e(TAG, "IronSource onAdLoadFailed >>>>>>>>> $adError")
                reloadInterstitialAd(Z1KUtils.Ad_FAILED_REFRESH_TIME)
                mListener.onAdFailedToLoad(Z1KUtils.getAdError(adError))

            }

            override fun onAdOpened(adInfo: AdInfo?) {
                Log.d(TAG, "IronSource onAdOpened >>>>>>>>> ")
            }

            override fun onAdClosed(adInfo: AdInfo?) {
                Log.d(TAG, "IronSource onAdClosed >>>>>>>>> ")
                eventAdDismiss()
            }

            override fun onAdShowFailed(adError: IronSourceError?, adInfo: AdInfo?) {
                Log.d(TAG, "IronSource onAdShowFailed >>>>>>>>> ")
                reloadInterstitialAd(Z1KUtils.Ad_FAILED_REFRESH_TIME)
                mListener.onAdFailedToShowFullScreenContent(Z1KUtils.getAdError(adError))

            }

            override fun onAdClicked(adInfo: AdInfo?) {
                Log.d(TAG, "IronSource onAdClicked >>>>>>>>> ")
                mListener.onAdClicked()
            }

            override fun onAdShowSucceeded(adInfo: AdInfo?) {
                Log.d(TAG, "IronSource onAdShowSucceeded >>>>>>>>> ")
                eventAdImpression()
                mListener.onAdShowedFullScreenContent()
            }
        })
        IronSource.loadInterstitial()
    }

    private fun eventAdLoaded(isAdLoadedListener:Boolean=false){
        val eventDataDto = Z1KUtils.getEventData(0, adUnitItem?.partner, 0.5, Z1KUtils.TYPE_VIDEO)

        if (!mIsPageViewMatchLogged){
            mIsPageViewMatchLogged = true
            PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, Z1KUtils.getPixelDto(packageName = mPackageName, pageUrl = "",
                tagName = mTagName, event = Z1EventNames.PAGE_VIEW_MATCH, eventDataDto= eventDataDto))
        }

        PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, Z1KUtils.getPixelDto(packageName = mPackageName, pageUrl = "",
            tagName = mTagName, event = Z1EventNames.AD_MATCH, eventDataDto= eventDataDto))

        if (!isAdLoadedListener){
            mListener.onAdLoaded()
        }
    }

    private fun eventAdImpression(){
        val eventDataDto = Z1KUtils.getEventData(0, adUnitItem?.partner, 0.5)

        PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, Z1KUtils.getPixelDto(packageName = mPackageName, pageUrl = "",
            tagName = mTagName, event = Z1EventNames.VIEWABLE_IMPRESSION, eventDataDto= eventDataDto))

        mListener.onAdImpression()
    }
    private fun eventAdDismiss(){
        PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, Z1KUtils.getPixelDto(packageName = mPackageName, pageUrl = "",
            tagName = mTagName, event = Z1EventNames.CROSS_CLICKED))
        mListener.onAdDismissedFullScreenContent()
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
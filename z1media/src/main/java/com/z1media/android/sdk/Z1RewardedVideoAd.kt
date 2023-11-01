package com.z1media.android.sdk

import android.app.Activity
import android.util.Log
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxError
import com.applovin.mediation.MaxReward
import com.applovin.mediation.MaxRewardedAdListener
import com.applovin.mediation.ads.MaxRewardedAd
import com.applovin.sdk.AppLovinSdkConfiguration
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.model.Placement
import com.ironsource.mediationsdk.sdk.LevelPlayRewardedVideoListener
import com.z1media.android.sdk.listeners.OnApplovinInitializeSdkListener
import com.z1media.android.sdk.listeners.Z1RewardedVideoI
import com.z1media.android.sdk.manager.Z1MediaManager
import com.z1media.android.sdk.models.AdUnitsItem
import com.z1media.android.sdk.models.ErrorLogDto
import com.z1media.android.sdk.models.GetTagConfigDto
import com.z1media.android.sdk.models.Z1AdError
import com.z1media.android.sdk.utils.*
import com.z1media.android.sdk.utils.ConfigApiHelper
import com.z1media.android.sdk.utils.PixelApiHelper
import com.z1media.android.sdk.utils.Z1EventNames

/**
 *  created by Ashish Saini at 13th Feb 2023
 *
 **/

class Z1RewardedVideoAd(builder : Builder) {

    private val TAG = Z1RewardedVideoAd::class.java.simpleName
    private val mActivity: Activity = builder.activity
    private val mListener: Z1RewardedVideoI = builder.mListener
    private val mEnvironment : String = builder.mEnvironment
    private val mTagName:String= builder.mTagName
    private val mApplovinAdUnitId : String? = builder.mApplovinAdUnitId
    private val mIronSourceApiKey : String = builder.mIronSourceApiKey?:""
    private val mIronSourceAdUnitId :String? =builder.mIronSourceAdUnitId
    private val mPackageName : String = builder.packageName
    private val errorLogService= builder.errorLogService
    private val logPixelService = builder.logPixelService
    private val tagConfigService = builder.tagConfigService
    private var tagConfigDto: GetTagConfigDto?=null
    private var rewardedAd: RewardedAd? = null
    private var adUnitItem: AdUnitsItem?= null
    private var isMediationIsAllowed: Boolean =builder.mIsMediationAllowed
    private var mIsPageViewLogged :Boolean = builder.mIsPageViewLogged
    private var mIsPageViewMatchLogged :Boolean = builder.mIsPageViewMatchLogged
    private var refreshAllowed :Boolean =builder.mRefreshAllowed

    init {
        PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, Z1KUtils.getPixelDto(packageName = mPackageName, pageUrl = "", tagName = mTagName, event = Z1EventNames.LOADED))
        Z1MediaManager.initializeAdsSdk(mActivity)
        getTagConfig()
    }

    class Builder(context: Activity)  : Z1BaseBuilder(context){
        lateinit var mListener: Z1RewardedVideoI

        fun setListener(listener: Z1RewardedVideoI): Builder{
            this.mListener = listener
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

        fun setIronSourceParams(ironSourceApiKey:String?, ironSourceAdUnitId: String?): Builder {
            this.mIronSourceApiKey = ironSourceApiKey
            this.mIronSourceAdUnitId = ironSourceAdUnitId
            return this
        }

        fun build(): Z1RewardedVideoAd {
            return Z1RewardedVideoAd(this)
        }
    }

    private fun getTagConfig() {
        ConfigApiHelper.getConfig(tagConfigService, mPackageName,
            mTagName, object : ConfigApiHelper.ConfigApiListener{

                override fun onSuccess(tagConfigDto: GetTagConfigDto) {
                    this@Z1RewardedVideoAd.tagConfigDto = tagConfigDto
                    loadRewardVideoAd()
                }

                override fun onFailure(code: Int, errorMessage: String?) {
                    Log.e(TAG, "onFailure >>>>> $errorMessage")
                    setErrorLog(null, ErrorFilterType.API_FAILURE, errorMessage)
                    val adError = Z1AdError(code,"", errorMessage,0, "", FailureType.API)
                    mListener.onAdFailedToLoad(adError)
                }
            })
    }

    fun loadRewardVideoAd() {
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

                        if (rewardedAd == null) {
                            val adRequest = Z1MediaManager.getAdManagerAdRequest()

                            RewardedAd.load(mActivity, it, adRequest, object : RewardedAdLoadCallback() {

                                override fun onAdLoaded(ad: RewardedAd) {
                                    Log.d(TAG, "GAM Ad was loaded.")
                                    rewardedAd = ad
                                    showRewardedVideoAd()
                                    mListener.onAdLoaded()
                                }

                                override fun onAdFailedToLoad(adError: LoadAdError) {
                                    Log.e(TAG, "GAM Ad failed to load $adError")
                                    rewardedAd = null

                                    if(mApplovinAdUnitId.isNullOrEmpty().not()){
                                        Z1MediaManager.initializeApplovinSdk(mActivity, object : OnApplovinInitializeSdkListener {
                                            override fun onSdkInitialized(configuration: AppLovinSdkConfiguration) {
                                                loadApplovinAd()
                                            }
                                        })
                                    }else if (mIronSourceApiKey.isNullOrEmpty().not() && mIronSourceAdUnitId.isNullOrEmpty().not()){
                                        loadIronSourceAd()
                                    }else{
                                        reloadRewardVideoAd(Z1KUtils.Ad_FAILED_REFRESH_TIME)
                                        mListener.onAdFailedToLoad(Z1KUtils.getAdError(adError))
                                    }
                                }
                            })
                        }else{
                            showRewardedVideoAd()
                        }
                    }
                }else{
                    PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, Z1KUtils.getPixelDto(packageName = mPackageName, pageUrl = "", tagName = mTagName , event = Z1EventNames.BLOCK_APP, reason = tagConfigDto?.reason))
                }
            }else{
                Log.d(TAG, "Ads is not showing due to app is in background ")
                reloadRewardVideoAd(Z1KUtils.Ad_FAILED_REFRESH_TIME)
            }
        }catch (e:Exception){
            setErrorLog(e, ErrorFilterType.LOAD)
        }
    }

    private fun showRewardedVideoAd() {
        try {
            if (rewardedAd == null) {
                Log.e(TAG, "GAM rewarded interstitial ad wasn't ready yet.")
                return
            }

            rewardedAd?.apply {
                fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        rewardedAd = null
                        Log.d(TAG, "GAM Ad was dismissed.")
                        eventAdDismiss()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        Log.e(TAG, "GAM Ad failed to show full screen $adError")
                        rewardedAd = null

                        if(mApplovinAdUnitId.isNullOrEmpty().not()){
                            Z1MediaManager.initializeApplovinSdk(mActivity, object : OnApplovinInitializeSdkListener {
                                override fun onSdkInitialized(configuration: AppLovinSdkConfiguration) {
                                    loadApplovinAd()
                                }
                            })
                        }else if (mIronSourceApiKey.isNullOrEmpty().not() && mIronSourceAdUnitId.isNullOrEmpty().not()){
                            loadIronSourceAd()
                        }else{
                            mListener.onAdFailedToShowFullScreenContent(Z1KUtils.getAdError(adError))
                        }
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "GAM Ad show full screen")
                        mListener.onAdShowedFullScreenContent()
                    }
                    override fun onAdImpression() {
                        super.onAdImpression()
                        Log.d(TAG, "GAM Ad impression")
                        eventAdImpression()
                        eventAdLoaded(true)
                    }

                }

                show(mActivity) { rewardItem ->
                    Log.d(TAG, "GAM User earned the reward. amount ${rewardItem.amount},  type : ${rewardItem.type}")
                    mListener.onUserEarnedReward(rewardItem.amount, rewardItem.type)
                }
            }
        }catch (e:Exception){
            setErrorLog(e, ErrorFilterType.SHOW)
        }
    }

    private fun reloadRewardVideoAd(seconds:Long){
        if(refreshAllowed){
            Z1KUtils.getMyHandler().postDelayed(runnable, seconds * 1000)
        }
    }

    private val runnable:Runnable = Runnable {
        loadRewardVideoAd()
    }


    private fun loadApplovinAd() {
        val  rewardedAd = MaxRewardedAd.getInstance( mApplovinAdUnitId, mActivity )
        rewardedAd.setListener( object : MaxRewardedAdListener {

            override fun onAdLoaded(maxAd: MaxAd) {
                Log.d(TAG, "Applovin onAdLoaded >>>>>>>>> ${rewardedAd.isReady}")
                eventAdLoaded()

                if(rewardedAd.isReady) {
                    rewardedAd.showAd()
                }
            }
            override fun onAdDisplayed(maxAd: MaxAd) {
                Log.d(TAG, "Applovin onAdDisplayed >>>>>>>>> ")
                mListener.onAdShowedFullScreenContent()
            }

            override fun onAdLoadFailed(adUnitId: String?, adError: MaxError?) {
                Log.e(TAG, "Applovin onAdLoadFailed >>>>>>>>> ")
                if (mIronSourceApiKey.isNullOrEmpty().not() && mIronSourceAdUnitId.isNullOrEmpty().not()){
                    loadIronSourceAd()
                }else{
                    reloadRewardVideoAd(Z1KUtils.Ad_FAILED_REFRESH_TIME)
                    mListener.onAdFailedToLoad(Z1KUtils.getAdError(adError))
                }
            }

            override fun onAdDisplayFailed(ad: MaxAd?, adError: MaxError?) {
                Log.e(TAG, "Applovin onAdDisplayFailed >>>>>>>>> ")
                if (mIronSourceApiKey.isNullOrEmpty().not() && mIronSourceAdUnitId.isNullOrEmpty().not()){
                    loadIronSourceAd()
                }else {
                    reloadRewardVideoAd(Z1KUtils.Ad_FAILED_REFRESH_TIME)
                    mListener.onAdFailedToShowFullScreenContent(Z1KUtils.getAdError(adError))
                }
            }

            override fun onAdClicked(maxAd: MaxAd) {
                Log.d(TAG, "Applovin onAdClicked >>>>>>>>> ")
                mListener.onAdClicked()
            }

            override fun onAdHidden(maxAd: MaxAd) {
                Log.d(TAG, "Applovin onAdHidden >>>>>>>>> ")
                eventAdDismiss()
            }

            override fun onRewardedVideoStarted(maxAd: MaxAd) {
                Log.d(TAG, "Applovin onRewardedVideoStarted >>>>>>>>> ")
            }

            override fun onRewardedVideoCompleted(maxAd: MaxAd) {
                Log.d(TAG, "Applovin onRewardedVideoCompleted >>>>>>>>> ")
            }

            override fun onUserRewarded(maxAd: MaxAd, maxReward: MaxReward) {
                Log.d(TAG, "Applovin onUserRewarded >>>>>>>>> Rewarded user: ${maxReward.amount}  ${maxReward.label} ")
                mListener.onUserEarnedReward(maxReward.amount, maxReward.label)
            }
        })
        rewardedAd.loadAd()
    }

    private fun loadIronSourceAd(){
        IronSource.init(mActivity, mIronSourceApiKey, IronSource.AD_UNIT.REWARDED_VIDEO)
        IronSource.setLevelPlayRewardedVideoListener(object : LevelPlayRewardedVideoListener {

            override fun onAdAvailable(p0: AdInfo?) {
                Log.d(TAG, "IronSource onAdLoaded >>>>>>>>> ${IronSource.isRewardedVideoAvailable()}")
                eventAdLoaded()

                if(IronSource.isRewardedVideoAvailable()) {
                    IronSource.showRewardedVideo(mIronSourceAdUnitId)
                }
            }
            override fun onAdOpened(adInfo: AdInfo?) {
                Log.d(TAG, "IronSource onAdOpened >>>>>>>>> ")
            }

            override fun onAdClosed(adInfo: AdInfo?) {
                Log.d(TAG, "IronSource onAdClosed >>>>>>>>> ")
                eventAdDismiss()
            }

            override fun onAdUnavailable() {
                Log.e(TAG, "IronSource onAdUnavailable >>>>>>>>> ")
                reloadRewardVideoAd(Z1KUtils.Ad_FAILED_REFRESH_TIME)
            }

            override fun onAdShowFailed(adError: IronSourceError?, adInfo: AdInfo?) {
                Log.d(TAG, "IronSource onAdShowFailed >>>>>>>>> ")
                reloadRewardVideoAd(Z1KUtils.Ad_FAILED_REFRESH_TIME)
                mListener.onAdFailedToShowFullScreenContent(Z1KUtils.getAdError(adError))

            }

            override fun onAdClicked(p0: Placement?, p1: AdInfo?) {
                Log.d(TAG, "IronSource onAdClicked >>>>>>>>> ")
                mListener.onAdClicked()
            }

            override fun onAdRewarded(p0: Placement?, p1: AdInfo?) {
                Log.d(TAG, "Applovin onUserRewarded >>>>>>>>> Rewarded user: ${p0?.rewardAmount}  ${p0?.rewardName} ")
                mListener.onUserEarnedReward(p0?.rewardAmount!!,p0?.rewardName!!)

            }
        })
        IronSource.loadRewardedVideo()
    }

    private fun eventAdLoaded(isImpressionAdListener:Boolean=false){
        val eventDataDto = Z1KUtils.getEventData(0, adUnitItem?.partner, 0.5, Z1KUtils.TYPE_VIDEO)

        if (!mIsPageViewMatchLogged){
            mIsPageViewMatchLogged = true
            PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, Z1KUtils.getPixelDto(packageName = mPackageName, pageUrl = "",
                tagName = mTagName, event = Z1EventNames.PAGE_VIEW_MATCH, eventDataDto= eventDataDto))
        }

        PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, Z1KUtils.getPixelDto(packageName = mPackageName, pageUrl = "",
            tagName = mTagName, event = Z1EventNames.AD_MATCH, eventDataDto= eventDataDto))

        if (!isImpressionAdListener){
        }else{
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
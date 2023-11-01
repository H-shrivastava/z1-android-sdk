package com.z1media.android.sdk

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.z1media.android.sdk.listeners.Z1RewardInterstitialI
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
*  created by Ashish Saini at 7th Feb 2023
*
**/

class Z1RewardInterstitialAd(builder : Builder) {

    private val TAG = Z1RewardInterstitialAd::class.java.simpleName
    private val mActivity: Activity = builder.activity
    private val mListener: Z1RewardInterstitialI = builder.mListener
    private val mEnvironment : String = builder.mEnvironment
    private val mTagName:String= builder.mTagName
    private val mPackageName : String = builder.packageName
    private val errorLogService = builder.errorLogService
    private val logPixelService = builder.logPixelService
    private val tagConfigService = builder.tagConfigService
    private var tagConfigDto: GetTagConfigDto?=null
    private var adUnitItem: AdUnitsItem?= null
    private var rewardedInterstitialAd: RewardedInterstitialAd? = null
    private var isMediationAllowed :Boolean?=builder.mIsMediationAllowed
    private var mIsPageViewLogged :Boolean = builder.mIsPageViewLogged
    private var mIsPageViewMatchLogged :Boolean = builder.mIsPageViewMatchLogged
    private var refreshAllowed :Boolean =builder.mRefreshAllowed

    init {
        PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, Z1KUtils.getPixelDto(packageName = mPackageName, pageUrl = "", tagName = mTagName, event = Z1EventNames.LOADED))
        Z1MediaManager.initializeAdsSdk(mActivity)
        getTagConfig()
    }

    class Builder(context: Activity)  : Z1BaseBuilder(context){
        lateinit var mListener: Z1RewardInterstitialI

        fun setListener(listener: Z1RewardInterstitialI): Builder{
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

        fun build(): Z1RewardInterstitialAd {
            return Z1RewardInterstitialAd(this)
        }
    }

    private fun getTagConfig() {
        ConfigApiHelper.getConfig(tagConfigService, mPackageName,
            mTagName, object : ConfigApiHelper.ConfigApiListener{

                override fun onSuccess(tagConfigDto: GetTagConfigDto) {
                    this@Z1RewardInterstitialAd.tagConfigDto = tagConfigDto
                    loadRewardInterstitialAd()
                }

                override fun onFailure(code: Int, errorMessage: String?) {
                    Log.e(TAG, "onFailure >>>>> $errorMessage")
                    setErrorLog(null, ErrorFilterType.API_FAILURE, errorMessage)
                    val adError = Z1AdError(code,"", errorMessage,0, "", FailureType.API)
                    mListener.onAdFailedToLoad(adError)
                }
            })
    }

    fun loadRewardInterstitialAd() {
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

                        if (rewardedInterstitialAd == null) {
                            val adRequest = Z1MediaManager.getAdManagerAdRequest()

                            RewardedInterstitialAd.load(mActivity, it, adRequest, object : RewardedInterstitialAdLoadCallback() {
                                override fun onAdFailedToLoad(adError: LoadAdError) {
    //                                super.onAdFailedToLoad(adError)
                                    Log.e(TAG, "GAM Ad failed to load $adError")
                                    rewardedInterstitialAd = null
                                    mListener.onAdFailedToLoad(Z1KUtils.getAdError(adError))
                                }

                                override fun onAdLoaded(rewardedAd: RewardedInterstitialAd) {
                                    super.onAdLoaded(rewardedAd)
                                    Log.d(TAG, "GAM Ad was loaded.")
                                    rewardedInterstitialAd = rewardedAd
                                    showRewardedInterstitialAd(adUnitItem!!)
                                    mListener.onAdLoaded()
                                }
                            })
                        }else{
                            showRewardedInterstitialAd(adUnitItem!!)
                        }
                    }
                }else{
                    PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, Z1KUtils.getPixelDto(packageName = mPackageName, pageUrl = "", tagName = mTagName , event = Z1EventNames.BLOCK_APP, reason = tagConfigDto?.reason))
                }
            }else{
                Log.d(TAG, "Ads is not showing due to app is in background ")
                reloadRewardInterstitialAd(Z1KUtils.Ad_FAILED_REFRESH_TIME)
            }
        }catch (e:Exception){
            setErrorLog(e, ErrorFilterType.LOAD)
        }
    }

    private fun showRewardedInterstitialAd(adUnitItem:AdUnitsItem) {
        try {
            if (rewardedInterstitialAd == null) {
                Log.e(TAG, "GAM rewarded interstitial ad wasn't ready yet.")
                return
            }

            rewardedInterstitialAd?.apply {
                fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        rewardedInterstitialAd = null
                        Log.d(TAG, "GAM Ad was dismissed.")

                        PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, Z1KUtils.getPixelDto(packageName = mPackageName, pageUrl = "",
                            tagName = mTagName, event = Z1EventNames.CROSS_CLICKED))
                        mListener.onAdDismissedFullScreenContent()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {

                        rewardedInterstitialAd = null
                        Log.e(TAG, "GAM Ad failed to show full screen $adError")
                        mListener.onAdFailedToShowFullScreenContent(Z1KUtils.getAdError(adError))
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
            mListener.onAdLoaded()
        }
    }
    private fun eventAdImpression(){
        val eventDataDto = Z1KUtils.getEventData(0, adUnitItem?.partner, 0.5)

        PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, Z1KUtils.getPixelDto(packageName = mPackageName, pageUrl = "",
            tagName = mTagName, event = Z1EventNames.VIEWABLE_IMPRESSION, eventDataDto= eventDataDto))

        mListener.onAdImpression()
    }


    private fun reloadRewardInterstitialAd(seconds:Long){
        if(refreshAllowed){
            Z1KUtils.getMyHandler().postDelayed(runnable, seconds * 1000)
        }
    }
    private val runnable:Runnable = Runnable {
        loadRewardInterstitialAd()
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
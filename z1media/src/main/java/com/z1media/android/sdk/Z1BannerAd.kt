package com.z1media.android.sdk

import android.app.Activity
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdViewAdListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.ads.MaxAdView
import com.applovin.sdk.AppLovinSdkConfiguration
import com.google.ads.interactivemedia.v3.api.AdErrorEvent
import com.google.ads.interactivemedia.v3.api.AdEvent
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.ima.ImaAdsLoader
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.admanager.AdManagerAdView
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.IronSourceBannerLayout
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.LevelPlayBannerListener
import com.z1media.android.sdk.databinding.MediationPlayerLayoutBinding
import com.z1media.android.sdk.listeners.OnApplovinInitializeSdkListener
import com.z1media.android.sdk.listeners.Z1BannerAdsI
import com.z1media.android.sdk.manager.Z1MediaManager
import com.z1media.android.sdk.models.AdUnitsItem
import com.z1media.android.sdk.models.ErrorLogDto
import com.z1media.android.sdk.models.GetTagConfigDto
import com.z1media.android.sdk.models.Z1AdError
import com.z1media.android.sdk.utils.*
/**
 *  created by Ashish Saini at 1st Feb 2023
 *
 **/

class Z1BannerAd(builder : Builder) {

    val TAG = Z1BannerAd::class.java.simpleName
    private val mActivity : Activity = builder.activity
    private val mPackageName : String = builder.packageName
    private val mEnvironment : String = builder.mEnvironment
    private val mTagName : String = builder.mTagName
    private val mApplovinAdUnitId : String? = builder.mApplovinAdUnitId
    private val mIronSourceAdUnitId :String? =builder.mIronSourceAdUnitId
    private val mAdSize : Z1AdSize = builder.mBannerAdSize
    var mAdContainer : ViewGroup? = builder.mAdContainer
    val mListener : Z1BannerAdsI = builder.mListener
    private val errorLogService = builder.errorLogService
    private val logPixelService = builder.logPixelService
    private val tagConfigService = builder.tagConfigService
    private var tagConfigDto: GetTagConfigDto?=null
    private var isReloadBannerAd = false
    private var adUnitItem:AdUnitsItem?= null
    private val mIronSourceApiKey : String = builder.mIronSourceApiKey?:""
    private var mIronSourceBannerLayout :IronSourceBannerLayout?=null
    private var maxAdView :MaxAdView?= null
    private var adManagerAdView : AdManagerAdView?= null
    private var adDisplayType:AdType = AdType.GOOGLE_AD_MANAGER
    private var isMediationAllowed : Boolean = builder.mIsMediationAllowed
    private var playerHandler:MediationPlayerHandler?= null
    private var mIsPageViewLogged = false
    private var mIsPageViewMatchLogged = false
    private var mRandomInt:Int?= builder.mRandomInt
    private var randomRefresh :Int?=null
    private var refreshAllowed :Boolean =builder.mRefreshAllowed

    init {
        PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, Z1KUtils.getPixelDto(packageName = mPackageName, pageUrl = "", tagName = mTagName, event = Z1EventNames.LOADED))
        Z1MediaManager.initializeAdsSdk(mActivity)
        setCurrentMediationIdentifier()
        getTagConfig()
    }

    class Builder(context: Activity) : Z1BaseBuilder(context){
        var mAdContainer: ViewGroup?=null
        lateinit var mListener: Z1BannerAdsI

        fun setEnvironment(environment: String): Builder {
            this.mEnvironment = environment
            return this
        }
        fun setMediation(mediationFlag:Boolean, random:Int?):Builder{
            this.mIsMediationAllowed=mediationFlag
            this.mRandomInt=random
            return this
        }
        fun setAllowRefresh(refresh:Boolean) :Builder{
            this.mRefreshAllowed = refresh
            return this
        }
        fun setBannerView( adContainer: ViewGroup?): Builder {
            this.mAdContainer = adContainer
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

        fun setAddSize(adSize: Z1AdSize): Builder {
            this.mBannerAdSize = adSize
            return this
        }

        fun setListener(listener: Z1BannerAdsI): Builder {
            this.mListener = listener
            return this
        }

        fun build(): Z1BannerAd {
            return Z1BannerAd(this)
        }
    }

    private fun getTagConfig() {
        ConfigApiHelper.getConfig(tagConfigService, mPackageName,
            mTagName, object : ConfigApiHelper.ConfigApiListener{

                override fun onSuccess(tagConfigDto: GetTagConfigDto) {
                    this@Z1BannerAd.tagConfigDto = tagConfigDto
                    loadBannerAd()
                }

                override fun onFailure(code: Int, errorMessage: String?) {
                    Log.e(TAG, "onFailure >>>>> $errorMessage")
                    setErrorLog(null, ErrorFilterType.API_FAILURE, errorMessage)
                    val adError = Z1AdError(code,"", errorMessage,0, "", FailureType.API)
                    mListener.onAdFailedToLoad(adError)
                }
            })
    }

    private class MediationPlayerHandler(val bannerAd: Z1BannerAd) : Player.Listener {

        private val TAG :String= "MediationAdView"
        private lateinit var binding: MediationPlayerLayoutBinding

        private var mExoPlayer: ExoPlayer?= null
        private var mImaAdsLoader: ImaAdsLoader?= null
        var isVideoAdLoaded = false
        var isViewabledImpressionListenerCalled = false
        var imaAdErrorOrAdBreakFetch = false

//        val mSampleVideoUrl : String by lazy {
//            "https://h5.vdo.ai/media_file/in-app-sample-b-v2-Z1/source//uploads/videos/1651688516796272c4441cf56.m3u8"
//            "https://storage.googleapis.com/gvabox/media/samples/stock.mp4"
//            "https://h5.vdo.ai/media_file/in-app-paytm-secure-b-Z1-v1/source/vhs/Top%2010%20biggest%20sporting%20event%20in%20the%20world_1.mp4"
//        }

        val mVastTagUrl : String by lazy {
            if (BuildConfig.DEBUG){

                "https://a.vdo.ai/core/test/vmap.xml"
            }else{
                "https://a.vdo.ai/core/${bannerAd.mTagName}/vmap"
            }
//        "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/vmap_ad_samples&sz=640x480&cust_params=sample_ar%3Dpremidpost&ciu_szs=300x250&gdfp_req=1&ad_rule=1&output=vmap&unviewed_position_start=1&env=vp&impl=s&cmsid=496&vid=short_onecue&correlator="
        }

        init {
            binding = MediationPlayerLayoutBinding.inflate(LayoutInflater.from(bannerAd.mAdContainer!!.context))
            mImaAdsLoader = initializeImaAdsLoader()
            mExoPlayer = initializeExoPlayer()
            setPlayer()
        }

        private fun initializeImaAdsLoader(): ImaAdsLoader {
            return ImaAdsLoader.Builder(bannerAd.mAdContainer!!.context)
                .setAdEventListener(buildAdEventListener())
                .setAdErrorListener(buildAdErrorListener())
                .build()
        }

        private fun buildAdEventListener(): AdEvent.AdEventListener {

            val imaAdEventListener = AdEvent.AdEventListener { adEvent ->
                val eventType = adEvent.type

                if (eventType == AdEvent.AdEventType.AD_PROGRESS) {
                    return@AdEventListener
                }
                Log.d(TAG, "IMA event: $eventType")

                if(eventType == AdEvent.AdEventType.LOADED){
                    if (!isVideoAdLoaded){
                        isVideoAdLoaded = true
                        Log.d(TAG, "view added when AdEvent.AdEventType.LOADED called  ")
                        bannerAd.mAdContainer?.addView(binding.root)

                        bannerAd.eventAdLoaded()
                        if (bannerAd.isMediationAllowed){
                            bannerAd.mListener.onMediationSuccess()
                        }
                    }else{
                        val eventDataDto = Z1KUtils.getEventData(0, bannerAd.adUnitItem?.partner, 0.5, Z1KUtils.TYPE_BANNER)
                        PixelApiHelper.logPixel(bannerAd.mActivity, bannerAd.mEnvironment, bannerAd.logPixelService, Z1KUtils.getPixelDto(packageName = bannerAd.mPackageName, pageUrl = "",
                            tagName = bannerAd.mTagName, event = Z1EventNames.AD_MATCH, eventDataDto= eventDataDto))
                    }
                    viewableImpression()
                } else if (eventType == AdEvent.AdEventType.AD_BREAK_FETCH_ERROR){
                    imaAdErrorOrAdBreakFetch = true
                }else if(eventType == AdEvent.AdEventType.ALL_ADS_COMPLETED){
                    releasePlayer()
                }
            }
            return imaAdEventListener
        }

        private fun buildAdErrorListener(): AdErrorEvent.AdErrorListener {

            val imaAdErrorListener= AdErrorEvent.AdErrorListener { adErrorEvent ->
                Log.d(TAG, "IMA error event: $adErrorEvent")
                imaAdErrorOrAdBreakFetch = true
                releasePlayer()
            }
            return imaAdErrorListener
        }

        private fun initializeExoPlayer(): ExoPlayer {
            val maxWidth = 300.toPx(bannerAd.mActivity) //resources.getDimension(R.dimen.floating_video_max_window_width).toInt()
            val maxHeight = 250.toPx(bannerAd.mActivity) //resources.getDimension(R.dimen.floating_video_max_window_height).toInt()

            val trackSelector = DefaultTrackSelector(bannerAd.mActivity,
                DefaultTrackSelector.Parameters.Builder(bannerAd.mActivity)
                    .setMaxVideoSize(maxWidth, maxHeight)
                    .build())

            val dataSourceFactory: DataSource.Factory = DefaultDataSource.Factory(bannerAd.mActivity)
            val mediaSourceFactory: MediaSource.Factory = DefaultMediaSourceFactory(dataSourceFactory)
                .setLocalAdInsertionComponents({
                        unusedAdTagUri: MediaItem.AdsConfiguration? -> mImaAdsLoader
                }, binding.player)

            return ExoPlayer.Builder(bannerAd.mActivity)
                .setTrackSelector(trackSelector)
                .setMediaSourceFactory(mediaSourceFactory)
                .build()
        }

        private fun setPlayer() {
            binding.player.player = mExoPlayer
            mExoPlayer?.addListener(this)

            binding.stopBtn.setOnClickListener {
                releasePlayer()
            }

            binding.player.setControllerVisibilityListener(
                StyledPlayerView.ControllerVisibilityListener { visibility ->
                    binding.stopBtn.visibility = visibility
                }
            )
        }

        fun showVideoAds(videoUrl:String, vastTagUrl:String) {

            binding.player.player = mExoPlayer
            mImaAdsLoader?.setPlayer(mExoPlayer)

            // Create the MediaItem to play, specifying the content URI and ad tag URI.
            val contentUri = Uri.parse(videoUrl)
            val adTagUri = Uri.parse(vastTagUrl)
            val mediaItem = MediaItem.Builder()
                .setUri(contentUri)
                .setAdsConfiguration(
                    MediaItem.AdsConfiguration
                        .Builder(adTagUri).build())
                .build()

            mExoPlayer?.let {
                it.stop()
                it.setMediaItem(mediaItem)
                it.prepare()
                it.volume = 0f
                it.playWhenReady = true
            }
        }

        fun releasePlayer(isDestroy:Boolean= false) {
            try {
                mImaAdsLoader?.setPlayer(null)
                if (this::binding.isInitialized){
                    binding.player.player = null
                }
                if (mExoPlayer!= null){
                    mExoPlayer?.release()
                    mExoPlayer = null
                }

                if (!isDestroy){
                    if (imaAdErrorOrAdBreakFetch){
                        bannerAd.loadBannerAd()
                    }else if (isVideoAdLoaded){
                        Log.d(TAG, "ALL_ADS_COMPLETED called player remove from container")
                        bannerAd.mAdContainer?.removeAllViews()
                    }
                }
            }catch (e:Exception){
                bannerAd.setErrorLog(e, ErrorFilterType.RELEASE_PLAYER_FAILURE)
                bannerAd.mAdContainer?.removeAllViews()
                bannerAd.loadBannerAd()
            }
        }

        fun viewableImpression(){
            val mainHandler = Handler(Looper.getMainLooper())
            mainHandler.postDelayed({
                Log.d(TAG,"ViewableImpression after ad loaded 2 seconds")
                bannerAd.eventViewableImpression()
                if (!isViewabledImpressionListenerCalled){
                    isViewabledImpressionListenerCalled = true
                    bannerAd.mListener.onAdImpression()
                }
            }, 2000)
        }
    }

    @Synchronized
    private fun setCurrentMediationIdentifier(){
        if(isMediationAllowed){
//            Log.d("BannerAdView", "old current value = ${Z1KUtils.currentMediationIdentifyer}")
            Z1KUtils.currentMediationIdentifyer = mRandomInt
//            Log.d("BannerAdView", "latest current value = ${Z1KUtils.currentMediationIdentifyer}")
        }
    }

    @Synchronized
    private fun killMediationRequest(){
        if (isMediationAllowed && mRandomInt != null){

            if (Z1KUtils.currentMediationIdentifyer != mRandomInt){
                Log.d("BannerAdView", "destroying........$mRandomInt.\n")
                destroyBanner(this@Z1BannerAd)
//                mAdContainer = null
//                mListener.onMediationDestroy()
                return
            }else{
                Log.d("BannerAdView", "current and random is equal.........\n")
            }

//            if (Z1KUtils.currentMediationIdentifyer != mRandomInt){
//                Z1KUtils.previousMap?.put(mRandomInt!!, Z1KUtils.currentMediationIdentifyer!!)
//                Log.d("BannerAdView", "updated previousMap .........\n")
//            }else{
//                Log.d("BannerAdView", "current and random is equal.........\n")
//            }
//
//            Z1KUtils.previousMap?.apply {
//                if (isNullOrEmpty().not() && contains(mRandomInt)){
//                    Log.e("BannerAdView", "onMediationRemovedFromStack().......$mRandomInt")
//                    mAdContainer = null
//                    mListener.onMediationDestroy()
//                    return
//                }
//            }
        }
    }

    fun loadBannerAd() {

        try {
            if (tagConfigDto == null || mAdContainer ==null)
                return

            killMediationRequest()

            adDisplayType = AdType.GOOGLE_AD_MANAGER

            if (Z1KUtils.isAppInForegrounded() && mAdContainer != null){

                if (!Z1KUtils.isConfigAllowed(tagConfigDto)){

                    adUnitItem = tagConfigDto?.adunits?.get(0)
                    adUnitItem?.adUrl?.let {

                        eventPageView()
                        if (mAdSize == Z1AdSize.MEDIUM_RECTANGLE &&  (playerHandler == null || !playerHandler!!.imaAdErrorOrAdBreakFetch)){
                            playerHandler = MediationPlayerHandler(this).apply {
                                showVideoAds(tagConfigDto?.mediaFile?:"", this.mVastTagUrl)
                            }
                        }else {
                            playerHandler = null
                            mAdContainer?.removeAllViews()
                            adManagerAdView = AdManagerAdView(mActivity)
                            adManagerAdView?.apply {
                                setAdSize(mAdSize.adSize)
                                adUnitId = it

                                val adRequest = Z1MediaManager.getAdManagerAdRequest()
                                loadAd(adRequest)
                                mAdContainer?.addView(adManagerAdView)

                                adListener = object : AdListener() {

                                    override fun onAdLoaded() {
                                        Log.d(TAG, "GAM Ad was loaded")
                                        mListener.onAdLoaded()
                                    }

                                    override fun onAdImpression() {
                                        Log.d(TAG, "GAM Ad was impression")
                                        eventAdLoaded(true)
                                        eventAdImpression()
                                    }

                                    override fun onAdClicked() {
                                        Log.d(TAG, "GAM Ad was clicked.")
                                        mListener.onAdClicked()
                                    }

                                    override fun onAdClosed() {
                                        Log.d(TAG, "GAM Ad was closed.")
                                        mListener.onAdClosed()
                                    }

                                    override fun onAdFailedToLoad(adError: LoadAdError) {
                                        Log.e(TAG, "GAM Ad was failed to load. $adError")

                                        if(mApplovinAdUnitId.isNullOrEmpty().not()){
                                            Z1MediaManager.initializeApplovinSdk(mActivity, object : OnApplovinInitializeSdkListener {
                                                override fun onSdkInitialized(configuration: AppLovinSdkConfiguration) {
                                                    loadApplovinAd()
                                                }
                                            })
                                        }else if (mIronSourceApiKey.isNullOrEmpty().not() && mIronSourceAdUnitId.isNullOrEmpty().not()){
                                            loadIronSourceAd()
                                        }else{
                                            reloadBannerAd(Z1KUtils.Ad_FAILED_REFRESH_TIME)
                                            mListener.onAdFailedToLoad(Z1KUtils.getAdError(adError))
                                        }
                                    }

                                    override fun onAdOpened() {
                                        Log.d(TAG, "GAM Ad was opened")
                                        mListener.onAdOpened()
                                    }
                                }
                            }
                        }
                    }
                }else{
                    PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, Z1KUtils.getPixelDto(packageName = mPackageName, pageUrl = "", tagName = mTagName , event = Z1EventNames.BLOCK_APP, reason = tagConfigDto?.reason))
                }
            }else{
                if (!isMediationAllowed){
                    Log.d(TAG, "Ads is not showing due to app is in background ")
                    reloadBannerAd(Z1KUtils.Ad_FAILED_REFRESH_TIME)
                }
            }
        }catch (e:Exception){
            setErrorLog(e, ErrorFilterType.LOAD)
        }
    }

    private fun reloadBannerAd(seconds:Long){
        if(refreshAllowed){
            isReloadBannerAd = true
            Z1KUtils.getMyHandler().postDelayed(runnable, seconds * 1000)
        }
    }

    private val runnable:Runnable = Runnable {
        loadBannerAd()
    }

    fun removeHandler(){
        Z1KUtils.getMyHandler().removeCallbacks(runnable)
    }

    private fun loadApplovinAd(){

        adDisplayType = AdType.APPLOVIN
        mAdContainer?.removeAllViews()
        maxAdView = Z1KUtils.getApplovinAdView(mActivity,mApplovinAdUnitId!!,mAdSize)
        maxAdView?.apply {
            setListener(object : MaxAdViewAdListener {

                override fun onAdLoaded(p0: MaxAd?) {
                    Log.d(TAG, "Applovin onAdLoaded >>>>>>>>> ")
                    eventAdLoaded()
                }

                override fun onAdDisplayed(p0: MaxAd?) {
                    Log.d(TAG, "Applovin onAdDisplayed >>>>>>>>> ")
                    eventAdImpression()
                }

                override fun onAdHidden(p0: MaxAd?) {
                    Log.d(TAG, "Applovin onAdHidden >>>>>>>>> ")
                }

                override fun onAdClicked(p0: MaxAd?) {
                    Log.d(TAG, "Applovin onAdClicked >>>>>>>>> ")
                    mListener.onAdClicked()
                }

                override fun onAdLoadFailed(p0: String?, adError: MaxError?) {
                    Log.e(TAG, "Applovin onAdLoadFailed >>>>>>>>> $adError")
                    if (mIronSourceApiKey.isNullOrEmpty().not() && mIronSourceAdUnitId.isNullOrEmpty().not()){
                        loadIronSourceAd()
                    } else {
                        reloadBannerAd(Z1KUtils.Ad_FAILED_REFRESH_TIME)
                        mListener.onAdFailedToLoad(Z1KUtils.getAdError(adError))
                    }
                }

                override fun onAdDisplayFailed(p0: MaxAd?, p1: MaxError?) {
                    Log.e(TAG, "Applovin onAdDisplayFailed >>>>>>>>> ")
                }

                override fun onAdExpanded(p0: MaxAd?) {
                    Log.d(TAG, "Applovin onAdCollapsed >>>>>>>>> ")
                }

                override fun onAdCollapsed(p0: MaxAd?) {
                    Log.d(TAG, "Applovin onAdCollapsed >>>>>>>>> ")
                }
            })

            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val heightPx =Z1KUtils.getApplovinBannerHeight(mActivity,mAdSize)
            layoutParams = FrameLayout.LayoutParams(width, heightPx)
            setBackgroundColor(Color.WHITE)
            mAdContainer?.addView(this)
            loadAd()
            Log.d(TAG, "Applovin maxAdView.loadAd() >>>>>>>>> ")
        }
    }

    private fun loadIronSourceAd(){

        adDisplayType = AdType.IRON_SOURCE
        mAdContainer?.removeAllViews()

        val adSize = Z1KUtils.getIronSourceAdSize(mAdSize)
        IronSource.init(mActivity, mIronSourceApiKey, IronSource.AD_UNIT.BANNER)
        mIronSourceBannerLayout  = IronSource.createBanner(mActivity, adSize)

        mIronSourceBannerLayout?.let {
            val layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT)

            it.levelPlayBannerListener =object:LevelPlayBannerListener{
                override fun onAdLoaded(adInfo: AdInfo?) {
                    Log.d(TAG, "IronSource onAdLoaded and addDisplayed >>>>>>>>> ")
                    eventAdLoaded()
                    eventAdImpression()
                }

                override fun onAdLoadFailed(adError: IronSourceError?) {
                    Log.d(TAG, "IronSource onAdLoadFailed >>>>>>>>> $adError")
                    reloadBannerAd(Z1KUtils.Ad_FAILED_REFRESH_TIME)
                    mListener.onAdFailedToLoad(Z1KUtils.getAdError(adError))
                }

                override fun onAdClicked(adinfo: AdInfo?) {
                    Log.d(TAG, "IronSource onAdClicked >>>>>>>>> ")
                    mListener.onAdClicked()
                }

                override fun onAdLeftApplication(adinfo: AdInfo?) {
                    Log.d(TAG, "IronSource onAdLeftApplication >>>>>>>>> ")
                }

                override fun onAdScreenPresented(adinfo: AdInfo?) {
                    Log.d(TAG, "IronSource onAdScreenPresented >>>>>>>>> ")

                }

                override fun onAdScreenDismissed(adinfo: AdInfo?) {
                    Log.d(TAG, "IronSource onAdScreenDismissed >>>>>>>>> ")
                    mListener.onAdClosed()
                }
            }

            IronSource.loadBanner(it, mIronSourceAdUnitId)
            mAdContainer?.addView(it, 0, layoutParams)
            Log.d(TAG, "IronSource loadBanner >>>>>>>>> ")
        }
    }

    fun onResume(activity:Activity){
        if (adDisplayType == AdType.IRON_SOURCE){
            IronSource.onResume(activity)
        }
    }

    fun onPause(activity: Activity){
        if (adDisplayType == AdType.IRON_SOURCE){
            IronSource.onPause(activity)
        }
    }

    fun destroyBanner(bannerAd:Z1BannerAd){

        playerHandler?.releasePlayer(true)
        playerHandler =null
        bannerAd.adManagerAdView?.destroy()
        bannerAd.adManagerAdView = null
        bannerAd.maxAdView?.destroy()
        bannerAd.maxAdView = null
        if (bannerAd.mIronSourceBannerLayout != null){
            IronSource.destroyBanner(bannerAd.mIronSourceBannerLayout)
        }
        bannerAd.mAdContainer = null
        Z1KUtils.getMyHandler().removeCallbacks(bannerAd.runnable)
    }

    private fun eventPageView(){
        if(!mIsPageViewLogged){
            mIsPageViewLogged = true
            PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, Z1KUtils.getPixelDto(packageName = mPackageName, pageUrl = "", tagName = mTagName, event = Z1EventNames.PAGE_VIEW))
        }
    }

    private fun eventViewableImpression(){
        val eventDataDto = Z1KUtils.getEventData(0, adUnitItem?.partner, 0.5)
        PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, Z1KUtils.getPixelDto(packageName = mPackageName, pageUrl = "",
            tagName = mTagName, event = Z1EventNames.VIEWABLE_IMPRESSION, eventDataDto= eventDataDto))
    }

    private fun eventAdLoaded(isImpressionAdListener:Boolean=false){

        val eventDataDto = Z1KUtils.getEventData(0, adUnitItem?.partner, 0.5, Z1KUtils.TYPE_BANNER)
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
        eventViewableImpression()
        mListener.onAdImpression()

        if (adDisplayType == AdType.GOOGLE_AD_MANAGER || adDisplayType == AdType.APPLOVIN){
            val refreshTime = if (isMediationAllowed) Z1KUtils.AD_REFRESH_MEDIATION else Z1KUtils.AD_REFRESH
            reloadBannerAd(refreshTime)

        }
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

}
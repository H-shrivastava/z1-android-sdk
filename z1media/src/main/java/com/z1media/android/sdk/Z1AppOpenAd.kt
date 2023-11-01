package com.z1media.android.sdk

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.z1media.android.sdk.listeners.OnShowAdCompleteListener
import com.z1media.android.sdk.listeners.Z1AppOpenI
import com.z1media.android.sdk.manager.Z1AppOpenAdManager
import com.z1media.android.sdk.manager.Z1MediaManager
import com.z1media.android.sdk.networking.RetrofitHelper
import com.z1media.android.sdk.utils.*
import com.z1media.android.sdk.utils.PixelApiHelper
import com.z1media.android.sdk.utils.Z1EventNames
/**
 *  created by Ashish Saini at 14th Feb 2023
 *
 **/

class Z1AppOpenAd(builder : Builder) : Application.ActivityLifecycleCallbacks {

    private val TAG = Z1AppOpenAd::class.java.simpleName
    private val mApplication:Application = builder.mApplication
    val mPackageName : String = mApplication.packageName
    val mEnvironment : String = builder.mEnvironment
    val mTagName : String = builder.mTagName
    val mApplovinAdUnitId : String? = builder.mApplovinAdUnitId
    val mIronSourceApiKey : String = builder.mIronSourceApiKey?:""
    val mIronSourceAdUnitId :String? =builder.mIronSourceAdUnitId
    private val mActivity = mApplication.applicationContext
    val mListener : Z1AppOpenI = builder.mListener

    private val tagConfigService = RetrofitHelper.getTagConfigServices(mApplication.applicationContext )
    private val logPixelService = RetrofitHelper.getLogPixelServices(mApplication.applicationContext)
    private val errorLogService = RetrofitHelper.getErrorLogServices(mApplication.applicationContext )

    private var appOpenAdMgr: Z1AppOpenAdManager?=null
    private var currentActivity: Activity? = null

    init {
        init()
    }

    class Builder(application: Application) {
        lateinit var mEnvironment:String
        lateinit var mTagName:String
        var mApplovinAdUnitId:String?=null
        var mIronSourceApiKey:String?=null
        var mIronSourceAdUnitId:String?=null
        val mApplication:Application = application
        lateinit var mListener: Z1AppOpenI

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

        fun setListener(listener: Z1AppOpenI): Builder {
            this.mListener = listener
            return this
        }

        fun build(): Z1AppOpenAd {
            return Z1AppOpenAd(this)
        }
    }


    private fun init() {
        PixelApiHelper.logPixel(mActivity, mEnvironment, logPixelService, Z1KUtils.getPixelDto(packageName = mPackageName, pageUrl = "", tagName = mTagName, event = Z1EventNames.LOADED))
        mApplication.registerActivityLifecycleCallbacks(this)

        Z1MediaManager.initializeAdsSdk(mApplication)
//        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        appOpenAdMgr = Z1AppOpenAdManager(mActivity,this@Z1AppOpenAd, tagConfigService, logPixelService, errorLogService)
    }
    /**
     * Shows an app open ad.
     *
     * @param activity the activity that shows the app open ad
     * @param onShowAdCompleteListener the listener to be notified when an app open ad is complete
     */
    fun showAdIfAvailable(activity: Activity, onShowAdCompleteListener: OnShowAdCompleteListener) {
        // We wrap the showAdIfAvailable to enforce that other classes only interact with MyApplication
        // class.
        appOpenAdMgr?.showAdIfAvailable(activity, onShowAdCompleteListener)
    }

    /** ActivityLifecycleCallback methods. */
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {

    }

    override fun onActivityStarted(activity: Activity) {
        // An ad activity is started when an ad is showing, which could be AdActivity class from Google
        // SDK or another activity class implemented by a third party mediation partner. Updating the
        // currentActivity only when an ad is not showing will ensure it is not an ad activity, but the
        // one that shows the ad.
        appOpenAdMgr?.let {
            if (!it.isShowingAd) {
                it.activity = activity
                currentActivity = activity
            }
        }

    }

    override fun onActivityResumed(activity: Activity) {

    }

    override fun onActivityPaused(activity: Activity) {

    }

    override fun onActivityStopped(activity: Activity) {

    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {

    }

    override fun onActivityDestroyed(activity: Activity) {

    }



}
package com.z1media.android.sdk.utils

import android.app.Activity
import com.z1media.android.sdk.networking.RetrofitHelper
import kotlin.random.Random

open class Z1BaseBuilder(context: Activity) {

    val activity: Activity = context
    val packageName:String =  context.packageName
    lateinit var mEnvironment: String
    lateinit var mTagName:String
    lateinit var mBannerAdSize: Z1AdSize
    val tagConfigService = RetrofitHelper.getTagConfigServices(context)
    val logPixelService = RetrofitHelper.getLogPixelServices(context)
    val errorLogService = RetrofitHelper.getErrorLogServices(context)
    var mApplovinAdUnitId:String?=null
    var mIronSourceApiKey :String?= null
    var mIronSourceAdUnitId :String?=null
    var mIsMediationAllowed:Boolean = false
    var mIsPageViewLogged : Boolean =false
    var mIsPageViewMatchLogged :Boolean =false
    var mRandomInt:Int?= null
    var mRefreshAllowed : Boolean=true




}
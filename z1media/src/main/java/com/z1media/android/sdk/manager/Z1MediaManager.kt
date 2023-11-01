package com.z1media.android.sdk.manager

import android.content.Context
import androidx.multidex.MultiDex
import com.applovin.sdk.AppLovinSdk
import com.applovin.sdk.AppLovinSdkConfiguration
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.z1media.android.sdk.listeners.OnApplovinInitializeSdkListener
import com.z1media.android.sdk.utils.Z1KUtils

class Z1MediaManager {

    companion object {

        fun setDeviceId(context: Context){

            val deviceId = Z1KUtils.getDeviceId(context)
            val testDeviceIds = mutableListOf(deviceId)
            val configuration = RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build()
            MobileAds.setRequestConfiguration(configuration)
        }

        @JvmStatic
        fun initializeAdsSdk(context: Context){
            // initialize GAM sdk
            MobileAds.initialize(context) {
            }
        }

        @JvmStatic
        fun getAdManagerAdRequest(): AdManagerAdRequest {
            return AdManagerAdRequest.Builder().build()
        }

        @JvmStatic
        fun getAdRequest(): AdRequest {
            return AdRequest.Builder().build()
        }

        @JvmStatic
        fun initMultiDex(context: Context){
            MultiDex.install(context)
        }

        fun initializeApplovinSdk(context: Context, listener: OnApplovinInitializeSdkListener){
            if (AppLovinSdk.getInstance(context).isInitialized){
                AppLovinSdk.getInstance(context).configuration
                listener.onSdkInitialized(AppLovinSdk.getInstance(context).configuration)
            }else{
                // Make sure to set the mediation provider value to "max" to ensure proper functionality
                AppLovinSdk.getInstance(context).mediationProvider = "max"
                AppLovinSdk.getInstance(context).initializeSdk { configuration: AppLovinSdkConfiguration ->
                    listener.onSdkInitialized(configuration)
                }
            }
        }

    }
}
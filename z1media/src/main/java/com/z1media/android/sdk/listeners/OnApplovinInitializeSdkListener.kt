package com.z1media.android.sdk.listeners

import com.applovin.sdk.AppLovinSdkConfiguration

interface OnApplovinInitializeSdkListener {

    fun onSdkInitialized(configuration : AppLovinSdkConfiguration)
}
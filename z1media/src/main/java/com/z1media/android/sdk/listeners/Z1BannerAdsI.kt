package com.z1media.android.sdk.listeners

import com.z1media.android.sdk.models.Z1AdError

/**
 *  created by Ashish Saini at 30th Jan 2023
 *
 **/

interface Z1BannerAdsI {

    fun onAdImpression()

    fun onAdLoaded()

    fun onAdFailedToLoad(adError: Z1AdError?)

    fun onAdClicked()

    fun onAdOpened()

    fun onAdClosed()

    fun onMediationSuccess(){}

//    fun onMediationDestroy(){}

}
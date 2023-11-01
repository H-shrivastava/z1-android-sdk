package com.z1media.android.sdk.listeners

import com.z1media.android.sdk.models.Z1AdError

/**
 *  created by Ashish Saini at 14th Feb 2023
 */
interface Z1AppOpenI {

    fun onAdLoaded()

    fun onAdFailedToLoad(adError: Z1AdError?)

    fun onAdDismissedFullScreenContent()

    fun onAdShowedFullScreenContent()

    fun onAdFailedToShowFullScreenContent(adError: Z1AdError?)


}
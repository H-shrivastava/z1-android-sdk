package com.z1media.android.core.listeners

import com.z1media.android.core.models.VdoAdError

/**
 *  created by Ashish Saini at 5th Oct 2023
 *
 **/
interface VdoInterstitialListener {

    fun onAdLoaded()

    fun onAdImpression()

    fun onAdFailedToLoad(adError: VdoAdError?)

    fun onAdClicked()

    fun onAdDismissedFullScreenContent()

    fun onAdShowedFullScreenContent()

    fun onAdFailedToShowFullScreenContent(adError: VdoAdError?)


}

package com.z1media.android.sdk.listeners

import com.z1media.android.sdk.models.Z1AdError


interface Z1AdManagerInterstitialI {

    fun onAdLoaded()

    fun onAdImpression()

    fun onAdFailedToLoad(adError: Z1AdError?)

    fun onAdClicked()

    fun onAdDismissedFullScreenContent()

    fun onAdShowedFullScreenContent()

    fun onAdFailedToShowFullScreenContent(adError: Z1AdError?)


}

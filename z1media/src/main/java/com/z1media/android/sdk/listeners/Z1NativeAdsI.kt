package com.z1media.android.sdk.listeners

import com.google.android.gms.ads.nativead.NativeAd
import com.z1media.android.sdk.models.Z1AdError

interface Z1NativeAdsI {

    fun forNativeAd(nativeAd:NativeAd){}

    fun onAdLoaded()

    fun onAdFailedToLoad(adError: Z1AdError?)

    fun onAdImpression()

    fun onAdClosed()

    fun onAdClicked()

}
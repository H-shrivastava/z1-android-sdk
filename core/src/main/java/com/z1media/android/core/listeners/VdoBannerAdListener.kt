package com.z1media.android.core.listeners

import com.z1media.android.core.models.VdoAdError


/**
 *  created by Ashish Saini at 4th Oct 2023
 *
 **/
interface VdoBannerAdListener {

    fun onAdImpression()

    fun onAdLoaded()

    fun onAdFailedToLoad(adError: VdoAdError?)

    fun onAdClicked()

    fun onAdOpened()

    fun onAdClosed()


}
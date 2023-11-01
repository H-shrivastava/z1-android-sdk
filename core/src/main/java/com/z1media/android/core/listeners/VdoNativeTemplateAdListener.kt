package com.z1media.android.core.listeners

import com.z1media.android.core.models.VdoAdError


/**
 *  created by Ashish Saini at 6th Oct 2023
 *
 **/
interface VdoNativeTemplateAdListener {

    fun onAdLoaded()

    fun onAdFailedToLoad(adError: VdoAdError?)

    fun onAdImpression()

    fun onAdClosed()

    fun onAdClicked()

}
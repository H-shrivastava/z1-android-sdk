package com.z1media.android.core.listeners

import com.google.android.gms.ads.nativead.NativeAd

/**
 *  created by Ashish Saini at 6th Oct 2023
 *
 **/
interface VdoNativeAdListener : VdoNativeTemplateAdListener{

    fun onNativeAd(nativeAd:NativeAd)
}
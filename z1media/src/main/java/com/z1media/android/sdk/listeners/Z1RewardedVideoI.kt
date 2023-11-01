package com.z1media.android.sdk.listeners

/**
 *  created by Ashish Saini at 13th Feb 2023
 *
 **/

interface Z1RewardedVideoI : Z1AdManagerInterstitialI {
    fun onUserEarnedReward(amount: Int, type: String)
}
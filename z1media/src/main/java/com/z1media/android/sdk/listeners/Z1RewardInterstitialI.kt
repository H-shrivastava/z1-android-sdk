package com.z1media.android.sdk.listeners

interface Z1RewardInterstitialI : Z1AdManagerInterstitialI {
    fun onUserEarnedReward(amount: Int, type: String)
}
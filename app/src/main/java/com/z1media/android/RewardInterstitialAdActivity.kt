package com.z1media.android

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.z1media.android.databinding.ActivityRewardInterstitialAdBinding
import com.z1media.android.sdk.Z1RewardInterstitialAd
import com.z1media.android.sdk.listeners.Z1RewardInterstitialI
import com.z1media.android.sdk.models.Z1AdError

class RewardInterstitialAdActivity : AppCompatActivity() {
   lateinit var binding : ActivityRewardInterstitialAdBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRewardInterstitialAdBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val rewardInterstitialAd = Z1RewardInterstitialAd.Builder(this)
            .setEnvironment(BuildConfig.BUILD_TYPE)
            .setTagName("in-app-sample-rw-Z1")
            .setAllowRefresh(false)
            .setListener(object : Z1RewardInterstitialI {

                override fun onAdLoaded() {

                }

                override fun onAdImpression() {

                }

                override fun onAdFailedToLoad(adError: Z1AdError?) {

                }

                override fun onAdClicked() {
                }

                override fun onAdDismissedFullScreenContent() {
                }

                override fun onAdFailedToShowFullScreenContent(errMsg: Z1AdError?) {
                }


                override fun onAdShowedFullScreenContent() {
                }

                override fun onUserEarnedReward(amount: Int, type: String) {
                    binding.title.text = "Reward Amount $amount , type: $type"
                }



            }).build()
        rewardInterstitialAd.loadRewardInterstitialAd()
    }
}
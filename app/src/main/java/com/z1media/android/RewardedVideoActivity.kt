package com.z1media.android

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.z1media.android.application.MyApplication
import com.z1media.android.databinding.ActivityRewardedVideoBinding
import com.z1media.android.sdk.Z1RewardedVideoAd
import com.z1media.android.sdk.listeners.Z1RewardedVideoI
import com.z1media.android.sdk.models.Z1AdError

class RewardedVideoActivity : AppCompatActivity() {
    lateinit var  binding: ActivityRewardedVideoBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRewardedVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val rewardedVideoAd = Z1RewardedVideoAd.Builder(this)
            .setEnvironment(BuildConfig.BUILD_TYPE)
            .setAllowRefresh(false)
            .setTagName("in-app-sample-rw-Z1")
            .setApplovinAdUnitId("df24933bfe0e0313")
            .setIronSourceParams((application as MyApplication).IRON_SOURCE_APP_KEY,"DefaultRewardedVideo")
            .setListener(object : Z1RewardedVideoI {

                override fun onAdLoaded() {

                }

                override fun onAdImpression() {

                }

                override fun onAdFailedToLoad(adError: Z1AdError?) {

                }

                override fun onAdClicked() {

                }

                override fun onAdShowedFullScreenContent() {

                }

                override fun onAdDismissedFullScreenContent() {

                }

                override fun onAdFailedToShowFullScreenContent(adError: Z1AdError?) {

                }

                override fun onUserEarnedReward(amount: Int, type: String) {
                    binding.title.text = "Reward Amount $amount , type: $type"
                }

            }).build()
        rewardedVideoAd.loadRewardVideoAd()
    }
}
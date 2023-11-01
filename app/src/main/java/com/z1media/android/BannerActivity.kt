package com.z1media.android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.z1media.android.application.MyApplication
import com.z1media.android.databinding.ActivityBannerBinding
import com.z1media.android.sdk.Z1BannerAd
import com.z1media.android.sdk.listeners.Z1BannerAdsI
import com.z1media.android.sdk.models.Z1AdError
import com.z1media.android.sdk.utils.Z1AdSize

class BannerActivity : AppCompatActivity() {

    lateinit var binding : ActivityBannerBinding
    val builder = StringBuilder()
    private lateinit var bannerAd :Z1BannerAd

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        val videoFragment = Z1VideoAdsFragment()
//        val bundle = Bundle()
//        bundle.putString(Z1VideoAdsFragment.VIDEO_URL_KEY, "https://storage.googleapis.com/gvabox/media/samples/stock.mp4")
//        bundle.putString(Z1VideoAdsFragment.VAST_TAG_URL_KEY, "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/single_ad_samples&sz=640x480&cust_params=sample_ct%3Dlinear&ciu_szs=300x250%2C728x90&gdfp_req=1&output=vast&unviewed_position_start=1&env=vp&impl=s&correlator=")
//        videoFragment.arguments = bundle
//        Z1KUtils.addFragment(supportFragmentManager, binding.videoAdsContainer.id, videoFragment )


        bannerAd = Z1BannerAd.Builder(this)
            .setEnvironment(BuildConfig.BUILD_TYPE)
            .setBannerView(binding.bannerAdsContainer)
            .setTagName("in-app-sample-b-Z1")
            .setApplovinAdUnitId("d166d2539686a150")
            .setIronSourceParams((application as MyApplication).IRON_SOURCE_APP_KEY,"DefaultBanner")
            .setAllowRefresh(false)
            .setAddSize(Z1AdSize.BANNER)
            .setListener( object : Z1BannerAdsI {
                override fun onAdClicked() {
                    builder.append("Add Clicked \n")
                    binding.textView.text = builder.toString()
                }

                override fun onAdClosed() {
                    builder.append("Add Closed \n")
                    binding.textView.text = builder.toString()
                }

                override fun onAdFailedToLoad(adError: Z1AdError?) {
                    builder.append("Add Fail to load \n")
                    binding.textView.text = builder.toString()
                }

                override fun onAdImpression() {
                    builder.append("Add impression \n")
                    binding.textView.text = builder.toString()
                }

                override fun onAdLoaded() {
                    builder.append("Add loaded \n")
                    binding.textView.text = builder.toString()
                }

                override fun onAdOpened() {
                    builder.append("Add opened \n")
                    binding.textView.text = builder.toString()
                }
            }).build()
        bannerAd.loadBannerAd()

    }

    override fun onResume() {
        super.onResume()
        if (this::bannerAd.isInitialized){
            bannerAd.onResume(this)
        }
    }

    override fun onPause() {
        super.onPause()

        if (this::bannerAd.isInitialized){
            bannerAd.onPause(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (this::bannerAd.isInitialized){
            bannerAd.destroyBanner(bannerAd)
        }
    }

    override fun onBackPressed() {
        supportFragmentManager.popBackStack()
        super.onBackPressed()
    }

}
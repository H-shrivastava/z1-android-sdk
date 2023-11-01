package com.z1media.android

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.z1media.android.databinding.ActivityNativeTemplateAdBinding
import com.z1media.android.sdk.Z1NativeAd
import com.z1media.android.sdk.listeners.Z1NativeAdsI
import com.z1media.android.sdk.models.Z1AdError
import com.z1media.android.sdk.utils.Z1MediaAspectRatio
import com.z1media.android.sdk.utils.gone
import com.z1media.android.sdk.utils.visible

class NativeTemplateAdActivity : AppCompatActivity() {

    lateinit var  binding: ActivityNativeTemplateAdBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNativeTemplateAdBinding.inflate(layoutInflater)
        setContentView(binding.root)

        smallNativeAd()

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_native_ad, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.small_native_ad ->{
                smallNativeAd()
                return true
            }

            R.id.medium_native_ad ->{
                mediumNativeAd()
                return true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

   private fun smallNativeAd(){

       binding.templateSmall.visible()
       binding.templateMedium.gone()

        val nativeAd = Z1NativeAd.Builder(this)
            .setEnvironment(BuildConfig.BUILD_TYPE)
            .setTemplateView(binding.templateSmall)
            .setTagName("in-app-sample-n-Z1")
            .setApplovinAdUnitId("bd4cea782509e809")
            .setMediaAspectRatio(Z1MediaAspectRatio.PORTRAIT)
            .setBackgroundColor(R.color.gnt_white)
            .setListener(object :  Z1NativeAdsI {

                override fun onAdLoaded() {

                }

                override fun onAdImpression() {

                }

                override fun onAdClosed() {

                }

                override fun onAdClicked() {

                }

                override fun onAdFailedToLoad(adError: Z1AdError?) {

                }

            }).build()
        nativeAd.loadNativeAd()
    }

    private fun mediumNativeAd(){

        binding.templateSmall.gone()
        binding.templateMedium.visible()

        val nativeAd = Z1NativeAd.Builder(this)
            .setEnvironment(BuildConfig.BUILD_TYPE)
            .setTemplateView(binding.templateMedium)
            .setAllowRefresh(false)
            .setTagName("in-app-sample-n-Z1")
            .setApplovinAdUnitId("9dd802ebdaa7cb7b")
            .setMediaAspectRatio(Z1MediaAspectRatio.PORTRAIT)
            .setBackgroundColor(R.color.gnt_white)
            .setListener(object :  Z1NativeAdsI {

                override fun onAdLoaded() {

                }

                override fun onAdImpression() {

                }

                override fun onAdClosed() {

                }

                override fun onAdClicked() {

                }

                override fun onAdFailedToLoad(adError: Z1AdError?) {

                }

            }).build()
        nativeAd.loadNativeAd()

    }

}
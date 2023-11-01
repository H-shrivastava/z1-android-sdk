package com.z1media.android

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.z1media.android.databinding.ActivityBannerBinding
import com.z1media.android.databinding.ActivityMainBinding
import com.z1media.android.databinding.ActivityVideoAdsBinding
import com.z1media.android.sdk.utils.Z1KUtils

class VideoAdsActivity : AppCompatActivity() {

    lateinit var binding: ActivityVideoAdsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoAdsBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        Z1KUtils.addFragment(supportFragmentManager,  R.id.ad_container, Z1VideoAdsFragment())

    }
}
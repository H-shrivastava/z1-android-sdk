package com.z1media.android.sdk.utils

import com.google.android.gms.ads.AdSize

enum class Z1AdSize(val adSize: AdSize) {
    BANNER(AdSize.BANNER), LARGE_BANNER(AdSize.LARGE_BANNER), MEDIUM_RECTANGLE(AdSize.MEDIUM_RECTANGLE),
    FULL_BANNER(AdSize.FULL_BANNER), LEADERBOARD(AdSize.LEADERBOARD)
}

enum class Z1MediaAspectRatio(val ratio:Int){
    UNKNOWN(0),  ANY(1), LANDSCAPE(2), PORTRAIT(3), SQUARE(4)
}

enum class FailureType(val code :Int){
    API(0), GOOGLE_AD_MANAGER(1), APPLOVIN(2), IRON_SOURCE(3)
}

enum class AdType(val code :Int){
     GOOGLE_AD_MANAGER(1), APPLOVIN(2), IRON_SOURCE(3)
}

enum class ErrorFilterType(val code :String){
//    API_FAILURE(1), LOAD(2), SHOW(3), RELEASE_PLAYER_FAILURE(4)
//
    API_FAILURE("api_failure"), LOAD("load_failure"), RELEASE_PLAYER_FAILURE("release_player_failure"),
    SHOW("show_failure")
}
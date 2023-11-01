package com.z1media.android.sdk.models

import com.z1media.android.sdk.utils.FailureType


data class Z1AdError(val code :Int?=0, val domain:String?="", val message:String?="",
                     val mediatedNetworkErrorCode :Int=0, val mediatedNetworkErrorMessage:String?="",
                     val failureType: FailureType)



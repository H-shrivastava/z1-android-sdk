package com.z1media.android.sdk.utils

import android.content.Context
import android.view.View


fun View?.visible(){
    this?.visibility = View.VISIBLE
}

fun View?.gone(){
    this?.visibility = View.GONE
}

fun View?.invisible(){
    this?.visibility = View.INVISIBLE
}

fun Int.toPx(context: Context): Int = (this * context.resources.displayMetrics.density).toInt()

fun Int.toDp(context: Context): Int = (this / context.resources.displayMetrics.density).toInt()
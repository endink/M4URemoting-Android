// Copyright (c) 2025 Anders Xiao. All rights reserved.
// https://github.com/endink
package com.labijie.m4u

import android.content.Context
import android.content.Context.MODE_PRIVATE

import android.content.SharedPreferences




object M4UUtils {

    private var latestHost: String? = null
    private var latestPort: Int = 0

    private fun getPreferences(ctx: Context):SharedPreferences
    {
        return ctx.getSharedPreferences("M4U_PREFERENCE", MODE_PRIVATE);
    }

    fun getLatestHost(ctx: Context): String {
        if(latestHost.isNullOrBlank()) {
            val sp: SharedPreferences = getPreferences(ctx);
            latestHost = sp.getString("latest_host", null) ?:  "192.168.1.100"
        }
        return latestHost!!
    }

    fun setLatestHost(ctx: Context, host:String) {
        latestHost = host
        val sp: SharedPreferences = getPreferences(ctx);
        val editor = sp.edit()
        editor.putString("latest_host", host)
        editor.apply()
    }

    fun getLatestPort(ctx: Context): Int {
        if(latestPort == 0) {
            val sp: SharedPreferences = getPreferences(ctx);
            latestPort = sp.getInt("latest_port", 22345)
        }
        return latestPort
    }

    fun setLatestPort(ctx: Context, port:Int) {
        latestPort = port
        val sp: SharedPreferences = getPreferences(ctx);
        val editor = sp.edit()
        editor.putInt("latest_port", port)
        editor.apply()
    }
}
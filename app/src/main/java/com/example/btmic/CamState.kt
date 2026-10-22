package com.example.btmic

import android.content.Context

object CamState {
    @Volatile var running = false
    @Volatile var pin = ""

    fun load(ctx: Context) {
        val p = ctx.getSharedPreferences("btmic", Context.MODE_PRIVATE)
        pin = p.getString("cam_pin", "") ?: ""
    }

    fun save(ctx: Context) {
        ctx.getSharedPreferences("btmic", Context.MODE_PRIVATE).edit()
            .putString("cam_pin", pin)
            .apply()
    }
}

package com.example.btmic

import android.content.Context

object CamState {
    @Volatile var running = false
    @Volatile var earbudEnabled = false

    fun load(ctx: Context) {
        val p = ctx.getSharedPreferences("btmic", Context.MODE_PRIVATE)
        earbudEnabled = p.getBoolean("earbud_trigger", false)
    }

    fun save(ctx: Context) {
        ctx.getSharedPreferences("btmic", Context.MODE_PRIVATE).edit()
            .putBoolean("earbud_trigger", earbudEnabled)
            .apply()
    }
}

package com.example.btmic

import android.content.Context

object LocationState {
    @Volatile var sharing = false
    @Volatile var room = "1234"
    @Volatile var serverHost = "traker-mi1p.onrender.com"

    fun load(ctx: Context) {
        val p = ctx.getSharedPreferences("btmic", Context.MODE_PRIVATE)
        room = p.getString("loc_room", "1234") ?: "1234"
        serverHost = "traker-mi1p.onrender.com"
    }

    fun save(ctx: Context) {
        ctx.getSharedPreferences("btmic", Context.MODE_PRIVATE).edit()
            .putString("loc_room", room)
            .apply()
    }
}

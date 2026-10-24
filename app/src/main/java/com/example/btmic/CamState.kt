package com.example.btmic

import android.content.Context

object CamState {
    @Volatile var running = false

    fun load(ctx: Context) {
        // ab koi stored PIN nahi hai, time hi PIN hai
    }

    fun save(ctx: Context) {
        // kuch save karne ki zaroorat nahi
    }
}

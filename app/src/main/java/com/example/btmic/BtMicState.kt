package com.example.btmic

import android.content.Context

object BtMicState {
    @Volatile var running = false
    @Volatile var level = 0f
    @Volatile var record = false
    @Volatile var currentRecPath: String? = null
    @Volatile var recStartMs = 0L

    @Volatile var gain = 1.5f
    @Volatile var voiceOnly = true
    @Volatile var blockOthers = true
    @Volatile var autoRecord = true
    @Volatile var autoStart = true
    @Volatile var multi = true
    @Volatile var earbudEnabled = false

    fun load(ctx: Context) {
        val p = ctx.getSharedPreferences("btmic", Context.MODE_PRIVATE)
        gain = p.getFloat("gain", 1.5f)
        voiceOnly = p.getBoolean("voiceOnly", true)
        blockOthers = p.getBoolean("blockOthers", true)
        autoRecord = p.getBoolean("autoRecord", true)
        autoStart = p.getBoolean("autoStart", true)
        multi = p.getBoolean("multi", true)
        earbudEnabled = p.getBoolean("earbud_trigger", false)
    }

    fun save(ctx: Context) {
        ctx.getSharedPreferences("btmic", Context.MODE_PRIVATE).edit()
            .putFloat("gain", gain)
            .putBoolean("voiceOnly", voiceOnly)
            .putBoolean("blockOthers", blockOthers)
            .putBoolean("autoRecord", autoRecord)
            .putBoolean("autoStart", autoStart)
            .putBoolean("multi", multi)
            .putBoolean("earbud_trigger", earbudEnabled)
            .apply()
    }
}

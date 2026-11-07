package com.example.btmic

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileOutputStream

class OfflineTileServer(private val ctx: Context, port: Int) : NanoHTTPD(port) {

    private var db: SQLiteDatabase? = null

    fun openDatabase(): Boolean {
        val dbFile = File(ctx.filesDir, "offline_map.mbtiles")
        if (!dbFile.exists()) {
            try {
                ctx.assets.open("offline_map.mbtiles").use { input ->
                    FileOutputStream(dbFile).use { output -> input.copyTo(output) }
                }
            } catch (e: Exception) {
                return false
            }
        }
        return try {
            db = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun serve(session: IHTTPSession): Response {
        val parts = session.uri.trim('/').split("/")
        if (parts.size != 3) return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "bad request")

        return try {
            val z = parts[0].toInt()
            val x = parts[1].toInt()
            val yRaw = parts[2].removeSuffix(".png").toInt()
            val y = (1 shl z) - 1 - yRaw

            val cursor = db?.rawQuery(
                "SELECT tile_data FROM tiles WHERE zoom_level=? AND tile_column=? AND tile_row=?",
                arrayOf(z.toString(), x.toString(), y.toString())
            )
            if (cursor != null && cursor.moveToFirst()) {
                val bytes = cursor.getBlob(0)
                cursor.close()
                newFixedLengthResponse(Response.Status.OK, "image/png", bytes.inputStream(), bytes.size.toLong())
            } else {
                cursor?.close()
                newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "no tile")
            }
        } catch (e: Exception) {
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "error")
        }
    }

    companion object {
        @Volatile private var instance: OfflineTileServer? = null
        @Volatile var ready = false

        fun start(ctx: Context): Int {
            if (instance != null && ready) return 8744
            val server = OfflineTileServer(ctx.applicationContext, 8744)
            ready = server.openDatabase()
            if (ready) {
                try {
                    server.start(SOCKET_READ_TIMEOUT, false)
                    instance = server
                } catch (e: Exception) {
                    ready = false
                }
            }
            return 8744
        }
    }
}

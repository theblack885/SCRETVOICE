package com.example.btmic

import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.StorageService
import java.io.File
import java.io.FileInputStream

object Transcriber {

    private var cachedModel: Model? = null
    private var cachedModelName: String? = null

    @Volatile var busy = false

    fun transcribe(
        ctx: Context,
        wavFile: File,
        language: String,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (busy) {
            onError("Pehle wali transcription chal rahi hai, thoda ruko")
            return
        }
        busy = true
        val modelAssetName = if (language == "hi") "model-hi" else "model-en-us"

        fun runRecognition(model: Model) {
            Thread {
                try {
                    val recognizer = Recognizer(model, 16000.0f)
                    val input = FileInputStream(wavFile)
                    input.skip(44)
                    val buffer = ByteArray(4096)
                    var n: Int
                    while (input.read(buffer).also { n = it } >= 0) {
                        if (n > 0) recognizer.acceptWaveForm(buffer, n)
                    }
                    input.close()
                    val finalJson = recognizer.finalResult
                    recognizer.close()
                    val text = try {
                        JSONObject(finalJson).optString("text", "").trim()
                    } catch (e: Exception) {
                        ""
                    }
                    busy = false
                    if (text.isBlank()) onError("Koi awaaz samajh nahi aayi")
                    else onResult(text)
                } catch (e: Exception) {
                    busy = false
                    onError("Transcribe fail hua: ${e.message}")
                }
            }.start()
        }

        if (cachedModel != null && cachedModelName == modelAssetName) {
            runRecognition(cachedModel!!)
            return
        }

        StorageService.unpack(
            ctx, modelAssetName, "model",
            { model ->
                cachedModel = model
                cachedModelName = modelAssetName
                runRecognition(model)
            },
            { exception ->
                busy = false
                Log.e("Transcriber", "Model load failed", exception)
                onError("Language model load nahi hua. Kya $modelAssetName assets me daala hai?")
            }
        )
    }

    fun saveTranscript(wavFile: File, text: String): File {
        val txtFile = File(wavFile.parentFile, wavFile.nameWithoutExtension + ".txt")
        txtFile.writeText(text)
        return txtFile
    }

    fun readTranscript(wavFile: File): String? {
        val txtFile = File(wavFile.parentFile, wavFile.nameWithoutExtension + ".txt")
        return if (txtFile.exists()) txtFile.readText() else null
    }
}

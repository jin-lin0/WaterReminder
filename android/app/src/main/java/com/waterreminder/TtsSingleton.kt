package com.waterreminder

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

object TtsSingleton : TextToSpeech.OnInitListener {

    private const val TAG = "TtsSingleton"
    private var tts: TextToSpeech? = null
    private var initialized = false
    private var pendingText: String? = null
    private var pendingContext: Context? = null

    fun speak(context: Context, text: String) {
        Log.e(TAG, "speak requested: $text")
        if (tts == null) {
            pendingText = text
            pendingContext = context.applicationContext
            tts = TextToSpeech(context.applicationContext, this)
        } else if (initialized) {
            doSpeak(text)
        } else {
            pendingText = text
        }
    }

    fun stop() {
        Log.d(TAG, "stop called")
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "stop error", e)
        }
    }

    fun shutdown() {
        Log.d(TAG, "shutdown called")
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            initialized = false
        } catch (e: Exception) {
            Log.e(TAG, "shutdown error", e)
        }
    }

    override fun onInit(status: Int) {
        Log.e(TAG, "onInit status=$status")
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { engine ->
                val result = engine.setLanguage(Locale.CHINESE)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    engine.setLanguage(Locale.US)
                }
                engine.setSpeechRate(0.9f)
                engine.setPitch(1.0f)

                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        Log.e(TAG, "TTS onStart: $utteranceId")
                    }

                    override fun onDone(utteranceId: String?) {
                        Log.e(TAG, "TTS onDone: $utteranceId")
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        Log.e(TAG, "TTS onError: $utteranceId")
                    }
                })

                initialized = true
                pendingText?.let { text ->
                    doSpeak(text)
                    pendingText = null
                }
            }
        } else {
            Log.e(TAG, "TTS init failed")
        }
    }

    private fun doSpeak(text: String) {
        try {
            tts?.let { engine ->
                engine.stop()

                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()

                val params = Bundle().apply {
                    putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                }

                engine.setAudioAttributes(audioAttributes)
                engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, "water_reminder_tts")
                Log.e(TAG, "TTS speak called successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "doSpeak error", e)
        }
    }
}

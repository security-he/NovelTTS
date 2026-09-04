package com.mosheng.noveltts.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import com.mosheng.noveltts.data.*
import kotlinx.coroutines.*
import java.io.File
import java.nio.FloatBuffer

/**
 * TTS引擎
 * 封装ONNX Runtime推理 + AudioTrack播放
 * 支持VITS/ChatTTS模型，三种音色：男声、女声、旁白
 */
class TTSEngine(private val context: Context) {

    companion object {
        private const val SAMPLE_RATE = 22050
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    // 音色配置
    data class VoiceProfile(
        val speakerId: Int,
        val speed: Float = 1.0f,
        val pitch: Float = 1.0f
    )

    private val voices = mapOf(
        "male" to VoiceProfile(speakerId = 0, speed = 1.0f, pitch = 0.95f),
        "female" to VoiceProfile(speakerId = 1, speed = 1.0f, pitch = 1.15f),
        "narrator" to VoiceProfile(speakerId = 2, speed = 0.95f, pitch = 1.0f)
    )

    private var audioTrack: AudioTrack? = null
    private var isPlaying = false
    private var isPaused = false
    private var playJob: Job? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    // 回调
    var onSegmentStart: ((blockIndex: Int, segmentIndex: Int) -> Unit)? = null
    var onFinish: (() -> Unit)? = null

    /**
     * 初始化音频播放器
     */
    fun initAudio() {
        val bufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT
        )
        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(CHANNEL_CONFIG)
                    .setEncoding(AUDIO_FORMAT)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize * 4)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    /**
     * 播放一系列文本块
     */
    fun playBlocks(blocks: List<TextBlock>, startIndex: Int = 0) {
        stop()
        isPlaying = true
        isPaused = false

        playJob = CoroutineScope(Dispatchers.Default).launch {
            for (i in startIndex until blocks.size) {
                if (!isPlaying) break
                while (isPaused && isPlaying) delay(100)
                if (!isPlaying) break

                val block = blocks[i]
                val segments = splitText(block.text)

                for (j in segments.indices) {
                    if (!isPlaying) break
                    while (isPaused && isPlaying) delay(100)
                    if (!isPlaying) break

                    mainHandler.post {
                        onSegmentStart?.invoke(i, j)
                    }

                    val audio = synthesize(segments[j], block)
                    if (audio != null) {
                        playAudio(audio)
                    }

                    // 句间停顿
                    val delay = if (segments[j].endsWithAny("。！？!?…")) 250L else 80L
                    delay(delay)
                }
                // 段落间停顿
                delay(150)
            }
            isPlaying = false
            mainHandler.post { onFinish?.invoke() }
        }
    }

    /**
     * 合成单段音频
     * 返回PCM float数组
     */
    private suspend fun synthesize(text: String, block: TextBlock): FloatArray? {
        return withContext(Dispatchers.Default) {
            try {
                val profile = when {
                    block.type == BlockType.DIALOGUE && block.gender == Gender.MALE -> voices["male"]!!
                    block.type == BlockType.DIALOGUE && block.gender == Gender.FEMALE -> voices["female"]!!
                    else -> voices["narrator"]!!
                }

                // TODO: 调用ONNX模型推理
                // 实际实现需要：文本转音素 -> 模型推理 -> 声码器
                // 这里返回占位，模型加载后替换
                ModelManager.getEngine()?.let { engine ->
                    engine.infer(text, profile.speakerId, profile.speed, profile.pitch)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * 播放PCM音频
     */
    private fun playAudio(audio: FloatArray) {
        val track = audioTrack ?: return
        // float -> short (PCM 16bit)
        val pcm = ShortArray(audio.size)
        for (i in audio.indices) {
            pcm[i] = (audio[i].coerceIn(-1f, 1f) * 32767).toInt().toShort()
        }
        if (track.state != AudioTrack.STATE_INITIALIZED) return
        if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
            track.play()
        }
        track.write(pcm, 0, pcm.size)
    }

    /**
     * 按标点分割长文本
     */
    private fun splitText(text: String, maxLen: Int = 80): List<String> {
        val result = mutableListOf<String>()
        val sentences = text.split(Regex("(?<=[。！？；!?;])")).map { it.trim() }.filter { it.isNotEmpty() }
        for (sent in sentences) {
            if (sent.length > maxLen) {
                val parts = sent.split(Regex("(?<=[，、,])")).map { it.trim() }.filter { it.isNotEmpty() }
                var current = ""
                for (part in parts) {
                    if (current.length + part.length > maxLen && current.isNotEmpty()) {
                        result.add(current)
                        current = part
                    } else {
                        current += part
                    }
                }
                if (current.isNotEmpty()) result.add(current)
            } else {
                result.add(sent)
            }
        }
        return result.ifEmpty { listOf(text) }
    }

    private fun String.endsWithAny(vararg suffixes: String): Boolean {
        return suffixes.any { this.endsWith(it) }
    }

    fun pause() {
        isPaused = true
        audioTrack?.pause()
    }

    fun resume() {
        isPaused = false
        audioTrack?.play()
    }

    fun stop() {
        isPlaying = false
        isPaused = false
        playJob?.cancel()
        audioTrack?.pause()
        audioTrack?.flush()
    }

    fun release() {
        stop()
        audioTrack?.release()
        audioTrack = null
    }

    fun isPlayingAudio(): Boolean = isPlaying
    fun isPausedAudio(): Boolean = isPaused
}

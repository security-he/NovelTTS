package com.mosheng.noveltts.model

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.FloatBuffer
import java.nio.LongBuffer

/**
 * ONNX TTS推理引擎
 * 封装VITS/ChatTTS模型的推理流程
 *
 * 模型输入：
 * - input_ids: 音素ID序列 [1, seq_len]
 * - speaker_id: 说话人ID [1]
 * - speed: 语速 [1]
 *
 * 模型输出：
 * - audio: PCM音频 [1, audio_len]
 */
class OnnxTTS(modelPath: String) {

    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession

    init {
        val opts = OrtSession.SessionOptions()
        opts.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
        opts.setIntraOpNumThreads(4)
        session = env.createSession(modelPath, opts)
    }

    /**
     * 推理：文本 -> PCM音频
     * @param text 文本
     * @param speakerId 说话人ID（0=男, 1=女, 2=旁白）
     * @param speed 语速
     * @param pitch 音调
     * @return PCM float数组
     */
    fun infer(text: String, speakerId: Int = 0, speed: Float = 1.0f, pitch: Float = 1.0f): FloatArray {
        // 1. 文本转音素ID
        val phonemeIds = textToPhonemes(text)

        // 2. 构造输入tensor
        val inputIds = LongBuffer.wrap(phonemeIds.toLongArray())
        val inputTensor = OnnxTensor.createTensor(env, inputIds, longArrayOf(1, phonemeIds.size.toLong()))

        val speakerTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(longArrayOf(speakerId.toLong())), longArrayOf(1))

        val speedTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(floatArrayOf(speed)), longArrayOf(1))

        // 3. 推理
        val inputs = mapOf(
            "input_ids" to inputTensor,
            "speaker_id" to speakerTensor,
            "speed" to speedTensor
        )

        val result = session.run(inputs)
        val output = result.get(0).value as Array<FloatArray>

        inputTensor.close()
        speakerTensor.close()
        speedTensor.close()
        result.close()

        return output[0]
    }

    /**
     * 文本转音素ID（简化版G2P）
     * 实际模型需要对应的音素表，这里提供框架
     */
    private fun textToPhonemes(text: String): IntArray {
        // TODO: 根据模型的音素表实现完整的G2P
        // 简化：每个汉字映射为一个ID，标点映射为特殊ID
        val ids = mutableListOf<Int>()
        for (ch in text) {
            when {
                ch.isChineseChar() -> ids.add(ch.code % 1000 + 10) // 简化映射
                ch in "，。！？、；：" -> ids.add(1) // 标点
                ch == ' ' -> ids.add(2) // 空格
                else -> ids.add(3) // 其他
            }
        }
        return ids.toIntArray()
    }

    private fun Char.isChineseChar(): Boolean {
        return this.code in 0x4E00..0x9FFF
    }

    fun release() {
        session.close()
        env.close()
    }
}

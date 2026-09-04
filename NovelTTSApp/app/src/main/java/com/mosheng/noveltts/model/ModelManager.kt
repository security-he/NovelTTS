package com.mosheng.noveltts.model

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * 模型管理器
 * 负责：检查模型、自动下载、初始化ONNX推理引擎
 * APP首次启动自动完成，用户无需手动配置
 */
object ModelManager {

    // 模型文件信息
    private const val MODEL_FILENAME = "vits_zh.onnx"
    private const val CONFIG_FILENAME = "config.json"

    // 模型下载地址（可替换为自己的CDN）
    private val MODEL_URLS = listOf(
        "https://huggingface.co/spaces/PlayVoice/vits_chinese/resolve/main/model.onnx",
        "https://modelscope.cn/models/PlayVoice/vits_chinese/resolve/master/model.onnx"
    )

    private var engine: OnnxTTS? = null
    private var isReady = false

    /**
     * 模型状态
     */
    enum class ModelState {
        NOT_DOWNLOADED,
        DOWNLOADING,
        DOWNLOAD_FAILED,
        READY
    }

    data class DownloadProgress(
        val state: ModelState,
        val progress: Int = 0,
        val message: String = ""
    )

    /**
     * 获取模型目录
     */
    private fun getModelDir(context: Context): File {
        val dir = File(context.filesDir, "models")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * 获取模型文件路径
     */
    fun getModelPath(context: Context): String {
        return File(getModelDir(context), MODEL_FILENAME).absolutePath
    }

    /**
     * 检查模型是否已下载
     */
    fun isModelDownloaded(context: Context): Boolean {
        val modelFile = File(getModelDir(context), MODEL_FILENAME)
        return modelFile.exists() && modelFile.length() > 1024 * 1024 // 大于1MB才算有效
    }

    /**
     * 检查模型是否就绪
     */
    fun isModelReady(): Boolean = isReady

    /**
     * 获取推理引擎
     */
    fun getEngine(): OnnxTTS? = engine

    /**
     * 下载模型（带进度回调）
     */
    suspend fun downloadModel(
        context: Context,
        onProgress: (DownloadProgress) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val modelFile = File(getModelDir(context), MODEL_FILENAME)
        val tempFile = File(getModelDir(context), "$MODEL_FILENAME.tmp")

        for ((index, urlStr) in MODEL_URLS.withIndex()) {
            try {
                onProgress(DownloadProgress(ModelState.DOWNLOADING, 0, "正在从镜像${index + 1}下载模型..."))

                val url = URL(urlStr)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                connection.instanceFollowRedirects = true
                connection.connect()

                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    onProgress(DownloadProgress(ModelState.DOWNLOAD_FAILED, 0, "镜像${index + 1}返回错误: $responseCode"))
                    connection.disconnect()
                    continue
                }

                val totalSize = connection.contentLength
                val inputStream = connection.inputStream
                val outputStream = FileOutputStream(tempFile)

                val buffer = ByteArray(8192)
                var downloaded = 0
                var read: Int
                var lastProgress = -1

                while (inputStream.read(buffer).also { read = it } != -1) {
                    outputStream.write(buffer, 0, read)
                    downloaded += read
                    if (totalSize > 0) {
                        val progress = (downloaded * 100 / totalSize)
                        if (progress != lastProgress) {
                            lastProgress = progress
                            onProgress(DownloadProgress(ModelState.DOWNLOADING, progress, "已下载 ${downloaded / 1024 / 1024}MB / ${totalSize / 1024 / 1024}MB"))
                        }
                    }
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()
                connection.disconnect()

                // 重命名临时文件
                if (tempFile.exists()) {
                    if (modelFile.exists()) modelFile.delete()
                    tempFile.renameTo(modelFile)
                }

                onProgress(DownloadProgress(ModelState.READY, 100, "模型下载完成"))
                return@withContext true

            } catch (e: Exception) {
                e.printStackTrace()
                onProgress(DownloadProgress(ModelState.DOWNLOAD_FAILED, 0, "镜像${index + 1}下载失败: ${e.message}"))
                if (tempFile.exists()) tempFile.delete()
            }
        }
        false
    }

    /**
     * 初始化模型（加载到ONNX Runtime）
     */
    suspend fun initModel(context: Context): Boolean = withContext(Dispatchers.IO) {
        if (isReady) return@withContext true

        val modelPath = getModelPath(context)
        if (!File(modelPath).exists()) {
            return@withContext false
        }

        try {
            engine = OnnxTTS(modelPath)
            isReady = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 一键准备模型（下载+初始化）
     * APP启动时调用，自动完成所有配置
     */
    suspend fun prepareModel(
        context: Context,
        onProgress: (DownloadProgress) -> Unit
    ): Boolean {
        if (!isModelDownloaded(context)) {
            val success = downloadModel(context, onProgress)
            if (!success) return false
        } else {
            onProgress(DownloadProgress(ModelState.READY, 100, "模型已存在，正在加载..."))
        }
        return initModel(context)
    }
}

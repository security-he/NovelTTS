package com.mosheng.noveltts

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.mosheng.noveltts.data.*
import com.mosheng.noveltts.model.ModelManager
import com.mosheng.noveltts.parser.NovelParser
import com.mosheng.noveltts.tts.TTSEngine
import com.mosheng.noveltts.ui.ModelSetupScreen
import com.mosheng.noveltts.ui.ReaderScreen
import com.mosheng.noveltts.ui.VoiceSettingsScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {

    private lateinit var ttsEngine: TTSEngine

    // 状态
    private var chapters by mutableStateOf<List<Chapter>>(emptyList())
    private var characters by mutableStateOf<Map<String, Character>>(emptyMap())
    private var currentChapterIndex by mutableStateOf(0)
    private var currentBlockIndex by mutableStateOf(0)
    private var isPlaying by mutableStateOf(false)
    private var isPaused by mutableStateOf(false)
    private var showVoiceSettings by mutableStateOf(false)
    private var modelProgress by mutableStateOf(
        ModelManager.DownloadProgress(ModelManager.ModelState.NOT_DOWNLOADED)
    )
    private var modelReady by mutableStateOf(false)

    // 文件选择器
    private val openFileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                loadNovel(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ttsEngine = TTSEngine(this)
        ttsEngine.initAudio()

        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Color(0xFFFFB74D),
                background = Color(0xFF16213E),
                surface = Color(0xFF1A1A2E)
            )) {
                AppContent()
            }
        }

        // 启动时自动准备模型
        prepareModel()
    }

    private fun prepareModel() {
        CoroutineScope(Dispatchers.Main).launch {
            val success = ModelManager.prepareModel(this@MainActivity) { progress ->
                modelProgress = progress
            }
            modelReady = success
            if (success) {
                modelProgress = ModelManager.DownloadProgress(
                    ModelManager.ModelState.READY, 100, "就绪"
                )
            }
        }
    }

    @Composable
    fun AppContent() {
        if (!modelReady) {
            ModelSetupScreen(
                progress = modelProgress,
                onRetry = { prepareModel() }
            )
        } else if (showVoiceSettings) {
            VoiceSettingsScreen(
                characters = characters,
                onGenderChange = { name, gender ->
                    characters = characters.toMutableMap().apply {
                        this[name]?.gender = gender
                    }
                },
                onTestVoice = { /* 试听逻辑 */ },
                onBack = { showVoiceSettings = false }
            )
        } else {
            ReaderScreen(
                chapters = chapters,
                characters = characters,
                currentChapterIndex = currentChapterIndex,
                currentBlockIndex = currentBlockIndex,
                isPlaying = isPlaying,
                isPaused = isPaused,
                onChapterSelect = { index ->
                    stopPlayback()
                    currentChapterIndex = index
                    currentBlockIndex = 0
                },
                onPlay = { startPlayback() },
                onPause = { pausePlayback() },
                onStop = { stopPlayback() },
                onPrevChapter = {
                    if (currentChapterIndex > 0) {
                        stopPlayback()
                        currentChapterIndex--
                        currentBlockIndex = 0
                    }
                },
                onNextChapter = {
                    if (currentChapterIndex < chapters.size - 1) {
                        stopPlayback()
                        currentChapterIndex++
                        currentBlockIndex = 0
                    }
                },
                onOpenVoiceSettings = { showVoiceSettings = true },
                onOpenFile = { openFilePicker() }
            )
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/plain", "application/octet-stream"))
        }
        openFileLauncher.launch(intent)
    }

    private fun loadNovel(uri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                contentResolver.openInputStream(uri)?.use { input ->
                    val reader = BufferedReader(InputStreamReader(input, "UTF-8"))
                    val text = reader.readText()
                    reader.close()

                    val (parsedChapters, parsedCharacters) = NovelParser.parseNovel(text)

                    withContext(Dispatchers.Main) {
                        chapters = parsedChapters
                        characters = parsedCharacters
                        currentChapterIndex = 0
                        currentBlockIndex = 0
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startPlayback() {
        if (chapters.isEmpty()) return
        val chapter = chapters[currentChapterIndex]
        if (chapter.blocks.isEmpty()) return

        isPlaying = true
        isPaused = false

        ttsEngine.onSegmentStart = { blockIndex, _ ->
            currentBlockIndex = blockIndex
        }
        ttsEngine.onFinish = {
            isPlaying = false
            isPaused = false
            // 自动下一章
            if (currentChapterIndex < chapters.size - 1) {
                currentChapterIndex++
                currentBlockIndex = 0
                startPlayback()
            }
        }

        ttsEngine.playBlocks(chapter.blocks, currentBlockIndex)
    }

    private fun pausePlayback() {
        isPaused = true
        ttsEngine.pause()
    }

    private fun stopPlayback() {
        isPlaying = false
        isPaused = false
        ttsEngine.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsEngine.release()
    }
}

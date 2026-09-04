package com.mosheng.noveltts.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosheng.noveltts.data.*
import kotlinx.coroutines.launch

/**
 * 主阅读界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    chapters: List<Chapter>,
    characters: Map<String, Character>,
    currentChapterIndex: Int,
    currentBlockIndex: Int,
    isPlaying: Boolean,
    isPaused: Boolean,
    onChapterSelect: (Int) -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onPrevChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onOpenVoiceSettings: () -> Unit,
    onOpenFile: () -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(currentChapterIndex) {
        listState.scrollToItem(0)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ChapterDrawer(
                chapters = chapters,
                currentIndex = currentChapterIndex,
                onSelect = { index ->
                    onChapterSelect(index)
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            chapters.getOrNull(currentChapterIndex)?.title ?: "墨声朗读器",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, "章节列表", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = onOpenFile) {
                            Icon(Icons.Default.FolderOpen, "打开小说", tint = Color.White)
                        }
                        IconButton(onClick = onOpenVoiceSettings) {
                            Icon(Icons.Default.Settings, "音色设置", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF1A1A2E)
                    )
                )
            },
            bottomBar = {
                PlaybackBar(
                    isPlaying = isPlaying,
                    isPaused = isPaused,
                    onPlay = onPlay,
                    onPause = onPause,
                    onStop = onStop,
                    onPrev = onPrevChapter,
                    onNext = onNextChapter
                )
            },
            containerColor = Color(0xFF16213E)
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                if (chapters.isEmpty()) {
                    EmptyState(onOpenFile = onOpenFile)
                } else {
                    ReadingContent(
                        chapter = chapters[currentChapterIndex],
                        currentBlockIndex = currentBlockIndex,
                        listState = listState
                    )
                }
            }
        }
    }
}

/**
 * 章节抽屉
 */
@Composable
fun ChapterDrawer(
    chapters: List<Chapter>,
    currentIndex: Int,
    onSelect: (Int) -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = Color(0xFF1A1A2E)
    ) {
        Text(
            "章节目录",
            color = Color(0xFFFFB74D),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(20.dp)
        )
        LazyColumn {
            items(chapters.size) { index ->
                val chapter = chapters[index]
                val isCurrent = index == currentIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(index) }
                        .background(
                            if (isCurrent) Color(0xFF2A2A4E) else Color.Transparent
                        )
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        chapter.title,
                        color = if (isCurrent) Color(0xFFFFB74D) else Color(0xFFCCCCCC),
                        fontSize = 14.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                    )
                }
                HorizontalDivider(color = Color(0xFF2A2A4E), thickness = 0.5.dp)
            }
        }
    }
}

/**
 * 阅读内容区
 */
@Composable
fun ReadingContent(
    chapter: Chapter,
    currentBlockIndex: Int,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        items(chapter.blocks.size) { index ->
            val block = chapter.blocks[index]
            val isCurrent = index == currentBlockIndex

            when (block.type) {
                BlockType.NARRATOR -> {
                    Text(
                        text = block.text,
                        color = if (isCurrent) Color(0xFFFFE0B2) else Color(0xFFE0E0E0),
                        fontSize = 17.sp,
                        lineHeight = 30.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .background(
                                if (isCurrent) Color(0x1AFFB74D) else Color.Transparent,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(4.dp)
                    )
                }
                BlockType.DIALOGUE -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .background(
                                if (isCurrent) Color(0x1AFFB74D) else Color(0x0DFFFFFF),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = block.speaker ?: "未知",
                                color = if (block.gender == Gender.FEMALE) Color(0xFFF48FB1) else Color(0xFF90CAF9),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = block.gender?.display() ?: "",
                                color = Color(0xFF888888),
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "\"${block.text}\"",
                            color = if (isCurrent) Color(0xFFFFE0B2) else Color(0xFFF0F0F0),
                            fontSize = 17.sp,
                            lineHeight = 28.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * 播放控制栏
 */
@Composable
fun PlaybackBar(
    isPlaying: Boolean,
    isPaused: Boolean,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Surface(
        color = Color(0xFF1A1A2E),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrev) {
                Icon(Icons.Default.SkipPrevious, "上一章", tint = Color(0xFFCCCCCC))
            }
            IconButton(onClick = onStop) {
                Icon(Icons.Default.Stop, "停止", tint = Color(0xFFCCCCCC))
            }
            FloatingActionButton(
                onClick = { if (isPlaying && !isPaused) onPause() else onPlay() },
                containerColor = Color(0xFFFFB74D),
                contentColor = Color(0xFF1A1A2E),
                modifier = Modifier.size(52.dp)
            ) {
                Icon(
                    if (isPlaying && !isPaused) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "播放/暂停",
                    modifier = Modifier.size(28.dp)
                )
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Default.SkipNext, "下一章", tint = Color(0xFFCCCCCC))
            }
        }
    }
}

/**
 * 空状态
 */
@Composable
fun EmptyState(onOpenFile: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.MenuBook,
                contentDescription = null,
                tint = Color(0xFF555577),
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "还没有打开小说",
                color = Color(0xFF888888),
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onOpenFile,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFB74D),
                    contentColor = Color(0xFF1A1A2E)
                )
            ) {
                Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("选择小说文件", fontWeight = FontWeight.Bold)
            }
        }
    }
}

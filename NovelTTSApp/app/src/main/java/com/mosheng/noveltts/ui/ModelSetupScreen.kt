package com.mosheng.noveltts.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosheng.noveltts.model.ModelManager

/**
 * 模型准备页面（首次启动显示）
 * 自动下载模型并初始化，用户无需操作
 */
@Composable
fun ModelSetupScreen(
    progress: ModelManager.DownloadProgress,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "墨声朗读器",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFB74D)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "多角色 · 离线 · 自然朗读",
                fontSize = 14.sp,
                color = Color(0xFFB0B0B0)
            )
            Spacer(modifier = Modifier.height(48.dp))

            when (progress.state) {
                ModelManager.ModelState.DOWNLOADING -> {
                    CircularProgressIndicator(
                        color = Color(0xFFFFB74D),
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = progress.message,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { progress.progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = Color(0xFFFFB74D),
                        trackColor = Color(0xFF333355)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${progress.progress}%",
                        color = Color(0xFFB0B0B0),
                        fontSize = 12.sp
                    )
                }
                ModelManager.ModelState.DOWNLOAD_FAILED -> {
                    Text(
                        text = "模型下载失败",
                        color = Color(0xFFEF5350),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = progress.message,
                        color = Color(0xFFB0B0B0),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFB74D),
                            contentColor = Color(0xFF1A1A2E)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("重试下载", fontWeight = FontWeight.Bold)
                    }
                }
                ModelManager.ModelState.READY -> {
                    CircularProgressIndicator(
                        color = Color(0xFFFFB74D),
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "正在加载模型...",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
                else -> {
                    CircularProgressIndicator(color = Color(0xFFFFB74D))
                }
            }
        }
    }
}

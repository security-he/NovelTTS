package com.mosheng.noveltts.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosheng.noveltts.data.Character
import com.mosheng.noveltts.data.Gender

/**
 * 音色设置界面
 * 查看识别到的角色、调整性别、试听音色
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSettingsScreen(
    characters: Map<String, Character>,
    onGenderChange: (String, Gender) -> Unit,
    onTestVoice: (Gender) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("角色与音色", color = Color.White, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A2E)
                )
            )
        },
        containerColor = Color(0xFF16213E)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // 音色试听区
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2B47)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "基础音色试听",
                        color = Color(0xFFFFB74D),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        VoiceTestButton("男声", Color(0xFF90CAF9)) { onTestVoice(Gender.MALE) }
                        VoiceTestButton("女声", Color(0xFFF48FB1)) { onTestVoice(Gender.FEMALE) }
                        VoiceTestButton("旁白", Color(0xFFFFB74D)) { onTestVoice(Gender.MALE) }
                    }
                }
            }

            // 角色列表
            Text(
                "识别到的角色（${characters.size}）",
                color = Color(0xFFCCCCCC),
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (characters.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("打开小说后自动识别角色", color = Color(0xFF666688))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(characters.values.toList().sortedByDescending { it.lineCount }) { char ->
                        CharacterItem(
                            character = char,
                            onGenderChange = { onGenderChange(char.name, it) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceTestButton(label: String, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FloatingActionButton(
            onClick = onClick,
            containerColor = color,
            contentColor = Color(0xFF1A1A2E),
            modifier = Modifier.size(48.dp)
        ) {
            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, color = Color(0xFFCCCCCC), fontSize = 12.sp)
    }
}

@Composable
fun CharacterItem(
    character: Character,
    onGenderChange: (Gender) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2B47)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 角色名
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    character.name,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "${character.lineCount} 句对话",
                    color = Color(0xFF888888),
                    fontSize = 12.sp
                )
            }

            // 性别切换
            Row(
                modifier = Modifier
                    .background(Color(0xFF2A3A5C), RoundedCornerShape(6.dp))
                    .padding(2.dp)
            ) {
                GenderChip("男", character.gender == Gender.MALE, Color(0xFF90CAF9)) {
                    onGenderChange(Gender.MALE)
                }
                GenderChip("女", character.gender == Gender.FEMALE, Color(0xFFF48FB1)) {
                    onGenderChange(Gender.FEMALE)
                }
            }
        }
    }
}

@Composable
fun GenderChip(
    label: String,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(
                if (selected) selectedColor else Color.Transparent,
                RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            color = if (selected) Color(0xFF1A1A2E) else Color(0xFFAAAAAA),
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

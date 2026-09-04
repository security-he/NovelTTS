package com.mosheng.noveltts.data

/**
 * 朗读块类型
 */
enum class BlockType {
    NARRATOR,  // 旁白
    DIALOGUE   // 对话
}

/**
 * 性别
 */
enum class Gender {
    MALE, FEMALE;

    fun display(): String = if (this == MALE) "男" else "女"
}

/**
 * 一个朗读块
 */
data class TextBlock(
    val type: BlockType,
    val text: String,
    val speaker: String? = null,  // 角色名（仅对话有）
    val gender: Gender? = null     // 性别（仅对话有）
)

/**
 * 章节
 */
data class Chapter(
    val title: String,
    val blocks: List<TextBlock>
)

/**
 * 角色信息
 */
data class Character(
    val name: String,
    var gender: Gender,
    var lineCount: Int = 0
)

/**
 * 音色配置
 */
data class VoiceConfig(
    var modelPath: String = "",
    var speakerId: Int = 0,
    var speed: Float = 1.0f,
    var pitch: Float = 1.0f
)

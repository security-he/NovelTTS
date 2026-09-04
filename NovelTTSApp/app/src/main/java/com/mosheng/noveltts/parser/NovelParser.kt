package com.mosheng.noveltts.parser

import com.mosheng.noveltts.data.*

/**
 * 小说解析器
 * 核心逻辑：
 * 1. 全文扫描发现角色名（从冒号前提取句首纯人名）
 * 2. 按章节分割
 * 3. 引号内对话 → 匹配角色名 → 按性别分配语音；引号外 → 旁白
 */
object NovelParser {

    // 说话动词（明确表示说话的词）
    private val SAY_VERBS = listOf(
        "说", "道", "喊", "叫", "问", "答", "言", "语", "开口", "开口道",
        "喝道", "怒道", "喜道", "惊道", "奇道", "疑惑道", "无奈道", "郑重道",
        "缓缓道", "淡淡道", "低声道", "高声道", "轻声道", "大声道", "小声道",
        "附和道", "插嘴道", "打断道", "继续道", "接着道", "又道", "再道",
        "回道", "应道", "答道", "笑道", "哭道", "骂道", "哼道", "啐道",
        "冷笑道", "苦笑道", "讪笑道", "解释道", "安慰道", "催促道", "威胁道",
        "嘟囔道", "嘀咕道", "念叨道", "调侃道", "戏谑道", "森然道", "幽幽道", "喃喃道",
        "说道", "问道", "喊道", "叫道", "叹道"
    ).sortedByDescending { it.length }

    private val SAY_VERBS_PATTERN = SAY_VERBS.joinToString("|") { Regex.escape(it) }

    // 人称代词
    private val PRONOUNS = listOf("你们", "我们", "他们", "她们", "咱们", "俺们", "你", "我", "他", "她", "咱", "俺")
        .sortedByDescending { it.length }

    // 女性名字特征字
    private val FEMALE_CHARS = setOf(
        '雪','婧','言','琴','雯','芳','丽','娜','婷','琳','敏','静','娟','媛','嫣','瑶',
        '萱','琪','薇','柔','兰','菊','梅','竹','莲','萍','霞','燕','莺','姬','娘','姐',
        '妹','姨','姑','婆','母','女','妻','媳','婉','娴','慧','颖','妍','妙','娇','娆','娥','媪'
    )

    // 人名常用字（用于判断3字名）
    private val NAME_CHARS = setOf(
        '婧','婷','雪','芳','娜','媛','嫣','瑶','萱','琪','薇','柔','娴','慧','颖','妍','妙',
        '娇','娆','婉','浩','然','宇','博','轩','杰','涛','明','超','鹏','华','飞','鑫','伟',
        '强','磊','军','洋','勇','言','文','君','林','枫','龙','虎','天','云','海','山','石','建','国'
    )

    // 停用词（非人名词）
    private val STOP_WORDS = setOf(
        "沉吟","自嘲","摇摇","脑袋","轻轻","摊摊","手臂","片刻","此时","此刻",
        "知道","觉得","感觉","看到","听到","想到","明白","理解","记得","忘记",
        "今天","明天","昨天","现在","以前","以后","时候","地方","东西","事情",
        "老师","同学","朋友","家人","孩子","男人","女人","老人","小孩","大家",
        "众人","旁人","有人","没人","每个人","所有人",
        "什么","怎么","为什么","怎么了","哪里","哪个","多么","这么","那么",
        "说道","笑道","问道","喊道","叫道","怒道","喜道","惊道","奇道",
        "一个","两个","这个","那个","一下","一起","一样","一直","一定",
        "因为","所以","但是","可是","然后","于是","如果","虽然","不过",
        "自己","对方","别人","人家","彼此"
    )

    // 不合理人名字符
    private val BAD_NAME_CHARS = setOf(
        '很','是','最','都','也','就','还','又','再','只','没','不','的','了','着',
        '过','我','你','他','她','它','们','这','那','与','及','和','或'
    )

    private val CHAPTER_PATTERN = Regex(
        "(?:^|\\n)\\s*(第[\\s]*[零一二三四五六七八九十百千万\\d]+[\\s]*[章节回卷部篇][^\\n]*)" +
        "|(?:^|\\n)\\s*(楔子|序言|序章|引子|尾声|后记|番外[^\\n]*)",
        RegexOption.MULTILINE
    )

    private val DIALOGUE_PATTERN = Regex("[「\"“]([^「””」]+?)[」””]")

    /**
     * 判断性别
     */
    fun guessGender(name: String): Gender {
        if (name.isEmpty()) return Gender.MALE
        if (name == "她" || name == "她们") return Gender.FEMALE
        if (name == "他" || name == "他们") return Gender.MALE
        for (ch in name) {
            if (ch in FEMALE_CHARS) return Gender.FEMALE
        }
        return Gender.MALE
    }

    /**
     * 判断候选词是否可能是人名
     */
    private fun isPlausibleName(name: String): Boolean {
        if (name in STOP_WORDS) return false
        if (name in SAY_VERBS) return false
        if (name.last() in setOf('了','着','过','的','地','得')) return false
        if (name.length == 2) {
            if (name.any { it in BAD_NAME_CHARS }) return false
        }
        return true
    }

    /**
     * 从文本开头提取纯人名
     * 例如："任昊自嘲地摇了摇脑袋" -> "任昊"
     *      "谢知婧绝美的脸颊" -> "谢知婧"
     *      "沉吟了片刻" -> null
     */
    private fun extractName(snippet: String): String? {
        var s = snippet.trimStart('，','。','；','、','！','？',' ','\t','\n')
        if (s.length < 2) return null

        // 2字候选
        val cand2 = s.take(2)
        if (cand2 in STOP_WORDS || cand2 in SAY_VERBS || cand2.last() in setOf('了','着','过','的','地','得')) {
            return null
        }

        // 检查是否可能是3字名
        if (s.length >= 3) {
            val third = s[2]
            if (third in NAME_CHARS) {
                val cand3 = s.take(3)
                if (cand3 !in STOP_WORDS && cand3 !in SAY_VERBS && cand3.last() !in setOf('了','着','过','的','地','得')) {
                    return cand3
                }
            }
        }
        return cand2
    }

    /**
     * 全文扫描发现角色名集合
     */
    private fun discoverCharacters(text: String, minOccurrences: Int = 1): Set<String> {
        val names = mutableListOf<String>()

        // 模式1：冒号 + 引号，从冒号前提取句首人名
        val colonDialogue = Regex("[：:]\\s*[「\"“]")
        for (m in colonDialogue.findAll(text)) {
            val colonPos = m.range.first
            val prefix = text.substring(maxOf(0, colonPos - 30), colonPos)
            // 找句子开头
            var sentenceStart = 0
            for (sep in listOf("。", "！", "？", "\n", "；")) {
                val idx = prefix.lastIndexOf(sep)
                if (idx >= 0) sentenceStart = maxOf(sentenceStart, idx + 1)
            }
            val sentence = prefix.substring(sentenceStart)
            val name = extractName(sentence)
            if (name != null) names.add(name)
        }

        // 模式2：引号 + 人名 + 说话动词
        val pattern2 = Regex("[」””]\\s*([\\u4e00-\\u9fa5]{2,4})\\s*(?:$SAY_VERBS_PATTERN)")
        for (m in pattern2.findAll(text)) {
            val name = m.groupValues[1]
            if (isPlausibleName(name)) names.add(name)
        }

        // 模式3：句首/标点后 + 人名 + 说话动词
        val pattern3 = Regex("(?:^|[，。；、\\n])\\s*([\\u4e00-\\u9fa5]{2,4})\\s*(?:$SAY_VERBS_PATTERN)", RegexOption.MULTILINE)
        for (m in pattern3.findAll(text)) {
            val name = m.groupValues[1]
            if (isPlausibleName(name)) names.add(name)
        }

        // 统计频率
        val counter = names.groupingBy { it }.eachCount()

        // 过滤：不合理的词 + 3字名优先于2字前缀
        val threeCharNames = mutableSetOf<String>()
        for ((name, count) in counter) {
            if (count >= minOccurrences && isPlausibleName(name) && name.length == 3) {
                threeCharNames.add(name)
            }
        }

        val result = mutableSetOf<String>()
        for ((name, count) in counter) {
            if (count >= minOccurrences && isPlausibleName(name)) {
                if (name.length == 2 && threeCharNames.any { it.startsWith(name) }) continue
                result.add(name)
            }
        }
        return result
    }

    /**
     * 分割章节
     */
    private fun splitChapters(text: String): List<Pair<String, String>> {
        val chapters = mutableListOf<Pair<String, String>>()
        var lastIdx = 0
        for (m in CHAPTER_PATTERN.findAll(text)) {
            if (m.range.first > lastIdx && chapters.isNotEmpty()) {
                val last = chapters.removeAt(chapters.size - 1)
                chapters.add(last.first to last.second + "\n" + text.substring(lastIdx, m.range.first).trim())
            }
            val title = (m.groupValues[1].ifEmpty { m.groupValues[2] }).trim()
            chapters.add(title to "")
            lastIdx = m.range.last + 1
        }
        if (lastIdx < text.length) {
            val rest = text.substring(lastIdx).trim()
            if (rest.isNotEmpty()) {
                if (chapters.isNotEmpty()) {
                    val last = chapters.removeAt(chapters.size - 1)
                    chapters.add(last.first to last.second + "\n" + rest)
                } else {
                    chapters.add("正文" to rest)
                }
            }
        }
        if (chapters.isEmpty()) chapters.add("正文" to text.trim())
        return chapters.mapIndexed { i, (t, c) -> (t.ifEmpty { "第${i+1}章" }) to c }
    }

    /**
     * 在对话前文本中找说话人
     * 优先级：句首人名 > 精确匹配(最靠前) > 人称代词 > fallback
     */
    private fun findSpeaker(before: String, characterSet: Set<String>, fallback: String?): String? {
        if (before.isEmpty()) return fallback

        val context = if (before.length > 25) before.substring(before.length - 25) else before

        // 1. 从句首提取人名
        val name = extractName(context)
        if (name != null && name in characterSet) return name

        // 2. 精确匹配，取最靠前的
        var bestName: String? = null
        var bestPos = context.length
        for (n in characterSet) {
            val pos = context.indexOf(n)
            if (pos in 0 until bestPos) {
                bestPos = pos
                bestName = n
            }
        }
        if (bestName != null) return bestName

        // 3. 人称代词
        for (pro in PRONOUNS) {
            if (pro in context) return pro
        }

        return fallback
    }

    /**
     * 解析一个段落
     */
    private fun parseParagraph(
        para: String,
        characterSet: Set<String>,
        lastSpeaker: String?
    ): Pair<List<TextBlock>, String?> {
        val blocks = mutableListOf<TextBlock>()
        var lastEnd = 0
        var currentSpeaker = lastSpeaker

        for (m in DIALOGUE_PATTERN.findAll(para)) {
            val dialogueText = m.groupValues[1].trim()
            val before = para.substring(lastEnd, m.range.first).trim()

            if (before.isNotEmpty()) {
                blocks.add(TextBlock(BlockType.NARRATOR, before))
            }

            val speaker = findSpeaker(before, characterSet, currentSpeaker)
            if (speaker != null) currentSpeaker = speaker

            val gender = if (speaker != null) guessGender(speaker) else null
            blocks.add(TextBlock(BlockType.DIALOGUE, dialogueText, speaker, gender))
            lastEnd = m.range.last + 1
        }

        if (lastEnd < para.length) {
            val rest = para.substring(lastEnd).trim()
            if (rest.isNotEmpty()) {
                blocks.add(TextBlock(BlockType.NARRATOR, rest))
            }
        }

        if (blocks.isEmpty()) {
            blocks.add(TextBlock(BlockType.NARRATOR, para.trim()))
        }

        return blocks to currentSpeaker
    }

    /**
     * 解析整部小说
     * 返回 (章节列表, 角色字典)
     */
    fun parseNovel(rawText: String): Pair<List<Chapter>, Map<String, Character>> {
        val text = rawText.replace("\r\n", "\n").replace("\r", "\n")

        // 第一步：全文扫描发现角色名
        val characterSet = discoverCharacters(text, minOccurrences = 1)

        // 第二步：分割章节
        val rawChapters = splitChapters(text)

        // 第三步：逐章逐段解析
        val chapters = mutableListOf<Chapter>()
        val characters = mutableMapOf<String, Character>()
        var lastSpeaker: String? = null

        for ((title, content) in rawChapters) {
            val blocks = mutableListOf<TextBlock>()
            val paragraphs = content.split("\n{2,}".toRegex()).map { it.trim() }.filter { it.isNotEmpty() }
            for (para in paragraphs) {
                for (line in para.split("\n")) {
                    val trimmed = line.trim()
                    if (trimmed.isEmpty()) continue
                    val (parsedBlocks, speaker) = parseParagraph(trimmed, characterSet, lastSpeaker)
                    lastSpeaker = speaker
                    blocks.addAll(parsedBlocks)

                    for (b in parsedBlocks) {
                        if (b.type == BlockType.DIALOGUE && b.speaker != null && b.speaker in characterSet) {
                            val char = characters.getOrPut(b.speaker) {
                                Character(b.speaker, guessGender(b.speaker))
                            }
                            char.lineCount++
                        }
                    }
                }
            }
            chapters.add(Chapter(title, blocks))
        }

        return chapters to characters
    }
}

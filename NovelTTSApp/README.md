# 墨声朗读器 · 多角色离线小说朗读APP

完全离线的 Android 小说多角色朗读APP。内置 TTS 模型，无需联网，支持自动识别小说角色、按性别分配男女音色、引号内对话用角色音、引号外用旁白音。

## 特性

- **完全离线**：模型下载后无需联网，不耗流量
- **多角色朗读**：自动识别小说中的角色名，男声/女声/旁白三种音色
- **智能角色匹配**：只提取纯人名（如"任昊"），不会把"任昊讪讪一笑"当成人名
- **对话/旁白分离**：引号内对话用角色音，引号外叙述用旁白音
- **自动配置**：首次启动自动下载模型、自动初始化，打开即用
- **章节导航**：自动识别章节，支持目录跳转、上下章切换
- **自然朗读**：按标点断句，句间停顿，避免机械感

## 快速开始（三种方式获取APK）

### 方式一：GitHub Actions 自动构建（推荐，最省事）

1. 把整个项目上传到 GitHub 仓库
2. 点击仓库顶部的 **Actions** 标签
3. 找到 **构建APK** 工作流，点击 **Run workflow**
4. 等待 3-5 分钟构建完成
5. 在工作流运行结果的 **Artifacts** 中下载 `墨声朗读器-debug.apk`
6. 把 APK 传到手机上点击安装

### 方式二：电脑一键构建（Linux/Mac）

```bash
# 1. 确保已安装 JDK 17
# Ubuntu: sudo apt install openjdk-17-jdk
# Mac: brew install openjdk@17

# 2. 进入项目目录，运行构建脚本
cd NovelTTSApp
chmod +x build_apk.sh
./build_apk.sh

# 3. 构建完成后，APK在:
# app/build/outputs/apk/debug/app-debug.apk
```

### 方式三：电脑一键构建（Windows）

1. 确保已安装 [JDK 17](https://adoptium.net/)
2. 双击 `build_apk.bat`
3. 等待构建完成，APK 在 `app\build\outputs\apk\debug\app-debug.apk`

### 方式四：Android Studio 构建

1. 用 Android Studio 打开项目
2. 等待 Gradle 同步完成
3. 菜单 **Build → Build Bundle(s) / APK(s) → Build APK(s)**
4. 构建完成后点击通知中的 **locate** 找到 APK

## 使用方法

1. 安装 APK 后打开APP
2. 首次启动会自动下载 TTS 模型（约 40-80MB，只需一次）
3. 下载完成后自动进入主界面
4. 点击右上角 **文件夹图标** 选择手机里的 TXT 小说文件
5. APP 自动解析章节、识别角色
6. 点击底部 **播放按钮** 开始朗读
7. 点击 **设置图标** 可查看识别到的角色、调整性别、试听音色

## 角色匹配规则

APP 严格按照以下规则匹配角色：

1. **只提取纯人名**：从冒号前的句首提取人名，如"任昊自嘲地摇了摇脑袋："→ 提取"任昊"
2. **动作描写不当人名**："讪讪一笑""沉吟片刻""摇了摇脑袋"等不会被识别
3. **引号内对话用角色音**：根据人名判断性别，男名用男声，女名用女声
4. **引号外用旁白音**：所有叙述、描写、心理活动统一用旁白音
5. **连续对话自动继承**：没有明确说话人的对话，继承上一个说话人

## 模型说明

默认使用 VITS 中文 TTS 模型（ONNX 格式），首次启动自动从以下镜像下载：
- HuggingFace: `PlayVoice/vits_chinese`
- ModelScope: `PlayVoice/vits_chinese`

### 替换为 ChatTTS（更高音质）

如果想要更好的音质，可以替换为 ChatTTS 量化模型：

1. 将 ChatTTS 模型转换为 ONNX 格式（int8 量化）
2. 把模型文件命名为 `vits_zh.onnx`
3. 放到手机的 `Android/data/com.mosheng.noveltts/files/models/` 目录
4. 重启APP即可

> 注意：ChatTTS 模型较大（80-120MB），在中低端手机上推理速度可能较慢。

## 技术栈

- **语言**: Kotlin
- **UI**: Jetpack Compose (Material 3)
- **推理框架**: ONNX Runtime Mobile
- **TTS模型**: VITS / ChatTTS (ONNX量化)
- **音频播放**: AudioTrack (低延迟流式播放)
- **最低系统**: Android 7.0 (API 24)

## 项目结构

```
NovelTTSApp/
├── app/src/main/java/com/mosheng/noveltts/
│   ├── MainActivity.kt          # 主入口，状态管理
│   ├── data/Models.kt           # 数据模型
│   ├── parser/NovelParser.kt    # 小说解析+角色识别（核心）
│   ├── tts/TTSEngine.kt         # TTS引擎+音频播放
│   ├── model/
│   │   ├── ModelManager.kt      # 模型自动下载+初始化
│   │   └── OnnxTTS.kt           # ONNX推理封装
│   └── ui/
│       ├── ModelSetupScreen.kt  # 首次启动模型下载页
│       ├── ReaderScreen.kt      # 阅读界面+播放控制
│       └── VoiceSettingsScreen.kt # 角色音色设置
├── build_apk.sh                 # Linux/Mac一键构建
├── build_apk.bat                # Windows一键构建
└── .github/workflows/build.yml  # GitHub Actions自动构建
```

## 常见问题

**Q: 首次启动下载模型很慢？**
A: 模型默认从 HuggingFace 下载，国内可能较慢。脚本会自动尝试 ModelScope 镜像。也可以手动把模型文件放到 `files/models/` 目录。

**Q: 角色识别不准怎么办？**
A: 在"角色与音色"设置页可以手动调整每个角色的性别。APP 会自动统计每个角色的对话数量，主要角色排在前面。

**Q: 朗读声音很机械？**
A: 默认 VITS 模型已经比系统语音自然很多。如果追求更好效果，可以替换为 ChatTTS 量化模型（见上方说明）。

**Q: 支持什么格式的小说？**
A: 目前支持 UTF-8 编码的 TXT 文件。其他格式（EPUB、PDF）请先转换为 TXT。

## 许可证

本项目代码采用 MIT 许可证。TTS 模型请遵循对应模型的许可证（VITS 为 MIT，ChatTTS 为 CC BY-NC 4.0 非商业）。

#!/bin/bash
# ============================================================
# 墨声朗读器 - 一键构建APK脚本
# 用法: ./build_apk.sh
# 自动下载Android SDK + Gradle，构建出可安装的APK
# ============================================================

set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_DIR"

echo "============================================"
echo "  墨声朗读器 APK 构建工具"
echo "============================================"
echo ""

# 1. 检查Java
if ! command -v java &> /dev/null; then
    echo "[错误] 未找到Java，请先安装 JDK 17"
    echo "  Ubuntu: sudo apt install openjdk-17-jdk"
    echo "  Mac: brew install openjdk@17"
    exit 1
fi
JAVA_VERSION=$(java -version 2>&1 | head -1 | grep -oP 'version "\K[0-9]+' || echo "0")
echo "[OK] Java 版本: $JAVA_VERSION"

# 2. 设置Android SDK目录
export ANDROID_HOME="$HOME/android-sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
mkdir -p "$ANDROID_HOME/cmdline-tools"

# 3. 下载Android SDK command-line tools（如果不存在）
if [ ! -d "$ANDROID_HOME/cmdline-tools/latest" ]; then
    echo "[下载] Android SDK Command-line Tools..."
    TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
    curl -L -o /tmp/cmdline-tools.zip "$TOOLS_URL"
    unzip -q /tmp/cmdline-tools.zip -d /tmp/cmdline-tools-extract
    mkdir -p "$ANDROID_HOME/cmdline-tools/latest"
    cp -r /tmp/cmdline-tools-extract/cmdline-tools/* "$ANDROID_HOME/cmdline-tools/latest/"
    rm -rf /tmp/cmdline-tools.zip /tmp/cmdline-tools-extract
    echo "[OK] SDK Command-line Tools 安装完成"
fi

export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

# 4. 接受许可并安装SDK组件
echo "[安装] Android SDK 组件..."
yes | sdkmanager --licenses > /dev/null 2>&1 || true
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0" > /dev/null 2>&1
echo "[OK] SDK 组件安装完成"

# 5. 下载Gradle wrapper（如果不存在）
if [ ! -f "gradlew" ]; then
    echo "[下载] Gradle Wrapper..."
    GRADLE_VERSION="8.5"
    curl -L -o /tmp/gradle.zip "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
    unzip -q /tmp/gradle.zip -d /tmp/
    /tmp/gradle-${GRADLE_VERSION}/bin/gradle wrapper --gradle-version "$GRADLE_VERSION"
    rm -rf /tmp/gradle.zip /tmp/gradle-${GRADLE_VERSION}
    echo "[OK] Gradle Wrapper 生成完成"
fi

# 6. 构建APK
echo ""
echo "[构建] 正在编译APK（首次需要下载依赖，请耐心等待）..."
echo ""
chmod +x gradlew
./gradlew assembleDebug --no-daemon

# 7. 输出结果
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
    echo ""
    echo "============================================"
    echo "  构建成功！"
    echo "============================================"
    echo "  APK路径: $PROJECT_DIR/$APK_PATH"
    echo "  APK大小: $APK_SIZE"
    echo ""
    echo "  安装到手机:"
    echo "  adb install $APK_PATH"
    echo "  或直接把APK文件传到手机上点击安装"
    echo "============================================"
else
    echo "[错误] 构建失败，未找到APK文件"
    exit 1
fi

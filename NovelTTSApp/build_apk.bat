@echo off
REM ============================================================
REM  墨声朗读器 - Windows一键构建APK脚本
REM  用法: 双击 build_apk.bat
REM ============================================================

chcp 65001 >nul
cd /d "%~dp0"

echo ============================================
echo   墨声朗读器 APK 构建工具 (Windows)
echo ============================================
echo.

REM 检查Java
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo [错误] 未找到Java，请先安装 JDK 17
    echo 下载地址: https://adoptium.net/
    pause
    exit /b 1
)

REM 检查Android SDK
if "%ANDROID_HOME%"=="" (
    echo [提示] 未设置ANDROID_HOME，将使用默认路径 %USERPROFILE%\android-sdk
    set ANDROID_HOME=%USERPROFILE%\android-sdk
    set ANDROID_SDK_ROOT=%USERPROFILE%\android-sdk
)

REM 生成Gradle Wrapper（如果不存在）
if not exist "gradlew" (
    echo [下载] Gradle Wrapper...
    where gradle >nul 2>nul
    if %errorlevel% equ 0 (
        gradle wrapper --gradle-version 8.5
    ) else (
        echo [错误] 未找到Gradle，请先安装Gradle或Android Studio
        echo 下载地址: https://gradle.org/install/
        pause
        exit /b 1
    )
)

echo.
echo [构建] 正在编译APK...
echo.
call gradlew.bat assembleDebug --no-daemon

if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo.
    echo ============================================
    echo   构建成功！
    echo ============================================
    echo   APK路径: %cd%\app\build\outputs\apk\debug\app-debug.apk
    echo.
    echo   把这个APK文件传到手机上点击安装即可
    echo ============================================
) else (
    echo.
    echo [错误] 构建失败
)
pause

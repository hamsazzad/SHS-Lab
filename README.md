# SHS Lab - Heaven for Android Coders 🚀

<p align="center">
  <img src="app/src/main/res/mipmap-xxhdpi/ic_launcher.png" width="120" alt="SHS Lab Icon"/>
</p>

**SHS Lab** is a powerful, all-in-one Android coding environment designed for developers who want a full IDE experience on their mobile device. Built with memory-efficiency as the top priority — fully optimized for 2GB RAM devices.

## ✨ Features

### 📁 Phase 1: File Manager (aaPanel Style)
- Browse internal & external storage
- Create folders and files on the fly
- Batch import multiple files at once
- Long-press to rename or delete any file/folder/ZIP
- Clean glassmorphism dark UI

### 🗜️ Phase 2: ZIP Engine
- Select & extract ZIP files ("Extract Here" or to a specific folder)
- Compress any file/folder into a new ZIP
- Chunk-based processing — no OOM crashes on low-end devices

### 💻 Phase 3: Integrated Code Editor (Acode Style)
- Powered by **CodeMirror 5** — fully offline
- Syntax highlighting for HTML, CSS, JS, PHP, JSON, XML, Python, Java, Kotlin & more
- Line numbers, bracket matching, active line highlight
- One-tap **SAVE** — overwrites instantly, no prompts
- Undo/Redo support

### 🌐 Phase 4: Live Preview
- Starts a local **NanoHTTPD** HTTP server (localhost:18080)
- Renders your HTML/CSS/JS project in a full WebView browser
- Directory listing for easy navigation
- Fallback to `file://` if server start fails

## 🛡️ Memory Optimization
- All file I/O runs on **background coroutines** — never blocks the UI
- ZIP extraction/creation uses **8KB chunk streaming**
- Glassmorphism uses **semi-transparent overlays** (no heavy blur libraries)
- Large files are **read in chunks** with truncation notice

## 📱 Compatibility
- **Minimum**: Android 4.4 KitKat (API 19)
- **Target**: Android 14 (API 34)
- **Architectures**: armeabi-v7a, arm64-v8a, x86, x86_64, universal

## 📦 Tech Stack
- **Language**: Kotlin
- **UI**: Material Components 3 + Custom Glassmorphism
- **Background**: Kotlin Coroutines
- **Editor**: CodeMirror 5 (bundled in assets)
- **HTTP Server**: NanoHTTPD 2.3.1
- **Build**: Gradle 8.2 + Android Gradle Plugin 8.2.2

## 🏗️ Build Instructions

```bash
# Clone the repo
git clone https://github.com/hamsazzad/SHS-Lab.git
cd SHS-Lab

# Build debug APK
./gradlew assembleDebug

# Build release APK (all ABIs)
./gradlew assembleRelease

# Build universal APK
./gradlew packageUniversalReleaseUniversalApk
```

## 📥 Download APKs
See [Releases](https://github.com/hamsazzad/SHS-Lab/releases) for pre-built APKs.

## 👨‍💻 Developer
- **GitHub**: [@hamsazzad](https://github.com/hamsazzad)
- **Email**: shobujkhan520@gmail.com

---
*Built with ❤️ for low-end Android devices*

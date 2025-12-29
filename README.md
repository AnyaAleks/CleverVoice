# CleverVoice
**A Privacy-First, Offline Voice Assistant for Android Smart Home Control Panels**

## Project Overview

CleverVoice is a fully local, privacy-preserving voice assistant designed for Android-based smart home systems. Unlike cloud-dependent assistants, CleverVoice processes all speech recognition and natural language understanding directly on the device using optimized AI models (Vosk for STT, TinyLlama for NLU), ensuring zero data leaves your environment.

### Key Features
*   **Complete Privacy & Offline Operation**: All audio processing happens locally; no internet connection or cloud servers required.
*   **Advanced Local AI Pipeline**: Integrates Vosk for speech-to-text and a quantized TinyLlama model for intent understanding via llama.cpp.
*   **Smart Home Ready**: Directly controls system functions like screen brightness, audio volume, and WiFi.
*   **Robust System Architecture**: Built with a multi-layer design (Android UI, JNI bridge, Native AI) for performance and maintainability.
*   **Comprehensive Testing Suite**: Includes dedicated modules for validating brightness, sound, and voice command processing.

## Project Structure

The project is organized into clear, modular packages:

```
CleverVoice/
├── app/src/main/
│   ├── java/pro/cleverlife/clevervoice/
│   │   ├── AI/                          # Core AI Integration Layer
│   │   │   ├── NativeLoader.java           # Loads native C++ libraries (Vosk, llama.cpp)
│   │   │   ├── TinyLLMProcessor.java       # Interface for the TinyLlama language model
│   │   │   └── VoskAIProcessor.java        # Interface for the Vosk speech recognition engine
│   │   │
│   │   ├── API/                         # System Control APIs
│   │   │   ├── BrightnessAPI.java          # Controls device screen brightness
│   │   │   ├── SoundAPI.java               # Manages audio volume and streams
│   │   │   └── WiFiAPI.java                # Handles WiFi connection state
│   │   │
│   │   ├── CleverLauncher/              # System Service & Management
│   │   │   ├── MainActivity.java           # Launcher's main UI and update manager
│   │   │   ├── BootCompleteReceiver.java   # Ensures auto-start on device boot
│   │   │   ├── AppCrashReceiver.java       # Monitors and restarts crashed services
│   │   │   └── AppInstallWatcher.java      # Tracks app installation events
│   │   │
│   │   ├── CleverServices/              # Background Services
│   │   │   ├── VoiceRecognitionService.java # Main service for audio capture & processing
│   │   │   ├── AudioRecordService.java     # Handles low-level audio recording
│   │   │   ├── BrightnessService.java      # Service wrapper for brightness API
│   │   │   ├── SoundService.java           # Service wrapper for sound API
│   │   │   ├── WiFiService.java            # Service wrapper for WiFi API
│   │   │   └── ServiceReceiver.java        # Manages communication with services
│   │   │
│   │   ├── processor/                   # Command Logic
│   │   │   ├── CommandParser.java          # Parses raw text into commands
│   │   │   └── CommandProcessor.java       # Executes parsed commands
│   │   │
│   │   ├── utils/                       # Utilities
│   │   │   ├── PermissionManager.java      # Centralized permission handling
│   │   │   ├── SettingsManager.java        # Manages app preferences and settings
│   │   │   └── FileUtils.java              # Helper for file operations
│   │   │
│   │   ├── SystemController.java           # Central coordinator for system actions
│   │   └── MainActivityCleverVoice.java    # Main UI of the voice assistant
│   │
│   └── res/                             # Android resources (layouts, strings, etc.)
│
└── app/src/androidTest/                 # Dedicated Test Modules
    ├── TestBrightnessActivity.java         # UI for testing brightness control
    └── TestSoundActivity.java              # UI for testing audio control
```

## Technology Stack

| Layer | Technology | Purpose |
| :--- | :--- | :--- |
| **Android Application** | Java, Android SDK | User interface, lifecycle management, system API calls. |
| **AI/Native Bridge** | Java Native Interface (JNI) | Communication between Java and high-performance C++ libraries. |
| **Speech Recognition** | [Vosk API](https://alphacephei.com/vosk/) (C++) | Lightweight, offline speech-to-text conversion. |
| **Language Understanding** | [TinyLlama](https://github.com/jzhang38/TinyLlama) + [llama.cpp](https://github.com/ggerganov/llama.cpp) | Quantized LLM for intent recognition and command parsing. |
| **System Control** | Android System APIs (Settings, AudioManager, WifiManager) | Execution of brightness, volume, and network commands. |
| **Model Format** | GGUF (via llama.cpp) | Efficient, quantized model format for mobile deployment. |

## Getting Started

### Prerequisites
*   **Android Device/Emulator**: Running Android 11 (API 30) or higher. **Android 11 (API 30)+ is recommended** for reliable background service execution.
*   **Development Environment**: [Android Studio](https://developer.android.com/studio) with SDK configured.
*   **AI Model Files**: Download and place the following in `app/src/main/assets/`:
    *   A Vosk model for your language (e.g., `vosk-model-small-ru-0.22`).
    *   A quantized TinyLlama model in GGUF format (e.g., `TinyLlama-1.1B-Chat-v1.0.Q4_K_M.gguf`).

### Installation & Build
1.  Clone the repository:
    ```bash
    git clone https://github.com/AnyaAleks/CleverVoice.git
    cd CleverVoice
    ```
2.  Open the project in Android Studio.
3.  Ensure the native C++ libraries for Vosk and llama.cpp are correctly built and linked (check `CMakeLists.txt` or `build.gradle` NDK configuration).
4.  Add the required AI model files to the `assets` folder.
5.  Build and run the project on your target device.

## How It Works

The assistant operates through a defined pipeline:

1.  **Wake Word Detection**: Listens for the keyword **"Клевер"** (Klever) using a lightweight audio model.
2.  **Audio Capture**: The `VoiceRecognitionService` records the subsequent voice command.
3.  **Speech to Text**: Audio is passed via JNI to the **Vosk** engine, which converts it to text.
4.  **Intent Understanding**: The transcribed text is sent to the **TinyLlama** model via `llama.cpp`. The LLM parses the natural language into a structured command (e.g., `{"intent":"brightness", "parameter":"max"}`).
5.  **Command Execution**: The `CommandProcessor` receives the intent and calls the appropriate `SystemController` or service (e.g., `BrightnessService`) to perform the action using Android APIs.
6.  **System Feedback**: The user interface is updated to reflect the change.

## Testing

The project includes standalone test modules to validate components:
*   **`TestBrightnessActivity`**: Manually test brightness control via sliders and buttons, and view command logs.
*   **`TestSoundActivity`**: Test control over different audio streams (Media, Alarm, etc.).

Run these activities directly from Android Studio to debug system integration without using the voice pipeline.

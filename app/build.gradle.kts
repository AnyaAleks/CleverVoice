plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "pro.cleverlife.clevervoice"
    compileSdk = 35

    defaultConfig {
        applicationId = "pro.cleverlife.clevervoice"
        minSdk = 25
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        externalNativeBuild {
            cmake {
                cppFlags("-std=c++17")
                arguments("-DANDROID_STL=c++_shared")
            }
        }

        ndk {
            abiFilters.add("arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        prefab = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    androidResources {
        noCompress += setOf("gguf", "bin", "tflite", "model", "json")
    }

    // Правильная конфигурация для библиотек в AGP 8.0+
    packaging {
        resources {
            excludes += setOf(
                "**/libc++_shared.so",
                "**/libOpenCL.so",
                "**/libft2.so",
                "**/libpng.so"
            )

            pickFirsts += setOf(
                "**/libllama-wrapper.so",
                "**/libggml.so",
                "**/libggml-base.so",
                "**/libggml-cpu.so",
                "**/libllama.so",
                "**/libomp.so",
                "**/libc++_shared.so"
            )
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(libs.vosk.android)

    // ДЛЯ AI:
    implementation("org.json:json:20230227")
    implementation("commons-io:commons-io:2.15.1")
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0")
    implementation("androidx.media:media:1.7.0")
}
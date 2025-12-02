# Castalk Socket
[![CI](https://github.com/castalk/android-socket-module/actions/workflows/android.yml/badge.svg)](https://github.com/castalk/android-socket-module/actions/workflows/android.yml)
[![Latest release](https://img.shields.io/badge/Latest%20release-brightgreen)](https://github.com/castalk/android-socket-module/releases/latest)
<!-- COVERAGE:START -->
![Coverage](https://img.shields.io/badge/coverage-100.00%25-brightgreen)
Coverage: 100.00%
<!-- COVERAGE:END -->

> Tori Talk is a JSON-RPC WebSocket client that gives Android apps a fully managed talk session for avatar chat and live video experiences backed by typed models and coroutine flows.

## Table of Contents
1. [Overview](#overview)
2. [Features](#features)
3. [Requirements](#requirements)
4. [Installation](#installation)
5. [Configuration](#configuration)
6. [Usage](#usage)
7. [Session Events](#session-events)
8. [Release Channels](#release-channels)
9. [Publishing a Release](#publishing-a-release)
10. [Resources](#resources)

## Overview
Castalk Socket unifies chat, live video, emotion tracking, and face/voice swap events under a single Kotlin API. The client streams structured session events over WebSocket, allowing UI layers to react with simple coroutine collections.

## Features
- Kotlin-first JSON-RPC WebSocket client with coroutine flows.
- Typed session events for chat, video, emotions, retries, and errors.
- Optional Hilt integration (`<= v1.0.4`) and lightweight manual setup (`>= v1.0.5`).
- Configurable custom headers and reconnect strategy.
- 100% test coverage with CI verification.

## Requirements
```text
compileSdk: 36
Android Gradle Plugin: 8.9.1
Cca-Version: 10 or higher
```

## Installation
1. Add the dependency:
   ```groovy
   dependencies {
       implementation "com.castalk:tori-talk:$latest-release"
   }
   ```
2. Include the repository helper in the root `build.gradle`:
   ```groovy
   apply from: 'https://gist.githubusercontent.com/tuan-jason/67b24bee582bb1eb03876fcb06927b54/raw/fd9afca16dd4a0219d3a1ac9cbdfe6ef0802bc1e/extra_repositories.gradle'

   allprojects {
       repositories {
           // existing repositories
           rootProject.ext.addExternalRepos(delegate as RepositoryHandler)
       }
   }
   ```
   
   For `Kotlin DSL`, update `settings.gradle.kts` as below:
    ```kotlin
    import groovy.lang.Closure
    
    apply(from = uri("https://gist.githubusercontent.com/tuan-jason/67b24bee582bb1eb03876fcb06927b54/raw/15814e91d4f10fe7ddd20c161ecdae31abafc061/extra_repositories.gradle"))
    val addExternalRepos = extra.properties["addExternalRepos"] as? Closure<*>
    //....
    dependencyResolutionManagement {
        repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
        repositories {
            addExternalRepos?.call(this)
        }
    }
    ```

## Configuration
Add your GitHub credentials to `local.properties` for authenticated artifact access:
```
READ_AUTOBAHN_ACTOR=<your personal access token>
READ_AUTOBAHN_TOKEN=<your GitHub username>
```

## Usage
### 1. Provide a `TalkSession`
- **With Hilt (≤ [v1.0.4](https://github.com/castalk/android-socket-module/releases/tag/v1.0.4))**
  ```kotlin
  @Inject lateinit var socketManager: ISocketManager
  ```
- **Without Hilt (≥ [v1.0.5](https://github.com/castalk/android-socket-module/releases/tag/v1.0.5))**
  ```kotlin
  private val talkSession by lazy { TalkSession() }
  ```

### 2. Connect
```kotlin
val customHeaders = mutableMapOf(String, String)
talkSession.connect(socketUrl, customHeaders = customHeaders)
```

### 3. Observe session events
```kotlin
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch {
            talkSession.observableSessionEvent().collect { socketEvent ->
                Log.d(TAG, "Collected socketEvent: $socketEvent")
                when (socketEvent) {
                    is SessionEvent.Connected -> Unit
                    is SessionEvent.Disconnected -> Unit
                    is SessionEvent.RetryConnectSuccess -> Unit
                    is SessionEvent.RetryConnectFailed -> Unit
                    is SessionEvent.TextMessage -> Unit
                    is SessionEvent.VideoMessage -> Unit
                    is SessionEvent.IsResponding -> Unit
                    is SessionEvent.GeneralError -> Unit
                    is SessionEvent.StreamingStop -> Unit
                    is SessionEvent.FaceEmotion -> Unit
                    is SessionEvent.TextEmotion -> Unit
                    SessionEvent.RetryingConnect -> Unit
                }
            }
        }
    }
}
```

## Session Events
Review the full list of session events and their payloads in [`socket/src/main/java/com/castalk/socket/data/model/SessionEvent.kt`](https://github.com/castalk/android-socket-module/blob/main/socket/src/main/java/com/castalk/socket/data/model/SessionEvent.kt).

## Release Channels
- Up to `v1.0.4`: Hilt DI module ships with the library.
- From `v1.0.5`: Hilt dependencies were removed; instantiate `TalkSession` manually.
- Since `v2.0.0`: artifacts are published to [maven-packages](https://github.com/castalk/maven-packages) for easier external consumption.

Refer to the [changelog](https://github.com/castalk/android-socket-module/releases) for detailed version notes.

## Publishing a Release
1. Update `libVersion` in `socket/build.gradle` and push to `main`.
   ```diff
   -def libVersion = "1.0.3"
   +def libVersion = "1.0.4"
   ```
2. Publish to GitHub Packages (requires `PUBLISH_LIB_ACTOR` and `PUBLISH_LIB_TOKEN` in `local.properties`):
   ```
   ./gradlew clean publish
   ```
3. Tag and push the release:
   ```
   git tag v1.0.4
   git push origin v1.0.4
   ```
4. Create a release entry on GitHub with notes and artifacts.

## Resources
- [Usage examples and internal docs](https://castalk.atlassian.net/wiki/x/TQDJIw)
- [Latest release](https://github.com/castalk/android-socket-module/releases/latest)
- [GitHub Actions workflow](https://github.com/castalk/android-socket-module/actions/workflows/android.yml)

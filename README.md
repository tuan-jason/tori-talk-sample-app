# Castalk Socket
[![CI](https://github.com/castalk/android-socket-module/actions/workflows/android.yml/badge.svg)](https://github.com/castalk/android-socket-module/actions/workflows/android.yml)
[![Latest release](https://img.shields.io/badge/Latest%20release-brightgreen)](https://github.com/castalk/android-socket-module/releases/latest)
<!-- COVERAGE:START -->
![Coverage](https://img.shields.io/badge/coverage-98.72%25-brightgreen)
Coverage: 98.72%
<!-- COVERAGE:END -->

> Tori Talk SDK is a JSON-RPC websocket/webrtc client that gives apps a ready-made talk session for avatar chat and video experiences backed by typed models and coroutine flows. It stands out by unifying chat, live video, emotion tracking, and face/voice swaps behind a single Kotlin API that continuously streams structured session events.

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
Tori Talk SDK unifies chat, live video, emotion tracking, and face/voice swap events under a single Kotlin API. The client streams structured session events over WebSocket, allowing UI layers to react with simple coroutine collections.

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

### 3. Start a call
```kotlin
// Default: two-way video with mic enabled
talkSession.startVideoCall { result ->
    when (result) {
        is StartVideoCallResult.Success -> {
            result.callData.videoView?.let(videoContainer::addView)
        }
        is StartVideoCallResult.Failure -> Log.e(TAG, "startVideoCall failed ${'$'}{result.error.code}")
    }
}
```

```kotlin
// Audio-only: keep microphone on but skip video tracks
talkSession.startVideoCall(options = CallOption(enableVideo = false)) { /* handle result */ }
```

```kotlin
// Video muted on join but microphone enabled
talkSession.startVideoCall(
    options = CallOption(enableVideo = false, enableMicrophone = true)
) { /* handle result */ }
```

```kotlin
// Silent join: camera on, microphone muted
talkSession.startVideoCall(options = CallOption(enableMicrophone = false)) { /* handle result */ }
```

Set `CallOption.enableVideo` to `true` (default) for a full video call or `false` for audio-only mode. Similarly, `CallOption.enableMicrophone` controls whether participants join with the mic open (`true`) or muted (`false`). Combine these flags to match the desired UX—e.g., `CallOption(enableVideo=false, enableMicrophone=false)` for a listen-only audio session.

### 4. Manually publish camera stream
Use `talkSession.publishCamera` when you need fine-grained control over when the local camera joins the RTC room—for example, showing a preview only after a user taps a toggle. The callback can also emit `PublishCameraResult.Unpublished` whenever the SDK removes the track—typically because the RTC room disconnected—so you can update UI state and decide whether to republish.

```kotlin
// Request CAMERA permission beforehand.
talkSession.publishCamera(facing = CameraFacing.FRONT) { result ->
    when (result) {
        is PublishCameraResult.Success -> {
            val cameraView = result.cameraView.cameraView
            previewContainer.removeAllViews()
            previewContainer.addView(cameraView)
        }
        is PublishCameraResult.Failure -> {
            Log.e(TAG, "Unable to publish camera: ${'$'}{result.error.message}")
        }
        PublishCameraResult.Unpublished -> {
            Log.w(TAG, "Camera track unpublished because the room disconnected")
            // Optionally trigger reconnection UI or attempt to republish.
        }
    }
}

// Later, when the user closes preview or leaves call:
talkSession.unpublishCamera()
```

### 5. Observe session events
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
                    is SessionEvent.UserTranscript -> Unit
                    is SessionEvent.AgentTranscript -> Unit
                    SessionEvent.RetryingConnect -> Unit
                }
            }
        }
    }
}
```

### 6. Mute/Unmute microphone
During a call, app can choose to mute/unmute user’s voice anytime.

#### Mute microphone
```kotlin
talkSession.muteMicrophone {
    binding.ivMute.load(R.drawable.ic_mute_on)
}
```

#### Unmute microphone
```kotlin
talkSession.unmuteMicrophone {
    binding.ivMute.load(R.drawable.ic_mute)
}
```

### 7. Manually publish your camera
When starting a call (either video or audio call) via startVideoCall API, if enableCamera param is not passed in, your FRONT camera track is published to server side by default.

If you wish to render the published camera track in your app, use `TalkSession.publishCamera()API`. Below is an example:
```kotlin
talkSession.publishCamera { result ->
    when (result) {
        is PublishCameraResult.Success -> {
            // Add this cameraView to your layout
            cameraView = result.cameraView.cameraView
        }
        //There's an error when publishing camera track. Look into result.error
        //to learn about the details.
        is PublishCameraResult.Failure -> {
        }
    }
}
```

When you no longer want to render the streaming camera video, un-publish it.
```kotlin
talkSession.unpublishCamera()
```

You can choose to disable this by passing `enableCamera = false`


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

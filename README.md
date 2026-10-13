# Muguet for Android

An experimental Android Studio port of Muguet, a native host for the game's WebAssembly build. The `android` branch adds an Android application around the existing Rust, Wasmtime, and wgpu engine while preserving the original iOS/macOS project and Apple-specific code.

> **Status: experimental.** The Android build is still being validated in GitHub Actions, and on-device graphics, touch controls, and gamepad behavior need physical-device testing. Do not treat this branch as a release-ready build yet.

## Android features and architecture

- Android Studio project using Gradle.
- Rust engine built for `arm64-v8a` (`aarch64-linux-android`) using `cargo-ndk`.
- Winit Android NativeActivity integration.
- wgpu selects Vulkan or GLES on Android; Apple platforms retain their Metal backend.
- Android launcher supports game-data ZIP import and common game settings.
- Game/config files are stored in the app-private files directory.
- Android touch events are mapped to game input; visual touch-control parity and external controller support remain in progress.

## Requirements

- Android Studio with the Android SDK and NDK installed.
- Rust toolchain with the Android target.
- `cargo-ndk`.
- An arm64 Android device for meaningful testing.
- Your own compatible game-data ZIP. This repository does not include game content.

## Build in Android Studio

1. Check out the Android branch:
   ```bash
   git switch android
   ```
2. Install the Android SDK and NDK from Android Studio's **Tools → SDK Manager**.
3. Install the Rust target and `cargo-ndk`:
   ```bash
   rustup target add aarch64-linux-android
   cargo install cargo-ndk
   ```
4. Set `ANDROID_NDK_HOME` (or `ANDROID_NDK_ROOT`) to the installed NDK directory.
5. Open the repository root in Android Studio, sync Gradle, select an arm64 device, and run the `app` configuration.

To build from a compatible shell, run:

```bash
bash ./gradlew assembleDebug
```

To build only the native Rust library:

```bash
./scripts/build-android.sh
```

The native build script targets Android API 28, matching the app's `minSdk`. This matters because CPAL's Android AAudio backend links `libaaudio`, which is available in the Android NDK sysroot starting at API 26.

On Windows, use `gradlew.bat` for Gradle commands and configure the Android NDK/Rust toolchain in a compatible shell for the native build.

## Game data

On first launch, use **Import game data .zip** and select your own compatible archive. It should contain `data/manifest.json` and `b/<build>/shaders/index.json`. After import, tap **Play**. The game data is not provided by this repository.

## Android troubleshooting

- **`App is private` Rust compiler error:** the Android entry point and its `App` return type must have compatible visibility. The Android branch now declares the type `pub(crate)`.
- **`gradlew: Permission denied` in CI:** the workflow invokes the wrapper with `bash ./gradlew assembleDebug`, so it does not rely on the executable bit being set.
- **`ld.lld: error: unable to find library -laaudio`:** `scripts/build-android.sh` now passes `-p 28` to `cargo-ndk` so it uses the Android API 28 NDK sysroot. Confirm this flag is present and `ANDROID_NDK_HOME` points to the installed NDK.
- **Missing Android target:** run `rustup target add aarch64-linux-android` with the Rust toolchain used by the build.
- **NDK not found:** confirm `ANDROID_NDK_HOME` points to an installed NDK version.

The current workflow is available at [GitHub Actions](https://github.com/mhmdeve/atg-diordna/actions). A green workflow and physical-device testing are required before considering the port build-verified.

For more Android-specific details and current limitations, see [README-ANDROID.md](README-ANDROID.md).

---

## Original iOS/macOS port

The original project supports Apple Silicon Macs (macOS 14 or later) and iOS. This Android branch does not replace the original Apple code or build process.

The engine runs the same `game.wasm` as a native app using [Wasmtime](https://wasmtime.dev) and [wgpu](https://wgpu.rs). On macOS, the game code is compiled to native arm64 on first launch and cached. On iOS, the game code is compiled at build time and included in the app. Muguet also provides a small UDP multiplayer mode for other users of the port.

### Original Apple build requirements

- Apple Silicon Mac with macOS 14 or later.
- Rust toolchain and Xcode 26 or later.
- For iOS: `rustup` toolchain 1.98.1, the `aarch64-apple-ios` target, `xcodegen`, an Apple developer account, and an iOS 15 or later device (ideally 8 GB RAM).

The original build scripts remain available:

```bash
./make-app.sh
TEAM=<your-team-id> ./make-ios.sh game.zip --install
```

For an IPA, use `--ipa`; with a free developer account, set `FREE=1` and provide your own `BUNDLE_ID`. The original upstream build helper is documented in the Apple project workflow; these commands are not used for Android builds.

## General limitations

- iOS needs the increased memory-limit entitlement.
- Story missions are single-player only.
- The Android port remains experimental until CI and device testing pass.

## Credits

- [Kagander](https://github.com/Kagander) for fixing the minimap.
- [inj-src](https://github.com/inj-src) for decoding BC textures on all cores.
- [Dbhardwaj99](https://github.com/Dbhardwaj99) for reporting real free disk space.
- [dre4moff](https://github.com/dre4moff) for releasing controller input when the app loses focus.
- [divyanshugarg36](https://github.com/divyanshugarg36) for keyboard support and steadier frames on iOS, the memory limit on free accounts, the frame-rate label, and fixes for game crashes/freezes.

Contributions are welcome!

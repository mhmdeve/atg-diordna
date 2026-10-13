# Android Studio port

The `android` branch adds an Android Studio Gradle application around the existing Rust/wgpu/Wasmtime engine. The original iOS/Xcode files are kept unchanged.

## Open and build

1. Install Android Studio, the Android SDK, and the NDK from **Tools → SDK Manager → SDK Tools**.
2. Install Rust and the Android NDK target:
   ```bash
   rustup target add aarch64-linux-android
   cargo install cargo-ndk
   ```
3. Set `ANDROID_NDK_HOME` (or `ANDROID_NDK_ROOT`) to the NDK folder shown in Android Studio.
4. Open the repository root in Android Studio and sync Gradle. Select an arm64 Android device or emulator, then run the `app` configuration. The `preBuild` task calls `scripts/build-android.sh`, which builds `libmuguet.so` for `arm64-v8a`.
5. Launch Muguet, tap **Import game data .zip**, and select a compatible game data zip that includes `data/manifest.json` and `b/<build>/shaders/index.json`. The game content is not bundled with this repository. Tap **Play** after import.

Standalone native library build:
```bash
export ANDROID_NDK_HOME="$HOME/Android/Sdk/ndk/<installed-version>"
rustup target add aarch64-linux-android
cargo install cargo-ndk
./scripts/build-android.sh
```

## Android adaptation notes

- The app entry point uses Winit's Android NativeActivity integration.
- The renderer selects Vulkan and GLES on Android; Apple targets keep their Metal backend.
- Android game/config files are stored in the app-private files directory.
- Android's default Wasm heap reservation is capped at 4 GiB. Set `MUGUET_ANDROID_HEAP_GB` at Rust compile time to alter it.
- The launcher mirrors the iOS options (mode, graphics preset, resolution scale, textures, frame limit, touch sensitivity, mute, FPS display, multiplayer name/host settings). The exact SwiftUI layout is not copied pixel-for-pixel: Android uses native Android views.
- Android touch input is wired to the game-facing input actions. The translucent button/joystick overlay's native drawing and all controller-specific behavior still need validation and parity work on physical Android devices.
- The Rust crate and Gradle project have been wired, but a successful Android NDK/Gradle build has not been run in this environment. The native touch overlay is currently input-mapping scaffolding rather than a pixel-matched rendering of the UIKit overlay, and external gamepad parity still needs implementation. Treat the branch as an in-progress port until it is built and tested with Android Studio and a physical device.


## Build status and parity scope

The project currently contains the Android Gradle module, NativeActivity entry point, Rust Android target adaptation, launcher settings form, ZIP data import, and an Android build workflow. It has **not** been compiled in this session because Rust/Cargo, Gradle, and Android SDK/NDK are unavailable in the runtime.

The visual behavior is not yet an exact replica of the SwiftUI/UIKit UI. The launcher preserves most settings and the Rust-side Android touch module maps touch gestures to game input, but its on-screen controls are not yet drawn as the matching translucent iOS buttons and joystick, and physical game-controller parity remains incomplete. The workflow is intended to expose compile errors once run in GitHub Actions; resolve those before treating this as release-ready.

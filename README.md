# Muguet
A native Mach-O iOS/macOS port of GTA V, with multiplayer

## How does this work ?
this whole port relies on the WebAssembly compilation of the game. in a browser, that `game.wasm` runs inside the JS engine, behind WebGPU, Web Audio, web workers...
Muguet throws that layer away and runs the same `game.wasm` as a regular native app, with [wasmtime](https://wasmtime.dev) for the code and [wgpu](https://wgpu.rs) on Metal for the graphics

On macOS, the game code is compiled to native arm64 the first time you launch it, then cached. On iOS, we can't properly do that (i dont rlly want you to deal with the hassle of enabling JIT), so the game code is compiled when you build the app and baked into it

On top of that, Muguet adds a small UDP multiplayer (for other people who have the same port to play with you)

## Game files
Because I care about me not getting sued, you will need your own copy as a `.zip` : the folder holding `data/` and `b/<build>/` (with `game.wasm` and `shaders/`)

It's basically the same source as the web port.

Muguet itself will then asks for that zip

## Requirements
- Apple Silicon Mac, macOS 14 or later
- Rust toolchain
- Xcode 26 or later

For iOS, also :
- `rustup` with `rustup toolchain install 1.98.1` and `rustup target add aarch64-apple-ios --toolchain 1.98.1`
- `xcodegen` (`brew install xcodegen`)
- an Apple developer account
- a device on iOS 15 or later, ideally 8 GB of RAM

## Building
The easy way, paste this in Terminal :
```
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/c22dev/muguet/main/build.sh)"
```
It downloads Muguet into `/tmp/muguet`, installs what's missing (Homebrew, Rust...), then asks whether you want the Mac or the iOS version and walks you through the rest. The `.app` / `.ipa` end up in `~/Downloads/muguetOutput`. Already have the repo ? Double-click `build.command`, or run `./build.sh`

You need Xcode 26 or later from the App Store. To install on your iPhone / iPad, sign in to Xcode with your Apple ID (Settings > Accounts), a free one works

By hand, macOS :
```
./make-app.sh
```
gives `build/Muguet.app`

iOS :
```
TEAM=<your team id> ./make-ios.sh game.zip --install
```
`--ipa` makes `build/Muguet.ipa` instead. With a free account, add `FREE=1` (+ a `BUNDLE_ID` of your own)

Please install this app with AltStore. This probably won't work well with Sideloadly (or even SideStore, as per [this issue])(https://github.com/SideStore/SideStore/issues/1616)

The app also works with a paid developer account.


## Android (experimental)

An Android Studio port is available on the [`android`](https://github.com/mhmdeve/atg-diordna/tree/android) branch. The original iOS/macOS project and its Apple-specific code remain available.

### Android requirements

- Android Studio with Android SDK and NDK installed
- Rust toolchain and the `aarch64-linux-android` target
- `cargo-ndk`
- An arm64 Android device for practical testing
- Your own compatible game-data ZIP; game data is not included in this repository

### Build for Android

Switch to the Android branch:

```bash
git switch android
rustup target add aarch64-linux-android
cargo install cargo-ndk
```

Set `ANDROID_NDK_HOME` to the NDK directory installed by Android Studio, then build the native library:

```bash
./scripts/build-android.sh
```

Alternatively, open the repository root in Android Studio, sync Gradle, and run the `app` configuration on an arm64 device. On Windows, use `gradlew.bat` for Gradle commands; the native Rust build script must also be run in a compatible shell with Rust and the Android NDK configured.

On first launch, choose **Import game data .zip** and select your own compatible archive containing `data/manifest.json` and `b/<build>/shaders/index.json`, then tap **Play**.

### Android status and limitations

This is an **experimental, in-progress port**, not yet a verified release build. The Android Gradle project, native activity integration, Rust Android entry point, launcher settings, data import, and Android build workflow are present. A successful Android NDK/Gradle build and on-device runtime test have not yet been confirmed.

The Android launcher uses native Android views and is not pixel-identical to the SwiftUI/UIKit interface. The touch-input mapping is in progress; matching translucent joystick/button rendering and complete external game-controller parity still require implementation and physical-device testing. See [README-ANDROID.md](https://github.com/mhmdeve/atg-diordna/blob/android/README-ANDROID.md) for Android-specific setup and current limitations.

## Multiplayer
One player hosts from the launcher (or runs `muguet-server` on any machine), the others join with that address. UDP port 47520

## Limitations
- iOS needs the increased memory limit entitlement
- Story missions are single player only

## Credits
- [Kagander](https://github.com/Kagander) for fixing the minimap
- [inj-src](https://github.com/inj-src) for decoding bc textures on all cores
- [Dbhardwaj99](https://github.com/Dbhardwaj99) for reporting real free disk space
- [dre4moff](https://github.com/dre4moff) for releasing controller input when the app loses focus
- [divyanshugarg36](https://github.com/divyanshugarg36) for keyboard support and steadier frames on iOS, the memory limit on free accounts, the frame rate label, and fixing the safehouse tv freeze and a pedestrian spawn crash

Contributions are welcome !

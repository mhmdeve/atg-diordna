#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

if ! command -v cargo-ndk >/dev/null 2>&1; then
  echo "cargo-ndk is required. Install it with: cargo install cargo-ndk"
  exit 1
fi
if [ -z "${ANDROID_NDK_HOME:-}" ] && [ -z "${ANDROID_NDK_ROOT:-}" ]; then
  echo "Set ANDROID_NDK_HOME to the NDK directory from Android Studio > SDK Manager > SDK Tools."
  exit 1
fi

rustup target add aarch64-linux-android
# cpal's Android AAudio backend links libaaudio, which is available from Android API 26.
# Use API 28 to match the app's minSdk and ensure cargo-ndk uses the correct NDK sysroot.
cargo ndk -t arm64-v8a -p 28 -o app/src/main/jniLibs build --release --lib
test -f app/src/main/jniLibs/arm64-v8a/libmuguet.so
echo "Built app/src/main/jniLibs/arm64-v8a/libmuguet.so"

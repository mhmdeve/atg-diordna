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

# Keep cargo-ndk on one NDK path if both environment variables are defined.
if [ -n "${ANDROID_NDK_HOME:-}" ]; then
  export ANDROID_NDK_ROOT="$ANDROID_NDK_HOME"
else
  export ANDROID_NDK_HOME="$ANDROID_NDK_ROOT"
fi

rustup target add aarch64-linux-android
# cpal's Android AAudio backend links libaaudio, available from Android API 26.
# Use API 28 to match the app's minSdk. Use the long option to avoid -p being
# interpreted as Cargo's package selector by cargo-ndk's command forwarding.
cargo ndk -t arm64-v8a --platform 28 -o app/src/main/jniLibs build --release --lib
test -f app/src/main/jniLibs/arm64-v8a/libmuguet.so
echo "Built app/src/main/jniLibs/arm64-v8a/libmuguet.so"

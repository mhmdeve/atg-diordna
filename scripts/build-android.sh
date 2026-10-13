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
cargo ndk -t arm64-v8a -o app/src/main/jniLibs build --release --lib
test -f app/src/main/jniLibs/arm64-v8a/libmuguet.so
echo "Built app/src/main/jniLibs/arm64-v8a/libmuguet.so"

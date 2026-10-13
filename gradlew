#!/usr/bin/env sh
# Minimal bootstrap wrapper: Android Studio can also build this project with its bundled Gradle.
set -eu
VERSION="8.13"
CACHE="${GRADLE_USER_HOME:-$HOME/.gradle}/wrapper/dists/gradle-$VERSION-bin/manual"
ZIP="$CACHE/gradle-$VERSION-bin.zip"
DIST="$CACHE/gradle-$VERSION"
if [ ! -x "$DIST/bin/gradle" ]; then
  mkdir -p "$CACHE"
  if [ ! -f "$ZIP" ]; then
    echo "Downloading Gradle $VERSION wrapper distribution..."
    command -v curl >/dev/null 2>&1 || { echo "curl is required to bootstrap Gradle"; exit 1; }
    curl -fL "https://services.gradle.org/distributions/gradle-$VERSION-bin.zip" -o "$ZIP"
  fi
  command -v unzip >/dev/null 2>&1 || { echo "unzip is required to bootstrap Gradle"; exit 1; }
  unzip -q -o "$ZIP" -d "$CACHE"
fi
exec "$DIST/bin/gradle" "$@"

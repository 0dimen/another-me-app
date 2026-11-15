#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
export JAVA_HOME="${JAVA_HOME:-$HOME/Library/Java/JavaVirtualMachines/corretto-17.0.7/Contents/Home}"
./gradlew testDebugUnitTest assembleDebug
mkdir -p dist
cp app/build/outputs/apk/debug/app-debug.apk dist/another-me-demo-debug.apk
shasum -a 256 dist/another-me-demo-debug.apk > dist/SHA256SUMS
printf 'APK: dist/another-me-demo-debug.apk\n'

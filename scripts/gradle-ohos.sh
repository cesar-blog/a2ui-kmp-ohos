#!/usr/bin/env bash
set -euo pipefail
task_repo_root="$(cd "$(dirname "$0")/.." && pwd)"
task_gradle_home="${GRADLE_USER_HOME:-$HOME/.gradle}"
task_gradle_bin=""
shopt -s nullglob
for candidate in "$task_gradle_home"/wrapper/dists/gradle-8.14-bin/*/gradle-8.14/bin/gradle; do
    if [[ -x "$candidate" ]]; then task_gradle_bin="$candidate"; break; fi
done
if [[ -z "$task_gradle_bin" ]]; then task_gradle_bin="$task_repo_root/gradlew"; fi
task_native_home="${KOTLIN_NATIVE_HOME:-$HOME/.konan/kotlin-native-prebuilt-macos-aarch64-2.2.21-1.2.0}"
if [[ -d "$task_native_home" ]]; then
    exec "$task_gradle_bin" "-Pkotlin.native.home=$task_native_home" "$@"
fi
exec "$task_gradle_bin" "$@"

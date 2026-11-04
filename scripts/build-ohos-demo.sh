#!/usr/bin/env bash
set -euo pipefail
task_root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$task_root"
task_checks=false
if [[ "${1:-}" == "--checks" ]]; then task_checks=true; shift; fi
if [[ $# -ne 0 ]]; then echo "Usage: $0 [--checks]" >&2; exit 2; fi
task_modules=(a2ui-compat a2ui-model a2ui-engine compose-runtime compose-ui)
task_publish=()
for module in "${task_modules[@]}"; do
    task_publish+=(":$module:publishAllPublicationsToLocalRepository")
done
scripts/gradle-ohos.sh -p target/a2ui-kmp "${task_publish[@]}" --console=plain
if $task_checks; then
    scripts/gradle-ohos.sh -p target/a2ui-kmp \
        :a2ui-compat:linkDebugTestOhosArm64 :compose-runtime:linkDebugTestOhosArm64 --console=plain
    scripts/gradle-ohos.sh -p ohos-sample :composeApp:publishDebugBinariesToHarmonyApp \
        -PwithNativeSelfTests=true --console=plain
else
    scripts/gradle-ohos.sh -p ohos-sample :composeApp:publishDebugBinariesToHarmonyApp --console=plain
fi
task_stage="${A2UI_HARMONY_STAGE:-/private/tmp/kai-a2ui-harmony}"
python3 ohos-sample/stage-harmony.py --output "$task_stage" \
    --sdk "${A2UI_OHOS_SDK:-$HOME/Library/OpenHarmony/Sdk}"
task_deveco="${DEVECO_HOME:-/Applications/DevEco-Studio.app/Contents}"
export NODE_HOME="${NODE_HOME:-$task_deveco/tools/node}"
cd "$task_stage"
"$task_deveco/tools/ohpm/bin/ohpm" install --all
"$task_deveco/tools/hvigor/bin/hvigorw" --mode module -p product=default \
    -p module=entry@default -p buildMode=debug assembleHap --no-daemon
echo "HAP: $task_stage/entry/build/default/outputs/default/entry-default-unsigned.hap"

# A2UI 鸿蒙示例

固定使用 CPF KMP `2.2.21-1.2.0` 与 CPF Compose Multiplatform `1.9.2-1.2.0`。
本示例复用 A2UI 的 parser、engine、Compose runtime 和 Compose renderer，采用应用自定义组件目录，不依赖新版 `material3-a2ui`。

## 页面

- **预订表单**：A2UI JSON 创建标题、文本、姓名输入框、人数选择器与提交按钮。输入使用 `bindUpdater` 写回数据模型；提交由 `dispatchAction` 触发，通过 `outboundEvents` 显示当前姓名和人数。
- **动态更新**：先创建订单界面，随后两条 `updateDataModel` 修改状态，再通过 `updateComponents` 插入取餐按钮。点击按钮会产生 `confirm_pickup` 事件。

消息由 `AgentMessages.kt` 预先写好，不连接模型或真实业务服务。界面上的“出站事件”是本地记录，不能代表真实预约完成。

## 工程结构

```text
composeApp/src/commonMain/kotlin/com/kai/a2ui/
  AgentMessages.kt  JSON 消息和延迟更新
  KaiCatalog.kt     六种应用自有组件及数据绑定
  App.kt            消息输入、surface 渲染、出站事件记录
composeApp/src/ohosMain/.../MainArkUIViewController.kt
                   复用已有 Compose 鸿蒙入口
composeApp/src/ohosMain/.../IcuProbe.kt
                   在 HAP 进程检查地区、数字与日期格式化
composeApp/src/desktopMain/.../Main.kt
                   同一页面的桌面入口
composeApp/src/desktopTest/.../A2uiDemoTest.kt
                   验证输入回写、真实事件和增量更新
harmonyApp/        ArkTS/CMake/NAPI 壳
```

## 构建

先在父工程发布适配库到 `../build/repo`。本示例对 `ai.littlepress.a2ui` 使用 exclusive repository，缺少本地产物时直接失败，不会退回下载上游未适配包。

```sh
# 在 ohos-sample 目录执行
./gradlew :composeApp:publishDebugBinariesToHarmonyApp
```

可用 `-Pa2uiRepo=/absolute/path/to/repo` 指定另一份本地发布目录。

默认只配置 `ohosArm64`。配套 x64 依赖全部具备后，添加 `-PenableOhosX64=true`，并把 `harmonyApp/entry/build-profile.json5` 中的 `abiFilters` 加上 `x86_64`。

此构建 profile 仅启用 OHOS 目标，没有配置 JVM/Desktop 目标、Desktop source set 依赖或桌面应用任务。工程保留 Desktop 入口与交互测试源码供后续参考。当前检查到的 CPF `1.9.2-1.2.0` Compose 发行包没有 Desktop 变体，本轮以固定版本的 OHOS 编译和设备运行验证为准。

## 鸿蒙应用

1. 通过 Gradle 发布任务生成并复制本次构建的 `libkn.so`、`libkn_api.h`。
2. 在 `harmonyApp` 安装依赖。Compose HAR 从 `@cpf-kmp-cmp/compose:1.9.2-1.2.0` 取得，不使用旧样例二进制。
3. 若工程所在路径含中文，先执行 `python3 stage-harmony.py`，再用 DevEco 或 Hvigor 打开输出的 ASCII 临时目录；Hvigor 会拒绝含中文的项目路径。
4. 本示例配置为 OpenHarmony SDK 20、兼容 API 17、`default` 设备类型，匹配当前 OpenHarmony 6 模拟器。项目包名为 `com.kai.a2ui`。签名或无签名安装能否使用，以目标设备的实际安装结果为准。

仓库中的签名列表为空，不包含原项目的证书、私钥、密码、签名材料路径或生成产物。签名材料应保留在工程外；打包源码时仍需排除本地签名配置。

OHOS 入口会调用一次 ICU 启动检查，分别验证地区标签、数字格式和 UTC 日期格式。结果写入 `A2UI_ICU_PROBE` 日志，并在页面底部显示通过项数；单项失败会被捕获，不阻止表单显示。此检查必须在真实 HAP 进程中观察结果，不能用 shell 进程的权限或 ICU 数据访问结果替代。

## 可验证结果

保留的 Desktop 测试源码描述了两项渲染交互检查，当前 profile 不运行它们：

- 将姓名改为 Lin、人数改为 3 后点击“确认预订”，检查 A2UI 事件上下文中的 `guest=Lin` 与 `party=3`。
- 等待订单状态变为“可以取餐了”，检查后来插入的“确认取餐”按钮及其出站事件。

测试是否通过、OHOS 是否完成编译和设备运行，以交付时的构建记录为准。上述 Desktop 测试当前受发行包缺少平台变体限制。

## 在 HAP 中运行已有 Native 测试 KLIB

当独立 `test.kexe` 因设备上的 Ark/Panda 动态库或进程环境无法启动时，可以在真实应用进程内调用已编译的测试方法。此路径不运行 shell 测试可执行文件，也不复制测试实现。

先在父工程生成以下测试编译产物：

```text
target/a2ui-kmp/a2ui-compat/build/classes/kotlin/ohosArm64/test/klib/a2ui-compat_test
target/a2ui-kmp/compose/compose-runtime/build/classes/kotlin/ohosArm64/test/klib/compose-runtime_test
```

本次检查到它们的 manifest 均为 `compiler_version=2.2.21-1.2.0`、`abi_version=2.2.0`、`native_targets=ohos_arm64`，各自的 `unique_name` 与主库不同。它们必须与即将链接的主库来自同一次适配版本。

```sh
# 在 ohos-sample 目录，只构建 debug arm64 验证版本
./gradlew :composeApp:publishDebugBinariesToHarmonyApp -PwithNativeSelfTests=true
```

该开关把上述测试 KLIB 作为文件依赖链接，增加 `kotlin-test` 与匹配版本的 `kotlinx-coroutines-test`，并启用 `src/nativeChecksMain`。没有开关的普通构建使用空 runner，不链接测试 KLIB；启用自测时会拒绝 Release 任务和 x64 目标。

HAP 启动后在后台依次调用五个已有测试方法：地区标签与非法输入、Native 锁互斥与重入、JSON Reader 的未知结构与转换失败、keyed side effect 的去重、提交数据与多 surface 隔离。每项保留原断言，记录 `A2UI_SELF_TEST START/PASS/FAIL`，最后记录 `SUMMARY`；页面底部显示实际通过数。单项失败会继续执行后续项，且不阻断表单。

这是五项显式调用的 Native 回归用例，不能代表上游全部单元测试已运行；也不提供测试框架的自动发现或生命周期钩子。是否通过必须以本次 HAP 的日志和界面结果为准，编译成功本身不算通过。

## 来源

自定义 Catalog 的结构参考 BinaryTape/a2ui-kmp 的 `samples/custom`；协议与渲染实现保留在父工程的上游适配库中。鸿蒙壳来源于 CPF-KMP-CMP 的 `kmp-cmp-example`，已去除旧二进制与本机配置。上游许可证见父工程 `LICENSE`、`NOTICE`。

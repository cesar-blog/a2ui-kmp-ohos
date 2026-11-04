# A2UI OHOS 适配

固定 CPF KMP **2.2.21-1.2.0**、CMP **1.9.2-1.2.0**。本适配以 BinaryTape/a2ui-kmp 的 AndroidX A2UI `1.0.0-alpha02` 源码为基础，构建 OHOS ARM64 协议模型、引擎、Compose Runtime 和渲染器，并提供六种自定义 Catalog 组件。

## 已验证的范围

2026-11-04，在 **OpenHarmony 6.0.0.47 ARM64 模拟器**验证：

- 核心模块编译、本地 Maven 元数据与 KLIB 发布、独立示例解析依赖、生成 so/h、构建并安装 HAP。
- 姓名从 Ada 改为 Lin、人数改为 3，提交得到 `book_table ... {guest=Lin, party=3}`。
- 数据更新将订单改为“可以取餐了”；组件更新插入按钮，点击得到 `confirm_pickup` 事件。
- HAP 进程的 Locale、数字、日期探测 **3/3** 通过。
- HAP 调试进程调用已有测试 KLIB 中的五项回归方法，**5/5** 通过：Locale、Native 锁竞争、JSON Reader、keyed SideEffect、协议事件和 Surface 隔离。

本示例使用预置 JSON，不连接模型或真实预约服务。Catalog 包含 Panel、Heading、Label、Input、Party、Action；未移植全部新版 Material 3 Basic Catalog。当前发行默认仅 OHOS ARM64，x64 代码入口未验证，Desktop/iOS 不属于本适配的默认构建目标。

## 构建

需要 JDK 17+（本次 Gradle daemon 使用 JDK 21）、Gradle 8.14、指定版本的 CPF Kotlin/Native、DevEco Native 工具链及 OpenHarmony SDK 20。框架和三方库从 CPF 文档所配置的 Maven public 仓解析。

```sh
# 工程根目录；编译库、发布到 build/repo、构建示例并打包 HAP
./scripts/build-ohos-demo.sh

# 带五项 HAP 内回归检查的调试配置
./scripts/build-ohos-demo.sh --checks
```

`scripts/gradle-ohos.sh` 优先使用本机缓存的 Gradle 8.14，随后回退到 wrapper。可用 `KOTLIN_NATIVE_HOME` 指定已经安装的 **2.2.21-1.2.0**；没有本机匹配目录时由插件正常解析发行包。首次构建需要下载配套 LLVM 和缺少的 KLIB。

`DEVECO_HOME` 默认 `/Applications/DevEco-Studio.app/Contents`；`NODE_HOME`、`A2UI_HARMONY_STAGE` 可覆盖工具和临时工程位置。SDK 默认 `~/Library/OpenHarmony/Sdk`，可用 `A2UI_OHOS_SDK` 覆盖。Hvigor 拒绝中文路径，因此壳工程在有标记的 ASCII 临时目录中构建。

HAP 输出：`/private/tmp/kai-a2ui-harmony/entry/build/default/outputs/default/entry-default-unsigned.hap`。

```sh
hdc -t <设备> install -r <HAP路径>
hdc -t <设备> shell aa start -a EntryAbility -b com.kai.a2ui
hdc -t <设备> shell pidof com.kai.a2ui
hdc -t <设备> shell hilog -x -P <进程号> -e 'A2UI_'
```

本次模拟器接受 unsigned HAP。真机应按目标设备要求完成签名；工程不包含证书、私钥或密码。

## 改造位置

| 位置 | 工作 |
|---|---|
| `target/build-logic`、`gradle/libs.versions.toml` | 固定版本、OHOS 目标、CPF 依赖和仅本地发布 |
| `target/a2ui-kmp/a2ui-compat/src/ohosMain` | ICU cinterop、时间/地区/格式化、锁、容器、IO 和宿主能力入口 |
| `compose-runtime/overlay-manual`、`src/commonMain` | 回补 Compose 1.9.2 缺少的 keyed SideEffect |
| `compose-ui/overlay-manual` | 同一兼容行为用于 Catalog 日期错误上报 |
| `src/commonTest`、`a2ui-compat/src/ohosTest` | 五项回归检查及 Native 线程实际实现 |
| `ohos-sample` | 六组件 Catalog、双向绑定、事件、动态更新、NAPI/ArkTS 壳 |

本次没有修改 CPF 的 Kotlin、Compose core、Skiko 或 LLVM 仓库。未来若纳入 CPF 公版统一发布，需要另行对齐模块注册、版本、发布集合、CI 与文档。

## 平台与验证边界

- 日期/数字使用公开 ICU API。独立 shell 中的 ICU 数据访问失败；HAP 中本次三个探测均成功。不要以开发机或 shell 的结果替代应用进程验证。
- `OhosA2uiPlatform.configure` 用于注入 URL opener、地区和 24 小时制偏好。未注册 URL opener 会报错；未提供偏好时使用进程默认值。示例没有开放 URL 组件。
- Locale 对无效 BCP 47 标签严格拒绝；日期解析暂不支持 Java 模式 `u`。其他行为边界见 `a2ui-compat/src/ohosMain/README.txt`。
- Native 独立 `test.kexe` 在当前模拟器 shell 缺少 Ark/Panda 运行环境。本次用 `withNativeSelfTests` 调试配置直接链接测试 KLIB，在 HAP 内调用五个测试方法；这不是上游全量测试的自动发现运行。
- 固定工具链的已有静态缓存出现过不匹配链接符号。本工程为验证明确关闭 Native cache，没有替换用户指定的编译器版本。
- `source/` 保留上游快照；修改通过 overlay 与新增平台源码组织。同步上游时须复核 manual overlay 记录的源文件 hash。

## 来源

基于 BinaryTape/a2ui-kmp，原始项目与 AndroidX 源码归属保留在 `LICENSE`、`NOTICE`、`source/UPSTREAM`。鸿蒙壳参考 CPF-KMP-CMP/kmp-cmp-example。

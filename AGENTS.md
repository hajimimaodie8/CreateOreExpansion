# AGENTS.md — 给 AI 协作代理的工程备忘

> 本文件是「记忆索引」：只放**跨会话必须知道的事实、红线、易踩的坑**；长内容一律在 `markdown_output/`，这里只留一行指针（写清「去哪个文件、找哪个标题」）。
> 最后更新：2026-10-02（**装备键（左 Alt）改为开关**：按一下开、再按一下关；W13 盔甲 + 技能换核见「挂起事项」与 `markdown_output/COE 层功能清点与变更史.md`）。
> **指令预算 65,536 B，本文件必须 ≤ 40,000 B** —— 想往这里加长内容之前，先问「这该不该进 `markdown_output/`」。

## 📚 长文档索引（⚠ `markdown_output/`、`docs/`、`tools/` **只在本机保留、不入公共仓库**——见 `.gitignore` 那段；按这里找「文件 + 标题」）

- **技能换核**（已定事实、现状速记）→ `markdown_output/技能内核换核执行方案.md` 的两节（`### A.` / `### B.`）。
- **模块拆分 P1–P3z** → `markdown_output/模块拆分进度与决策链.md` 的 `## 0. 一页时间线` 与 `## 1. 原文逐条`；P3z 那轮的取证在 `build/patch/p3z-EVIDENCE.txt` 第 0 节。
- **变器应力闸门 / 转速分档 / 护目镜读数口径**（2026-09-24/25 定稿全部细则）→ `markdown_output/能量波三态与变器双模式（需求与进度）.md`，找标题 **`## 从 AGENTS.md 迁入（2026-09-25）：变器应力闸门 · 转速分档 · 护目镜读数口径`**。
- **四模块自由组合方案 / 各层依赖矩阵 / 发布策略** → `markdown_output/四模块自由组合（方案评判·风险·工作量）.md`，找 **`## 13. 各层依赖矩阵`**、**`## 15. P3 模块布局定稿`**。
- **CEWS 拆分方案**（耦合点 12 条、模块边界判据、待定项、风险与验收）→ `markdown_output/CEWS 能量波阵学模块（拆分方案与思索）.md`（读全文）。
- **变器配方判定与执行逻辑**（含「给桶注液不是配方」的完整取证）→ `markdown_output/变器配方判定与执行逻辑.md`，找 **`§0.2`**。
- **两层各自的功能清点与变更史**（现在有什么 + 变更时间线，供「历史留存」会话逐轮追加；CEWS 那份 `## 4.` 是**边界**）→ `markdown_output/COE 层功能清点与变更史.md`、`…/CEWS 层功能清点与变更史.md`
- **本次重构的教训**（六次「全绿但坏掉」、依赖方向判据、勘误）→ `markdown_output/分层重构（依赖方向重排）复盘.md`
- **新会话交接提示词** → `markdown_output/新会话交接提示词.md`（COE）/ `…（CEWS）.md`；**工程环境与工具链细节** → `markdown_output/工程环境与工具链细节（AGENTS迁入）.md`
- **技能现状地图 / Skiller API 契约 / 各类取证脚本与日志** → `build/patch/coe_skill_map.md`、`build/patch/skiller_api_foundation.md`、`build/patch/p3?-EVIDENCE.txt`（`build/` 被 git 忽略，重生成即可）。

## 工程速览

- 模组：`createoreexpansion`（Create 6.0.10 附属，NeoForge / Minecraft 1.21.1）
- 根目录：`E:\mc\mcmod\createoreexpansion`，包根 `com.hjmmd_8.createoreexpansion`
- 当前分支：`leaf-dev`
- **工程结构（P7b 后）**：五个 Gradle 子工程 = `core`（JarJar 嵌套共享库，**不是 mod**）、`coe`（mod id `createoreexpansion`）、`cews`（`cews`）、`transmutation`（`transmutation`）、`all-neoforge`（0 文件占位）；分层细节见 `markdown_output/模块拆分进度与决策链.md` §2.6。**根 `src/main/java` 只剩 7 个 dev-only 文件**；共享接线由 core 的幂等入口 `LayerBootstrap#ensureAttached` 承担（每个 `@Mod` 构造器**第一条语句**，`synchronized`——FML 的 mod 构造**并行**派发）。**根自己还是一个 mod：mod id `coe_integration`**。
- 构建与验证流程（本工程一贯用法）：
  1. `.\gradlew.bat compileJava`
  2. 改动语言/资源时：`.\gradlew.bat runData`（同时兼作 Bootstrap / mixin 冒烟测试）
  3. `.\gradlew.bat processResources jar --rerun-tasks`
  4. 校验 `build\libs\createoreexpansion-1.0.0.jar` 的时间与大小，再用 `dsh_im_return_file` 交付给用户
- **测试启动项（dev，按模块组合）**：`runCoeOnly`（独立新实例 `run-coe-only/`）/ `runCoeCews`（**与 `runClient` 共用 `run/`** ⇒ 直接载入已有世界）—— `loadedMods` 收窄本次加载的本模组 mod 文件；⚠ **必须含 `coe_integration`**（dev 里 `core` 由它携带，去掉即 NoClassDefFoundError）；⚠ 共用 `run/` 的启动项**别同时开两个**。加组合照抄 `build.gradle` 的 W13 段。
- 崩溃排查：`run/crash-reports/*.txt`、`run/logs/latest.log`
- 可选依赖（Jade / JEI / CC&A / Create Optical / Vintage Improvements / Aeronautics / Sable）一律通过 `compat/*` 层的 `ModList.isLoaded` + 反射隔离，**绝不在 `content` 包直接 import 可选模组类**。

## 🧯 工程坑与自检（动手前后都用得上）

**1）`.gitignore` 的目录模式必须锚定根目录**
裸写 `data/`、`net` 这类模式会匹配**任意层级**同名目录：曾因此让 `src/main/resources/data/`、`src/generated/resources/data/` 整片被忽略，而 `git add <dir>` 对忽略文件是**静默跳过**（不报错、连 `git status` 都看不见），结果 **81 个数据文件**（64 个 ε/ω 充能配方 + 12 个机器掉落表 + 5 个方块标签）从没进过版本库。现已全部锚定（`/data/`、`/net/`、`/libs/`、`/repo/`、`/_create_src`、`/markdown_output`、`/.run/`）。
自检（改 ignore 规则后必做）：
```
git ls-files --others --ignored --exclude-standard <目录>   # 本该提交却被忽略的文件
git check-ignore -v <路径>                                  # 到底谁在忽略它
git status --short                                          # git add 之后再复核一次暂存区
```

**2）javadoc 检查（本工程 javadoc 任务默认跑不通）**
跑法、为什么必须 `cmd /c` 直连、以及现状（仅剩 3 处错误且全在 `content/skill/*`）→ `markdown_output/工程环境与工具链细节（AGENTS迁入）.md` 的 `## 2.`。

**3）PowerShell 与全库盘点（含会误判事实的编码坑）**
五条最容易反复踩的：① 全库盘点用 `Select-String`（本仓 `grep` 漏文件）；② PS 5.1 按 ANSI 读无 BOM 的 UTF-8 `.ps1` ⇒ 中文注释吞引号，**一次性脚本一律纯 ASCII**；③ 临时文件写 `%TEMP%`/`build/`；④ 抓 javac 中文诊断必须 `cmd /c "... > f 2>&1"` + `-Encoding Default`；⑤ **改 Java/文档只用 write/edit 工具**（`Get-Content|Set-Content` 按 ANSI 重写会毁文件）。**逐条实例与正确命令 → `markdown_output/工程环境与工具链细节（AGENTS迁入）.md` 的 `## 4.`**。

**4）构造期陷阱（2026-09-14 真实崩溃）**
父类构造器会调用**可覆写**方法。`Entity` 的构造器就会调 `setPos(0,0,0)`（另有 `defineSynchedData` / `getBoundingBox` / `getFireImmuneTicks` / `getMaxAirSupply` / `getTeam` / `level()`），而覆写体在**字段初始化器之前**执行 → 碰任何对象字段都是 NPE。波实体曾在 `setPos` 里调 `wavePath.markLiveBreak()`，导致**开炮即服务端崩溃**（`run/crash-reports/crash-2026-09-14_09.47.56-server.txt`）。
**规则：构造期可被调用的覆写体只能使用参数、静态成员与原始类型字段。** 已审计全仓（`Entity` 侧只有 `AbstractChargerWaveEntity` 覆写这两个方法；`BlockEntity` 构造器调的 `getType`/`isValidBlockState`/`validateBlockState`/`getNameForReporting` 在本仓无覆写），现在都合规。

**5）静态关卡覆盖不到什么（三关全绿 ≠ 不会崩 —— 动手前必读）**
`compileJava` / `runData` / javadoc **都不构造实体、不跑 tick、不跑渲染、不装配界面**。以下类别**没有任何静态关卡覆盖**，改到时必须明确告知用户「这一步只能进游戏实测」：

- **实体 / tick / 玩家交互**：实体生命周期（生成、存档、开炮、命中）、tick 逻辑——2026-09-14 的「开炮即崩」就是在这三关全绿的情况下发生的。
- **客户端渲染**：技能预览框、波口指示灯、护目镜档位提示、Jade/查询仪读数——只有进游戏才看得见（技能 W5 渲染器移植的验收条件就是「进游戏看预览是否还在」）。
- **JEI**：`@JeiPlugin` 的类别装配与侧栏顺序在运行期才发生；UID 重复、类别漏装配只有加载时暴露。
- **配置**：`runData` 时**根本不加载配置**；配置键名/容器名写错要到进游戏才暴露，**容器名一变（如 `coe_core-common.toml`）= 老玩家设置静默丢失**。
- **创造页内容同步**：现在**全模组只有一个页**（`base_tab`），成员由两个 Registrate 的 `defaultCreativeTab` 自动填入（`CoeRegistrate` + `CewsRegistrate`），**展示顺序由 `CoeCreativeSections` 的分区规则决定**（不再有手写清单）。加减内容只需两处：在哪一层的 Registrate 注册 + 分区判据族；`LayerCreativeTab.registerAll` 的 `TABS` 列表漏页 = **静默不注册**（无编译错误、无警告）。⚠ 别用 `accept(ItemStack.EMPTY)` 造空行——它抛 `The stack count must be 1`，空行只能在 `getDisplayItems()` 里补。
- **包重叠（JPMS）**：`runData`/`runClient` 覆盖不到——dev 里根与 core 是同一个 mod 文件，只有**真发布 jar 进游戏**才能验。
- **可选依赖「没装也能加载」**：唯一取证方式是**把该模组从 dev 运行时依赖里临时去掉跑一次 `runData`**，日志里该类名 0 命中才算通过。

**5.5）数据包标签目录是单数（`tags/item/`、`tags/block/`）；留档不能放 `build/`**
两点都会**静默失效**：复数目录名的整个文件被忽略；`build/` 被 `.gitignore` 忽略 ⇒ 放那里的留档 `git add` 被静默拒绝。**细则与自检 → `markdown_output/工程环境与工具链细节（AGENTS迁入）.md` 的 `## 5.`**。

## 🚩 红线（不许越，逐条都是踩过的坑）

- **注册命名空间恒 `createoreexpansion`**（存档红线）：命名空间类引用一律指 `CoeCore.REGISTRY_NAMESPACE`，`modLoc` 一律 `CoeCore.modLoc`，日志一律 `CoeCore.LOGGER`（前缀 `[createoreexpansion/]`）；`@Mod` / `@EventBusSubscriber` 上的 `MOD_ID`（17 处）**不许**换成命名空间。**配置注册动作在 :coe，但目标容器仍是 `createoreexpansion`**——否则文件名变 `coe_core-common.toml`，老玩家设置静默丢失。CEWS/TRANS 拆包同理：**命名空间、配置键、语言键、数据包路径一律不许改**（mod id 可以分家）。
- **包粒度（JPMS 硬约束）**：同一个 Java 包**不能同时属于两个 mod 文件**，否则启动期抛 `java.lang.module.ResolutionException`。**一个包要么全在 core/子模块、要么全在根；搬走后根侧同名包必须为空。** 生产里 JarJar 嵌套的库是 PLUGIN 层的真 JPMS 自动模块，所以这条**只有真发布 jar 进游戏能验**。详见 `markdown_output/模块拆分进度与决策链.md` §1.9。
- **依赖接线（照抄，别自创）**：
  - **`evaluationDependsOn` 必需**：跨项目读 `project(':core').sourceSets` / `project(':coe').sourceSets` 之前必须 `evaluationDependsOn(':core')` / `evaluationDependsOn(':coe')`，否则静默落回根工程自己的扩展（路径全错但 `projectDir` 是对的，极易误判）。两行现在都在根 `build.gradle`。
  - **`additionalRuntimeClasspath` 禁止**：它把 core 钉成 **BOOT 层**自动模块（父层只有 JDK boot）⇒ core 看不见 MC/NeoForge/Create，`runData` 崩 `NoClassDefFoundError: ModConfigSpec$Builder`。正确配方 = `evaluationDependsOn(':core')` + 把 core 的 sourceSet 绑进**根 mod 的 `mods{}` 条目**（`neoForge { mods { "createoreexpansion" { sourceSet(sourceSets.main); sourceSet(project(':core').sourceSets.main) } } }`）；`additionalRuntimeClasspath files(...)` 目录形态同样是错的。
  - **根 → `:coe` 必须 `compileOnly(project(':coe'))` + `jarJar(compileOnly(project(':coe')))`**；**用 `implementation` 会让 dev 硬崩**（那个 jar 进 runtimeClasspath 后被 FML 当成第二个 `createoreexpansion` mod 文件 → `duplicate_mod`）。
  - **根必须保留 `jarJar(compileOnly(project(':core')))`**（P4e 改；写 `implementation` 会让带 `FMLModType` 的 core jar 进 dev classpath 抢包，`runData` 崩 `ResolutionException`）。
- **`@EventBusSubscriber` 的作用域是「每个 mod 文件」，不是全局**：`AutomaticEventSubscriber.inject` 只喂本文件的扫描结果并按 `mod.getModId()==modid` 过滤 —— **「住在 A 文件里、却标 B 的 modid」的类会静默不注入，且没有任何警告**。现修法在 `common/hub/IntegrationBootstrap`（走 scan data、不加载类、自维护无名单）。**这项审计必须查两种形态、而且必须在文件搬完之后对搬完的树再跑一遍**：① **正向**（P3w 发现）＝根文件里的类标着某个层的 id；② **镜像**（P3y `TransmutationEventHandler`、P3z `content/energyfield/` 两个类实证）＝**子模块文件里的类标着别的 id**。判据永远是「**类的 modid == 它所在 mod 文件的 id**」——文件一搬，正向就可能变成镜像，两种形态都只静默失效（无警告、无报错、编译全绿）。
- **订阅者类不许按「引用计数」判死**：它只被事件总线调用、grep 引用恒 0。我据此误删过 HIT/USE 两个技能触发点（**六关全绿而技能不触发**）；已加 `tools/check-event-subscribers.ps1`。
- **jar 字节数只在同一个工作树里可比**：`.cache/` 曾被整片塞进发布 jar（`exclude("src/generated/**/.cache")` 从来没匹配过），在临时 worktree 里重建 HEAD 会少 21 KB，**差点被误判成回归**。现已 `exclude(".cache/**")` + `exclude("**/.cache/**")`。
- **`content/` 包不得 import 任何可选模组类**（Curios 只允许出现在 `compat/curios`；条件加载走 `Class.forName` + `ModList.isLoaded`）。可选依赖在 `neoforge.mods.toml` 里**必须 optional**（`curios` 曾是 required，导致没装的玩家在加载阶段直接崩）。**CA（`createaddition`）是唯一例外：第 2、3 层对它是 required，第 4 层 optional。**
- **改动分层 / 注册 / 搬运代码后必须跑分层断言并到 0 违规**（白名单保持为空）。**删类后另跑 `tools/check-stale-imports.ps1`**（javac 对失效 import 惰性、会静默放过）。工具见下一节。

## 🎨 美术资源的红线（用户 2026-09-14 明确要求，必须遵守）

**不要改任何贴图，也不要自己画贴图。** 用户原话：「机壳什么样就什么样，不要改动那个图，一个图都不要改，
自己也不要画，要不然的话，很烦」。因此：

- `src/main/resources/assets/.../textures/**` 视为**用户资产**：发现问题**只报告、只解释，不动文件**；
  也不要"生成一张差不多的"顶替（例如给灯做"熄灭版"贴图这类替代方案，**必须先问**，默认不做）。
- **模型（`models/**/*.json`）与贴图不是一回事**：几何坐标（元素位置/尺寸/UV 窗口）在用户明确指定数值时
  可以改——本会话就按用户给的"开口内缩 1px / 关闭 2px"改过 16 个变体模型；但 UV 指向的贴图内容不许动。
- 贴图相关的判断（亮块在哪几个像素、哪一列是透明）**靠量**：`build/patch/dump_light_squares.ps1`
  逐像素列出亮块坐标；`build/patch/rasterize_top_face.ps1` 把顶面每像素最终采到的贴图像素画成字符图
  ——"某盏灯只亮一半"这类问题靠它一眼定位（实测：东灯 UV `[12,7,14,9]` 采到透明列，应为 `[13,7,15,9]`）。

## 🔧 工具（一行一个；细节都写在脚本头注释里）

- `tools/check-layering.ps1` —— 分层方向断言（**扫五个根**：`src/main/java` + `core/` + `coe/` + `cews/` + `transmutation/` 各自的 `src/main/java`；层 = COE/CEWS/TRANS/SHARED/**CORE**；禁止 `COE→CEWS`、`COE→TRANS`、`TRANS→CEWS`、`CEWS→TRANS`、`CORE→*`）。跑法：`powershell -NoProfile -ExecutionPolicy Bypass -File tools\check-layering.ps1`（**本机没有 `pwsh`**）。改动分层/注册/搬运后**必须跑到 0 违规**；也是「搬运是否干净」的体检工具（拿它跑 `git archive HEAD` 树对比）。**每拆一层要把该层加进两个脚本的 roots 表**——不加则那层计数变 0 而脚本照样 exit 0（四次同形状的静默逃逸）。
- `tools/layer-usage.ps1` —— 依赖普查（每个文件被哪几层引用、哪些 SHARED 文件传递地不碰层专属代码）；产物 `build/patch/layer-usage.txt`、`core-candidates*.txt`、`core-packages.txt`、`package-usage.txt`。**与 `check-layering.ps1` 的分层规则必须逐字一致**（改一处两处同改）。
- `tools/check-package-overlap.ps1` —— **包重叠审计（JPMS）**：同包跨两模块 = 启动即 `ResolutionException`，三关全绿看不到；改包结构/拆层/加 `FMLModType` 后必跑到 0。
- `tools/check-module-selfsufficiency.ps1` —— **模块 jar 自足性关卡**（92 条断言，只读，exit 0/1）。断言清单与逐条口径**写在脚本头注释里**（含 P7a 的 mixin 洞、`compat/jei` 的 `@JeiPlugin`、根 lang 完整拷贝、221 条生成配方按层分发 61/160/0、跨层标签拆半、**X2 冻结序**、`Class.forName` 三形态扫描）；**每条都配反空转守护**（匹配 0 条即红）。
- `tools/check-package-heritage.ps1` —— 包名血统**报告**（基准 `fbf33cdf~1`，`-Base` 可覆盖）：列「被迫/非被迫」改名并计账，**exit 0**；非被迫的要人工判。
- `tools/check-skill-render-coverage.ps1` —— 策略渲染覆盖关卡（注册调用数 == 带渲染器的策略数 / 漏注册渲染器 = 预览静默消失 / 两个 outline 渲染器都得有槽位门）。改渲染器必跑。
- `tools/check-asset-attribution.ps1` —— **资产归属关卡**（只读，exit 0/1）：blockstates/models 里指向 `createoreexpansion:` 的 `parent`/`model`/`textures.*` 必须解析到本模块**或本模块声明 required 的**模块（**判据必须认识依赖方向**，零白名单；`cews → coe` 合法、`coe → cews` 非法）。附 A2/A3/A5 与 15 条反空转守护；**孤儿只报告不判红**。
- `tools/check-data-attribution.ps1` —— **跨层数据引用扫描**（**观测型，exit 0**）：唯一红源是 23 条反空转守护；报载体覆盖率、两路索引覆盖、跨模块引用表、`UNINDEXED` 明细。**实测跨模块数据引用 = 0 对**。
- `build/patch/w6d-run-matrix.ps1` —— **发布形态验收矩阵驱动**（14 用例；`-DryRun` 看期望）。⚠ **不要用 `-LaunchMode Direct`**：对"必须到达 Done"的用例会给**假 FAIL**（C1 实测 Direct 下 `reached-Done=0`、gradle 下 `Done (5.784s)`）。需人做的 5 个用例 → `build/patch/human-cases-D-E-F1.md`。
- `build/patch/*` 取证脚本（被 git 忽略，按需重生成）：`ChargerBandCheck.java` / `BandCheck.java`（转速分档逐整数比对）、`dump_light_squares.ps1` / `rasterize_top_face.ps1`（贴图取证）、`p3?-EVIDENCE.txt`（拆模块各阶段取证）。
- `tools/prepare-run-published.ps1` —— **发布形态测试台**：清空 `<gameDir>/mods` → 装三个模块**发布 jar** + 必需第三方（清单由 Gradle 任务 `w3ThirdPartyModJars` 生成，取自缓存、**不下载**）→ 写 `eula.txt`/`server.properties`/sha256 清单。配套 run = 根 `build.gradle` 的 `publishedServer`/`publishedClient`（不绑 sourceSet + `loadedMods` 置空 ⇒ **只从 `mods/` 加载**，机理与源码出处见该段头注释）；作业单 `build/patch/w3-user-checklist.md`。

## 🌊 波系统与变器：现行口径（要点；细则见「长文档索引」）

- **"给桶注液"不是配方**：Create 的 19 条内置 `create:filling` 配方**没有一条以空桶为原料**（真机走 `GenericItemFilling` 特判，借物品自身的 `Capabilities.FluidHandler.ITEM`）。已补货载旁路 `content/charger/craft/WaveItemFluidFilling`（用量与成品一律委托 Create，只认载荷流体，不耗链数）。**取证全文（19 条逐条 + 为什么不是类型门缺陷）→ `变器配方判定与执行逻辑.md` §0.2。**
- **储存模式释放档位**：口径只有一处 `AbstractCreateChargerBlockEntity#resolveReleaseLevel(int)`——有应力取**当前档位**（与普通模式一致），停转才回落"最后一次有效档位"（否则停转后囤的层数一发都放不出）。蓝宝石用 `storedLevel` 做回落值；星辉石等级是持久手动设置，直接用 `getManualLevel()`。
- **波系统日志唯一出口**：`content/charger/wave/WaveDiag`（`[变体波轨迹]` = 事件流，默认开；`[变体波加工]` = 逐条淘汰，默认关）。实体侧 `craftTrace` / `craftDebug` 只是它的委托包装——**新代码要写波相关日志一律走 WaveDiag**，别再自建前缀。
- **变器"哪些面算波口"是机器本地概念**：`StellarWaveTransmuterBlock#isOpenable(BlockState, Direction)` 按"该世界方向能否映射到本机模型的一个侧面"判定（口径唯一处 `propertyForWorld`）。**别再写成"世界水平方向"**——本机六向放置，躺倒时自己的 4 个侧面里有 2 个朝上下，旧写法会让那 2 个口"显示开着却点不掉"。
- **波波碰撞与波级/波型无关**（用户明确、长期口径）：任意两波相交即 `triggerBoom(爆炸等级 = min)` + 相互湮灭，范围伤害/范围充能加工、不破坏地形；几何上不可能"高速对穿而不触发"（相对步长 ≤1.2 格/tick < 判定窗口 2×0.6）。改碰撞效果前先看 `handleWaveCollision`，那里的日志会打印等级对与实际粒子数。
- **波情"五要素"的唯一取值点**（用户 2026-09-14 定稿）：波速 → 波级 → 波载荷 → 波型 → **剩余寿命**。显示只有**两处**——Jade 波实体提示（`compat/jade/WaveJadePlugin`，要素块之后才是可加工清单/电荷）与波情查询仪的动作栏读数（`content/wave/gauge/WaveReadout#line()` 的词条 `wave_gauge.readout`）。寿命口径 = `AbstractChargerWaveEntity#getRemainingLifetime()`（200 tick 上限 − 已存活）÷ 20，**两处共用这一处取值**；它是时间寿命，飞满 64 格的距离上限不折算进来。查询仪是**动态**的（扫描动画 32 tick 内 `inventoryTick` 每 tick 刷新，见 §28.3），Jade 每帧重建。要加/改要素就改这两处 `...show/hide`，别再新开第三条显示路径。
- **变器波口指示灯几何**（用户 2026-09-14 定稿）：灯片 **2×2 px**、**开口时离方块边缘 1 px / 关闭时 2 px**（沿顶面往内侧挪，非高度方向）；**尺寸/y/UV/贴图选择都不随状态变**。完整口径（16 个变体模型逐变体编码、变体 index 状态位 `NORTH=8/SOUTH=4/WEST=2/EAST=1`、差波器 overlay 走法）→ `markdown_output/机器交互实现细节（AGENTS迁入）.md` 的 `## 5.`。
- **变器应力闸门 · 转速门槛 · 转速分档 · 护目镜读数（2026-09-24/25 全部定稿）** —— 细则**已整体迁到** `markdown_output/能量波三态与变器双模式（需求与进度）.md` 的标题 **`## 从 AGENTS.md 迁入（2026-09-25）：变器应力闸门 · 转速分档 · 护目镜读数口径`**（含 A 应力闸门与转速门槛 / B `util/SpeedBands` 唯一实现 / C 护目镜档位提示 / D 过期计数更正）。**只留 5 条最容易写歪的**：
  1. **应力闸门唯一判据** = `StellarWaveTransmuterBlockEntity#isWavePowered()`；**未接入应力时变器对波的属性零改变**，闸门只掐「赋属性」、不碰「让不让波过」。
  2. **转速门槛由模式自报** `TransmuterMode#minimumRpm()`：加工态 **0**（**别写 1**）、攻击态 **128**；按**绝对值**判定。
  3. **转速分档唯一实现 = `util/SpeedBands`**（禁内联三元 + 魔法数字；判定与显示共用同一张表）；**档位下限量化必须 `ceil`，不许 `round`/`floor`**（`round` 会把 213 RPM 挪档、直接改玩法）——改公式后重跑 `build/patch/ChargerBandCheck.java`。
  4. **显示层零档数/阈值/半径常量**：攻击态一档一行、当前档亮白；充能器一档一行、当前档用**该档光芒色**；行序以机器名开头；**别再合并成一条长对照行**。
  5. **"转速不足"不自己写文案**：覆写 BE 的 `isSpeedRequirementFulfilled()` 让 Create 自己打两行（零新增翻译键）；场节拍固定 **2 tick**（`FIELD_INTERVAL_FLOOR_TICKS`）。

## 🧱 模块拆分：结论与踩坑（P1–P3z；全过程见 `markdown_output/模块拆分进度与决策链.md`）

- **现状（P7b 后）**：`:coe` / `:cews` / `:transmutation` 都是真子模块，各自带 `mods.toml`，由根 `jarJar(compileOnly(project(':…')))` 嵌进 `META-INF/jarjar/` 第 1 层；`core` 是 JarJar 嵌套共享库（**不是 mod**）；`all-neoforge` 仍是 0 文件占位。dev 的 `neoForge.mods{}` 四个条目 = 四个本模组 mod 文件。细节 → 同文档 §2.6。
- **集成层去哪（P3z 定论）：根工程自己是一个 mod，mod id `coe_integration`**（入口 `common/hub/IntegrationMod`）。**为什么必须如此**：FML 对 `[[mods]]`/`@Mod` 是双向硬约束（有条目没类＝静默无入口；有类没条目＝`dangling_entrypoint` 硬错）；而根模板为空就没有 `ModContainer` ⇒ `AutomaticEventSubscriber` 不注入，根侧 datagen / `IntegrationBootstrap` / Ctrl+扳手 / 配置重载集体静默失效。被否的另两个选项与代价 → `markdown_output/模块拆分进度与决策链.md` §2.7。
- **⛔ P3g 那句「拆任何一层都不可能」已被 P3w 推翻**（别再引用）：拐点是承认「**`@Mod` 入口是集成文件**」。`coe/build.gradle` 只有 `compileOnly(project(':core'))`、**没有** `project(':')` ⇒ **`:coe:compileJava` 通过本身就是「零根引用」的机器证明**（`:cews` / `:transmutation` 同理，三者都没有 `project(':')`）。
- **注册器已按三层分家**：`AllBlocks` / `AllItems` / `AllBlockEntityTypes` **已删除**，声明住在 `common/registry/{coe,cews,transmutation}/`；**注册触发顺序必须在 `CreateOreExpansion` / `IntegrationBootstrap` 里显式写死**（没有兜底）。**别和 Create 自己的同名 `AllBlocks` / `AllItems` 搞混**（批量改名要做「裸名解析」判定）。
- **P4a/P4b/P4c 的机制，与「拆 lang 时不许把根文件改小」的 HashCache 坑** → `markdown_output/模块拆分进度与决策链.md` 标题 **`## 2. 从 AGENTS.md 迁入（P7b，2026-09-27）`**。**铁律仍在：根 lang 文件一个字节都不许动**。
- **P7b：mixin 与语言按层自足**（机制全文 → 同文档 **§2.8**）：mixin **配置跟着类走**（全在 `:coe`，**根侧配置与声明都已删**；无 mixin 类的层**不声明、不造空配置**）；lang 每层落一份根文件完整拷贝。
- **108 生成标签与 221 生成配方都已自足**（P7c/P7d）：按注入的 `id→层` 表逐文件改道（单层归模块、跨层各写一份靠**合并**语义还原），豁免 `purgeStale` 的 `/tags/` 与 `/recipe/`；**不许按路径判层**（两层命名空间相同 ⇒ 路径逐字同类）。⇒ ⑥ 已闭合。机制全文 → `markdown_output/模块拆分进度与决策链.md` §2.8。
- **手写 `data/**` 按层自足（P7c）**：「只引用某一层」的进该层模块，「全原版/外部或横跨两层」的住 `core/src/main/resources/data/**`，由 `coe-conventions`+根各一行复制进每个模块 jar；根侧 `src/main/resources/data/**` 必须为空。
- **`runData` 的 `written: 0` 不是健康判据** → 同文档 §2.4。
- **待查**：`runData` 偶发 `Found unused register callbacks`（现在约 7 次 2 次），日志里 sable 的 mixin 仍被装载 ⇒ `build.gradle` 对 runData 的 sable/aeronautics 排除**没有真正生效**。
- **「全模组共用的东西」必须住 SHARED/`core`**：被两层以上用的契约/登记表若住在某一层，就会让另一层反向 import（禁止方向）。
- **搬迁名单按「整包」生成、不能按文件；差一个文件就整包不合格** → 同文档 §2.5；工具见「工具」一节。
- **P7a 收口**：`:coe` 的 `Class.forName("…compat.curios.CurioMedallionBridge")` / `"…compat.jade.BasinLiveJadePlugin"` 字面量与那两个类同住 `:coe`（原先类在根 ⇒ 单装 coe.jar 静默把 6 个凝能佩降级）；硬时序（须在 `CoeItems.register()` 之前）未变。
- **一条通用判据**：**core 里的方法只要描述符里出现 MC 类型就不可用**（哪怕方法体 MC-free）——`NoClassDefFoundError` 抛在调用点那一帧；「core 能不能放这个类」只能用**真调用一次**验证。
- 历史细节（P3b–P3w、P3h–P3v 取证）→ 同文档。

## 📐 其它现行口径（系列特性 / 机器交互 / 语言键）

- **物品注册只有一个入口 `CoeItems`**（用户 2026-10-01 裁定）：本模组物品（工具/佩/弓/盔甲/回旋镖…）**声明与能量注册全在 `CoeItems`**；**同族物品的注册链一律收进族级 helper**（`gemIngot/gemRaw/.../stressMedallion/armor/grindingWheel/skillItem`），声明处**一行一件**——别再逐件复制同一段链（曾 57 处逐字重复）。能量走现成路径（`EnergyItemBuilder` 的 `defaultEnergy == maxEnergy` = 初始即满）。**创造页分栏也在这条链上声明**：`.transform(CoeCreativeSections.section(CreativeSection.ORE/MACHINE/GEAR))`（声明优先于规则；见 `CoeCreativeSections`）。**加物品前先把 `CoeItems` 读一遍**。
- **系列特性登记口径**（用户 2026-09-15 定稿）：**唯一入口 = `common/SeriesTraits`**；判定 = 物品标签 ∪ 系列方块标签 ∪ 注册名约定（限定本模组命名空间）。**四个系列标签由 datagen 生成，手写文件禁止同名**（同名让 processResources 报 duplicate 直接失败）；**两个系列（含方块物品）免疫嬗乱销毁**，判定在 `TransmutationDisorderEffect#canTransmutationDestroy`（W6 后随嬗化住 `:coe`）——**方块物品进不了物品标签，故不能用标签覆盖**。链式写法与来由 → `markdown_output/机器交互实现细节（AGENTS迁入）.md` 的 `## 6.`。
- **机器交互四条统一规则**（用户 2026-09-15 定稿）：① 空手右键某个面 = 开/关该面开口；② 空手右键指示灯 = 只切那盏灯对应的开口；③ 扳手右键 = 有特殊模式的机器只切模式、没模式的机器照旧切开口；④ 旋转必须 **Ctrl + 扳手右键**。
  实现三件套（契约 `common/machine/MachineInteraction` / 载荷 `common/machine/MachineRotatePayload` / 客户端 `client/MachineRotateClient`）的逐条细节、来由与 `IWrenchable` 副作用更正 → `markdown_output/机器交互实现细节（AGENTS迁入）.md`。**Ctrl 的判定必须在客户端**：使用物品包不带修饰键、Ctrl 也不同步（只有潜行会同步），服务器根本读不到；客户端拦截 + 自定义包才能让"没按 Ctrl 就不旋转"成为服务端权威行为。
- **装备键（左 Alt）= 开关**（2026-10-02 改，原为按住）：按一下开、再按一下关；开时工具技能键（键一/二/三）让位给装备技能。判定唯一处 `CoeSkillClient#isEquipmentModeOn()`（`consumeClick()` 边沿翻转、离世复位），关卡 `check-armor-sets.ps1` §25。
- **中英语言键集必须对齐**：`assets/createoreexpansion/lang/{en_us,zh_cn}.json` 的键集差集**只允许**是 4 条中文侧覆盖 Create 自带键的本地化（`create.tooltip.holdForControls` / `holdForDescription` / `keyCtrl` / `keyShift`）。历史上英文曾漏 17 条（雷鸣合金整条材料线 + 能量场控制器 + 蓝宝石充能器/两个调节器 + 嬗变液方块与流体），英文客户端在这些条目上显示原始键名——已在 `d869e2d2` 补齐。自检（**`Get-Content` 必须带 `-Encoding UTF8`**，否则 PS 5.1 按 ANSI 读中文 JSON，会在中文引号处解析失败并吐出一大坨乱码）：
  ```powershell
  $en = Get-Content src\generated\resources\assets\createoreexpansion\lang\en_us.json -Raw -Encoding UTF8 | ConvertFrom-Json
  $zh = Get-Content src\generated\resources\assets\createoreexpansion\lang\zh_cn.json -Raw -Encoding UTF8 | ConvertFrom-Json
  Compare-Object @($en.PSObject.Properties.Name) @($zh.PSObject.Properties.Name)
  ```

## ⏸ 挂起事项（重要，动手前必读）

**技能内核移植（2026-09-19 已开工，进行中）**：用 Leaf 的独立内核 **Skiller**（<https://github.com/lizhanyu-leaf/Skiller>，本地副本 `E:\mc\mcmod\_ref\Skiller`，MIT）替换本模组自研的技能框架。
**现状（2026-09-30 第 1–8 阶段收口）**：9/9 技能在新内核；**旧执行层已删净**（旧技能实现类、策略类、handler、旧释放入口 `SkillsComponent#releaseSkills/releaseSkillAt`、迁移闸门、整套技能属性修饰机制）。旧技能树 **64 → 34**，剩下的是**数据与契约**：14 个 `*Config(s)`（新内核的数值真源）、7 个上下文类型、以及 `DataSkill`（id 载体）/`MetadataSkill`（元数据壳）/`SkillItemStack`/`SkillsComponent`/`AllKeys`/tooltip。
**必须记住的几条**（细则同上）：
- **id 与翻译键零改动**：技能 id 一律 `createoreexpansion:xxx`，三个 `SkillType` 沿用 `excavation_skill`/`hit_skill`/`use_skill`，12 条中英 lang 键一条不改；旧数据组件 `createoreexpansion:skills` 与 NBT 格式不变（`tools/check-save-format.ps1` + `check-skill-registry-parity.ps1` 守着；老存档兼容）。
- **不要 Skiller 的「按 R 启用技能」总开关**：客户端进世界自动 `ClientSkillCache.enable(...)`、换主手 `refresh(player)`，并 `setToggleKeysEnabled(false)`；服务端释放走 `CoeSkillRelease` 直接读 `PlayerPressedKeys`。
- **内置的是我方 fork 版**（`_ref/Skiller` 的 `coe-embed` 分支；上游对我方只读，push 403 ⇒ 分支只在本地）——**升级步骤 / 上游基线 `cebb47b` / 冲突口径 / 全量补丁 → `docs/skiller-embed/skiller-embed-README.md`**（补丁已实测可复现）。⚠ 留档别放 `build/`（`.gitignore:3` 忽略，`git add` 静默拒绝）。
- **skiller 只由 `:coe` 内置**（2026-09-30 用户裁定）：根 `build.gradle` 用 `compileOnly` + `runtimeOnly`（后者**仅供 dev 的 runData/runClient**，缺它被 FML 拒载）。
- **扣能时机易写歪**：`consumeResource` 必须先过 `CoeSkillSupport.willDoWork(...)`；**冷却类技能要在 `consumeResource` 与 `release` 两处同判**。
- **拆任何一层的前后都要做 `@EventBusSubscriber` 的「文件/modid 错配」审计，两种形态都查**（见红线）。

**CEWS 模块（能量波阵学）—— 拆包已完成；⚠ 边界与本文旧版相反**：CEWS = Create: Energy Wave Studies（机械动力：能量波阵学），独立内置 jar（mod id `cews`）。
- **现行边界（W6-c/W9 之后）**：**第一层 COE** = 矿物/宝石/工具/技能 + **波引擎**（`content/charger/{wave,craft,entity,payload}/**`、`content/wave/api/**`、`content/energyfield/**`）+ **三台应力充能器** + **三种机壳** + **嬗化全部**；**第二层 CEWS** = **除充能器以外的波机器**（差波器 / 调级器 / 波速调节器 / 变器 / 场控制器 / 查询仪）。**依赖方向是唯一判据**：第一层需要的必须住第一层；靠窄契约反向补出来的全部撤掉了。
- **创造页：全模组只剩一个（2026-09-30 用户裁定）**——`createoreexpansion:base_tab` = **一页 + 三条分区横幅**（矿物/机械/装备）。**CEWS 的 `energy_wave_study` 页与其搬运逻辑 `EnergyWaveStudyTab` 已删**；该层物品改由 `CewsRegistrate#defaultCreativeTab(CoeCreativeTabs.BASE_TAB.key())` 直接进本页（12 台波机器落**机械**分区）。**加减内容只改两处**：注册点 + `CoeCreativeSections` 的判据族。⚠ 横幅的坑（`accept(EMPTY)` 抛异常 / 图集只扫 `gui/sprites/` / 自适应整行空）→ `docs/共享经验-盔甲与材料集/08-…md` §6；关卡 `tools/check-creative-sections.ps1`。
- **第三层 `transmutation` 是空壳**（用户 2026-09-28 裁定）：mod 已注册（对 `createoreexpansion` required），**内容全部并入第一层**；**「善化」模块单独只属第一层**，善化系列的新加工配方（含今后的扩展）归第三层——**第三层只是对第一层「善化」的扩展**。空壳仍必须调 `LayerBootstrap.ensureAttached(modBus)`（构造器第一条语句）。
- **拆包红线**：注册命名空间必须保持 `createoreexpansion`（mod id 可分家），否则所有 id 全变、老存档报废；**配置键、语言键、数据包路径同理不许改**。英文名 `Create: Energy Wave Studies`（缩写不变）。
- 耦合点与方案 → `markdown_output/CEWS 能量波阵学模块（拆分方案与思索）.md`；**拆包后的方向判据与六次「全绿但坏掉」→ `markdown_output/分层重构（依赖方向重排）复盘.md`**。

**第一层：矿物拓展（COE）—— 已落地**：`:coe` 就是它（mod id 仍是 `createoreexpansion`，它就是"本体"）。**依赖方向是唯一判据**：谁需要它，它就必须住在谁那里；CEWS 与 transmutation 都对它 required，反向禁止。

## 🧠 记忆写入规则（用户明确要求，务必遵守）

**只有用户明确要求时才把内容写入 Hindsight 记忆库；不允许每轮对话自动导入。**

用户 2026-09-10 的原话诉求：「每次在需要我植入要求的情况下，才会自动导入到记忆库中，而不是每对一次话就导入一次」。为此已在 `C:\Users\Lenovo\.hindsight\coding-agent.json` 关闭三项自动导入：

| 开关 | 值 | 关掉的是什么 |
|---|---|---|
| `retainSessions` | `false` | 每轮 `agent/turn-stopping` 的会话转写写回（即"每对一次话就导入一次"） |
| `autoSeed` | `false` | 每个仓库首次的 git 历史播种（默认上限 300 个提交） |
| `codebaseSurvey` | `false` | 代码库普查（默认带 $2 预算上限） |

仍然开着的（都是**只读**，成本低）：`autoReflect` 默认 true（**每个会话一次**的回忆，不是导入）、`pageRefreshEveryTurns: 10`（知识页列表 GET）。若要更省可一并关掉。

**因此的后果与做法**：

- **会话结束不再自动入库**。想让某段对话/结论进记忆，必须由用户提出，然后用 `hindsight_ingest_document`；成体系的计划用 `hindsight_capture_initiative`。
- 手动工具**不受这些开关影响**（已核源码 dsh.js:16375 —— `hindsight_ingest_document` 直接调 `client.retain`，不经过 `retainSessions` 判断），所以"按需入库"这条路是通的。
- **改动在下一个新会话才生效**：插件把每个 workspace 的运行时对象缓存在进程级 Map 里（dsh.js:17238 `workspaces`，:17244 命中即返回），配置只在对象创建时读取一次。
- 别依赖 `hindsight_ingest_document` 工具描述里那句"会话结束会自动捕获"——那是默认配置下的行为，这里已被关掉。

## 已知环境限制

- Hindsight 记忆、`maven.neoforged.net` 可达性与 Skiller 参考工程的构建方式 → `markdown_output/工程环境与工具链细节（AGENTS迁入）.md` 的 `## 3.`。

# AGENTS.md — 给 AI 协作代理的工程备忘

> 本文件是「记忆索引」：只放**跨会话必须知道的事实、红线、易踩的坑**；长内容一律在 `markdown_output/`，这里只留一行指针（写清「去哪个文件、找哪个标题」）。
> 最后更新：2026-09-27（P7b：mixin 配置随类进 `:coe`、根侧配置与声明已删；每层落一份根 lang 完整拷贝；新增 `tools/check-module-selfsufficiency.ps1` 自足性关卡。原 68,930 B 的长叙述已迁入 `markdown_output/`）。
> **指令预算 65,536 B，本文件必须 ≤ 40,000 B** —— 想往这里加长内容之前，先问「这该不该进 `markdown_output/`」。

## 📚 长文档索引（要查细节，先按这里找「文件 + 标题」）

- **技能换核**（15 条已定事实、技能系统现状速记、W5/W6 收尾清单）→ `markdown_output/技能内核换核执行方案.md`，找标题 **`## 从 AGENTS.md 迁入（2026-09-25）`** 的两节（`### A.` 已定事实 1–15、`### B.` 技能系统现状速记）；旧分析 `markdown_output/技能内核对比与迁移分析.md` 只作参考。
- **模块拆分 P1–P3z**（时间线、决策表、逐条原文、P3h–P3z 取证索引）→ `markdown_output/模块拆分进度与决策链.md`，找标题 **`## 0. 一页时间线（TL;DR）`** 与 **`## 1. 从 AGENTS.md 迁入（2026-09-25）：原文逐条`**；**P3z 那一轮「根工程拆完 CEWS 之后是什么」的完整方案（3 个选项 + 各自代价 + 选择理由 + 遗留）在 `build/patch/p3z-EVIDENCE.txt` 第 0 节**（`build/` 被忽略，重跑该轮即可重生成）。
- **变器应力闸门 / 转速分档 / 护目镜读数口径**（2026-09-24/25 定稿全部细则）→ `markdown_output/能量波三态与变器双模式（需求与进度）.md`，找标题 **`## 从 AGENTS.md 迁入（2026-09-25）：变器应力闸门 · 转速分档 · 护目镜读数口径`**。
- **四模块自由组合方案 / 各层依赖矩阵 / 发布策略** → `markdown_output/四模块自由组合（方案评判·风险·工作量）.md`，找 **`## 13. 各层依赖矩阵`**、**`## 15. P3 模块布局定稿`**。
- **CEWS 拆分方案**（耦合点 12 条、模块边界判据、待定项、风险与验收）→ `markdown_output/CEWS 能量波阵学模块（拆分方案与思索）.md`（读全文）。
- **变器配方判定与执行逻辑**（含「给桶注液不是配方」的完整取证）→ `markdown_output/变器配方判定与执行逻辑.md`，找 **`§0.2`**。
- **技能现状地图 / Skiller API 契约 / 各类取证脚本与日志** → `build/patch/coe_skill_map.md`、`build/patch/skiller_api_foundation.md`、`build/patch/p3?-EVIDENCE.txt`（`build/` 被 git 忽略，重生成即可）。

## 工程速览

- 模组：`createoreexpansion`（Create 6.0.10 附属，NeoForge / Minecraft 1.21.1）
- 根目录：`E:\mc\mcmod\createoreexpansion`，包根 `com.hjmmd_8.createoreexpansion`
- 当前分支：`leaf-dev`
- **工程结构（P7b 后）**：五个 Gradle 子工程 = `core`（JarJar 嵌套共享库，**不是 mod**）、`coe`（mod id `createoreexpansion`）、`cews`（`cews`）、`transmutation`（`transmutation`）、`all-neoforge`（0 文件占位）；各层文件数与逐项机制见 `markdown_output/模块拆分进度与决策链.md` §2.6 与下节。**根 `src/main/java` 只剩 7 个 dev-only 文件**；共享注册表挂载 / 旋转载荷 / 配方类型唤醒顺序由 core 的幂等入口 `common/registry/LayerBootstrap#ensureAttached` 承担（每个 `@Mod` 构造器**第一条语句**，`synchronized`——FML 的 mod 构造**并行**派发），创造页由每层 `LayerCreativeTab.registerAll(...)` 自持。**根自己还是一个 mod：mod id `coe_integration`**。
- 构建与验证流程（本工程一贯用法）：
  1. `.\gradlew.bat compileJava`
  2. 改动语言/资源时：`.\gradlew.bat runData`（同时兼作 Bootstrap / mixin 冒烟测试）
  3. `.\gradlew.bat processResources jar --rerun-tasks`
  4. 校验 `build\libs\createoreexpansion-1.0.0.jar` 的时间与大小，再用 `dsh_im_return_file` 交付给用户
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
javadoc 用平台编码（本机 GBK）读 UTF-8 源码 → 满屏乱码 + 假告警，任务从建立起就是红的，于是编译不校验的文档缺陷长期无人发现。可用做法：
```
.\gradlew.bat javadoc        # 仅为生成 build\tmp\javadoc\javadoc.options
cmd /c ""%JAVA_HOME%\bin\javadoc.exe" @build\patch\javadoc_utf8.options -d build\patch\jd_out -Xdoclint:all,-missing -J-Duser.language=en -J-Duser.country=US > build\patch\jd.log 2>&1"
```
- options 文件需追加 `-encoding UTF-8 -docencoding UTF-8`；classpath 在同一文件首行（`-classpath '...'`，注意把 `\\` 还原成 `\`）。
- 必须 `cmd /c` 直连：PowerShell 的 `*>` 会把 javadoc 的 stderr 包成 NativeCommandError 记录并按宽度折行，把绝对路径切成碎片、毁掉诊断行。
- 现状（2026-09-14）：13 死链 + 6 处 HTML + 13 处非法 `@param` 已清，**仅剩 3 处错误且全在 `content/skill/*`**（按挂起事项未动）。

**3）PowerShell 与全库盘点（含会误判事实的编码坑）**
- 全库文字盘点用 `Select-String`：**本仓 `grep` 会漏文件**（实测只扫出 71 行，实际更多）。
- PS 5.1 读**无 BOM 的 UTF-8 `.ps1`** 会按 ANSI 解码：脚本里的中文注释可能吞掉引号 → ParserError。**一次性脚本一律写纯 ASCII**（中文用 `[regex]::Unescape('\uXXXX')`）。
- 临时文件写 `%TEMP%` 或 `build/`（已忽略），别落进仓库——`git add -A` 会把它带进去。
- **`.\gradlew ... 2>&1 | Select-Object` 会把 javac 的 GBK 中文诊断解成乱码**（`����: �� Color`）；要 `cmd /c "... > file 2>&1"` + `Get-Content -Encoding Default`；**`Select-String` 匹配中文也必须显式 `-Encoding Default`**（有一次因此把「@Mod 构造器已跑」误判成「没跑」）。
- **改 Java / 文档一律只用 write/edit 工具**：用 `Get-Content | -replace | Set-Content` 改文件时 PS 5.1 按 ANSI 重写，曾把 `SkillerIntegration.java` 改到语法崩掉（48 个编译错误），只能 `git checkout --` 重做。

**4）构造期陷阱（2026-09-14 真实崩溃）**
父类构造器会调用**可覆写**方法。`Entity` 的构造器就会调 `setPos(0,0,0)`（另有 `defineSynchedData` / `getBoundingBox` / `getFireImmuneTicks` / `getMaxAirSupply` / `getTeam` / `level()`），而覆写体在**字段初始化器之前**执行 → 碰任何对象字段都是 NPE。波实体曾在 `setPos` 里调 `wavePath.markLiveBreak()`，导致**开炮即服务端崩溃**（`run/crash-reports/crash-2026-09-14_09.47.56-server.txt`）。
**规则：构造期可被调用的覆写体只能使用参数、静态成员与原始类型字段。** 已审计全仓（`Entity` 侧只有 `AbstractChargerWaveEntity` 覆写这两个方法；`BlockEntity` 构造器调的 `getType`/`isValidBlockState`/`validateBlockState`/`getNameForReporting` 在本仓无覆写），现在都合规。

**5）静态关卡覆盖不到什么（三关全绿 ≠ 不会崩 —— 动手前必读）**
`compileJava` / `runData` / javadoc **都不构造实体、不跑 tick、不跑渲染、不装配界面**。以下类别**没有任何静态关卡覆盖**，改到时必须明确告知用户「这一步只能进游戏实测」：

- **实体 / tick / 玩家交互**：实体生命周期（生成、存档、开炮、命中）、tick 逻辑——2026-09-14 的「开炮即崩」就是在这三关全绿的情况下发生的。
- **客户端渲染**：技能预览框、波口指示灯、护目镜档位提示、Jade/查询仪读数——只有进游戏才看得见（技能 W5 渲染器移植的验收条件就是「进游戏看预览是否还在」）。
- **JEI**：`@JeiPlugin` 的类别装配与侧栏顺序在运行期才发生；UID 重复、类别漏装配只有加载时暴露。
- **配置**：`runData` 时**根本不加载配置**；配置键名/容器名写错要到进游戏才暴露，**容器名一变（如 `coe_core-common.toml`）= 老玩家设置静默丢失**。
- **创造页内容同步**：每层的 `XxxCreativeTabs` 各持一份手写 `TABS` 列表，**新增页忘了加进列表 = 静默不注册**（无编译错误、无警告）；`EnergyWaveStudyTab#CONTENTS` 同理。
- **包重叠（JPMS）**：`runData`/`runClient` 覆盖不到——dev 里根与 core 是同一个 mod 文件，只有**真发布 jar 进游戏**才能验。
- **可选依赖「没装也能加载」**：唯一取证方式是**把该模组从 dev 运行时依赖里临时去掉跑一次 `runData`**，日志里该类名 0 命中才算通过。

**5.5）数据包标签目录是单数：`tags/item/`、`tags/block/`，写错就静默失效**
写成 `tags/items/`（复数）= 一个"名叫 items 的注册表标签目录"，游戏不认识，**整个文件被忽略且不报错**。
实例：`transmutation_protected.json` 曾在 `tags/items/` 里 → `stack.is(TRANSMUTATION_PROTECTED)` 恒为假
（龙蛋/下界之星/信标其实从没被保护）。2026-09-15 已挪回单数目录。
自检：`runData` 只校验 `src/generated/**`，手写数据文件放错目录**没有任何关卡会报**——只能目视核对路径。
另：手写数据文件与 datagen 产出**同名就会让 `processResources` 报 duplicate 而构建失败**
（本仓造过一次：手写的系列标签 vs 新加的 `.tag(...)` 注册）——两者只能留一个。

## 🚩 红线（不许越，逐条都是踩过的坑）

- **注册命名空间恒 `createoreexpansion`**（存档红线）：命名空间类引用一律指 `CoeCore.REGISTRY_NAMESPACE`，`modLoc` 一律 `CoeCore.modLoc`，日志一律 `CoeCore.LOGGER`（前缀 `[createoreexpansion/]`）；`@Mod` / `@EventBusSubscriber` 上的 `MOD_ID`（17 处）**不许**换成命名空间。**配置注册动作在 :coe，但目标容器仍是 `createoreexpansion`**——否则文件名变 `coe_core-common.toml`，老玩家设置静默丢失。CEWS/TRANS 拆包同理：**命名空间、配置键、语言键、数据包路径一律不许改**（mod id 可以分家）。
- **包粒度（JPMS 硬约束）**：同一个 Java 包**不能同时属于两个 mod 文件**，否则启动期抛 `java.lang.module.ResolutionException`。**一个包要么全在 core/子模块、要么全在根；搬走后根侧同名包必须为空。** 生产里 JarJar 嵌套的库是 PLUGIN 层的真 JPMS 自动模块，所以这条**只有真发布 jar 进游戏能验**。详见 `markdown_output/模块拆分进度与决策链.md` §1.9。
- **依赖接线（照抄，别自创）**：
  - **`evaluationDependsOn` 必需**：跨项目读 `project(':core').sourceSets` / `project(':coe').sourceSets` 之前必须 `evaluationDependsOn(':core')` / `evaluationDependsOn(':coe')`，否则静默落回根工程自己的扩展（路径全错但 `projectDir` 是对的，极易误判）。两行现在都在根 `build.gradle`。
  - **`additionalRuntimeClasspath` 禁止**：它把 core 钉成 **BOOT 层**自动模块（父层只有 JDK boot）⇒ core 看不见 MC/NeoForge/Create，`runData` 崩 `NoClassDefFoundError: ModConfigSpec$Builder`。正确配方 = `evaluationDependsOn(':core')` + 把 core 的 sourceSet 绑进**根 mod 的 `mods{}` 条目**（`neoForge { mods { "createoreexpansion" { sourceSet(sourceSets.main); sourceSet(project(':core').sourceSets.main) } } }`）；`additionalRuntimeClasspath files(...)` 目录形态同样是错的。
  - **根 → `:coe` 必须 `compileOnly(project(':coe'))` + `jarJar(compileOnly(project(':coe')))`**；**用 `implementation` 会让 dev 硬崩**（那个 jar 进 runtimeClasspath 后被 FML 当成第二个 `createoreexpansion` mod 文件 → `duplicate_mod`）。
  - **根必须保留 `jarJar(compileOnly(project(':core')))`**（P4e 改；写 `implementation` 会让带 `FMLModType` 的 core jar 进 dev classpath 抢包，`runData` 崩 `ResolutionException`）。
- **`@EventBusSubscriber` 的作用域是「每个 mod 文件」，不是全局**：`AutomaticEventSubscriber.inject` 只喂本文件的扫描结果并按 `mod.getModId()==modid` 过滤 —— **「住在 A 文件里、却标 B 的 modid」的类会静默不注入，且没有任何警告**。现修法在 `common/hub/IntegrationBootstrap`（走 scan data、不加载类、自维护无名单）。**这项审计必须查两种形态、而且必须在文件搬完之后对搬完的树再跑一遍**：① **正向**（P3w 发现）＝根文件里的类标着某个层的 id；② **镜像**（P3y `TransmutationEventHandler`、P3z `content/energyfield/` 两个类实证）＝**子模块文件里的类标着别的 id**。判据永远是「**类的 modid == 它所在 mod 文件的 id**」——文件一搬，正向就可能变成镜像，两种形态都只静默失效（无警告、无报错、编译全绿）。
- **jar 字节数只在同一个工作树里可比**：`.cache/` 曾被整片塞进发布 jar（`exclude("src/generated/**/.cache")` 从来没匹配过），在临时 worktree 里重建 HEAD 会少 21 KB，**差点被误判成回归**。现已 `exclude(".cache/**")` + `exclude("**/.cache/**")`。
- **`content/` 包不得 import 任何可选模组类**（Curios 只允许出现在 `compat/curios`；条件加载走 `Class.forName` + `ModList.isLoaded`）。可选依赖在 `neoforge.mods.toml` 里**必须 optional**（`curios` 曾是 required，导致没装的玩家在加载阶段直接崩）。**CA（`createaddition`）是唯一例外：第 2、3 层对它是 required，第 4 层 optional。**
- **改动分层 / 注册 / 搬运代码后必须跑分层断言并到 0 违规**（白名单保持为空）。工具见下一节。

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

- `tools/check-layering.ps1` —— 分层方向断言（**扫五个根**：`src/main/java` + `core/` + `coe/` + `cews/` + `transmutation/` 各自的 `src/main/java`；层 = COE/CEWS/TRANS/SHARED/**CORE**；禁止 `COE→CEWS`、`COE→TRANS`、`TRANS→CEWS`、`CEWS→TRANS`、`CORE→*`）。跑法：`powershell -NoProfile -ExecutionPolicy Bypass -File tools\check-layering.ps1`（**本机没有 `pwsh`**）。改动分层/注册/搬运代码后**必须跑到 0 违规**；也是「搬运是否干净」的体检工具（拿它跑 `git archive HEAD` 树做对比）。**每拆一层就要把该层加进两个脚本的 roots 表**——不加的后果是那一层计数变 0 而脚本照样 exit 0（P3d-β core / P3w coe / P3y transmutation / P3z cews 四次同一形状的静默逃逸）。
- `tools/layer-usage.ps1` —— 依赖普查（每个文件被哪几层引用、哪些 SHARED 文件传递地不碰层专属代码）；产物 `build/patch/layer-usage.txt`、`core-candidates*.txt`、`core-packages.txt`、`package-usage.txt`。**它与 `check-layering.ps1` 的分层规则（`Get-FileLayer` 函数体）必须逐字一致**（改一处要两处同改）。
- `tools/check-package-overlap.ps1` —— **包重叠审计（JPMS）**：同包跨两模块 = 启动即 `ResolutionException`，三关全绿看不到；改包结构/拆层/加 `FMLModType` 后必跑到 0。
- `tools/check-module-selfsufficiency.ps1` —— **模块 jar 自足性关卡**（45 条断言，只读，exit 0/1；跑法同其它工具）。断言：jar 内 `[[mixins]]` 条数 == `*.mixins.json` 个数**且** config 点名的类在同一 jar（有 mixin 类而 0 条声明 = 红，即 P7a 的洞）、`compat/jei/**` ≥1 个 `@JeiPlugin`（`javap -v` 复核）、jar 自带根 lang 的完整拷贝（逐键逐值比对）、`[[mods]]` 为空的 core 无 `@EventBusSubscriber`、模块侧不碰 `LayerBootstrap` 的共享接线。
- `build/patch/*` 取证脚本（被 git 忽略，按需重生成）：`ChargerBandCheck.java` / `BandCheck.java`（转速分档逐整数比对）、`dump_light_squares.ps1` / `rasterize_top_face.ps1`（贴图取证）、`p3?-EVIDENCE.txt`（拆模块各阶段取证）。

## 🌊 波系统与变器：现行口径（要点；细则见「长文档索引」）

- **"给桶注液"不是配方**：Create 的 19 条内置 `create:filling` 配方里**没有一条以空桶为原料**；真机的流体喷口走 `GenericItemFilling` 特判（借物品自身的 `Capabilities.FluidHandler.ITEM`）。所以"波拿不到注液配方"不是类型门/流体门槛的缺陷，而是**口径缺失**——已补货载旁路 `content/charger/craft/WaveItemFluidFilling`（用量与成品一律委托 Create，只认载荷流体，不耗链数）。详见 `变器配方判定与执行逻辑.md` §0.2。
- **储存模式释放档位**：口径只有一处 `AbstractCreateChargerBlockEntity#resolveReleaseLevel(int)`——有应力取**当前档位**（与普通模式一致），停转才回落"最后一次有效档位"（否则停转后囤的层数一发都放不出）。蓝宝石用 `storedLevel` 做回落值；星辉石等级是持久手动设置，直接用 `getManualLevel()`。
- **波系统日志唯一出口**：`content/charger/wave/WaveDiag`（`[变体波轨迹]` = 事件流，默认开；`[变体波加工]` = 逐条淘汰，默认关）。实体侧 `craftTrace` / `craftDebug` 只是它的委托包装——**新代码要写波相关日志一律走 WaveDiag**，别再自建前缀。
- **变器"哪些面算波口"是机器本地概念**：`StellarWaveTransmuterBlock#isOpenable(BlockState, Direction)` 按"该世界方向能否映射到本机模型的一个侧面"判定（口径唯一处 `propertyForWorld`）。**别再写成"世界水平方向"**——本机六向放置，躺倒时自己的 4 个侧面里有 2 个朝上下，旧写法会让那 2 个口"显示开着却点不掉"。
- **波波碰撞与波级/波型无关**（用户明确、长期口径）：任意两波相交即 `triggerBoom(爆炸等级 = min)` + 相互湮灭，范围伤害/范围充能加工、不破坏地形；几何上不可能"高速对穿而不触发"（相对步长 ≤1.2 格/tick < 判定窗口 2×0.6）。改碰撞效果前先看 `handleWaveCollision`，那里的日志会打印等级对与实际粒子数。
- **波情"五要素"的唯一取值点**（用户 2026-09-14 定稿）：波速 → 波级 → 波载荷 → 波型 → **剩余寿命**。显示只有**两处**——Jade 波实体提示（`compat/jade/WaveJadePlugin`，要素块之后才是可加工清单/电荷）与波情查询仪的动作栏读数（`content/wave/gauge/WaveReadout#line()` 的词条 `wave_gauge.readout`）。寿命口径 = `AbstractChargerWaveEntity#getRemainingLifetime()`（200 tick 上限 − 已存活）÷ 20，**两处共用这一处取值**；它是时间寿命，飞满 64 格的距离上限不折算进来。查询仪是**动态**的（扫描动画 32 tick 内 `inventoryTick` 每 tick 刷新，见 §28.3），Jade 每帧重建。要加/改要素就改这两处 `...show/hide`，别再新开第三条显示路径。
- **变器波口指示灯几何**（用户 2026-09-14 定稿，改动在 16 个 `stellarstone_wave_transmuter_<i>.json` 里逐变体编码）：灯片 **2×2 px**，**开口时离方块边缘 1 px、关闭时 2 px**（沿顶面往机器内侧挪，非高度方向）；尺寸/y/UV/贴图选择都不随状态变。变体 index 的状态位是 `NORTH=8 / SOUTH=4 / WEST=2 / EAST=1`（`CewsBlocks` datagen）。差波器家族走另一条路——独立 overlay 模型、只在开口时绘制、恒 1 px 内缩。
- **变器应力闸门 · 转速门槛 · 转速分档 · 护目镜读数（2026-09-24/25 全部定稿）** —— 细则**已整体迁到** `markdown_output/能量波三态与变器双模式（需求与进度）.md` 的标题 **`## 从 AGENTS.md 迁入（2026-09-25）：变器应力闸门 · 转速分档 · 护目镜读数口径`**（含 A 应力闸门与转速门槛 / B `util/SpeedBands` 唯一实现 / C 护目镜档位提示 / D 过期计数更正）。**只留 5 条最容易写歪的**：
  1. **应力闸门唯一判据** = `StellarWaveTransmuterBlockEntity#isWavePowered()`；**未接入应力时变器对波的属性零改变**，闸门只掐「赋属性」、不碰「让不让波过」。
  2. **转速门槛由模式自报** `TransmuterMode#minimumRpm()`：加工态 **0**（**别写 1**）、攻击态 **128**；按**绝对值**判定。
  3. **转速分档唯一实现 = `util/SpeedBands`**（禁内联三元 + 魔法数字；判定与显示共用同一张表）；**档位下限量化必须 `ceil`，不许 `round`/`floor`**（`round` 会把 213 RPM 挪档、直接改玩法）——改公式后重跑 `build/patch/ChargerBandCheck.java`。
  4. **显示层零档数/阈值/半径常量**：攻击态一档一行、当前档亮白；充能器一档一行、当前档用**该档光芒色**；行序以机器名开头；**别再合并成一条长对照行**。
  5. **"转速不足"不自己写文案**：覆写 BE 的 `isSpeedRequirementFulfilled()` 让 Create 自己打两行（零新增翻译键）；场节拍固定 **2 tick**（`FIELD_INTERVAL_FLOOR_TICKS`）。

## 🧱 模块拆分：结论与踩坑（P1–P3z；全过程见 `markdown_output/模块拆分进度与决策链.md`）

- **现状（P7b 后）**：`:coe` / `:cews` / `:transmutation` 都是真子模块（细节见同文档 §2.6），各自带 `src/main/templates/META-INF/neoforge.mods.toml`，由根 `build.gradle` 的 `jarJar(compileOnly(project(':…')))` 嵌进 `META-INF/jarjar/` 第 1 层；`core` 是 JarJar 嵌套共享库（不是 mod）；`all-neoforge` 仍是 0 文件占位。dev 的 `neoForge.mods{}` 四个条目 = **四个本模组 mod 文件**（`coe_integration` = 根输出 + core 输出、`coe`、`cews`、`transmutation`）。
- **集成层去哪（P3z 定论）：根工程自己是一个 mod，mod id `coe_integration`**，入口 `common/hub/IntegrationMod`（构造器为空，接线仍由 `IntegrationBootstrap` 在 `FMLConstructModEvent` 上做）。**为什么必须如此**：CEWS 搬走后根里一个本模组 @Mod 都不剩，而 FML 对 `[[mods]]`/`@Mod` 是双向硬约束（有条目没类＝静默无入口；有类没条目＝`dangling_entrypoint` 硬错）；让根模板 `[[mods]]` 为空同样不行——没有 `ModContainer` 就没有 `AutomaticEventSubscriber` 注入，根侧 datagen 入口 / `IntegrationBootstrap` / Ctrl+扳手 / 配置重载会集体静默失效。被否掉的另两个选项与代价 → 同文档 §2.7。**根模板现状**：`[[mods]]` = 1（`coe_integration`）、`[[dependencies.*]]` = 0（内容需求由三个嵌套 mod 各自声明）。
- **⛔ P3g 那句「拆任何一层都不可能」已被 P3w 推翻**（别再引用）：拐点是承认「**`@Mod` 入口是集成文件**」。`coe/build.gradle` 只有 `compileOnly(project(':core'))`、**没有** `project(':')` ⇒ **`:coe:compileJava` 通过本身就是「零根引用」的机器证明**（`:cews` / `:transmutation` 同理，三者都没有 `project(':')`）。
- **注册器已按三层分家**：`AllBlocks` / `AllItems` / `AllBlockEntityTypes` **已删除**（`ccaf8a21`），声明住在 `common/registry/{coe,cews,transmutation}/`；**注册触发顺序必须在 `CreateOreExpansion` / `IntegrationBootstrap` 里显式写死**（没有兜底），每个层类的空 `register()` 只是类初始化触发器。**别和 Create 自己的 `com.simibubi.create.AllBlocks` / `AllItems` 搞混**（批量改名要做「裸名解析」判定）。
- **P4a/P4b/P4c 的机制，与「拆 lang 时不许把根文件改小」的 HashCache 坑** → `markdown_output/模块拆分进度与决策链.md` 标题 **`## 2. 从 AGENTS.md 迁入（P7b，2026-09-27）`**。**铁律仍在：根 lang 文件一个字节都不许动**。
- **P7b：mixin 与语言按层自足**（机制全文 → 同文档 **§2.8**）。mixin **配置跟着类走**——7 个 mixin 类 + 配置插件全在 `:coe`，故配置是 `coe/src/main/resources/createoreexpansion.mixins.json` + coe 模板的 `[[mixins]]`；**根侧配置与声明都已删**，`:cews`/`:transmutation` 无 mixin 类 ⇒ 不声明、不造空配置。机制：FML 按**每个文件自己的 `[[mixins]]`** 登记（`LoadingModList.addMixinConfigs → file.getMixinConfigs()`），config 名走**类路径资源**（`MixinConfig.create → contextClassLoader`）。语言：`LayerLangSplitter` 现**另给每层落一份根 lang 的完整拷贝**（同路径、逐条复制）——lang 按整条 `getResourceStack` 逐键 `put`，内容一致 ⇒ 合装结果不变、单装不再露原始键名。
- **221 生成配方 + 108 生成标签仍由集成层提供**（出处≠声明）：`CreateOreExpansionDatagen` 直挂 `RecipeProvider`；标签仍是单一 union 提供器，但**落盘时按注入的 `id→层` 表逐文件改道**（单层归模块、跨层按层各写一份，靠**合并**语义还原），并豁免 `purgeStale` 的 `/tags/`。
- **`runData` 的 `written: 0` 不是健康判据**（判据是 `git status` 与内容）→ 同文档 §2.4。
- **待查**：`runData` 偶发 `Found unused register callbacks`（现在约 7 次 2 次），日志里 sable 的 mixin 仍被装载 ⇒ `build.gradle` 对 runData 的 sable/aeronautics 排除**没有真正生效**。
- **「全模组共用的东西」必须住 SHARED/`core`**：被两层以上用的契约/登记表若住在某一层，就会让另一层反向 import（禁止方向）。
- **搬迁名单按「整包」生成、不能按文件；差一个文件就整包不合格** → 同文档 §2.5；工具见「工具」一节。
- **P7a 收口**：`:coe` 的 `Class.forName("…compat.curios.CurioMedallionBridge")` / `"…compat.jade.BasinLiveJadePlugin"` 字面量与那两个类现在同住 `:coe`（原先类在根 ⇒ 单装 coe.jar 静默把 6 个凝能佩降级）；硬时序（须在 `CoeItems.register()` 之前）未变。
- **一条通用判据**：**core 里的方法只要描述符里出现 MC 类型就不可用**（哪怕方法体 MC-free）——`NoClassDefFoundError` 抛在调用点那一帧；「core 能不能放这个类」只能用**真调用一次**验证。
- 历史细节（P3b/P3c/P3d-α/β/P3e/P3f/P3g/P3m/P3w 的逐条原文与提交号、P3h–P3v 取证索引）全部在 `markdown_output/模块拆分进度与决策链.md`。

## 📐 其它现行口径（系列特性 / 机器交互 / 语言键）

- **系列特性登记口径**（用户 2026-09-15 定稿）：common/SeriesTraits 是唯一入口——方块 .transform(SeriesTraits.addStellarstoneTraits()/addThunderiteTraits())、物品链上 .tag(AllModItemTags.STELLARSTONE_ITEMS/THUNDERITE_ITEMS)（Java 没有扩展方法，物品侧写不出 .addXxx()）；判定 = 物品标签 ∪ 系列方块标签 ∪ 注册名约定（限定本模组命名空间）。**四个系列标签由 datagen 生成，手写文件禁止同名**（同名会让 processResources 报 duplicate 直接失败）。两个系列（含方块物品）免疫嬗乱销毁，该判定在 TransmutationDisorderEffect#canTransmutationDestroy 里调 SeriesTraits——方块物品进不了物品标签，故不能用标签覆盖。
- **机器交互四条统一规则**（用户 2026-09-15 定稿）：① 空手右键某个面 = 开/关该面开口；② 空手右键指示灯 = 只切那盏灯对应的开口；③ 扳手右键 = 有特殊模式的机器只切模式、没模式的机器照旧切开口；④ 旋转必须 **Ctrl + 扳手右键**。
  实现三件套：契约 **`common/machine/MachineInteraction`**（原 `content/machine/CewsMachine`，P2 改名并从 CEWS 挪到共享层；`hasModeSwitch()`；`onWrenched` 默认"吞掉但不旋转"，防 Create 默认旋转在没按 Ctrl 时生效；`rotateAsCreate()` 直接复用 `IWrenchable` 默认旋转、不复制逻辑；`onEmptyHandPortToggle`）、载荷 **`common/machine/MachineRotatePayload`**（服务端校验：本模组机器 + 无模式 + 手持扳手 + 距离）、客户端 `client/MachineRotateClient`（Ctrl 判定后取消本地交互并发包）。
  **为什么 Ctrl 必须在客户端判定**：使用物品包不带修饰键、Ctrl 也不同步（只有潜行会同步），服务器根本读不到；客户端拦截 + 自定义包才能让"没按 Ctrl 就不旋转"成为服务端权威行为。
  副作用修复：`StellarWaveTransmuterBlock` 以前**没实现 `IWrenchable`**（`onWrenched` 没有 `@Override`）→ Create 的扳手从来没分发到它，变器切模式其实一直不生效；现已随契约修好。
- **中英语言键集必须对齐**：`assets/createoreexpansion/lang/{en_us,zh_cn}.json` 的键集差集**只允许**是 4 条中文侧覆盖 Create 自带键的本地化（`create.tooltip.holdForControls` / `holdForDescription` / `keyCtrl` / `keyShift`）。历史上英文漏了 **17 条**（雷鸣合金整条材料线 11 条 + 能量场控制器 + 蓝宝石充能器/两个调节器 + 嬗变液方块与流体），英文客户端在这些条目上显示原始键名——已在 `d869e2d2` 补齐，英文名沿用「与 Stellarstone 同构」的规律。自检（**`Get-Content` 必须带 `-Encoding UTF8`**，否则 PS 5.1 按 ANSI 读中文 JSON，会在中文引号处解析失败并吐出一大坨乱码）：
  ```powershell
  $en = Get-Content src\generated\resources\assets\createoreexpansion\lang\en_us.json -Raw -Encoding UTF8 | ConvertFrom-Json
  $zh = Get-Content src\generated\resources\assets\createoreexpansion\lang\zh_cn.json -Raw -Encoding UTF8 | ConvertFrom-Json
  Compare-Object @($en.PSObject.Properties.Name) @($zh.PSObject.Properties.Name)
  ```
  顺带：`block…jade_stress_charger` 的英文曾误写为 "Jade Create Charger"，已与护目镜条目统一为 "Jade Stress Charger"。

## ⏸ 挂起事项（重要，动手前必读）

**技能内核移植（2026-09-19 已开工，进行中）**：用 Leaf 的独立内核 **Skiller**（<https://github.com/lizhanyu-leaf/Skiller>，本地副本 `E:\mc\mcmod\_ref\Skiller`，MIT）替换本模组自研的技能框架。
**现状一句话**：9/9 技能已迁移进内核，**旧框架一个没删**（回退路径 + 客户端预览仍靠旧渲染器）；**剩余 W5/W6** = 把 `client/tool/**` 的渲染器适配成 Skiller 的 `StrategyRenderer` 并注册进 `StrategyRenderers`（顺带必须修上游 `schedule()` 判错对象那个 bug），然后才删旧框架 + 跑一次专用服务端验证。
**全部决策、15 条已定事实、技能系统现状速记见 `markdown_output/技能内核换核执行方案.md` 的标题 `## 从 AGENTS.md 迁入（2026-09-25）`**（旧分析 `技能内核对比与迁移分析.md` 只作参考）；现状地图 `build/patch/coe_skill_map.md`、API 契约 `build/patch/skiller_api_foundation.md`。
**必须记住的几条**（细则同上）：
- **id 与翻译键零改动**：技能 id 一律 `createoreexpansion:xxx`，三个 `SkillType` 沿用 `excavation_skill`/`hit_skill`/`use_skill`，12 条中英 lang 键一条不改；旧数据组件 `createoreexpansion:skills` 与 NBT 格式原样保留（老存档天然兼容，不需要 DataFixer）。
- **不要 Skiller 的「按 R 启用技能」总开关**：客户端进世界自动 `ClientSkillCache.enable(...)`、换主手 `refresh(player)`，并 `setToggleKeysEnabled(false)`；服务端释放走 `CoeSkillRelease` 直接读 `PlayerPressedKeys`。
- **内置的是我方 fork 版**（`_ref/Skiller` 的 `coe-embed` 分支，补丁 `build/patch/skiller-coe-embed.patch`，**未 push**）——升级 Skiller 必须重放补丁。
- **扣能时机易写歪**：`consumeResource` 必须先过 `CoeSkillSupport.willDoWork(...)`；**冷却类技能要在 `consumeResource` 与 `release` 两处同判**。
- **拆任何一层的前后都要做 `@EventBusSubscriber` 的「文件/modid 错配」审计，两种形态都查**（见红线）。

**CEWS 模块（能量波阵学，进行中）**：能量波系统要从矿物拓展里独立成一个板块 **CEWS = Create: Energy Wave Studies（机械动力：能量波阵学）**，最终形态是**独立的内置 jar**（JarJar 嵌套模块）。
- **阶段 0 已完成**：创造标签页 `createoreexpansion:energy_wave_study`（顺序 **矿物拓展 → 能量波阵学 → Create 调色板**）；**"什么属于 CEWS"的唯一清单** = `common/registry/cews/EnergyWaveStudyTab#CONTENTS`（**17 项**；**强化避雷针按用户裁定留在矿物页**）；内容同步走 `BuildCreativeModeTabContentsEvent`（往新页放 + 从基础页剔除，注册代码未动）——**加/减机器只改这份清单**。
- **阶段 1/2 已落地（P3z）**：`cews/src/main/java` **133 文件**，`CewsMod` 是它的 `@Mod` 入口、已嵌进根 jar 第 1 层；`cews/build.gradle` = `compileOnly(project(':core'))` + `compileOnly(project(':coe'))` + **自己重抄的外部编译面**（create/ponder/registrate + jei + flywheel-api + createaddition + jade + 本地 jar optical/vintage），**没有 `project(':')`** ⇒ `:cews:compileJava` 通过 = 「零根引用」的机器证明。逐条取证见 `markdown_output/CEWS 能量波阵学模块（拆分方案与思索）.md` §7。
- **拆包红线**：注册命名空间必须保持 `createoreexpansion`（mod id 可以是 `cews`），否则所有方块/物品/配方/标签 id 全变、老存档报废；配置键、语言键、数据包路径同理不许改。
- **英文名建议用复数**：`Create: Energy Wave Studies`（学科名英文习惯用 Studies；缩写 CEWS 不变）。id 一律用 `energy_wave_study`，与英文名解耦。
- 全部耦合点（12 条）、模块边界判定、3 个待定项、风险清单与验收标准见 `markdown_output/CEWS 能量波阵学模块（拆分方案与思索）.md`——**动手前先读那份**。

**第二个模块：矿物拓展（长期计划，仅登记，不要动手）**：将来把现有本体（矿物/宝石/水晶芽/工具/技能/嬗变液/雷鸣合金）也收成独立模块（暂称 COE）。**CEWS 单向依赖 COE**（材料），反向禁止。**不能与技能换核并行**（两者都要动注册与 `foundation/item/skill`）。

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

- **Hindsight 记忆已可用**（2026-09-10 起配置）：`C:\Users\Lenovo\.hindsight\coding-agent.json`，`serverMode: cloud`，本仓库 bank 为 `coding-agent::createoreexpansion`。**但已按用户要求关闭全部自动导入**，写入规则见上面「记忆写入规则」一节。
- **`maven.neoforged.net` 现在可达**（2026-09-19 实测 HTTP 200；早先的「连接被重置」是暂时的）。因此 **Skiller 参考工程已能在本机构建**：`cd E:\mc\mcmod\_ref\Skiller && .\gradlew.bat jar`（第一次会下载 neoform-runtime 2.0.24 等，需几分钟）。首次构建偶发 `Connection reset`，重跑一次即可。内置用的 jar 由这条命令产出。

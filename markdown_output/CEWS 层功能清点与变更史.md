# CEWS 层（第二层 / `cews`，能量波阵学）功能清点与变更史

> **这份文件是干什么的**：它是「**CEWS 层现在有什么**」+「**它怎么变成现在这样的**」的单一索引。
> 与 COE 那份同规矩（`markdown_output/COE 层功能清点与变更史.md`）：
> **只追加不改写**（要更正就追加一条「更正：…」）、**每条变更必带提交号**、**数字必须可复跑**（§1 给了量法）。
>
> **分工**：工程红线/坑 → `AGENTS.md`；发布说明 → `RELEASE.md`；本次重构的教训 → `markdown_output/分层重构（依赖方向重排）复盘.md`；**本文件只做「CEWS 功能清单 + 变更时间线」**。
>
> 生成时间：**2026-09-29**（数字全部由当时的树实测，不是回忆）。

---

## 0. 一句话定位

**CEWS = 第二层**，mod id **`cews`**（**注册命名空间仍是 `createoreexpansion`** —— 存档红线），是**独立的内置 jar**（JarJar 嵌套，深度 1）。

- **依赖方向**：`cews` 对 `createoreexpansion`（= 第一层 COE）声明 **required** ⇒ **L2 → L1 合法，反向禁止**。
  ⇒ 只装 `cews.jar` 会被 FML **在加载期拒载**（实测：`Mod ID: 'createoreexpansion', Requested by: 'cews'`）；这是**设计内的行为**，不是缺陷。
- **它自己不带 COE**：`compileOnly(project(':coe'))`、**不 jarJar**（COE 是独立发布的 mod，嵌套一份会重复）。

---

## 1. 规模（实测，含量法）

| 项 | 数 | 量法 |
|---|---|---|
| CEWS 层 Java 文件 | **75** | `Get-ChildItem cews\src\main\java -Recurse -File -Filter *.java \| Measure-Object` |
| 包数 / 分层关卡计数 | **18 包** / **CEWS=75** | `tools\check-package-overlap.ps1`、`tools\check-layering.ps1` |
| 手写资源（`cews/src/main/resources`） | assets **497** / data **1** | 递归计数 |
| 生成资源（`cews/src/generated/resources`） | assets **28** / data **13** | 递归计数 |
| 语言键（**两条路径**） | `assets/cews/lang` = **14/14**（中英）；`assets/createoreexpansion/lang` = **403/407**（根 lang 完整拷贝） | `ConvertFrom-Json` 后数属性 |
| `cews.jar` | **657,597 B** | `Get-Item cews\build\libs\cews-1.0.0.jar` |

> ⚠ **语言是两条路径，别混**：`assets/cews/lang/*` 只有 **14 条**（CEWS 自己的**命名空间子集**，即它新增/改动的键）；而 `assets/createoreexpansion/lang/*` 是**根 lang 的完整拷贝**（403/407 条）—— 这是 P7b 的设计（每层落一份完整拷贝），**不是重复劳动**。
> ⚠ `cews.jar` 字节数只在同一个工作树里可比（重建会有几十字节差异）。

---

## 2. 注册内容（按注册顺序，实测）

### 2.1 方块/机器 **11** 台 —— `common/registry/cews/CewsBlocks.java`

| # | id | 归类 |
|---|---|---|
| 1 | `energy_field_controller` | 能量场控制器（能量场：加速 / 偏转 / 赋能） |
| 2 | `stellar_wave_transmuter` | 星辉波变器（普通波 ↔ 全能波 / 攻击波） |
| 3–5 | `energy_wave_regulator` `sapphire_wave_regulator` `stellarstone_wave_regulator` | **能量调级器 ×3**（穿过即顺/逆基准升降一级） |
| 6–8 | `wave_speed_regulator` `sapphire_speed_regulator` `stellarstone_speed_regulator` | **波速调节器 ×3**（穿过即加减波速） |
| 9–11 | `energy_wave_disperser` `six_face_disperser` `octa_energy_wave_differencer` | **差波器家族 ×3**（四面 / 六面 / 八面：分配 / 转向 / 降级 / 分裂） |

### 2.2 物品 **3** 个 —— `common/registry/cews/CewsItems.java`

`energy_mechanism`（能量构件）、`incomplete_energy_mechanism`（未完成的能量构件）、`wave_query_gauge`（波情查询仪）

### 2.3 注册类（8 个）

`CewsRegistrate`（本层 Registrate 实例 ⇒ **决定 datagen 判层**）、`CewsBlocks`、`CewsItems`、`CewsBlockEntityTypes`、`CewsCreativeTabs`、`CewsJeiCategories`、`CewsMod`（`@Mod` 入口）、`EnergyWaveStudyTab`（**"什么属于 CEWS"的唯一清单**）。

---

## 3. 子系统（按包，文件数实测）

| 包 | 文件 | 职责 |
|---|---|---|
| `content/wave/block` | **25** | 波机器方块本体（最大的一块） |
| `content/wave/regulation` | 6 | 调级器 / 波速调节器的共享逻辑 |
| `content/wave/frame` / `content/wave/gauge` | 2 / 3 | 波机器框架与**波情查询仪**（五要素读数，`WaveReadout#line()`） |
| `content/machine/stellarwavetransmuter` | 7（+`display` 1 / `payload` 1 / `registry` 3 / `scan` 1） | 变器：模式、端口、显示、网络载荷、扫描 |
| `content/machine/energyfieldcontroller` | 2 | 能量场控制器机器 —— ⚠ **与第一层的「场引擎」（`coe` 的 `content/energyfield/**`）是两个东西** |
| `client/cews` / `client/renderer/cews` / `client/renderer/wave` | 2 / 2 / 4 | 客户端拦截与波渲染 |
| `compat/createaddition` / `compat/optical` / `compat/vintageimprovements` / `compat/jei/cews` | 2 / 1 / 3 / 2 | 可选依赖隔离层（**`content/` 包不得 import 可选模组类**） |

---

## 4. ★ 边界：CEWS 现在还剩什么、什么已被裁定搬走

### 4.1 已被裁定搬去第一层的（**别再搬回来**）

| 搬走的 | 提交 | 为什么 |
|---|---|---|
| **波引擎 71 个 L1 文件**（`content/charger/{wave,craft,entity,payload}/**`、`content/wave/api/**`、`content/energyfield/**`） | `0a774eb4`（W6-c） | 第一层需要它们 ⇒ 它们必须住第一层（依赖方向是唯一判据） |
| **三种机壳**（`jade_casing` / `sapphire_casing` / `stellarstone_casing`）+ 其资源 | `f1c07323`（W9） | 用户裁定「三个机壳归属于 COE，而不归属于 CEWS」 |
| **三台应力充能器**（`CoeChargerBlocks`）+ 71 个资源 | `0a774eb4`（Java）+ `6e82fdb1`（资源） | 同上：第一层需要 |
| **两个能量构件**（`energy_mechanism` / `incomplete_energy_mechanism`）的**展示位** | `fc07c161`（W12） | 用户裁定：归矿物页（**注册点未动**，走 `EnergyWaveStudyTab#BASE_PAGE_ITEMS` 展示层搬运） |

### 4.2 CEWS 从第一层读什么（L2→L1，**允许方向**，实测 24 个导入去重）

```
common.registry.coe.CoeBlocks / CoeChargerBlocks / CoeCreativeTabs / CoeRecipeTypes
common.CoeCore
content.charger.entity.{AbstractChargerWaveEntity, ChargerWaveFx, StellarWaveEntity, WavePath}
content.charger.payload.WavePayloadGather
content.charger.wave.{WaveDiag, WaveMachineHandler, WaveMachineHandlers}
content.energyfield.{ChargePolarity, EnergyField, EnergyFields, EnergyFieldType}
content.wave.api.{WaveLevels, WaveMachineIntegrationPoints, WaveType, WaveTypes}
```

### 4.3 CEWS 从**共享层**读什么（L2→CORE，允许方向）

| 类 | 住在哪 | 作用 |
|---|---|---|
| `compat.sable.SableSubLevelBridge` | **`core`** | Sable 物理结构桥接（`CewsMod` 用 `Class.forName` 反射加载） |
| `common.AllPartialModels` | **`core`** | 部分模型 |
| `util.GoggleUtil` | **`core`** | 护目镜读数工具 |

> ✅ **实测 CEWS 不引用任何「根工程 / integration」的包**（`integration`、`common.hub`、`data` 全为 0）——分层干净。
> ⚠ **一条已知的"共享层瘦身"候选（登记未做）**：`core` 的 `common.AllSpriteShifts` 是**混合的**——三个 `*_CASING` 只为 COE 机壳服务，但它同时还有 CEWS 波机器的齿轮箱 sprite shift ⇒ **不能整类搬进 `:coe`**，要动就得**拆分**，别顺手动。

---

## 5. 依赖与构建（精确值）

**`cews/src/main/templates/META-INF/neoforge.mods.toml`**：

| 依赖 | 类型 |
|---|---|
| `neoforge` / `minecraft` / `create` | **required** |
| **`createoreexpansion`** | **required** ← 决定性的一条 |
| `createaddition`（CC&A） | **required** |
| `jei` / `curios` | **optional** |

**`cews/build.gradle`**（要点）：
- `jarJar(implementation(project(':core')))` —— **本模块 jar 要能单独拿出去跑**（core 以嵌套库形式跟着走）
- **`compileOnly(project(':coe'))`——`compileOnly` 不传递、也绝不 jarJar**：写 `implementation` 会让 `:coe` 的 jar 进 runtimeClasspath（dev 的 `duplicate_mod` 风险），嵌套一份则会让 COE 的 mod 文件出现两次
- 自己重抄的外部编译面：create / ponder / registrate / jei（api）/ flywheel-api / createaddition / jade / 本地 jar（optical、vintageimprovements）
- **没有 `project(':')`** ⇒ **`:cews:compileJava` 通过本身就是「零根引用」的机器证明**

---

## 6. `CewsMod` 入口（启动顺序 + Sable 判据）

```
@Mod(CewsMod.MOD_ID)  →  构造器第一条语句：LayerBootstrap.ensureAttached(modEventBus)
  →  WaveMachineHandlers.register(GateHit / DisperserHit / TransmuterHit)
  →  LayerCreativeTab.registerAll(CewsCreativeTabs.tabs())
  →  CewsRegistrate.REGISTRATE.registerEventListeners(modEventBus)
  →  CewsBlocks.register() / CewsBlockEntityTypes.register() / CewsItems.register()
  →  bootstrapSable()  ← 可选桥接，最后做
```

**Sable 桥接的两级判据**（2026-09-20 修正，原样保留）：
① 先看**类** `dev.ryanhcode.sable.companion.math.Pose3dc` 在不在（`Class.forName(name, false, loader)` —— **不初始化**，避免副作用）；
② 兜底看 modId `sable` / `sablecompanion` / `aeronautics` 任一已加载。
> **为什么不能只看 modId**：实测 `sable-companion-common-*.jar` 的 modId 是 `sablecompanion`，而声明 `sable` 的是 aeronautics；用户实例里主 sable 未加载 ⇒ 旧判据为假 ⇒ 整条物理结构链路被惰性化。
> 未装 Sable 时**绝不触碰 Sable 类**（`SableSubLevelBridge` 直接引用 Sable 类型，只能反射加载）。

---

## 7. 创造页

| 页 | id | 内容 |
|---|---|---|
| 能量波阵学 | `createoreexpansion:energy_wave_study` | **恰好 12 项** = `EnergyWaveStudyTab#CONTENTS`（能量场控制器 / 变器 / 三个调级器 / 三个波速调节器 / 三个差波器 / 波情查询仪） |

- 页序：**矿物拓展 → 能量波阵学 → Create 调色板**。
- **同步机制**：`BuildCreativeModeTabContentsEvent`，**先 `remove` 再 `accept`**（幂等；只 `accept` 会抛 `already exists in the tab's list` —— 那个崩溃 W4 修过）。
- **该事件只在逻辑客户端触发** ⇒ 「页里到底有哪几项」只能进游戏看（作业单 `build/patch/human-cases-D-E-F1.md` 的 E1）。
- **不在本页的**：三种机壳、三台应力充能器、两个能量构件（都已归矿物页，见 §4.1）。

---

## 8. 关卡与验证现状

- `tools/check-layering.ps1` **EXIT=0**（`COE=261 CEWS=75 TRANS=1 SHARED=22 CORE=58`，零违规、白名单为空）
- `tools/check-package-overlap.ps1` **EXIT=0**（`cews` 75 文件 / 18 包，无重叠）
- 发布形态矩阵（`build/patch/w6d-run-matrix.ps1`，**不要用 `-LaunchMode Direct`**）里与 CEWS 直接相关的用例：
  - **A2**（只装 `cews.jar`）→ 期望**加载期拒载**：`PASS 10/0`
  - **C1**（`coe+cews`，玩家最常用）→ `PASS 16/0`，`Done 7.204s`
  - **C3**（`cews+transmutation`，无 coe）→ 期望两条结构化依赖声明 + 拒载：`PASS 6/0`
  - **B**（三层全装但摘 JEI/Curios/Jade）→ `PASS 20/0`（含 `[CEWS]` 入口行、Curios 降级行）

---

## 9. 变更史（**逐轮追加**）

### 9.1 CEWS 这一线的完整提交史（`git log -- cews`，实测）

| 提交 | 做了什么 |
|---|---|
| `fbf33cdf` | **P3a** 构建骨架：多模块工程 + buildSrc 约定（此阶段 `cews` 还是空模块） |
| `a862831a` | **拆包**：`:cews` 成为真子模块（**133 文件**）；三个内容模块各自独立；根变成集成 mod `coe_integration` |
| `4833a98b` | **资源按层分家**（972 文件 / 1.23 MB；手写逐字节搬、生成物按层改道） |
| `f163ce2d` | **数据按层分家**（217 文件；生成标签与 221 条配方按出处留在集成层） |
| `7d7650e7` | **语言按层分家**（`LayerLangSplitter` 把每层子集写进 `assets/<module>/lang`，**根文件逐字节未动**） |
| `df73c10c` | **构建**：每个内容模块用 `jarJar(implementation(project(':core')))` 嵌套 core，使模块 jar 可单独拿出去跑 |
| `a26494cf` | **P4f** 108 个生成标签按层逐文件改道（跨层各写一份，靠 TagLoader 合并语义还原） |
| `9e7b9dc1` | **P7a** 根降级为 dev-only；集成层按归属搬进各层；共享接线改 core 幂等入口 |
| `c4f888e8` | **P7b** mixin 配置随类进 `:coe`；**每层落一份根 lang 完整拷贝**；新增模块自足性关卡 |
| `e97816dc` | **P7d** 221 条生成配方按 provider 显式绑层；2 条跨层手写标签按层拆半；自足性关卡 52 → 66 |
| `19b2c77f` | **P10** 补 `rotate_modifier` 的语言键（中英），查清 `keyinfo.*` 的真实来源 |
| `9289a98f` | **P12** 还原被「按路径判层」工具逼改的包名（68 个回基准、31 个被迫保留；层计数逐字不变） |
| `a47c3e9a` | **W4** 修**创造页重复添加崩溃**（先 `remove` 再 `accept`，幂等且保住 `CONTENTS` 顺序） |
| `04956d38` | **W6-a** 让分层工具先看见目标边界，就地破掉全部 `COE→CEWS` 边（文件零搬运、包名零改动） |
| `0a774eb4` | **W6-c** **CEWS 的 71 个 L1 文件全部搬进 COE**（6 批全绿，无一批退回） |
| `901b211e` | **W6-d** 修关卡静默空洞（剥注释吞掉 3081 B 代码）+ 清残留 + 头号验收真跑（A1 归零） |
| `6e82fdb1` | 修注释日期（**该提交的说明与内容不符**：实际装了 77 个资源重命名，见复盘 §7.1） |
| `f1c07323` | **W9** **三种机壳从 CEWS 搬进 COE**（用户裁定）：77 个资源 + 14 个生成物，`R100` 逐字节相同；`CONTENTS` 17 → 15 |
| `fc07c161` | **W12** 三台应力充能器与两个能量构件**全部归矿物页**（用户裁定）⇒ **CEWS 页 17 → 12 项，与清单逐项一致** |

### 9.2 后续追加区（**每一轮改动往这里加一行**）

格式：`- <提交号> <日期> <一句话>（验证：<跑过什么>）`

- （待追加）

- `fa37a12d` 2026-09-30 就地更正 `RELEASE.md` 两处过期表述（`coe+cews` 组合**已真验**；资产归属关卡 A1 判据**早已是**依赖方向感知，原文「正在改为…会把合法的 `cews → coe` 报成红」不成立）（验证：`tools/check-asset-attribution.ps1` EXIT=0、`checks run: 5, failed: 0`、`illegal-direction=0`、`legal-cross=107`）

- `e7ad698c` 2026-09-30 `tools/check-layering.ps1` 的**通配符 import** 从「pass 2 贪婪匹配**意外**覆盖」改为**显式分支**：正确贴标（`[wildcard import: …]`）、按「包内实际存在的每一层」展开、无法解析者**判红**，并补**独立宽松正则交叉计数**与**注释行豁免**（防假红）；**同轮更正我先前「关卡对通配符失明」的误判**（详见 §9.2.1）（验证：改前/改后 **EXIT 均 0**、层计数不变 `COE=264 CEWS=75 TRANS=1 SHARED=22 CORE=58`、末尾报告 `wildcard imports seen=8 resolved=8`；**扰动实测**：注入 COE→CEWS 通配符探针后 **EXIT=1**、注释 `//` 与块注释 `*` 两行**均被忽略**、移除探针后 **EXIT=0** 且工作树无残留）

#### 9.2.1 更正条目（**2026-09-30** 追加）

> **为什么没有提交号**：本区铁律是「每条变更**必带提交号**」，但 `markdown_output/` 被 `.gitignore:45` 忽略、**0 个文件被 git 跟踪**（实测 `git ls-files markdown_output` = 0）⇒ 纯文档更正**在现行规则下拿不到提交号**。
> **约定（2026-09-30 起）**：`markdown_output/**` 内**尚无任何文件被 git 跟踪**（本目录被 `.gitignore:45` 忽略 ⇒ `git ls-files markdown_output` = 0），故**只改本目录正文**的更正无法带提交号，以「**日期 + 复跑命令**」代替；**若同一轮也改了被跟踪文件**（如本次的 `RELEASE.md`），则照常带该提交号（本次 = `fa37a12d`）。若日后把本目录 `git add -f` 纳管，此后条目一律恢复带提交号。**该约定已由用户 2026-09-30 确认：保持 `markdown_output/` 被忽略，采用本约定。**

- **更正 §4.2**：标题「实测 24 个导入去重」**不可复跑** —— 该节正文代码块逐项列出的是 **21 个**标识符（4 + 1 + 4 + 1 + 3 + 4 + 4），不是 24。
- **更正 §4.2**：`common.CoeCore` 被列进「CEWS 从**第一层**读什么」，但它实际住在 **`core`（共享层）**、不是 `:coe`；故该块 21 个标识符里只有 **20 个**属于第一层。
- **更正 §4.2（漏项）**：漏了一条真实的 L2→L1 边 —— `content.lightning.block.ReinforcedLightningRodBlockEntity`（住 `:coe`）。**实测 L2→L1 去重后 = 21 个类**（上述 20 个 + 这 1 个）。
- **更正 §4.3**：原文只列 **3** 个共享层类（`SableSubLevelBridge` / `AllPartialModels` / `GoggleUtil`）。**实测 L2→CORE 去重后 = 16 个类**：`common.AllConfig`、`common.AllPartialModels`、`common.CoeCore`、`common.machine.MachineRotateClient`、`common.registry.{LayerBootstrap, LayerCreativeTab, LayerRegistrate, RegistrateTooltips, WaveRecipeCapabilities}`、`content.wave.bridge.{SableBridges, SubLevelBridge}`、`util.{GoggleUtil, HeatLevelNames, RadiusScan, RecipeTypeNames, SpeedBands}`。
  另有 `compat.sable.SableSubLevelBridge` 属**反射边**（`CewsMod` 走 `Class.forName`，无 import）⇒ 不计入这 16。
- **更正（工具行为；本次先误判、后由实测推翻）**：`tools/check-layering.ps1` 对**通配符 import** 一直**是能查出来的 —— 但靠的是意外**。
  - **误判**：pass 1 的 import 正则字符类 `[A-Za-z0-9_.]` 不含 `*`，实测 `import …common.*;` 匹配 False。我据此写下「关卡对通配符完全失明」——**这个结论错了**，错在**只隔离测试 pass 1 就推断整个工具**（正是 `复盘 §7.4` 警告过的取证方式）。
  - **实测反证**：在 `:coe` 里注入一条真实的 `import …content.wave.block.*;`（COE→CEWS，禁止方向）后，**改动前的 HEAD 版同样 EXIT=1**，报的是 `-> CEWS [fully-qualified, not an import]   (…content.wave.block.)`。机制：**pass 2** 把这条 import 行当作全限定引用扫到了 —— FQN 正则贪婪，把 `*` 前那个点一并吃掉，得到以点结尾的裸包名 `…content.wave.block.`，而这个**尾点恰好满足** `Get-TargetLayer` 里 `(\.|$)` 那类分支。
  - ⇒ 旧行为是「**真覆盖、但意外**」：查得出，却挂在**错误标签**下，且依赖贪婪匹配这一偶然。本仓通配符 import 实测 **8 条**（7 × `…createoreexpansion.common.*` 于 `:coe`/`:cews`，1 × `…foundation.item.skill.*` 于 `:coe`），全部合法 ⇒ **改前改后 EXIT 均为 0**（层计数亦不变：COE=264 CEWS=75 TRANS=1 SHARED=22 CORE=58）。
  - **本次落地（`W12-b`）**：把通配符改成**显式分支**（不再依赖贪婪匹配的偶然）＋**正确贴标**（`[wildcard import: …]`）＋**按「包内实际存在的每一层」展开**＋**无法解析者判红**（unknown 不得静默降级为「推定合法」）＋**独立宽松正则交叉计数**（严格扫描若失效则两者不等即红）。另给该分支补了**注释行豁免** —— 否则一条注释掉的通配符 import 会变成假红（pass 2 一直豁免注释，pass 1 没有）。
  - **扰动实测**（本仓纪律「弄坏必须变红」）：注入探针后 **EXIT=1**（1 条 COE→CEWS 违规 + 1 条 guard problem）；注释 `//` 行与块注释 `*` 行**均被正确忽略**；删除探针后 **EXIT=0**、工作树无残留。
  - ⚠ **顺带登记的既有行为（未改，待裁定）**：pass 1 的**具名** import 分支同样**不豁免注释行** ⇒ 若有人把一条具名 import 注释掉，它仍会被当成真边（贴 `[dead import]` 标签）并可能判红。本仓当前无此情形（全绿）；是否一并修属对既有判据的改动，**不在本次范围**。

**复跑命令**（满足本文件 §1「数字必须可复跑」）：

```powershell
# 各层 Java 文件数与包数
Get-ChildItem cews\src\main\java -Recurse -File -Filter *.java | Measure-Object

# L2→L1 / L2→CORE 的去重类清单（判层依据 = 目标文件实际所在模块，不看包名猜）
Get-ChildItem cews\src\main\java -Recurse -File -Filter *.java |
  Select-String -Pattern '^\s*import\s+(static\s+)?(com\.hjmmd_8\.createoreexpansion\.[A-Za-z0-9_.]+)' |
  ForEach-Object { $_.Matches[0].Groups[2].Value } | Sort-Object -Unique

# 通配符 import 清单（关卡现已显式解析这类边；改前靠 pass 2 的贪婪匹配意外覆盖）
Get-ChildItem cews\src\main\java -Recurse -File -Filter *.java |
  Select-String -Pattern '^\s*import\s+.*\*;'
```

- **更正 §1（待生效，跨会话）**：§1 的语言键计数「`assets/createoreexpansion/lang` = **403/407**」**在 HEAD `67dc28e7` 上仍然准确**；但另一个会话正在做的 COE 盔甲（4 材质 × 4 件）跑过 `runData` 后，已把 **16 条**盔甲语言键灌进**每个模块**的「根 lang 完整拷贝」——包括本层的 `cews/src/generated/resources/assets/createoreexpansion/lang/*`，实测工作树已变成 **419/423**。这是 P7b（每层落一份完整拷贝）的**设计行为**，不是本层的改动（`cews/src/main` 干净）。**盔甲落地后 §1 需按新值更新**；`assets/cews/lang` 的 **14/14** 子集不受影响。

- **更正（跨文件，已在各自文件就地更正）**：另有两处过期表述**不在本文件**：
  - `RELEASE.md` §3 的「`coe` + `cews` … 运行期整组待跑」与该文件 §5 的「C1 = `PASS 16/0/1`（`Done 7.204s`）」及 §5 末「九条已在 HEAD 重建 jar 上复跑 9/9 PASS」**自相矛盾**（§5 更晚）⇒ 已在 `RELEASE.md` §3 更正。
  - `RELEASE.md` §4.4 的「该关卡的 A1 判据**正在改为**依赖方向感知…现状它会把合法的 `cews → coe` 引用也报成红」**已过期**：脚本早已读各模块 `neoforge.mods.toml` 的 `type = "required"`，实测 `legal-cross=107` / `illegal-direction=0` / `checks run: 5, failed: 0`，`cews → coe` 那 3 个贴图的闭包被正确判为**合法** ⇒ 已就地更正。
  - `markdown_output/分层重构（依赖方向重排）复盘.md` §6 的「三台应力充能器与两个能量机构**仍出现在能量波阵学创造页**…（**待用户裁定**）」已被 W12 `fc07c161` 取代 ⇒ 已在该文件追加更正。

---

## 10. 已知的开放项（登记，别当已做）

1. **`AllSpriteShifts` 是混合类**（三个 `*_CASING` + CEWS 齿轮箱 shift）：要搬进 `:coe` 必须**先拆分**——属于"共享层瘦身"，**不在本次收口范围**。
2. **`cews.jar` 里有 3 个空目录条目**（`data/createoreexpansion/recipe/` 那三层，`purgeStale` 删文件不删目录）：清理方案要么影响所有层、要么给 jar 加 `exclude`（后者将来 CEWS 真有自己配方时会**静默丢文件**）⇒ **风险大于收益，保留不动**（`RELEASE.md` §4.3 同口径）。
3. **CEWS 树里两张无人引用的遗留美术**（`energy_wave_machine/jade_casing_base_side.png`、`jade_gearbox.png`）：**按美术红线不动、不搬**，只登记。
4. **`incomplete_energy_mechanism` 的可见性**：它是**正常注册的物品**（合成链中间物），用户 2026-09-28 只裁定了它**出现在哪一页**（矿物页）。若将来要它**彻底不进任何创造页**，从 `EnergyWaveStudyTab#BASE_PAGE_ITEMS` 移除并加一次 `event.remove` 即可（**这是游戏内容决策，需先问用户**）。
5. **可选的升级**：`tools/check-data-attribution.ps1` 从「观测型」升级成归属断言（判据与 U1–U4 前提在脚本第 9 节与复盘 §8；实测**跨模块数据引用 = 0 对**）。

---

## 11. 记忆入库指引

与 COE 那份同一情况（2026-09-29 实测）：写这份文件的会话**没有 `hindsight_*` 工具**，本机也没有该运行时/命令行；且这台机器按用户要求关掉了自动导入（`~/.hindsight/coding-agent.json`：`retainSessions=false`、`autoSeed=false`、`codebaseSurvey=false`）。

想让记忆真写进去，三条路：
1. 在**有 `hindsight_*` 工具**的会话里调用 `hindsight_ingest_document(title="CEWS 层功能清点与变更史（第二层 / cews）", content=<本文件正文>)` —— **不受 `retainSessions` 影响**；
2. 把 `retainSessions` 改回 `true`（⚠ 与用户 2026-09-10 的既有要求相反，**改前先问**；且该配置**下一个新会话**才生效）；
3. 就只留文件（当前做法）。

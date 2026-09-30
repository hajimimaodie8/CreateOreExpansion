# CEWS：《机械动力：能量波阵学》模块 —— 拆分方案与设计思索

> 版本：2026-09-14 · 状态：**阶段 0 已落地**（独立创造标签页 + 唯一清单）；
> 阶段 1/2（包级隔离 → 独立内置 jar）为**后续任务**，本文给出可直接执行的方案。
> 相关：`能量波三态与变器双模式（需求与进度）.md`（系统功能）、`星辉波变器代码审查与结构地图.md`（代码地图）。

---

## 0. 一句话

能量波系统（充能器 / 调级器 / 波速调节器 / 波差器 / 能量场控制器 / 星辉波变器 / 机壳 /
波情查询仪）从本模组的"矿物拓展"里**独立成一个板块**，叫 **CEWS = Create: Energy Wave Studies
（机械动力：能量波阵学）**；最终形态是**一个独立的内置 jar（新模块）**，现在先把标签页与边界清单做出来。

---

## 1. 命名与翻译口径（含用户提问的答复）

| 项 | 取值 | 说明 |
|---|---|---|
| 中文名 | 机械动力：能量波阵学 | 用户定稿 |
| 英文名 | **Create: Energy Wave Studies** | 用户原写 "Energy Wave Study"，建议改用**复数 Studies** |
| 缩写 | **CEWS** | 两种写法缩写都成立（Create Energy Wave Studies） |

**关于"学"译成 study 是否合理（答复）**：学科/领域名在英文里的固定习惯是**复数 Studies**
（Media Studies、Cultural Studies、Wave Studies）；单数 "Study" 更像"一项研究 / 一份研究报告"，
读起来是"一次调研"而不是"一门学问"。其他常见选择与取舍：

- `-ology` 后缀（Waveology）：英语里少见、生造感强 ✗；
- `Theory / Science`（Wave Theory / Wave Science）：偏"理论/科学"，与"某学"的中文语感也接近，
  但 Minecraft 语境里 Studies 更常见 ✓；
- **Studies（推荐）**：既保留用户命名，又符合学术命名习惯。

**若用户坚持单数**，只需改两处 lang 词条（`itemGroup.createoreexpansion.energy_wave_study` 与本文档），
代码与注册 id 全都不含英文名（id 一律用 `energy_wave_study`）。

---

## 2. 阶段 0 已落地的东西（本次任务）

| 产出 | 位置 | 说明 |
|---|---|---|
| 新标签页 | `common/AllCreativeModeTabs#ENERGY_WAVE_STUDY` | id `createoreexpansion:energy_wave_study`；**图标 = 翡翠应力充能器**（用户指定：波从充能器发出，它才是这一线的起点）；`withTabsBefore` 链把顺序排成 **矿物拓展 → 能量波阵学 → Create 调色板** |
| 唯一内容清单 | `common/EnergyWaveStudyTab#CONTENTS` | **17 项**：3 充能器 / 1 能量场控制器 / 1 波变器 / 3 调级器 / 3 波速调节器 / 3 波差器 / 2 机壳 / 1 波情查询仪 |
| 内容同步 | `EnergyWaveStudyTab#onBuildContents`（MOD 总线 `BuildCreativeModeTabContentsEvent`） | **一处清单**同时"往新页放 + 从基础页剔除"，注册代码一行未改；命中时打一行 INFO 日志便于核对 |
| 语言 | `itemGroup.createoreexpansion.energy_wave_study` | 「机械动力：能量波阵学」/「Create: Energy Wave Studies」 |

**为什么用事件而不是改注册**：本模组所有条目靠 Registrate 的 `defaultCreativeTab` 自动进基础页
（`CreateOreExpansion` 静态块），逐个改要在两处动近百行注册代码，且以后加机器容易漏。
事件方案把"什么属于 CEWS"收敛成**一份清单**——这份清单正是**将来拆包的搬运单**。

---

## 3. 模块边界（拆包时的"进/不进"判定）

### 3.1 判据（三条，按序用）

1. **它是不是"能量波"这件事本身的机器或仪器？**（波的产生 / 整形 / 转向 / 变换 / 探测）
2. **它是否只在能量波语境下有意义？**（拆掉波系统后就是个纯装饰方块 ⇒ 归 CEWS）
3. **它是否被矿物线当材料/产物使用？**（是 ⇒ 留在矿物模块，或两边都注册）

### 3.2 清单（含用户已裁定项）

| 归属 | 内容 |
|---|---|
| **进 CEWS** | 应力充能器 ×3（翡翠/蓝宝石/星辉石）、能量调级器 ×3、波速调节器 ×3、波差器家族 ×3（四面/六面/八面）、能量场控制器 ×1、星辉波变器 ×1、**机壳 ×2**（翡翠/蓝宝石）、波情查询仪 ×1 |
| **留矿物模块** | 矿石/宝石/水晶芽/块、锭/粒/粉、嬗变液（流体）、雷鸣合金、**强化避雷针**（用户 2026-09-14 明确：它属于矿物拓展——雷击加工本身也加工矿物类配方，波只是引雷手段之一）、工具与技能物品、凝能之佩 |
| **待定（拆包时拍板）** | ① **能量机构 `energy_mechanism`**（合成件，名带"能量"但属材料）；② **能量场**相关方块/数据（场是波的"外部条件"，但场控也可能服务别的机器）；③ 能量工具（`content/equipment/tool/energy`）——名字像、但属技能/工具线 |

> 判据与剩下的 3 个"待定项"就是"思索"的主体：**拆包能否干净，取决于这 3 项怎么切**。
> 我的建议：① 进 CEWS（它就是能量机器的合成件）；② 进 CEWS（场的唯一消费者是波）；
> ③ 留矿物模块（工具线）。

**关于"星辉石机壳"（重要事实）**：注册表里**只有两种机壳方块**——`jade_casing`、`sapphire_casing`
（都是 `CasingBlock`，且都已加入 Create 的 `create:casing` 方块/物品标签，可用来包轴/齿轮）。
**`stellarstone_casing` 不是方块，只是一张贴图**（`textures/block/stellarstone_casing.png`，
被星辉石系机器当机壳材质用）。所以"三种机壳"目前只能收两种；
若确实需要一个"星辉石机壳方块"，那是**新内容**（注册方块 + 模型/方块态 + 战利品表 + 语言 + 标签），
需用户单独拍板——**属于新增内容，不是搬运**。

> **2026-09-25 更正（P1 实测）**：上面这句"`stellarstone_casing` 不是方块"**已经不成立**——
> 注册器分家时实测到**第三种机壳方块确实存在**（`AllBlocks`/现 `CewsBlocks` 里的机壳家族共 3 个：
> `jade_casing`、`sapphire_casing`、`stellarstone_casing`，P1 归 CEWS）。它**不在** CEWS 标签页的
> `EnergyWaveStudyTab#CONTENTS` 清单里，所以目前仍显示在基础创造页。
> → 待用户拍板两件事：① 归属确认（P1 已按"机壳家族"归 CEWS）；② 要不要把它加进 CEWS 标签页清单。

---

## 4. 阶段 2 拆包方案（后续任务，先不动手）

### 4.1 "内置 jar"的技术形态

"独立的内置 jar"在 NeoForge 里对应 **JarJar 嵌套模块**（`jarJar(...)` 把一个子模块 jar 打进主 jar，
运行期由 Jar-in-Jar 加载器展开）。要点：

- **必须有独立的 mod id**（JarJar 里每个嵌套 jar 都是一个独立 mod）⇒ 提议 mod id `cews`；
- **注册命名空间必须保持 `createoreexpansion`**（`ResourceLocation.fromNamespaceAndPath(MOD_ID, id)`
  里的 namespace）——**这是存档兼容的红线**：一旦命名空间改成 `cews`，
  所有方块/物品/配方/标签 id 全变，老存档直接报废。**mod id 与注册命名空间不必相同**，
  所以做法是：新模块的 `MOD_ID = "cews"` 只用于元数据/依赖/日志，注册表继续用
  `createoreexpansion` 命名空间（把 `CreateOreExpansion.MOD_ID` 拆成 `MOD_ID` 与 `REGISTRY_NAMESPACE` 两个常量）。

### 4.2 需要处理的耦合点（现状盘点，逐个都有对策）

| # | 耦合点 | 现状 | 拆包对策 |
|---|---|---|---|
| 1 | 注册器 | `common/AllBlocks`、`AllItems`、`AllBlockEntityTypes`、`AllRecipeTypes`…全是**一个** Registrate 实例（`CreateOreExpansion.REGISTRATE`） | 新模块自带 Registrate 实例；`defaultCreativeTab` 指向 CEWS 标签页 |
| 2 | **注册顺序** | 静态初始化顺序敏感（方块→方块实体→物品） | 新模块内部自成顺序；跨模块引用（CEWS 用矿物线的星辉石材料）改成**显式依赖 + 延迟取用**（`DeferredHolder`/`Supplier`） |
| 3 | 配置 | `common/AllConfig` 一份，含 `[wave]`/`[charger]` 段 | 按 module 拆成 `CewsConfig`；**配置键路径保持不变**（`createoreexpansion-common.toml` 的老键要能继续读，避免玩家配置失效） |
| 4 | 数据生成 | `CreateOreExpansionDatagen` 一份，含方块态/模型/语言/配方/战利品表/标签 | 拆成两个 datagen 入口；`runData` 任务分别跑；语言文件按 namespace 合并（同 namespace 会写入同一份 json，需约定谁写哪一段，避免互相覆盖） |
| 5 | `compat/*` | Jade / JEI / CC&A / Vintage / Optical / Sable 的波相关代码混在同一层 | 按"这个 compat 类是否只服务波"切分：`WaveJadePlugin`、`StellarWaveMachineIntegrations` 等随 CEWS 走；机器无关的（如 `BasinLiveJadePlugin`）留在主模块 |
| 6 | mixin | `mixins.json` 一份 | CEWS 自带 `cews.mixins.json`（JarJar 支持每个子模块自己的 mixin 配置） |
| 7 | 跨模块引用 | 波需要"读机器能力"（`StellarWaveMachineRegistry` 扫描半径内的加工机，含 CC&A/Vintage 机型） | 这是**最脏的一处**：波系统依赖"世界上有什么机器"这件事本质上是**运行时能力探测**，不是编译期依赖 ⇒ 保留在主模块的机器目录 API，CEWS 只依赖该 API（方向：CEWS → 主模块，单向） |
| 8 | 材料依赖 | CEWS 的机器由星辉石/蓝宝石/翡翠部件合成 | CEWS → 主模块单向依赖（主模块不反向 import CEWS）——**方向必须钉死，否则拆不开** |
| 9 | 标签页 | 本次已建 CEWS 页 | 拆包后标签页归属 CEWS（主模块不再注册它） |
| 10 | 语言键 | `createoreexpansion.*` 命名空间 | 键名不动（同理：key 变了玩家资源包/第三方会碎） |
| 11 | 能量波实体/粒子 | 波实体在 `content/charger/entity`、粒子在 `client/renderer` | 整体随 CEWS；但**波与主模块的"雷击统一加工"有交叉**（引雷 → 落点加工），需保留一个窄接口 |
| 12 | 存档/数据包 | 64 个充能配方 + 战利品表 + 标签在 `data/createoreexpansion/**` | 命名空间不变 ⇒ 数据包路径不变 ⇒ 存档与整合包无需改 |

### 4.3 分期路线（建议）

- **阶段 0（已完成）**：标签页 + `CONTENTS` 清单（"什么属于 CEWS"落地成代码）。
- **阶段 1（同 jar 内隔离，低风险）**：把清单涉及的包整体挪到 `content/cews/**`，
  用**架构测试/脚本**断言"主模块不 import cews 包"（反向依赖只许 compat 例外），
  行为零变化、存档零影响。这一步做完，"拆包"就只剩工程配置。
- **阶段 2（真拆）**：新建 Gradle 子模块 `cews`（独立 mod id、独立 mixins、独立 datagen），
  主 jar `jarJar(project(":cews"))`；主模块只保留矿物线与"机器目录 API"。
  验收：老存档可直接打开、所有 id 不变、`runData` 两份产物、JEI/Jade 插件仍工作。

### 4.4 风险清单（拆包时逐条验收）

1. **存档兼容**：注册命名空间必须 `createoreexpansion`（否则全炸）；
2. **配置键**：`[wave]`/`[charger]` 段键名保持；
3. **语言键**：保持 `createoreexpansion.*`；
4. **数据包路径**：保持 `data/createoreexpansion/**`；
5. **注册顺序**：跨模块的 `Supplier` 必须延迟取用；
6. **单机/专用服务器**：CEWS 的客户端渲染器/模型注册要在正确的 dist 下；
7. **可选模组**：CC&A/Vintage 相关仍在 compat 层，且"未安装时绝不触碰其类"这条铁律不能破。

---

## 5. 第二个模块：矿物拓展（长期计划，本次不做，仅登记）

用户口径："然后再处理补全第二个矿物扩展的模块（长期的任务打算，先不要做，你了解就行）"。
登记如下，避免将来重新讨论：

- **定位**：本模组的"本体"（现有 `base_tab`：矿物/宝石/水晶芽/工具/技能/嬗变液/雷鸣合金）将来
  同样收成一个独立模块（暂称 **COE = Create: Ore Expansion**），与 CEWS 平级；
- **与 CEWS 的关系**：CEWS 单向依赖 COE（材料），COE 不依赖 CEWS；
- **前置条件**：CEWS 拆包完成 + 注册命名空间策略确定（见 §4.1）+ 技能内核迁移（`AGENTS.md` 挂起事项）落地；
- **风险**：技能系统换核（Skiller）与矿物模块拆分**会撞车**（都动 `foundation/item/skill` 与注册），
  顺序必须先技能后矿物，或先矿物后技能，**不能并行**。

---

## 6. 验收标准（本阶段）

- 进游戏：创造栏出现第二个页签「机械动力：能量波阵学」，内含 17 项、顺序即 `CONTENTS` 顺序；
- 基础页「机械动力：矿物拓展」**不再出现**这 17 项（不重复、不遗漏）；
- 日志出现两行 `[CEWS] 能量波阵学标签页内容同步：17 项（加入新页 / 从基础页剔除）`；
- `compileJava` / `runData` / javadoc / 语言审计全绿。

## 7. 阶段 1/2 落地取证（P3z，2026-09-26；从 AGENTS.md 迁入）

`cews/src/main/java` 有 **133 个文件**：`content/{charger,wave,machine,energyfield}/**` + `common/registry/cews/**` + `compat/jei/cews/**` + `client/renderer/cews/**`；`CewsMod` 是它的 `@Mod` 入口。
`jarJar(compileOnly(project(':cews')))` 把 `…cews-1.0.0.jar` 嵌进根 jar 的 `META-INF/jarjar/` 第 1 层。
`cews/build.gradle` = `compileOnly(project(':core'))` + `compileOnly(project(':coe'))` + **自己重抄的外部编译面**（create / ponder / registrate + jei + flywheel-api + createaddition + jade + 本地 jar optical / vintage），**没有 `project(':')`**
⇒ `:cews:compileJava` 通过本身就是「零根引用」的机器证明。
逐条取证（七个关卡、类名并集前后对照、`@EventBusSubscriber` 审计与跨文件真调用探针）在 `build/patch/p3z-EVIDENCE.txt`（`build/` 被 git 忽略，重跑该轮即可重生成）。

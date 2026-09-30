# COE 层（第一层 / `createoreexpansion`）功能清点与变更史

> **这份文件是干什么的**：它是「**COE 层现在有什么**」+「**它怎么变成现在这样的**」的单一索引。
> 专为「**历史留存**」会话使用：**每一轮改动都往 §6 追加一行**，其余各节只在内容真的变了时才改数。
>
> **三条约定（请后续会话遵守）**
> 1. **只追加、不改写既有条目**（要更正就追加一条「更正：…」，保留原文，别抹掉）。
> 2. **每条变更必须带提交号**；数字必须**可复跑**（§1 每条都给了量法，别凭记忆写数）。
> 3. **与既有文档的分工**：工程红线/坑 → `AGENTS.md`；发布说明 → `RELEASE.md`；本次重构的教训 → `markdown_output/分层重构（依赖方向重排）复盘.md`；**本文件只做「功能清单 + 变更时间线」**，不重复上面那份的论述。
>
> 生成时间：**2026-09-29**（数字全部由当时的树实测得到，不是回忆）。

---

## 0. 一句话定位

**COE = 第一层**，是本模组的「本体」：**mod id 仍然是 `createoreexpansion`**（注册命名空间/注册 id/语言键/数据包路径一律未改——这是存档红线），它是**发布的最小单元**：玩家只装 `coe.jar` 也必须能完整玩（这是 A1 验收）。

- 第二层 `cews`（能量波阵学）与第三层 `transmutation`（空壳）都对它 **required**。
- **COE 对自己以外的任何本模组模块零依赖**（`:coe:compileJava` 通过本身就是「零根引用」的机器证明）。

---

## 1. 规模（实测，含量法）

| 项 | 数 | 量法 |
|---|---|---|
| COE 层 Java 文件 | **276** | `Get-ChildItem coe\src\main\java -Recurse -File -Include *.java \| Measure-Object` |
| 手写资源（`coe/src/main/resources`） | assets **234** / data **185** | 分别对 `assets`、`data` 递归计数 |
| 生成资源（`coe/src/generated/resources`） | assets **218** / data **374** | 同上 |
| 语言键（根 lang 完整拷贝） | zh_cn **407** / en_us **403** | `ConvertFrom-Json` 后数属性 |
| `coe.jar`（发布产物） | **1,490,011 B** | `Get-Item coe\build\libs\createoreexpansion-1.0.0.jar` |
| 全仓提交数 / 分支 | **227** / `leaf-dev` | `git rev-list --count HEAD` / `git branch --show-current` |

> ⚠ **jar 字节数只在同一个工作树里可比**（重建会有几十字节级差异；见 `AGENTS.md` 红线）。

---

## 2. 注册内容（按注册顺序，实测）

### 2.1 方块 **22** 个 —— `common/registry/coe/CoeBlocks.java`

| # | id | 说明 |
|---|---|---|
| 1–4 | `jade_ore` `deepslate_jade_ore` `jade_block` `raw_jade_block` | 翡翠线（矿 / 深板岩矿 / 块 / 粗矿块） |
| 5–8 | `topaz_ore` `deepslate_topaz_ore` `topaz_block` `raw_topaz_block` | 黄玉线 |
| 9–11 | `nether_sapphire_ore` `sapphire_block` `raw_sapphire_block` | 蓝宝石线（下界矿） |
| 12–14 | `end_stellarstone_ore` `stellarstone_block` `raw_stellarstone_block` | 星辉石线（末地矿） |
| 15 | `thunderite_block` | 雷鸣合金块 |
| 16 | `ruby_block` | 红宝石块 |
| 17 | `sanctstone_block` | 圣石（钐石）块 |
| 18–20 | `jade_casing` `sapphire_casing` `stellarstone_casing` | **三种机壳**（`CasingBlock`；用户 2026-09-28 裁定：归 COE） |
| 21 | `power_angle_grinder` | 动力角磨床（拆磨加工） |
| 22 | `reinforced_lightning_rod` | 强化避雷针（**用户裁定留在矿物页**） |

### 2.2 应力充能器 **3** 台 —— `common/registry/coe/charger/CoeChargerBlocks.java`

`jade_stress_charger` / `sapphire_stress_charger` / `stellarstone_stress_charger`
（对应方块实体在 `charger/CoeChargerBlockEntityTypes.java`、实体类型在 `charger/AllEntityTypes.java`）

### 2.3 物品 **82** 个 —— `common/registry/coe/CoeItems.java`

| 组 | 个数 | 内容 |
|---|---|---|
| 翡翠线 | 15 | `jade_ingot` `raw_jade` `jade_nugget` `crushed_jade_ore` `jade_small_shard` `jade_big_shard` `jade_sheet` `jade_rod` `jade_wire` + 剑/镐/斧/铲/锄 + `jade_stress_medallion` |
| 黄玉线 | 15 | 同上结构（`topaz_*`） |
| 蓝宝石线 | 15 | 同上结构（`sapphire_*`） |
| 星辉石线 | 15 | 同上结构（`stellarstone_*`） |
| 雷鸣合金线 | 11 | `thunderite_ingot` `thunderite_scrap` `thunderite_sheet` `thunderite_rod` `thunderite_wire` + 剑/镐/斧/铲/锄 + `thunderite_stress_medallion` |
| 红宝石线 | 4 | `ruby_ingot` `ruby_sheet` `ruby_rod` `ruby_wire` |
| 圣石线 | 4 | `sanctstone_ingot` `sanctstone_sheet` `sanctstone_rod` `sanctstone_wire` |
| 下界合金压力佩饰 | 1 | `netherite_stress_medallion` |
| 幸运粉 | 1 | `lucky_dust` |
| 特殊武器 | 1 | `jade_topaz_bow`（翡翠黄玉弓 —— 箭矢走**嬗乱**分支） |

> **合计核对**：15×4（翡翠 / 黄玉 / 蓝宝石 / 星辉石）+ 11（雷鸣合金）+ 4（红宝石）+ 4（圣石）+ 1 + 1 + 1 = **82** ✓
> 复量法：`Select-String -Path coe\...\CoeItems.java -Pattern '\.item\("([a-z0-9_]+)"'`。

### 2.4 配方类型 **6** 条 —— `common/registry/coe/CoeRecipeTypes.java`

**顺序是玩法不变量**（决定波按什么次序处理配方；X2 冻结序断言守着它）：

```
TRANSMUTING → LIGHTNING → LIGHTNING_BLOCK → GRINDING → DISMANTLING → CHARGING
（前 4 条 processing，后 2 条 serializer；数值 id 7..12）
```

### 2.5 其它注册类（`common/registry/coe/` 共 13 个 + `charger/` 5 个）

`CoeRegistrate`（本层的 Registrate 实例 —— **它决定 datagen 判层**）、`CoeBlockEntityTypes`、`CoeCreativeTabs`（本层唯一的页 `base_tab`）、`CoeJeiCategories`、`CoeRecipeProvider`、`AllDataComponents`、`AllModifiableAttributes`、`AllSkills`、`AllStructureProcessors`、`AllTiers`；`charger/` 里另有 `AllEntityTypes`、`ChargerKineticTooltip`、`CoeChargingRecipeProvider`。

**并入第一层的嬗化注册（8 个，W6-b2）** —— `common/registry/transmutation/`：`TransmutationRegistrate`（**必须用 `CoeRegistrate.REGISTRATE` 语义**，见 `AGENTS.md`）、`TransmutationFluids`、`TransmutationEffects`、`AllModPotions`、`AllFanProcessingTypes`、`TransmutationItems`、`TransmutationRecipeTypes`、`TransmutationJeiCategories`。

---

## 3. 子系统（按包，文件数是实测）

| 包 | 文件 | 职责 |
|---|---|---|
| `content/charger/wave` + `content/wave/api` | 7 + 6 | **波引擎**：波实体、轨迹、碰撞/爆炸、加工；`api` 是给第二层用的波能力接口 |
| `content/charger/craft`(+`family`) | 11 + 1 | 波加工的执行器族（含 `WaveItemFluidFilling` 货载旁路 —— 「给桶注液不是配方」） |
| `content/charger/block`、`entity`、`payload`、`recipe` | 12 / 6 / 2 / 2 | 充能器方块、波实体与其网络载荷、充能配方的数据形态 |
| `content/energyfield` | 11 | **场引擎**（加速/偏转/赋能）—— ⚠ 与第二层的「能量场控制器机器」是两回事 |
| `content/crystal` | 4 | 可生长水晶（芽 / 簇 / 芽床） |
| `content/grinding/{block,effect,item,recipe,behaviour}` | 3/6/2/3/2 | 拆磨：动力角磨床 + 磨轮效果 + 掉落与加工行为 |
| `content/lightning`(+`block`) | 6 + 2 | 雷击加工与强化避雷针 |
| `content/transmuting/{effect,event,fluid}` + 同级 2 | 2 + 1 + 1 + 2 | 嬗化：嬗化液、嬗乱效果、事件、流体 |
| `content/equipment/{medallion,item,tool}` | 10(+bridge 2/handler 2) / 1 / 1 | 佩饰（含 Curios 桥）、宝石工具 |
| `content/skill/{config,context,strategy,handler,attribute,input,tooltip}` | 14/5/3/3/1/1/1 | 旧技能框架（**未删，作回退路径与客户端预览**） |
| `integration/skiller/{skill,context,client,strategy}` | 7/8/8/3 | **Skiller 内核集成**（9/9 技能已迁移；剩余 = 渲染器适配进 `StrategyRenderers`） |
| `foundation/item/skill/{strategy,config}` + `foundation/util` | 4/2 + 6 | 技能物品、策略、配置、工具类 |
| `client`(+`renderer`) | 4 + 3 | 客户端拦截（Ctrl+扳手旋转）、渲染器 |
| `compat/{jei,jade,curios,createaddition/coe}` | 3+4+6+3+2 / 3 / 2 / 1 | **可选依赖隔离层**（`content/` 包不得 import 可选模组类） |
| `mixin`(+`renderers`) | 5 + 3 | 配方装配 / POI / 玩家游戏模式 / 结构模板；渲染器 accessor |

**mixin 配置**：`coe/src/main/resources/createoreexpansion.mixins.json`（配置跟着类走；无 mixin 类的层不声明、不造空配置）。

---

## 4. 创造页

| 页 | id | 内容 |
|---|---|---|
| 矿物拓展（**第一**） | `createoreexpansion:base_tab` | 本层全部内容（图标 = 翡翠锭） |
| 能量波阵学（第二） | `createoreexpansion:energy_wave_study` | **恰好 12 项** = `EnergyWaveStudyTab#CONTENTS`（能量场控制器 / 变器 / 三个调级器 / 三个波速调节器 / 三个差波器 / 波情查询仪） |

**已归矿物页**：三种机壳、三台应力充能器、两个能量构件（`energy_mechanism` / `incomplete_energy_mechanism`）——最后一次由 `1e25b015` 落定，两个构件走 `BASE_PAGE_ITEMS` 反向搬运（**不动注册点**：Registrate 实例决定 datagen 判层）。

> ⚠ 标签页内容构建事件**只在逻辑客户端触发** ⇒ 「页里到底有哪几项」只能进游戏看（作业单 `build/patch/human-cases-D-E-F1.md` 的 E1）。

---

## 5. 关卡与工具（现状）

七个只读关卡 + 一个验收矩阵驱动（细节写在各自脚本头注释里）：
`check-layering` / `layer-usage` / `check-package-overlap` / `check-module-selfsufficiency`（**92 条**断言）/ `check-skill-render-coverage` / `check-package-heritage`（报告型）/ **`check-asset-attribution`**（资产归属，依赖方向感知）/ **`check-data-attribution`**（跨层数据引用，观测型）。
验收矩阵：`build/patch/w6d-run-matrix.ps1`（14 用例；**不要用 `-LaunchMode Direct`**）。

---

## 6. 变更史（**逐轮追加**）

### 6.1 本次「依赖方向重排 + 收口」（W3 → W12，2026-09-27 → 09-29）

| 提交 | 日期 | 做了什么 |
|---|---|---|
| `d2e0499c` | 09-27 14:07 | **W3** 发布形态测试台：不绑 sourceSet、只从 `mods/` 加载真 jar；服务端 7 项逐条验证通过 |
| `a47c3e9a` | 09-27 14:19 | **W4** 修**创造页重复添加崩溃**（先 `remove` 再 `accept`，幂等且保住清单顺序） |
| `1e11e2d8` | 09-27 14:21 | `check-package-heritage` 改**报告型**（冻结基准上的硬关卡必然逼出白名单） |
| `2c325bc3` | 09-27 21:23 | **W5** 单装与可选依赖缺失实测：测试台加 `-OnlyModId`；**查出「单装」承诺当时是破的** |
| `04956d38` | 09-27 22:02 | **W6-a** 让分层工具先看见目标边界，并就地破掉全部 `COE→CEWS` 边（文件零搬运、包名零改动） |
| `c3826f9f` | 09-27 22:35 | **W6-b2** **嬗化 16 个 Java 文件整块并入 COE**，第三层留空壳（读法 ii） |
| `0a774eb4` | 09-27 23:12 | **W6-c** CEWS 的 **71 个 L1 文件全部搬进 COE**（6 批全绿，无一批退回） |
| `901b211e` | 09-27 23:35 | **W6-d** 修关卡静默空洞（剥注释吞掉 3081 B 代码）+ 清残留 + 头号验收真跑（A1 归零） |
| `847d10ff` | 09-27 23:50 | **W8**（暂停，未验证）`Class.forName` 字面量扫描 + 通用反空转守护（断言 71 → 88） |
| `273e596c` | 09-28 12:17 | **`RELEASE.md`** 发布说明（发什么 / 依赖矩阵 / 组合方式 / 已知限制 / 验证状态） |
| `6e82fdb1` | 09-28 12:21 | 修注释日期（**该提交的说明与内容不符**：实际装了 77 个资源重命名，见复盘 §7.1） |
| `9987f132` | 09-28 12:21 | 修两处注释日期 |
| `97d2615b` | 09-28 12:27 | `RELEASE.md`：资产归属欠账「已修」写成事实；机壳已离开 CEWS 页 |
| `f1c07323` | 09-28 12:38 | **W9 三种机壳从 CEWS 搬进 COE**（用户裁定）：77 个资源 + 14 个生成物，`R100` 逐字节相同；CONTENTS 17 → 15 |
| `26ebe9ba` | 09-28 12:45 | **W10 新增资产归属关卡**（依赖方向感知） |
| `1fe1b73e` | 09-28 13:27 | `RELEASE.md` 第 5 节：写入矩阵已真跑的 9 个用例 |
| `2799397c` | 09-28 13:31 | **W11 新增跨层数据引用扫描**（观测型；实测**跨模块数据引用 = 0 对**） |
| `89757c03` | 09-28 13:38 | **W8-b**（**未交回报告**，父代理核实后落盘）六个工具补反空转守护 + 层一致性核对 |
| `e541c7cf` | 09-28 13:41 | `AGENTS.md` 收口：状态行、**CEWS 段方向写反的重写**、工具节补两种新关卡与矩阵驱动 |
| `63e2dbda` | 09-28 13:43 | `RELEASE.md` 基准换成「从 HEAD 重建的 jar 上复跑」的那一批（9/9 PASS） |
| `fc07c161` | 09-29 12:09 | **W12** 三台应力充能器与两个能量构件**全部归矿物页**（用户裁定）；CEWS 页 17 → **12 项、与清单逐项一致** |
| `1e25b015` | 09-29 12:10 | W12 文档同步（AGENTS.md 创造页清单行 + RELEASE.md §4.5 + E1 作业单） |

### 6.2 后续追加区（**每一轮改动往这里加一行**）

格式：`- <提交号> <日期> <一句话>（验证：<跑过什么>）`

- （待追加）

---

## 7. 把这份内容写进 Hindsight 记忆（给下一个会话的操作指引）

**本文件生成时的已知事实（2026-09-29）**：写这份文件的会话**没有 `hindsight_*` 工具**，本机也没有该运行时/命令行可调用；并且这台机器按用户要求关掉了自动导入
（`~/.hindsight/coding-agent.json`：`retainSessions=false`、`autoSeed=false`、`codebaseSurvey=false`）⇒ **那次会话的转写不会自动入库**。

**想让记忆真的写进去，三选一**：

1. **在一个「有 `hindsight_*` 工具」的会话里**调用
   `hindsight_ingest_document(title="COE 层功能清点与变更史（第一层 / createoreexpansion）", content=<本文件正文>)`。
   这是最直接的一条，且**不受 `retainSessions` 影响**（手动工具直连 `client.retain`）。
2. **想让历史会话自动入库**：把 `retainSessions` 改回 `true` —— ⚠ 但这条与用户 2026-09-10 的既有要求相反
   （「每次在需要我植入要求的情况下，才会自动导入」，**不允许每轮对话自动导入**）⇒ **改之前先问用户**。
   另注意：插件按 workspace 缓存运行时对象、**配置只在对象创建时读一次** ⇒ 改动在**下一个新会话**才生效。
3. **只留文件**（当前做法）：本文件就是版本化的历史，任何会话都能读它、往里追加；不依赖记忆库。

**一句话交接**：新会话只要在开头读本文件 §1–§5 就知道「COE 层现在有什么」，读 §6 就知道「它是怎么来的」，**并把每一轮改动追加到 §6.2**。

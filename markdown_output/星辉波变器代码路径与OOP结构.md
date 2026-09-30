# 星辉波变器（变体波）代码路径 + OOP 结构说明

> 目的：一眼看清"每块逻辑在哪个文件、扮演什么角色"，以及哪些文件还需要继续拆。
> 行数＝非空行数（2026-09-13 实测）。所有路径相对 `src/main/java/com/hjmmd_8/createoreexpansion/`。

---

## 1. 路径总览（按 OOP 分层）

### 1.1 机器本体（星辉波变器）

| 路径 | 行数 | 角色 |
|---|---|---|
| `content/machine/stellarwavetransmuter/StellarWaveTransmuterBlock.java` | 241 | **方块**：六向开口/朝向、穿波入口出口判定表 |
| `content/machine/stellarwavetransmuter/StellarWaveTransmuterBlockEntity.java` | 633 | **方块实体**：扫描编排、应力、护目镜入口、NBT 同步、`isMachinery`（各项实现已下沉到 `scan/`、`display/`、`payload/`） |
| `content/machine/stellarwavetransmuter/payload/TransmuterPayloadCollector.java` | **107** | **载荷收集**（本轮新抽）：`estimate`/`gather` 两个命名入口共用一条私有 `collect`，`Payload` 记录随之上移（含"取料来源位置"）；跳过口径由变器侧 `payloadGatherSkip()` 传入 |
| `content/machine/stellarwavetransmuter/scan/TransmuterScanner.java` | 138 | **扫描服务（纯读取）**：加热源最高档 `HeatReading`、加工机位置、已蓄满避雷针、载荷源设备计数 `DeviceCounts` |
| `content/machine/stellarwavetransmuter/display/TransmuterGoggles.java` | 168 | **视图**：护目镜面板渲染（读数由 `Readout` 传入，不反依赖方块实体） |
| `content/machine/stellarwavetransmuter/StellarWaveMachineIntegrations.java` | 154 | **集成门面**：可选模组（Vintage / CC&A / Optical）统一入口，`ModList.isLoaded` 守卫 |

### 1.2 机器目录（"哪些机器 → 提供哪些配方类型"）

| 路径 | 行数 | 角色 |
|---|---|---|
| `…/registry/StellarWaveMachineCatalog.java` | 196 | **登记表**：所有机器→配方类型的唯一清单（Create 原生 + 本模组 + compat） |
| `…/registry/StellarWaveMachineRegistry.java` | 186 | **注册表**：查询/解析、`isMachinery`（唯一"算不算加工机"判定）、`allRecipeTypes` |
| `…/registry/MachineStateSelector.java` | 28 | **策略接口**：机器实时状态 → 当前可用配方类型（真空室 mode / 杠杆锤锤下方块 / 磨轮等级） |

### 1.3 波与加工引擎

| 路径 | 行数 | 角色 |
|---|---|---|
| `content/charger/entity/AbstractChargerWaveEntity.java` | 634 | **波基类**：飞行、掉落物 sweep、撞墙、分裂契约（`createChildWave`/`onPayloadDistributedToChildren`）；命中分支链已委托 `charger/wave/WaveHitResolver`；5 个状态字段私有化 + 访问器 |
| `content/charger/wave/WaveHitResolver.java` | **184**（本轮新建） | **方块命中解析**（分支顺序即优先级）：避雷针 → 波闸 → 三差器 → 变器穿波 → 带槽方块钩子 → 撞墙钩子；主世界无命中才问 contraption / sub-level |
| `content/charger/entity/StellarWaveEntity.java` | **979** | **变体波（引擎）**：携带状态 + NBT + 引雷 + 分裂 + 释放载荷；候选检索/门槛、解析、排序、产物推导、环境判定与读数、产物落点、资源扣减、**加工编排**已分别委托给 `craft/` 八个类（各留一行壳/工厂/接口实现） |
| `content/charger/entity/ChargerWaveEntity.java` | 27 | 普通能量波实体（无载荷） |
| `content/charger/entity/ChargerWaveProcessor.java` | 159 | 普通波的 `createoreexpansion:charging` 加工 |
| `content/machine/stellarwavetransmuter/StellarWaveTransmuterPass.java` | 161 | **穿波转换**：原波 → 变体波（携带能力快照/载荷/引雷次数）※ 仍在 `charger/entity` 包（见 §3 约束） |
| `content/charger/wave/WaveMachineActions.java` | 357 | 调级器/波速调节器/各类差器**执行结果**（反弹/拐弯/均摊分裂/湮灭） |
| `content/charger/wave/WaveContraptionCollisions.java` | 368 | contraption 场景的同类判定 |
| `content/charger/wave/WaveSubLevelCollisions.java` | 156 | Sable 物理结构内的碰撞 |
| `content/charger/entity/ChargerWaveFx.java` | 135 | 粒子与绽放特效 |

### 1.4 载荷（模型的物质侧）

| 路径 | 行数 | 角色 |
|---|---|---|
| `content/charger/payload/WavePayloadGather.java` | 309 | **取料**（"傻抽"铁律在此类注释）：物品/流体/电量范围内取到上限 + 来源记录 |
| `content/charger/payload/WavePayloadRelease.java` | 328 | **余料处置**：三种口径 + 排除口径 `isStoreTarget` + 逐目标异常隔离 |

### 1.5 加工模型与族（craft 包）

| 路径 | 行数 | 角色 |
|---|---|---|
| `content/charger/craft/Candidate.java` | 70 | **候选模型**：配方 id + 配方 + 三类资源引用 + 族归属（`familyTarget/firstAux/energyRequired`） |
| `content/charger/craft/WaveResources.java` | 119 | **资源引用模型**：`AuxSource/AuxRef`、`FluidSource/FluidRef`、`EnergySource/EnergyDraw`（"就近优先、载荷兜底"三同构） |
| `content/charger/craft/WaveAuxResolver.java` | **364** | **辅料解析器**（本轮新抽）：三条通道的取/扣实现 + 估算零副作用 + 环境扫描 `ENV_RADIUS`/`envBlocks` + 诊断串 |
| `content/charger/craft/WaveCandidateOrdering.java` | **120** | **候选排序纯策略**（本轮新抽，零字段）：批锁 → 记忆配方 → 转速档 → 专用度，永不随机；LRU 状态由调用方传入 |
| `content/charger/craft/WaveCraftResults.java` | **426** | **产物推导策略族**（本轮新抽）：`Strategy{claims,compute}` + `STRATEGIES`（族→升级→锻造→默认 `RecipeApplier`）+ `Context{level,aux,debug}` |
| `content/charger/craft/WaveEnvironmentChecks.java` | **289** | **环境前置条件**（本轮新抽）：加热 / Vintage 压弯机头（全反射）/ fan 媒介；判定中心 = 命中点 + 波源 + 盆上搅拌器格；状态入参化、日志回调 |
| `content/charger/craft/WaveCandidateEvaluator.java` | **567** | **候选检索与逐条门槛**（本轮新抽）：`collect`（类型门 → 输入规模 → 辅料 → 流体 → 电量 → 材料 → 环境）；`evalCandidate`/`evalFamilyCandidate`/`matches`/`genericIngredientsMatch`/`matchesQuietly`/`allWaveRecipes`/`isTypeAllowed`/过滤器闸门三件套 + 两个上限常量；`Context` 注入世界/辅料解析器/携带状态/载荷三件套与 **debug + trace 两个**日志出口 |
| `content/charger/craft/WaveOutputPlacer.java` | **124** | **产物落点**（本轮新抽）：`insertBack`（扫整个容器找空位）/`dropAtBlock`/`fillBlock`/`fillNearby`/`fillNearestTank`/`chargeNearest`/`radiusBlocks`；纯函数式（`Level` + `center`），与载荷余料处置分开 |
| `content/charger/craft/WaveCraftConsumption.java` | **214** | **资源扣减**（本轮新抽）：`consumeForCraft`/`consumeAux`（载荷降序扣 + 容器逐槽取 + 手持物开关）/`consumeCraftFluid`/`consumeCraftEnergy`（并发缺口回流）/`isHeldItemRecipe`；`Host` 接口由实体实现（直通载荷对象，不给只读副本） |
| `content/charger/craft/WaveCraftExecutor.java` | **333** | **加工编排**（本轮新抽）：`handleInventoryBlock`（方块槽批次循环 + 批锁 + 链尽释放）/`tryCraft`（掉落物逐件）/`findCraftableSlot`/`craftFromSlot`/`applyCraft`；`Host` 25 个成员由实体实现，含 `handleInventoryBlockAsPlainWave()` 兜住基类退化路径 |
| `content/charger/craft/family/WaveRecipeFamilies.java` | 302 | **族登记表**：非 ProcessingRecipe 族（拆解、序列装配）的执行契约 |

### 1.6 引雷 / 闪电

| 路径 | 行数 | 角色 |
|---|---|---|
| `content/lightning/LightningEventHandler.java` | 158 | **事件层（只剩分发与时序）**：首 tick 去重 → 调加工服务；单物品兜底；火焰生成后才做方块转化 |
| `content/lightning/LightningStrikeProcessor.java` | **282** | **落点加工服务**（本轮新抽）：来源收集 + 多输入配方优先匹配 + 产出分发；CC&A 充电只经 compat 门面 |
| `content/lightning/block/ReinforcedLightningRodBlockEntity.java` | 172 | 强化避雷针：伽马蓄能、满充能待释放、客户端同步 |
| `content/lightning/ReinforcedLightningRodEffects.java` | 38 | 闪电/粒子生成（纯视觉闪电的口径在此） |
| `content/lightning/LightningRecipe.java` / `LightningBlockRecipe.java` | 51 / 25 | 闪电配方类型 |

### 1.7 compat（可选模组隔离层）

| 路径 | 行数 | 角色 |
|---|---|---|
| `compat/jade/WaveJadePlugin.java` | 297 | 波的 Jade 提示（等级/速度/寿命/携带类型/载荷/引雷） |
| `compat/jade/BasinLiveJadePlugin.java` | 约 40 | 工作盆物品行：注册接管用 |
| `compat/jade/BasinLiveItemStorage.java` | 111 | 工作盆物品行**整条接管**（实时数据 + Jade 原生渲染） |
| `compat/jei/category/StellarWaveTransmuterCategory.java` | 133 | JEI 变器页 |
| `compat/createaddition/CreateAdditionTransmuterSupport.java` | 179 | CC&A：线圈抽电、配方耗电、额外类型、雷击类型口径 |
| `compat/createaddition/TeslaCoilWaveCharger.java` | 203 | 线圈作为充能源的桥接 |
| `compat/vintageimprovements/VintageImprovementsMachineIntegration.java` | 175 | Vintage：加工机登记 + 真空室/杠杆锤状态选择器 |
| `compat/optical/OpticalMachineIntegration.java` | — | Create Optical 聚光器登记 |

### 1.8 配置 / 工具 / 数据

| 路径 | 角色 |
|---|---|
| `common/AllConfig.java` | 配置：`[grinding]` / `[charger]` / `[wave]`（取料上限、余料口径、类型门、手持物扣料…） |
| `util/RecipeTypeNames.java` · `util/HeatLevelNames.java` · `util/GoggleUtil.java` | 类型名/热档名/护目镜排版工具 |
| `data/lang/ChineseLangProvider.java` · `EnglishLangProvider.java` | 中英词条（Jade/护目镜/JEI 文案、配方类型名） |
| `markdown_output/变器配方判定与执行逻辑.md` | **设计口径**（判定/执行/载荷/引雷全流程） |
| `markdown_output/星辉波变器代码审查与结构地图.md` | 审计结论 + 分包进度 |
| 本文件 | 路径与 OOP 结构地图 |

---

## 2. OOP 分层小结

```
方块/实体        StellarWaveTransmuterBlock · StellarWaveTransmuterBlockEntity
   ├─ 扫描服务    scan/TransmuterScanner          （纯函数：输入 level+pos+radius，输出记录）
   ├─ 视图        display/TransmuterGoggles       （输入 Readout，输出面板行）
   ├─ 目录/策略   registry/*                      （登记表 + 状态选择器策略）
   └─ 集成门面    StellarWaveMachineIntegrations   （可选模组隔离）
波/引擎          AbstractChargerWaveEntity（基类） → StellarWaveEntity（变体波）
   ├─ 模型        craft/Candidate · craft/WaveResources        （不可变数据载体）
   ├─ 族契约      craft/family/WaveRecipeFamilies              （可执行族登记）
   └─ 物质        payload/WavePayloadGather · payload/WavePayloadRelease
闪电             content/lightning/*（闪电落地统一加工）
可选模组          compat/*（Jade/JEI/CC&A/Vintage/Optical，全部 isLoaded 守卫）
```

**两条设计取向**（都是实测/审计换来的，改前先读）：
1. **视图与方块实体解耦**：`TransmuterGoggles` 只吃 `Readout`，所以它不能反向读到"变器私有字段"，读数口径（"只报读到什么、绝不报坐标"）只有一处。
2. **模型不用 record 而用 `public final` 字段**：这些类型原是波实体内的**私有嵌套 record**，40+ 处调用点写的是字段式访问；抽成顶层 record 后字段转为私有会让调用点全部编译不过。现形态＝不可变数据载体 + 同名访问器，既有调用点零改动。

---

## 3. 拆分中的两条硬约束（继续拆之前必须知道）

1. **`protected` 同包访问**（2026-09-13 更新）：
   - ✅ **字段侧已解决**：`movement`/`speedOffset`/`waveLevel`/`renderColor`/`spawnPos` 已改为 `private` 并补了 `get/setMovement`、`get/setSpawnPos`、`get/setRenderColor`、`getSpeedOffset`（等级沿用 `getWaveLevel()`），4 个助手/穿波类共 33 处写 + 约 120 处读已改走访问器。
   - ⚠️ **方法侧仍在**：`createChildWave(...)` / `setWaveLevel(int)` / `addSpeedOffset(double)` 仍是 `protected`，`WaveMachineActions`、`WaveContraptionCollisions`、`StellarWaveTransmuterPass` 会调用它们 → **这三个助手要搬出 `content/charger/entity/`，就必须把这三个契约也放宽成 `public`**（等于反向削弱封装）。当前取舍：**留在同包**，只把不碰这三个契约的 `WaveHitResolver` 放进 `charger/wave/`。
2. **嵌套类型的私有字段**：嵌套 record 的字段在"同一外层类"内可直接访问，一旦搬到别的类就变私有 → 搬模型类型时要保持"字段可直读"的形态（见 §2 第 2 条）。

---

## 4. 仍偏大的文件与建议拆分方案

| 文件 | 行数 | 建议拆法 |
|---|---|---|
| `StellarWaveEntity` | **979**（原 3041） | 按**职责块**继续下沉（每块用"参数化 + 同包/跨包"两种手法）：~~① `craft/WaveAuxResolver`~~ ✅（364）；~~② `craft/WaveCraftResults`~~ ✅（426）；~~③ `craft/WaveCandidateOrdering`~~ ✅（120）；~~④ `craft/WaveCandidateEvaluator`~~ ✅（567）；~~⑤ `craft/WaveOutputPlacer` 124 + `craft/WaveCraftConsumption` 214 + `craft/WaveCraftExecutor` 333~~ ✅；~~⑥ `craft/WaveEnvironmentChecks`~~ ✅（324）；**剩**：⑦ `summonLightningAt` 与分裂/补料/释放载荷仍可再各成一块（但已无"整块职责"级的大件，实体主要是状态访问器 + NBT + 三个 `Host` 实现） |
| `AbstractChargerWaveEntity` | **634**（原 702） | ~~字段封装~~ ✅；~~命中判定 → `charger/wave/WaveHitResolver`~~ ✅（184 行）；**剩**："飞行/速度/粒子"可抽 `WaveMotion`；`triggerBoom`/波波湮灭可抽 `WaveAnnihilation` |
| `LightningEventHandler` | 158（原 391） | ~~拆 `lightning/LightningStrikeProcessor`~~ ✅ 已落地（282）；剩"方块转化"可再抽 `lightning/LightningBlockTransform`（`prepareBlockTransform` + 暂存表） |
| `WaveContraptionCollisions` / `WaveMachineActions` | 366 / 355 | 属于同一包约束下的助手；建议把"纯判定"（分散度/出口方向）留在 regulation 类，"副作用"（分裂/反弹/湮灭）收敛到 `WaveSplitExecutor` |
| `StellarWaveTransmuterBlockEntity` | **633**（原 685） | ~~载荷收集 → `payload/TransmuterPayloadCollector`~~ ✅（107 行）；**剩**：`scan` 里的"半径解析"（`resolveRadius`/`fieldControllerSpeed` 读能量场档位） |
| `StellarstoneStressChargerBlockEntity` / `AbstractCreateChargerBlockEntity` | 391 / 340 | 充能器体系（非本次机器）留给后续；套路同上：`charger/store/StoreLayers` + `charger/recipe/ChargingExecutor` |

**拆分优先级建议**：`StellarWaveEntity` 的 ①②③ 与闪电的处理器**已完成**（见上表）；下一步先做基类的 getter 封装（解锁 `StellarWaveTransmuterPass` 与三个助手搬包 + `WaveHitResolver`），再回来处理波实体的 ④⑤⑥，最后充能器体系。

---

## 5. 三条硬约定（用户 2026-09 明确；改动前必读）

1. **联动（compat）内容必须独立成层，核心层绝不硬编码别的模组**
   - 别的模组的类/方块/配方类型/字符串 id **只允许**出现在 `compat/<mod>/` 下；
   - 核心层（`content/*`）只允许看见**中立类型**（`Recipe`/`RecipeHolder`/`ItemStack`/`FluidStack`/`BlockPos`…）与"装没装"这一个布尔问题；
   - 现成范式：`compat/createaddition/CreateAdditionTransmuterSupport` 就是 CC&A 的门面（`isLoaded()` / `findChargingRecipe` / `addChargingRecipes` / `matchesChargingRecipe` / `rollChargingResults` / `extraEnergyRecipeTypes` / `strikeHandledTypeIds` / 线圈抽电），核心层只调这些方法。
   - **已整改**：`content/lightning/LightningEventHandler` 原有 5 处以全限定名直接引用 CC&A 类（`com.mrh0.createaddition.*`），现已全部改为调门面；复查结果：`content/*` 下对 `com.mrh0` / `com.negodya1` / `snownee` / `mezz` 的直接引用数为 **0**（Create 本身是硬依赖，`com.simibubi.*` 允许）。
   - 新增联动时的顺序：先写 `compat/<mod>/XxxSupport` 门面 → 再由 `content` 的集成入口（如 `StellarWaveMachineIntegrations`）用 `ModList.isLoaded` 守卫后唤醒 → 门面内部所有第三方类引用都放在方法体里（类加载期不解析）。

2. **用抽象/继承消除重复，不复制粘贴**
   - 同构行为抽成**基类/接口 + 模板方法**，或抽成**参数化的协作类**（本轮已验证两种手法都可行）；
   - 典型可抽象点：三类资源引用同构（`WaveResources` 已集中）、"命中 → 候选 → 门槛 → 执行"流水线（下一步抽 `craft/WaveRecipeExecutor`）、分裂/反弹/湮灭的副作用（抽 `WaveSplitExecutor`）；
   - 禁止把 `content` 的判定逻辑在 compat 层再写一份（例如"算不算加工机"只能有 `StellarWaveMachineRegistry#isMachinery` 一处）。

3. **分包后再动行为**：结构性搬移（改包名/抽类）与行为修改分两次做，各自单独提交，便于按 diff 复核；跨包搬助手前先检查 `protected` 同包访问与嵌套类型的私有字段（见 §3）。


# 06 - 对 COE / CEWS 现有代码的观察

> **本文只报告、不修改。** 没有改动 `createoreexpansion` 的任何源码、构建脚本或资源文件。
>
> **阅读快照**：2026-09-30 约 13:00–13:20（本机时间）。当时工作区里**已经有别人未提交的盔甲相关改动**
> （`coe/.../CoeArmorItems.java`、`CoeArmorMaterials.java`、`content/equipment/armor/`、
> 16 张物品贴图、8 张装备层贴图、`data/minecraft/tags/item/*_armor.json` 等都是 `?? 未跟踪` 或 `M`）。
> 我**没有动其中任何一个文件**；下面的结论只对**那一时刻的那一份工作区**成立。
> 如果之后有人继续改盔甲代码，请以 `git diff` 为准重新核对。
>
> **诚实声明**：本次审阅**没有跑过游戏**（没有 `runServer` / `runClient` / `runData`）。
> 因此下面每一条都标了依据等级：`【读源码】`（给出类名行号）、`【像素实测】`（直接读文件算出来的）、
> `【资源实测】`（直接读资源清单/JSON）、`【推断】`。**凡标【推断】的都只是推断**，
> 每条都给了"怎么才能确认"。
>
> 审阅范围：盔甲（`ArmorItem` / `ArmorMaterial` / 贴图 / 纹饰 / 属性）、工具（`Tier` /
> `TieredItem` 子类 / 属性修饰符）、材料链（锭/粒/原料/块/标签/配方）、自定义效果与战斗逻辑、
> 以及与它们直接相关的 CEWS 侧引用。**没有**审阅波系统、变器配方执行、能量场、技能内核等与本主题无关的部分。

---

## 一、确定的 bug（2 条）

### 1. 翠玉盔甲的两张装备层贴图**互换了** ⇒ 头盔完全不渲染

| 项 | 内容 |
|---|---|
| **文件** | `coe/src/main/resources/assets/createoreexpansion/textures/models/armor/jade_armor_layer_1.png`<br>`coe/src/main/resources/assets/createoreexpansion/textures/models/armor/jade_armor_layer_2.png` |
| **相关代码** | `coe/src/main/java/com/hjmmd_8/createoreexpansion/content/equipment/armor/CoeArmorItem.java:55`（`slot == LEGS ? 2 : 1`）<br>`coe/src/main/java/com/hjmmd_8/createoreexpansion/common/registry/coe/CoeArmorItems.java:60`（`textureBase = "jade_armor"`） |
| **现象**（【推断】，机制见下） | 穿翠玉头盔时**头盔完全不显示**（人物头部只有默认皮肤）；翠玉胸甲的**两条手臂不显示**（躯干正常）；翠玉靴子正常；翠玉护腿会显示，但用的是本该给头盔/胸甲的那张画。 |
| **依据** | **【像素实测】** 逐区域数不透明像素：<br>`jade_armor_layer_1.png`：head 区(0,0~32,16)=**0**、arm 区(40,16~56,32)=**0**、body=120、leg=160<br>`jade_armor_layer_2.png`：head=**228**、arm=**104**、body=238、leg=112<br>对照另外三套（全部正常）：`*_layer_1` 的 head 约 240、arm 约 98~144；`*_layer_2` 的 head/arm **都是 0**。<br>**【读源码】** `HumanoidModel.java:82`（head `texOffs(0,0)`）、`:97/:102`（arm `texOffs(40,16)`）、`:107/:112`（leg `texOffs(0,16)`）；`HumanoidArmorLayer.java:80/163-165`（`LEGS` 用内层=layer_2，其余用 layer_1）。 |
| **根因** | 两张 PNG 的内容与文件名不匹配（`_layer_1` 里装的是"只有躯干+腿"的护腿画，`_layer_2` 里装的是"有头有臂"的身体画）。`runData` / `check-asset-attribution` 之流只校验**文件存在**，不看内容，所以一路绿灯。 |
| **建议改法** | **把这两个文件的内容对调**（改名/截图前请先跟用户确认，因为本仓库的红线是"贴图是用户资产，只报告不动"）。**不要**改代码去适配错误的贴图。 |
| **如何验证** | ① 复跑本文 README §四的 `Add-Type ... Region` 脚本，断言：`*_layer_1` 的 head>0 且 arm>0，`*_layer_2` 的 head==0 且 arm==0（这套断言建议直接做成脚本留在 `tools/`）。<br>② 进游戏：创造模式拿齐 4 件翠玉盔甲穿上，转一圈看头盔/护臂有没有渲染（**这一步只能人眼**）。<br>③ 反向确认：把 `jade_armor_layer_1.png` 临时换成 `gem_armor_layer_1.png`，头盔应该立刻出现。 |

> 附带结论：**`runData` 通过并不代表贴图是对的。** 这一条正好印证 `04` §3.3 ——
> "存在性"与"正确性"是两件事。

---

### 2. `lightning_block`（方块雷击加工）**整条链路不可达**：实现是死代码、数据是死数据、JEI 却在展示

| 项 | 内容 |
|---|---|
| **文件** | `coe/src/main/java/com/hjmmd_8/createoreexpansion/content/lightning/LightningEventHandler.java:35`（`PENDING_BLOCK_TRANSFORMS` 声明）、**`:150-189`（`prepareBlockTransform(...)` 定义，`private`、全仓**零调用**）**、`:75`（`PENDING_BLOCK_TRANSFORMS.remove(...)`）<br>`coe/src/main/resources/data/createoreexpansion/recipe/lightning_block/stone_to_diamond_ore.json`<br>`coe/src/main/java/com/hjmmd_8/createoreexpansion/common/registry/coe/CoeJeiCategories.java:42-48`<br>`coe/src/main/java/com/hjmmd_8/createoreexpansion/compat/jei/CreateOreExpansionJEI.java:65`（收集类别）、`:75`（注册类别）<br>`coe/src/main/java/com/hjmmd_8/createoreexpansion/content/charger/craft/WaveCandidateEvaluator.java:658-663` |
| **现象** | JEI 里有「方块雷击加工」类别、能查到 `stone → diamond_ore` 这条配方；但游戏里**永远不会有方块被雷击转化**。（另外：强化避雷针释放的是 `setVisualOnly(true)` 的闪电，见 `03` §6.4，它不会触发 `EntityStruckByLightningEvent`。） |
| **依据** | **【读源码】** 全仓 `grep prepareBlockTransform` 只有 3 处命中：声明（:35 的 map 字段）、定义（:150）、以及 :75 的 `remove`。**没有任何一处调用它，也没有任何一处往那张 map 里 `put`。**<br>⇒ `:75` 的 `remove(bolt.getUUID())` **必然返回 `null`**，`:76-77` 立刻 `return` ⇒ `onLightningBoltPostTick`（:68-91）整体是死代码。<br>`WaveCandidateEvaluator.java:658-663` 把 `LIGHTNING_BLOCK` 从"全库池"里排掉，注释（`StellarWaveMachineCatalog.java:63`）说它"走避雷针释放机会专用路径"——而那条专用路径的实现就是 `prepareBlockTransform`。<br>**【资源实测】** `data/createoreexpansion/recipe/lightning_block/` 下只有 1 个文件，`type` 为 `createoreexpansion:lightning_block`，游戏会加载它，但没有任何代码查询 `CoeRecipeTypes.LIGHTNING_BLOCK`（唯一查询点就是那段死代码）。 |
| **根因** | `LightningEventHandler` 在一次重构/收口中被拆成了"物品路径（`LightningStrikeProcessor.processBoltStrike`，活）"与"方块路径（`prepareBlockTransform` + `PENDING_BLOCK_TRANSFORMS` + `onLightningBoltPostTick`，未接线）"两半，**方块那半的接线丢了**。JEI 侧仍在展示，所以从玩家视角是"规则写了但不生效"。 |
| **建议改法（三选一，需作者裁定）** | **(a) 接上**：在 `EntityTickEvent.Pre`（`onLightningBoltTick`，:52-65）里对落点调用 `prepareBlockTransform`，把返回值 `put` 进 `PENDING_BLOCK_TRANSFORMS`，交给 `onLightningBoltPostTick` 执行——这正是 :146-149 注释描述的设计意图。<br>**(b) 删干净**：删掉 `prepareBlockTransform` / `PENDING_BLOCK_TRANSFORMS` / `onLightningBoltPostTick` / 那个 recipe 文件 / JEI 类别，避免"看起来能用"。<br>**(c) 明确降级**：保留实现但把 JEI 类别摘掉，并在类注释里写明"暂未接线"。<br>⚠️ 无论选哪个，**都要连带处理 `WaveCandidateEvaluator` 的那条排除**（否则变器会把该类配方当成"已由专用路径处理"而跳过）。 |
| **如何验证** | ① 静态：`grep -rn "prepareBlockTransform\|PENDING_BLOCK_TRANSFORMS" coe cews core` ⇒ 应能看到"有调用点"（当前看不到）。<br>② 动态：`runServer` 里放一块石头 + 一个已蓄满的强化避雷针，右键释放引雷（或等自然雷），看石头是否变成钻石矿。建一个临时探针在 `BlockDropsEvent`/`Block` 层打日志，或在 `prepareBlockTransform` 首行打一行 `LOGGER.info`——**如果那行永远不打印，就是本条成立**。<br>③ 顺带确认 JEI：装 JEI 进游戏，看「方块雷击加工」类别是否存在（应当存在，`CreateOreExpansionJEI.java:65` 已把它收进列表）。 |

---

## 二、风险 / 建议核实的点（8 条）

> 这一档的共同特征：**当前不一定是错的，但"改一下就会静默坏"或者"规模一上来就出问题"。**
> 每条都给了最小验证方式。

### R1. 每 tick 遍历全维度所有实体（性能 / 网络包）

- **文件**：`coe/.../content/equipment/medallion/handler/MedallionEffectHandler.java:112-149`
- **看到什么**：【读源码】`onServerTick(ServerTickEvent.Post)` 里
  `for (ServerLevel level : event.getServer().getAllLevels()) for (Entity entity : level.getEntities().getAll())`，
  对每个 `ItemEntity` 至少做一次 `getFluidTypeHeight(嬗变液)`（:119-120），命中系列物品时还会
  `setInvulnerable` / `setRemainingFireTicks(0)` / `setGlowingTag(...)` / `setNoGravity(true)`（:121-144）。
- **为什么是风险**：`getEntities().getAll()` 每 tick、每维度构造一次全实体集合；
  大型整合包里实体以万计，这是 O(实体数) 的每 tick 成本。`setGlowingTag` / `setNoGravity` /
  `setInvulnerable` 都是**同步实体数据**（`SynchedEntityData`），值没变时通常不会再发包，
  但依然有装箱与比较开销。
- **建议**：① 先把"与本模组系列无关的物品"在**最早**处短路（例如先判 `item.getItem().getItem()` 的命名空间，
  或用一个 `Set<Item>` 缓存）；② 更彻底：改成只在"物品实体 tick"或"进入新方块/新流体时"处理。
- **如何验证**：在 `ServerTickEvent.Pre/Post` 里用 `System.nanoTime()` 打这段耗时，
  与"注释掉这段"对比；或在有大量掉落物的场景（刷怪塔/开采现场）看 MSPT。
  **注意**：本仓库 `AGENTS.md` 已说明 tick 类问题静态关卡覆盖不到，必须实测。

### R2. `AllTiers` 的耐久注释与 1.21.1 实际语义相反

- **文件**：`coe/.../common/registry/coe/AllTiers.java:43`（`@param uses 耐久基数，耐久基数加上工具的耐久修正值才是耐久度。`）
- **依据**：【读源码】`TieredItem.java:7`：`super(properties.durability(tier.getUses()))`
  ⇒ **`getUses()` 就是最终耐久**，1.21.1 没有"基数 + 修正"这一步。
- **影响**：后续维护者若照注释"加修正值"，会得到一个两倍左右的耐久；**编译与静态关卡都不会报**。
- **建议**：改注释为「工具的总耐久（直接进 `Item.Properties.durability`）」。
- **如何验证**：`runServer` 探针打 25 件器具的 `getMaxDamage()`，应等于 1600/1828/2048/2400/2400。

### R3. 用 `ArmorItem.Type.ordinal()` 当护甲值数组下标

- **文件**：`coe/.../common/registry/coe/CoeArmorMaterials.java:140-145`
- **看到什么**：【读源码】`for (ArmorItem.Type type : ArmorItem.Type.values()) if (type.ordinal() < defense.length) defenseMap.put(type, defense[type.ordinal()]);`
  而调用点传入的数组是 `{2,6,5,2}` 这种 4 元数组，注释（:124-125）要求"顺序必须是 `ArmorItem.Type` 的枚举声明序"。
- **为什么是风险**：`ArmorItem.Type` 的声明序是 `HELMET(0) / CHESTPLATE(1) / LEGGINGS(2) / BOOTS(3) / BODY(4)`
  【读源码】`ArmorItem.java:149-154`。**数组顺序写错不会编译失败、不会运行报错**，
  只会把护甲值配到别的槽位（`BODY` 被 `type.ordinal() < 4` 守卫跳过，得到 `getOrDefault(type, 0)` = 0）。
- **建议**：改成显式命名参数（`helmet(...)/chest(...)/legs(...)/boots(...)`）或 `switch (type)` + `default -> throw`，
  让"少写/写错槽位"变成编译期或启动期错误。
- **如何验证**：探针逐件打 `ArmorItem#getDefense()`（`01` §2.4 的骨架），
  与设计表（翠玉 2/6/5/2、宝石 3/7/5/2、星界 3/8/6/3、雷鸣 3/8/6/3）逐件对齐。

### R4. 全仓没有 `minecraft:smelting` / `minecraft:blasting` 配方

- **范围**：`coe/src/**` + `cews/src/**` + `core/src/**` + 根 `src/**` 的所有 `*.json`（排除 `build/`、`_create_src/`）
- **依据**：【资源实测】grep `minecraft:(smelting|blasting)` **零命中**。
  矿石掉落 `raw_*`（例如 `coe/src/generated/resources/data/createoreexpansion/loot_table/blocks/jade_ore.json` → `createoreexpansion:raw_jade`），
  而 `raw_* → *_ingot` 的配方只存在于 Create 的加工链里（`crushing/raw_jade.json` → `crushed_*` → `splashing/...` → 粒 ×9 → 锭）。
- **对照物（这条让"更像漏了"的判断有依据）**：【资源实测】Create 本体**为自家的锌同时提供了两条路**：
  `_create_src/data/create/recipe/smelting/zinc_ingot_from_raw_ore.json`（`minecraft:smelting`，原矿直烧）
  与 `.../smelting/zinc_ingot_from_crushed.json` + `.../blasting/zinc_ingot_from_crushed.json`（粉碎矿路线），
  此外还有 `zinc_ingot_from_ore.json`（烧原矿方块）。
  ⇒ 「Create 附属模组的矿石也该能进原版熔炉」是这个生态的**既有惯例**，本模组 5 种材料一个都没有。
- **为什么仍然列成风险而不是 bug**：本模组是"机械动力矿物扩展"，"必须走机械加工"也可能是有意的差异化设计。
  **这是产品决策，代码里读不出来。**
- **怎么确认**：问作者一句"原版熔炉该不该能烧 `raw_*`"就够了。
- **若确认要补**：只需给每种 `raw_*` 加一条 `minecraft:smelting` + 一条 `minecraft:blasting`
  （不要手写进 `src/main/resources` 与 datagen 撞名——本仓库 `AGENTS.md` 记过 duplicate 会让构建失败）。

### R5. `armorTag` 的 `default -> ItemTags.HEAD_ARMOR` 会把 `BODY` 误归到头盔

- **文件**：`coe/.../common/registry/coe/CoeArmorItems.java:198-206`
- **依据**：【读源码】`switch (type) { ... default -> ItemTags.HEAD_ARMOR; // BODY（狼/马甲）本模组不使用 }`。
  但 **1.21.1 原版根本没有 `body_armor` 这个物品标签**（【资源实测】`data/minecraft/tags/item/` 下只有
  `head_armor/chest_armor/leg_armor/foot_armor`；【读源码】`ItemTags.java:118-121` 也只有这四个），
  狼铠本来就不进任何槽位标签。
- **影响**：当前不用 `BODY` ⇒ 无影响。将来若加"马铠/狼铠"类物品，它会被静默塞进 `#minecraft:head_armor`
  ⇒ 变成可被头盔类附魔附上。
- **建议**：`case BODY -> throw new IllegalArgumentException("COE 不使用 BODY 盔甲槽")`，
  把"误用"变成启动期错误。
- **如何验证**：注册一件 `ArmorItem.Type.BODY` 的测试物品，检查它是否出现在
  `#minecraft:head_armor` 里（探针打 `new ItemStack(item).is(ItemTags.HEAD_ARMOR)`）。

### R6. CEWS 用**反射**读 COE 方块实体的私有字段，失败时完全静默

- **文件**：`cews/.../content/machine/stellarwavetransmuter/StellarWaveTransmuterPass.java:180-207`
- **看到什么**：【读源码】`ReinforcedLightningRodBlockEntity.class.getDeclaredField("gammaChargeProgress")`
  / `getDeclaredField("readyCharges")` + `setAccessible(true)` + `setInt(...)`，
  整段包在 `catch (Throwable ignored) { return false; }`（:204-206）。
- **为什么是风险**：`coe/.../content/lightning/block/ReinforcedLightningRodBlockEntity.java:45 / :47`
  里这两个字段是 `private`。**只要有人改名/改类型，这里会静默返回 false**——
  症状是"变器再也吸不到避雷针的释放机会"，而且**日志里什么都没有**。
  这正好是本仓库 `AGENTS.md`「隐藏契约」那类问题的又一个实例。
- **建议**：既然依赖方向 `cews → coe` 是允许的，就在
  `ReinforcedLightningRodBlockEntity` 上加一个公开方法（例如 `public boolean consumeReadyCharge()`，
  内部做清零 + `setChanged()` + `blockChanged`），让 CEWS 直接调用；删除反射。
  这一步还能顺手把 `syncToClient()`（:160-164）复用起来，避免两处各写一份同步口径。
- **如何验证**：① 静态：`grep -rn "getDeclaredField" cews core coe` 应只剩合理用途；
  ② 动态：把 `gammaChargeProgress` 临时改名，看变器是否还吸得到——
  按当前写法**不会有任何日志**，这正是需要修的理由（至少失败时也该 `LOGGER.warn` 一次）。

### R7. `visualOnly` 闪电仍会激活避雷针 / 给铜块除锈

- **文件**：`coe/.../content/lightning/ReinforcedLightningRodEffects.java:28`（`bolt.setVisualOnly(true)`）
- **依据**：【读源码】`LightningBolt.tick`：`:116 spawnFire(4)`（内部才判 `!visualOnly`）、
  **`:119 powerLightningRod()` 与 `:120 clearCopperOnLightningStrike(...)` 不受 `visualOnly` 影响**、
  `:155 } else if (!this.visualOnly) {` 只控制"对实体结算"。
- **影响**：注释写的是"纯视觉，不引燃、不生成骷髅陷阱马，规避原版漏洞"——**这两点是真的**；
  但"顺手激活避雷针 + 除铜锈 + 抛 `LIGHTNING_STRIKE` 游戏事件"没有写在注释里。
  如果作者认为"纯视觉就该什么都不做"，这里需要覆盖 `spawnFire`/自己控制激活，而不是只 `setVisualOnly`。
- **如何验证**：`runClient` 里在强化避雷针旁边放氧化铜块，右键释放引雷，看铜块是否除锈、避雷针是否进入 POWERED。

### R8. `TransmutationDisorderEffect` 的两处小语义问题

- **文件**：`coe/.../content/transmuting/effect/TransmutationDisorderEffect.java`
- (a) `:79` `target.hurtAndBreak(level, player, EquipmentSlot.MAINHAND)` —— 目标可能是背包里任意一格，
  却报"主手"。影响：破碎动画/统计口径不准（不至于崩）。**【推断】** 建议传该物品实际所在槽位，或传 `null`。
- (b) `:82-91` `isTool(ItemStack)` 的判定是"可损坏 且 不是盔甲/方块物品/鞘翅" ⇒ **过宽**：
  会命中本模组的凝能佩（`BaseStressMedallionItem` 家族）、Create 的护目镜/扳手等非工具物品。
  影响：嬗乱会随机扣这些东西的耐久，玩家观感是"我的饰品莫名其妙掉耐久"。
  **【推断】** 建议改成白名单（`#createoreexpansion:skill_tools` / `#createoreexpansion:cooldown_tools`
  这两个标签本仓库已经存在，见 `coe/src/generated/resources/data/createoreexpansion/tags/item/`）。
- **如何验证**：装 Curios + 若干饰品，在嬗变液里泡到嬗乱，统计哪些物品掉了耐久（探针可在 `hurtAndBreak` 附近打日志）。

---

## 三、我核对过、**是对的**（附依据，不凑数）

这些位置我逐个读过，**没有发现问题**。列出来是为了让接手的人知道"这些不用再查一遍"。

| # | 项目 | 依据 |
|---|---|---|
| 1 | 四套 `ArmorMaterial` 的字段与顺序 | 【读源码】`CoeArmorMaterials.java:65-102` 与 `ArmorMaterial.java:13-21` 的 7 个字段位置一致；`repairIngredient` 用了 `Supplier`（:72 等），**正确** |
| 2 | 四套盔甲的**耐久数值** | 【读源码】`CoeArmorItems.java:138-169`：36/47/74/74 × 倍率 11/16/15/13，与设计表逐件吻合；`armor()` 用 `Properties.durability(...)`（:190）——1.21.1 的唯一正确位置 |
| 3 | 16 件盔甲**全部进了槽位标签** | 【资源实测】`coe/src/generated/resources/data/minecraft/tags/item/{head,chest,leg,foot}_armor.json` 各 4 条，与 `CoeArmorItems.java:59-120` 的注册 id 一一对应 ⇒ 附魔路径通畅（`01` §4） |
| 4 | **16 张物品图标**的部位与尺寸 | 【像素实测】全部 16×16；逐行不透明像素宽度的轮廓与部位一致（头盔圆顶、胸甲肩片、护腿两条分离腿、靴子下宽上窄）⇒ 图标没有错配 |
| 5 | **装备层贴图的 alpha 通道** | 【像素实测】8 张全部保留透明背景（`alpha==0` 的像素占多数，无"整块不透明"）⇒ 不会渲染成实心方块 |
| 6 | **另外三套**（宝石/星界/雷鸣）的 6 张层贴图区域分布 | 【像素实测】`*_layer_1` 有头有臂、`*_layer_2` 头臂为 0（见 `01` §3.2 的表） |
| 7 | 全部 **8 张层贴图互不重复** | 【像素实测】SHA256 两两不同 ⇒ 没有"复制一份改名"的偷懒 |
| 8 | 五个 Tier 的六个方法实现 | 【读源码】`AllTiers.java:69-97` 全部正确覆写；`repairIngredient` 延迟求值（:96），`CoeItems.<clinit>` 读 `AllTiers.JADE` **不构成类初始化环**（`02` §4） |
| 9 | **25 件器具全进了类别标签** | 【读源码】`CoeItems.java:143/164/184/205/225`（翡翠）等 5 组，各挂 `ItemTags.SWORDS/PICKAXES/AXES/SHOVELS/HOES`；【资源实测】生成的 5 个类别标签各 5 条 ⇒ **附魔台能附**（`01` §4） |
| 10 | `piglin_loved.json` 的用法 | 【资源实测】`coe/src/main/resources/data/minecraft/tags/item/piglin_loved.json` 只有 `sapphire_ingot` / `sapphire_block`；本模组**没有蓝宝石盔甲** ⇒ 不会出现"猪灵捡走你的盔甲"；且该文件在 `src/main/resources`，`src/generated` 下无同名文件 ⇒ 不会撞 duplicate（`01` §6） |
| 11 | `MedallionEffectHandler.onEffectApplicable` 的写法 | 【读源码】`:78` 用 `Holder` 身份比较（**正确**），`:80` 用 `Applicable.Result.DO_NOT_APPLY`（`Applicable` 不是 `ICancellableEvent`，**用对了**）；类注释 :47-55 把这个坑记下来了 |
| 12 | `TransmutationDisorderEffect` 的节流方式 | 【读源码】`:37-39` 恒 `true` + `:51` 用 `entity.tickCount % 20` —— 恰好绕开了"效果被每 tick 刷新时 `duration % N` 恒真"的陷阱（`03` §2.3）；本仓库的 `TransmutationEventHandler.java:75` 正是每 tick 刷新的写法，两者配套是**对的** |
| 13 | `CoeArmorItem.getArmorTexture` 的行为 | 【读源码】`:55-58` 拼出的路径与 `ArmorMaterial.Layer.resolveTexture`（`ArmorMaterial.java:47-50`）**完全一致** ⇒ 冗余但**行为正确**，不会造成显示差异（见 `01` §1 末） |
| 14 | CEWS 侧**没有**盔甲/工具/材料注册 | 【读源码】`cews/src/main/java/**` 的包结构只有 `client/*`、`common/registry/cews`、`compat/*`、`content/{charger,energyfield,machine,wave}` ⇒ 盔甲与器具这一主题**只属于 `:coe`**，没有跨层重复注册的风险 |

---

## 四、我**没能确认**的部分（诚实清单）

1. **没有跑过游戏**（没有 `runServer` / `runClient` / `runData`）。
   `06` 第一节第 1 条的"头盔不渲染"是**由像素分布 + 模型 UV 推出的**，不是看到的。
   机制层面我核对到了源码（`HumanoidModel` 的 `texOffs` + `HumanoidArmorLayer` 的选层），
   所以我对结论有信心，但**严格说它是【推断】**。
2. **`jade_armor_layer_*` 到底是"Swap"还是"当年就画成这样"**：我只能证明"内容与文件名语义不符"，
   不能证明它是操作失误。两种情况的修法相同（把内容对调），但**要不要动用户素材必须先问作者**
   （本仓库红线：贴图是用户资产，只报告不动）。
3. **`lightning_block` 的三选一该选哪个**：这是产品决策，不是技术判断。我只能证明"当前不可达"。
4. **R4（无熔炼配方）是否有意**：无法从代码判断，需要作者一句话。
5. **R1（全实体遍历）的实际开销**：没有实测 MSPT，所以只标"风险"不标"bug"。
   判据已给（nanoTime 对比 + 大实体量场景）。
6. **`03` §3.3 那条"hurt 会清燃烧计时"**：源仓库笔记这么写，但我在 1.21.1 基类里**没找到这条路径**
   （逐条列了我核对过的全部清火点）。我给出了一个更可能的解释（无敌帧吃掉燃烧伤害），
   但**标为未确认**，并给了实测方法。
7. **源仓库 `bettergold` 自身的两处问题**（顺带发现，不属本次任务范围，仅供参考）：
   - `MetalFamily.java:404-412` 的锻造模板空位图标用了 `item/empty_slot_helmet` 等**不存在的路径**
     （原版是 `item/empty_armor_slot_helmet`，见 `01` §8.2）——我没有进游戏确认它是否显示为紫黑格；
   - 自动熔炼"实现完但从未实机验证"（它自己的交接文档里写明了）。
8. **本仓库其余三套盔甲（宝石/星界/雷鸣）是否有未发现的资源问题**：
   我只做了"区域分布 + alpha + 唯一性 + 尺寸 + 图标轮廓"这几项可自动化的检查，
   **没有**核对配色是否与物品图标一致、也没有核对模型叠加是否穿模（那些只能人眼）。

---

## 五、问题计数（给接手的人一个总数）

| 档位 | 数量 | 条目 |
|---|---|---|
| **确定的 bug** | **2** | ① 翠玉装备层贴图互换；② `lightning_block` 整链不可达 |
| **风险 / 建议核实** | **8** | R1 全实体每 tick 遍历；R2 耐久注释与实现相反；R3 `ordinal()` 下标脆弱；R4 无原版熔炼配方；R5 `armorTag` 的 `BODY` 兜底；R6 CEWS 反射读私有字段静默失败；R7 `visualOnly` 的副作用未记录；R8 嬗乱扣耐久的两处小语义 |
| **已正确（核对过）** | **14** | 见第三节表 |

**没有为了凑数编造问题**：第三节那 14 项是我逐条读过并给出依据的"确认无误"；
第四节列了我**真正没能确认**的东西。如果你要在本仓库继续做盔甲/材料相关工作，
建议的处理顺序是：**先修第 1 条（一行文件对调，玩家可见）→ 再裁决第 2 条**，
其余风险按"改动是否顺手"决定要不要一起做。

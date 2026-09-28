# 发布说明（RELEASE）

> 本文件回答四件事：**发什么、依赖什么、怎么组合、验到哪一步**。
> 与 `HEAD` 同步；改动分层/注册/资源归属后**必须同步本文件**。
> 文件的字节数只用于辨认「是哪一份产物」，**不保证跨次构建一致**（工程内已有记录：jar 不是字节可复现的）。

---

## 0. 先读这一条：**不要发根 jar**

仓库里有两个**同名**的 `createoreexpansion-1.0.0.jar`：

| 路径 | 大小（本次记录） | 它是什么 |
|---|---|---|
| `build/libs/createoreexpansion-1.0.0.jar` | 2,048,111 B | **开发用整包**，mod id = `coe_integration`，**内嵌三个模块 jar** ⇒ **不发** |
| `coe/build/libs/createoreexpansion-1.0.0.jar` | 1,396,592 B | **第一层模组本体**，mod id = `createoreexpansion` ⇒ **要发** |

两者只差所在目录与约 65 万字节。**打包上传前请用 `jar tf` 确认里面有没有 `META-INF/jarjar/…cews-1.0.0.jar`** —— 有它是整包（不发），只有 `…coe_core…` 与 `skiller-1.0.0.jar` 才是第一层本体（要发）。

**为什么不能发整包**：整包内嵌了模块 jar，玩家若再单独装模块 jar，FML 会同时看到顶层与内嵌的两份同 id 文件 —— 实测**不会报 `duplicate_mod`**（`JarSelector` 会发 WARN 并改用顶层那份），但模组列表会多出一个 `coe_integration`、数据包会多一份，**行为不可预期**。发布策略是**只发单个模组，不发整包**。

---

## 1. 发什么

| 产物 | mod id | 层 | 内嵌（`META-INF/jarjar/`） | 发吗 |
|---|---|---|---|---|
| `coe/build/libs/createoreexpansion-1.0.0.jar` | `createoreexpansion` | 第一层（矿物拓展） | `coe_core-1.0.0.jar`、`skiller-1.0.0.jar` | **发** |
| `cews/build/libs/cews-1.0.0.jar` | `cews` | 第二层（能量波阵学） | `coe_core-1.0.0.jar` | **发** |
| `transmutation/build/libs/transmutation-1.0.0.jar` | `transmutation` | 第三层（**当前是空壳**） | `coe_core-1.0.0.jar` | **发**（见 §4） |
| `build/libs/createoreexpansion-1.0.0.jar` | `coe_integration` | 集成层（dev） | 三个模块 + core + skiller | **不发** |
| `core/build/libs/coe_core-1.0.0.jar` | 无 `neoforge.mods.toml`（**不是模组**） | 共享库 | —— | **不单独发** |

关于共享库：三个模块 jar **各自**内嵌了一份**同名**的 `com.hjmmd_8.createoreexpansion.coe_core-1.0.0.jar`。同时装三个 jar 时，FML 的 JarJar 选择器按 `(identifier, version, path, isObfuscated)` 四元组把它们**塌成一份**（已实测：Mixin 容器清单里只有一份 `…coe.core`，容器总数 31 而非 33）。所以**不需要**单独发 core，也**不要**把 core 当模组塞进 `mods/`。

---

## 2. 依赖矩阵

| 模组 | 必需 | 可选（缺失时自身降级，不影响加载） |
|---|---|---|
| `createoreexpansion`（第一层） | Minecraft 1.21.1、NeoForge 21.1.228+、Create 6.0.10、**Create: Crafts & Additions（createaddition）** | JEI、Jade、Curios |
| `cews`（第二层） | **`createoreexpansion`（required）** + Create | JEI、Jade |
| `transmutation`（第三层） | **`createoreexpansion`（required）** | JEI |

- **可选依赖真的可以不在场**：实测在发布形态下同时摘掉 JEI / Curios / Jade ⇒ 服务端照常起到 `Done`，本方代码对这三个模组的类名 **0 命中**，Curios 缺失时主动打一行降级日志（凝能佩按纯物品注册）。
- **第三层对第一层是硬前置**：单独装 `transmutation.jar` 或 `cews.jar` 会在**加载期**被 FML 拒载（`Missing or unsupported mandatory dependencies` + `Mod ID: 'createoreexpansion', Requested by: '<请求方>'`）。**这是设计，不是 bug** —— 不要为此开 issue。

---

## 3. 怎么组合

| 组合 | 状态 |
|---|---|
| **只装 `coe.jar`** | **已真验**（发布形态服务端：`PASS 20 / FAIL 0`，标签与配方解析缺陷均为 0） |
| 只装 `cews.jar` | 被 FML 拒载（设计如此） |
| 只装 `transmutation.jar` | 被 FML 拒载（设计如此） |
| `coe` + `cews` | 静态与加载期证据齐；运行期整组待跑（见 §5） |
| 三层全装 | 已真验（服务端 7 项全过） |
| **整包与模块 jar 混装** | **不要**（见 §0） |

---

## 4. 已知限制

1. **第三层目前是空壳**：只有 `neoforge.mods.toml`（声明 `transmutation`、对 `createoreexpansion` required）、一个 `@Mod` 入口（空构造器）与一次共享注册表挂载。**它现在不提供任何内容**，存在意义是「先把位置占住」，嬗化系列的新加工配方以后往里加。
2. **`createoreexpansion-common.toml` 由第一层注册**：只装 `cews.jar` 时**不会生成**该配置文件（但那种组合本来也会被拒载）。
3. **`cews.jar` 里有 3 个空目录条目**（`data/createoreexpansion/recipe/` 那三层）：清理方案要么影响所有层的 stale 清理逻辑，要么给 jar 加 `exclude` —— 而后者在 cews 将来真有自己的配方时会**静默丢文件**。**风险大于收益，故保留不动**。
4. ~~资产归属欠账~~ **已修复（2026-09-28）**：三台应力充能器的**手写模型与贴图**（71 个 = 模型 24 + 贴图 47）曾在模块搬家时滞留在第二层，后果是**只装 `coe.jar` 时能放下机器、但模型与贴图缺失**（玩家可见）。现已全部 `git mv` 进第一层，**字节不变**（贴图逐个比过 sha256）。
   **新增关卡** `tools/check-asset-attribution.ps1`（5 条断言）专守这一类：它检查「每个模块的资产引用必须能在自己模块的资源根里解析到」。**它已确认本条已修** —— `coe` 侧的非法方向未解析引用 = **0**。
   ⚠ 该关卡的 A1 判据正在改为**依赖方向感知**（读各模块 `neoforge.mods.toml` 的 `required` 依赖）：现状它会把**合法的** `cews → coe` 引用也报成红。那一类引用是正当的 —— `cews` 已声明 `createoreexpansion` 为 required，玩家装 `cews.jar` 时必然也装了 `coe.jar`，那些贴图一定在场。
5. **创造页归属仍有待裁定项**（不影响可用性）：
   - **已解决**：三种机壳按裁定归第一层 ⇒ 它们已从「能量波阵学」页移到**矿物页**。`cews` 侧那份 `stellarstone_blocks` 标签半边因此变空并被删除，而 `coe` 侧半边同时提供 `stellarstone_block` 与 `stellarstone_casing` ⇒ **并集语义完整保留**。
   - **待裁定**：三台应力充能器与两个能量机构（`energy_mechanism` / `incomplete_energy_mechanism`）当前仍出现在「能量波阵学」页，而它们的内容归属已在第一层。

---

## 5. 验证状态（**分清「真验过」与「只有静态证据」**）

### 已真验（跑过真发布形态 / 真进程）
- **发布形态服务端 7 项**：三个模块 jar 作为三个独立 mod 文件加载；三份同名嵌套 core **塌成一份**；mixin 配置登记且真注入（4 条）；数据包无 `Couldn't load tag`；`createoreexpansion-common.toml` **真生成**（4,904 B，容器名未变）；三个 `@Mod` 入口都被识别；四类硬错各 0。
- **只装 `coe.jar`（A1）**：`PASS 20 / FAIL 0`。
- **只装 `cews.jar`（A2）**：形态反转达成（构造期 `NoClassDefFoundError` → 加载期缺依赖拒载）。
- **可选依赖缺失**：摘掉 JEI/Curios/Jade 后照常加载。
- **六个静态工具** + 自足性断言（88 条，另有在途扩充至 92 条）全绿。
- **资产归属关卡**（`tools/check-asset-attribution.ps1`）：已跑，**确认 `coe` 侧非法方向引用 = 0**（原欠账已闭合）。

### 只有静态证据（**尚未真跑**）
- 验收矩阵的 C / D / E / F / G 组：两两组合、存档读入降级、客户端单装、网络载荷对端缺失、配置逐键比对、摘 `createaddiction` 拒载。
- 资产归属断言的**完整滞留清单**。

### 必须由人验证（静态关卡在原理上覆盖不到）
- 客户端渲染与交互：技能预览、波口指示灯、护目镜读数、Jade 提示。
- **按 `E` 打开创造物品栏**（曾在此处崩过：创造页重复添加）。
- `Ctrl + 扳手右键` 旋转（客户端拦截 + 自定义载荷，载荷静默失效时表现为「没反应」而不是报错）。

---

## 6. 老存档兼容

**本模组从未改动以下任何一项**，因此拆分成多个模组**不需要 DataFixer**，老存档可直接读入：

- 注册命名空间（恒 `createoreexpansion`）
- 全部注册 id（方块 / 物品 / 方块实体 / 配方类型 / 流体 / 效果……）
- 语言键
- 数据包路径
- 数据组件与 NBT 格式（技能数据组件 `createoreexpansion:skills` 原样保留）

配置文件容器名也保持 `createoreexpansion`（**不是** `coe_core` 之类），所以老玩家的配置值不会静默丢失。

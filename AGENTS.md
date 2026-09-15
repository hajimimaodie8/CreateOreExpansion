# AGENTS.md — 给 AI 协作代理的工程备忘

> 本文件是「记忆索引」，只放跨会话必须知道的事实与挂起事项；长文档都在 `markdown_output/`。
> 最后更新：2026-09-14（能量波系统收尾 + 工程坑补记 + 注液/储存档位/波型日志三修）。

## 工程速览

- 模组：`createoreexpansion`（Create 6.0.10 附属，NeoForge / Minecraft 1.21.1）
- 根目录：`E:\mc\mcmod\createoreexpansion`，包根 `com.hjmmd_8.createoreexpansion`
- 当前分支：`leaf-dev`
- 构建与验证流程（本工程一贯用法）：
  1. `.\gradlew.bat compileJava`
  2. 改动语言/资源时：`.\gradlew.bat runData`（同时兼作 Bootstrap / mixin 冒烟测试）
  3. `.\gradlew.bat processResources jar --rerun-tasks`
  4. 校验 `build\libs\createoreexpansion-1.0.0.jar` 的时间与大小，再用 `dsh_im_return_file` 交付给用户
- 崩溃排查：`run/crash-reports/*.txt`、`run/logs/latest.log`
- 可选依赖（Jade / JEI / CC&A / Create Optical / Vintage Improvements / Aeronautics / Sable）一律通过 `compat/*` 层的 `ModList.isLoaded` + 反射隔离，**绝不在 `content` 包直接 import 可选模组类**。

## 🧯 工程坑与自检（2026-09-14 补记，动手前后都用得上）

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

**3）PowerShell 与全库盘点**
- 全库文字盘点用 `Select-String`：**本仓 `grep` 会漏文件**（实测只扫出 71 行，实际更多）。
- PS 5.1 读**无 BOM 的 UTF-8 `.ps1`** 会按 ANSI 解码：脚本里的中文注释可能吞掉引号 → ParserError。**一次性脚本一律写纯 ASCII**（中文用 `[regex]::Unescape('\uXXXX')`）。
- 临时文件写 `%TEMP%` 或 `build/`（已忽略），别落进仓库——`git add -A` 会把它带进去。

**4）构造期陷阱（2026-09-14 真实崩溃）**
父类构造器会调用**可覆写**方法。`Entity` 的构造器就会调 `setPos(0,0,0)`（另有 `defineSynchedData` / `getBoundingBox` / `getFireImmuneTicks` / `getMaxAirSupply` / `getTeam` / `level()`），而覆写体在**字段初始化器之前**执行 → 碰任何对象字段都是 NPE。波实体曾在 `setPos` 里调 `wavePath.markLiveBreak()`，导致**开炮即服务端崩溃**（`run/crash-reports/crash-2026-09-14_09.47.56-server.txt`）。
**规则：构造期可被调用的覆写体只能使用参数、静态成员与原始类型字段。** 已审计全仓（`Entity` 侧只有 `AbstractChargerWaveEntity` 覆写这两个方法；`BlockEntity` 构造器调的 `getType`/`isValidBlockState`/`validateBlockState`/`getNameForReporting` 在本仓无覆写），现在都合规。

**5）静态关卡覆盖不到什么**
`compileJava` / `runData` / javadoc **都不构造实体、不跑 tick**。改动涉及实体生命周期或玩家交互时，这三关全绿也不代表不崩——必须明确告知用户"这一步只能进游戏实测"。

**5.5）数据包标签目录是<b>单数</b>：`tags/item/`、`tags/block/`，写错就静默失效**
写成 `tags/items/`（复数）= 一个"名叫 items 的注册表标签目录"，游戏不认识，**整个文件被忽略且不报错**。
实例：`transmutation_protected.json` 曾在 `tags/items/` 里 → `stack.is(TRANSMUTATION_PROTECTED)` 恒为假
（龙蛋/下界之星/信标其实从没被保护）。2026-09-15 已挪回单数目录。
自检：`runData` 只校验 `src/generated/**`，手写数据文件放错目录**没有任何关卡会报**——只能目视核对路径。
另：手写数据文件与 datagen 产出**同名就会让 `processResources` 报 duplicate 而构建失败**
（本仓造过一次：手写的系列标签 vs 新加的 `.tag(...)` 注册）——两者只能留一个。

**6）三条"别再查一遍"的既有结论（2026-09-14 实测/反编译取证，长文见 `markdown_output/`）**
- **"给桶注液"不是配方**：Create 的 19 条内置 `create:filling` 配方里**没有一条以空桶为原料**；真机的流体喷口走 `GenericItemFilling` 特判（借物品自身的 `Capabilities.FluidHandler.ITEM`）。所以"波拿不到注液配方"不是类型门/流体门槛的缺陷，而是**口径缺失**——已补货载旁路 `content/charger/craft/WaveItemFluidFilling`（用量与成品一律委托 Create，只认载荷流体，不耗链数）。详见 `变器配方判定与执行逻辑.md` §0.2。
- **储存模式释放档位**：口径只有一处 `AbstractCreateChargerBlockEntity#resolveReleaseLevel(int)`——有应力取**当前档位**（与普通模式一致），停转才回落"最后一次有效档位"（否则停转后囤的层数一发都放不出）。蓝宝石用 `storedLevel` 做回落值；星辉石等级是持久手动设置，直接用 `getManualLevel()`。
- **波系统日志唯一出口**：`content/charger/wave/WaveDiag`（`[变体波轨迹]` = 事件流，默认开；`[变体波加工]` = 逐条淘汰，默认关）。实体侧 `craftTrace` / `craftDebug` 只是它的委托包装——**新代码要写波相关日志一律走 WaveDiag**，别再自建前缀。
- **变器"哪些面算波口"是机器本地概念**：`StellarWaveTransmuterBlock#isOpenable(BlockState, Direction)` 按"该世界方向能否映射到本机模型的一个侧面"判定（口径唯一处 `propertyForWorld`）。**别再写成"世界水平方向"**——本机六向放置，躺倒时自己的 4 个侧面里有 2 个朝上下，旧写法会让那 2 个口"显示开着却点不掉"。
- **波波碰撞与波级/波型无关**（用户明确、长期口径）：任意两波相交即 `triggerBoom(爆炸等级 = min)` + 相互湮灭，范围伤害/范围充能加工、不破坏地形；几何上不可能"高速对穿而不触发"（相对步长 ≤1.2 格/tick < 判定窗口 2×0.6）。改碰撞效果前先看 `handleWaveCollision`，那里的日志会打印等级对与实际粒子数。
- **变器波口指示灯几何**（用户 2026-09-14 定稿，改动在 16 个 `stellarstone_wave_transmuter_<i>.json` 里逐变体编码）：灯片 **2×2 px**，**开口时离方块边缘 1 px、关闭时 2 px**（沿顶面往机器内侧挪，非高度方向）；尺寸/y/UV/贴图选择都不随状态变。变体 index 的状态位是 `NORTH=8 / SOUTH=4 / WEST=2 / EAST=1`（`AllBlocks` datagen）。差波器家族走另一条路——独立 overlay 模型、只在开口时绘制、恒 1 px 内缩。
- **波情"五要素"的唯一取值点**（用户 2026-09-14 定稿）：波速 → 波级 → 波载荷 → 波型 → **剩余寿命**。  显示只有**两处**——Jade 波实体提示（`compat/jade/WaveJadePlugin`，要素块之后才是可加工清单/电荷）与
  波情查询仪的动作栏读数（`content/wave/gauge/WaveReadout#line()` 的词条 `wave_gauge.readout`）。
  寿命口径 = `AbstractChargerWaveEntity#getRemainingLifetime()`（200 tick 上限 − 已存活）÷ 20，
  **两处共用这一处取值**；它是时间寿命，飞满 64 格的距离上限不折算进来。
  查询仪是**动态**的（扫描动画 32 tick 内 `inventoryTick` 每 tick 刷新，见 §28.3），
  Jade 每帧重建。要加/改要素就改这两处 `...show/hide`，别再新开第三条显示路径。
- **系列特性登记口径**（用户 2026-09-15 定稿）：common/SeriesTraits 是唯一入口——方块 .transform(SeriesTraits.addStellarstoneTraits()/addThunderiteTraits())、物品链上 .tag(AllModItemTags.STELLARSTONE_ITEMS/THUNDERITE_ITEMS)（Java 没有扩展方法，物品侧写不出 .addXxx()）；判定 = 物品标签 ∪ 系列方块标签 ∪ 注册名约定（限定本模组命名空间）。**四个系列标签由 datagen 生成，手写文件禁止同名**（同名会让 processResources 报 duplicate 直接失败）。两个系列（含方块物品）免疫嬗乱销毁，该判定在 TransmutationDisorderEffect#canTransmutationDestroy 里调 SeriesTraits——方块物品进不了物品标签，故不能用标签覆盖。

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

## ⏸ 挂起事项（重要，动手前必读）

**CEWS 模块（能量波阵学，进行中）**：能量波系统要从矿物拓展里独立成一个板块
**CEWS = Create: Energy Wave Studies（机械动力：能量波阵学）**，最终形态是**独立的内置 jar**（JarJar 嵌套模块）。

- **阶段 0 已完成**：创造标签页 `createoreexpansion:energy_wave_study`（顺序 **矿物拓展 → 能量波阵学 → Create 调色板**）；
  **"什么属于 CEWS"的唯一清单** = `common/EnergyWaveStudyTab#CONTENTS`（17 项：机器 + 机壳×2 + 查询仪；**强化避雷针按用户裁定留在矿物页**；图标 = 翡翠应力充能器）；内容同步走
  `BuildCreativeModeTabContentsEvent`（往新页放 + 从基础页剔除，注册代码未动）——**加/减机器只改这份清单**。
- **阶段 1/2 未做**（用户明确"工程量大，现在先只做标签页"）：包级隔离 → 独立 Gradle 子模块 + `jarJar`。
  全部耦合点（12 条）、模块边界判定、3 个待定项、风险清单与验收标准见
  `markdown_output/CEWS 能量波阵学模块（拆分方案与思索）.md`——**动手前先读那份**。
- **拆包红线**：注册命名空间必须保持 `createoreexpansion`（mod id 可以是 `cews`），
  否则所有方块/物品/配方/标签 id 全变、老存档报废；配置键、语言键、数据包路径同理不许改。
- **英文名建议用复数**：`Create: Energy Wave Studies`（学科名英文习惯用 Studies；缩写 CEWS 不变）。
  id 一律用 `energy_wave_study`，与英文名解耦。

**第二个模块：矿物拓展（长期计划，仅登记，不要动手）**：将来把现有本体（矿物/宝石/水晶芽/工具/技能/
嬗变液/雷鸣合金）也收成独立模块（暂称 COE）。**CEWS 单向依赖 COE**（材料），反向禁止。
**不能与技能换核并行**（两者都要动注册与 `foundation/item/skill`）。

**技能内核移植**：用户的朋友 Leaf 已把本模组「工具技能系统」的核心抽成独立实现（仓库 <https://github.com/lizhanyu-leaf/Skiller>，本地副本 `E:\mc\mcmod\_ref\Skiller`）。用户决定：

- **在 Leaf 宣布 core 重构完毕（或有其他情况）之前，不要改动本工程任何代码**，尤其不要动 `foundation/item/skill/` 与技能相关内容。
- 届时的迁移顺序与全部前置条件见 `markdown_output/技能内核对比与迁移分析.md`（§4 抽象映射表、§6 十二条实证缺口、§7 迁移风险、§8 分阶段路线、§8.5 给 Leaf 的清单）。

动手前必须确认的前提（否则会白做）：

1. Leaf 侧的三处硬阻塞已修：`SkillData` 序列化往返（`toString` 丢 `factoryId` / `fromString` 读从未写入的 `"Factory"` 键）、`releaseSkills` 无调用点、`StrategyRenderers.register` 无调用点。
2. 注册技能时**条目 id 必须保持 `createoreexpansion:xxx`**（Skiller 的注册表键是 `skiller:skill`，但条目 id 的命名空间由注册方决定）。翻译键方案两仓同构（`skill.{namespace}.{path}`），守住 id 命名空间则现有中英 13 条 lang 键一条都不用改。
3. 旧存档兼容策略需先拍板：新版反序列化强制要求 NBT 内有 `Factory` 键，老格式字符串会被静默丢弃（返回 null → `SkillBundle.getSkills` 过滤掉，不报错）。

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

- **Hindsight 记忆已可用**（2026-09-10 起配置）：`C:\Users\Lenovo\.hindsight\coding-agent.json`，`serverMode: cloud`，本仓库 bank 为 `coding-agent::createoreexpansion`。**但已按用户要求关闭全部自动导入**，写入规则见下面「记忆写入规则」一节。
- 本机无法访问 `maven.neoforged.net`（连接被重置），因此**无法在本地编译 Skiller 参考工程**（它需要 `neoform-runtime:2.0.24`，本机 Gradle 缓存只有 2.0.18）。本工程的依赖均已缓存，可正常构建。

## 本工程的技能系统（现状速记）

- 框架层 `foundation/item/skill/`（24 文件）+ 实现层 `content/skill/`（37 文件）+ 客户端 `client/tool/`（8 文件）+ 能量 `content/equipment/tool/energy/`。
- 9 个技能：`fell` / `shatter` / `channel` / `grade` / `skin` / `plunder` / `hoe` / `bow_curse` / `bow_disarm`，统一注册在 `common/AllSkills.java`（`SkillBuilder` DSL + 三个静态 Map），每个技能都带 `.config()` + `.configsByLevel()` 分级数值表（数值表在 `content/skill/config/*Configs.java`）。
- 框架层对模组侧存在 **11 处反向依赖**（静态注册表、`ToolEnergy`/`SkillEnergyCost`/`ToolEnchantments`、`client.tool.*` 渲染器、`MOD_ID` 拼接翻译键），是将来换核的主要障碍。逐条清单见分析文档 §3。
- 技能键位 (`common/AllKeys.java`) 是纯客户端对象，而触发入口 `mixin/ServerPlayerGameModeMixin.java` 注入的是服务端类；本工程**没有任何按键同步包** → 专用服务器上技能键不会触发。这是一个独立于换核的既有缺陷，用户尚未决定何时修。

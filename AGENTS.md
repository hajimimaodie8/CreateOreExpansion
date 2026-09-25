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
- **变器"应力闸门"是"要不要给波赋属性"的唯一判据**（用户 2026-09-24 规格 + 当面确认）：**未接入应力时变器对波的波形/属性零改变**——加工波变态不转换（两口皆开时波原样飞过）、攻击态的攻击场连实体查询都不做。判据唯一处 = `StellarWaveTransmuterBlockEntity#isWavePowered()` = `hasNetwork() && getSpeed() != 0 && isSpeedRequirementFulfilled()`。生效点两处：`StellarWaveTransmuterPass#tryConvert`（新 `Result.PASSED` → `WaveOutcome.TRANSPARENT`）与 `TransmuterMode.ATTACK#applyField` 首行。**闸门只掐"赋属性"，不碰"让不让波过"**：波口开关 / 入口撞墙 / 对面遣返是机壳几何，仍照旧（闸门刻意排在两个口位判定之后）。
- **转速门槛由模式自报**：`TransmuterMode#minimumRpm()` —— 加工态 **0**（"只要在转"，由 `getSpeed() != 0` 承担；**别写 1**，Create 转速是浮点，慢速网络 0.5 RPM 会被误判成转速不足）、攻击态 **128 RPM**（`ATTACK_MINIMUM_RPM`，用户 2026-09-24："限制这个攻击，转速 128 RPM 以上才能开启"）。按**绝对值**判定（反转的轴算接通）。它只限制**攻击效果**，模式切换 / 四口强制全开 / 口位锁定一条都不受影响。`ATTACK_MINIMUM_RPM` 与 BE 里那个同为 128 的 `FIELD_SPEED_THRESHOLD`（能量场半径分档）**概念不同、禁止互改**。
- **攻击场三档 = 转速线性分档**（用户 2026-09-25 规格，分界已当面确认为**区间三等分**）：`TransmuterMode#attackTier(float speed)` 是唯一实现（`t = clamp((|speed| − 128) / (256 − 128), 0, 1)`，档 = `clamp(floor(t × 3), 0, 2)` → **128–170.67 档一 / 170.67–213.33 档二 / ≥213.33（含 256 以上）档三**）；`attackFieldRadius(speed)` 是它的别名（档位序号就是半径：0/1/2）。三个常量 `ATTACK_MINIMUM_RPM=128` / `ATTACK_MAXIMUM_RPM=256` / `ATTACK_TIER_COUNT=3` 在 `TransmuterMode` 里各只有一份，**显示层一个新数字都不写**（区间两端由 `minimumRpm()` + `attackTierCeilingRpm()` 自报）。档一 = **场盒就是机器本体**（波必须真的穿过这台机器），档二 = 半径 1 的立方体，档三 = 半径 2。**攻击场半径因此不再取能量场 `scanRadius`**；`applyField` 签名已改为 `(Level, BlockPos, float speed)`，BE 只递 `getSpeed()`。
- **场节拍固定 2 tick、不随档位变化**：最小档降到"机器本体"后 `MIN_FIELD_RADIUS` 由 1 改为 **0**（公式于是只得 1 tick），但正确性早已由波自己的 `WavePath` 折线（跨度 `MAX_OBSERVATION_GAP_TICKS` = 4 tick）保证——**观测间隔 ≤ 折线跨度就不漏判**，压到 1 tick 只是把每 tick 一次实体查询换回来，故用 `FIELD_INTERVAL_FLOOR_TICKS = 2` 托底。别再引用"半径 1 是最坏情形"那句旧口径。
- **两态读数不混，且由模式自报**（用户 2026-09-25 规格："攻击态按住 Shift 不该显示加工态的内容"）：`TransmuterMode#appendReadout(tooltip, readout)` —— 加工态委托 `TransmuterGoggles#append`（扫描半径/加热/载荷/配方类型/最近一波那套），攻击态走 `TransmuterGoggles#appendAttackReadout` 的三行（① 攻击转速 + 需求区间 ② 第 X 档 + 场盒半径 ③ 场作用说明；档位/半径直读 `attackTier` 那一个映射，档位序号只在这里 +1）。调用方（BE）零 `if (mode == ATTACK)`。新增键：`..._attack_rpm` / `..._attack_tier` / `..._attack_field_hint`（加在 `data/lang/{Chinese,English}LangProvider`，`runData` 生成，**别只手改 generated**）。
- **行序（用户 2026-09-25 追加规格："星辉波变器"这个机器名称放最前面）**：名称行 → 模式行（`TransmuterGoggles#appendModeLine`，全类唯一输出点，已从 `append` 剥离且 `Readout` 去掉 mode 字段 → 不会两行）→ 状态/读数行（转速 0 → 既有 `goggles.stellar_wave_transmuter_idle`「未接入应力」；在转未达门槛 → 我方零文案，由 Create 打"转速不足"；否则 `mode.appendReadout`）→ **末尾**才是 `super.addToGoggleTooltip`（Create 的"动能统计/应力影响"，原先在最前，为用户这条规格整体下移）。
  `GoggleOverlayRenderer` 用**同一个 List** 先后调 `addToGoggleTooltip` 与 `addToTooltip`（中间插 `CommonComponents.EMPTY`、结尾 `remove(size-1)`），所以 BE 在 `addToTooltip` 里用 `tooltip.isEmpty()` 去重是正确判据（那里同样先补名称行、再补模式行）；**无护目镜时前者根本不调**（条件是 `isWearingGoggles && instanceof IHaveGoggleInformation`），此时头两行由 `addToTooltip` 补出，且它的返回值必须 `|| added` 兜住，否则 Create 侧返回 false 会让整块提示被丢弃。**`addToGoggleTooltip` 里三处早退已改成 if/else 链**——不能保留 `return true`，否则末尾的 `super` 会被跳过、Create 的动能行整块消失。
- **"转速不足"不要自己写文案**：覆写 BE 的 `isSpeedRequirementFulfilled()`（门槛取模式自报值）即可让 Create 的 `KineticBlockEntity#addToTooltip` 自己打出搅拌器同款两行——金色 `create.tooltip.speedRequirement`（"需求转速："）+ `create.gui.contraptions.not_fast_enough`（"显然 <机器名> _没有_达到_足够_的_转速_。"，`%1$s` 是机器名，中英双语现成），**零新增翻译键**。注意 `IRotate.SpeedLevel.FAST` 取的是配置 `kinetics.fastSpeed`（**默认 100**，不是 128），所以门槛**不绑** SpeedLevel。
- **变器波口指示灯几何**（用户 2026-09-14 定稿，改动在 16 个 `stellarstone_wave_transmuter_<i>.json` 里逐变体编码）：灯片 **2×2 px**，**开口时离方块边缘 1 px、关闭时 2 px**（沿顶面往机器内侧挪，非高度方向）；尺寸/y/UV/贴图选择都不随状态变。变体 index 的状态位是 `NORTH=8 / SOUTH=4 / WEST=2 / EAST=1`（`AllBlocks` datagen）。差波器家族走另一条路——独立 overlay 模型、只在开口时绘制、恒 1 px 内缩。
- **波情"五要素"的唯一取值点**（用户 2026-09-14 定稿）：波速 → 波级 → 波载荷 → 波型 → **剩余寿命**。  显示只有**两处**——Jade 波实体提示（`compat/jade/WaveJadePlugin`，要素块之后才是可加工清单/电荷）与
  波情查询仪的动作栏读数（`content/wave/gauge/WaveReadout#line()` 的词条 `wave_gauge.readout`）。
  寿命口径 = `AbstractChargerWaveEntity#getRemainingLifetime()`（200 tick 上限 − 已存活）÷ 20，
  **两处共用这一处取值**；它是时间寿命，飞满 64 格的距离上限不折算进来。
  查询仪是**动态**的（扫描动画 32 tick 内 `inventoryTick` 每 tick 刷新，见 §28.3），
  Jade 每帧重建。要加/改要素就改这两处 `...show/hide`，别再新开第三条显示路径。
- **系列特性登记口径**（用户 2026-09-15 定稿）：common/SeriesTraits 是唯一入口——方块 .transform(SeriesTraits.addStellarstoneTraits()/addThunderiteTraits())、物品链上 .tag(AllModItemTags.STELLARSTONE_ITEMS/THUNDERITE_ITEMS)（Java 没有扩展方法，物品侧写不出 .addXxx()）；判定 = 物品标签 ∪ 系列方块标签 ∪ 注册名约定（限定本模组命名空间）。**四个系列标签由 datagen 生成，手写文件禁止同名**（同名会让 processResources 报 duplicate 直接失败）。两个系列（含方块物品）免疫嬗乱销毁，该判定在 TransmutationDisorderEffect#canTransmutationDestroy 里调 SeriesTraits——方块物品进不了物品标签，故不能用标签覆盖。
- **机器交互四条统一规则**（用户 2026-09-15 定稿）：① 空手右键某个面 = 开/关该面开口；② 空手右键指示灯 = 只切那盏灯对应的开口；③ 扳手右键 = 有特殊模式的机器只切模式、没模式的机器照旧切开口；④ 旋转必须 **Ctrl + 扳手右键**。
  实现三件套：契约 `content/machine/CewsMachine`（`hasModeSwitch()`；`onWrenched` 默认"吞掉但不旋转"，防 Create 默认旋转在没按 Ctrl 时生效；`rotateAsCreate()` 直接复用 `IWrenchable` 默认旋转、不复制逻辑；`onEmptyHandPortToggle`）、载荷 `MachineRotatePayload`（服务端校验：本模组机器 + 无模式 + 手持扳手 + 距离）、客户端 `client/MachineRotateClient`（Ctrl 判定后取消本地交互并发包）。
  **为什么 Ctrl 必须在客户端判定**：使用物品包不带修饰键、Ctrl 也不同步（只有潜行会同步），服务器根本读不到；客户端拦截 + 自定义包才能让"没按 Ctrl 就不旋转"成为服务端权威行为。
  副作用修复：`StellarWaveTransmuterBlock` 以前**没实现 `IWrenchable`**（`onWrenched` 没有 `@Override`）→ Create 的扳手从来没分发到它，变器切模式其实一直不生效；现已随契约修好。

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

**技能内核移植（2026-09-19 已开工，进行中）**：用 Leaf 的独立内核 **Skiller**（<https://github.com/lizhanyu-leaf/Skiller>，本地副本 `E:\mc\mcmod\_ref\Skiller`，MIT）替换本模组自研的技能框架。
**执行方案与全部决策见 `markdown_output/技能内核换核执行方案.md`**（旧分析文档 `技能内核对比与迁移分析.md` 只作参考）；现状地图见 `build/patch/coe_skill_map.md`，Skiller API 契约见 `build/patch/skiller_api_foundation.md`（两者都在被 git 忽略的 `build/` 下，重生成即可）。

已定事实（别再重新论证）：

1. **引入方式 = JarJar 嵌套**：jar 在 `libs-maven/com/leaf/skiller/1.0.0/`（本地 maven 布局、**随仓库提交**），`build.gradle` 里 `jarJar(implementation("com.leaf:skiller:1.0.0"))` + 仓库 `maven { url = uri("libs-maven") }`。dev 运行时它作为独立 mod（mod id `skiller`）加载，mixin 生效（已用 `runData` 验证）。
2. **上游 HEAD `abe5688` 的硬阻塞一条都没修**（`SkillData` 往返丢 `factoryId`、`factoryId` 恒 null、两个 `releaseSkills` 重载读错表必 CCE、`AllKeys` 缺 `Dist.CLIENT` 专用服务器崩、`StrategyRenderers` 无注册且 `schedule` 对 null 无防护、`ServerSkillCache` 空组件裸抛 NPE、jar 缺 `Automatic-Module-Name`）。**我方在 `_ref/Skiller` 的分支 `coe-embed` 上全修好了**（补丁 `build/patch/skiller-coe-embed.patch`，**未 push**）→ 内置的是「我方 fork 版」，升级 Skiller 要重放补丁。
3. **id 与翻译键零改动**：技能条目 id 一律 `createoreexpansion:xxx`，三个 `SkillType` 沿用 `excavation_skill`/`hit_skill`/`use_skill` → 12 条中英 lang 键一条不改。
4. **老存档天然兼容**：旧数据组件 `createoreexpansion:skills` 与 NBT 格式（`Level`/`Config`/`OutlineColor`）**原样保留**，换的只是「谁解释这些数据」→ 不需要 DataFixer，也不会静默丢技能。
5. **增量并行迁移**：技能注册进 `SkillerBuiltInRegistries.SKILLS` 就是新内核接管；没注册的由旧框架照旧处理（`CoeSkillProvider` 会跳过未注册的 id）→ 不会双重生效。
6. **不要 Skiller 的「按 R 启用技能」总开关**（保持本模组随时可用的既有玩法）：客户端进世界自动 `ClientSkillCache.enable(...)`、换主手物品调 `ClientSkillCache.refresh(player)`；服务端释放走 `CoeSkillRelease` 直接读 `PlayerPressedKeys`，**不读** `ServerSkillCache`/`SkillReleaser`。
7. **键位已定**：用本模组既有的 Shift/R/G —— 内核加了 `ClientSkillCache.setKeySource(...)` 注入点（消费方决定键位），`CoeSkillClient` 注入自己的键位并 `setToggleKeysEnabled(false)` 关掉内核的开关键闸（默认 R 会撞槽位 2）。
8. **迁移进度（2026-09-19 晚）**：**9/9 全部迁移完成** —— `shatter`/`channel`/`grade`（`AreaAoeItemSkill` + `CoeAreaAoeStrategy`）、`fell`（`FellingItemSkill` + `CoeFellingStrategy`）、`skin`/`plunder`（`SkinItemSkill`/`PlunderItemSkill`）、`hoe`（`HoeItemSkill` + `UseItemSkillContext`）、`bow_curse`/`bow_disarm`（`BowShootItemSkill` + `BowShootSkillContext`）。runData 自检：`[Skiller] registry event: skill -> entries=9`、`skill_context_factory -> entries=4`、`skill_resource -> entries=2`。
   **旧东西一个没删**：`foundation/item/skill/**`、`content/skill/**`、`AllSkills`、`client/tool/**` 都还在——① 它们是回退路径；② **客户端预览仍由旧渲染器提供**（旧类还在，所以预览没退化）；③ 弓的「箭命中」第二段仍走旧的 `JadeTopazBowEventHandler` + 旧 `applyTo`（标记里写的 id 没变，旧注册表条目还在）。
   **剩余收尾（W5/W6）**：把 `client/tool/**` 的渲染器适配成 Skiller 的 `StrategyRenderer` 并注册进 `StrategyRenderers`（**顺带必须修上游一个 bug**：`StrategyRenderers.schedule()` 判的是 `instance.skill()`（注册条目）而不是技能本体 `instance.skill().getSkill()` → 策略渲染**永远不会被调度**）；然后才能删旧框架、跑一次专用服务端启动验证。
9. **上游基点（2026-09-19 直接向服务器核对）**：`https://github.com/lizhanyu-leaf/Skiller.git` 只有 `master` 一条分支，HEAD = `abe5688`，**无任何 issue**。完整历史 4 个提交：`04cd4a8` init → `2ab25ba` 简单实现 EntityStrategy → `4cb1d4f` 写一些 TODO → `abe5688` 修改 SkillSet 的实现。
   **教训**：本地克隆默认是**浅克隆**（`git rev-parse --is-shallow-repository` 为 true），`git log` 只能看到 `abe5688`/`4cb1d4f` 两条，别据此断言"上游只有 2 个提交"——要么 `git fetch --unshallow origin`，要么直接问服务器（`git ls-remote` / GitHub API）。
   **另一个重要事实**：这条策略渲染链路**上游从来没有被真正跑过**——`StrategyRenderers.register` 全仓零调用点、`schedule()` 判错对象（我方已修）、连它自带的 `EntityStrategy` 都只是"简单实现"（`calculate` 恒返回空集合）。我们是这条路径的第一个消费者，所以它的问题只能靠我们自己趟出来。我方补丁分支 `coe-embed` 基于 `abe5688`，**从未 push**。
10. **扣能时机口径（易写歪，务必照做）**：旧实现是「策略算出**非空**集合之后才扣能」，而新内核顺序是 `consumeResource`（全部累加）→ `canConsume` → 落账 → 才 `release`。因此每个技能的 `consumeResource` 都必须先过 `CoeSkillSupport.willDoWork(...)`（有方块/实体版与通用版），否则"砍一块孤零零的原木""挖到空气"这类空结果场景会白掉一份能量。
11. **冷却类技能要两处同判**：`consumeResource` 与 `release` 都判冷却（冷却中 `consumeResource` 直接不累加、`release` 直接返回），只有真正执行过才 `ToolSkillCooldown.start*`。只在 `release` 判 = 冷却期间每次触发都白扣能量。
12. **需要"同一次释放内传递结果"时用上下文 scratch**：新内核会给每个技能实例各建一个上下文对象（`SkillBundle.releaseSkills` 里 `contexts.put(instance, factory.create(env, instance))`），所以 `HitSkillContext`/`UseItemSkillContext` 上的 `putScratch/getScratch` 天然是"本次释放"作用域。`skin`（随机判定只滚一次）与 `hoe`（优先级只解析一次）都靠它。
13. **改 Java 文件一律只用 write/edit 工具**：本轮我用 PowerShell `Get-Content | -replace | Set-Content` 改 `SkillerIntegration.java` 的 import，PS 5.1 按 ANSI 重写导致整个文件语法崩掉（48 个编译错误），只能 `git checkout --` 恢复重做。
14. **物理结构（Aeronautics/Sable sub-level）上的技能预览**（2026-09-22 打通，坑很密）：
    - **判据必须看「类在不在」，不能看 modId**：我们真正依赖的 `dev.ryanhcode.sable.companion.math.Pose3dc` 属于 modId=**`sablecompanion`** 的 jar，而 `aeronautics` jar 只是声明依赖 `sable`。原来写 `ModList.isLoaded("sable")` → 实机为假 → **整套结构兼容代码从未激活**，前面改的内部逻辑等于全白做。现在主判据 `Class.forName(Pose3dc, false, loader)`（依次试上下文/本模组/`Level.class`/系统四种类加载器——生产环境本模组加载器未必看得见别人），兜底才是三个 modId，并把命中的判据打进日志。
    - **`player.pick` 在结构上返回的是结构局部(plot)坐标，不是世界坐标**：Sable 覆写了 `BlockGetter#clip`（射线逆变换进 plot 空间求交、从不投影回世界），站在结构上的玩家自身也在局部系。把局部点当世界点用 → 框被画到十几万格外 → 现象是"结构上完全没有框"。渲染前必须 `mulPose(结构位姿矩阵)`，且**在局部空间**做 AABB 合并/去内部边（先转世界再合并，旋转后不再轴对齐、框会碎）。
    - **位姿矩阵**：`t = toWorld(hit, Vec3.ZERO, partialTick)`，三个基向量 = `toWorld(hit, e_i, partialTick) − t`（列向量），平移 = t；用带 `partialTick` 的重载（按 `lastPose→logicalPose` 插值），否则结构高速旋转时框会落后。
    - **相机位移不能忘**：旧调度器 `SkillsStrategyRenderer` 统一做过 `pushPose → translate(-camPos) → 画 → popPose`；迁到新内核时漏掉它 → 框"又远又小"。
    - **AOE 矩形朝向要本地化**：结构场景把玩家视线与所击方块面先用 `bridge.toLocalDir` 转进局部系再交给 `DualDirection`（新增 `fromLocal`，旧签名一字未改），矩形才会随结构倾斜；玩家自身已在局部系时**不要**再转一次（会反向旋转两遍）。
    - **已知未做项**：服务端释放路径的 `ExcavationSkillContext` 仍由 4 参构造器建立（`subLevelHit` 恒 null）→ **结构上真正挖哪些方块仍按世界坐标算**，预览与实效在结构上不一致。
15. **调试"看不到效果"的问题，先确证路径有没有被激活**：这轮我在结构兼容代码从未激活的前提下，连改相机位移、发光钩子、矩形朝向、局部坐标识别四轮而毫无效果。正确顺序是：**先让现场数据可见 → 确认入口成立 → 再改内部逻辑**。用户在生产整合包里测试时日志路径与 dev 的 `run/logs` 不同，**优先用动作栏显示诊断**（`displayClientMessage(..., true)` + 节流），成本最低。

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
- **`maven.neoforged.net` 现在可达**（2026-09-19 实测 HTTP 200；早先的「连接被重置」是暂时的）。因此 **Skiller 参考工程已能在本机构建**：`cd E:\mc\mcmod\_ref\Skiller && .\gradlew.bat jar`（第一次会下载 neoform-runtime 2.0.24 等，需几分钟）。首次构建偶发 `Connection reset`，重跑一次即可。内置用的 jar 由这条命令产出。

## 本工程的技能系统（现状速记）

- 框架层 `foundation/item/skill/`（24 文件）+ 实现层 `content/skill/`（37 文件）+ 客户端 `client/tool/`（8 文件）+ 能量 `content/equipment/tool/energy/`。
- 9 个技能：`fell` / `shatter` / `channel` / `grade` / `skin` / `plunder` / `hoe` / `bow_curse` / `bow_disarm`，统一注册在 `common/AllSkills.java`（`SkillBuilder` DSL + 三个静态 Map），每个技能都带 `.config()` + `.configsByLevel()` 分级数值表（数值表在 `content/skill/config/*Configs.java`）。
- 框架层对模组侧存在 **11 处反向依赖**（静态注册表、`ToolEnergy`/`SkillEnergyCost`/`ToolEnchantments`、`client.tool.*` 渲染器、`MOD_ID` 拼接翻译键），是将来换核的主要障碍。逐条清单见分析文档 §3。
- 技能键位 (`common/AllKeys.java`) 是纯客户端对象，而触发入口 `mixin/ServerPlayerGameModeMixin.java` 注入的是服务端类；本工程**没有任何按键同步包** → 专用服务器上技能键不会触发。这是一个独立于换核的既有缺陷，用户尚未决定何时修。

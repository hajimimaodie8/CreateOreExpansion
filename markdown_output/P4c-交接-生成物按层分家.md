# P4c 交接：生成物按层分家（2026-09-26 14:32 落盘）

> 这一轮**超时失败并整体回滚**，但探明了正解与一个很贵的陷阱。下次开工**先读这份**，再照它做，可省约 30 分钟。
> 落盘时刻：HEAD `f163ce2d`、工作树干净、无后台任务。

> **状态更新（2026-09-26 本轮开工后）：§3 的方案已按 §2 的约束落地，未 commit。**
> 实现 = 新增 `src/main/java/com/hjmmd_8/createoreexpansion/data/lang/LayerLangSplitter.java`（`DataProvider`），
> 在 `data/CreateOreExpansionDatagen#includeClient()` 里排在两份 `LanguageProvider` **之后**注册。
> 实测：归属 **155/155**（COE 133 / CEWS 20 / TRANS 2）；`en_us` 402、`zh_cn` 406 → 合并后 **only-before=0 / only-after=0 / value-diff=0**，`en_ud` 未动；
> 新增 6 个文件 `<模块>/src/generated/resources/assets/<模块>/lang/{en_us,zh_cn}.json`，根 lang 三个文件对 HEAD **逐字节相同**（sha 相同）。
> 注意：按硬约束 1（根文件一字不动），那 155 条键在根与模块**各有**一份 ⇒ `p4c-verify-lang.ps1` 的 `multi-carrier keys = 155` 是**预期值**，
> 它按设计把 multi-carrier 计入失败；本次另加了 `build/patch/p4c2-verify-lang-dup.ps1` 证明 155 条重复载体的**值全部相同**（合并与加载顺序无关）。
> 本文 §5 的插入块已按「已落地」改写并进 `AGENTS.md`（39,967 B ≤ 40,000）。

## 1. 本轮做到哪 / 为什么回滚
- 读码到 13:47，真正动手约 13:52，**14:15 的硬截止到点时没察觉**（14:31 才发现），按规则 `git reset --hard` + `git clean` 回滚。
- 回滚前**已经跑通**：`runData` 两次成功；lang 拆分方案在 en_us 上**等价性成立**（合并后 402/402、键差 0、值差 0），模块侧落盘 coe 133 / cews 20 / transmutation 2。
- `AGENTS.md` 的插入块**随回滚一起丢了**，下节是重建版。

## 2. ⚠ 最贵的一条教训：**拆 lang 时根文件一个字节都不许动**
第一版做法是「把已归层的键从根文件里删掉」（直觉上更干净）——**这是陷阱**：
- `HashCache` 记的是**提供器自己的哈希**，看不到「别的提供器后写了这个文件」；
- 于是下一轮 `runData` 里语言提供器的重写**被哈希命中而跳过**，根文件永远停在「已拆走」的形态；
- 此时若删掉模块侧的生成目录，那些键**再也补不回来**（datagen 不自愈）。
- 另外**同一轮里对同一路径的第二次写不可靠**（实测 en_us 的改写落地、同一轮的 zh_cn 改写没落地，要跑第二轮才收敛）。

**正确形态**：根文件**一个字节都不动**，只往模块侧**新增**一份子集（值逐条复制 ⇒ 等价性是构造性的）。
代价：那 155 条键在根与模块各有一份（可接受；lang 是覆盖式合并，同值不冲突）。

## 3. lang 拆分的正解（已验证可行）
- 新增 `data/lang/LayerLangSplitter`（`DataProvider`），在 `CreateOreExpansionDatagen` 的 `includeClient()` 里**排在两份 `LanguageProvider` 之后**注册。
- **归属靠注册对象、不靠字符串**：`AbstractRegistrate#getAll(Registries.BLOCK/ITEM)` 取每层登记过的条目，用 `getDescriptionId()` 建 `语言键 → 模块` 表（实测 155 条命中 155 条：COE 133 = 92 物品 + 41 方块、CEWS 20、TRANS 2）。
- **必须换资源命名空间**：写到 `assets/coe|cews|transmutation/lang/<locale>.json`。
  只换工程根会让多个 mod 携带**同一路径** `assets/createoreexpansion/lang/en_us.json`，资源包同路径只有一个胜出者 ⇒ **随机遮蔽、丢一半键**。
  换命名空间后 MC 的 `LanguageManager` 仍会把**所有** `assets/*/lang/*.json` 并进同一张表 ⇒ **键一个都不用改**。
- **做不到「完整拆干净」**：en_us 的 402 条 `add(...)` 里 **249 条是裸字符串键**（`itemGroup.*`、tooltip、键位、技能名、药水效果…）**无机械归属，只能留根**（62%）。目标是「每层自己产出自己那份」，不是「每层只保留自己的键」。
- `en_ud` 不动（Registrate 自动倒写，不属于任何一层）。
- 等价性检查器可直接复用：`build/patch/p4c-verify-lang.ps1`（只读）；基线 `build/patch/p4c-head/*.json`。

## 4. 仍未做的两件事（都已想清、未落地）
- **标签**：122 个 tag = 单层 112 / 跨层 union 5 / 纯外部 5。**不能按内容拆**——`LayerRegistrate#genData` 只在主层 COE 跑 COE→CEWS→TRANS 并集，全部 108 个生成标签出自**同一个提供器**，路径改写按提供器作用域生效。
  **正解 = 按 tag 文件逐文件路由**：单层归各模块，**跨层（5）与纯外部（5）留根**（否则「只装一个模块」的数据包语义就变了）。
  **标签不能换命名空间**（tag id 是 `createoreexpansion:xxx`，变了废存档）⇒ 只能靠「每个 tag 文件只属于一层」做到分家。
- **221 条生成配方**：`CreateOreExpansionDatagen` 把 `RecipeProvider` **直挂 generator**（不过 `LayerDataProvider`）。硬搬的后果实测过：runData 会在根**重新生成**，然后 `LayerDataProvider` 的 stale 清理把模块副本删掉。要真分家得先改 `RecipeProvider` 的**挂载结构**。

## 5. 给 `AGENTS.md` 的插入块（回滚时丢失，可直接粘贴）
位置：`## 🧱 模块拆分` 里 **"- **注册器已按三层分家** …（批量改名要做「裸名解析」判定）。" 之后**。粘贴后约 39,993 B（上限 40,000，紧但安全）。

```
- **P4a/P4b：assets 与 data 已按模块分家**（手写 161 + 生成 56 逐文件 `git mv`，命名空间与路径一字未改）。机制 = **改写落盘路径**而非改 `--output`：`LayerDataProvider#run(CachedOutput)` 包一层 `CachedOutput`。`build.gradle` 两处**承重**：`--existing <module>/src/main/resources/` 与系统属性 `coe.datagen.layerAssetRoots`（缺了=不改写=静默退回没分家）。
- **P4c：lang 已按层落地**（2026-09-26 重做；见 `markdown_output/P4c-交接-生成物按层分家.md`）：新增集成层 `data/lang/LayerLangSplitter`，排两份 `LanguageProvider` **之后**注册；**归属靠注册对象的 `getDescriptionId()`、必须换资源命名空间、根文件一个字节都不许动**。实测归属 155/155（COE 133 / CEWS 20 / TRANS 2），249 条裸字符串键留根。
- **⚠ 拆 lang 时不要把根文件改小**：`HashCache` 看不到「别的提供器后写了这个文件」，下一轮提供器的重写会被哈希命中而跳过 ⇒ 根文件永远停在拆分后的形态，而模块侧生成目录一删，那些键**再也补不回来**（datagen 不自愈）。同轮对同一路径的第二次写也不可靠（en_us 落地、zh_cn 没落地）。
- **221 生成配方 + 108 生成标签仍由集成层提供，判据是「出处不是声明」**：`CreateOreExpansionDatagen` 直挂 `RecipeProvider`；标签是单一 union 提供器。硬搬会被下次 `runData` 的 stale 清理吃掉。标签要拆只能**按 tag 文件逐文件路由**（单层归模块、跨层 5 + 外部 5 留根）。
- **`runData` 第二次跑 `written: 0` 不再是健康判据**：`HashCache` 存越界路径（`../../../<mod>/…`）不能往返，被改写的 238 资产 + 56 data 每次都会重写（`written: 294`）。判据是 `git status` 与内容。
- **待查**：`runData` 偶发 `Found unused register callbacks`（现在约 6 次 2 次），日志里 sable 的 mixin 仍被装载 ⇒ `build.gradle` 对 runData 的 sable/aeronautics 排除**没有真正生效**。
```

## 6. 剩余的轮次（供下次排期）
1. **P4c 重做**（lang，约 30 分钟）：照 §3 的正解 + §2 的陷阱，跑完 4 个关卡。
2. **标签分家**（按 tag 文件路由）。
3. **221 配方分家**（要先改 `RecipeProvider` 的挂载结构）。
4. **JEI 拆三个插件**（UID 互不相同；侧栏顺序是玩家可观测口径，不许变）。
5. **各模块嵌套 core + 声明 runtimeOnly 依赖**（现在模块对 core 只有 `compileOnly`，单模块 jar 拿出去跑不起来）。
6. **集成层降级成开发专用**（用户已裁定：先留着，最后搬进 `:all-neoforge`）。
7. **真实发布 jar 进游戏验证 + 修正**（**需要用户**）：JPMS 包重叠、GAME/PLUGIN 层归属、Ctrl+扳手、配置重载、创造页 17 项顺序、JEI 侧栏、嬗乱效果、以及 `IntegrationBootstrap` 在生产里可能补挂不到 `core/AllConfig` 的风险。

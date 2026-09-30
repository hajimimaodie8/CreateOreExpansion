# AGENTS.md 溢出（2026-09-28）：机器交互四条统一规则 · 变器波口指示灯几何 · 系列特性登记口径

> 2026-09-28 为给 W3 发布形态测试台腾出 `AGENTS.md` 的 40,000 B 预算，把 `AGENTS.md`
> 「📐 其它现行口径」一节里「机器交互四条统一规则」的**实现三件套 / 为什么 Ctrl 在客户端判定 /
> `IWrenchable` 副作用更正**三段原样搬到本文件（一字未删）。
> `AGENTS.md` 只留：四条规则本身 + 一句「Ctrl 的判定必须在客户端」+ 指向本文件的指针。
>
> 四条规则本体（用户 2026-09-15 定稿，仍在 `AGENTS.md`）：
> ① 空手右键某个面 = 开/关该面开口；② 空手右键指示灯 = 只切那盏灯对应的开口；
> ③ 扳手右键 = 有特殊模式的机器只切模式、没模式的机器照旧切开口；④ 旋转必须 **Ctrl + 扳手右键**。
>
> **2026-09-28（W6 收口）再次扩权**：为给「依赖方向重排」的收口内容腾预算，又搬入两节
> ——「变器波口指示灯几何」（`## 5.`）与「系列特性登记口径」（`## 6.`）。两节都是**原样搬入**，
> 但 `AGENTS.md` 侧**保留了全部「不许 / 禁止 / 必须」句**（见本文件各节的「仍在 AGENTS.md 的部分」）。

## 1. 实现三件套（原文）

实现三件套：契约 **`common/machine/MachineInteraction`**（原 `content/machine/CewsMachine`，P2 改名并从 CEWS 挪到共享层；`hasModeSwitch()`；`onWrenched` 默认"吞掉但不旋转"，防 Create 默认旋转在没按 Ctrl 时生效；`rotateAsCreate()` 直接复用 `IWrenchable` 默认旋转、不复制逻辑；`onEmptyHandPortToggle`）、载荷 **`common/machine/MachineRotatePayload`**（服务端校验：本模组机器 + 无模式 + 手持扳手 + 距离）、客户端 `client/MachineRotateClient`（Ctrl 判定后取消本地交互并发包）。

## 2. 为什么 Ctrl 必须在客户端判定（原文）

**为什么 Ctrl 必须在客户端判定**：使用物品包不带修饰键、Ctrl 也不同步（只有潜行会同步），服务器根本读不到；客户端拦截 + 自定义包才能让"没按 Ctrl 就不旋转"成为服务端权威行为。

> ⚠ 这一条是**必须**级判据：任何"把 Ctrl 判定挪到服务端"的改法都不可能成立（服务端收不到 Ctrl）。

## 3. `IWrenchable` 副作用更正（原文）

副作用更正：`StellarWaveTransmuterBlock` 曾**没实现 `IWrenchable`**（`onWrenched` 无 `@Override`）→ 扳手不分发、切模式不生效；**这是基准 `fbf33cdf~1` 之前（P2 机器交互契约那轮）就修好的**，不记在本次模块分区的账上；规则本身现仍生效。

## 4. 相关取证（本仓现行）

- W3 发布形态测试台（2026-09-28 新增）：`build.gradle` 的 `publishedServer` / `publishedClient`
  运行配置 + `tools/prepare-run-published.ps1`。这一台机器是**第一次**能让"真发布 jar 进游戏"
  成立的路子，机器交互/渲染/JEI 这类运行期症状以后都可以挂在它上面验。

## 5. 变器波口指示灯几何（原文，2026-09-28 从 AGENTS.md 迁入）

**变器波口指示灯几何**（用户 2026-09-14 定稿，改动在 16 个 `stellarstone_wave_transmuter_<i>.json` 里逐变体编码）：灯片 **2×2 px**，**开口时离方块边缘 1 px、关闭时 2 px**（沿顶面往机器内侧挪，非高度方向）；尺寸/y/UV/贴图选择都不随状态变。变体 index 的状态位是 `NORTH=8 / SOUTH=4 / WEST=2 / EAST=1`（`CewsBlocks` datagen）。差波器家族走另一条路——独立 overlay 模型、只在开口时绘制、恒 1 px 内缩。

**仍在 `AGENTS.md` 的部分**（判据本体，不许丢）：灯片 2×2 px、开口 1 px / 关闭 2 px、**尺寸/y/UV/贴图选择都不随状态变**。

**为什么可以搬**：这四组数值是**变体模型的编码细节**（16 个 JSON 逐变体写死），不是"改代码时会踩空"的红线；而 `AGENTS.md` 侧保留的四条判据足以让人知道**改动边界在哪**。

## 6. 系列特性登记口径（原文，2026-09-28 从 AGENTS.md 迁入）

**系列特性登记口径**（用户 2026-09-15 定稿）：common/SeriesTraits 是唯一入口——方块 .transform(SeriesTraits.addStellarstoneTraits()/addThunderiteTraits())、物品链上 .tag(AllModItemTags.STELLARSTONE_ITEMS/THUNDERITE_ITEMS)（Java 没有扩展方法，物品侧写不出 .addXxx()）；判定 = 物品标签 ∪ 系列方块标签 ∪ 注册名约定（限定本模组命名空间）。**四个系列标签由 datagen 生成，手写文件禁止同名**（同名会让 processResources 报 duplicate 直接失败）。两个系列（含方块物品）免疫嬗乱销毁，该判定在 TransmutationDisorderEffect#canTransmutationDestroy 里调 SeriesTraits——方块物品进不了物品标签，故不能用标签覆盖。

**W6 收口后的位置变更（父代理 2026-09-28 补注）**：`TransmutationDisorderEffect` 随嬗化整块并入 `:coe`（第一层），判定本身未变。`common/SeriesTraits` 仍住共享层（被两层以上用 ⇒ 按「全模组共用的东西必须住 SHARED/`core`」）。

**仍在 `AGENTS.md` 的部分**（不许丢的三条）：
- **唯一入口 = `common/SeriesTraits`**；
- **四个系列标签由 datagen 生成，手写文件禁止同名**（同名让 processResources 报 duplicate 直接失败）；
- **两个系列（含方块物品）免疫嬗乱销毁**，判定在 `TransmutationDisorderEffect#canTransmutationDestroy`；**方块物品进不了物品标签，故不能用标签覆盖**。

**为什么可以搬**：搬走的是**链式写法的形式**（`.transform(...)` / `.tag(...)` 与"Java 没有扩展方法所以物品侧写不出 `.addXxx()`"这条实现约束）；留下的是**判据与禁止项**。

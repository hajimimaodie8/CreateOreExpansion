# 新会话交接提示词（CEWS / 能量波阵学 —— 整块复制给新会话即可）

> 这份文件是给**负责 CEWS 这一部分的下一个会话**的开场提示词。它自带：该读什么、现在什么状态、边界在哪、待办什么、必须守什么规矩。
> 生成时间：2026-09-29（交接时的 HEAD = `1e25b015`）。

---

你是 **createoreexpansion** 工程里 **CEWS（能量波阵学 / Create: Energy Wave Studies）第二层** 的**历史留存 + 继续开发**会话。

- 工作目录：`E:\mc\mcmod\createoreexpansion`，分支 `leaf-dev`
- 技术栈：Create 6.0.10 附属，NeoForge 21.1.228 / Minecraft 1.21.1
- 本机：**没有 `pwsh`，用 `powershell`**；本仓 `grep` 会漏文件，**用 `Select-String`**；改文件**只用 write/edit 工具**
- 你负责的范围：**`cews/` 这个模块**（mod id = `cews`）。第一层 `:coe` 有它自己的会话，**别越界去改第一层**（要改先说明理由并问用户）。

## 一、第一件事：读这六份（别凭猜）

| # | 文件 | 它给你什么 |
|---|---|---|
| 1 | `markdown_output/CEWS 层功能清点与变更史.md` | **主档案**：11 台机器 / 3 件物品 / 子系统 / **边界（什么已被裁定搬走）** / 20 个提交的变更史 |
| 2 | `AGENTS.md` | **红线 / 坑 / 工具 / 现行口径**（含 CEWS 那一段；≤ 40,000 B 的硬预算写在它头部） |
| 3 | `markdown_output/CEWS 能量波阵学模块（拆分方案与思索）.md` | 这一层当初为什么这么切：12 条耦合点、边界判据、待定项、验收标准 |
| 4 | `markdown_output/分层重构（依赖方向重排）复盘.md` | 本次重构的判据、**六次「全绿但坏掉」**、工具缺口 |
| 5 | `RELEASE.md` | 发布形态、依赖矩阵、组合方式、已知限制、验证状态 |
| 6 | `build/patch/human-cases-D-E-F1.md` | 只有人能做的那 5 个验收用例（其中 **E1 直接管 CEWS 页**） |

## 二、你的两条职责

**A. 历史留存**
每完成一轮改动，往 `markdown_output/CEWS 层功能清点与变更史.md` 的 **§9.2** 追加一行：

```
- <提交号> <日期> <一句话>（验证：<跑过什么关卡/命令>）
```

三条铁律：**只追加、不改写既有条目**（要更正就追加一条「更正：…」）；**每条必带提交号**；**数字必须可复跑**（§1 给了量法）。

**B. 继续开发**
按下面「当前状态」与「待办」推进。**遇到游戏内容决策，先问用户再动手。**

## 三、当前状态（2026-09-29 交接时）

- `HEAD = 1e25b015`，分支 `leaf-dev`，**交接那一刻**工作树干净（`porcelain = 0`、`?? = 0`）——⚠ **但交接之后**已经发现一个平行会话正在写 COE 盔甲特性（见下面 §3.1）⇒ **永远以你自己跑出来的 `git status` 为准**，不要信这份文档里的状态快照

### 3.1 ⚠ 这个仓库可能有**别的会话同时在改**（2026-09-29 实测）

交接后不久发现：**另一个会话正在实现 COE 盔甲特性**（`CoeArmorMaterials` / `CoeArmorItems` / `content/equipment/armor/CoeArmorItem` 等新文件，且不属 CEWS 范围）。

**因此你的操作纪律是**：
- **只提交你自己的路径**：`git commit -- <路径>`（**绝不要 `git add -A`**——本工程就因为"`git add` 一个文件 + `git commit` 提交整个暂存区"把别人 77 个重命名卷进过自己的提交，见复盘 §7.1）
- **跨层的事不要自己拍**：盔甲是**第一层**的东西，归另一个会话；你只动 `cews/`，要动第一层先说明理由并问用户
- 提交前先 `git diff --cached --name-only` 确认暂存区里只有你要的东西
- **CEWS 的规模与内容**：**75 个 Java 文件 / 18 个包**；注册 **11 台机器 + 3 件物品**；手写资源 assets 497 / data 1，生成资源 assets 28 / data 13；`cews.jar` ≈ 657 KB；创造页**恰好 12 项**
- **分层与依赖**：`cews` 对 `createoreexpansion`（第一层）**required** ⇒ **L2 → L1 合法，反向禁止**；它另外对 `create` 与 **`createaddition`** required，对 `jei` / `curios` optional
- **构建**：`jarJar(implementation(project(':core')))` + **`compileOnly(project(':coe'))`** + 自抄的外部编译面；**没有 `project(':')`**
- **关卡现状**：`check-layering` **EXIT=0**（`CEWS=75`，零违规、白名单为空）；`check-package-overlap` **EXIT=0**（18 包无重叠）；发布形态矩阵里 A2 / C1 / C3 / B 四个用例均 **PASS、0 FAIL**
- **它现在**：是一个干净的、可单独发布的第二层模块；**波引擎、三种机壳、三台应力充能器、两个能量构件都已按裁定归第一层**（见主档案 §4.1）

## 四、★ 三条最容易踩的边界（**动手前先记牢**）

1. **别把已被裁定搬走的东西搬回来**：波引擎（71 文件）、三种机壳、三台应力充能器、两个能量构件的**内容归属**都在第一层。
   —— 判断标准只有一条：**谁需要它，它就必须住在谁那里**（依赖方向是唯一判据）。CEWS 需要的波引擎接口在 `content/wave/api`（第一层），**读它是合法的**。
2. **`compileOnly(project(':coe'))` 不许改成 `implementation` 或 `jarJar`**：前者会让 dev 出现 `duplicate_mod`，后者会让 COE 的 mod 文件出现两次。同理 **`jarJar(implementation(project(':core')))` 不许删**（删了模块 jar 就不能单独拿出去跑）。
3. **语言有两条路径，别混**：`assets/cews/lang/*` 只有 **14 条**（CEWS 自己的命名空间子集）；`assets/createoreexpansion/lang/*` 是**根 lang 的完整拷贝**（403/407 条，P7b 设计）。**根 lang 文件一个字节都不许动。**

## 五、待办（按建议顺序）

1. **先向用户确认两件**（与第一层那份是同一批裁定）：① `energy_sensing_lamp` 那条死 tag 条目怎么处理（它只被 `core/src/main/resources/data/createoreexpansion/tags/block/machines_light.json` 里一条 `"required": false` 的条目引用，方块已下架、Java 里 0 字面量，游戏静默忽略）；② 这些历史档案要不要 `git add -f` 纳管（`markdown_output/` 目前被 `.gitignore:45` 忽略、**0 文件被跟踪**）。
2. **CEWS 页的客户端复核**（唯一能验那条改动的方式）：进游戏按 `E`，确认**能量波阵学页恰好 12 项**、且**两个能量构件与三台充能器都不在那里**（它们应在矿物页）。判据与日志行见作业单 E1。
3. **登记未做的开放项**（主档案 §10）：`AllSpriteShifts` 混合类拆分（共享层瘦身）、`cews.jar` 里 3 个空目录条目（已决议：不动）、CEWS 树里两张无人引用的遗留美术（**按美术红线不动**）。
4. **可选**：`tools/check-data-attribution.ps1` 从「观测型」升级成归属断言。

## 六、必须遵守的规矩（用户的长期要求，原话精神）

- **所有有争议的地方都主动说出来**；**不要中途摇摆**（先说清理解，再一次性执行）。
- 每次做完**自动做分包整理**；写代码**尽量符合 OOP**。
- 用户不说「继续」也要持续推进；**尽量用子代理等方式减少 token 消耗**。
- **代码层的最优化由你决定；涉及游戏内容的决定必须先问用户。**
- **不擅自变更**：注册命名空间（**仍是 `createoreexpansion`**，只有 mod id 是 `cews`）/ 注册 id / 语言键 / 数据包路径。**贴图一个字都不许改、也不许自己画。**
- **`AGENTS.md` 有 40,000 B 硬预算**（当前 ≈ 39,963 B）：任何净增都必须**先把一节搬进 `markdown_output/`**。
- 子代理交回的东西**不要只看报告**：先抽查（`git diff` / `javap` / 日志），再提交；提交用 **`git commit -- <路径>`**。
- 改关卡后要跑**扰动实测**：弄坏必须变红，恢复必须变绿。
- 提交信息里**不要写 `->`**；信息含 ASCII 单引号时用 **`git commit -F <文件>`**。
- **标签页内容构建事件只在逻辑客户端触发** ⇒ 创造页相关的改动只能进游戏验。
- **不要信 `-LaunchMode Direct`**（对「必须到达 Done」的用例会给假 FAIL，用默认 gradle 模式）。
- **Sable 桥接的判据不许简化成只看 modId**：必须**类优先**（`dev.ryanhcode.sable.companion.math.Pose3dc`），modId 只作兜底；并且**未装 Sable 时绝不触碰 Sable 类**。

## 七、开场请先做这三件

1. 读**第一份与第二份**文件（`CEWS 层功能清点与变更史.md`、`AGENTS.md`），然后用两三句话向我复述「**CEWS 现在有什么、它的边界在哪（哪些东西已归第一层）**」——我要确认你真的读到了，而不是接了活就动手。
2. 跑 `git status --porcelain`、`git log -3`，以及 `powershell -NoProfile -ExecutionPolicy Bypass -File tools\check-layering.ps1`，确认状态与本文档写的一致。
3. 告诉我你打算先做哪一件待办，以及是否需要我先给裁定。

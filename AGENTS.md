# AGENTS.md — 给 AI 协作代理的工程备忘

> 本文件是「记忆索引」，只放跨会话必须知道的事实与挂起事项；长文档都在 `markdown_output/`。
> 最后更新：2026-09-10（技能内核对比分析完成后）。

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

## ⏸ 挂起事项（重要，动手前必读）

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

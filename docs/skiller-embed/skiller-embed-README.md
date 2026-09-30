# Skiller 内置内核（coe-embed）—— 留档与升级说明

> 这份文件回答三件事：**内嵌的是哪一版**、**我方改了什么**、**上游更新后怎么升**。
> 相关代码：`libs-maven/com/leaf/skiller/1.0.0/skiller-1.0.0.jar`（工程内本地 maven 布局）、
> `coe/build.gradle` 的 `jarJar(implementation("com.leaf:skiller:1.0.0"))`。

## 1. 上游与版本

| 项 | 值 |
|---|---|
| 上游仓库 | <https://github.com/lizhanyu-leaf/Skiller>（作者 Leaf，MIT） |
| **本 jar 的上游基线** | `cebb47b1151c4954c0c59710216d9a64bfecd2f2`（`origin/master`，2026-09-29 21:26） |
| 我方分支 | `coe-embed`，本地副本 `E:\mc\mcmod\_ref\Skiller`（**未推送**：上游仓库无写权限，403） |
| 我方分支 HEAD | `67a23945258a8c29e1a60d5be9686c8ae499434e` |
| 是否已含上游全部提交 | **是**（`git rev-list --count coe-embed..origin/master` = 0） |
| 产物 jar | `skiller-1.0.0.jar`，114,855 B，SHA256 `30006F8F347CCB24…` |
| 内置位置 | **只有 `:coe`**（用户 2026-09-30 裁定）；`cews` / `transmutation` 不内置 |
| 补丁可复现性 | **已实测**：`git clone` 上游 → `git checkout cebb47b` → `git apply skiller-coe-embed.patch` → 得到的树与 `coe-embed` **逐字节一致**（`0e27799cce1685c54f9302606b471fd88eae0a5a`） |

> ⚠ **本地 fork 的 `master` 分支停在 `abe5688`**（早于上游那两个新提交，因为它是旧克隆）。
> 重建时**不要**从它起步 —— 按 `origin/master`（= `cebb47b`）或下面「换机器重建」一节的做法。

## 2. 我方改了什么（5 笔，顺序即补丁序列）

| 提交 | 一句话 |
|---|---|
| `e0f1275` | 为 COE 嵌入修复 Skiller 的硬阻塞（序列化往返 / Dist 边界 / 空表 NPE / 空组件崩溃 / JarJar 模块名） |
| `c107681` | `ClientSkillCache` 增加 `refresh`，供换工具后重算技能与按键槽位 |
| `9e74158` | `ClientSkillCache` 按键状态比较补默认值，修 `Boolean` 拆箱 NPE |
| `06aa4a9` | 新增 `SkillKeySource` 注入点与总开关闸开关，便于消费方使用自己的键位 |
| `67a2394` | 同 `e0f1275` 的收口（rebase 后重放，含上游缺陷修补，见下） |

**逐项内容**：

1. **`SkillKeySource` 注入点**（`client/SkillKeySource.java` + `ClientSkillCache.setKeySource`）——
   消费方（COE）用自己的 3 个技能键位覆盖内核默认键位，`onKeyInput()` 只监控真正绑了技能的槽位。
2. **`setToggleKeysEnabled`** —— 「按 R 总开关技能系统」这条闸的开关。
   ⚠ **上游已删除开关键与 `KeyCooldown`**，所以此方法现在**只是保留签名的空操作**，
   技能系统改由服务端 `SkillSyncRequestPacket` / `SyncSkillComponentPacket` 驱动。
3. **`ClientSkillCache.refresh(Player)`** —— 换主手后重算技能表与受监控键槽，并重建渲染调度。
4. **按键比较补默认值** —— `pressed.getOrDefault(idx, Boolean.FALSE)`，
   否则首次按键时对 `null` 拆箱抛 NPE。
5. **`StrategyRenderers.schedule()` 判错对象** —— `instance.skill()` 是 `ItemSkillRegistration`
   注册条目，技能本体在 `getSkill()`；原写法让每次迭代都 `return`，**策略渲染从未被调度过**。
6. **`SkillData` 序列化往返** —— 工厂 id 的写入与读出共用同一常量 `KEY_FACTORY`，避免漂移。
7. **`Automatic-Module-Name: skiller`**（上游 `build.gradle` 的 `jar` 块）——
   没有显式 Java 模块名时，其他模组**无法**用 JarJar 嵌套这个 jar。

## 3. 我方额外修掉的上游缺陷（不修就崩）

| 文件 | 缺陷 | 后果 |
|---|---|---|
| `server/ServerSkillCache.java` | 标了 `@EventBusSubscriber`，却**没有任何 `@SubscribeEvent` 方法** | NeoForge 抛 `has no @SubscribeEvent methods, but register was called anyway` ⇒ **dev 直接拒载** |
| `server/ServerSkillCache.java` | `getComponent` javadoc 承诺「未启用返回 null」，实现却 `throw` | 与文档矛盾；改为返回 null |
| `content/factory/NbtSkillInstanceFactory.java` | `toData` javadoc 说解析不到记日志返回 null，实现却 `throw` | 与文档矛盾；改为返回 null，让调用方能丢弃该实例 |
| `foundation/skill/SkillBundle.java` | `releaseSkills` 取的是**技能定义表**（`getSkills`），不是实例表 | 迭代时 `ClassCastException`；已改为 `skillData.get(type)` |

## 4. 上游更新后怎么升（照这个顺序做）

```powershell
# 0) 先备份本地分支（失败可回退）
cd E:\mc\mcmod\_ref\Skiller
git branch -f backup/coe-embed-pre-merge coe-embed

# 1) 拉上游
git fetch origin --prune

# 2) 把 coe-embed 的 5 笔 rebase 到新 master（会有冲突，见下面"冲突处理口径"）
git rebase origin/master

# 3) 若冲突：逐个文件按语义合并，然后
git add -u ; git -c core.editor=true rebase --continue

# 4) 编译 + 出 jar
.\gradlew.bat jar

# 5) 装入工程
Copy-Item build\libs\skiller-1.0.0.jar `
  E:\mc\mcmod\createoreexpansion\libs-maven\com\leaf\skiller\1.0.0\skiller-1.0.0.jar -Force

# 6) 在 COE 侧验证（这一步必须过）
cd E:\mc\mcmod\createoreexpansion
.\gradlew.bat runData           # 冒烟：能加载 skiller mod
.\gradlew.bat jar               # 出 coe.jar
# 核对 jar 内 skiller 与 libs-maven 那份字节一致：
#   jar 内 META-INF/jarjar/skiller-1.0.0.jar 的 Length 应等于 libs-maven 里那份的 Length

# 7) 更新补丁留档
git -C E:\mc\mcmod\_ref\Skiller diff --output=docs\skiller-embed\skiller-coe-embed.patch origin/master..coe-embed
```

> ⚠ **必须用 `git diff --output=<file>`**，不要用 PowerShell 的 `>` 重定向：
> PS 5.1 会把输出写成 UTF-16，`git apply` 直接报 `No valid patches in input`（本轮踩过）。
> 自检：补丁文件前 3 字节应为 `100 105 102`（即 `"dif"`）。

**冲突处理口径**（本仓 2026-09-30 那轮的经验，逐条都是真实取舍）：

| 情况 | 取哪边 | 理由 |
|---|---|---|
| 上游也修了同一个 bug（如 `pressed` 拆箱 NPE、`SkillData` 工厂 id） | **取上游** | 同一修复不维护两份 |
| 我方新增的能力（`SkillKeySource` / `refresh` / `cacheKeys`） | **取我方** | 上游没有对应物，删了就断 COE |
| 上游删除了我方依赖的 API（开关键、`KeyCooldown`） | **改成等价薄封装** | 让 COE 侧零改动编译，同时尊重上游新设计 |
| 上游新增方法/字段 | **必须保留** | 解冲突时最容易被吃掉（本轮 `ServerSkillCache` 就吃过一次） |

**解冲突后的自检**（强烈建议每次升级都跑）：
`docs/skiller-embed/skiller-api-diff.ps1` —— 逐文件比对上游，报出「上游有、我方缺失的 public static 方法」
与「`@SubscribeEvent` 数量变少」。本轮它确认 11 个改动文件无一丢失上游方法。

## 5. 留档文件清单（**住在 `docs/skiller-embed/`，被 git 纳管**）

> ⚠ 早先这几份放在 `build/patch/` 下 —— 那是**错的**：`build/` 被 `.gitignore` 第 3 行忽略，
> 放那里等于没留档（`git add` 被静默拒绝，`git clean -xfd` 一跑就没）。2026-09-30 已迁到 `docs/` 下。

| 文件 | 内容 |
|---|---|
| `skiller-embed-README.md` | 本文件（版本、改动、升级步骤、冲突口径） |
| `skiller-coe-embed.patch` | 上游 `cebb47b` → 我方 `67a2394` 的完整补丁（62,070 B，12 文件 / +692 −74） |
| `skiller-coe-embed-commits.txt` | 上述 5 笔提交的哈希与标题（逐行） |
| `skiller-api-diff.ps1` | 升级自检脚本（找丢失的上游方法 / 变少的订阅者） |
| `libs-maven/com/leaf/skiller/1.0.0/skiller-1.0.0.jar` | 我方构建产物（随仓库提交，发布时被 `:coe` 嵌进 jar） |
| `libs-maven/com/leaf/skiller/1.0.0/*.pom` + `LICENSE.txt` | 本地 maven 坐标与上游 MIT 许可原件 |

> ⚠ **本地 `E:\mc\mcmod\_ref\Skiller` 那份 fork 才是唯一含全套补丁的可编译副本**
> （上游仓库没有 `coe-embed` 分支）。补丁留档的目的就是换机器也不必依赖 `_ref` 目录：

```powershell
# 换机器重建（不需要 _ref 目录，也不需要写权限）
git clone https://github.com/lizhanyu-leaf/Skiller.git skiller-work
cd skiller-work
git checkout -B coe-embed cebb47b          # 我方补丁的上游基线
git apply <本目录>\skiller-coe-embed.patch # 12 文件 / +692 -74
.\gradlew.bat jar                          # 产物应与 114,855 B / SHA 30006F8F… 一致
```

若重建后的 jar 哈希对上，就说明补丁留档完好；对不上则先看 `skiller-api-diff.ps1` 的输出。

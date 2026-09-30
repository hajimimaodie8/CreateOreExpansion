# 工程环境与工具链细节（从 AGENTS.md 迁入 2026-09-29）

> 2026-09-29 为给 `AGENTS.md` 的 **40,000 B 预算**腾空间，把两节**原样搬进**本文件（一字未删）：
> 「工程坑与自检 §2 javadoc 检查」与「已知环境限制」。`AGENTS.md` 侧只留一行指针。
> **搬走的理由**：这两节都是**程序性 / 环境性**内容，不是「改代码时会踩空」的红线。

## 2. javadoc 检查（本工程 javadoc 任务默认跑不通）

javadoc 用平台编码（本机 GBK）读 UTF-8 源码 ⇒ 满屏乱码 + 假告警，任务从建立起就是红的。跑法：`cmd /c ""%JAVA_HOME%\bin\javadoc.exe" @build\patch\javadoc_utf8.options -d build\patch\jd_out -Xdoclint:all,-missing -J-Duser.language=en -J-Duser.country=US > build\patch\jd.log 2>&1"`（**必须 `cmd /c` 直连**：PowerShell 的 `*>` 会把 stderr 包成 NativeCommandError 并按宽度折行、把绝对路径切碎、毁掉诊断行）；options 由 `.\gradlew.bat javadoc` 生成后需追加 `-encoding UTF-8 -docencoding UTF-8`。现状：仅剩 3 处错误且全在 `content/skill/*`。

## 3. 已知环境限制

- **Hindsight 记忆**：已配置（`C:\Users\Lenovo\.hindsight\coding-agent.json`，bank `coding-agent::createoreexpansion`）但**已关闭全部自动导入**（写入规则见 `AGENTS.md` 的「🧠 记忆写入规则」节）；配置改动**下一个新会话**才生效。
- **`maven.neoforged.net` 可达**（2026-09-19 实测）：Skiller 参考工程可在本机构建 —— `cd E:\mc\mcmod\_ref\Skiller && .\gradlew.bat jar`（首次下载 neoform-runtime，几分钟；偶发 `Connection reset` 重跑一次）。内置用的 jar 由它产出。

## 4. 2026-09-29 本轮实测新增的 PowerShell / git 陷阱

1. **`git commit -m '…'` 的信息里含 ASCII 单引号会把 PowerShell 字符串提前截断**
   实例：信息里写了 `already exists in the tab's list`，那个 `'` 终止了我的单引号字符串，后面的碎片被当成 git 参数，报
   `fatal: /: '/' is outside repository at 'E:/mc/mcmod/createoreexpansion'`（**提交没发生，暂存区没动** —— 这一点很重要：它是安全失败）。
   **规避**：信息里有任何 ASCII 单引号时，**写进文件再用 `git commit -F <文件>`**（`build/patch/*.txt` 已被 ignore）。
2. **`Select-String` 没有 `-Recurse` 参数**（那是 `Get-ChildItem` 的）。写 `Select-String -Path dir -Pattern x -Recurse` 会直接报 `找不到与参数名称"Recurse"匹配的参数`。
   正确写法：`Get-ChildItem dir -Recurse -File -Filter *.java | Select-String -Pattern x`。
3. **`Get-ChildItem -File -Include *.java` 只给一个目录路径时返回空**（`-Include` 需要通配符路径）。要么用 `-Filter *.java`，要么路径写成 `dir\*`。
   —— 这个坑会**静默给出"0 个文件"**，比报错更坏：本轮我就因此一度以为 `cews/src/main/java` 下没有注册类。
4. **`git commit -- <未跟踪文件>` 不成立**：`--` 之后是路径过滤器，未跟踪文件不在索引里 ⇒ 报 `pathspec did not match any file(s) known to git`。**未跟踪文件必须先 `git add`。**

## 5. 数据包标签目录是单数（写错就静默失效）＋ 留档不能放 `build/`

> 2026-09-30 从 `AGENTS.md` 原地迁入（为腾出该文件的 40,000 B 预算），内容未改。

**5.1 标签目录单数**

写成 `tags/items/`（复数）= 一个"名叫 items 的注册表标签目录"，游戏不认识 ⇒ **整个文件被忽略且不报错**（实例：`transmutation_protected.json` 曾放错 ⇒ 龙蛋/下界之星/信标从没被保护）。`runData` 只校验 `src/generated/**` ⇒ 手写文件放错目录**没有任何关卡会报**。

正确目录名恒为单数：`data/<ns>/tags/item/**`、`data/<ns>/tags/block/**`、`data/<ns>/tags/fluid/**`、`data/<ns>/tags/entity_type/**`。

另：手写文件与 datagen 产出**同名会让 `processResources` 报 duplicate 而构建失败**（本仓造过一次）——两者只能留一个。

**5.2 留档类文件不能放 `build/`**

`.gitignore` 第 3 行是 `build/`，它匹配**任意层级**的 `build/`（含根与各子工程）。后果与第 1 节同一机制：`git add build/patch/xxx` 会被**静默拒绝**（`git status` 里也看不到），`git clean -xfd` 一跑就没。

- `build/patch/*` 的定位本来就是「**取证脚本，按需重生成**」；凡是**要长期留存**的东西（补丁留档、升级说明、API 契约）必须放 **`docs/`** 或 `markdown_output/`（二者都被纳管）。
- 实例：Skiller 的内嵌补丁留档最初放 `build/patch/skiller-coe-embed.patch` ⇒ 等于没留档；2026-09-30 迁到 `docs/skiller-embed/`（见 `docs/skiller-embed/skiller-embed-README.md`）。
- 自检：`git check-ignore -v <路径>`；以及 `git ls-files <目录>` 看它到底有没有被跟踪。

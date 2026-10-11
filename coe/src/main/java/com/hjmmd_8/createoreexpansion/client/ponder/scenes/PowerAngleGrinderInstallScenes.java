package com.hjmmd_8.createoreexpansion.client.ponder.scenes;

import com.hjmmd_8.createoreexpansion.client.ponder.PonderSceneSupport;
import com.hjmmd_8.createoreexpansion.client.ponder.PowerAngleGrinderPonderScenes;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeItems;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeMachines;
import com.hjmmd_8.createoreexpansion.content.grinding.block.PowerAngleGrinderBlock;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.logistics.funnel.FunnelBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * <b>第一幕：安装动力角磨轮并使之运作</b>
 * （storyboard path {@code power_angle_grinder/install_and_run}，结构
 * {@code assets/createoreexpansion/ponder/power_angle_grinder/install_and_run.nbt}）。
 *
 * <h2>一、来源与「照搬 / 重写」口径</h2>
 * <p>整幕是 Ponderer JSON {@code run/config/ponderer/scripts/power_angle_grinder.json} 的
 * {@code scenes[0]}（Ponderer 自动生成的 id {@code scene_1}，<b>74 步</b>）的 Java 落点，
 * 正式名与注册表见 {@link PowerAngleGrinderPonderScenes#INSTALL_AND_RUN}。
 * 每个分段都带一行注释头，写明 <b>原 JSON 怎么做 / 现在怎么做 / 为什么</b>。</p>
 *
 * <h2>二、74 步 → 13 段（段号与方案 §4.5 的对照表逐字对应）</h2>
 * <table border="1">
 *   <caption>分段对照</caption>
 *   <tr><th>段</th><th>JSON {@code scene_1} 步号</th><th>内容</th><th>照搬/重写</th></tr>
 *   <tr><td>①</td><td>steps[0..1]</td><td>底板亮相</td><td>照搬（+ 补 {@code addKeyframe}）</td></tr>
 *   <tr><td>②</td><td>steps[2]</td><td>角磨床滑入</td><td><b>重写</b>（补 erase）</td></tr>
 *   <tr><td>③</td><td>steps[3..5]</td><td>开场文案 ⌈text_1⌋</td><td>照搬</td></tr>
 *   <tr><td>④</td><td>steps[6..10]</td><td>开盖演示 ⌈text_2⌋</td><td>照搬</td></tr>
 *   <tr><td>⑤</td><td>steps[11..12]</td><td>为何要装轮 ⌈text_3⌋</td><td>照搬</td></tr>
 *   <tr><td>⑥</td><td>steps[13..17]</td><td>装轮演示 ⌈text_4⌋</td><td>照搬</td></tr>
 *   <tr><td>⑦</td><td>steps[18..22]</td><td>合盖演示 ⌈text_5⌋</td><td>照搬</td></tr>
 *   <tr><td>⑧</td><td>steps[23..32]</td><td>搭动力（6 步齿轮组）</td><td>照搬（粒子 = true）+ <b>批 C 订正</b>（补 {@code ensureSceneCanShowRange}）</td></tr>
 *   <tr><td>⑨</td><td>steps[33..36]</td><td>转速控制器滑入 ⌈text_6⌋</td><td><b>重写</b>（补 erase）+ 照搬</td></tr>
 *   <tr><td>⑩</td><td>steps[37..48]</td><td>开盖投料 ⌈text_7⌋</td><td>照搬</td></tr>
 *   <tr><td>⑪</td><td>steps[49..52]</td><td>合盖加工</td><td>照搬</td></tr>
 *   <tr><td>⑫</td><td>steps[53..58]</td><td>取出产物 ⌈text_8⌋</td><td>照搬</td></tr>
 *   <tr><td>⑬</td><td>steps[59..73]</td><td>漏斗进出 ⌈text_9⌋ + 抽入动画</td><td><b>重写</b>（漏斗滑入 / 掉落物）+ 照搬</td></tr>
 * </table>
 * <p>「重写」的理由逐条写在对应分段的注释头里：甲类 {@code set_block}（带 {@code entranceAnimation}）
 * 要补一行 Ponderer 的去重 {@code erase}（见 ②/⑨/⑬），{@code create_item_entity} 换成官方
 * 便捷方法（见 ⑬），以及作者 2026-10-10 追加的漏斗抽入动画（见 ⑬ 末尾）。
 * <b>批 C 另外订正了 ⑧：乙类 {@code set_block} 之前必须补
 * {@code PonderSceneSupport.ensureSceneCanShowRange}（Ponderer 的"长包围盒 + 并回基准世界段"），
 * 否则场景包围盒（(0,0,0)-(4,9,4)）之外的 (4,1,5)/(4,2,5)/(3,0,5) 三格会被
 * {@code ReplaceBlocksInstruction} 静默吞掉 —— 这是本批唯一一处真静默失效，机制见
 * {@link com.hjmmd_8.createoreexpansion.client.ponder.PonderSceneSupport}。</b></p>
 *
 * <h2>三、为什么用 Create 的 {@code CreateSceneBuilder}</h2>
 * <p>批 A 用的是裸 {@code SceneBuilder}（那时一个 Create 专有指令都不需要）。本批 ⑬ 要用
 * Create 官方的 <b>安山漏斗抽入动画</b>，而那个 API 只住在 {@code CreateSceneBuilder} 的
 * {@code WorldInstructions} 里：{@code flapFunnel(BlockPos, boolean)}
 * （{@code CreateSceneBuilder.java:259-261}）。升级路径就是 {@code CreateSceneBuilder} 的唯一构造器
 * —— {@code CreateSceneBuilder(SceneBuilder)}（{@code :61-63}，内部转交 {@code baseSceneBuilder.getScene()}），
 * 与 Create 官方每一个思索的写法逐字相同（例如 {@code ArmScenes.java:28}）。
 * {@code CreateSceneBuilder} 继承 {@code PonderSceneBuilder} ⇒ 批 A 的那些调用一行都不用改。</p>
 *
 * <h2>四、⚠ 只能进游戏验的部分</h2>
 * <p>底板是否出现、机器是否"只有一台"（不重叠）、文案编号是否错位、BE 的 {@code open}/{@code Wheel}/{@code Items}
 * 状态是否按 JSON 变化、漏斗翻板是否真的往内偏 —— 静态关卡一律看不见
 * （{@code compileJava} / {@code runData} 都不构造 Ponder 场景、不跑渲染、不跑 BE tick）。</p>
 */
public final class PowerAngleGrinderInstallScenes {

    /**
     * ⚠ 这里**没有**任何英文/中文文案 —— 那是硬要求（作者 2026-10-10）。
     * {@code scene.title(...)} 与 {@code .text(...)} 的 String 入参在运行时**不显示**
     * （它们只是 Ponder 在编辑模式下用的默认值：{@code PonderSceneBuilder.java:141-143} 与
     * {@code TextWindowElement.java:78-81} → {@code PonderScene.java:404-410}；
     * 非编辑态一律走 {@code PonderLocalization.getSpecific} → {@code I18n.get(键)}），
     * 所以两处都传**键本身**，文案只写在根的两份 {@code LanguageProvider} 里。
     * 键的取值与换算见 {@link PowerAngleGrinderPonderScenes#INSTALL_AND_RUN_HEADER}。
     */

    /** 角磨床在场景里的唯一格：原 JSON 两幕都把它放在 {@code [3,1,3]}，底板结构 {@code basic.nbt} 的 {@code size} 是 5x10x5。 */
    private static final BlockPos MACHINE_POS = new BlockPos(3, 1, 3);

    /** 转速控制器：原 JSON {@code steps[33]} 放在 {@code [4,1,3]}。 */
    private static final BlockPos SPEED_CONTROLLER_POS = new BlockPos(4, 1, 3);

    /**
     * 安山漏斗：原 JSON {@code steps[61]} 放在 {@code [3,1,2]}，{@code facing=north / extracting=false}
     * —— 也就是"漏斗口朝北、把吸进来的东西往背后（南边 = 角磨床那一格）推"。
     * ⚠ 后面 {@code steps[65]} 的 {@code clear_item_entities} 与作者追加的抽入动画都打在这一格上。
     */
    private static final BlockPos FUNNEL_POS = new BlockPos(3, 1, 2);

    private PowerAngleGrinderInstallScenes() {}

    public static void installAndRun(SceneBuilder builder, SceneBuildingUtil util) {
        // CreateSceneBuilder.java:61-63 —— 官方唯一升级路径；PonderSceneBuilder 的子类，
        // 所以下面批 A 已有的 idle/overlay/addInstruction/title/markAsFinished 全部照旧可用。
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        // ========================================================================
        // ==== 对应 JSON scene_1 steps[0..1]（show_structure → idle 20）====  【照搬】
        // 原 JSON：show_structure（**没有** blockPos ⇒ Ponderer 走 select().everywhere()）→ idle 20；
        //          该步带 attachKeyFrame=true，而 Ponderer 对**任何** type 都先看这个字段并补一个关键帧
        //          （DynamicPonderPlugin.java:492-494）⇒ 关键帧要排在 showSection 之前。
        // 现在   ：addKeyframe() + title(...) + showSection(util.select().everywhere(), Direction.UP) + idle(20)
        //          —— 与 DynamicPonderPlugin.java:1019/1023 逐字对应（everywhere + Direction.UP）。
        // 为什么 ：结构 basic.nbt 就是"5x10x5 的原版底板"（white_concrete + snow_block），
        //          所以**不**用 configureBasePlate/showBasePlate（那会换掉作者现在看到的底板外观）。
        // 订正   ：批 A 漏了这一处的 addKeyframe()（原 JSON s1[0] 的 attachKeyFrame=true），本批补上。
        // ========================================================================
        scene.addKeyframe();
        scene.title(PowerAngleGrinderPonderScenes.INSTALL_AND_RUN,
            PowerAngleGrinderPonderScenes.INSTALL_AND_RUN_HEADER);
        scene.world().showSection(util.select().everywhere(), Direction.UP);
        scene.idle(20);

        // ========================================================================
        // ==== 对应 JSON scene_1 steps[2]（set_block 动力角磨床，entranceAnimation=north / duration=20 / interval=1）====  【重写】
        // 原 JSON：applyAnimatedSetBlock(DynamicPonderPlugin.java:1373) =
        //           ① ensureSceneCanShowRange(forceVisibleNow=false) → 从 base 世界段 erase 该格（:1407-1411）
        //           ② world().setBlocks(sel, state, false) —— 方块立刻落地、不冒粒子（:1384，因为 immediateDisplay=false ⇒ particles=false）
        //           ③ applySetBlockNbtPatch（本处无 nbt ⇒ 空转）
        //           ④ applyAnimatedShowSectionAndMerge → DisplayWorldSectionInstruction(20, NORTH, 单组, null)（:1665）
        // 现在   ：手写 erase instruction → setBlock(..., false) → showIndependentSection(sel, NORTH) → idle(5)
        // 为什么 ：①官方 showIndependentSection（PonderSceneBuilder.java:445-450）用的**就是**同一个
        //            DisplayWorldSectionInstruction，但它 ① 固定 15 tick（原为 20）② **不擦除** base 世界段；
        //            而 show_structure 用的是 everywhere()（一个包围盒选区），机器一落地就已经被 base 段渲染出来了，
        //            不先擦就会变成"静止一台 + 滑入一台"两台重叠。
        //          ②erase 排在 setBlock **前面**（与 Ponderer 的 ①② 同序）。
        //            ⚠ 批 C 订正一句批 A/B 写歪的话：Ponder 的指令**不是**"每条占一个 tick" ——
        //              PonderInstruction.isBlocking() 缺省返回 false，只有 idle（DelayInstruction）挡后续
        //              （PonderScene.java:311-322）⇒ 本条 erase、setBlock、showIndependentSection
        //              在**同一个 tick** 内按加入顺序跑完。所以真正要守的是"三条之间不许插 idle"，
        //              先后次序本身不影响画面（两条都跑完才渲染）；保留 Ponderer 的原序只为逐字对应。
        //          ③erase 的写法照官方 hideSection（PonderSceneBuilder.java:465-471）的惯用法，等价于 :1409 那两行。
        // 代价   ：淡入 20 → 15 tick，用 idle(5) 把 20-15 补回来对齐总时长（Ponderer 那边是 instruction 自己
        //          占满 20 tick，所以两者后面接的下一个 JSON 步会同时开始）；entranceInterval 的
        //          "逐行错开"语义丢失（**本 JSON 用不到**：两幕的这 6 处都是单格 + interval=1 ⇒ 只有一组）。
        // ========================================================================
        Selection machine = util.select().position(MACHINE_POS);
        // 原 JSON blockProperties = {"facing":"east"}；OPEN 不写 —— PowerAngleGrinderBlock 的
        // registerDefaultState 已把 open 定为 false（PowerAngleGrinderBlock.java:74），与 JSON 一致。
        BlockState machineState = CoeMachines.POWER_ANGLE_GRINDER.getDefaultState()
            .setValue(HorizontalKineticBlock.HORIZONTAL_FACING, Direction.EAST);
        scene.addInstruction(ps -> {                              // 重写：补 Ponderer 的去重 erase（DPP:1409）
            ps.getBaseWorldSection().erase(machine);
            ps.getBaseWorldSection().queueRedraw();
        });
        scene.world().setBlock(MACHINE_POS, machineState, false); // 照搬：DPP:1384 setBlocks(..., false)
        scene.world().showIndependentSection(machine, Direction.NORTH); // 重写：官方滑入（15 tick，原为 20）
        scene.idle(5);                                            // 重写：把 20-15=5 tick 补回来

        // ========================================================================
        // ==== 对应 JSON scene_1 steps[3..5]（idle 5 → text#1 → idle 80）====  【照搬】
        // 原 JSON：idle 5 → text（duration=60, attachKeyFrame=true, placeNearTarget=true, point=[3,2,3]）→ idle 80
        // 现在   ：idle(5) → addKeyframe() → showText(60)...placeNearTarget() → idle(80)
        //          —— DynamicPonderPlugin.java:492-494 对**任何** type 都先看 attachKeyFrame，
        //             所以关键帧要排在文案指令之前；:599-612 对应 duration/pointAt/placeNearTarget。
        // ⚠ 编号：这是本幕的**第 1 次** showText ⇒ 它就是 lang 键
        //          createoreexpansion.ponder.power_angle_grinder/install_and_run.text_1。
        //          下面每一次 showText 都带一行 "text_N ← JSON steps[i]" 的编号注释。
        // ========================================================================
        scene.idle(5);
        scene.addKeyframe();
        scene.overlay()
            .showText(60)
            .text(PowerAngleGrinderPonderScenes.INSTALL_AND_RUN_TEXT_1)   // text_1 ← JSON steps[4]
            .pointAt(util.vector().of(3, 2, 3))
            .placeNearTarget();
        scene.idle(80);

        // ========================================================================
        // ==== 对应 JSON scene_1 steps[6..10]（show_controls(wrench) → text#2 → idle 10 → 开盖 nbt → idle 70）====  【照搬】
        // 原 JSON：show_controls(point=[3,3,3], direction=down, action=right, item=create:wrench, duration=60)
        //          → text（duration=60, placeNearTarget, point=[3,2,3]）→ idle 10
        //          → modify_block_entity_nbt{blockProperties:{facing:east, open:true}, blockPos:[3,1,3], nbt=…}
        //          → idle 70
        // 现在   ：showControls(...).rightClick().withItem(wrench.asStack())
        //          → showText → idle(10) → modifyBlock(OPEN=true) → mergeNbt(…, false) → idle(70)
        //          —— 对应 DynamicPonderPlugin.java:822-850（show_controls）与 :1811-1858（modify_block_entity_nbt：
        //             先按 blockProperties 逐格 modifyBlock，再 modifyBlockEntityNBT(…, redraw)，
        //             而本 JSON 没有 reDrawBlocks 字段 ⇒ 第 4 参恒 false）。
        // no-op  ：blockProperties 里的 facing=east 是 steps[2] 已经设过的值，Ponderer 会再 setValue 一遍
        //          （:1507-1528 对每个属性 entry 都无条件 setValue），语义上等于没变，这里不重复发指令。
        // ========================================================================
        scene.overlay()
            .showControls(util.vector().of(3, 3, 3), Pointing.DOWN, 60)
            .rightClick()
            .withItem(AllItems.WRENCH.asStack());
        scene.overlay()
            .showText(60)
            .text(PowerAngleGrinderPonderScenes.INSTALL_AND_RUN_TEXT_2)   // text_2 ← JSON steps[7]
            .pointAt(util.vector().of(3, 2, 3))
            .placeNearTarget();
        scene.idle(10);
        scene.world().modifyBlock(MACHINE_POS, s -> s.setValue(PowerAngleGrinderBlock.OPEN, true), false);
        PonderSceneSupport.mergeNbt(scene, machine, false,
            "{Filter:{},FilterAmount:64,Inventory:{AppliedRecipe:0b,Items:[],ProcessingTime:-1.0f,RecipeTime:0.0f,Size:32},RecipeIndex:0,Speed:0.0f,UpTo:1b}");   // JSON steps[9] 的 nbt 逐字
        scene.idle(70);

        // ========================================================================
        // ==== 对应 JSON scene_1 steps[11..12]（text#3 → idle 70）====  【照搬】
        // 原 JSON：text（duration=60, placeNearTarget, point=[3,2,3]）→ idle 70
        // ========================================================================
        scene.overlay()
            .showText(60)
            .text(PowerAngleGrinderPonderScenes.INSTALL_AND_RUN_TEXT_3)   // text_3 ← JSON steps[11]
            .pointAt(util.vector().of(3, 2, 3))
            .placeNearTarget();
        scene.idle(70);

        // ========================================================================
        // ==== 对应 JSON scene_1 steps[13..17]（show_controls(iron_grinding_wheel) → text#4 → idle 10 → 装轮 nbt → idle 70）====  【照搬】
        // 原 JSON：show_controls(item=createoreexpansion:iron_grinding_wheel, point=[3,3,3], down/right, 60)
        //          → text#4 → idle 10 → modify_block_entity_nbt{open:true, nbt 里多一个
        //            Wheel:"createoreexpansion:iron_grinding_wheel"} → idle 70
        // ⚠ nbt 串逐字照抄：Wheel 的冒号必须带引号（SNBT 里带 ':' 的字符串不合法，必须引起来）。
        // ========================================================================
        scene.overlay()
            .showControls(util.vector().of(3, 3, 3), Pointing.DOWN, 60)
            .rightClick()
            .withItem(CoeItems.IRON_GRINDING_WHEEL.asStack());
        scene.overlay()
            .showText(60)
            .text(PowerAngleGrinderPonderScenes.INSTALL_AND_RUN_TEXT_4)   // text_4 ← JSON steps[14]
            .pointAt(util.vector().of(3, 2, 3))
            .placeNearTarget();
        scene.idle(10);
        scene.world().modifyBlock(MACHINE_POS, s -> s.setValue(PowerAngleGrinderBlock.OPEN, true), false);
        PonderSceneSupport.mergeNbt(scene, machine, false,
            "{Filter:{},FilterAmount:64,Inventory:{AppliedRecipe:0b,Items:[],ProcessingTime:-1.0f,RecipeTime:0.0f,Size:32},RecipeIndex:0,Speed:0.0f,UpTo:1b,Wheel:\"createoreexpansion:iron_grinding_wheel\"}");   // JSON steps[16] 的 nbt 逐字
        scene.idle(70);

        // ========================================================================
        // ==== 对应 JSON scene_1 steps[18..22]（show_controls(wrench) → text#5 → idle 10 → 合盖 nbt → idle 70）====  【照搬】
        // 原 JSON：show_controls(create:wrench) → text#5 → idle 10
        //          → modify_block_entity_nbt{open:false, nbt 里仍带 Wheel} → idle 70
        // ========================================================================
        scene.overlay()
            .showControls(util.vector().of(3, 3, 3), Pointing.DOWN, 60)
            .rightClick()
            .withItem(AllItems.WRENCH.asStack());
        scene.overlay()
            .showText(60)
            .text(PowerAngleGrinderPonderScenes.INSTALL_AND_RUN_TEXT_5)   // text_5 ← JSON steps[19]
            .pointAt(util.vector().of(3, 2, 3))
            .placeNearTarget();
        scene.idle(10);
        scene.world().modifyBlock(MACHINE_POS, s -> s.setValue(PowerAngleGrinderBlock.OPEN, false), false);
        PonderSceneSupport.mergeNbt(scene, machine, false,
            "{Filter:{},FilterAmount:64,Inventory:{AppliedRecipe:0b,Items:[],ProcessingTime:-1.0f,RecipeTime:0.0f,Size:32},RecipeIndex:0,Speed:0.0f,UpTo:1b,Wheel:\"createoreexpansion:iron_grinding_wheel\"}");   // JSON steps[21] 的 nbt 逐字
        scene.idle(70);

        // ========================================================================
        // ==== 对应 JSON scene_1 steps[23..32]（5 个 set_block + 5 个 idle 2）====  【照搬 + ⚠ 批 C 订正】
        // 原 JSON：每个 set_block 都**没有** entranceAnimation（写成 "none"）、immediateDisplay=true、
        //          spawnParticles 字段缺失 ⇒ 走 DynamicPonderPlugin.java:1351 的 else 支：
        //            ① ensureSceneCanShowRange(…, forceVisibleNow=true)（:1404-1424）：
        //                 长场景包围盒到该格 + 把该格并回基准世界段
        //            ② immediateDisplay（缺省 true）⇒ setBlocks(…, particles)（:1356/:1359）
        //            ③ particles = immediateDisplay && !FALSE.equals(spawnParticles)
        //               —— spawnParticles **缺失**时 !Boolean.FALSE.equals(null) == true ⇒ **粒子 = true**
        //          nbt 走 applySetBlockNbtPatch（:1427-1439）⇒ modifyBlockEntityNBT(…, **true**)。
        // 现在   ：ensureSceneCanShowRange(scene, pos) → setBlock(pos, state, true)
        //          → PonderSceneSupport.mergeNbt(scene, sel, **true**, …) → idle(2)
        // ⚠ 粒子那一项最容易漏：本段的齿轮组是"立刻出现 + 带粒子"，不是滑入。
        // ⚠⚠ 批 C 订正（改前 → 改后 → 为什么，本批唯一一处真静默失效）：
        //   改前：只有 setBlock + mergeNbt。本段的 5 格里，(4,1,5)/(4,2,5)/(3,0,5) 三格在
        //         场景包围盒之外 —— 两幕的结构都是 Ponderer 的 basic.nbt（size=[5,10,5]，
        //         25 个底板方块在 y=0 + 1 个 air 在 (4,9,4)）⇒ placeInWorld 之后盒子是
        //         (0,0,0)-(4,9,4)，z=5 **越界**；而官方 set_block 落在 ReplaceBlocksInstruction，
        //         它第一句就是 `if (!level.getBounds().isInside(pos)) return;`（:29-31）
        //         ⇒ 这三格被**直接吞掉**（同格的三条 mergeNbt 也连带成 no-op：盒子外没有 BE）。
        //   改后：每一格前先 PonderSceneSupport.ensureSceneCanShowRange(scene, pos)，
        //         逐字对应 Ponderer 的 ensureSceneCanShowRange（:1404-1424）那两行
        //         `getBounds().encapsulate(...)` + `getBaseWorldSection().add(...)`。
        //   为什么：Ponderer 每次放方块前都先长盒 + 把该格并回基准世界段，我们的移植只继承了
        //         它的 erase 那一半，漏了这一半。机制全文与三层证据链写在 PonderSceneSupport
        //         的类注释上，此处不重复。
        // ⚠ 顺序：两条指令都**不是** blocking（PonderInstruction.isBlocking 缺省 false，只有 idle 挡后续）
        //         ⇒ 同一 tick 内先长盒后放方块；反过来放那一刻盒子还没长，照样被吞。
        // ========================================================================
        // JSON steps[23] large_cogwheel @ (4,2,3)
        PonderSceneSupport.ensureSceneCanShowRange(scene, new BlockPos(4, 2, 3));
        scene.world().setBlock(new BlockPos(4, 2, 3), AllBlocks.LARGE_COGWHEEL.getDefaultState()
            .setValue(BlockStateProperties.AXIS, Direction.Axis.Z), true);
        PonderSceneSupport.mergeNbt(scene, util.select().position(4, 2, 3), true,
            "{ConnectedToExtraKinetics:0b,Network:{Capacity:262144.0f,Id:824633061447L,Size:8,Stress:128.0f},Source:[I;2,72,-164],Speed:16.0f}");
        scene.idle(2);
        // JSON steps[25] cogwheel @ (4,2,4)
        PonderSceneSupport.ensureSceneCanShowRange(scene, new BlockPos(4, 2, 4));
        scene.world().setBlock(new BlockPos(4, 2, 4), AllBlocks.COGWHEEL.getDefaultState()
            .setValue(BlockStateProperties.AXIS, Direction.Axis.Z), true);
        PonderSceneSupport.mergeNbt(scene, util.select().position(4, 2, 4), true,
            "{ConnectedToExtraKinetics:0b,Network:{Capacity:262144.0f,Id:824633061447L,Size:8,Stress:128.0f},Source:[I;2,72,-163],Speed:16.0f}");
        scene.idle(2);
        // JSON steps[27] cogwheel @ (4,1,5)  ← ⚠ 批 C 订正的三格之一（z=5 越界）
        PonderSceneSupport.ensureSceneCanShowRange(scene, new BlockPos(4, 1, 5));
        scene.world().setBlock(new BlockPos(4, 1, 5), AllBlocks.COGWHEEL.getDefaultState()
            .setValue(BlockStateProperties.AXIS, Direction.Axis.Z), true);
        PonderSceneSupport.mergeNbt(scene, util.select().position(4, 1, 5), true,
            "{ConnectedToExtraKinetics:0b,Network:{Capacity:262144.0f,Id:1099510964294L,Size:8,Stress:128.0f},Source:[I;3,70,-163],Speed:-32.0f}");
        scene.idle(2);
        // JSON steps[29] cogwheel @ (4,2,5)  ← ⚠ 批 C 订正的三格之二
        PonderSceneSupport.ensureSceneCanShowRange(scene, new BlockPos(4, 2, 5));
        scene.world().setBlock(new BlockPos(4, 2, 5), AllBlocks.COGWHEEL.getDefaultState()
            .setValue(BlockStateProperties.AXIS, Direction.Axis.Z), true);
        PonderSceneSupport.mergeNbt(scene, util.select().position(4, 2, 5), true,
            "{ConnectedToExtraKinetics:0b,Network:{Capacity:262144.0f,Id:824633061447L,Size:8,Stress:128.0f},Source:[I;2,71,-163],Speed:16.0f}");
        scene.idle(2);
        // JSON steps[31] large_cogwheel @ (3,0,5)  ← ⚠ 批 C 订正的三格之三
        PonderSceneSupport.ensureSceneCanShowRange(scene, new BlockPos(3, 0, 5));
        scene.world().setBlock(new BlockPos(3, 0, 5), AllBlocks.LARGE_COGWHEEL.getDefaultState()
            .setValue(BlockStateProperties.AXIS, Direction.Axis.Z), true);
        PonderSceneSupport.mergeNbt(scene, util.select().position(3, 0, 5), true,
            "{ConnectedToExtraKinetics:0b,Network:{Capacity:262144.0f,Id:1099510964294L,Size:8,Stress:128.0f},Source:[I;3,70,-162],Speed:16.0f}");
        scene.idle(2);

        // ========================================================================
        // ==== 对应 JSON scene_1 steps[33..36]（set_block 转速控制器，anim=down/dur=20 → 机器 nbt → text#6 → idle 70）====  【重写 + 照搬】
        // 原 JSON：applyAnimatedSetBlock(…, entranceAnimation="down", entranceDuration=20)
        //            = erase 该格 → setBlocks(…, false) → applySetBlockNbtPatch（**有** nbt）
        //              → DisplayWorldSectionInstruction(20, DOWN, 单组, null)
        //          → modify_block_entity_nbt{facing:east, open:false, 机器 nbt（Stress 由 128 变 512、Speed 0→16）}
        //          → text#6 → idle 70
        // 现在   ：erase → setBlock(..., false) → mergeNbt(…, **true**) → showIndependentSection(sel, DOWN) → idle(5)
        //          → modifyBlock(OPEN=false) → mergeNbt(机器, false) → showText → idle(70)
        // 为什么 ：与 ② 同一条重写（官方 showIndependentSection 不擦除、且固定 15 tick）。
        // ⚠ 顺序：nbt 补丁排在 showIndependentSection **之前** —— 与 Ponderer 逐字同序
        //          （erase :1382 → setBlocks :1384 → nbtPatch :1385 → 滑入 :1396）。
        //          ⚠ 批 C 订正：这一条**不是**硬约束（两者都不是 blocking 指令，同一 tick 内先后都跑完，
        //            且 nbt 改的是 BE 内部状态、与淡入指令无关）；保留 Ponderer 的原序只为逐字对应。
        // ⚠ 本处 set_block 的 nbt 是**真需要**的（控制器自己也要 Source/Speed/ScrollValue），
        //          所以不能像 ② 那样省掉。它的第 4 参是 true（:1437 硬编码）。
        // ========================================================================
        Selection speedController = util.select().position(SPEED_CONTROLLER_POS);
        scene.addInstruction(ps -> {                              // 重写：补 Ponderer 的去重 erase（DPP:1409）
            ps.getBaseWorldSection().erase(speedController);
            ps.getBaseWorldSection().queueRedraw();
        });
        scene.world().setBlock(SPEED_CONTROLLER_POS, AllBlocks.ROTATION_SPEED_CONTROLLER.getDefaultState()
            .setValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS, Direction.Axis.X), false);   // 照搬：DPP:1384
        PonderSceneSupport.mergeNbt(scene, speedController, true,
            "{ConnectedToExtraKinetics:0b,HasAttachedComputer:0b,Network:{Capacity:262144.0f,Id:824633061447L,Size:8,Stress:128.0f},Owner:[I;940439953,-167562164,-1601161573,-1389718966],ScrollValue:16,Source:[I;2,72,-165],Speed:16.0f}");   // JSON steps[33] 的 nbt 逐字
        scene.world().showIndependentSection(speedController, Direction.DOWN);   // 重写：官方滑入（15 tick，原为 20）
        scene.idle(5);                                            // 重写：把 20-15=5 tick 补回来
        scene.world().modifyBlock(MACHINE_POS, s -> s.setValue(PowerAngleGrinderBlock.OPEN, false), false);
        PonderSceneSupport.mergeNbt(scene, machine, false,
            "{ConnectedToExtraKinetics:0b,Filter:{},FilterAmount:64,Inventory:{AppliedRecipe:0b,Items:[],ProcessingTime:-1.0f,RecipeTime:0.0f,Size:32},Network:{AddedStress:8.0f,Capacity:262144.0f,Id:1099510964294L,Size:8,Stress:128.0f},RecipeIndex:0,Source:[I;2,71,-165],Speed:16.0f,UpTo:1b,Wheel:\"createoreexpansion:iron_grinding_wheel\"}");   // JSON steps[34] 的 nbt 逐字
        scene.overlay()
            .showText(60)
            .text(PowerAngleGrinderPonderScenes.INSTALL_AND_RUN_TEXT_6)   // text_6 ← JSON steps[35]
            .pointAt(util.vector().of(3, 2, 3))
            .placeNearTarget();
        scene.idle(70);

        // ========================================================================
        // ==== 对应 JSON scene_1 steps[37..48]（提转速 → text#7 → 开盖 → 投料、再合盖）====  【照搬】
        // 原 JSON：modify_block_entity_nbt（机器：Speed 16→64、Stress 128→512）
        //          → modify_block_entity_nbt（转速控制器：ScrollValue 16→64、Speed 16→64）
        //          → text#7 → idle 70
        //          → show_controls(wrench) → idle 3 → modify(机器 open=true) → idle 70
        //          → show_controls(raw_sapphire) → idle 3 → modify(机器 open=true 且 Items 里塞进 1 个粗蓝宝石)
        // ⚠ 转速控制器那一步的 blockProperties 是 {axis:x} —— steps[33] 已经设过，同 no-op（见 ④ 的说明）。
        // ========================================================================
        PonderSceneSupport.mergeNbt(scene, machine, false,
            "{ConnectedToExtraKinetics:0b,Filter:{},FilterAmount:64,Inventory:{AppliedRecipe:0b,Items:[],ProcessingTime:-1.0f,RecipeTime:0.0f,Size:32},Network:{AddedStress:8.0f,Capacity:262144.0f,Id:1099510964294L,Size:8,Stress:512.0f},RecipeIndex:0,Source:[I;2,71,-165],Speed:64.0f,UpTo:1b,Wheel:\"createoreexpansion:iron_grinding_wheel\"}");   // JSON steps[37] 的 nbt 逐字
        PonderSceneSupport.mergeNbt(scene, speedController, false,
            "{ConnectedToExtraKinetics:0b,HasAttachedComputer:0b,Network:{Capacity:262144.0f,Id:1099510964294L,Size:8,Stress:512.0f},ScrollValue:64,Source:[I;2,72,-165],Speed:64.0f}");   // JSON steps[38] 的 nbt 逐字
        scene.overlay()
            .showText(60)
            .text(PowerAngleGrinderPonderScenes.INSTALL_AND_RUN_TEXT_7)   // text_7 ← JSON steps[39]
            .pointAt(util.vector().of(3, 2, 3))
            .placeNearTarget();
        scene.idle(70);
        scene.overlay()
            .showControls(util.vector().of(3, 3, 3), Pointing.DOWN, 60)
            .rightClick()
            .withItem(AllItems.WRENCH.asStack());
        scene.idle(3);
        scene.world().modifyBlock(MACHINE_POS, s -> s.setValue(PowerAngleGrinderBlock.OPEN, true), false);
        PonderSceneSupport.mergeNbt(scene, machine, false,
            "{ConnectedToExtraKinetics:0b,Filter:{},FilterAmount:64,Inventory:{AppliedRecipe:0b,Items:[],ProcessingTime:-1.0f,RecipeTime:0.0f,Size:32},Network:{AddedStress:8.0f,Capacity:262144.0f,Id:1099510964294L,Size:8,Stress:512.0f},RecipeIndex:0,Source:[I;2,71,-165],Speed:64.0f,UpTo:1b,Wheel:\"createoreexpansion:iron_grinding_wheel\"}");   // JSON steps[43] 的 nbt 逐字（与 steps[37] 逐字相同）
        scene.idle(70);
        scene.overlay()
            .showControls(util.vector().of(3, 3, 3), Pointing.DOWN, 60)
            .rightClick()
            .withItem(CoeItems.RAW_SAPPHIRE.asStack());
        scene.idle(3);
        scene.world().modifyBlock(MACHINE_POS, s -> s.setValue(PowerAngleGrinderBlock.OPEN, true), false);
        PonderSceneSupport.mergeNbt(scene, machine, false,
            "{ConnectedToExtraKinetics:0b,Filter:{},FilterAmount:64,Inventory:{AppliedRecipe:0b,Items:[{Slot:0,count:1,id:\"createoreexpansion:raw_sapphire\"}],ProcessingTime:-1.0f,RecipeTime:0.0f,Size:32},Network:{AddedStress:8.0f,Capacity:262144.0f,Id:1099510964294L,Size:8,Stress:512.0f},RecipeIndex:0,Source:[I;2,71,-165],Speed:64.0f,UpTo:1b,Wheel:\"createoreexpansion:iron_grinding_wheel\"}");   // JSON steps[47] 的 nbt 逐字
        scene.idle(70);                                           // 照搬：JSON steps[48]（⚠ 本处最初漏掉过，靠 batchB-coverage.mjs 的 idle 逐值序列比对抓出来）

        // ========================================================================
        // ==== 对应 JSON scene_1 steps[49..52]（show_controls(wrench) → idle 3 → 合盖 nbt → idle 70）====  【照搬】
        // 原 JSON：show_controls(create:wrench) → idle 3
        //          → modify_block_entity_nbt{open:false, 机器 nbt（ProcessingTime 103.99989f / RecipeTime 200.0f = 加工中）}
        //          → idle 70
        // ========================================================================
        scene.overlay()
            .showControls(util.vector().of(3, 3, 3), Pointing.DOWN, 60)
            .rightClick()
            .withItem(AllItems.WRENCH.asStack());
        scene.idle(3);
        scene.world().modifyBlock(MACHINE_POS, s -> s.setValue(PowerAngleGrinderBlock.OPEN, false), false);
        PonderSceneSupport.mergeNbt(scene, machine, false,
            "{ConnectedToExtraKinetics:0b,Filter:{},FilterAmount:64,Inventory:{AppliedRecipe:0b,Items:[{Slot:0,count:1,id:\"createoreexpansion:raw_sapphire\"}],ProcessingTime:103.99989f,RecipeTime:200.0f,Size:32},Network:{AddedStress:8.0f,Capacity:262144.0f,Id:1099510964294L,Size:8,Stress:512.0f},RecipeIndex:0,Source:[I;2,71,-165],Speed:64.0f,UpTo:1b,Wheel:\"createoreexpansion:iron_grinding_wheel\"}");   // JSON steps[51] 的 nbt 逐字
        scene.idle(70);

        // ========================================================================
        // ==== 对应 JSON scene_1 steps[53..58]（text#8 → idle 70 → show_controls(shard) → idle 3 → 取货 nbt → idle 70）====  【照搬】
        // 原 JSON：text#8 → idle 70 → show_controls(createoreexpansion:sapphire_small_shard)
        //          → idle 3 → modify_block_entity_nbt{open:false, Items 清空、RecipeTime 20.0f} → idle 70
        // ========================================================================
        scene.overlay()
            .showText(60)
            .text(PowerAngleGrinderPonderScenes.INSTALL_AND_RUN_TEXT_8)   // text_8 ← JSON steps[53]
            .pointAt(util.vector().of(3, 2, 3))
            .placeNearTarget();
        scene.idle(70);
        scene.overlay()
            .showControls(util.vector().of(3, 3, 3), Pointing.DOWN, 60)
            .rightClick()
            .withItem(CoeItems.SAPPHIRE_SMALL_SHARD.asStack());
        scene.idle(3);
        PonderSceneSupport.mergeNbt(scene, machine, false,
            "{ConnectedToExtraKinetics:0b,Filter:{},FilterAmount:64,Inventory:{AppliedRecipe:0b,Items:[],ProcessingTime:-1.0f,RecipeTime:20.0f,Size:32},Network:{AddedStress:8.0f,Capacity:262144.0f,Id:1099510964294L,Size:8,Stress:512.0f},RecipeIndex:0,Source:[I;2,71,-165],Speed:64.0f,UpTo:1b,Wheel:\"createoreexpansion:iron_grinding_wheel\"}");   // JSON steps[57] 的 nbt 逐字
        scene.idle(70);

        // ========================================================================
        // ==== 对应 JSON scene_1 steps[59..73]（text#9 → 漏斗滑入 → 掉落物 → 清掉落物 + 抽入动画 → 收货）====  【重写 + 照搬】
        // 原 JSON：text#9 → idle 70
        //          → set_block 安山漏斗 @[3,1,2]（_anim=down/dur=20_，facing=north / extracting=false，
        //            blockProperties 另带 waterlogged=false / powered=false；nbt={Filter:{},FilterAmount:64,TransferCooldown:0,UpTo:1b}）
        //          → idle 20 → create_item_entity(raw_sapphire @[3,1,2], motion=[0,0,0], count=1) → idle 10
        //          → clear_item_entities(blockPos=[3,1,2])                ← 作者追加的抽入动画就钉在这一步的紧后
        //          → modify_block_entity_nbt（漏斗 nbt 再合一次）→ idle 10
        //          → modify_block_entity_nbt（机器：Items 里出现 1 个粗蓝宝石 = 漏斗把它送进去了）→ idle 60
        //          → show_controls(shard) → idle 3 → modify_block_entity_nbt（Items 清空）→ idle 60
        // 现在   ：①漏斗滑入 = 与 ② 同一条重写（erase → setBlock(...,false) → mergeNbt(…,true) →
        //            showIndependentSection(sel, DOWN) → idle(5)）；blockProperties 里 waterlogged/powered
        //            本来就是默认值、facing/extracting 就是 set_block 给的值 ⇒ 全 no-op，不重复发指令。
        //          ②create_item_entity 换成官方便捷方法 createItemEntity(pos, motion, stack)
        //            （WorldInstructions.java:80 / PonderSceneBuilder.java:617-623）——
        //            原 JSON 走的是更底层的 createEntity(工厂)（DynamicPonderPlugin.java:737-744），
        //            两者在这里**语义等价**：4 处都没有 nbt 字段，motion/count 都被便捷方法原样覆盖；
        //            坐标也逐字照抄（Ponderer 的 toPoint 就是 new Vec3(x,y,z)，**不**做格心换算，:2487-2489）。
        //          ③clear_item_entities 照搬：本 JSON 4 处都没有 linkId/fullScene/item 过滤 ⇒ 走
        //            DynamicPonderPlugin.java:1911 支 = modifyEntitiesInside(ItemEntity.class, **单格**选区, discard)。
        //            ⚠ 是 position(…) 而**不是** fromTo(…)。
        //          ④漏斗抽入动画：见下面那一大段注释。
        // ========================================================================
        scene.overlay()
            .showText(60)
            .text(PowerAngleGrinderPonderScenes.INSTALL_AND_RUN_TEXT_9)   // text_9 ← JSON steps[59]
            .pointAt(util.vector().of(3, 2, 3))
            .placeNearTarget();
        scene.idle(70);

        Selection funnel = util.select().position(FUNNEL_POS);
        scene.addInstruction(ps -> {                              // 重写：补 Ponderer 的去重 erase（DPP:1409）
            ps.getBaseWorldSection().erase(funnel);
            ps.getBaseWorldSection().queueRedraw();
        });
        scene.world().setBlock(FUNNEL_POS, AllBlocks.ANDESITE_FUNNEL.getDefaultState()
            .setValue(FunnelBlock.FACING, Direction.NORTH)
            .setValue(FunnelBlock.EXTRACTING, false), false);      // 照搬：DPP:1384（waterlogged/powered 本就是默认 false）
        PonderSceneSupport.mergeNbt(scene, funnel, true,
            "{Filter:{},FilterAmount:64,TransferCooldown:0,UpTo:1b}");   // JSON steps[61] 的 nbt 逐字
        scene.world().showIndependentSection(funnel, Direction.DOWN);    // 重写：官方滑入（15 tick，原为 20）
        scene.idle(5);                                            // 重写：把 20-15=5 tick 补回来
        scene.idle(20);                                           // 照搬：JSON steps[62]
        scene.world().createItemEntity(util.vector().of(3, 1, 2), util.vector().of(0, 0, 0),
            CoeItems.RAW_SAPPHIRE.asStack());                     // 重写：官方 createItemEntity（JSON steps[63]）
        scene.idle(10);                                           // 照搬：JSON steps[64]
        scene.world().modifyEntitiesInside(ItemEntity.class, funnel, Entity::discard);   // 照搬：JSON steps[65]（DPP:1911-1916）

        // ------------------------------------------------------------------------
        // ==== 作者 2026-10-10 追加要求：紧接 clear_item_entities 之后播「安山漏斗抽入物品」====
        // 原 JSON：**没有**这一步（作者在本批新加）。要求的原话是"紧接着这 4 个时刻的下一个时刻，
        //          立刻播放安山漏斗的抽入物品动画"，所以这里**不插 idle**、也不 addKeyframe，
        //          就是 clear_item_entities 的下一行。
        // 现在   ：scene.world().flapFunnel(new BlockPos(3,1,2), false)
        // 为什么 direction 参数是 **false**（三条独立证据，都指向"false = 向里吸"）：
        //   ① 形参名与实现：CreateSceneBuilder.java:259-261
        //        public void flapFunnel(BlockPos position, boolean outward) {
        //            modifyBlockEntity(position, FunnelBlockEntity.class, funnel -> funnel.flap(!outward));
        //        }
        //      ⇒ 传 false 才得到 flap(true)。
        //   ② flap(boolean) 的语义：FunnelBlockEntity.java:317-324 里 flap(true) ⇒
        //        flap.setValue(-1)（负值）；而"物品被塞进漏斗"这条最核心的触发链
        //        AbstractFunnelBlock.java:102-109 调的就是 flap(true) ⇒ flap(true) = 吸入。
        //   ③ 官方思索的用法：机械臂把物品放进漏斗的三处全写 false ——
        //        ArmScenes.java:235 / :351 / :371（对应 AllArmInteractionPointTypes.java:501-502 的 flap(true)）；
        //      另外 FunnelScenes.java:61 / :240 / :436 三处也是 false，它们都紧跟在
        //        removeItemsFromBelt(...)（= 把物品从传送带上吸走）后面。
        //      反之 FunnelScenes.java:204 / :387 / :399 用 true，那三处紧跟 createItemEntity / 提示语
        //        "extract … items"，是"吐出来"。
        //     （方案文档附录 A.1 里"官方漏斗思索里抽入就是 flapFunnel(pos, true)"的说法与上面三条都对不上，
        //       以源码为准；写进报告，未改文档。）
        // ⚠ 翻板**会自己摆回来**，不是定格 —— 这一点与方案附录 A.4 的结论相反，故把真实机制记在这里：
        //      Ponder 里"可见世界段内的方块实体"**是逐 tick 被调的**：
        //        PonderScene.java:299-306   public void tick() { … forEach(e -> e.tick(this)); }
        //        WorldSectionElementImpl.java:266-279   tick(PonderScene) → isVisible() 为真时才
        //              tickableBlockEntities.forEach(be -> be.getSecond().accept(scene.getWorld()));
        //        WorldSectionElementImpl.java:290-315   loadBEsIfMissing 对段内每个 BE 取
        //              ((EntityBlock) block).getTicker(...)（IBE.java:68-73 → SmartBlockEntityTicker）
        //              ⇒ SmartBlockEntity.tick()（SmartBlockEntity.java:74-86，**不**检查 virtualMode）。
        //      漏斗是经 showIndependentSection 进场的（在世界段里且可见）⇒ FunnelBlockEntity.tick()
        //      （FunnelBlockEntity.java:96-98）里的 flap.tickChaser() 每 tick 推进一次；
        //      LerpedFloat 从 ±1 按 5% 指数回落到目标 0（FunnelBlockEntity.java:370-374
        //      createChasingFlap = startWithValue(.25f).chase(0, .05f, EXP)），
        //      FlapStuffs.flapAngle（FlapStuffs.java:59-66）把它变成绕 X 轴的摆动并被渲染器读出
        //      （FunnelRenderer.java:39 / FunnelVisual.java:35,:42 都只读）。
        //      ⇒ 与真机、与 Create 官方思索的表现一致：吸一下、然后自己复位。
        //      （补一句给将来的读者：flapness 的幅值在 |flapness| = 1 处恰好为 0°，最大偏转在
        //        |flapness| ≈ 0.5 处 —— 所以"定格在完全偏转位"这种描述本身也不成立。）
        // ⚠ 本幕这一处是 4 处里唯一在本批可运行的一处；另外三处（第二幕 steps[27]/[39]/[50]）
        //    在 PowerAngleGrinderTierScenes 里按相对顺序预埋。
        // ------------------------------------------------------------------------
        scene.world().flapFunnel(FUNNEL_POS, false);   // ← 对应 JSON steps[65] clear_item_entities 的紧后（不加 idle）

        // ==== 继续照搬 JSON steps[66..73] ====
        // ⚠ steps[66] 的 blockProperties 是 {waterlogged:false,powered:false,facing:north,extracting:false}
        //   —— 四项与 steps[61] 的 set_block 逐字相同（也都是方块默认值）⇒ Ponderer 会再 setValue 一遍
        //   （:1507-1528），语义等于没变。这里**不**为它单发一条 modifyBlock：那会白占一个 tick、把
        //   steps[66]→[67] 的 10 tick 间隔撑成 11 tick。该 no-op 由这条注释承担说明责任。
        PonderSceneSupport.mergeNbt(scene, funnel, false,
            "{Filter:{},FilterAmount:64,TransferCooldown:0,UpTo:1b}");   // JSON steps[66] 的 nbt 逐字
        scene.idle(10);
        PonderSceneSupport.mergeNbt(scene, machine, false,
            "{ConnectedToExtraKinetics:0b,Filter:{},FilterAmount:64,Inventory:{AppliedRecipe:0b,Items:[{Slot:0,count:1,id:\"createoreexpansion:raw_sapphire\"}],ProcessingTime:103.99989f,RecipeTime:200.0f,Size:32},Network:{AddedStress:8.0f,Capacity:262144.0f,Id:1099510964294L,Size:8,Stress:512.0f},RecipeIndex:0,Source:[I;2,71,-165],Speed:64.0f,UpTo:1b,Wheel:\"createoreexpansion:iron_grinding_wheel\"}");   // JSON steps[68] 的 nbt 逐字（与 steps[51] 逐字相同）
        scene.idle(60);
        scene.overlay()
            .showControls(util.vector().of(3, 3, 3), Pointing.DOWN, 60)
            .rightClick()
            .withItem(CoeItems.SAPPHIRE_SMALL_SHARD.asStack());
        scene.idle(3);
        PonderSceneSupport.mergeNbt(scene, machine, false,
            "{ConnectedToExtraKinetics:0b,Filter:{},FilterAmount:64,Inventory:{AppliedRecipe:0b,Items:[],ProcessingTime:-1.0f,RecipeTime:20.0f,Size:32},Network:{AddedStress:8.0f,Capacity:262144.0f,Id:1099510964294L,Size:8,Stress:512.0f},RecipeIndex:0,Source:[I;2,71,-165],Speed:64.0f,UpTo:1b,Wheel:\"createoreexpansion:iron_grinding_wheel\"}");   // JSON steps[72] 的 nbt 逐字（与 steps[57] 逐字相同）
        scene.idle(60);                                           // 照搬：JSON steps[73]（本幕最后一步）

        scene.markAsFinished();
    }

    // ==========================================================================
    // ⚠ 批 C 订正：本类原有的两个私有 helper（mergeNbt / parseSnbt）已整体搬到
    //   {@link com.hjmmd_8.createoreexpansion.client.ponder.PonderSceneSupport}，
    //   因为第二幕（PowerAngleGrinderTierScenes）需要一模一样的实现。
    //   搬走的是**逐字原实现**，调用点只多了类名前缀，语义零变化。
    // ==========================================================================
}

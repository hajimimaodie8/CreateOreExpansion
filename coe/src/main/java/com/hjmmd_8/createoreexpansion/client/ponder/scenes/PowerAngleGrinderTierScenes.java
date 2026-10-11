package com.hjmmd_8.createoreexpansion.client.ponder.scenes;

import com.hjmmd_8.createoreexpansion.client.ponder.PonderSceneSupport;
import com.hjmmd_8.createoreexpansion.client.ponder.PowerAngleGrinderPonderScenes;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeItems;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeMachines;

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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * <b>第二幕：不同等级的角磨轮及其功能</b>
 * （storyboard path {@code power_angle_grinder/wheel_tiers}，结构
 * {@code assets/createoreexpansion/ponder/power_angle_grinder/wheel_tiers.nbt}）。
 *
 * <h2>一、来源</h2>
 * <p>Ponderer JSON {@code power_angle_grinder.json} 的 {@code scenes[1]}（Ponderer 自动生成的 id
 * {@code s_6cb3b}，共 <b>55 步</b>）。Ponderer 那串 id 不带任何语义，本模组按作者裁定改成正式名
 * {@link PowerAngleGrinderPonderScenes#WHEEL_TIERS}，并且与 {@code scene.title} 的第一参、
 * 结构资源路径三者逐字相同。</p>
 *
 * <h2>二、55 步 → 7 段（段号与方案 §4.5 的对照表逐字对应）</h2>
 * <table border="1">
 *   <caption>分段对照</caption>
 *   <tr><th>段</th><th>JSON {@code s_6cb3b} 步号</th><th>内容</th><th>照搬/重写</th></tr>
 *   <tr><td>①</td><td>steps[0..1]</td><td>底板亮相</td><td>照搬（⚠ 本幕<b>没有</b> {@code attachKeyFrame}）</td></tr>
 *   <tr><td>②</td><td>steps[2]</td><td>角磨床滑入</td><td><b>重写</b>（补 erase）</td></tr>
 *   <tr><td>③</td><td>steps[3]</td><td>关盖状态（写回 {@code Wheel}）</td><td>照搬</td></tr>
 *   <tr><td>④</td><td>steps[4..14]</td><td>搭动力（4 齿轮 + 转速控制器）</td><td>齿轮：照搬 + <b>补长包围盒</b>；控制器：<b>重写</b>（补 erase）</td></tr>
 *   <tr><td>⑤</td><td>steps[15..22]</td><td>补一块大齿轮 + 三级总述 ⌈text_1..3⌋</td><td>齿轮同上 / 文案照搬</td></tr>
 *   <tr><td>⑥</td><td>steps[23..34]</td><td>一级轮演示（漏斗投入 → 产出）⌈text_4 在下一段⌋</td><td>漏斗滑入 <b>重写</b> / 掉落物 <b>重写</b> / 其余照搬</td></tr>
 *   <tr><td>⑦</td><td>steps[35..45]</td><td>二级轮演示 ⌈text_4⌋</td><td>同上</td></tr>
 *   <tr><td>⑧</td><td>steps[46..54]</td><td>三级轮演示 ⌈text_5⌋</td><td>同上</td></tr>
 * </table>
 * <p>⚠ 本表段号与方案 §4.5 那张表的 ①–⑦ 是同一套；文档把"总述三段"与"补大齿轮"并成一段
 * （④+⑤），这里按代码里的实际边界拆开写。</p>
 *
 * <h2>三、⚠ 批 C 订正：乙类 {@code set_block} 之前必须补 {@code ensureSceneCanShowRange}</h2>
 * <p>本幕的 5 个齿轮（{@code steps[5]/[7]/[9]/[11]/[16]}）走 Ponderer 的乙类支
 * （{@code immediateDisplay=true} + {@code entranceAnimation="none"}），
 * 其中 {@code (4,1,5)} / {@code (4,2,5)} / {@code (3,0,5)} <b>三格在场景包围盒之外</b>
 * （结构 {@code basic.nbt} 的 {@code size=[5,10,5]} ⇒ 盒子是 (0,0,0)-(4,9,4)），
 * 不先长盒就会被 {@code ReplaceBlocksInstruction} 静默吞掉。机制与三层证据链写在
 * {@link PonderSceneSupport} 的类注释上；批 B 的第一幕有完全相同的三格，本批一并修。</p>
 *
 * <h2>四、本幕的关键帧（作者 2026-10-11 问的"节点"）</h2>
 * <p>JSON 里本幕的 5 条 {@code text} 步<b>全部</b>带 {@code attachKeyFrame=true}
 * （{@code steps[17]/[19]/[21]/[35]/[46]}）⇒ 代码里各自配一次 {@code scene.addKeyframe()}。
 * 与第一幕不同：本幕 {@code steps[0]} 的 {@code show_structure} <b>没有</b> {@code attachKeyFrame}，
 * 所以开场不补关键帧（照搬 JSON，不自行加戏）。</p>
 *
 * <h2>五、⚠ 只能进游戏验的部分</h2>
 * <p>底板是否出现、机器是否"只有一台"（不重叠）、文案编号是否错位、BE 的 {@code open}/{@code Wheel}/
 * {@code Items} 状态是否按 JSON 变化、漏斗翻板是否真的往内偏、z=5 那三格齿轮这次是否<b>真的出现</b>
 * —— 静态关卡一律看不见（{@code compileJava} / {@code runData} 都不构造 Ponder 场景、不跑渲染、不跑 BE tick）。</p>
 */
public final class PowerAngleGrinderTierScenes {

    /**
     * ⚠ 这里**没有**任何英文/中文文案 —— 那是硬要求（作者 2026-10-10）。
     * {@code scene.title(...)} 与 {@code .text(...)} 的 String 入参在运行时**不显示**
     * （只是 Ponder 编辑模式的默认值：{@code PonderSceneBuilder.java:141-143} 与
     * {@code TextWindowElement.java:78-81} → {@code PonderScene.java:404-410}；
     * 非编辑态一律走 {@code PonderLocalization.getSpecific} → {@code I18n.get(键)}），
     * 所以两处都传**键本身**，文案只写在根的两份 {@code LanguageProvider} 里。
     */

    /** 角磨床在场景里的唯一格（原 JSON 第二幕同样放在 {@code [3,1,3]}）。 */
    private static final BlockPos MACHINE_POS = new BlockPos(3, 1, 3);

    /** 转速控制器：原 JSON 第二幕 {@code steps[13]} 放在 {@code [4,1,3]}，与第一幕同一格。 */
    private static final BlockPos SPEED_CONTROLLER_POS = new BlockPos(4, 1, 3);

    /** 安山漏斗：原 JSON 第二幕 {@code steps[23]} 放在 {@code [3,1,2]}，与第一幕同一格。 */
    private static final BlockPos FUNNEL_POS = new BlockPos(3, 1, 2);

    private PowerAngleGrinderTierScenes() {}

    public static void wheelTiers(SceneBuilder builder, SceneBuildingUtil util) {
        // CreateSceneBuilder.java:61-63 —— 官方唯一升级路径；PonderSceneBuilder 的子类，
        // 所以 idle/overlay/addInstruction/title/markAsFinished 全部照旧可用，
        // 另加 Create 专有的 flapFunnel（CreateSceneBuilder.java:259-261）。
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        // ========================================================================
        // ==== 对应 JSON s_6cb3b steps[0..1]（show_structure → idle 20）====  【照搬】
        // 原 JSON：show_structure（无 blockPos ⇒ everywhere）→ idle 20
        //          ⚠ 该步**没有** attachKeyFrame（与第一幕 steps[0] 不同）⇒ 本段不补关键帧。
        // 现在   ：title(...) + showSection(util.select().everywhere(), Direction.UP) + idle(20)
        //          —— 与 DynamicPonderPlugin.java:1019/1023 逐字对应。底板同样来自 basic.nbt，
        //             所以不调 configureBasePlate/showBasePlate（保住作者现在看到的底板外观）。
        // ========================================================================
        scene.title(PowerAngleGrinderPonderScenes.WHEEL_TIERS,
            PowerAngleGrinderPonderScenes.WHEEL_TIERS_HEADER);
        scene.world().showSection(util.select().everywhere(), Direction.UP);
        scene.idle(20);

        // ========================================================================
        // ==== 对应 JSON s_6cb3b steps[2]（set_block 动力角磨床，entranceAnimation=north / duration=20）====  【重写】
        // 原 JSON：applyAnimatedSetBlock(DynamicPonderPlugin.java:1373) =
        //           ① ensureSceneCanShowRange(forceVisibleNow=false) → 从 base 世界段 erase 该格（:1407-1411）
        //           ② world.setBlocks(sel, state, false)  —— 方块立刻落地（:1384）
        //           ③ applySetBlockNbtPatch（本处无 nbt ⇒ 空转）
        //           ④ DisplayWorldSectionInstruction(20, NORTH, 单组, null)（:1665）
        // 现在   ：手写 erase instruction → setBlock(..., false) → showIndependentSection(sel, NORTH) → idle(5)
        // 为什么 ：官方 showIndependentSection（PonderSceneBuilder.java:445-450）用的**就是**同一个
        //           DisplayWorldSectionInstruction，但它 ① 固定 15 tick（原为 20）② **不擦除** base 世界段；
        //           而 show_structure 用的是 everywhere()（包围盒选区），机器一落地就已经被 base 段渲染出来了，
        //           不先擦就会变成"静止一台 + 滑入一台"两台重叠。
        //           erase 排在 setBlock **之前**（与 Ponderer 的 ①② 同序）：三条指令都不是 blocking 指令
        //           （PonderInstruction.isBlocking 缺省 false，只有 idle 挡后续）⇒ 同一 tick 内按顺序跑完。
        // 代价   ：淡入 20 → 15 tick，用 idle(5) 把 20-15 补回来对齐总时长。
        // ⚠ 为什么这里**不需要** ensureSceneCanShowRange：机器格 (3,1,3) 在包围盒 (0,0,0)-(4,9,4) 之内
        //         （Ponderer 那条指令在甲类支里只做 erase，长盒对本格是 no-op）。
        // ========================================================================
        Selection machine = util.select().position(MACHINE_POS);
        // 原 JSON blockProperties = {"facing":"east"}；OPEN 不写 —— PowerAngleGrinderBlock 的
        // registerDefaultState 已把 open 定为 false，与 JSON 一致。
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
        // ==== 对应 JSON s_6cb3b steps[3]（modify_block_entity_nbt：open=false + Wheel=铁轮）====  【照搬】
        // 原 JSON：blockProperties={facing:east, open:false} + nbt（190 B，含 Wheel:"createoreexpansion:iron_grinding_wheel"）
        //          —— 紧跟在 steps[2] 的滑入之后，**中间没有 idle**。
        // 现在   ：mergeNbt(machine, false, …)（第 4 参 false：本步没有 reDrawBlocks 字段，DPP:1854）
        // no-op  ：blockProperties 的 facing=east 与 steps[2] 同值、open=false 是方块默认值
        //          （Ponderer :1507-1528 会再 setValue 一遍）⇒ 语义等于没变，这里不重复发 modifyBlock。
        // ========================================================================
        PonderSceneSupport.mergeNbt(scene, machine, false,
            "{Filter:{},FilterAmount:64,Inventory:{AppliedRecipe:0b,Items:[],ProcessingTime:-1.0f,RecipeTime:0.0f,Size:32},RecipeIndex:0,Speed:0.0f,UpTo:1b,Wheel:\"createoreexpansion:iron_grinding_wheel\"}");   // JSON steps[3] 的 nbt 逐字

        // ========================================================================
        // ==== 对应 JSON s_6cb3b steps[4..14]（idle 2 ×5 + 4 个齿轮 + 转速控制器滑入 + 机器 nbt）====  【照搬 + 批 C 订正】
        // 原 JSON：4 个齿轮全部 immediateDisplay=true / entranceAnimation="none"/ spawnParticles 缺失
        //          ⇒ 走 DPP:1351 的乙类支：
        //            ① ensureSceneCanShowRange(…, forceVisibleNow=true)（:1404-1424）= 长盒 + 并回 base 段
        //            ② particles = immediateDisplay && !FALSE.equals(spawnParticles) ⇒ **粒子 = true**
        //          nbt 走 applySetBlockNbtPatch（:1427-1439）⇒ modifyBlockEntityNBT(…, **true**)。
        //          转速控制器是甲类（entranceAnimation="down"/duration=20/immediateDisplay=false）
        //          ⇒ erase → setBlocks(false) → nbtPatch → DisplayWorldSection(20, DOWN)。
        // 现在   ：齿轮 = ensureSceneCanShowRange(scene, pos) → setBlock(pos, state, true) → mergeNbt(…, true)
        //          控制器 = 与 ② 同一条重写 + mergeNbt(…, **true**)（本处 nbt 是真需要的：控制器自己也要
        //          Source/Speed/ScrollValue，所以不能像 ② 那样省掉；第 4 参 true 是 DPP:1437 硬编码）。
        // ⚠ 批 C 订正：齿轮的 ensureSceneCanShowRange 是**必需**的 —— (4,1,5)/(4,2,5)/(3,0,5) 三格
        //          在包围盒 (0,0,0)-(4,9,4) 之外（结构 basic.nbt 的 size=[5,10,5]），
        //          不先长盒就被 ReplaceBlocksInstruction.java:29-31 的 isInside 判断直接吞掉，
        //          同格的三条 mergeNbt 也连带成 no-op。详见 PonderSceneSupport 类注释。
        // ========================================================================
        scene.idle(2);                                            // 照搬：JSON steps[4]
        // JSON steps[5] cogwheel @ (4,2,4)（盒内；长盒调用照样保留 = 与 Ponderer 逐字同形）
        PonderSceneSupport.ensureSceneCanShowRange(scene, new BlockPos(4, 2, 4));
        scene.world().setBlock(new BlockPos(4, 2, 4), AllBlocks.COGWHEEL.getDefaultState()
            .setValue(BlockStateProperties.AXIS, Direction.Axis.Z), true);
        PonderSceneSupport.mergeNbt(scene, util.select().position(4, 2, 4), true,
            "{ConnectedToExtraKinetics:0b,Network:{Capacity:262144.0f,Id:824633061447L,Size:8,Stress:128.0f},Source:[I;2,72,-163],Speed:16.0f}");   // JSON steps[5] 的 nbt 逐字
        scene.idle(2);                                            // 照搬：JSON steps[6]
        // JSON steps[7] cogwheel @ (4,1,5)  ← ⚠ 越界格（z=5），批 C 订正
        PonderSceneSupport.ensureSceneCanShowRange(scene, new BlockPos(4, 1, 5));
        scene.world().setBlock(new BlockPos(4, 1, 5), AllBlocks.COGWHEEL.getDefaultState()
            .setValue(BlockStateProperties.AXIS, Direction.Axis.Z), true);
        PonderSceneSupport.mergeNbt(scene, util.select().position(4, 1, 5), true,
            "{ConnectedToExtraKinetics:0b,Network:{Capacity:262144.0f,Id:1099510964294L,Size:8,Stress:128.0f},Source:[I;3,70,-163],Speed:-32.0f}");   // JSON steps[7] 的 nbt 逐字
        scene.idle(2);                                            // 照搬：JSON steps[8]
        // JSON steps[9] cogwheel @ (4,2,5)  ← ⚠ 越界格（z=5），批 C 订正
        PonderSceneSupport.ensureSceneCanShowRange(scene, new BlockPos(4, 2, 5));
        scene.world().setBlock(new BlockPos(4, 2, 5), AllBlocks.COGWHEEL.getDefaultState()
            .setValue(BlockStateProperties.AXIS, Direction.Axis.Z), true);
        PonderSceneSupport.mergeNbt(scene, util.select().position(4, 2, 5), true,
            "{ConnectedToExtraKinetics:0b,Network:{Capacity:262144.0f,Id:824633061447L,Size:8,Stress:128.0f},Source:[I;2,71,-163],Speed:16.0f}");   // JSON steps[9] 的 nbt 逐字
        scene.idle(2);                                            // 照搬：JSON steps[10]
        // JSON steps[11] large_cogwheel @ (3,0,5)  ← ⚠ 越界格（z=5），批 C 订正
        PonderSceneSupport.ensureSceneCanShowRange(scene, new BlockPos(3, 0, 5));
        scene.world().setBlock(new BlockPos(3, 0, 5), AllBlocks.LARGE_COGWHEEL.getDefaultState()
            .setValue(BlockStateProperties.AXIS, Direction.Axis.Z), true);
        PonderSceneSupport.mergeNbt(scene, util.select().position(3, 0, 5), true,
            "{ConnectedToExtraKinetics:0b,Network:{Capacity:262144.0f,Id:1099510964294L,Size:8,Stress:128.0f},Source:[I;3,70,-162],Speed:16.0f}");   // JSON steps[11] 的 nbt 逐字
        scene.idle(2);                                            // 照搬：JSON steps[12]
        // JSON steps[13] set_block 转速控制器（甲类，anim=down / dur=20）
        Selection speedController = util.select().position(SPEED_CONTROLLER_POS);
        scene.addInstruction(ps -> {                              // 重写：补 Ponderer 的去重 erase（DPP:1409）
            ps.getBaseWorldSection().erase(speedController);
            ps.getBaseWorldSection().queueRedraw();
        });
        scene.world().setBlock(SPEED_CONTROLLER_POS, AllBlocks.ROTATION_SPEED_CONTROLLER.getDefaultState()
            .setValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS, Direction.Axis.X), false);   // 照搬：DPP:1384
        PonderSceneSupport.mergeNbt(scene, speedController, true,
            "{ConnectedToExtraKinetics:0b,HasAttachedComputer:0b,Network:{Capacity:262144.0f,Id:824633061447L,Size:8,Stress:128.0f},Owner:[I;940439953,-167562164,-1601161573,-1389718966],ScrollValue:16,Source:[I;2,72,-165],Speed:16.0f}");   // JSON steps[13] 的 nbt 逐字
        scene.world().showIndependentSection(speedController, Direction.DOWN);   // 重写：官方滑入（15 tick，原为 20）
        scene.idle(5);                                            // 重写：把 20-15=5 tick 补回来
        // JSON steps[14] 机器 nbt（325 B：Network 里出现 Id/Stress，open=false）
        PonderSceneSupport.mergeNbt(scene, machine, false,
            "{ConnectedToExtraKinetics:0b,Filter:{},FilterAmount:64,Inventory:{AppliedRecipe:0b,Items:[],ProcessingTime:-1.0f,RecipeTime:0.0f,Size:32},Network:{AddedStress:8.0f,Capacity:262144.0f,Id:1099510964294L,Size:8,Stress:128.0f},RecipeIndex:0,Source:[I;2,71,-165],Speed:16.0f,UpTo:1b,Wheel:\"createoreexpansion:iron_grinding_wheel\"}");   // JSON steps[14] 的 nbt 逐字

        // ========================================================================
        // ==== 对应 JSON s_6cb3b steps[15..22]（idle 5 → 补一块大齿轮 → 三级总述 text_1..3）====  【照搬 + 批 C 订正】
        // 原 JSON：idle 5 → set_block large_cogwheel @(4,2,3)（乙类，同上下）→ text#1（kf）/idle 65
        //          → text#2（kf）/idle 65 → text#3（kf）/idle 65
        // 现在   ：idle(5) → ensureSceneCanShowRange + setBlock(...,true) + mergeNbt(…,true)
        //          → 三次 [addKeyframe() + showText(60)…placeNearTarget() + idle(65)]
        // ⚠ 编号：这是本幕第 1..3 次 showText ⇒ lang 键 wheel_tiers.text_1 / _2 / _3。
        // ⚠ 三个关键帧都排在各自文案指令**之前**（照 DynamicPonderPlugin.java:492-494：对任何
        //    type 都先看 attachKeyFrame 并补一个关键帧）；JSON 这三步都带 attachKeyFrame=true。
        // ========================================================================
        scene.idle(5);                                            // 照搬：JSON steps[15]
        // JSON steps[16] large_cogwheel @ (4,2,3)（盒内；长盒调用照样保留 = 与 Ponderer 逐字同形）
        PonderSceneSupport.ensureSceneCanShowRange(scene, new BlockPos(4, 2, 3));
        scene.world().setBlock(new BlockPos(4, 2, 3), AllBlocks.LARGE_COGWHEEL.getDefaultState()
            .setValue(BlockStateProperties.AXIS, Direction.Axis.Z), true);
        PonderSceneSupport.mergeNbt(scene, util.select().position(4, 2, 3), true,
            "{ConnectedToExtraKinetics:0b,Network:{Capacity:262144.0f,Id:824633061447L,Size:8,Stress:128.0f},Source:[I;2,72,-164],Speed:16.0f}");   // JSON steps[16] 的 nbt 逐字
        scene.addKeyframe();                                      // JSON steps[17] 的 attachKeyFrame=true
        scene.overlay()
            .showText(60)
            .text(PowerAngleGrinderPonderScenes.WHEEL_TIERS_TEXT_1)   // text_1 ← JSON steps[17]
            .pointAt(util.vector().of(3, 2, 3))
            .placeNearTarget();
        scene.idle(65);                                           // 照搬：JSON steps[18]
        scene.addKeyframe();                                      // JSON steps[19] 的 attachKeyFrame=true
        scene.overlay()
            .showText(60)
            .text(PowerAngleGrinderPonderScenes.WHEEL_TIERS_TEXT_2)   // text_2 ← JSON steps[19]
            .pointAt(util.vector().of(3, 2, 3))
            .placeNearTarget();
        scene.idle(65);                                           // 照搬：JSON steps[20]
        scene.addKeyframe();                                      // JSON steps[21] 的 attachKeyFrame=true
        scene.overlay()
            .showText(60)
            .text(PowerAngleGrinderPonderScenes.WHEEL_TIERS_TEXT_3)   // text_3 ← JSON steps[21]
            .pointAt(util.vector().of(3, 2, 3))
            .placeNearTarget();
        scene.idle(65);                                           // 照搬：JSON steps[22]

        // ========================================================================
        // ==== 对应 JSON s_6cb3b steps[23..34]（漏斗滑入 → 投入 → 清除 + 抽入动画 → 产出 → 取出）====  【重写 + 照搬】
        // 原 JSON：set_block 安山漏斗 @[3,1,2]（甲类 anim=down/dur=20，facing=north / extracting=false，
        //            blockProperties 另带 waterlogged=false / powered=false；
        //            nbt={Filter:{},FilterAmount:64,TransferCooldown:0,UpTo:1b}）
        //          → idle 20 → create_item_entity(raw_sapphire @[3,1,2], motion=[0,0,0], count=1) → idle 10
        //          → clear_item_entities(blockPos=[3,1,2])          ← 作者追加的抽入动画钉在这一步的紧后
        //          → idle 10 → modify_block_entity_nbt（机器：Items 里 1 个粗蓝宝石 = 加工中）→ idle 60
        //          → show_controls(shard) → idle 65 → modify_block_entity_nbt（Wheel 换二级、Stress 768、Speed 96）→ idle 10
        // 现在   ：①漏斗滑入 = 与 ② 同一条重写（erase → setBlock(...,false) → mergeNbt(…,true) →
        //            showIndependentSection(sel, DOWN) → idle(5)）；blockProperties 里 waterlogged/powered
        //            本来就是默认值、facing/extracting 就是 set_block 给的值 ⇒ 全 no-op，不重复发指令。
        //          ②create_item_entity 换成官方便捷方法 createItemEntity(pos, motion, stack)
        //            （WorldInstructions.java:80 / PonderSceneBuilder.java:617-623）——
        //            原 JSON 走更底层的 createEntity(工厂)（DPP:737-744），两者在这里**语义等价**：
        //            本幕 3 处都没有 nbt 字段，motion/count 都被便捷方法原样覆盖；坐标也逐字照抄
        //            （Ponderer 的 toPoint 就是 new Vec3(x,y,z)，**不**做格心换算，:2487-2489）。
        //          ③clear_item_entities 照搬：本 JSON 4 处都没有 linkId/fullScene/item 过滤 ⇒ 走
        //            DPP:1911 支 = modifyEntitiesInside(ItemEntity.class, **单格**选区, discard)。
        //            ⚠ 是 position(…) 而**不是** fromTo(…)。
        //          ④漏斗抽入动画：flapFunnel(pos, false) 的完整证据链见第一幕 ⑬ 末尾那一大段注释。
        // ⚠ 漏斗格 (3,1,2) 在包围盒内 ⇒ 甲类这一处不需要长盒（与 Ponderer 的甲类支一致：那支只做 erase）。
        // ========================================================================
        Selection funnel = util.select().position(FUNNEL_POS);
        scene.addInstruction(ps -> {                              // 重写：补 Ponderer 的去重 erase（DPP:1409）
            ps.getBaseWorldSection().erase(funnel);
            ps.getBaseWorldSection().queueRedraw();
        });
        scene.world().setBlock(FUNNEL_POS, AllBlocks.ANDESITE_FUNNEL.getDefaultState()
            .setValue(FunnelBlock.FACING, Direction.NORTH)
            .setValue(FunnelBlock.EXTRACTING, false), false);      // 照搬：DPP:1384（waterlogged/powered 本就是默认 false）
        PonderSceneSupport.mergeNbt(scene, funnel, true,
            "{Filter:{},FilterAmount:64,TransferCooldown:0,UpTo:1b}");   // JSON steps[23] 的 nbt 逐字
        scene.world().showIndependentSection(funnel, Direction.DOWN);    // 重写：官方滑入（15 tick，原为 20）
        scene.idle(5);                                            // 重写：把 20-15=5 tick 补回来
        scene.idle(20);                                           // 照搬：JSON steps[24]
        scene.world().createItemEntity(util.vector().of(3, 1, 2), util.vector().of(0, 0, 0),
            CoeItems.RAW_SAPPHIRE.asStack());                     // 重写：官方 createItemEntity（JSON steps[25]）
        scene.idle(10);                                           // 照搬：JSON steps[26]
        scene.world().modifyEntitiesInside(ItemEntity.class, funnel, Entity::discard);   // 照搬：JSON steps[27]（DPP:1911-1916）
        scene.world().flapFunnel(FUNNEL_POS, false);   // ← 对应 s2[27] clear_item_entities 的紧后（不加 idle）
        scene.idle(10);                                           // 照搬：JSON steps[28]
        // JSON steps[29] 机器 nbt（385 B，与第一幕 steps[51] 逐字相同：加工中）
        PonderSceneSupport.mergeNbt(scene, machine, false,
            "{ConnectedToExtraKinetics:0b,Filter:{},FilterAmount:64,Inventory:{AppliedRecipe:0b,Items:[{Slot:0,count:1,id:\"createoreexpansion:raw_sapphire\"}],ProcessingTime:103.99989f,RecipeTime:200.0f,Size:32},Network:{AddedStress:8.0f,Capacity:262144.0f,Id:1099510964294L,Size:8,Stress:512.0f},RecipeIndex:0,Source:[I;2,71,-165],Speed:64.0f,UpTo:1b,Wheel:\"createoreexpansion:iron_grinding_wheel\"}");   // JSON steps[29] 的 nbt 逐字
        scene.idle(60);                                           // 照搬：JSON steps[30]
        scene.overlay()
            .showControls(util.vector().of(3, 3, 3), Pointing.DOWN, 60)
            .rightClick()
            .withItem(CoeItems.SAPPHIRE_SMALL_SHARD.asStack());    // 照搬：JSON steps[31]
        scene.idle(65);                                           // 照搬：JSON steps[32]
        // JSON steps[33] 机器 nbt（390 B：Wheel 升为钻石轮、Stress 768、Speed 96；成品区 2 个碎片）
        PonderSceneSupport.mergeNbt(scene, machine, false,
            "{ConnectedToExtraKinetics:0b,Filter:{},FilterAmount:64,Inventory:{AppliedRecipe:0b,Items:[{Slot:1,count:2,id:\"createoreexpansion:sapphire_small_shard\"}],ProcessingTime:-1.0f,RecipeTime:20.0f,Size:32},Network:{AddedStress:8.0f,Capacity:262144.0f,Id:1099510964294L,Size:8,Stress:768.0f},RecipeIndex:0,Source:[I;2,71,-165],Speed:96.0f,UpTo:1b,Wheel:\"createoreexpansion:diamond_grinding_wheel\"}");   // JSON steps[33] 的 nbt 逐字
        scene.idle(10);                                           // 照搬：JSON steps[34]

        // ========================================================================
        // ==== 对应 JSON s_6cb3b steps[35..45]（二级轮演示：text_4 → 投入下界岩 → 清除 + 抽入动画 → 产出）====  【照搬 + 重写】
        // 原 JSON：text#4（kf, duration 60）→ idle 65
        //          → create_item_entity(netherrack @[3,1,2]) → idle 10
        //          → clear_item_entities([3,1,2])                   ← 抽入动画钉在这一步的紧后
        //          → modify nbt（413 B：输入下界岩 + 产出 cinder_flour）→ idle 45
        //          → show_controls(create:cinder_flour) → idle 65
        //          → modify nbt（330 B：Wheel 升为蓝宝石轮）→ idle 10
        // ⚠ 编号：text#4 ⇒ lang 键 wheel_tiers.text_4。
        // ========================================================================
        scene.addKeyframe();                                      // JSON steps[35] 的 attachKeyFrame=true
        scene.overlay()
            .showText(60)
            .text(PowerAngleGrinderPonderScenes.WHEEL_TIERS_TEXT_4)   // text_4 ← JSON steps[35]
            .pointAt(util.vector().of(3, 2, 3))
            .placeNearTarget();
        scene.idle(65);                                           // 照搬：JSON steps[36]
        scene.world().createItemEntity(util.vector().of(3, 1, 2), util.vector().of(0, 0, 0),
            Items.NETHERRACK.getDefaultInstance());               // 重写：官方 createItemEntity（JSON steps[37]）
        scene.idle(10);                                           // 照搬：JSON steps[38]
        scene.world().modifyEntitiesInside(ItemEntity.class, funnel, Entity::discard);   // 照搬：JSON steps[39]（DPP:1911-1916）
        scene.world().flapFunnel(FUNNEL_POS, false);   // ← 对应 s2[39] clear_item_entities 的紧后（不加 idle）
        // JSON steps[40] 机器 nbt（413 B：下界岩 → 焦炭粉，加工中）
        PonderSceneSupport.mergeNbt(scene, machine, false,
            "{ConnectedToExtraKinetics:0b,Filter:{},FilterAmount:64,Inventory:{AppliedRecipe:0b,Items:[{Slot:0,count:1,id:\"minecraft:netherrack\"},{Slot:1,count:1,id:\"create:cinder_flour\"}],ProcessingTime:61.0f,RecipeTime:85.0f,Size:32},Network:{AddedStress:8.0f,Capacity:262144.0f,Id:1099510964294L,Size:8,Stress:768.0f},RecipeIndex:0,Source:[I;2,71,-165],Speed:96.0f,UpTo:1b,Wheel:\"createoreexpansion:diamond_grinding_wheel\"}");   // JSON steps[40] 的 nbt 逐字
        scene.idle(45);                                           // 照搬：JSON steps[41]
        scene.overlay()
            .showControls(util.vector().of(3, 3, 3), Pointing.DOWN, 60)
            .rightClick()
            .withItem(AllItems.CINDER_FLOUR.asStack());            // 照搬：JSON steps[42]
        scene.idle(65);                                           // 照搬：JSON steps[43]
        // JSON steps[44] 机器 nbt（330 B：Wheel 升为蓝宝石轮、成品区清空）
        PonderSceneSupport.mergeNbt(scene, machine, false,
            "{ConnectedToExtraKinetics:0b,Filter:{},FilterAmount:64,Inventory:{AppliedRecipe:0b,Items:[],ProcessingTime:-1.0f,RecipeTime:20.0f,Size:32},Network:{AddedStress:8.0f,Capacity:262144.0f,Id:1099510964294L,Size:8,Stress:768.0f},RecipeIndex:0,Source:[I;2,71,-165],Speed:96.0f,UpTo:1b,Wheel:\"createoreexpansion:sapphire_grinding_wheel\"}");   // JSON steps[44] 的 nbt 逐字
        scene.idle(10);                                           // 照搬：JSON steps[45]

        // ========================================================================
        // ==== 对应 JSON s_6cb3b steps[46..54]（三级轮演示：text_5 → 投入蓝宝石剑 → 清除 + 抽入动画 → 拆解产出）====  【照搬 + 重写】
        // 原 JSON：text#5（kf, duration 60）→ idle 65
        //          → create_item_entity(createoreexpansion:sapphire_sword @[3,1,2]) → idle 10
        //          → clear_item_entities([3,1,2])                   ← 抽入动画钉在这一步的紧后
        //          → modify nbt（392 B：拆磨配方，AppliedRecipe=1b，产出 4 个蓝宝石锭）→ idle 45
        //          → show_controls(createoreexpansion:sapphire_ingot) → idle 65（本幕最后一步）
        // ⚠ 编号：text#5 ⇒ lang 键 wheel_tiers.text_5。
        // ========================================================================
        scene.addKeyframe();                                      // JSON steps[46] 的 attachKeyFrame=true
        scene.overlay()
            .showText(60)
            .text(PowerAngleGrinderPonderScenes.WHEEL_TIERS_TEXT_5)   // text_5 ← JSON steps[46]
            .pointAt(util.vector().of(3, 2, 3))
            .placeNearTarget();
        scene.idle(65);                                           // 照搬：JSON steps[47]
        scene.world().createItemEntity(util.vector().of(3, 1, 2), util.vector().of(0, 0, 0),
            CoeItems.SAPPHIRE_SWORD.asStack());                   // 重写：官方 createItemEntity（JSON steps[48]）
        scene.idle(10);                                           // 照搬：JSON steps[49]
        scene.world().modifyEntitiesInside(ItemEntity.class, funnel, Entity::discard);   // 照搬：JSON steps[50]（DPP:1911-1916）
        scene.world().flapFunnel(FUNNEL_POS, false);   // ← 对应 s2[50] clear_item_entities 的紧后（不加 idle）
        // JSON steps[51] 机器 nbt（392 B：拆磨产物 4 个蓝宝石锭、Stress 1024、Speed 128）
        PonderSceneSupport.mergeNbt(scene, machine, false,
            "{ConnectedToExtraKinetics:0b,Filter:{},FilterAmount:64,Inventory:{AppliedRecipe:1b,Items:[{Slot:1,count:4,id:\"createoreexpansion:sapphire_ingot\"}],ProcessingTime:14.666666f,RecipeTime:20.0f,Size:32},Network:{AddedStress:8.0f,Capacity:262144.0f,Id:1099510964294L,Size:8,Stress:1024.0f},RecipeIndex:0,Source:[I;2,71,-165],Speed:128.0f,UpTo:1b,Wheel:\"createoreexpansion:sapphire_grinding_wheel\"}");   // JSON steps[51] 的 nbt 逐字
        scene.idle(45);                                           // 照搬：JSON steps[52]
        scene.overlay()
            .showControls(util.vector().of(3, 3, 3), Pointing.DOWN, 60)
            .rightClick()
            .withItem(CoeItems.SAPPHIRE_INGOT.asStack());          // 照搬：JSON steps[53]
        scene.idle(65);                                           // 照搬：JSON steps[54]（本幕最后一步）

        scene.markAsFinished();
    }
}

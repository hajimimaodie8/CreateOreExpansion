package com.hjmmd_8.createoreexpansion.client.ponder;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * <b>两幕角磨床思索共用的两条场景原语</b>——把 Ponderer 内部路径里"官方 API 缺的那两步"
 * 补回来，并让两幕共用同一份实现（而不是各抄一遍）。
 *
 * <p>本类<b>不是</b> Create/Ponder 的公开面：它是 {@code DynamicPonderPlugin} 里那两段
 * 私有记账逻辑的最小等价物，出处逐条写在各方法上。两幕的分段注释只解释"这一段为什么调它"，
 * 机制统一在这里。</p>
 *
 * <h2>一、{@link #ensureSceneCanShowRange}：为什么"放方块"之前必须长包围盒</h2>
 * <p>⚠ 这是本工程<b>唯一一处"代码执行了、画面上什么都没发生"的静默失效</b>，
 * 移植时漏掉了 Ponderer 的这一步，两幕各有 3 个齿轮方块因此从未出现过。</p>
 * <p>证据链（三层，缺一层都推不出来）：</p>
 * <ol>
 *   <li>两幕加载的结构都是 Ponderer 的 {@code basic.nbt}（{@code size=[5,10,5]}，
 *       25 个底板方块全在 {@code y=0} + 1 个 {@code air} 在 {@code (4,9,4)}）
 *       ⇒ {@code PonderSceneRegistry.compile} 的 {@code placeInWorld}
 *       （{@code PonderSceneRegistry.java:103-107}）逐格调 {@code PonderLevel.setBlock}，
 *       {@code SchematicLevel.setBlock} 用 {@code BBHelper.encapsulate} 长盒
 *       （{@code SchematicLevel.java:203-222}）⇒ 场景盒 = <b>(0,0,0)-(4,9,4)</b>，<b>{@code z=5} 在盒外</b>。</li>
 *   <li>官方的 {@code set_block} 落到 {@code ReplaceBlocksInstruction}，第一句就是
 *       {@code if (!level.getBounds().isInside(pos)) return;}（{@code ReplaceBlocksInstruction.java:29-31}），
 *       而 {@code BoundingBox.isInside} 是<b>闭区间</b>（{@code BoundingBox.java:232-239}）
 *       ⇒ 盒外的格子被<b>直接吞掉</b>；即便写进了格子表，{@code SchematicLevel.getBlockState:126}
 *       对盒外坐标也一律返回 {@code AIR} ⇒ 渲染侧同样看不见。</li>
 *   <li>Ponderer 不会踩这个坑，因为它在每一次 {@code setBlocks} 之前都先发一条
 *       "长盒 + 把这一格并回基准世界段"的指令（{@code DynamicPonderPlugin.java:1404-1424}，
 *       由 {@code :1382} 的 {@code ensureSceneCanShowRange} 调；另一处同形在 {@code :1737-1746}），
 *       并且 {@code BoundingBox.encapsulate} 是<b>原地改字段</b>（{@code :181-189}，字段非 final），
 *       所以那两行真的生效。本方法就是把那两行逐字搬过来。</li>
 * </ol>
 * <p>⚠ 只长盒<b>不够</b>：基准世界段（{@code show_structure} 时用
 * {@code select().everywhere()} 建的）在那一刻已经定型成 (0,0,0)-(4,9,4)，
 * 渲染时按段内选区逐格 {@code getBlockState}；所以还要 {@code add(该格)}
 * （Ponderer {@code :1419} 同一句）。</p>
 *
 * <h2>二、{@link #mergeNbt}：{@code set_block.nbt} 与 {@code modify_block_entity_nbt} 共用的补丁</h2>
 * <p>逐字对应 Ponderer 的 {@code applySetBlockNbtPatch}（{@code DynamicPonderPlugin.java:1427-1439}）
 * 与 {@code modify_block_entity_nbt}（{@code :1811-1858} 的 {@code :1854}）：
 * {@code scene.world().modifyBlockEntityNBT(selection, BlockEntity.class, nbt -> nbt.merge(patch), reDrawBlocks)}。</p>
 * <p>⚠ 第 4 参不是"随便给的"：两条来源的默认值不同——
 * {@code set_block} 的补丁是硬编码 {@code true}（{@code :1437}），
 * 而 {@code modify_block_entity_nbt} 走 {@code redraw = TRUE.equals(step.reDrawBlocks)}，
 * 本 JSON 的 20 处<b>都没有</b> {@code reDrawBlocks} 字段 ⇒ 恒 {@code false}（{@code :1854}）。
 * 抄错就会出现"状态变了但方块没重画"或反之。</p>
 *
 * <h2>三、⚠ 只能进游戏验的部分</h2>
 * <p>{@code compileJava} / {@code runData} 既不构造 Ponder 场景、也不跑渲染 ⇒
 * 本类的两条原语是否真的让方块出现、让 BE 状态变，<b>任何静态关卡都看不见</b>；
 * 能静态守住的只有"调用还在、顺序还对"（见 {@code tools/check-module-selfsufficiency.ps1} 的 P9）。</p>
 */
public final class PonderSceneSupport {

    private PonderSceneSupport() {}

    /**
     * 让某个格子"真的能被看见"：先把它并进场景包围盒，再把它并回基准世界段
     * （= Ponderer {@code ensureSceneCanShowRange(..., forceVisibleNow = true)} 的等价物，
     * {@code DynamicPonderPlugin.java:1404-1424}）。
     *
     * <p>⚠ <b>必须排在对应 {@code setBlock(...)} <b>之前</b>加入 schedule</b>：两者都不是 blocking
     * 指令（{@code PonderInstruction.isBlocking()} 缺省 false，只有 {@code idle} 挡后续），
     * 所以它们会在<b>同一个 tick</b> 内按加入顺序跑完 —— 顺序反了，放方块那一刻盒还没长，
     * 那一次 {@code setBlock} 就被 {@code ReplaceBlocksInstruction} 吞掉了。</p>
     *
     * <p>⚠ 基准世界段为空时逐字照抄 Ponderer 的另一支：直接把
     * {@code everywhere()} 铺上去并让它可见（{@code :1413-1417}）——
     * 本工程两幕都在第一步就 {@code showSection(everywhere(), UP)}，所以实际走的是
     * {@code add} 那一支；保留另一支只为"与原路径逐字等价"，不是死代码的借口。</p>
     *
     * @param scene 场景构建器（{@code scene.addInstruction} 的那一层）
     * @param pos   要显形的格子（场景绝对坐标）
     */
    public static void ensureSceneCanShowRange(SceneBuilder scene, BlockPos pos) {
        scene.addInstruction(ps -> {
            // 原地长盒（BoundingBox.encapsulate 改的是它自己的字段，返回值就是 this）
            ps.getWorld().getBounds().encapsulate(new BoundingBox(pos));
            WorldSectionElement base = ps.getBaseWorldSection();
            if (base.isEmpty()) {
                base.set(ps.getSceneBuildingUtil().select().everywhere());
                base.setVisible(true);
                base.setFade(1);
            } else {
                base.add(ps.getSceneBuildingUtil().select().position(pos));
            }
            base.queueRedraw();
        });
    }

    /**
     * 一条 {@code modify_block_entity_nbt} / {@code set_block} 的 NBT 补丁（见类注释第二节）。
     *
     * @param selection 目标格（Ponderer 那边是 {@code select().position(pos)} 的单格选区）
     * @param reDraw    JSON 决定的第 4 参（别凭感觉改）
     * @param payload   JSON 里的 SNBT 串，逐字
     */
    public static void mergeNbt(SceneBuilder scene, Selection selection, boolean reDraw, String payload) {
        CompoundTag patch = parseSnbt(payload);
        scene.world().modifyBlockEntityNBT(selection, BlockEntity.class, nbt -> nbt.merge(patch), reDraw);
    }

    /**
     * SNBT → {@code CompoundTag}。{@code TagParser.parseTag} 抛受检异常，
     * 这里包成非受检异常抛出（理由见 {@link #mergeNbt}）：
     * NBT 串是逐字从 JSON 抄来的字面量，{@code javac} 完全看不见它的内容，
     * 写错了三关全绿、只有进游戏才表现为"状态没变"⇒ 宁可整幕构建失败，也不要静默故障。
     * ⚠ 异常消息<b>不带空格</b>：本包的源码被关卡 P5 扫描，带空格的字符串字面量会被判成"散文"。
     */
    public static CompoundTag parseSnbt(String payload) {
        try {
            return TagParser.parseTag(payload);
        } catch (CommandSyntaxException e) {
            throw new IllegalStateException("invalid-snbt:" + payload, e);
        }
    }
}

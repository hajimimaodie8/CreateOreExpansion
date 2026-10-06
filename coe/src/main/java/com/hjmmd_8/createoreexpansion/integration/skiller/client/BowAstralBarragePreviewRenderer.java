package com.hjmmd_8.createoreexpansion.integration.skiller.client;

import com.hjmmd_8.createoreexpansion.client.render.types.AllRenderTypes;
import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.content.charger.entity.ChargerWaveFx;
import com.hjmmd_8.createoreexpansion.content.equipment.item.BowTier;
import com.hjmmd_8.createoreexpansion.content.equipment.item.JadeTopazBowItem;
import com.hjmmd_8.createoreexpansion.content.skill.config.weapon.BowAstralBarrageConfigs;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveTrailStyle;
import com.hjmmd_8.createoreexpansion.foundation.util.SkillOutlineColors;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * <b>星界弓「星元波置」的圆形技能范围预选框</b>（弓技能批 9，作者 2026-10-05）。
 *
 * <h2>作者原话（逐字，本类存在的唯一理由）</h2>
 * <blockquote>
 * 下优化：当他拉弓时，应该会看到一个圆形技能范围的预选框。<br>
 * 1. 预选框外观：边界是一圈类似于能量波的粉色线条。<br>
 * 2. 移动机制：预选框会一点一点往远离视角的方向移动，边缘最多移动至距离玩家准心 5 格的位置。<br>
 * 3. 释放机制：确定位置后松手，开始释放技能。……技能释放完之后，该预选框才会消失。
 * </blockquote>
 *
 * <h2>它照的是仓里哪一条既有栈（<b>不自创</b>）</h2>
 * <ul>
 *   <li><b>注册形状</b>：纯客户端 {@code @EventBusSubscriber(modid = CoeCore.MOD_ID,
 *       value = Dist.CLIENT)} + {@link ClientTickEvent.Post}（推进状态）+ {@link RenderLevelStageEvent}
 *       的 {@link RenderLevelStageEvent.Stage#AFTER_TRANSLUCENT_BLOCKS} 自绘 —— 与
 *       {@code content/energyfield/EnergyFieldGoggleOutlineRenderer} <b>逐条同形</b>
 *       （那条是"场区域预览框"，跟本类同一件事：把一块区域画成线框）；</li>
 *   <li><b>绘制形状</b>：批 12 起<b>主边界不再画线</b>，外观由 {@link #emitBoundaryParticles}
 *       的粒子环带承担（批 13 起是"有宽度"的环带）—— 见下面的批 12 / 批 13 两节。
 *       仍然保留的那条线渲染路径（不透明 {@link RenderType#LINES} + 穿透
 *       {@link AllRenderTypes#LINES_TRANSPARENT}，alpha 再乘 0.3）<b>只服务批 11 的过渡环</b>
 *       （alpha 再乘 {@link #TRANSITION_RING_ALPHA_FACTOR}），顶点写法与
 *       {@code OutlineRenderer#renderEdge} 一字不差（{@code addVertex → setColor → setNormal}）；
 *       冲刷方式照 {@code CoeBlockOutlineRenderer#flush}（<b>必须自己 endBatch</b>：
 *       框架不替我们冲刷，漏了这一步的症状是"算完了却什么都看不到"）；
 *       相机位移也照它（{@code translate(-camPos)}，漏了框会被画到大约两倍距离外）。</li>
 * </ul>
 * <p>⚠ <b>为什么不是 Skiller 的 {@code StrategyRenderer}</b>：那条管线只调度
 * {@code ClientSkillCache} 里<b>实现 {@code StrategySkill} 的技能</b>（
 * {@code CoeBlockOutlineRenderer} / {@code CoeEntityOutlineRenderer} 就是这么被驱动的），
 * 而星界弓这条专属技能是 {@code BowExclusiveShotItemSkill implements
 * ItemSkill<BowShootSkillContext>} —— 它进不了那条桶，所以本类自己订阅游戏总线，
 * 但<b>用同一套渲染类型、同一个阶段、同一个顶点写法</b>。</p>
 *
 * <h2>2026-10-06 弓技能批 11（作者原话，逐字）</h2>
 * <blockquote>
 * 第二条需要限制……第三条最好贴地，但不贴地也是可以接受。你可以加入一个检测机制，通过玩家的纵坐标
 * 与附近地面的距离（高度差）来实现：(a) 当玩家飞在空中……圈就没必要固定在地面上。
 * (b) 当玩家在起伏较小的平坡上……圈最好固定在地上，而不是必须设定在玩家的平面。<br>
 * 2. 高度差显示优化：如果有高度差，圈的显示最好在衔接的地方加入一些预选框，否则高度突然落差
 * 可能会显得有些突兀。
 * </blockquote>
 * <p>本类改的三处（数值与几何仍然一条都不在本类算）：</p>
 * <ol>
 *   <li><b>③"需要限制"</b>：圈<b>只在按住本技能自己的槽位键</b>时才出现 —— 槽位号来自真源
 *       {@code BowAstralBarrageConfigs#previewKeySlot()}（= 那条专属技能自己的 {@code SLOT}），
 *       此刻按下没有走既有的客户端键位表 {@code CoeSkillClient#toolSlotKeyHeld}。
 *       ⛔ 不是"按住任意技能键"（批 7/8 修掉的歧义），也<b>不</b>改用服务端权威读数
 *       （纯客户端逐帧渲染该读客户端键位；服务端那一半仍是 {@code CoeSkillProvider#slotPressed}）；
 *       ⚠ 松手之后的存活仍按批 9 的作者口径（"技能释放完之后，该预选框才会消失"）数排程时长；</li>
 *   <li><b>④贴地 / 不贴地</b>：{@code nearbyGroundY}(向下射线) → {@code sticksToGround}(高度差阈值)
 *       → {@code landingCentre}(圆心 Y 收口)，三步都调<b>服务端发射处调的同样三个方法</b>
 *       ⇒ 圈与落点不可能分家；</li>
 *   <li><b>④-2过渡环</b>：不贴地（有高度差）时，在主圈下方按真源给的间隔补
 *       {@code transitionRingYs} 那几道环，落进地面以下就停；画法与主圈逐字同形。</li>
 * </ol>
 *
 * <h2>★ 圆心 / 半径 / 颜色三件都不在本类算</h2>
 * <ul>
 *   <li><b>圆心</b>：{@code BowAstralBarrageConfigs#previewCenter(眼睛, 视线, 拉弓 tick 数)} ——
 *       <b>服务端发射弹幕用的是同一个方法</b>（{@code BowAstralBarrageLauncher#fire}）⇒
 *       "客户端看到的圈"与"实际落点"<b>结构上不可能不一致</b>（关卡
 *       {@code bow9-one-centre}：两个调用点都不许自己 {@code scale(} 视线、
 *       也不许提 {@code ANCHOR_FORWARD_BLOCKS} / {@code PREVIEW_MAX_FORWARD_BLOCKS} /
 *       {@code PREVIEW_FORWARD_BLOCKS_PER_TICK}）；</li>
 *   <li><b>半径</b>：{@code BowAstralBarrageConfigs#radiusFor(BowTier#thirdSkillLevel())} —— 与发射处
 *       同一处真源（"半径等于技能等级加 1"）。⚠ 批 12 之前这里读 {@code tier.baseSkillLevel()}
 *       （星界 = 3 ⇒ 半径 4），现在读槽 2 的等级（= 1 ⇒ 半径 <b>2</b>）——
 *       <b>两处同改</b>，否则画的圈与落的盘会差两格；</li>
 *   <li><b>粉色</b>：{@code BowTier#skillOutlineColor()} —— 也就是<b>星界那一档既有的描边色</b>
 *       （{@code SkillOutlineColors.STELLARSTONE_PINK}，仓库里五件星界工具用的同一色）。
 *       本类<b>不写任何 RGB / hex 字面量</b>，也不新造色值（作者："边界是一圈类似于能量波的
 *       粉色线条"——"粉色"取的就是弓自己那一色；批 12 换成粒子之后<b>这一条口径没变</b>：
 *       粒子色仍由本类的 red/green/blue 三个字段给出，而它们只从 {@code skillOutlineColor()} 取）；
 *       alpha 走既有的 {@link OutlineColors#ALPHA}（技能预览统一的 0.5）与既有的穿透层系数，
 *       批 12 起只由<b>过渡环</b>用（再乘 {@link #TRANSITION_RING_ALPHA_FACTOR}）——
 *       ⚠ 批 13 把主边界那条线删掉之后，{@code OutlineColors.ALPHA} 仍然被用着
 *       （批 9 的关卡 {@code bow9-preview-pink} 钉着这个名字，删掉这个名字就是删掉那条断言的一半）。</li>
 * </ul>
 *
 * <h2>★ 什么时候出现、什么时候消失（作者第 3 条）</h2>
 * <ol>
 *   <li><b>拉弓中 + 按住专属键</b>（{@code player.isUsingItem()} 且手里那把弓的档位
 *       <b>走这条技能</b>（判据就是发射处那同一个
 *       {@code BowAstralBarrageConfigs#appliesTo(档位)}）且<b>本技能自己的槽位键被按住</b>
 *       —— 第三个条件是批 11③ 加的，见上面的批 11 一节）：
 *       每 tick 用"已经拉了多少 tick"重算圆心 ⇒ 圈<b>一点一点往远离视角的方向移动</b>，
 *       到了真源的上限就不动；</li>
 *   <li><b>松手那一刻</b>（或中途松开专属键）：圆心<b>冻结</b>（本类不再重算，
 *       保留最后一次算出来的值）；服务端此刻用同一个输入算出同一个圆心 ⇒ 落点就是圈所在处；</li>
 *   <li><b>释放期间</b>：圈一直在，直到 {@code BowAstralBarrageConfigs#barrageScheduleTicks()}
 *       （= 条数 × 节拍）数完才消失 —— 作者批 9："技能释放完之后，该预选框才会消失"。
 *       客户端没有"弹幕打完了"的包可收，所以这个时长与发射处的排程用<b>同两个常量</b>算，
 *       改条数/节拍时两边一起动。</li>
 * </ol>
 *
 * <h2>⛔ 零注册、零贴图、零模型、零语言键</h2>
 * <p>整条预选框是<b>代码画线 + 既有粒子</b>（批 12 起后半句成立）：不加物品/方块/实体/渲染器注册，
 * 不动任何 {@code textures/**} 或 {@code models/**}（用户资产红线），不写语言键、不跑 {@code runData}。
 * <b>批 12 的粒子也没有新增任何注册</b> —— 它用的是波自己那个既有构造口
 * （{@link ChargerWaveFx#waveParticle(WaveTrailStyle, Vec3, float)}，原版尘埃粒子）与既有的
 * {@code Level#addParticle(..)}，见 {@link #emitBoundaryParticles}。</p>
 *
 * <h2>2026-10-06 弓技能批 12（作者原话，逐字）</h2>
 * <blockquote>
 * 1. 能跑的距离：上限 5 → <b>15</b>（推进速度不动）。<br>
 * 2. 边界外观：从"一圈线"改成<b>闪烁的能量波样式粒子</b>（复用能量波那套粒子）。
 * </blockquote>
 * <p>本类承担两件事：</p>
 * <ol>
 *   <li><b>上限 15</b>：<b>本类一个字都没改</b> —— 圆心仍只问
 *       {@code BowAstralBarrageConfigs#previewCenter}（上限住在那张表里，5.0D → 15.0D）；
 *       ⚠ 推进速度也没动（{@code PREVIEW_FORWARD_BLOCKS_PER_TICK} 仍 0.05 格/tick）
 *       ⇒ 推满 15 格需要 220 tick（11 秒）的持续拉弓（原版弓的使用时长足够）；</li>
 *   <li><b>边界改成闪烁的粒子</b>：即 {@link #emitBoundaryParticles} —— 粒子类型与颜色口径
 *       <b>都取自既有来源</b>（波自己的构造口 + 弓自己那一色），"闪烁"由<b>同一个相位</b>
 *       驱动的尺寸脉动 + 概率性抽稀两条手段表达；原来那两层线退成
 *       {@link #TRANSITION_RING_ALPHA_FACTOR} 系数的<b>很淡底线</b>（形状参考，同时是批 11
 *       过渡环的共用画法）。性能量级：期望 6 颗/tick。</li>
 * </ol>
 *
 * <h2>2026-10-06 弓技能批 13（作者实测反馈，逐字）</h2>
 * <blockquote>
 * 预选框它还是一个圆形的，不应该是像能量波那样吗
 * </blockquote>
 * <p>他附的游戏内截图里只能看到<b>一圈很细的粉色线</b>、<b>看不到粒子</b>。批 12 的两条病根
 * （都在本类，与几何真源无关）：</p>
 * <ol>
 *   <li><b>太稀</b>：8 个采样 × 0.75 ≈ 6 颗/tick，而半径 2 的圆周有 2π × 2 ≈ 12.6 格
 *       ⇒ 每格圆周约 3 颗、每颗目视只有约 0.05 格（0.45f 的尘埃）
 *       ⇒ 线性覆盖率约 17%，远看就是"几个点"；</li>
 *   <li><b>线还在</b>：那两层 0.22 系数的底线<b>带着完整的圆周形状</b>，人眼把它读成"一个圈"，
 *       而粒子的存在感远远盖不过它。</li>
 * </ol>
 * <p>批 13 的落法（只动外观，<b>几何三项一个字节没动</b>：圆心仍只问
 * {@code previewCenter} + {@code landingCentre}，半径仍只问 {@code radiusFor(thirdSkillLevel())}，
 * 上限 15 与速率 0.05 仍只住 {@code BowAstralBarrageConfigs}）：</p>
 * <ol>
 *   <li><b>密度</b>：{@link #WAVE_PARTICLE_COUNT} 8 → 12，并且每一道子环都发 ⇒
 *       期望 {@code 12 × }{@link #WAVE_BAND_RINGS}{@code  × 0.75 = 27 颗/tick}（批 12 的 4.5 倍），
 *       硬上限 36 颗/tick（见 {@link #FLICKER_KEEP_CHANCE}）；</li>
 *   <li><b>带宽</b>：{@link #WAVE_BAND_RINGS} 道同心子环 + 逐颗径向抖动 ⇒ 一条宽
 *       {@code 2 × }{@link #WAVE_BAND_HALF_WIDTH}{@code  = 0.56 格}的连续环带
 *       （不再是"数学上零宽度的圆"）；</li>
 *   <li><b>尺寸</b>：{@link #WAVE_PARTICLE_SCALE} 0.45f → 0.62f（取仓里既有的
 *       {@code ChargerWaveFx#ORBIT_TRAIL_SCALE}，<b>没有自造新尺寸</b>）；</li>
 *   <li><b>线的去留</b>：<b>主边界那条线删掉</b>（作者看到的就是它）；
 *       {@link #ring} 这条渲染路径<b>保留给批 11 的过渡环</b>（那只在空中且有落差时出现）
 *       ⇒ 不新增第二条渲染路径，{@code OutlineColors.ALPHA} 也仍然被引用。</li>
 * </ol>
 * <p>关卡：{@code bow13-density}（粒子密度下限，从源码里的三个常量与波自己的拖尾基数算出来）、
 * {@code bow13-band}（有宽度的环带）、{@code bow13-no-main-line}（主边界那条线不许回来）、
 * {@code bow13-geometry}（圆心 / 半径 / 上限 15 / 速率 0.05 四项未变）、
 * {@code bow13-no-new-assets}（零新增粒子类型 / 贴图 / 注册 / 语言键），各带负向与反空转。</p>
 *
 * <h2>⚠ 只有进游戏才看得见</h2>
 * <p>按 {@code AGENTS.md} 的"客户端渲染只有进游戏才看得见"：{@code compileJava} / {@code runData}
 * 都<b>不</b>跑渲染，本类的正确性最终只能在游戏里验收（按住专属键拉弓才看到粉色边界并向外推进、
 * 远端最多 <b>15</b> 格、<b>远看就是一枚平面上的粉色能量波：边界有宽度、粒子明显在闪烁流动，
 * 而不再是一圈细线</b>、平坡上圈贴地而飞在空中时留在准心平面、有落差处能看到过渡环、
 * 松手后箭与波几乎同时落地、放完圈才消失）。</p>
 *
 * @since 1.0.0
 */
@EventBusSubscriber(modid = CoeCore.MOD_ID, value = Dist.CLIENT)
public final class BowAstralBarragePreviewRenderer {

    /** 圆环的段数（画得足够圆、顶点又少；纯表现参数，不进数值真源）。 */
    private static final int RING_SEGMENTS = 64;

    /**
     * 穿透层的 alpha 系数（= {@code CoeBlockOutlineRenderer#TRANSPARENT_ALPHA_FACTOR} 同值）：
     * 第二层用 {@link AllRenderTypes#LINES_TRANSPARENT}（无深度测试）画同样的圈，
     * 被地形挡住时也还看得见一圈淡淡的边。
     */
    private static final float TRANSPARENT_ALPHA_FACTOR = 0.3F;

    // ==================== 批 13：边界 = 一条「有宽度、在流动」的平面能量波 ====================
    // 作者 2026-10-06（批 12 之后实测反馈，逐字）："预选框它还是一个圆形的，不应该是像能量波
    // 那样吗"，并附言只能看到**一圈很细的粉色线**、**看不到粒子**。
    // 读码结论（"一条波在游戏里视觉上由什么构成"，本仓既有栈）：
    //   · 一条主波的拖尾 = 每 tick **6 颗**原版染色尘埃（DustParticleOptions）
    //     （AbstractChargerWaveEntity#trailCount() 的 6），尺寸 **0.45f**
    //     （同文件 #trailScale()），散布 = 运动方向 × 0.12（所以 6 颗挤在约 0.2 格的一小团里）；
    //   · 环绕波那档更粗更密：**12 颗 / 0.62f**（ChargerWaveFx#ORBIT_TRAIL_COUNT / #ORBIT_TRAIL_SCALE）；
    //   · 原版尘埃的寿命与尺寸**同一个 scale 参数**：DustParticleBase 里
    //     quadSize ×= 0.75 × scale、lifetime = max(i × scale, 1)，i = (int)(8 / (rand*0.8+0.2)) ∈ [8,40]
    //     ⇒ 0.45f 时期望寿命约 7 tick（一条波的稳态同屏约 6 × 7 ≈ 43 颗，且全挤在一小团里）；
    //   · 仓里"用尘埃粒子画一个环"的既有写法就是 ChargerWaveFx#sendOrbitMarks
    //     （每 3 tick 补一圈 8 个静态桩点、散布 0、速度 0）。
    // 批 12 的圈为什么读起来还是"几个点 + 一圈线"：同一条 0.45f 的粒子，一条波是**43 颗挤在
    // 0.2 格**里（于是亮成一团），而这个圈把 6 颗/tick 摊在 2π × 2 ≈ 12.6 格的圆周上
    // （半径 2 = BowAstralBarrageConfigs#radiusFor(1)）= 每格圆周约 3 颗、每颗目视只有约 0.05 格
    // ⇒ 线性覆盖率约 17%，远看就是"几个点"；而那条 0.22 系数的底线（见本批的取舍）带着完整的
    // 圆周形状，人眼自然把它读成"一个圈"。
    // 本批的画法（三个表现参数，全在本类 —— 与 RING_SEGMENTS / TRANSPARENT_ALPHA_FACTOR 同分工：
    // "数值真源放玩法数字、渲染器放表现参数"）：

    /**
     * <b>每道子环每 tick 取几个采样点</b>（表现参数，{@value #WAVE_PARTICLE_COUNT}）。
     *
     * <p>它<b>不是</b>每 tick 的总颗数：真正的采样总数 = 本值 × {@link #WAVE_BAND_RINGS}
     * （见那里），期望颗数再乘 {@link #FLICKER_KEEP_CHANCE}。</p>
     */
    private static final int WAVE_PARTICLE_COUNT = 12;

    /**
     * <b>边界的"带宽"用几道同心子环表达</b>（表现参数，{@value #WAVE_BAND_RINGS}）。
     *
     * <p>作者要的是"像能量波"，而一条波是<b>有厚度的一团</b>、不是一个数学上零宽度的圆：
     * 批 12 那条边界是单一半径上的采样（宽度 0），所以无论多密都还是"一条线"。
     * 本批沿半径铺 {@value #WAVE_BAND_RINGS} 道子环，相邻两道之间再叠
     * {@link #WAVE_BAND_HALF_WIDTH} 的一半作逐颗抖动 ⇒ 三段的径向区间首尾相接，
     * 合成一条<b>连续、宽 2 × {@value #WAVE_BAND_HALF_WIDTH} 格</b>的环带
     * （半径 2 的圈上约 0.56 格宽 ≈ 圈半径的 28%）。</p>
     */
    private static final int WAVE_BAND_RINGS = 3;

    /**
     * <b>环带的半宽</b>（格，表现参数，{@value #WAVE_BAND_HALF_WIDTH}）。
     *
     * <p>取 0.28 的依据：粒子的目视直径约 {@code 0.15 × 0.75 × }{@link #WAVE_PARTICLE_SCALE}
     * ≈ 0.07 格（{@code DustParticleBase} 的 quadSize 算式），一条"看起来连成一片"的环带
     * 至少要几个粒子宽度 ⇒ 0.56 格总宽约 8 个粒子宽；同时它只占圈半径（2 格）的 28%，
     * 不至于把"圆盘边界"糊成一个实心甜甜圈。</p>
     */
    private static final double WAVE_BAND_HALF_WIDTH = 0.28D;

    /**
     * <b>边界粒子尺寸</b>（表现参数，{@value #WAVE_PARTICLE_SCALE}）= 环绕波主体尘埃的调用基线
     * （{@code ChargerWaveFx#ORBIT_TRAIL_SCALE} 那条 {@code 0.62f}）⇒ 圈上的粒子与仓里
     * <b>既有的"环状尘埃"（环绕波那档）</b>是同一个尺寸、同一种东西，这正是作者
     * "能量波样式粒子"那句话的落点。
     *
     * <p>为什么不取主波拖尾的 0.45f：那 0.45f 只在"43 颗挤在 0.2 格"时才亮得起来，
     * 摊到 12.6 格的圆周上就只剩约 17% 的线性覆盖率（批 12 的实况）。取环绕波那档的 0.62f
     * 既让单颗看得见，又<b>没有自造新尺寸</b>。</p>
     */
    private static final float WAVE_PARTICLE_SCALE = 0.62F;

    /**
     * <b>闪烁周期</b>（tick，{@value #FLICKER_PERIOD_TICKS} = 半秒）："闪烁"= 存在感随时间脉动，
     * 本类用<b>两个同相位的量</b>表达它 ——
     * ① <b>尺寸脉动</b>（{@link #FLICKER_SCALE_AMPLITUDE}：尘埃粒子没有 alpha 参数，
     *    而"亮度/存在感"在原版尘埃上最直观的可调量就是尺寸）；
     * ② <b>概率性抽稀</b>（{@link #FLICKER_KEEP_CHANCE}：这一 tick 的某些采样点干脆不发）。
     * 两者都由 {@link #flickerPhase} 驱动，相位每 tick 走 1。
     */
    private static final int FLICKER_PERIOD_TICKS = 10;

    /** 尺寸脉动幅度（表现参数）：实际尺寸在 {@code 1 ± 本值} × {@link #WAVE_PARTICLE_SCALE} 之间摆动。 */
    private static final float FLICKER_SCALE_AMPLITUDE = 0.35F;

    /**
     * 每个采样点这一 tick 被发出来的概率（表现参数）。
     *
     * <p>0.75 ⇒ 期望每 tick <b>{@value #WAVE_PARTICLE_COUNT} × {@value #WAVE_BAND_RINGS} × 0.75
     * = 27 颗</b>，硬上限（概率只减不增）<b>36 颗/tick</b>。稳态同屏期望约 27 × 10 ≈ 270 颗
     * （0.62f 的尘埃期望寿命约 10 tick），仍然<b>低于本模组自己一次 5 级波爆炸的单发量</b>
     * （{@code ChargerWaveFx#boomParticleCount(5)} = 155 主色 + 77 第二色 + 58 点缀 ≈ 348 颗），
     * 约等于一次 20 枚波的弹幕拖尾总量（20 × 6 颗 × 7 tick ≈ 864）的三分之一。</p>
     *
     * <p>概率 <b>只决定"这一颗发不发"</b>（不是决定每 tick 的总量），所以 36 是结构上限。</p>
     */
    private static final double FLICKER_KEEP_CHANCE = 0.75D;

    /**
     * <b>过渡环（批 11 那个"衔接落差"的环）的透明度系数</b>（表现参数，
     * {@value #TRANSITION_RING_ALPHA_FACTOR}）。
     *
     * <p>⚠ <b>批 13 起这个系数只服务过渡环</b>：主边界那条线<b>已被删掉</b>（见
     * {@link #onRender} 与类注释的批 13 一节 —— 作者看到的那"一圈很细的粉色线"就是它）。
     * 过渡环<b>保留</b>它、也保留 {@link #ring} 这条渲染路径，理由是过渡环表达的是完全另一件事
     * （悬在空中时把 15 格远处的圈与地面接起来的那几道参考环，只在
     * {@code BowAstralBarrageConfigs#transitionRingYs} 非空 = 空中且有落差时才出现），
     * 而作者抱怨的是"主边界读起来是个圈"。透明度的绝对值仍由既有的
     * {@link OutlineColors#ALPHA} 乘本系数给出，本类不写第二个颜色/透明度真源
     * （批 9 的 {@code bow9-preview-pink} 仍钉着 {@code OutlineColors.ALPHA} 这个名字）。</p>
     */
    private static final float TRANSITION_RING_ALPHA_FACTOR = 0.22F;

    private static final double TWO_PI = 2.0D * Math.PI;

    /** 当前是否该画圈（拉弓中 = true；松手后数排程时长，数完置 false）。 */
    private static boolean active;

    /** 圆心（世界坐标，水平圆环所在的 Y 就是它）。 */
    private static Vec3 center = Vec3.ZERO;

    /** 圆环半径（格）—— 来自真源的"技能等级 + 1"。 */
    private static double radius;

    /** 松手之后还要亮多少 tick（排程时长；数到 0 才让圈消失）。 */
    private static int holdTicks;

    /**
     * <b>主圈下方那几道过渡环的 Y</b>（批 11④-2，作者："如果有高度差，圈的显示最好在衔接的地方
     * 加入一些预选框"）—— 由真源 {@code BowAstralBarrageConfigs#transitionRingYs} 给出：
     * 不贴地时才非空，且不会落进地面以下。贴地时是空数组（圈本来就在地面上，没有落差要衔接）。
     */
    private static double[] transitionYs = new double[0];

    /** 本档的描边色（取色处唯一：{@code BowTier#skillOutlineColor()}，在拉弓那一 tick 记下来）。 */
    private static float red;
    private static float green;
    private static float blue;

    /**
     * <b>闪烁相位</b>（每 tick +1）—— 边界粒子的<b>唯一</b>时间输入（批 12 引入、批 13 沿用）：
     * 它同时驱动
     * ① 尺寸脉动（{@link #FLICKER_SCALE_AMPLITUDE} + {@link #FLICKER_PERIOD_TICKS}）与
     * ② 每一道子环采样点绕圈的前进量（每 tick 转过 {@code 360 / }{@value #WAVE_PARTICLE_COUNT} 度
     * ⇒ 粒子看起来在边界上流动，而不是原地闪）。
     */
    private static int flickerPhase;

    private BowAstralBarragePreviewRenderer() {
    }

    // ========== 状态推进（每 tick） ==========

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null) {
            clear();
            return;
        }

        ItemStack using = player.getUseItem();
        BowTier tier = barrageTier(using);
        // ★ 批 11③（作者 2026-10-06："第二条需要限制"）：预选框**只在按住本技能自己的槽位键**
        //   时才出现 —— 批 9 那版是"任何一次拉弓都显示"（按 Shift / R 也画）。槽位号只有一处
        //   （BowAstralBarrageConfigs#previewKeySlot = 那条专属技能自己的 SLOT），键位读数走
        //   既有的客户端键位表（CoeSkillClient#toolSlotKeyHeld：SLOT_KEYS + AllKeys.isPressed()）。
        //   ⛔ 不是"按住任意技能键"（批 7/8 刚把那种歧义修掉）；也**不**改用服务端权威读数
        //   （纯客户端渲染逐帧读按键，用按键包回传的服务端状态只会慢半拍）。
        boolean dedicatedKeyHeld = CoeSkillClient.toolSlotKeyHeld(BowAstralBarrageConfigs.previewKeySlot());
        if (player.isUsingItem() && tier != null && dedicatedKeyHeld) {
            // 拉弓中：圆心跟着"已经拉了多少 tick"一点点往前推。
            // "拉了多少 tick"与"圆心怎么算"两端共用真源同一条（BowAstralBarrageConfigs），
            // 所以这里与服务端发射处喂进去的是同一个数、算出的是同一个点。
            center = BowAstralBarrageConfigs.previewCenter(
                player.getEyePosition(), player.getLookAngle(),
                BowAstralBarrageConfigs.drawnTicks(using, player, player.getUseItemRemainingTicks()));
            // ★ 批 11④：贴不贴地（以及圈最终落在哪个 Y）走与服务端**完全同一组**真源方法 ——
            //   同一个"附近地面"射线、同一个高度差阈值、同一个圆心 Y 收口方法。
            double nearbyGroundY = BowAstralBarrageConfigs.nearbyGroundY(level, player);
            boolean stickToGround = BowAstralBarrageConfigs.sticksToGround(player.getY(), nearbyGroundY);
            center = BowAstralBarrageConfigs.landingCentre(center, nearbyGroundY, stickToGround);
            //   有高度差才要过渡环：圈悬在空中时，在它下方按固定间隔补一两道环把落差接起来。
            transitionYs = stickToGround
                ? new double[0]
                : BowAstralBarrageConfigs.transitionRingYs(center.y, nearbyGroundY);
            // ★ 批 12：半径读的是"该弓槽 2 那条技能的等级"（BowTier#thirdSkillLevel）——
            //   与服务端发射处（JadeTopazBowItem#fireAstralBarrageInsteadOfArrow）同一个读数，
            //   所以画的圈与落的盘半径仍逐值相同（关卡 bow12-skill-table 钉着这条同源）。
            radius = BowAstralBarrageConfigs.radiusFor(tier.thirdSkillLevel());
            SkillOutlineColors.SkillColor color = tier.skillOutlineColor();
            red = color.r();
            green = color.g();
            blue = color.b();
            holdTicks = BowAstralBarrageConfigs.barrageScheduleTicks();
            active = true;
            // ★ 批 13：边界的平面能量波环带（每 tick 12 × 3 个采样点，见 emitBoundaryParticles）。
            flickerPhase++;
            emitBoundaryParticles(level, center.y);
            return;
        }

        if (!active) {
            return;
        }
        // 已松手（或中途松开了专属键）：圆心停在最后一次算出来的值，只把排程时长数完。
        holdTicks--;
        if (holdTicks <= 0) {
            clear();
            return;
        }
        // 松手之后圈还在（作者批 9："技能释放完之后，该预选框才会消失"）⇒ 边界粒子在这段时间里
        // 照旧闪（圆心已冻结，粒子仍按 flickerPhase 脉动 / 绕圈），形状与位置都不变。
        flickerPhase++;
        emitBoundaryParticles(level, center.y);
    }

    /** 只在"圈该消失"时统一收尾（三处出口共用，免得留下一半个状态）。 */
    private static void clear() {
        active = false;
        holdTicks = 0;
        transitionYs = new double[0];
        flickerPhase = 0;
    }

    /**
     * <b>边界的那条"平面能量波"</b>（批 12 起是粒子，批 13 起是<b>有宽度的环带</b>，
     * 作者 2026-10-06："边界外观：从'一圈线'改成闪烁的能量波样式粒子"，同日实测反馈
     * "它还是一个圆形的，不应该是像能量波那样吗"）。
     *
     * <h2>粒子用的是哪一套（<b>不自创</b>）</h2>
     * <p>就是<b>能量波自己的那一套</b>：{@link ChargerWaveFx#waveParticle(WaveTrailStyle, Vec3, float)}
     * —— 波实体逐 tick 发拖尾用的<b>同一个</b>构造口（原版尘埃粒子 {@code DustParticleOptions}，
     * 颜色经该风格的颜色变换）。这里风格传 {@link WaveTrailStyle#NORMAL}（未登记/无魔素的基线
     * = <b>颜色原样使用入参</b>），入参色仍是弓自己那一色
     * （{@link BowTier#skillOutlineColor()} = 星界的 {@code STELLARSTONE_PINK}）⇒
     * 粒子类型与颜色口径<b>都不是本类自造的</b>：一个取自波，一个取自弓的既有档位表。</p>
     *
     * <h2>形状 = 一条有宽度的环带（批 13 的落点）</h2>
     * <p>沿半径铺 {@link #WAVE_BAND_RINGS} 道同心子环（每道 {@link #WAVE_PARTICLE_COUNT} 个采样），
     * 相邻两道的径向距离是 {@code 2 × }{@link #WAVE_BAND_HALF_WIDTH}{@code  / (RINGS - 1)}，
     * 而每一颗再叠 <b>半格间距</b>的随机径向抖动 ⇒ 三段的径向区间<b>首尾相接</b>，
     * 合成一条连续的环带（不是三道能数出来的圆、更不是零宽度的圆）。</p>
     * <p>三道子环的采样点还各自错开 {@code WAVE_PARTICLE_COUNT / RINGS} 个采样格，
     * 于是它们不会落在同一条半径上排成辐条。</p>
     *
     * <h2>流动与闪烁（三条手段，全部由同一个 {@link #flickerPhase} 驱动）</h2>
     * <ol>
     *   <li><b>绕圈流动</b>：采样点每 tick 沿圈前进一格（{@code 1 / WAVE_PARTICLE_COUNT} 圈）
     *       ⇒ 粒子看起来在边界上走，而不是原地闪；</li>
     *   <li><b>尺寸脉动</b>：尘埃粒子没有 alpha 参数 ⇒ "亮度/存在感"落在尺寸上：
     *       实际尺寸 = {@link #WAVE_PARTICLE_SCALE} ×
     *       {@code (1 + }{@link #FLICKER_SCALE_AMPLITUDE}{@code  × sin(2π × phase / }{@link #FLICKER_PERIOD_TICKS}{@code ))}，
     *       半秒一个来回（人眼一眼能看出"在闪"而不刺眼）。
     *       ⚠ <b>批 13 读码补充</b>：{@code DustParticleBase} 里 quadSize 与 <b>寿命</b>用的是
     *       <b>同一个 scale 参数</b>（{@code lifetime = max(i × scale, 1)}），所以这一条脉动
     *       同时让每颗的寿命在 0.65×~1.35× 之间呼吸 —— 这是粒子本来的行为，不是新加的旋钮；</li>
     *   <li><b>概率性抽稀</b>：每个采样点这一 tick 以 {@link #FLICKER_KEEP_CHANCE} 的概率才发出
     *       ⇒ 密度本身也在抖（这是作者允许的"概率性跳过部分粒子"那条做法）。</li>
     * </ol>
     *
     * <h2>⛔ 零注册 / 零贴图 / 零新粒子类型</h2>
     * <p>走的是原版 {@code Level#addParticle(ParticleOptions, ..)}（客户端本地发射，不落包、
     * 不进存档）+ 既有的 {@link ChargerWaveFx} ⇒ 本批<b>没有</b>新增
     * {@code ParticleType} 注册、<b>没有</b>新增贴图 / 粒子 JSON、也没有新实体或渲染器
     * （关卡 {@code bow12-preview-particles} / {@code bow13-no-new-assets} 把这几条钉成负向断言）。</p>
     *
     * <h2>性能（有界）</h2>
     * <p>每 tick 期望 {@value #WAVE_PARTICLE_COUNT} × {@value #WAVE_BAND_RINGS} ×
     * {@link #FLICKER_KEEP_CHANCE} = <b>27 颗</b>；概率只减不增 ⇒ <b>硬上限 36 颗/tick</b>。
     * 0.62f 的尘埃期望寿命约 10 tick（见 {@link #WAVE_PARTICLE_SCALE}）⇒ 稳态同屏期望约 270 颗，
     * 仍低于本模组自己一次 5 级波爆炸的单发量（≈348 颗）与一次 20 枚波弹幕的拖尾总量（≈864 颗）
     * —— 同一条论证见 {@link #FLICKER_KEEP_CHANCE}。本方法只有一个发射点、没有 tick 循环，
     * 每次调用最多 {@value #WAVE_PARTICLE_COUNT} × {@value #WAVE_BAND_RINGS} 次 {@code addParticle}。</p>
     *
     * <h2>⚠ 刻意<b>没有</b>给粒子初速</h2>
     * <p>速度传 0（与仓里既有的"用尘埃画环"写法 {@code ChargerWaveFx#sendOrbitMarks} 一致，
     * 那里散布与速度都是 0）：预选框的职责是<b>标出技能真正的落点</b>，环带的<b>平均</b>半径必须
     * 落在真源半径上；"有宽度"由逐颗径向抖动给、"在流动"由绕圈采样给 —— 若再加一个径向外速，
     * 环带会整体外扩，画出来的边界就不再是落点。想让带子看起来更"鼓"，只该调
     * {@link #WAVE_BAND_HALF_WIDTH}。</p>
     *
     * @param level 客户端世界（{@code Minecraft#level}；粒子只在客户端存在）
     * @param y     这一圈所在的 Y（主圈传 {@link #center} 的 Y；圆心已冻结时就是冻结那一刻的值）
     */
    private static void emitBoundaryParticles(ClientLevel level, double y) {
        // 尺寸脉动：半秒一个来回（表现参数，见 FLICKER_PERIOD_TICKS / FLICKER_SCALE_AMPLITUDE）。
        float pulse = 1.0F + FLICKER_SCALE_AMPLITUDE
            * (float) Math.sin(TWO_PI * (double) flickerPhase / (double) FLICKER_PERIOD_TICKS);
        // 粒子类型 + 颜色口径都取自既有来源：波自己的构造口 + 弓自己那一色（本类不写 RGB 字面量）。
        ParticleOptions particle = ChargerWaveFx.waveParticle(
            WaveTrailStyle.NORMAL, new Vec3(red, green, blue), WAVE_PARTICLE_SCALE * pulse);
        // 环带的几何：RINGS 道子环均分 2 × HALF_WIDTH 的带宽，每颗再叠半格间距的径向抖动
        // ⇒ 三段区间首尾相接，合成一条连续的环带（见 WAVE_BAND_HALF_WIDTH）。
        double ringStep = WAVE_BAND_RINGS > 1
            ? (2.0D * WAVE_BAND_HALF_WIDTH) / (double) (WAVE_BAND_RINGS - 1)
            : 0.0D;
        double radialJitter = WAVE_BAND_RINGS > 1 ? ringStep * 0.5D : WAVE_BAND_HALF_WIDTH;
        for (int ring = 0; ring < WAVE_BAND_RINGS; ring++) {
            double ringRadius = radius + (double) ring * ringStep - WAVE_BAND_HALF_WIDTH;
            // 每道子环把自己的采样点错开 1 / RINGS 圈 ⇒ 三道环不会在同一条半径上排成辐条。
            int ringPhase = (ring * WAVE_PARTICLE_COUNT) / WAVE_BAND_RINGS;
            for (int i = 0; i < WAVE_PARTICLE_COUNT; i++) {
                // ② 概率性抽稀（"闪烁"的第二条手段）。
                if (level.random.nextDouble() >= FLICKER_KEEP_CHANCE) {
                    continue;
                }
                // ① 采样点每 tick 沿圈前进一格（1 / WAVE_PARTICLE_COUNT 圈）⇒ 在边界上流动。
                double angle = TWO_PI * (double) (i + ringPhase + flickerPhase % WAVE_PARTICLE_COUNT)
                    / (double) WAVE_PARTICLE_COUNT;
                double jitter = (level.random.nextDouble() * 2.0D - 1.0D) * radialJitter;
                double offset = ringRadius + jitter;
                level.addParticle(particle,
                    center.x + Math.cos(angle) * offset, y, center.z + Math.sin(angle) * offset,
                    0.0D, 0.0D, 0.0D);
            }
        }
    }

    /**
     * <b>这把弓是不是走「星元波置」的那一档</b>（唯一判据 = 发射处那同一个
     * {@code BowAstralBarrageConfigs#appliesTo(档位)}）。
     *
     * <p>刻意复用发射闸门而不是另写一份"是不是星界弓"：哪一档有这条技能<b>只有那一处</b>说了算，
     * 将来挪档位/加弓时，预选框与真发射会一起变（不会出现"画了圈但没有技能"或反之）。</p>
     */
    private static BowTier barrageTier(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof JadeTopazBowItem bow)) {
            return null;
        }
        return BowAstralBarrageConfigs.appliesTo(bow.tier()) ? bow.tier() : null;
    }

    // ========== 自绘渲染（每帧） ==========

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || !active) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera()
            .getPosition();

        MultiBufferSource.BufferSource buffer = mc.renderBuffers()
            .bufferSource();

        // **必须做相机位移**（照 CoeBlockOutlineRenderer）：这一步漏掉的症状是把世界坐标当成
        // 相机相对坐标画 → 圈被画到大约两倍距离的地方，又远又小、几乎看不见。
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        PoseStack.Pose pose = poseStack.last();

        // ★ 批 13：主边界**不再画线**（作者 2026-10-06 的实测反馈就是"它还是一个圆形的"，而他
        //   只看到"一圈很细的粉色线"）：那条 0.22 系数的底线带着完整的圆周形状，人眼自然把它
        //   读成"一个圈"并盖过粒子。边界的外观现在**完全**由 {@link #emitBoundaryParticles}
        //   的环带承担 —— 见类注释的批 13 一节与 {@link #TRANSITION_RING_ALPHA_FACTOR}。
        //   ⚠ 取舍：{@link #ring} 这条渲染路径**保留**，因为批 11 的过渡环照旧走它（见下面的 for）
        //   ⇒ 既不用为过渡环另开一条渲染路径，{@code OutlineColors.ALPHA} 也仍是那几道环的唯一
        //   透明度真源（批 9 的关卡 bow9-preview-pink 钉着这个名字）。
        //
        // ★ 批 11④-2：有高度差时把落差"接"起来的那几道过渡环（真源给的 Y 列表；贴地时是空的）。
        //   画法与批 12 逐字同形（同一个 ring(..)，只是 Y 更低），所以两层渲染的顶点格式与冲刷
        //   路径一条都不新增。它们<b>只在空中且与地面有落差时出现</b>（贴地时 transitionYs 为空），
        //   因此不会在作者站的平地上重新画出一个"细线圆圈"。
        if (transitionYs.length > 0) {
            VertexConsumer solid = buffer.getBuffer(RenderType.LINES);
            VertexConsumer transparent = buffer.getBuffer(AllRenderTypes.LINES_TRANSPARENT);
            for (double transitionY : transitionYs) {
                ring(pose, solid, transitionY, red, green, blue,
                    OutlineColors.ALPHA * TRANSITION_RING_ALPHA_FACTOR);
                ring(pose, transparent, transitionY, red, green, blue,
                    OutlineColors.ALPHA * TRANSITION_RING_ALPHA_FACTOR * TRANSPARENT_ALPHA_FACTOR);
            }
            // **必须自己冲刷**：接口给的是原版 buffer source，没有人替我们 endBatch
            // （形状照 CoeBlockOutlineRenderer#flush，按 RenderType 立即结算这两批）。
            buffer.endBatch(RenderType.LINES);
            buffer.endBatch(AllRenderTypes.LINES_TRANSPARENT);
        }

        poseStack.popPose();
    }

    /**
     * 把一个水平圆环画成圈线（圆心 {@link #center} 的 XZ、半径 {@link #radius}、
     * 高度取调用方给的 {@code y}）—— 每一道都与弹幕圆盘在 XZ 上同心。
     *
     * <p>⚠ <b>批 13 起<u>唯一的调用方是批 11 的过渡环</u></b>（真源给的
     * {@code transitionYs}，只在空中且有落差时非空）：主边界那条线已经删掉（见 {@link #onRender}
     * 与类注释的批 13 一节 —— 作者看到的那"一圈很细的粉色线"就是它）。alpha 由调用方乘
     * {@link #TRANSITION_RING_ALPHA_FACTOR}（很淡）。保留这条路径的全部理由就是<b>不要为过渡环
     * 另开第二条渲染路径</b>，同时也让 {@code OutlineColors.ALPHA} 继续有一个真实读者
     * （批 9 的关卡 {@code bow9-preview-pink} 钉着那个名字）。</p>
     *
     * <p>顶点写法照 {@code OutlineRenderer#renderEdge}：{@code addVertex → setColor → setNormal}
     * （法线取该段的方向），两种渲染类型的格式差异（{@code RenderType.LINES} 是
     * POSITION_COLOR_NORMAL、穿透层是 POSITION_COLOR）在这里与既有渲染器处理方式完全一致。</p>
     */
    private static void ring(PoseStack.Pose pose, VertexConsumer consumer, double y,
                             float r, float g, float b, float a) {
        for (int i = 0; i < RING_SEGMENTS; i++) {
            double from = TWO_PI * i / RING_SEGMENTS;
            double to = TWO_PI * (i + 1) / RING_SEGMENTS;
            double x0 = center.x + Math.cos(from) * radius;
            double z0 = center.z + Math.sin(from) * radius;
            double x1 = center.x + Math.cos(to) * radius;
            double z1 = center.z + Math.sin(to) * radius;
            double dx = x1 - x0;
            double dz = z1 - z0;
            double length = Math.sqrt(dx * dx + dz * dz);
            float nx = length > 1.0E-9D ? (float) (dx / length) : 1.0F;
            float nz = length > 1.0E-9D ? (float) (dz / length) : 0.0F;
            vertex(pose, consumer, x0, y, z0, nx, nz, r, g, b, a);
            vertex(pose, consumer, x1, y, z1, nx, nz, r, g, b, a);
        }
    }

    /** 圈上的一个顶点（Y 取该道环自己的高度 ⇒ 每一道都是水平圆环）。 */
    private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, double x, double y, double z,
                               float nx, float nz, float r, float g, float b, float a) {
        consumer.addVertex(pose, (float) x, (float) y, (float) z)
            .setColor(r, g, b, a)
            .setNormal(pose, nx, 0.0F, nz);
    }
}

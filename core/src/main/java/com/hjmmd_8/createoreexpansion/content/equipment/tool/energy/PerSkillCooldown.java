package com.hjmmd_8.createoreexpansion.content.equipment.tool.energy;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * <b>按技能记</b>的冷却载体（coe-pact 批 2；作者 2026-10-03 裁定：冷却载体 = <b>B</b>）。
 *
 * <h2>为什么不是 {@link ToolSkillCooldown}（= 裁定里的"A 沿用按物品记"）</h2>
 * <p>那个载体的冷却截止时刻写在<b>物品 stack 的数据组件</b>上
 * （{@code ToolDataComponents.SKILL_COOLDOWN_UNTIL}），判定入口是
 * {@code CoeSkillSupport#onCooldown} ⇒ <b>同一把剑上的所有技能共用一个冷却</b>。
 * 血契置换是 30 秒的长冷却，若走那条路，这把剑上的剥取 / 夺取会被一起冻结 30 秒
 * （作者 2026-10-03 明确不要这种"连坐"）。所以这里另起一份：<b>键 = 技能 id</b>，
 * 同一把剑上的三个技能各记各的。</p>
 *
 * <h2>载体落点（我的选择与依据）</h2>
 * <ul>
 *     <li><b>玩家 {@code persistentData}</b>（{@code createoreexpansion:skill_cd_<技能 id 路径>}）
 *         —— 与既有"装备技能冷却"同一形状（{@code ArmorSkillRuntime} 的
 *         {@code createoreexpansion:equip_cd_<skill>}）：冷却对<b>技能</b>来说本来就是
 *         玩家级状态，写在物品上既会因换剑失配、又会与同剑的其它技能纠缠；</li>
 *     <li><b>不改</b> {@code SkillEnergyCost} / {@code ToolEnergy} / {@code ToolSkillCooldown}
 *         的任何既有语义（本类是<b>纯新增</b>，上面那份按物品记的载体一个字节都不动）；</li>
 *     <li><b>老存档</b>：键不存在 ⇒ {@code getLong} 返回 0 ⇒ 剩余 0 秒 = <b>未冷却</b>
 *         —— 这正是最自然的缺省语义，<b>不需要任何迁移</b>；反之旧版本读新存档也只是多一个
 *         它不认识的键（同一 NBT 容器，格式未变）。</li>
 * </ul>
 *
 * <h2>显示（§2.5：要给玩家看的状态必须显式同步）</h2>
 * <p>承 {@code mcmod_experience.md} §2.5：<b>玩家持久数据不会同步到客户端</b>，所以本条冷却
 * 绝不能让客户端去读它。现有"技能冷却怎么给玩家看"的两条通道查清了：</p>
 * <ul>
 *     <li><b>装备技能</b>：{@code EquipCooldownPayload}（服务端起冷却时发技能 id + 秒数）
 *         → {@code ArmorCooldownClient} 客户端自己倒数 → {@code EquipmentSkillHud} 的括号冷却行
 *         —— 那是"装备段"的面板，只有穿着成套护甲且装备模式开关打开时才画；</li>
 *     <li><b>工具 / 武器技能</b>（剥取 / 夺取用的就是这条，也是本类的技能所属的那一族）：
 *         {@code player.getCooldowns().addCooldown(item, ticks)} = <b>原版快捷栏冷却遮罩</b>。
 *         它由原版自己同步：{@code ServerItemCooldowns#onCooldownStarted} 会
 *         {@code send(new ClientboundCooldownPacket(item, ticks))}（见
 *         {@code net.minecraft.world.item.ServerItemCooldowns}），客户端不需要读我方任何容器。
 *         工具 tooltip 里<b>没有</b>冷却行，所以这就是武器技能唯一的既有显示通道。</li>
 * </ul>
 * <p>⇒ 本类沿用<b>后一条</b>（同一套显示通道），不新造 HUD、不新发载荷：
 * 一条只发不收的同步或一个没有读取方的客户端缓存，正是 §2.5 里"看着在跑、其实没接上"的形状。</p>
 *
 * <p><b>已知口径（不修，属该通道固有）</b>：原版遮罩按 <b>Item</b> 记，同一把剑上剥取 / 夺取
 * 起冷却时会用它们自己的（更短的）时长覆盖本技能那 30 秒的显示 —— 影响的是<b>显示</b>，
 * 判定仍各按各的键（剥取 / 夺取读物品组件，本技能读自己的技能键）。
 * 若日后要一条"血契置换 冷却中：还需 N 秒"的<b>带字</b>HUD 行，那是新增显示面，需作者点头。</p>
 *
 * @since 1.0.0
 */
public final class PerSkillCooldown {

    /**
     * 玩家持久数据里的冷却键前缀（后接技能 id 的 path）。
     *
     * <p>命名与 {@code ArmorSkillRuntime.COOLDOWN_PREFIX}（{@code createoreexpansion:equip_cd_}）
     * 同一家族、不同段：{@code skill_cd_} = 工具 / 武器技能，{@code equip_cd_} = 装备技能。
     * 两者<b>互不读取</b>。</p>
     */
    public static final String KEY_PREFIX = "createoreexpansion:skill_cd_";

    private PerSkillCooldown() {
        throw new AssertionError("This class should not be instantiated");
    }

    /** 该技能还剩多少 tick 冷却（0 = 就绪；键不存在、或已到期都返回 0）。 */
    public static int remainingTicks(Player player, ResourceLocation skillId) {
        if (player == null || skillId == null) {
            return 0;
        }
        long until = player.getPersistentData().getLong(KEY_PREFIX + skillId.getPath());
        if (until <= 0L) {
            return 0;
        }
        return (int) Math.max(0L, until - player.level().getGameTime());
    }

    /** 该技能是否就绪（冷却已过）。<b>键不存在 ⇒ true</b>（老存档 / 从没用过 = 未冷却）。 */
    public static boolean isReady(Player player, ResourceLocation skillId) {
        return remainingTicks(player, skillId) <= 0;
    }

    /**
     * 起冷却：写自己那一格<b>并</b>走既有显示通道（快捷栏冷却遮罩）。
     *
     * <p>写与显示放在同一个入口，是为了让"记了冷却却没让玩家看见"这件事写不出来
     * （{@link ToolSkillCooldown#startTicks} 也是这个形状）。</p>
     *
     * @param player  服务端玩家（持久数据不进客户端，本方法只在服务端调用）
     * @param skillId 技能 id —— 就是这一格的键，所以同剑的其它技能不受影响
     * @param ticks   冷却时长（tick；&le; 0 时什么都不做）
     */
    public static void startTicks(Player player, ResourceLocation skillId, int ticks) {
        if (player == null || skillId == null || ticks <= 0) {
            return;
        }
        player.getPersistentData().putLong(KEY_PREFIX + skillId.getPath(),
                player.level().getGameTime() + ticks);
        // 显示通道：与剥取 / 夺取同一条（原版快捷栏遮罩；原版 ServerItemCooldowns 自己同步给客户端）
        ItemStack held = player.getMainHandItem();
        if (!held.isEmpty()) {
            player.getCooldowns().addCooldown(held.getItem(), ticks);
        }
    }
}

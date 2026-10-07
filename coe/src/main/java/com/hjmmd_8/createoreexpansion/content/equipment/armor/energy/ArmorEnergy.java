package com.hjmmd_8.createoreexpansion.content.equipment.armor.energy;

import java.util.List;

import com.hjmmd_8.createoreexpansion.common.energy.ToolDataComponents;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.SkillEnergyCost;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnchantments;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.ArmorEnchantments;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.ArmorSet;

/**
 * <b>装备（护甲）能量层</b>：四套护甲各自的单件储能、四件合计的"套装能量池"，以及
 * 「四件平摊」的消耗编排。
 *
 * <h2>用户 2026-10-01 定稿的全部口径</h2>
 * <ol>
 *   <li><b>单件储能</b>：翠玉 <b>250</b> / 宝石 <b>1000</b> / 星界 <b>2500</b> / 雷鸣 <b>2500</b>
 *       （{@link ArmorSet#perPieceEnergy()}）；满套 = 四件之和（1000 / 4000 / 10000 / 10000）。</li>
 *   <li><b>带「散构聚能」的件一律 2500</b>（用户按字面选定，<b>含本模组四套的件</b>）——
 *       该附魔是宝藏附魔、不好获取，所以附上就"被赋予一套能量机制"。</li>
 *   <li><b>充能两条路</b>：① 凝能佩的充能模式（{@code BaseStressMedallionItem#chargeBoundTools}）；
 *       ② 能量波打中穿戴护甲的玩家（{@code AbstractChargerWaveEntity} 命中实体那一支）。</li>
 *   <li><b>消耗 = 四件平摊</b>：各付 {@code amount / 4}，<b>余数顺延</b>
 *       （头 → 胸 → 腿 → 脚，见 {@link #splitEvenly(int)}）；预检 = 四件可用之和 ≥ 消耗
 *       （这就是用户说的"综合累计计算"）；<b>全有或全无</b>（先全检、再全扣，与内核一致）。</li>
 *   <li><b>附魔按件叠加、总折扣封顶 50%</b>：减耗（{@link ToolEnchantments#reduceConsumptionLevel}）
 *       与迅启（{@link ToolEnchantments#swiftStartLevel}）在四件上各自生效后<b>相乘</b>，
 *       结果不低于 {@link #DISCOUNT_FLOOR}。低级附魔叠多件有意义，高级叠加被上限压住。</li>
 * </ol>
 *
 * <h2>为什么容量是"算出来"的，而不是物品组件</h2>
 * <p>工具那套是 {@code MAX_ENERGY} 组件写死在物品上（{@link com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergy}），
 * 但护甲的容量取决于<b>有没有散构聚能</b>（250 ↔ 2500），而附魔可以被铁砧随时加减 ⇒
 * 写死组件必然漂移。所以护甲<b>只存"当前能量"</b>（沿用 {@link ToolDataComponents#ENERGY}），
 * 容量一律由 {@link #maxOf(ItemStack)} 现算。</p>
 * <p>副作用（<b>刻意</b>）：护甲身上没有 {@code MAX_ENERGY} 组件 ⇒
 * {@code ToolEnergy.canCharge} 对护甲恒为 false ⇒ <b>掉在地上的护甲不会被能量波当物品充能</b>。
 * 想充能只能穿在身上（或用凝能佩）—— 正是用户要的形态。</p>
 *
 * @since 1.0.0
 */
public final class ArmorEnergy {

    /** 带「散构聚能」的单件容量（用户定稿：一律 2500，含本模组件）。 */
    public static final int LOOSE_CONVERGENCE_CAPACITY = 2500;

    /**
     * 附魔叠加后的<b>折扣下限</b>（总折扣封顶 50%）：消耗与冷却都不低于这个倍数。
     *
     * <p>批 15：这个 {@code 0.5} 与折扣斜率 {@code 1 - 0.1L} 的唯一真源搬到了 core 的
     * {@link SkillEnergyCost#discountMultiplier(int)}（工具侧 {@code SkillEnergyCost#compute}
     * 用的是同一个数）；本常量是它的<b>别名</b>，值逐位不变（仍是 0.5）。</p>
     */
    public static final double DISCOUNT_FLOOR = SkillEnergyCost.DISCOUNT_FLOOR;

    /** 护甲只有四个槽位：<b>顺序的唯一来源</b>是 {@link ArmorSet#armorSlots()}（分摊语义依赖它）。 */
    private static final List<EquipmentSlot> ARMOR_SLOTS = ArmorSet.armorSlots();

    private ArmorEnergy() {
        throw new AssertionError("This class should not be instantiated");
    }

    /**
     * 单件护甲的储能上限（现算）。
     *
     * @return 带散构聚能 ⇒ {@link #LOOSE_CONVERGENCE_CAPACITY}；本模组四套 ⇒ 该套单件值；
     *         其余（既没附魔也不是本模组护甲）⇒ {@code 0}（不储能）
     */
    public static int maxOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        if (ArmorEnchantments.looseConvergenceLevel(stack) > 0) {
            return LOOSE_CONVERGENCE_CAPACITY;
        }
        ArmorSet set = ArmorSet.of(stack);
        return set == null ? 0 : set.perPieceEnergy();
    }

    /** 该件是不是"能储能的护甲"（容量 > 0）。 */
    public static boolean stores(ItemStack stack) {
        return maxOf(stack) > 0;
    }

    /** 该件当前能量（未储能或未写过 ⇒ 0）。 */
    public static int getEnergy(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        Integer energy = stack.getComponents().get(ToolDataComponents.ENERGY);
        return energy == null ? 0 : Math.max(0, energy);
    }

    /**
     * 给单件充能（充到上限为止）。
     *
     * @return 实际加入的能量（0 = 已满或不储能）
     */
    public static int addEnergy(ItemStack stack, int amount) {
        if (amount <= 0) {
            return 0;
        }
        int max = maxOf(stack);
        if (max <= 0) {
            return 0;
        }
        int current = Math.min(getEnergy(stack), max);
        int added = Math.min(amount, max - current);
        if (added <= 0) {
            return 0;
        }
        stack.set(ToolDataComponents.ENERGY, current + added);
        return added;
    }

    /**
     * 给玩家穿戴中的四件护甲各充能（能量波打中穿戴者 / 凝能佩充能模式用）。
     *
     * @param perPiece 每件尝试加入的能量
     * @return 四件实际加入的总量
     */
    public static int chargeWorn(Player player, int perPiece) {
        if (player == null || perPiece <= 0) {
            return 0;
        }
        int added = 0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            added += addEnergy(player.getItemBySlot(slot), perPiece);
        }
        return added;
    }

    /** 四件护甲的容量之和（不要求成套：判定用"穿戴中的四件"，与消耗口径一致）。 */
    public static int totalMax(Player player) {
        if (player == null) {
            return 0;
        }
        int total = 0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            total += maxOf(player.getItemBySlot(slot));
        }
        return total;
    }

    /** 四件护甲的当前能量之和（"综合累计"的可用量）。 */
    public static int totalEnergy(Player player) {
        if (player == null) {
            return 0;
        }
        int total = 0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            total += Math.min(getEnergy(player.getItemBySlot(slot)), maxOf(player.getItemBySlot(slot)));
        }
        return total;
    }

    /**
     * 把一次消耗按「四件平摊 + 余数顺延」拆成四份（<b>纯函数</b>，便于实测与关卡断言）。
     *
     * <p>例：{@code amount = 10} ⇒ {@code [3, 3, 2, 2]}（前 {@code 10 % 4 = 2} 件多 1）；
     * {@code amount = 4} ⇒ {@code [1, 1, 1, 1]}；{@code amount <= 0} ⇒ 全 0。</p>
     *
     * @return 长度 4 的数组，顺序 = 头 / 胸 / 腿 / 脚
     */
    public static int[] splitEvenly(int amount) {
        int[] parts = new int[ARMOR_SLOTS.size()];
        if (amount <= 0) {
            return parts;
        }
        int base = amount / parts.length;
        int rest = amount % parts.length;
        for (int i = 0; i < parts.length; i++) {
            parts[i] = base + (i < rest ? 1 : 0);
        }
        return parts;
    }

    /** 四件加起来够不够付这一次消耗。 */
    public static boolean canAfford(Player player, int amount) {
        return amount <= 0 || totalEnergy(player) >= amount;
    }

    /**
     * 扣一次装备技能的能耗：<b>四件平摊、余数顺延、全有或全无</b>。
     *
     * @return 是否成功扣款（false = 四件合计不足，<b>一件都没扣</b>）
     */
    public static boolean consume(Player player, int amount) {
        if (player == null) {
            return false;
        }
        if (amount <= 0) {
            return true;
        }
        if (!canAfford(player, amount)) {
            return false; // 全有或全无：先全检
        }
        int[] parts = splitEvenly(amount);
        for (int i = 0; i < parts.length; i++) {
            if (parts[i] <= 0) {
                continue;
            }
            ItemStack piece = player.getItemBySlot(ARMOR_SLOTS.get(i));
            int current = Math.min(getEnergy(piece), maxOf(piece));
            piece.set(ToolDataComponents.ENERGY, Math.max(0, current - parts[i]));
        }
        return true;
    }

    /**
     * 四件护甲上「减耗」附魔合成后的<b>能耗倍数</b>（1.0 = 无折扣）。
     *
     * <p>按件相乘（工具口径：等级 L ⇒ {@code 1 - 0.1L}，L≥5 已到 0.5），结果不低于
     * {@link #DISCOUNT_FLOOR}（总折扣封顶 50%）。</p>
     */
    public static double consumptionMultiplier(Player player) {
        return stackMultiplier(player, true);
    }

    /** 四件护甲上「迅启」附魔合成后的<b>冷却倍数</b>（口径同 {@link #consumptionMultiplier}）。 */
    public static double cooldownMultiplier(Player player) {
        return stackMultiplier(player, false);
    }

    private static double stackMultiplier(Player player, boolean consumption) {
        if (player == null) {
            return 1.0;
        }
        double multiplier = 1.0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack piece = player.getItemBySlot(slot);
            int level = consumption
                ? ToolEnchantments.reduceConsumptionLevel(piece)
                : ToolEnchantments.swiftStartLevel(piece);
            if (level > 0) {
                // 批 15：折扣除法只此一处（core 的 SkillEnergyCost#discountMultiplier = max(0.5, 1 - 0.1L)）。
                multiplier *= SkillEnergyCost.discountMultiplier(level);
            }
        }
        return Math.max(DISCOUNT_FLOOR, multiplier);
    }
}

package com.hjmmd_8.createoreexpansion.compat.curios;

import com.hjmmd_8.createoreexpansion.content.equipment.medallion.item.JadeStressMedallionItem;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.item.NetheriteStressMedallionItem;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.item.SapphireStressMedallionItem;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.item.StellarstoneStressMedallionItem;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.item.ThunderiteStressMedallionItem;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.item.TopazStressMedallionItem;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * 凝能佩的<b>饰品支线</b>物品类（{@code implements ICurioItem}）—— 只在装了 Curios 时被构造。
 *
 * <p><b>两支分家的口径</b>：纯物品支线 {@code content.equipment.medallion.*StressMedallionItem}
 * 不含任何 Curios 引用，任何时候都能存在（可合成、可持有、可绑定，只是没有饰品槽效果）；
 * 本类里的子类各自 {@code extends} 对应的纯物品支线类，只叠加"Curios 饰品契约"这一层，
 * 因此 {@code content/} 包里一个 Curios 类都不出现。注册 id 与显示名两支完全一致。</p>
 *
 * <p><b>加载约束</b>：本类直接引用 Curios 类型，只能由
 * {@link CurioMedallionBridge} 的 {@code create(...)} 触碰（其自身也只在 Curios 已安装时被
 * {@code Class.forName} 加载）。未装 Curios 时本文件永不进入 JVM。</p>
 */
public final class CurioMedallionItems {

	private CurioMedallionItems() {
	}

	/**
	 * 饰品支线共用契约：右键不自动装备到 Curios 槽位（右键保留给模式切换/绑定）——
	 * 与改前 {@code BaseStressMedallionItem#canEquipFromUse} 一字不差。
	 */
	public interface NoAutoEquip extends ICurioItem {

		@Override
		default boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
			return false;
		}
	}

	/** 翡翠凝能佩 · 饰品支线（行为全在父类 {@link JadeStressMedallionItem}）。 */
	public static final class Jade extends JadeStressMedallionItem implements NoAutoEquip {

		public Jade(Item.Properties properties) {
			super(properties);
		}
	}

	/** 黄玉凝能佩 · 饰品支线（行为全在父类 {@link TopazStressMedallionItem}）。 */
	public static final class Topaz extends TopazStressMedallionItem implements NoAutoEquip {

		public Topaz(Item.Properties properties) {
			super(properties);
		}
	}

	/** 沧蓝凝能佩 · 饰品支线（行为全在父类 {@link SapphireStressMedallionItem}）。 */
	public static final class Sapphire extends SapphireStressMedallionItem implements NoAutoEquip {

		public Sapphire(Item.Properties properties) {
			super(properties);
		}
	}

	/**
	 * 狱红怪佩 · 饰品支线：佩戴时永久抗火（时长 60 tick 续期，图标不闪烁）——
	 * 实现逐字照搬改前的 {@code NetheriteStressMedallionItem#curioTick}，
	 * 只是搬到了"只有装了 Curios 才会被加载"的这一支。
	 */
	public static final class Netherite extends NetheriteStressMedallionItem implements NoAutoEquip {

		public Netherite(Item.Properties properties) {
			super(properties);
		}

		@Override
		public void curioTick(SlotContext slotContext, ItemStack stack) {
			Entity entity = slotContext.entity();
			if (entity instanceof LivingEntity living && !living.level().isClientSide) {
				MobEffectInstance effect = living.getEffect(MobEffects.FIRE_RESISTANCE);
				if (effect == null || effect.getDuration() < 30) {
					living.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 0, false, false));
				}
			}
		}
	}

	/** 星辉凝能佩 · 饰品支线（被动效果见 {@code MedallionEffectHandler}）。 */
	public static final class Stellarstone extends StellarstoneStressMedallionItem implements NoAutoEquip {

		public Stellarstone(Item.Properties properties) {
			super(properties);
		}
	}

	/** 雷暴凝能佩 · 饰品支线（雷电吸收见 {@code MedallionEffectHandler}）。 */
	public static final class Thunderite extends ThunderiteStressMedallionItem implements NoAutoEquip {

		public Thunderite(Item.Properties properties) {
			super(properties);
		}
	}
}

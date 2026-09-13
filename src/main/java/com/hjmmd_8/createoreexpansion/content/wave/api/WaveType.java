package com.hjmmd_8.createoreexpansion.content.wave.api;

import net.minecraft.resources.ResourceLocation;

/**
 * <b>波型</b>：一个能量波"是什么波"的完整定义（波情四属性之一）。
 *
 * <p>本接口是<b>对扩展模组开放的稳定面</b>：实现它（或直接用 {@link SimpleWaveType}）并在
 * {@link WaveTypes#register} 注册，就得到一个可被本模组全部机制识别的新波型——
 * 例如"雷波"（只伤害、不加工）、"冰波"（会充能加工且不伤害）之类，<b>无需修改本模组源码</b>。</p>
 *
 * <p>本接口只描述<b>能力开关</b>与<b>表现风格</b>，不持有任何世界状态；波实体侧的
 * 状态读写见 {@code AbstractChargerWaveEntity#getWaveType()} 与 {@link WaveAttributes}。</p>
 *
 * <p><b>中立性约束</b>（本仓库 hard rule）：实现方与调用方都只允许使用中立类型
 * （{@code ResourceLocation} / 原版 / NeoForge / Create 基础类型），
 * <b>不得</b> import 任何可选联动模组（CC&amp;A / Vintage / Jade / JEI / Optical / Sable）的类。</p>
 */
public interface WaveType {

	/** 唯一 id（形如 {@code createoreexpansion:normal}）。 */
	ResourceLocation id();

	/**
	 * 是否具备"充能加工"能力（{@code createoreexpansion:charging} 配方）。
	 *
	 * <p>普通波的看家本领；全能波本身没有，但可在特定条件下（变器读取半径内有星辉石应力充能器）
	 * <b>临时</b>获得——那种放行由机制侧单独判断，不改这里返回的"本性"。</p>
	 */
	boolean allowsChargingProcessing();

	/** 是否具备"读取机器 → 远程加工"能力（扫描携带 / 候选门槛 / 链式）。 */
	boolean allowsRemoteProcessing();

	/** 是否对生物造成伤害（伤害值仍由波级决定，见 {@code WaveLevels#damage}）。 */
	boolean dealsDamage();

	/** 拖尾与绽放的视觉风格。 */
	WaveTrailStyle trailStyle();
}

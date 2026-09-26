package com.hjmmd_8.createoreexpansion.content.wave.block;

import net.minecraft.core.Direction;

/**
 * 八面能量波差器的 4 个 45° 斜向（对应模型斜板，世界水平对角方向）。
 */
public enum OctaCorner {

	NORTH_EAST(Direction.NORTH, Direction.EAST),
	NORTH_WEST(Direction.NORTH, Direction.WEST),
	SOUTH_EAST(Direction.SOUTH, Direction.EAST),
	SOUTH_WEST(Direction.SOUTH, Direction.WEST);

	private final Direction a;
	private final Direction b;

	OctaCorner(Direction a, Direction b) {
		this.a = a;
		this.b = b;
	}

	/** 该斜向含有的两个正交方向之一（用于判断命中面与相邻关系）。 */
	public Direction dirA() {
		return a;
	}

	public Direction dirB() {
		return b;
	}

	/** 该斜向的水平方位角（度，0=北，顺时针递增），供顶/底 8 扇区定位。 */
	public double angle() {
		return switch (this) {
			case NORTH_EAST -> 45;
			case NORTH_WEST -> -45;
			case SOUTH_EAST -> 135;
			case SOUTH_WEST -> -135;
		};
	}
}

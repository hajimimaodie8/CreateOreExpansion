package com.hjmmd_8.createoreexpansion.integration.skiller;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * 技能玩法开关的<b>存档</b>存储（服务端权威）。
 *
 * <h2>为什么不是配置文件</h2>
 * <p>用户明确要求：开关放在游戏内（MC 键位绑定界面 + 一个小设置界面）里，
 * <b>不要让玩家去改配置文件</b>；并且值要<b>存进存档</b>（跟着世界走，而不是跟着
 * 客户端/安装目录走）。因此这里用 {@link SavedData}：数据落在维度的
 * {@code data/createoreexpansion_skill_settings.dat}，由服务端读写。</p>
 *
 * <h2>为什么必须服务端权威</h2>
 * <p>它决定的是"创造模式释放技能要不要扣能量"——这是纯粹的<b>资源消耗规则</b>。
 * 若由客户端自己决定，作弊客户端只要说自己"要扣能量"就能白嫖，或者反过来。
 * 所以：客户端只发意愿（{@link SkillSettingsPayload}），
 * 服务端校验发送者身份后 {@link #setConsumeInCreative} 写存档，再广播回客户端显示。</p>
 *
 * <h2>默认值</h2>
 * <p>缺省 {@code true}（= 创造模式也消耗能量）。这与本模组<b>换核前</b>的玩法一致
 * （旧 {@code ToolEnergy.tryConsume} 无论创造与否都扣能），因此老存档/首次进世界
 * 不会因为本功能而突然改变手感。</p>
 *
 * <h2>存在主世界（一个存档一份值）</h2>
 * <p>开关描述的是"这个世界怎么玩"，不该按维度分裂成三份。因此虽然入口签名收
 * {@link ServerLevel}（调用方手上只有 {@code player.serverLevel()}），内部一律
 * <b>归一到主世界的 DataStorage</b>——在下界切换开关，回主世界依然是同一个值，
 * 玩家不会遇到"换个维度开关自己变了"。</p>
 *
 * <p><b>生效点唯一</b>：{@link CoeSkillRelease#releaseBundle} 里那一次
 * {@code consume = !player.isCreative() || SkillSettings.consumeInCreative(...)}。
 * 其它任何地方都不该再读这个值。</p>
 *
 * @since 1.0.0
 */
public final class SkillSettings {

    /** 存档文件名（{@code <世界>/data/createoreexpansion_skill_settings.dat}）。 */
    private static final String DATA_NAME = "createoreexpansion_skill_settings";

    /** NBT 键名 */
    private static final String KEY_CONSUME_IN_CREATIVE = "ConsumeInCreative";

    /**
     * 缺省值：<b>true = 创造模式也消耗能量</b>。
     *
     * <p>换核前旧实现（{@code ToolEnergy.tryConsume}）创造模式照扣，所以缺省取 true
     * 才是"行为不变"；只有玩家主动关掉开关才会变成创造模式免耗。</p>
     */
    public static final boolean DEFAULT_CONSUME_IN_CREATIVE = true;

    private SkillSettings() {
        throw new AssertionError("This class should not be instantiated");
    }

    /**
     * 读取开关值（缺省 {@link #DEFAULT_CONSUME_IN_CREATIVE}）。
     *
     * @param level 任意服务端维度（通常传 {@code player.serverLevel()}）；
     *              内部会归一到主世界，因此传哪个维度结果都一样
     * @return true = 创造模式释放技能同样消耗能量
     */
    public static boolean consumeInCreative(ServerLevel level) {
        Data data = data(level);
        return data == null ? DEFAULT_CONSUME_IN_CREATIVE : data.consumeInCreative;
    }

    /**
     * 写入开关值并标脏（下一次存档落盘）。
     *
     * @param level 任意服务端维度（通常传发送者自己的 {@code serverLevel()}）
     * @param value 新值
     */
    public static void setConsumeInCreative(ServerLevel level, boolean value) {
        Data data = data(level);
        if (data == null) {
            return;
        }
        data.consumeInCreative = value;
        data.setDirty();
    }

    /** 取主世界那份存档数据（一个存档一份值）；拿不到服务端时返回 null。 */
    private static Data data(ServerLevel level) {
        if (level == null || level.getServer() == null) {
            return null;
        }
        ServerLevel storage = level.getServer()
                .overworld();
        return storage.getDataStorage()
                .computeIfAbsent(Data.factory(), DATA_NAME);
    }

    /**
     * 实际承载数据的 {@link SavedData}（实例挂在主世界的 DataStorage 上，一个存档一份）。
     *
     * <p>做成嵌套类而不是独立文件：它只被本类使用，且字段语义与 {@link SkillSettings}
     * 的读写方法一一对应，放在一起更不容易看漏。</p>
     */
    public static final class Data extends SavedData {

        private boolean consumeInCreative = DEFAULT_CONSUME_IN_CREATIVE;

        public static SavedData.Factory<Data> factory() {
            return new SavedData.Factory<>(Data::new, Data::load);
        }

        @Override
        public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
            nbt.putBoolean(KEY_CONSUME_IN_CREATIVE, consumeInCreative);
            return nbt;
        }

        private static Data load(CompoundTag nbt, HolderLookup.Provider registries) {
            Data data = new Data();
            // 老存档/首次进世界没有这个键：保持缺省 true（行为与换核前一致）
            if (nbt.contains(KEY_CONSUME_IN_CREATIVE)) {
                data.consumeInCreative = nbt.getBoolean(KEY_CONSUME_IN_CREATIVE);
            }
            return data;
        }

        private Data() {
        }
    }
}

package com.hjmmd_8.createoreexpansion.common.registry;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.hjmmd_8.createoreexpansion.common.registry.cews.CewsRegistrate;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeRegistrate;
import com.hjmmd_8.createoreexpansion.common.registry.transmutation.TransmutationRegistrate;
import com.simibubi.create.api.registrate.CreateRegistrateRegistrationCallback;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateDataProvider;
import com.tterrag.registrate.providers.RegistrateProvider;

import net.minecraft.data.DataGenerator;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * <b>三个模块各自 Registrate 的共同基类</b>（P3b"Registrate 分家"的地基，住 SHARED 层）。
 *
 * <p>它解决"一个 mod 三个 Registrate"下<b>必须</b>解决、光改调用点解决不了的事。三层的
 * Registrate 命名空间<b>必须</b>都是 {@code createoreexpansion}（否则注册 id 全变），
 * 于是它们的 datagen 提供器、标签路径、语言路径全都会撞在一起；而 NeoForge 又只为
 * {@code --mod} 指定的那个 mod 执行生成器。逐条对策：</p>
 *
 * <h2>一、datagen 提供器：一层一个，但名字必须区分</h2>
 * <p>{@code RegistrateDataProvider#getName()} 只由 modid + 子提供器集合决定，三层完全相同 →
 * {@code DataGenerator.addProvider} 直接抛 {@code Duplicate provider}（实测）。
 * 所以由 {@code data/CreateOreExpansionDatagen} 用 {@link LayerDataProvider} 逐层挂提供器：
 * COE 不加前缀（名字与拆分前<b>逐字相同</b>），CEWS / TRANS 加 {@code [layer ...]} 前缀。
 * 同时三层的 {@code onData} 都保持空实现——NeoForge 只执行 {@code --mod} 那个 mod 的生成器，
 * 让它们往自己那三个<b>永远不会被执行的</b>生成器上挂提供器毫无意义。</p>
 *
 * <h2>二、共享路径的提供器只由主层写，且写三层并集</h2>
 * <p>绝大多数提供器的落盘路径带具体条目名（方块状态 / 物品模型 / 战利品表 / 配方），三层天然不重叠，
 * 各自在本层提供器里跑自己的回调即可。<b>例外</b>是两类路径<b>不含条目名</b>、三层会互相整文件覆盖：</p>
 * <ul>
 *   <li><b>标签</b>：{@code TagsProvider} 写 {@code data/<tag 的命名空间>/tags/...}——例如
 *       {@code minecraft:mineable/pickaxe} 由 COE 与 CEWS 同时贡献，后写者会覆盖先写者，标签丢一半；</li>
 *   <li><b>语言</b>：{@code RegistrateLangProvider} 写 {@code assets/createoreexpansion/lang/en_us.json}
 *       与 {@code en_ud.json}，三层各写一份同样会互相覆盖。</li>
 * </ul>
 * <p>对这两类，{@link #genData} 只在<b>主层（COE）</b>那个提供器上按
 * <b>COE → CEWS → TRANS</b> 的固定顺序跑<b>三层并集</b>，其余两层的同名子提供器保持为空（不写）。
 * 固定顺序不是随便定的：拆分前注册触发顺序就是 {@code CoeBlocks → CewsBlocks}、
 * {@code CoeItems → CewsItems → TransmutationItems}，按同样顺序拼接才能得到<b>完全相同的条目顺序</b>
 * （标签/语言文件是逐字节比对的）。</p>
 *
 * <h2>三、getDataProvider 为什么也要覆写</h2>
 * <p>Registrate 基类把提供器存在<b>私有</b>字段里、只在它自己的 {@code onData} 里赋值，
 * 而 {@code BlockBuilder.item()} 会在生成物品模型时回调
 * {@code getOwner().getDataProvider(ProviderType.BLOCKSTATE)} 去读本方块的模型名。
 * 因为三层的 {@code onData} 都是空实现，基类字段恒为 {@code null} → 会抛
 * {@code Cannot get data provider before datagen is started}（实测）。所以这里覆写
 * {@link #getDataProvider}，改从<b>本层自己记住的那个</b>提供器取——它就是挂在本层、
 * 会真正执行的那个实例，内部按 {@code Blockstates → Item models} 顺序跑，取 BLOCKSTATE 时已构建完毕。</p>
 *
 * <p>另注：战利品表<b>不能</b>跨层合并——{@code RegistrateBlockLootTables#getKnownBlocks()} 取的是
 * {@code parent.getAll(Registries.BLOCK)}（本层注册的方块），拿 COE 的提供器去跑 CEWS 的方块战利品表
 * 会抛 {@code Created block loot tables for non-blocks}（实测）。所以它就是上面"路径带条目名"那一类，
 * 各层各写各的。</p>
 */
public class LayerRegistrate extends CreateRegistrate {

    /** 并集遍历进行中标志：遍历时再进来的 {@code genData} 只跑"自己那一层"，避免无限递归。 */
    private static boolean mergingSharedData;

    /** 按固定顺序缓存的三层 Registrate（懒建，避免类初始化互相牵连）。 */
    private static List<CreateRegistrate> sharedOwners;

    /** 本层被挂到"会执行的生成器"上的那一个数据提供器（见 {@link #attachDataGenerator}）。 */
    @Nullable
    private RegistrateDataProvider attachedProvider;

    protected LayerRegistrate(String modid) {
        super(modid);
    }

    /**
     * 建一个分层 Registrate。
     *
     * <p><b>为什么不直接调 {@code CreateRegistrate.create(...)}</b>：那个静态工厂内部会调
     * {@code CreateRegistrateRegistrationCallback.provideRegistrate(...)}，而它<b>按 mod id 去重</b>
     * ——同一个 {@code createoreexpansion} 调第二次会直接抛
     * {@code IllegalArgumentException: Tried to register a duplicate CreateRegistrate instance for mod ID}。
     * 三个 Registrate 的命名空间参数<b>必须</b>都是 {@code createoreexpansion}（否则注册 id 全变），
     * 所以这里自己建实例：只让 COE 那个当"命名空间代表"去登记回调表
     * （本仓与 Create 都不消费该回调 API，登记与否对行为无影响，保留只是为了与拆分前的状态位对齐）。</p>
     *
     * @param namespace         注册命名空间，恒为 {@code createoreexpansion}
     * @param namespacePrimary  {@code true} = 本层实例代表该命名空间登记进 Create 的回调表（仅 COE）
     */
    public static LayerRegistrate create(String namespace, boolean namespacePrimary) {
        LayerRegistrate registrate = new LayerRegistrate(namespace);
        if (namespacePrimary) {
            CreateRegistrateRegistrationCallback.provideRegistrate(registrate);
        }
        return registrate;
    }

    /**
     * 三层 Registrate 的并集顺序（COE → CEWS → TRANS）。
     *
     * <p>懒建 + 每次现取字段，既保证顺序固定，又不会在某个 Registrate 的静态初始化过程中
     * 反过来触发另一层的静态初始化。</p>
     */
    private static List<CreateRegistrate> sharedOwners() {
        List<CreateRegistrate> owners = sharedOwners;
        if (owners == null) {
            owners = List.of(
                CoeRegistrate.REGISTRATE,
                CewsRegistrate.REGISTRATE,
                TransmutationRegistrate.REGISTRATE);
            sharedOwners = owners;
        }
        return owners;
    }

    /** "主层" = COE（命名空间代表）：共享路径的并集由它那个提供器负责写。 */
    private static CreateRegistrate primaryOwner() {
        return CoeRegistrate.REGISTRATE;
    }

    /** 落盘路径<b>不含条目名</b>、三层会互相整文件覆盖的那几类提供器（见类注释"二"）。 */
    private static boolean isSharedPathType(ProviderType<?> type) {
        return type == ProviderType.LANG
            || type == ProviderType.BLOCK_TAGS
            || type == ProviderType.ITEM_TAGS
            || type == ProviderType.FLUID_TAGS
            || type == ProviderType.ENTITY_TAGS
            || type == ProviderType.ENCHANTMENT_TAGS;
    }

    /**
     * 把本层的 datagen 提供器挂到<b>真正会执行</b>的那个生成器上（{@code createoreexpansion} 的），
     * 并记住它供 {@link #getDataProvider} 使用。
     *
     * @param registrate 本层 Registrate（必须是 {@link LayerRegistrate} 实例）
     * @param generator  目标生成器（createoreexpansion 的）
     * @param namespace  注册命名空间（恒为 {@code createoreexpansion}，只用于提供器标签）
     * @param layerId    层标签；{@code null} = 不加前缀（COE 保持与拆分前同名）
     * @param event      datagen 事件
     */
    public static void attachDataGenerator(CreateRegistrate registrate, DataGenerator generator,
                                           String namespace, @Nullable String layerId, GatherDataEvent event) {
        LayerRegistrate self = (LayerRegistrate) registrate;
        self.attachedProvider = new LayerDataProvider(registrate, namespace, layerId, event);
        generator.addProvider(true, self.attachedProvider);
    }

    @Override
    public <T extends RegistrateProvider> void genData(ProviderType<? extends T> type, T gen) {
        if (!isSharedPathType(type)) {
            // 路径带条目名：跑本层自己的回调即可（含战利品表，见类注释末段）。
            super.genData(type, gen);
            return;
        }
        if (mergingSharedData) {
            // 并集遍历中的子调用 → 只跑自己这一层。
            super.genData(type, gen);
            return;
        }
        if (this != primaryOwner()) {
            // 非主层：共享路径一律不写（否则会与主层的整文件覆盖打架）。
            return;
        }
        mergingSharedData = true;
        try {
            for (CreateRegistrate owner : sharedOwners()) {
                // 虚分派回到本覆写；此时 mergingSharedData 为真 → 各自只跑自己那一层的回调。
                owner.genData(type, gen);
            }
        } finally {
            mergingSharedData = false;
        }
    }

    /**
     * datagen 入口由 {@code data/CreateOreExpansionDatagen} 统一驱动（见类注释"一"），
     * 所以这里不自动挂 {@code RegistrateDataProvider}。
     *
     * <p>注意：<b>不</b>调 {@code super.onData(event)}——那会往"本 mod 自己的生成器"上塞提供器，
     * 而 CEWS / TRANS 的生成器根本不会被执行（NeoForge 只跑 {@code --mod} 那个）。</p>
     */
    @Override
    protected void onData(GatherDataEvent event) {
        // 故意为空：三层的提供器由 CreateOreExpansionDatagen 统一挂到 createoreexpansion 的生成器上。
    }

    /** 见类注释"三"：基类的私有字段永远是 null，这里改从本层挂上的那个提供器取子提供器。 */
    @Override
    public <P extends RegistrateProvider> Optional<P> getDataProvider(ProviderType<P> type) {
        RegistrateDataProvider provider = attachedProvider;
        if (provider != null) {
            return provider.getSubProvider(type);
        }
        return super.getDataProvider(type);
    }
}

package com.hjmmd_8.createoreexpansion.common.registry.coe;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.registry.transmutation.TransmutationItems;
import com.hjmmd_8.createoreexpansion.content.charger.recipe.ChargingRecipe;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.MedallionBindingRecipe;
import com.hjmmd_8.createoreexpansion.content.grinding.recipe.GrindingRecipe;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.transfer.FillingRecipe;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.kinetics.saw.CuttingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipeBuilder;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluids;

/**
 * <b>序列组装族（{@code create:sequenced_assembly} 9 条 + 1 条同目录的 {@code CustomRecipe}）的生成动作</b>
 * （配方迁移 批 7 —— 本轮<b>最后一族</b>，迁完之后全模组再无手写配方）。
 *
 * <p>本族原先<b>手写</b>在
 * {@code coe/src/main/resources/data/createoreexpansion/recipe/sequenced_assembly/}
 * （10 个文件），本批改由本类产出——产物落点从 {@code src/main/resources} 换到
 * {@code coe/src/generated/resources}（由 {@code LayerRecipeRouter} 按调用点绑层
 * {@code "coe"} 改道），<b>配方 id 逐字不变</b>。</p>
 *
 * <p><b>本族不是"一族同形"，是两种东西共用一个目录</b>：</p>
 * <ul>
 *   <li><b>9 条 {@code create:sequenced_assembly}</b>：4 条 {@code *_big_shard}（{@code loops=2}，
 *       锯切 → 压片 → 角磨，两个加权输出）+ 4 条 {@code *_budding_block}（{@code loops=4}，
 *       部署 ×2 → 本模组充能 ×3 → CC&amp;A 充能 ×1，单输出）+ 1 条 {@code transmute_mechanism}
 *       （{@code loops=3}，部署 ×2 → 本模组充能 → 注液，四个加权输出）。</li>
 *   <li><b>1 条 {@code createoreexpansion:medallion_binding}</b>：它<b>根本不是序列组装</b>
 *       ——{@link MedallionBindingRecipe} 是一个 {@code CustomRecipe}，codec 是
 *       {@code MapCodec.unit(...)}（无任何 JSON 字段），落在这个目录里纯粹是<b>历史误名</b>。
 *       ⚠ 它的 id {@code createoreexpansion:sequenced_assembly/medallion_binding}
 *       <b>一个字都不许改</b>：那是数据包契约（改了就动 id = 配方搬家）。</li>
 * </ul>
 *
 * <h2>⚠ 该族的"时长"判决（逐<b>步骤类型</b>，不是逐族）</h2>
 * <p>批 1 的教训：{@code ProcessingRecipe#validate()} 里有一条
 * {@code if (processingDuration &gt; 0 &amp;&amp; !canSpecifyDuration())}，而
 * {@code ProcessingRecipe#codec(...)} 把它包成了 {@code MapCodec#validate(...)}
 * ⇒ <b>编解码两个方向都跑它</b>；且 {@code ProcessingRecipeBuilder#build(RecipeOutput)} 里那句
 * {@code recipe.validate()} <b>只记 warning</b>，真正的硬失败发生在写盘时的
 * {@code encodeStart(...).getOrThrow()}。</p>
 * <p>本族与其它族的<b>关键不同</b>：时长是<b>每个步骤各自的</b>配方字段，
 * 而本族 10 条配方里<b>没有任何一步带时长</b>（{@code processing_time} 一律缺席
 * ⇒ {@code processingDuration = 0} ⇒ 上面那条守卫的 {@code &gt; 0} 分支根本不进）。
 * 所以"允不允许写时长"在本族是<b>惰性判决</b>，但每一个步骤类型仍然逐一核实过，
 * 记在这里以免日后有人"顺手补个时长"：</p>
 * <table border="1">
 *   <caption>步骤类型 → {@code canSpecifyDuration()} 核实结论</caption>
 *   <tr><th>步骤 type</th><th>注册类</th><th>{@code canSpecifyDuration()}</th><th>本族是否写时长</th></tr>
 *   <tr><td>{@code create:cutting}</td><td>{@code CuttingRecipe}</td><td><b>true</b>（覆写）</td><td>不写（0）</td></tr>
 *   <tr><td>{@code create:pressing}</td><td>{@code PressingRecipe}</td><td>false（默认）</td><td>不写</td></tr>
 *   <tr><td>{@code create:deploying}</td><td>{@code DeployerApplicationRecipe}</td><td>false（默认）</td><td>不写</td></tr>
 *   <tr><td>{@code create:filling}</td><td>{@code FillingRecipe}</td><td>false（默认）</td><td>不写</td></tr>
 *   <tr><td>{@code createoreexpansion:grinding}</td><td>{@link GrindingRecipe}</td><td><b>true</b>（覆写）</td><td>不写（0）</td></tr>
 *   <tr><td>{@code createoreexpansion:charging}</td><td>{@link ChargingRecipe}</td><td>false（默认）</td><td>不写</td></tr>
 *   <tr><td>{@code createaddition:charging}</td><td>{@code com.mrh0.createaddition.recipe.charging.ChargingRecipe}</td><td>false（默认）</td><td>不写</td></tr>
 * </table>
 * <p>⇒ 与手写版逐条对齐：<b>10 条里一个 {@code processing_time} 键都没有</b>。</p>
 *
 * <h2>⚠ 该族的"数据组件"判决</h2>
 * <p>本族<b>没有任何输出带数据组件</b>（{@code components} 键在手写版里 0 命中），所以批 3 那条
 * "{@code output(ItemStack)} 会静默丢组件"的坑在本族<b>不适用</b>（不是"绕过了"，是"本来就没有"）。
 * 但本族有<b>自己的一条同形陷阱</b>，必须走 {@code ItemStack} 重载：</p>
 * <p>⚠ {@link SequencedAssemblyRecipeBuilder#addOutput(net.minecraft.world.level.ItemLike, float)}
 * 内部是 {@code addOutput(new ItemStack(item), weight)} —— <b>{@code count} 恒为 1</b>。
 * 而 {@code *_big_shard} 的加权输出是 <b>3 个</b>与 <b>2 个</b>（不是 1 个）：
 * 走 {@code ItemLike} 重载会把 {@code count: 3} / {@code count: 2} <b>静默压成 1</b>
 * ——JSON 里那条 {@code count} 键直接消失，而且<b>静态关卡全绿</b>（键还在、只是值小了）。
 * ⇒ 本类一律走 {@code addOutput(ItemStack, float)}，由它取
 * {@code item.getCount()} 与 {@code item.getComponentsPatch()}。逐条核对见
 * {@link #bigShard} / {@link #transmuteMechanism}。</p>
 *
 * <h2>⚠ 该族的"镜像"判决</h2>
 * <p><b>不适用</b>：{@code create:sequenced_assembly} 继承层次里没有
 * {@code ShapedRecipe}（它是 {@code Recipe&lt;RecipeWrapper&gt;}），既没有
 * {@code accept_mirrored} 也没有 {@code show_notification} 字段。批 5 那条
 * "{@code disallowMirrored()} 会连带改 {@code showNotification}}"的坑在本族不存在。</p>
 *
 * <h2>⚠ 步骤工厂写法（逐类型；尤其两种 charging）</h2>
 * <p>{@code SequencedAssemblyRecipeBuilder} 有三个 {@code addStep} 重载，靠"工厂方法能不能接住
 * 目标描述符的参数类型"区分，实测（本批 compileJava）无歧义：</p>
 * <ol>
 *   <li><b>Create 的标准加工类</b>（{@code CuttingRecipe} / {@code PressingRecipe} /
 *       {@code FillingRecipe} / {@link GrindingRecipe}）：走<b>第一个</b>重载
 *       {@code addStep(StandardProcessingRecipe.Factory<R>, UnaryOperator<Builder<R>>)}，
 *       直接传类构造器 {@code XRecipe::new}。之所以不会误配到第二个重载
 *       （{@code ItemApplicationRecipe.Factory<R>}，其 {@code R extends ItemApplicationRecipe}）：
 *       这些类的构造器返回的类型<b>不</b>满足 {@code R extends ItemApplicationRecipe} 这条约束。</li>
 *   <li><b>{@code create:deploying}</b>：走<b>第二个</b>重载，{@code DeployerApplicationRecipe::new}
 *       ——它 {@code extends ItemApplicationRecipe}，而<b>不</b> {@code extends StandardProcessingRecipe}
 *       ⇒ 第一个重载反过来不适用。这是唯一一个"又一个物品压上去"的步骤类型，
 *       额外原料用 {@code b -&gt; b.require(item)} 追加（<b>占位物已由 addStep 自动置于 ingredients[0]</b>）。</li>
 *   <li><b>{@code createoreexpansion:charging}</b>：走<b>第三个</b>重载
 *       {@code addStep(Function&lt;ResourceLocation, B&gt;, UnaryOperator&lt;B&gt;)}，工厂传
 *       {@link #coeChargingBuilder}（内部 {@code new ChargingRecipe.Builder(id)}）。
 *       <b>为什么不能像别的类型那样用 {@code Serializer.factory()}</b>：本模组那个类的
 *       {@code ChargingRecipe.Serializer} <b>没有</b> {@code factory()} 访问器
 *       （它只 expose {@code codec()} / {@code streamCodec()}），而
 *       {@code ChargingRecipe.Builder} 的构造器<b>只需要一个 id</b>——所以直接 {@code new} 它。
 *       等级用 {@code b.withLevel(n)}（字段 {@code level}，codec
 *       {@code optionalFieldOf("level", 1)}）。</li>
 *   <li><b>{@code createaddition:charging}</b>：同样走第三个重载，工厂传
 *       {@link #caChargingBuilder}（内部
 *       {@code new com.mrh0.createaddition.recipe.charging.ChargingRecipe.Builder&lt;&gt;(ChargingRecipe::new, id)}
 *       ——CC&amp;A 的 Builder 是<b>泛型</b>且构造器<b>要一个 Factory</b>，与上一条不同）。
 *       {@code .energy(int)} / {@code .maxChargeRate(int)} 写 {@code energy} /
 *       {@code max_charge_rate}（CC&amp;A 的 {@code ChargingRecipeParams.CODEC} 对这两个键用
 *       {@code fieldOf}（<b>必填</b>，不是 {@code optionalFieldOf}）⇒ 一定落盘）。</li>
 * </ol>
 * <p>⛔⚠ <b>绝不要用 CC&amp;A 自带的 {@code ChargingRecipeBuilder}</b>：它的 {@code save}
 * <b>有损</b>（{@code new ChargingRecipe(new ChargingRecipeParams())} 之后直接 accept
 * ⇒ {@code ingredient} / {@code energy} / {@code maxChargeRate} <b>全丢</b>）。
 * 本类用上面那条"泛型 Builder + Factory 构造器"的写法，三个字段都在。</p>
 * <p>⛔ <b>不要用 Create 自带的 {@code SequencedAssemblyRecipeGen}</b>（也不是本模组那些
 * {@code *RecipeGen}）：它们自带 {@code PackOutput} 直接写盘，绕过
 * {@code LayerRecipeRouter} 的按调用点绑层 ⇒ 9 条落进根输出、三个模块 jar 里一条都没有（F1 红）。
 * 本类用纯 builder {@code SequencedAssemblyRecipeBuilder}，把配方交给参数里的 {@code output}。</p>
 *
 * <h2>⚠ 三条写配方的硬顺序 / 硬形态</h2>
 * <ol>
 *   <li><b>{@code transitionTo(...)} 必须在第一个 {@code addStep(...)} 之前</b>：
 *       {@code addStep} 内部读 {@code recipe.getTransitionalItem().getItem()} 来给每一步
 *       自动种上"占位物"原料与输出，而 {@code SequencedAssemblyRecipe} 的构造器
 *       <b>不</b>初始化 {@code transitionalItem} ⇒ 顺序反了会 NPE（不是静默出错）。</li>
 *   <li><b>{@code build(RecipeOutput)} 会自动加 {@code sequenced_assembly/} 前缀</b>
 *       （{@code AllRecipeTypes.SEQUENCED_ASSEMBLY.getId().getPath() + "/"}）⇒ 这里只传
 *       {@code CoeCore.modLoc("&lt;名&gt;")}，得到的 id 与手写路径逐字相同。
 *       <b>不要</b>自己再拼一次前缀（那样会变成 {@code sequenced_assembly/sequenced_assembly/...}）。</li>
 *   <li><b>{@code medallion_binding} 必须绕开上面那条前缀</b>：它不是
 *       {@code SequencedAssemblyRecipeBuilder} 能产出的东西，直接
 *       {@code output.accept(CoeCore.modLoc("sequenced_assembly/medallion_binding"), new MedallionBindingRecipe(CraftingBookCategory.MISC), null)}
 *       ——id 里那段 {@code sequenced_assembly/} 是<b>显式</b>给的（历史路径，见类注释）。</li>
 * </ol>
 *
 * <h2>等价口径：哪些键"手写写了、生成器不写"</h2>
 * <p>Create 的可选字段一律用 {@code optionalFieldOf(name, 默认值)} ⇒ <b>等于默认值就不落盘</b>。
 * 本族因此有<b>五类</b>"手写显式写出默认值、生成物省略"的差异，全部<b>语义等价</b>
 * （同一棵解析树）：</p>
 * <ol>
 *   <li>{@code results[].chance}：{@code optionalFieldOf("chance", 1F)} ⇒ 手写版 4 条
 *       {@code *_big_shard} 与 4 条 {@code *_budding_block} 里那些 {@code "chance": 1.0}
 *       在生成物里<b>不</b>出现（{@code transmute_mechanism} 的 0.95/0.02/0.01/0.02 都在）。</li>
 *   <li>{@code results[].count} / {@code transitional_item.count}：
 *       {@code optionalFieldOf("count", 1)} ⇒ 值 1 不落盘（手写版在 {@code transmute_mechanism}
 *       里显式写了 4 个 {@code "count": 1}）。</li>
 *   <li>{@code sequence[].level}：{@code optionalFieldOf("level", 1)} ⇒
 *       {@code jade_budding_block} / {@code topaz_budding_block} 的第一段
 *       {@code "level": 1} 不落盘（2~5 级照写）。</li>
 *   <li>{@code sequence[].processing_time} / {@code heat_requirement}：默认 0 / {@code NONE}
 *       ⇒ 一个都不落盘（手写版也没有这两个键）。</li>
 *   <li>{@code components}：{@code DataComponentPatch.EMPTY} 是默认值 ⇒ 不落盘（手写版也没有）。</li>
 * </ol>
 * <p>另外<b>键序</b>不同（{@code RecordCodecBuilder} 按字段声明序写：
 * {@code ingredient, transitional_item, sequence, results, loops}，手写版是
 * {@code ingredient, transitional_item, loops, sequence, results}），属于无意义的 JSON 空白差异。
 * 逐条深比（含每步的 type 与专属字段、results 的 count·chance·components）由本批的等价脚本负责。</p>
 *
 * <h2>provider 落点选择（为什么住本包、而不是 {@code compat/createaddition/coe/}）</h2>
 * <p>批 6 把 rolling 族放进 {@code compat/createaddition/coe/}，理由是"那一族<b>整族</b>的配方类型
 * 都来自第三方"。本族不同：<b>10 条配方里只有一步的类型来自第三方</b>（4 条
 * {@code *_budding_block} 各一步 + {@code transmute_mechanism} 一步 = 5 个步骤），
 * 另外 9 条配方的宿主类型与全部 10 条的 id 都是本模组的。把整族放进
 * {@code compat/createaddition/} 会错误地暗示"这 10 条都依赖 CC&amp;A"，
 * 还会把一次等价证明的单位拆到两个包。而 CC&amp;A 对 {@code :coe} 是
 * <b>{@code type = "required"}</b>（{@code coe/src/main/templates/META-INF/neoforge.mods.toml}），
 * 所以在本包 import 它不是"可选依赖泄漏"（那条红线禁的是 {@code content/} 里的可选模组类）。
 * ⇒ 落点选 {@code common/registry/coe/}，与另外 11 个 provider 同包。</p>
 *
 * <p><b>调用方</b>：{@link CoeRecipeProvider#generate} 的末尾（批 1~6 十一族之后）。
 * 跟着它收到的同一个 {@code RecipeOutput} 走 = 自动拿到「本层 coe」的落点归属，
 * 根工程的 {@code data/RecipeProvider} 一行都不用改（红线：根 {@code src} 不许动）。</p>
 */
public final class CoeSequencedAssemblyRecipeProvider {

    /**
     * 本族全部 10 条配方。调用顺序<b>无契约</b>（每条一个文件、互不覆盖），
     * 这里按"大碎片 → 晶芽块 → 嬗化构件 → 凝能佩绑定"排列，便于与手写目录逐条对照。
     */
    public static void generate(RecipeOutput output) {
        // ================= 4 条大碎片：粗矿 → loops=2 ×（锯切 → 压片 → 角磨）→ 3 个 / 2 个 =================
        bigShard(output, "jade_big_shard", CoeItems.RAW_JADE.get(), CoeItems.JADE_BIG_SHARD.get());
        bigShard(output, "topaz_big_shard", CoeItems.RAW_TOPAZ.get(), CoeItems.TOPAZ_BIG_SHARD.get());
        bigShard(output, "sapphire_big_shard", CoeItems.RAW_SAPPHIRE.get(), CoeItems.SAPPHIRE_BIG_SHARD.get());
        bigShard(output, "stellarstone_big_shard", CoeItems.RAW_STELLARSTONE.get(),
            CoeItems.STELLARSTONE_BIG_SHARD.get());

        // ================= 4 条晶芽块：石头/下界岩/末地石 → loops=4 ×（部署 ×2 → 充能 ×4） =================
        // 最后一个参数是那三段 createoreexpansion:charging 的起始等级（1/2/3 起，各占 3 连级）。
        buddingBlock(output, "jade_budding_block", Items.STONE, CoeItems.JADE_INGOT.get(),
            CoeBlocks.JADE_BUDDING_BLOCK.get(), 1);
        buddingBlock(output, "topaz_budding_block", Items.STONE, CoeItems.TOPAZ_INGOT.get(),
            CoeBlocks.TOPAZ_BUDDING_BLOCK.get(), 1);
        buddingBlock(output, "sapphire_budding_block", Items.NETHERRACK, CoeItems.SAPPHIRE_INGOT.get(),
            CoeBlocks.SAPPHIRE_BUDDING_BLOCK.get(), 2);
        buddingBlock(output, "stellarstone_budding_block", Items.END_STONE, CoeItems.STELLARSTONE_INGOT.get(),
            CoeBlocks.STELLARSTONE_BUDDING_BLOCK.get(), 3);

        // ================= 1 条嬗化构件：星辉石板 → loops=3 ×（部署 ×2 → 充能 → 注液） =================
        transmuteMechanism(output);

        // ================= 1 条凝能佩绑定：不是序列组装，只有 type 键（历史路径，id 不许改） =================
        medallionBinding(output);
    }

    /**
     * 一条大碎片配方：{@code loops=2}，每轮 {@code 锯切 → 压片 → 角磨}，
     * 终结输出是两个<b>权重相同</b>的条目（3 个 / 2 个，都是必得 {@code chance=1}）。
     *
     * <p>占位物（{@code transitional_item}）就是粗矿本身，与手写版逐字相同；每一步的
     * {@code ingredients} 只有占位物一项（由 {@code addStep} 自动种上），
     * {@code results} 也只有占位物一项。</p>
     *
     * <p>⚠ 两个输出都必须走 {@code addOutput(ItemStack, float)}：{@code count} 是 3 与 2
     * （不是 1），{@code ItemLike} 重载会把它们静默压成 1（见类注释"数据组件"段）。
     * 权重按手写版的 {@code "chance": 1.0} 传 {@code 1.0F}；两个都是 1.0
     * ⇒ {@code rollResult} 的 {@code totalWeight=2} 下各占一半，与手写版一致。</p>
     */
    private static void bigShard(RecipeOutput output, String name, ItemLike raw, ItemLike shard) {
        new SequencedAssemblyRecipeBuilder(CoeCore.modLoc(name))
            .require(raw)
            .transitionTo(raw)
            .loops(2)
            .addStep(CuttingRecipe::new, b -> b)
            .addStep(PressingRecipe::new, b -> b)
            .addStep(GrindingRecipe::new, b -> b)
            .addOutput(new ItemStack(shard, 3), 1.0F)
            .addOutput(new ItemStack(shard, 2), 1.0F)
            .build(output);
    }

    /**
     * 一条晶芽块配方：{@code loops=4}，每轮
     * {@code 部署(幸运粉尘) → 部署(对应锭) → 本模组充能(base) → 充能(base+1) → 充能(base+2) → CC&amp;A 充能}，
     * 终结输出恒 1 个晶芽块（{@code chance=1}）。
     *
     * <p>{@code base} 逐条不同（翡翠/黄玉 = 1，蓝宝石 = 2，星辉石 = 3），与手写版的三段 {@code level} 逐条对齐：
     * 1/2/3、1/2/3、2/3/4、3/4/5。<b>⚠ 别把四个都写成同一组</b>——那是内容变化，
     * 而且 {@code level} 是"玩法语义"字段（决定哪台充能器能完成这一步）。</p>
     *
     * <p>CC&amp;A 那一步的 {@code energy=1000} / {@code max_charge_rate=200} 四条<b>全部相同</b>，
     * 与手写版一致；两个键在 CC&amp;A 的 codec 里是 {@code fieldOf}（必填）⇒ 一定落盘。</p>
     */
    private static void buddingBlock(RecipeOutput output, String name, ItemLike base, ItemLike ingot,
                                     ItemLike result, int chargingBase) {
        new SequencedAssemblyRecipeBuilder(CoeCore.modLoc(name))
            .require(base)
            .transitionTo(base)
            .loops(4)
            .addStep(DeployerApplicationRecipe::new, b -> b.require(CoeItems.LUCKY_DUST.get()))
            .addStep(DeployerApplicationRecipe::new, b -> b.require(ingot))
            .addStep(CoeSequencedAssemblyRecipeProvider::coeChargingBuilder, b -> b.withLevel(chargingBase))
            .addStep(CoeSequencedAssemblyRecipeProvider::coeChargingBuilder, b -> b.withLevel(chargingBase + 1))
            .addStep(CoeSequencedAssemblyRecipeProvider::coeChargingBuilder, b -> b.withLevel(chargingBase + 2))
            .addStep(CoeSequencedAssemblyRecipeProvider::caChargingBuilder, b -> b.energy(1000).maxChargeRate(200))
            .addOutput(result, 1.0F)
            .build(output);
    }

    /**
     * 那唯一一条 {@code transmute_mechanism}：{@code loops=3}，每轮
     * {@code 部署(蓝宝石粒) → 部署(Create 齿轮) → 本模组充能(3 级) → 注 500mB 岩浆}，
     * 终结输出是<b>四个加权条目</b>（合计权重 1.00，逐条见下）。
     *
     * <p>⚠ <b>本条的每一步都显式覆盖了 {@code ingredients[0]} 与 {@code results}
     * —— 这是本族唯一的例外，必须照抄手写值</b>。另外 8 条配方的占位物（{@code transitional_item}）
     * <b>就是</b>主原料（粗矿 / 石头 / 下界岩 / 末地石），而 {@code SequencedAssemblyRecipeBuilder#addStep}
     * 会自动给每一步种上"占位物"作为 {@code ingredients[0]} 与唯一 {@code results}
     * —— 恰好等于手写值。本条不同：占位物是
     * {@code createoreexpansion:incomplete_transmute_mechanism}，但手写 JSON 里
     * <b>四步的 {@code ingredients[0]} 与 {@code results} 写的都是主原料
     * {@code createoreexpansion:stellarstone_sheet}</b>。所以自动种子与本条手写值不一致，
     * 必须用 {@code withItemIngredients(...)} / {@code withItemOutputs(...)}
     * （<b>替换</b>整个列表）显式改回来。</p>
     *
     * <p><b>为什么这不是"顺手修好"而是"照抄"</b>：这两个位置在运行期<b>都会被覆盖</b>
     * ——{@code SequencedRecipe#initFromSequencedAssembly}（解码时按 j==0 把 index 0 换成
     * 占位物，或 {@code CompoundIngredient(占位物, 主原料)}）与
     * {@code ProcessingRecipe#enforceNextResult}（匹配时把结果换成
     * {@code SequencedAssemblyRecipe#advance} 的返回值 = 占位物）。⇒ 手写值与生成器的自动种子
     * 在游戏里<b>完全等价</b>，手写那四个 {@code stellarstone_sheet} 是<b>惰性数据</b>。
     * 本批的红线是"行为零变化"且验收要求逐条深度等价，所以这里照抄手写值：
     * 两种写法行为相同，照抄则语义比较（含每步 ingredients/results）也能逐字段通过。
     * <b>⚠ 别把这两句删掉当作"清理"</b>：那会把 9 条里的 1 条从"逐字段等价"降级成
     * "靠运行期覆盖才等价"，正是本批要避免的。</p>
     *
     * <h2>四个输出逐条核对（顺序 = {@code resultPool} 顺序 = 权重掷取顺序）</h2>
     * <ol>
     *   <li>{@code createoreexpansion:transmute_mechanism} × 1，{@code chance=0.95}（主产物，
     *       也是 {@code getResultItem} 读的第一项）；</li>
     *   <li>{@code createoreexpansion:stellarstone_sheet} × 1，{@code chance=0.02}（返还板材）；</li>
     *   <li>{@code create:cogwheel} × 1，{@code chance=0.01}（返还齿轮）；</li>
     *   <li>{@code createoreexpansion:sapphire_nugget} × 1，{@code chance=0.02}（返还蓝宝石粒）。</li>
     * </ol>
     * <p>{@code count} 一律 1（= codec 默认值 ⇒ 生成物里不写该键，手写版显式写了，语义等价）；
     * 四个 {@code chance} 都<b>不等于</b>默认值 1F ⇒ 四个都会落盘。
     * 注液那一步用 {@code FillingRecipe} + {@code b.require(Fluids.LAVA, 500)}
     * （{@code ProcessingRecipeBuilder#require(FlowingFluid, int)}）⇒ 序列化成
     * {@code {"type":"neoforge:single","amount":500,"fluid":"minecraft:lava"}}，
     * 与手写版逐字段相同（item 原料在 {@code ingredients[0]}、流体在其后，
     * 与 {@code ProcessingRecipeParams#ingredients()} 的"先物品后流体"顺序一致）。
     * ⚠ 那一步的 {@code results} 必须用 {@code withItemOutputs(...)}（替换）而<b>不是</b>
     * {@code output(...)}（追加）：{@code FillingRecipe#getMaxOutputCount()} 是 <b>1</b>，
     * 追加会让 {@code validate()} 报"more item outputs than supported"⇒ 写盘直接抛。</p>
     */
    private static void transmuteMechanism(RecipeOutput output) {
        // 手写版这四步用的"被加工物"是主原料，不是占位物（见上）。照抄。
        Ingredient sheet = Ingredient.of(CoeItems.STELLARSTONE_SHEET.get());
        ProcessingOutput sheetResult = new ProcessingOutput(CoeItems.STELLARSTONE_SHEET.get(), 1, 1F);
        new SequencedAssemblyRecipeBuilder(CoeCore.modLoc("transmute_mechanism"))
            .require(CoeItems.STELLARSTONE_SHEET.get())
            .transitionTo(TransmutationItems.INCOMPLETE_TRANSMUTE_MECHANISM.get())
            .loops(3)
            .addStep(DeployerApplicationRecipe::new, b -> b
                .withItemIngredients(sheet, Ingredient.of(CoeItems.SAPPHIRE_NUGGET.get()))
                .withItemOutputs(sheetResult))
            .addStep(DeployerApplicationRecipe::new, b -> b
                .withItemIngredients(sheet, Ingredient.of(AllBlocks.COGWHEEL.get()))
                .withItemOutputs(sheetResult))
            .addStep(CoeSequencedAssemblyRecipeProvider::coeChargingBuilder, b -> b
                .withLevel(3)
                .withItemIngredients(sheet)
                .withItemOutputs(sheetResult))
            .addStep(FillingRecipe::new, b -> b
                .withItemIngredients(sheet)
                .withItemOutputs(sheetResult)
                .require(Fluids.LAVA, 500))
            .addOutput(new ItemStack(TransmutationItems.TRANSMUTE_MECHANISM.get()), 0.95F)
            .addOutput(new ItemStack(CoeItems.STELLARSTONE_SHEET.get()), 0.02F)
            .addOutput(new ItemStack(AllBlocks.COGWHEEL.get()), 0.01F)
            .addOutput(new ItemStack(CoeItems.SAPPHIRE_NUGGET.get()), 0.02F)
            .build(output);
    }

    /**
     * 那条 {@code medallion_binding}：{@link MedallionBindingRecipe} 是 {@code CustomRecipe}
     * （匹配/合成/返还全在代码里），它的序列化器 codec 是 {@code MapCodec.unit(...)}
     * ⇒ 生成的 JSON 只有 {@code type} 一个键。
     *
     * <p>⚠ <b>id 里的 {@code sequenced_assembly/} 是历史误名，一个字都不许改</b>：
     * 手写文件的相对路径就是 {@code recipe/sequenced_assembly/medallion_binding.json}，
     * 所以配方 id 是 {@code createoreexpansion:sequenced_assembly/medallion_binding}。
     * 改掉这段路径 = 改数据包契约（{@code /recipe give} 过的 id、存档里的引用全失配），
     * 与"这条不是序列组装"这个事实无关。<b>所以这里显式拼全路径</b>，
     * <b>不</b>走 {@code SequencedAssemblyRecipeBuilder#build(RecipeOutput)} 那条自动前缀。</p>
     */
    private static void medallionBinding(RecipeOutput output) {
        output.accept(CoeCore.modLoc("sequenced_assembly/medallion_binding"),
            new MedallionBindingRecipe(CraftingBookCategory.MISC), null);
    }

    /**
     * {@code createoreexpansion:charging} 的步骤工厂（第三个 {@code addStep} 重载用）。
     *
     * <p>为什么需要这个方法：本模组 {@link ChargingRecipe.Serializer} <b>没有</b>
     * {@code factory()} 访问器（对比 {@code StandardProcessingRecipe.Serializer}），
     * 而 {@link ChargingRecipe.Builder} 的构造器只吃一个 id ⇒ 这里直接 {@code new}，
     * 由 {@code Factory} 之外的那条重载接住。方法的返回类型<b>写全</b>
     * （不用 lambda 内联）是为了让重载解析毫无歧义。</p>
     */
    private static ChargingRecipe.Builder coeChargingBuilder(ResourceLocation id) {
        return new ChargingRecipe.Builder(id);
    }

    /**
     * {@code createaddition:charging} 的步骤工厂（同样给第三个 {@code addStep} 重载）。
     *
     * <p>CC&amp;A 的 Builder 与上面那个<b>形状不同</b>：它<b>是泛型</b>，且构造器签名是
     * {@code (Factory&lt;R&gt;, ResourceLocation)} —— 所以这里要把
     * {@code ChargingRecipe::new} 当作 Factory 一起传进去（写显式类型参数，
     * 免得 {@code &lt;&gt;} 的钻石推断在方法引用上出岔）。</p>
     *
     * <p>⚠ 返回类型里的 {@code com.mrh0.createaddition.recipe.charging.ChargingRecipe} 与
     * 本模组的 {@link ChargingRecipe} <b>同名不同类</b>：本类 import 的是本模组那个，
     * CC&amp;A 那个一律写全限定名（Java 没有 import 别名）。</p>
     */
    private static com.mrh0.createaddition.recipe.charging.ChargingRecipe.Builder<com.mrh0.createaddition.recipe.charging.ChargingRecipe>
    caChargingBuilder(ResourceLocation id) {
        return new com.mrh0.createaddition.recipe.charging.ChargingRecipe.Builder<>(
            com.mrh0.createaddition.recipe.charging.ChargingRecipe::new, id);
    }

    private CoeSequencedAssemblyRecipeProvider() {}
}

package com.hjmmd_8.createoreexpansion.common.registry.coe;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.compat.createaddition.coe.CoeRollingRecipeProvider;
import com.hjmmd_8.createoreexpansion.content.grinding.recipe.DismantlingRecipe;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import net.neoforged.neoforge.common.conditions.ICondition;

/**
 * <b>COE（矿物拓展）自己的配方生成动作</b>（P3c：从 {@code data/RecipeProvider} 拆出）。
 *
 * <p>本层负责<b>拆磨配方</b>（{@code createoreexpansion:dismantling/...}）：原版 4 组装备
 * （钻/铁/金/下界合金，每组 9 条）+ 本模组 5 组工具（翡翠/黄玉/蓝宝石/星辉石/雷鸣，每组 5 条），
 * 共 61 条——三级角磨轮专属。</p>
 *
 * <p><b>调用方</b>：{@code data/RecipeProvider}（SHARED 层的协调入口，也是唯一挂到
 * {@code DataGenerator} 上的那个提供器）。它按"拆分前 {@code buildRecipes} 里的<b>逐条顺序</b>"
 * 调用本类，所以配方文件的生成顺序与拆分前逐字相同。</p>
 *
 * <p><b>为什么生成顺序要紧</b>：配方落盘的文件名带条目名（互不覆盖），但 {@code runData} 的
 * 产物清单（{@code src/generated}）是要逐条目比对的——保持调用顺序就是保持产物的<b>集合与顺序</b>
 * 都不变，这是本轮"产物零变化"的硬指标之一。</p>
 *
 * <p><b>配方迁移 批 1（手写 → 生成器）：本类还转发另外两族。</b>
 * {@link CoeGrindingRecipeProvider}（角磨 9 条）与 {@link CoeLightningBlockRecipeProvider}
 * （方块雷击 1 条）原先手写在 {@code coe/src/main/resources}，本批起由生成器产出，
 * 并由本方法在末尾（拆磨 61 条之后）转发——<b>拆磨那 61 条之间的相对顺序一字未动</b>。</p>
 *
 * <p><b>配方迁移 批 2：再转发 Create 标准类型的四族，共 29 条</b>（{@link CoePressingRecipeProvider}
 * 4 + {@link CoeSplashingRecipeProvider} 4 + {@link CoeCuttingRecipeProvider} 8 +
 * {@link CoeCrushingRecipeProvider} 13），同样追加在末尾、同样用参数里的 output。
 * <b>⚠ 批 1 的教训</b>：批 0 把驼峰 {@code processingTime} 改成 {@code processing_time} 之后，
 * 凡是"有 {@code processing_time} 键、但该配方类型没覆写
 * {@code canSpecifyDuration()} 返回 true"的配方都会被 {@code ProcessingRecipe#validate()}
 * 整条丢弃（编解码两个方向都跑这句）。所以迁移每一族前，必须先核实该类型的
 * {@code canSpecifyDuration()}：pressing / splashing 是<b>默认 false</b>（不许写时长），
 * cutting / crushing 覆写为 <b>true</b>（必须写，分别 100 与 350/400/450）。</p>
 *
 * <p><b>配方迁移 批 3：再转发两族 COE 自有类型，共 23 条</b>（{@link CoeLightningRecipeProvider} 8 +
 * {@link CoeTransmutingRecipeProvider} 15），同样追加在末尾、同样用参数里的 output。
 * 两者都<b>不</b>覆写 {@code canSpecifyDuration()}（默认 {@code false}）⇒ 一律不许写时长，
 * 手写版也确实没有 {@code processing_time} 键；雷击那 5 条充能配方的输出带
 * {@code createoreexpansion:energy} 数据组件（5000 / 凝能佩 10000），走
 * {@code output(ItemStack)} 才不丢组件。逐族结论写在各自类的 javadoc 里。</p>
 *
 * <p><b>配方迁移 批 4：再转发原版合成族，共 26 条</b>（{@link CoeCraftingRecipeProvider}：
 * 14 条 {@code minecraft:crafting_shaped} + 12 条 {@code minecraft:crafting_shapeless}），
 * 同样追加在末尾、同样用参数里的 output。本族是<b>唯一</b>不走 Create 配方类型的族
 * （直接用原版 {@code ShapedRecipe} / {@code ShapelessRecipe} 构造器），因此它<b>不受</b>
 * 批 1 那条"{@code canSpecifyDuration()}"约束——原版合成配方根本没有时长字段。
 * 但本族有<b>两个自己的陷阱</b>，都写在 {@link CoeCraftingRecipeProvider} 的类注释里：
 * ① id 的 {@code crafting/materials/} 前缀必须显式写（原版 builder 的默认 id 不带它 ⇒
 * 不显式给就等于配方改名 = 搬家）；② 不许用 builder 的 {@code save(output, id)}——它会额外
 * 产出 {@code data/<ns>/advancement/recipes/**}，而 {@code LayerRecipeRouter} 只改道
 * {@code data/<ns>/recipe/} ⇒ 那些 advancement 会留在根输出、静默消失且不被 F1 抓到；
 * 正确形态是 {@code output.accept(id, recipe, null)}（{@code advancement = null}）。</p>
 *
 * <p><b>配方迁移 批 5：再转发 Create 专用类型的动力合成族，共 5 条</b>
 * （{@link CoeMechanicalCraftingRecipeProvider}：5 条 {@code create:mechanical_crafting}，
 * 翡翠剑/镐/斧/锹/锄），同样追加在末尾、同样用参数里的 output。本族三条特有结论：</p>
 * <ol>
 *   <li><b>类型面</b>：必须用 {@code MechanicalCraftingRecipeBuilder}（纯 builder，把配方交给
 *       调用方给的 {@code RecipeOutput}）；<b>不能</b>用 {@code MechanicalCraftingRecipeGen}
 *       ——后者自带 {@code PackOutput} 直接写盘，绕过按调用点绑层 ⇒ 5 条落进根输出、
 *       三个模块 jar 里一条都没有（F1 立刻红）。</li>
 *   <li><b>时长</b>：{@code MechanicalCraftingRecipe extends ShapedRecipe}，
 *       <b>不是</b> {@code ProcessingRecipe} ⇒ 批 1 那条
 *       {@code canSpecifyDuration()} 约束<b>不适用</b>（该类型继承层次里没有这个方法，
 *       也没有 {@code processing_time} 字段）。手写版 5 条确实一条都没有该键。</li>
 *   <li><b>镜像</b>：{@code disallowMirrored()} 把 {@code acceptMirrored=false}
 *       <b>同时</b>当作 {@code ShapedRecipe} 的 {@code showNotification} 参数传下去 ⇒
 *       一个调用同时产出 {@code accept_mirrored=false} 与 {@code show_notification=false}，
 *       与手写版逐字段一致。详见 {@link CoeMechanicalCraftingRecipeProvider} 的类注释。</li>
 * </ol>
 *
 * <p><b>配方迁移 批 6：最后转发第三方 CC&amp;A 类型的轧制族，共 8 条</b>
 * （{@link CoeRollingRecipeProvider}：8 条 {@code createaddition:rolling}，四种宝石
 * × {锭 → 杆, 板 → 线}），同样追加在末尾、同样用参数里的 output。本族是本轮<b>唯一</b>
 * 一个配方类型来自<b>第三方模组</b>的族（{@code com.mrh0.createaddition.recipe.rolling.RollingRecipe}），
 * 因此它的 provider 住 {@code compat/createaddition/coe/} 而不是本包，见该类注释的取舍说明。
 * 本族三条特有结论：</p>
 * <ol>
 *   <li><b>时长</b>：{@code RollingRecipe extends StandardProcessingRecipe<RecipeWrapper>}，
 *       <b>是</b> {@code ProcessingRecipe} 那条链上的类型，而
 *       {@code ProcessingRecipe#canSpecifyDuration()} 的默认实现是 {@code false}、
 *       {@code StandardProcessingRecipe} 与 {@code RollingRecipe} <b>都没有</b>覆写它
 *       ⇒ 批 1 那条约束<b>适用</b>，结论是<b>"核实为 false ⇒ 绝不许写"</b>
 *       （与批 5 的"链上根本没有这个方法"是两种不同的判决，别混）。</li>
 *   <li><b>组件</b>：8 条产物都是普通物品（杆 / 线），手写版 {@code results} 里没有
 *       {@code components} 键，走 {@code output(ItemLike, int)} 得到的是空
 *       {@code DataComponentPatch} ⇒ 批 3 那条"{@code output(ItemStack)} 会静默丢组件"的坑
 *       在本族<b>不适用</b>（不是"绕过了"，是"本来就没有组件"）。</li>
 *   <li><b>类型面</b>：必须用纯 builder（{@code StandardProcessingRecipe.Builder}），
 *       <b>不能</b>用 CC&amp;A 自带的 {@code RollingRecipeGen}——后者自带 {@code PackOutput}
 *       直接写盘，与批 5 被否掉的 {@code MechanicalCraftingRecipeGen} 同款形状。</li>
 * </ol>
 *
 * <p><b>配方迁移 批 7（最后一族）：再转发序列组装族，共 10 条</b>
 * （{@link CoeSequencedAssemblyRecipeProvider}：9 条 {@code create:sequenced_assembly}
 * + 1 条同目录下的 {@code createoreexpansion:medallion_binding}），同样追加在末尾、
 * 同样用参数里的 output。<b>本族是迁移的最后一族——转完这一族，全模组再无手写配方。</b>
 * 本族四条特有结论（逐条细节见该类的 javadoc）：</p>
 * <ol>
 *   <li><b>时长是"每步各自"的，不是每族一个</b>：本族 10 条配方里<b>没有任何一步</b>带时长
 *       （{@code processingDuration = 0}），所以批 1 那条
 *       {@code processingDuration &gt; 0 &amp;&amp; !canSpecifyDuration()} 的守卫
 *       <b>一个分支都进不去</b>；但七个步骤类型的 {@code canSpecifyDuration()} 仍已逐一核实
 *       （cutting / grinding 是 true，其余五个是默认 false）并写在该类里。</li>
 *   <li><b>组件</b>：本族没有任何输出带 {@code components}，批 3 那条坑不适用；
 *       但有一条<b>同形陷阱</b>——{@code addOutput(ItemLike, float)} 把 {@code count} 钉死为 1，
 *       而 {@code *_big_shard} 的输出是 3 个与 2 个，必须走 {@code addOutput(ItemStack, float)}。</li>
 *   <li><b>镜像</b>：{@code sequenced_assembly} 不是 {@code ShapedRecipe}，
 *       没有 {@code accept_mirrored} / {@code show_notification} ⇒ 批 5 那条坑不适用。</li>
 *   <li><b>类型面</b>：必须用纯 builder {@code SequencedAssemblyRecipeBuilder}（把配方交给
 *       调用方给的 {@code RecipeOutput}），<b>不能</b>用 Create 的
 *       {@code SequencedAssemblyRecipeGen}（自带 {@code PackOutput} 直接写盘，
 *       与批 5/6 被否掉的两个 {@code *RecipeGen} 同款形状）。</li>
 * </ol>
 *
 * <p><b>为什么转发挂在这里、而不是根工程的 {@code buildRecipes}</b>：层的归属由
 * {@code LayerRecipeRouter} 在<b>调用点</b>绑定，而根工程的调用点已经把
 * {@code CoeRecipeProvider.generate} 收到的 {@code RecipeOutput} 包成了「本层 coe」的
 * （{@code bind(output, "coe")}）。所以只要跟着<b>同一个 output</b> 往下写，落点自然是
 * {@code coe/src/generated/resources}——根工程那一行调用<b>不用改</b>（红线：根 {@code src} 不动）。
 * 反过来说：<b>本类里这两个 generate 必须用参数里的 output，绝不能自己另造一个
 * {@code RecipeOutput}</b>，否则会绕过绑层、产物落进根工程的 generated 目录
 * （{@code check-module-selfsufficiency} 的 F1 立刻红）。</p>
 */
public final class CoeRecipeProvider {

    /**
     * 本层的配方生成入口：拆磨 61 条（调用顺序与拆分前的 {@code buildRecipes} 逐字相同），
     * 末尾再转发配方迁移七批的十二族（批 1：角磨 9 + 方块雷击 1；批 2：压片 4 + 洗涤 4 +
     * 锯切 8 + 粉碎 13；批 3：雷击 8 + 嬗变 15；批 4：原版合成 26 = 有序 14 + 无序 12；
     * 批 5：动力合成 5；批 6：第三方轧制 8；批 7：序列组装 10 = 序列组装 9 + 凝能佩绑定 1，
     * 见类注释）。<b>批 7 是本轮最后一族</b>——转完之后本层再无手写配方。
     */
    public static void generate(RecipeOutput output) {
        // ========== 原版装备/武器拆磨（权重：剑2 镐3 斧3 铲1 锄2 / 头盔5 胸甲8 护腿7 靴子4） ==========
        dismantleSet(output, Items.DIAMOND, "diamond",
            Items.DIAMOND_SWORD, Items.DIAMOND_PICKAXE, Items.DIAMOND_AXE, Items.DIAMOND_SHOVEL, Items.DIAMOND_HOE,
            Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS);
        dismantleSet(output, Items.IRON_INGOT, "iron",
            Items.IRON_SWORD, Items.IRON_PICKAXE, Items.IRON_AXE, Items.IRON_SHOVEL, Items.IRON_HOE,
            Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS);
        dismantleSet(output, Items.GOLD_INGOT, "gold",
            Items.GOLDEN_SWORD, Items.GOLDEN_PICKAXE, Items.GOLDEN_AXE, Items.GOLDEN_SHOVEL, Items.GOLDEN_HOE,
            Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS);
        dismantleSet(output, Items.NETHERITE_INGOT, "netherite",
            Items.NETHERITE_SWORD, Items.NETHERITE_PICKAXE, Items.NETHERITE_AXE, Items.NETHERITE_SHOVEL, Items.NETHERITE_HOE,
            Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS);

        // ========== 本模组工具拆磨（5 组 × 5 工具） ==========
        dismantleTools(output, CoeItems.JADE_INGOT.get(), "jade",
            CoeItems.JADE_SWORD.get(), CoeItems.JADE_PICKAXE.get(), CoeItems.JADE_AXE.get(), CoeItems.JADE_SHOVEL.get(), CoeItems.JADE_HOE.get());
        dismantleTools(output, CoeItems.TOPAZ_INGOT.get(), "topaz",
            CoeItems.TOPAZ_SWORD.get(), CoeItems.TOPAZ_PICKAXE.get(), CoeItems.TOPAZ_AXE.get(), CoeItems.TOPAZ_SHOVEL.get(), CoeItems.TOPAZ_HOE.get());
        dismantleTools(output, CoeItems.SAPPHIRE_INGOT.get(), "sapphire",
            CoeItems.SAPPHIRE_SWORD.get(), CoeItems.SAPPHIRE_PICKAXE.get(), CoeItems.SAPPHIRE_AXE.get(), CoeItems.SAPPHIRE_SHOVEL.get(), CoeItems.SAPPHIRE_HOE.get());
        dismantleTools(output, CoeItems.STELLARSTONE_INGOT.get(), "stellarstone",
            CoeItems.STELLARSTONE_SWORD.get(), CoeItems.STELLARSTONE_PICKAXE.get(), CoeItems.STELLARSTONE_AXE.get(), CoeItems.STELLARSTONE_SHOVEL.get(), CoeItems.STELLARSTONE_HOE.get());
        dismantleTools(output, CoeItems.THUNDERITE_INGOT.get(), "thunderite",
            CoeItems.THUNDERITE_SWORD.get(), CoeItems.THUNDERITE_PICKAXE.get(), CoeItems.THUNDERITE_AXE.get(), CoeItems.THUNDERITE_SHOVEL.get(), CoeItems.THUNDERITE_HOE.get());

        // ========== 配方迁移 批 1：角磨 9 条 + 方块雷击 1 条（原手写，现由生成器产出） ==========
        // 顺序说明：这两组追加在 61 条拆磨<b>之后</b>，所以拆磨内部的相对顺序与拆分前逐字相同；
        // 这两组之间、以及它们与拆磨之间没有顺序契约（每条一个文件、互不覆盖）。
        // 用参数里的 output（= 根调用点已绑好层 "coe" 的那个），见类注释。
        CoeGrindingRecipeProvider.generate(output);
        CoeLightningBlockRecipeProvider.generate(output);

        // ========== 配方迁移 批 2：Create 标准类型四族 29 条（原手写，现由生成器产出） ==========
        // 压片 4 + 洗涤 4 + 锯切 8 + 粉碎 13 = 29 条。同样追加在批 1 两族之后，四族之间
        // 也没有顺序契约（每条一个文件、互不覆盖）。
        // ⚠ "该类型允不允许时长"逐族不同（批 1 的教训）：pressing / splashing 的注册类没有覆写
        // canSpecifyDuration() ⇒ 绝不能写 duration；cutting / crushing 覆写了 true ⇒ 必须写
        // （分别是 100 与 350/400/450）。逐族结论写在各自类的 javadoc 里。
        CoePressingRecipeProvider.generate(output);
        CoeSplashingRecipeProvider.generate(output);
        CoeCuttingRecipeProvider.generate(output);
        CoeCrushingRecipeProvider.generate(output);

        // ========== 配方迁移 批 3：COE 自有类型两族 23 条（原手写，现由生成器产出） ==========
        // 雷击 8 + 嬗变 15 = 23 条。同样追加在批 1/批 2 六族之后，两族之间也没有顺序契约
        // （每条一个文件、互不覆盖）。
        // ⚠ "该类型允不允许时长"逐族判决（批 1 的教训）：这两族的注册类
        // LightningRecipe / AllTransmutingRecipe 都没有覆写 canSpecifyDuration()
        // ⇒ 用 ProcessingRecipe 的默认 false ⇒ 绝不能写 duration（手写版也没有该键）。
        // ⚠ 雷击那 5 条充能配方的输出带 createoreexpansion:energy 数据组件，走
        // output(ItemStack) 才保留 getComponentsPatch()。逐族结论写在各自类的 javadoc 里。
        CoeLightningRecipeProvider.generate(output);
        CoeTransmutingRecipeProvider.generate(output);

        // ========== 配方迁移 批 4：原版合成族 26 条（原手写，现由生成器产出） ==========
        // 14 条有序合成 + 12 条无序合成 = 26 条。同样追加在批 1/2/3 八族之后。
        // ⚠ 本族是唯一不走 Create 配方类型的族 ⇒ 与"该类型允不允许时长"无关（原版合成没有时长字段）。
        // ⚠ 本族两个特有陷阱（见 CoeCraftingRecipeProvider 类注释）：
        //   ① 配方 id 必须显式写全 "crafting/materials/<名>"——原版 builder 的默认 id 不带该前缀，
        //      不显式给就等于把 26 条配方集体改名（数据包层面 = 配方搬家）。
        //   ② 必须用 output.accept(id, recipe, null) 直接提交，不许用 builder 的 save(output, id)：
        //      save 会额外产出 data/<ns>/advancement/recipes/**，而 LayerRecipeRouter 只改道
        //      data/<ns>/recipe/ ⇒ 那些 advancement 留在根输出（根不发布）⇒ 静默消失，F1 也看不到。
        CoeCraftingRecipeProvider.generate(output);

        // ========== 配方迁移 批 5：动力合成族 5 条（原手写，现由生成器产出） ==========
        // 5 条 create:mechanical_crafting（翡翠剑/镐/斧/锹/锄）。同样追加在批 1/2/3/4 九族之后。
        // ⚠ "该类型允不允许时长"对本族<b>不适用</b>：注册类 MechanicalCraftingRecipe extends
        //   ShapedRecipe（不是 ProcessingRecipe）⇒ 它的继承层次里根本没有 canSpecifyDuration()，
        //   也没有 processing_time 字段（手写版 5 条确实都没有该键）。
        // ⚠ 本族特有陷阱（见 CoeMechanicalCraftingRecipeProvider 类注释）：
        //   ① 必须用 MechanicalCraftingRecipeBuilder（纯 builder、写调用方给的 output），
        //      <b>不能</b>用 MechanicalCraftingRecipeGen —— 后者自带 PackOutput 直接写盘，
        //      会绕过 LayerRecipeRouter 的按调用点绑层 ⇒ 产物落进根输出、模块 jar 里一条没有（F1 红）。
        //   ② 必须显式传 id "mechanical_crafting/<名>"：build(output) 的默认 id 是
        //      <b>产物物品的 id</b>（本族文件名恰好等于产物名 ⇒ 会静默改名 = 配方搬家，
        //      而且文件数一个不少、静态关卡全绿）。
        //   ③ disallowMirrored() 必须调：它把 acceptMirrored=false <b>同时</b>当作 ShapedRecipe 的
        //      showNotification 参数传下去 ⇒ 恰好同时产出 accept_mirrored=false 与
        //      show_notification=false，与 5 条手写版逐字段一致；漏掉就变成"允许镜像"（玩法变化）。
        CoeMechanicalCraftingRecipeProvider.generate(output);

        // ========== 配方迁移 批 6：第三方 CC&A 轧制族 8 条（原手写，现由生成器产出） ==========
        // 8 条 createaddition:rolling（翡翠/黄玉/蓝宝石/星辉石 × {锭→杆, 板→线}）。同样追加在
        // 批 1/2/3/4/5 十族之后。本族是本轮唯一使用"第三方模组配方类型"的族，所以 provider 住
        // compat/createaddition/coe/（第三方耦合集中在 compat/ 子树），见该类注释的取舍说明。
        // ⚠ "该类型允不允许时长"对本族<b>适用</b>：RollingRecipe 在 ProcessingRecipe 那条继承链上
        //   （StandardProcessingRecipe<RecipeWrapper>），而 canSpecifyDuration() 的默认实现是 false、
        //   它没有覆写 ⇒ 结论是"核实为 false ⇒ 绝不许写 duration"（手写版 8 条也没有该键）。
        //   注意这与批 5 的判决<b>不是同一个</b>：那条是"继承层次里根本没有这个方法"。
        // ⚠ 组件：8 条产物都是普通物品，手写版没有 components 键，output(ItemLike, int) 给的是空补丁
        //   ⇒ 批 3 的"output(ItemStack) 静默丢组件"在本族不适用（本来就没有组件）。
        // ⚠ 目录段 "rolling/" 由 build(output) 从 RollingRecipe.TYPE_INFO 的 id 补齐（第三方），
        //   该类自己加了一道断言把它钉住，见 CoeRollingRecipeProvider#requireStableTypePath。
        CoeRollingRecipeProvider.generate(output);

        // ========== 配方迁移 批 7（最后一族）：序列组装族 10 条（原手写，现由生成器产出） ==========
        // 9 条 create:sequenced_assembly + 1 条同目录的 createoreexpansion:medallion_binding
        // （后者不是序列组装，是 CustomRecipe；它 id 里的 "sequenced_assembly/" 是历史误名，
        //  一个字不许改）。同样追加在批 1/2/3/4/5/6 十一族之后——**本族是最后一族**。
        // ⚠ 时长在本族是"每个步骤各自"的字段：10 条里没有任何一步带时长（processingDuration=0），
        //   所以批 1 的 canSpecifyDuration 守卫一个分支都进不去；七个步骤类型的结论仍逐一核实过。
        // ⚠ 本族特有的同形陷阱：addOutput(ItemLike, float) 把 count 钉死为 1，而 *_big_shard
        //   的输出是 3 个与 2 个 ⇒ 必须走 addOutput(ItemStack, float)（否则 count 键静默消失）。
        // ⚠ 必须用纯 builder SequencedAssemblyRecipeBuilder（不能再用 Create 自带的
        //   SequencedAssemblyRecipeGen：它自带 PackOutput 直接写盘、绕过按调用点绑层 ⇒ F1 红）。
        CoeSequencedAssemblyRecipeProvider.generate(output);
    }

    /** 一套材料：5 工具 + 4 装备的拆磨配方 */
    private static void dismantleSet(RecipeOutput output, ItemLike material, String materialName,
                                     ItemLike sword, ItemLike pickaxe, ItemLike axe, ItemLike shovel, ItemLike hoe,
                                     ItemLike helmet, ItemLike chestplate, ItemLike leggings, ItemLike boots) {
        dismantling(output, sword, material, 2, materialName + "_sword");
        dismantling(output, pickaxe, material, 3, materialName + "_pickaxe");
        dismantling(output, axe, material, 3, materialName + "_axe");
        dismantling(output, shovel, material, 1, materialName + "_shovel");
        dismantling(output, hoe, material, 2, materialName + "_hoe");
        dismantling(output, helmet, material, 5, materialName + "_helmet");
        dismantling(output, chestplate, material, 8, materialName + "_chestplate");
        dismantling(output, leggings, material, 7, materialName + "_leggings");
        dismantling(output, boots, material, 4, materialName + "_boots");
    }

    /** 一套材料：5 工具的拆磨配方（本模组） */
    private static void dismantleTools(RecipeOutput output, ItemLike material, String materialName,
                                       ItemLike sword, ItemLike pickaxe, ItemLike axe, ItemLike shovel, ItemLike hoe) {
        dismantling(output, sword, material, 2, materialName + "_sword");
        dismantling(output, pickaxe, material, 3, materialName + "_pickaxe");
        dismantling(output, axe, material, 3, materialName + "_axe");
        dismantling(output, shovel, material, 1, materialName + "_shovel");
        dismantling(output, hoe, material, 2, materialName + "_hoe");
    }

    /** 单条拆磨配方：装备 → 材料 × 权重系数 */
    private static void dismantling(RecipeOutput output, ItemLike item, ItemLike result, int materialCount, String name) {
        DismantlingRecipe recipe = new DismantlingRecipe(new ItemStack(item), new ItemStack(result), materialCount);
        output.accept(CoeCore.modLoc("dismantling/" + name), recipe, null, new ICondition[0]);
    }

    private CoeRecipeProvider() {}
}

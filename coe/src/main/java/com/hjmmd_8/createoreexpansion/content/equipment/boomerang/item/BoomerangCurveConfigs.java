package com.hjmmd_8.createoreexpansion.content.equipment.boomerang.item;

/**
 * <b>花瓣曲线（长按投掷）的数值真源</b> —— 与 {@code StarShockConfigs} 同形：本类只写数值与纯数学，
 * 不进任何 Minecraft 类型，浮点常数与公式<b>只在这里出现一次</b>。
 *
 * <p>需求依据：开工需求 2026-10-02 §3.4（★ 花瓣曲线，文档已给出数学构造）+ §3.2（长按 = 花瓣曲线）。
 * 消费方是 {@code AbstractBoomerangEntity}（长按分支 {@code tickPetal}）——实体里只做"取位置、写 setPos"，
 * 曲线本身一律回这里算。</p>
 *
 * <h2>一、曲线本体（需求 §3.4.1，逐字照抄）</h2>
 * <pre>
 *   r(φ) = R · cos( (π / Δθ) · φ )        φ ∈ [ −Δθ/2 , +Δθ/2 ]
 *   Δθ = 60°  ⇒  r(φ) = R · cos(3φ)        φ ∈ [ −30° , +30° ]
 * </pre>
 * <ul>
 *   <li><b>{@code Δθ} 同时就是"底部两边切线的夹角"</b>（需求 §5.3 陷阱 #1：两端 {@code r → 0}，
 *       两条分支沿 ±Δθ/2 的射线离开原点）⇒ 取 60° 就写 {@code cos(3φ)}。</li>
 *   <li><b>最远距离 = 该档收回距离 {@code R}</b>：{@code φ = 0}（尖端）时 {@code r = R}
 *       （{@code BoomerangTier#returnDistance()} 的 5 / 10 / 15 / 20 格）。</li>
 *   <li><b>两端闭合到玩家身上</b>：{@code φ = ±Δθ/2} 时 {@code r = 0} ⇒ 起点与终点都是原点
 *       （"飞完一瓣才返回"的几何依据）。</li>
 *   <li><b>不抬升</b>：{@code y} 恒等于原点高度（同水平面；需求 §六 推断值 #3）。</li>
 * </ul>
 *
 * <h2>二、总弧长（需求 §3.4.1 的表 + §附二 的复现脚本，已复算）</h2>
 * <pre>
 *   L_total = R · 2.2275        （Δθ = 60° 时的 L/R；其余夹角：45°→2.1446、75°→2.3207、90°→2.4221）
 *   R = 5  格 ⇒ L_total ≈ 11.14 格
 *   R = 10 格 ⇒ L_total ≈ 22.27 格
 *   R = 15 格 ⇒ L_total ≈ 33.41 格
 *   R = 20 格 ⇒ L_total ≈ 44.55 格
 * </pre>
 * <p>⚠ {@link #ARC_LENGTH_COEFFICIENT} 是<b>写死的常数</b>（需求给死），而 {@link #PROGRESS_BY_PHI}
 * 是<b>现算的弧长表</b>：1024 段梯形积分给出 {@code 2.227482}（与常数差 4×10⁻⁶，远小于需求文档的
 * 四位有效数字）。两者不一致时的取用口径：<b>弧长参数化的分母用常数</b>（速率剖面按它标定），
 * <b>φ(s) 的查表用表</b>（表自带归一化，与常数无关，只决定"走到哪儿"）。</p>
 *
 * <h2>三、速率剖面与弧长参数化（需求 §3.4.3 + §5.3 陷阱 #2）</h2>
 * <pre>
 *   s ∈ [0,1] 为归一化弧长（0 = 刚出手，1 = 回到手里，0.5 = 尖端）
 *   v(s) = v_min + (v_max − v_min) · |cos(π·s)|          ← 关于 s = 0.5 严格对称
 *   Δs   = v(s) · Δt / L_total      （Δt = 1 tick）
 * </pre>
 * <p><b>必须走弧长参数化</b>：{@code φ} 与时间<b>不是</b>线性关系（按时间线性插值会让尖端"冲过去"
 * 或"卡一下"，速率对称也就没了）。本类的做法：先用上表把 {@code s} 反查成 {@code φ}
 * （{@link #phiAt(double)}：二分 + 线性插值），再代入极坐标公式。</p>
 *
 * <h2>四、速率的调和标定（两个输入只有 R 与 T，其余全导出；2026-10-02 批 3 修正）</h2>
 * <pre>
 *   T_real = ∫₀¹ L ds / v(s) = (L_total / v_min) · (2/π)·ln(1+√2)/√2      ← 走完一瓣的<b>真实</b>用时
 *   令 T_real = T（= {@value #PETAL_FLIGHT_TICKS} tick = 3.0 秒）
 *   ⇒ v_min = L_total · {@value #TRAVERSAL_TIME_FACTOR} / T              （← 调和标定的<b>唯一</b>式子）
 *   ⇒ v_max = {@value #SPEED_RATIO_MAX_TO_MIN} · v_min                   （需求 §六 推断值 #2：作者只要求"发射最大、拐点最小、对称"）
 * </pre>
 * <p>逐档数值（{@code build/patch/boomerang-curve-check.ps1} 复算；格/tick）：</p>
 * <table border="1">
 *   <caption>R → L_total / v_min / v_max / 走完一瓣的真实 tick</caption>
 *   <tr><th>R（格）</th><th>L_total（格）</th><th>v_min</th><th>v_max</th><th>v_max（格/秒）</th><th>T_real（tick）</th></tr>
 *   <tr><td>5</td><td>11.1375</td><td>0.073648113</td><td>0.294592452</td><td>5.89</td><td>60</td></tr>
 *   <tr><td>10</td><td>22.2750</td><td>0.147296226</td><td>0.589184903</td><td>11.78</td><td>60</td></tr>
 *   <tr><td>15</td><td>33.4125</td><td>0.220944339</td><td>0.883777355</td><td>17.68</td><td>60</td></tr>
 *   <tr><td>20</td><td>44.5500</td><td>0.294592452</td><td>1.178369806</td><td>23.57</td><td>60</td></tr>
 * </table>
 *
 * <h2>⚠ 五、被推翻的旧标定（2026-10-02 批 2 用过一轮，批 3 换掉；按本仓习惯留档，不静默删）</h2>
 * <p>批 2 标的是一行 <b>"弧长平均速率"</b>：{@code v_avg = L_total / T}，
 * 再用 {@code v_avg = v_min + (v_max − v_min)·(2/π)}（{@code |cos πs|} 的弧长平均 = 2/π）
 * 与 {@code v_max = 4·v_min} 联立，得到</p>
 * <pre>
 *   v_min = v_avg / (1 + 3·2/π) = v_avg / 2.909859…      ← 被推翻的 MIN_SPEED_DIVISOR
 * </pre>
 * <p>问题：<b>"平均速率"与"平均用时的倒数"不是一回事</b>。{@code v_avg} 是<b>弧长平均</b>
 * （{@code ∫v ds / L}），而"走完一瓣要多久"是<b>时间积分</b> {@code T_real = L·∫ds/v(s)}。
 * 两者只在 {@code v} 为常数时相等；对本剖面，旧标定给出
 * {@code T_real = T · 2.909859 × 0.396758 ≈ T × 1.1545 ≈ 69.3 tick}（全部档位同一倍数），
 * 也就是<b>一瓣实际飞 3.46 秒而不是 3.0 秒</b>。</p>
 * <p>批 3 裁定：把标定换成上面的<b>调和口径</b>（直接对 {@code T_real} 解 {@code v_min}），
 * 于是 {@code T_real ≡ T = 60 tick}、{@code v_max : v_min} 仍是 {@code 4 : 1}。
 * 被删掉的两个常量（{@code MIN_SPEED_DIVISOR} = {@code 1 + 3·2/π} = 2.909859…、
 * {@code ARC_MEAN_OF_COS} = {@code 2/π}）不再是代码，只剩本节的留档
 * ——{@link #TRAVERSAL_TIME_FACTOR} 仍在（它现在是标定式里的那个系数，
 * 而不再只是"报告偏差"用的诊断量）。</p>
 *
 * @since 1.0.0
 */
public final class BoomerangCurveConfigs {

    private BoomerangCurveConfigs() {
        throw new AssertionError("This class should not be instantiated");
    }

    /**
     * 花瓣的<b>张角 Δθ</b>（度）—— 同时就是"底部两边切线的夹角"（需求 §3.4.1 与 §5.3 陷阱 #1）。
     *
     * <p>想改成 45° / 75° / 90° 只改这一个数：曲线阶数 {@link #PETAL_FREQUENCY} = {@code π/Δθ}
     * 会跟着变成 4 / 2.4 / 2，但 <b>{@link #ARC_LENGTH_COEFFICIENT} 必须手改</b>
     * （2.1446 / 2.3207 / 2.4221），本类不做运行期积分来省这个数——它是需求给死的表。</p>
     */
    public static final double APERTURE_DEGREES = 60.0D;

    /** 半张角（弧度）= {@code Δθ/2} = 30°：曲线参数 {@code φ} 的取值半径。 */
    public static final double HALF_APERTURE_RADIANS = Math.toRadians(APERTURE_DEGREES) / 2.0D;

    /**
     * 曲线阶数 {@code n = π / Δθ}（弧度制）—— Δθ = 60° 时恰为 <b>3</b>（需求 §3.4.1 的"整数"那一档）。
     *
     * <p>写成 {@code Math.PI / Math.toRadians(APERTURE_DEGREES)} 而不是字面量 {@code 3.0}：
     * 目的是让"改 Δθ"这件事<b>只改一处</b>（阶数自动跟随），代价是浮点上
     * {@code 3 * (π/6)} 与 {@code π/2} 差 1 ulp ⇒ 端点 {@code r} 是 {@code 6.1E-17·R} 而不是精确 0
     * （量级上是 0；{@link #radiusAt(double, double)} 另有下限钳位，见该方法的注释）。</p>
     */
    public static final double PETAL_FREQUENCY = Math.PI / Math.toRadians(APERTURE_DEGREES);

    /** 单位半径的总弧长系数 {@code L/R}（Δθ = 60°；需求 §3.4.1 / §附二 复算 = 2.227482）。 */
    public static final double ARC_LENGTH_COEFFICIENT = 2.2275D;

    /**
     * 长按花瓣的<b>总飞行时长 T</b>（tick）= {@value PETAL_FLIGHT_TICKS} tick = 3.0 秒
     * （需求 §六 推断值 #1；作者没给，取"看得清拐点减速"的时长）。
     *
     * <p>⚠ <b>2026-10-02 批 3 改名</b>：它原来叫 {@code NOMINAL_FLIGHT_TICKS}（"标称"），
     * 因为批 2 的标定式把 {@code v_avg = L_total / T} 当弧长平均、于是<b>实际</b>一瓣要 ≈69.3 tick
     * （见类注释第五节）。批 3 换成调和标定后 <b>{@code T} 就是实际用时</b>（{@code T_real ≡ T}），
     * "标称"这个限定词不再成立，所以改名 —— 名字里的 {@code PETAL} 指"走完一瓣"，
     * 与 {@link #TRAVERSAL_TIME_FACTOR} 的定义式一一对应。</p>
     */
    public static final int PETAL_FLIGHT_TICKS = 60;

    /** 速率比 {@code v_max : v_min = 4 : 1}（需求 §六 推断值 #2；批 3 的调和标定<b>保留</b>这个比例）。 */
    public static final double SPEED_RATIO_MAX_TO_MIN = 4.0D;

    /**
     * <b>走完一瓣所需的 tick 系数</b> {@code (2/π)·ln(1+√2)/√2} ≈ <b>0.396757511</b>：
     * {@code T_real = (L_total / v_min) · 它}。
     *
     * <p>推导：{@code T_real = ∫₀¹ L ds / (v_min + 3v_min|cos πs|) = (L/v_min)·(2/π)·∫₀^{π/2} du/(1+3cos u)}，
     * 而 {@code ∫₀^{π/2} du/(1+3cos u) = ln(1+√2)/√2}（半角代换 + 部分分式）。</p>
     *
     * <p>它在本类里有<b>两个</b>用途，且是同一个系数：</p>
     * <ol>
     *   <li><b>标定</b>（批 3 起）：把 {@code T_real = T} 反解出 {@code v_min}
     *       （{@link #petal(int)} 用的就是这一条，见类注释第四节）；</li>
     *   <li><b>诊断</b>：{@link Petal#traversalTicks()} 直接报"这一瓣实际飞多少 tick"
     *       ——调和标定之后它恒等于 {@code T}，所以它现在是<b>恒等式而不是偏差表</b>
     *       （批 2 它报的是 69.3，正是这一批要消掉的那个数）。</li>
     * </ol>
     */
    public static final double TRAVERSAL_TIME_FACTOR =
        (2.0D / Math.PI) * (Math.log(1.0D + Math.sqrt(2.0D)) / Math.sqrt(2.0D));

    /** 弧长表的段数（1024 段梯形积分 ⇒ {@code L/R} 精确到 1×10⁻⁶ 量级）。 */
    private static final int TABLE_STEPS = 1024;

    /**
     * 弧长表：{@code PROGRESS_BY_PHI[i]} = 从 {@code φ = −Δθ/2} 走到第 {@code i} 段时的
     * <b>归一化弧长</b>（0 → 1）。单调递增，{@code i = 0} 为 0、{@code i = TABLE_STEPS} 为 1。
     *
     * <p>它是 {@code s → φ} 反查的载体（{@link #phiAt(double)}）。为什么用表而不是每 tick 现积分：
     * 本函数与 {@code R} 无关（弧长随 R 线性缩放），所以<b>整张表与档位无关、只需建一次</b>；
     * 每 tick 现积分也能做，但会让"同一个 s 在不同档位下落到同一个 φ"这件事变得难以验证。</p>
     */
    private static final double[] PROGRESS_BY_PHI = buildProgressTable();

    private static double[] buildProgressTable() {
        double[] progress = new double[TABLE_STEPS + 1];
        double delta = 2.0D * HALF_APERTURE_RADIANS / TABLE_STEPS;
        double previousFactor = arcSpeedFactor(-HALF_APERTURE_RADIANS);
        double total = 0.0D;
        progress[0] = 0.0D;
        for (int i = 1; i <= TABLE_STEPS; i++) {
            double phi = -HALF_APERTURE_RADIANS + i * delta;
            double factor = arcSpeedFactor(phi);
            total += 0.5D * (previousFactor + factor) * delta; // 梯形
            progress[i] = total;
            previousFactor = factor;
        }
        if (total <= 0.0D) {
            throw new IllegalStateException("petal arc length collapsed: " + total);
        }
        for (int i = 0; i <= TABLE_STEPS; i++) {
            progress[i] /= total;
        }
        return progress;
    }

    /** 极坐标曲线的"速率因子" {@code |dP/dφ|}（单位半径）= {@code √(r² + r'²)}，只给建表用。 */
    private static double arcSpeedFactor(double phi) {
        double r = Math.cos(PETAL_FREQUENCY * phi);
        double dr = -PETAL_FREQUENCY * Math.sin(PETAL_FREQUENCY * phi);
        return Math.sqrt(r * r + dr * dr);
    }

    /**
     * 一档花瓣的导出量（{@code R} 是唯一输入）。
     *
     * @param radius    {@code R}：该档的收回距离（格）—— 同时就是花瓣尖端到原点的距离
     * @param arcLength {@code L_total = R × 2.2275}（格）
     * @param minSpeed  {@code v_min}（格/tick；尖端处速率）
     * @param maxSpeed  {@code v_max = 4 · v_min}（格/tick；出手与回手处速率）
     */
    public record Petal(double radius, double arcLength, double minSpeed, double maxSpeed) {

        /**
         * 走完这一瓣的<b>实际</b> tick 数（闭式）。
         *
         * <p>批 3 的调和标定就是为了让这个数恒等于 {@link #PETAL_FLIGHT_TICKS}
         * （逐 tick 数值模拟同样落在 60；实体侧 {@code tickPetal} 的推进式与它同源）。
         * 批 2 它报的是 ≈69.3 —— 那个偏差是本批消掉的东西，不是这里的公式变了。</p>
         */
        public double traversalTicks() {
            return arcLength / minSpeed * TRAVERSAL_TIME_FACTOR;
        }
    }

    /**
     * 按该档收回距离造一条花瓣（<b>唯一入口</b>；两个输入只有 R 与 T，其余全导出）。
     *
     * <p><b>调和标定</b>（批 3 裁定；见类注释第四节）：直接对"走完一瓣的真实用时"解 {@code v_min}
     * ——{@code v_min = L_total · TRAVERSAL_TIME_FACTOR / T}，于是 {@code T_real ≡ T}。
     * ⚠ 这里<b>不</b>再有 {@code v_avg = L_total / T} 那一行：它是批 2 被推翻的标定
     * （弧长平均 ≠ 用时的倒数，留档见类注释第五节）。</p>
     *
     * @param returnDistance 该档的收回距离（格）—— 实体侧一律传
     *                       {@code BoomerangTier#returnDistance()}，不许出现距离字面量
     */
    public static Petal petal(int returnDistance) {
        double radius = returnDistance;
        double arcLength = radius * ARC_LENGTH_COEFFICIENT;
        double minSpeed = arcLength * TRAVERSAL_TIME_FACTOR / PETAL_FLIGHT_TICKS;
        return new Petal(radius, arcLength, minSpeed, SPEED_RATIO_MAX_TO_MIN * minSpeed);
    }

    /**
     * 归一化弧长 {@code s} 处的速率 {@code v(s) = v_min + (v_max − v_min)·|cos(π·s)|}（格/tick）。
     *
     * <p>关于 {@code s = 0.5} 严格对称：{@code s = 0 / 1 → v_max}（出手最快、回手再加快），
     * {@code s = 0.5 → v_min}（尖端最慢）—— 需求 §3.4.3 的三条逐条对上。</p>
     */
    public static double speedAt(double s, Petal petal) {
        double clamped = Math.max(0.0D, Math.min(1.0D, s));
        return petal.minSpeed()
            + (petal.maxSpeed() - petal.minSpeed()) * Math.abs(Math.cos(Math.PI * clamped));
    }

    /**
     * 推进一个 tick：返回<b>新的</b>归一化弧长 {@code s'} = {@code s + v(s)·Δt / L_total}（{@code Δt = 1}）。
     *
     * <p>刻意<b>不</b>在这里钳到 1.0：调用方要靠 {@code s' ≥ 1} 判"飞完一瓣了"（需求 §3.2：
     * 必须飞完一瓣才能返回），钳掉就把那个判据变成了"s 恰好等于 1"的浮点比较。</p>
     */
    public static double stepProgress(double s, Petal petal) {
        if (petal.arcLength() <= 0.0D) {
            return 1.0D;
        }
        return s + speedAt(s, petal) / petal.arcLength();
    }

    /**
     * 归一化弧长 {@code s} → 曲线参数 {@code φ}（弧度；{@code φ ∈ [−Δθ/2, +Δθ/2]}）。
     *
     * <p>这就是"弧长参数化"的落点：{@link #PROGRESS_BY_PHI} 上二分查找 + 线性插值
     * （表在 φ 上等距，所以插值就是等距线性）。{@code s = 0.5} 落在 <b>φ = 0</b>（尖端）。</p>
     */
    public static double phiAt(double s) {
        double clamped = Math.max(0.0D, Math.min(1.0D, s));
        int low = 0;
        int high = TABLE_STEPS;
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (PROGRESS_BY_PHI[mid] < clamped) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        int upper = Math.max(1, low);
        int lower = upper - 1;
        double span = PROGRESS_BY_PHI[upper] - PROGRESS_BY_PHI[lower];
        double fraction = span <= 0.0D ? 0.0D : (clamped - PROGRESS_BY_PHI[lower]) / span;
        double step = 2.0D * HALF_APERTURE_RADIANS / TABLE_STEPS;
        return -HALF_APERTURE_RADIANS + (lower + fraction) * step;
    }

    /**
     * 半径 {@code r(φ) = R·cos(3φ)}（格）；两端因 1 ulp 的浮点误差可能给出 {@code −6.1E-17·R}，
     * 这里<b>下限钳到 0</b>（负半径没有几何意义，且会让位置落到反方向去）。
     */
    public static double radiusAt(double phi, double radius) {
        return Math.max(0.0D, radius * Math.cos(PETAL_FREQUENCY * phi));
    }

    /**
     * 极坐标 → 笛卡尔的 <b>X 偏移</b>（相对原点 {@code P}）：{@code r(φ)·cos(α)}，{@code α = ψ + φ}。
     *
     * <p>{@code ψ}（{@code baseAngle}，弧度）是投掷那一刻玩家水平朝向的<b>数学角</b>
     * （{@code atan2(朝向.z, 朝向.x)}）——这样 {@link #offsetX}/{@link #offsetZ} 与需求 §3.4.3
     * 的 {@code x = P.x + r·cos(α)}、{@code z = P.z + r·sin(α)} 逐字同形。</p>
     */
    public static double offsetX(double s, double radius, double baseAngle) {
        double phi = phiAt(s);
        return radiusAt(phi, radius) * Math.cos(baseAngle + phi);
    }

    /** 极坐标 → 笛卡尔的 <b>Z 偏移</b>（见 {@link #offsetX}）。 */
    public static double offsetZ(double s, double radius, double baseAngle) {
        double phi = phiAt(s);
        return radiusAt(phi, radius) * Math.sin(baseAngle + phi);
    }
}

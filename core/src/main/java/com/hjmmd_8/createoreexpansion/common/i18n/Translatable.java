package com.hjmmd_8.createoreexpansion.common.i18n;

/**
 * 「有翻译键」契约：实现者提供一个语言键，由语言 Provider 负责填词条。
 *
 * <p><b>P4e 为什么住在 {@code common/i18n} 而不是 {@code data/lang}</b>：本接口是 core 的
 * 共享契约（{@code common/registry/LayerCreativeTab}、{@code util/HeatLevelNames} 以及 COE 的
 * 技能枚举都实现它），而 {@code data/lang} 是**根集成层**的包（{@code ChineseLangProvider} /
 * {@code EnglishLangProvider} / {@code LayerLangSplitter} 三个文件）。同一个 Java 包落在两个
 * mod 文件里 = 启动期 {@code java.lang.module.ResolutionException}。</p>
 *
 * <p>这个错误为什么能潜伏很久：core 过去是无类型 JarJar 库、落在 unnamed module，包重叠没人管；
 * 一旦 core 的 jar 声明 {@code FMLModType: GAMELIBRARY}（P4e），core 就变成 GAME 层里真正的
 * JPMS 模块，重叠立刻从「调用时才 NoClassDefFoundError」变成「开不起来」。
 * 判据见 {@code tools/check-package-overlap.ps1}（五根包重叠必须为 0）。</p>
 */
public interface Translatable {
    String getTranslateKey();
}

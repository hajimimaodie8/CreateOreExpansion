package com.hjmmd_8.createoreexpansion.data.lang;

import com.hjmmd_8.createoreexpansion.common.AllCreativeModeTabs;
import com.hjmmd_8.createoreexpansion.common.AllKeys;
import com.hjmmd_8.createoreexpansion.common.AllSkills;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.EnergyTooltipHandler;
import com.hjmmd_8.createoreexpansion.content.skill.tooltip.SkillsTooltipHandler;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillType;
import com.hjmmd_8.createoreexpansion.mixin.LanguageProviderAccessor;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.data.LanguageProvider;
import org.jetbrains.annotations.Nullable;

public enum COELangProvider {
    INSTANCE;

    public void addTranslations(LanguageProvider provider) {
        var builder = Builder.of(provider)
                .add(AllCreativeModeTabs.BASE_TAB.translatable,
                        "机械动力：矿物拓展", "Create: Ore Expansion")
        ;
        builder
                .add(SkillsTooltipHandler::translate)
                .add(AllKeys::translate)
                .add(AllSkills::translate)
                .add(EnergyTooltipHandler::translate)
                .add(SkillType::translate)
        ;
    }



    public static class Builder {
        private final LanguageProvider provider;
        private final LanguageProviderAccessor accessor;

        private Builder(LanguageProvider provider) {
            this.provider = provider;
            accessor = (LanguageProviderAccessor) provider;
        }

        private boolean shouldAdd(String locale) {
            return accessor.getLocale().equals(locale);
        }

        private void add(Translatable key, String value) {
            provider.add(key.getTranslateKey(), value);
        }

        public static Builder of(LanguageProvider provider) {
            return new Builder(provider);
        }

        public Builder addLang(Translatable key, String locale, String value) {
            if (value != null && shouldAdd(locale)) add(key, locale);
            return this;
        }

        public Builder addLang(String key, String locale, String value) {
            return addLang(() -> key, locale, value);
        }

        public Builder add(Translatable key,
                           @Nullable String chineseTranslate,
                           @Nullable String englishTranslate) {
            if (chineseTranslate != null && shouldAdd("zh_cn")) add(key, chineseTranslate);
            if (englishTranslate != null && shouldAdd("en_us")) add(key, englishTranslate);
            return this;
        }

        public  Builder add(String key,
                            @Nullable String chineseTranslate,
                            @Nullable String englishTranslate) {
            return add(() -> key, chineseTranslate, englishTranslate);
        }

        public Builder add(Item key,
                           @Nullable String chineseTranslate,
                           @Nullable String englishTranslate) {
            return add(key::getDescriptionId, chineseTranslate, englishTranslate);
        }

        public Builder add(MobEffect key,
                           @Nullable String chineseTranslate,
                           @Nullable String englishTranslate) {
            return add(key::getDescriptionId, chineseTranslate, englishTranslate);
        }

        public Builder add(Translator translator) {
            return translator.translate(this);
        }
    }
}

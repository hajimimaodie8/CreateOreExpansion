package com.hjmmd_8.createoreexpansion.mixin;

import net.neoforged.neoforge.common.data.LanguageProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LanguageProvider.class)
public interface LanguageProviderAccessor {
    @Accessor("locale")
    String getLocale();
}

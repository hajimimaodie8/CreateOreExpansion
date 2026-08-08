package com.hjmmd_8.createoreexpansion.data;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.data.lang.ChineseLangProvider;
import com.hjmmd_8.createoreexpansion.data.lang.EnglishLangProvider;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = CreateOreExpansion.MOD_ID)
public class CreateOreExpansionDatagen {

    @SubscribeEvent
    public static void gather(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

        if (event.includeClient()) {
            generator.addProvider(true, new ChineseLangProvider(output));
            System.out.println("========== CreateOreExpansion ChineseLangProvider START ==========");
            generator.addProvider(true, new EnglishLangProvider(output));
            System.out.println("========== CreateOreExpansion EnglishLangProvider START ==========");
        }
    }
}

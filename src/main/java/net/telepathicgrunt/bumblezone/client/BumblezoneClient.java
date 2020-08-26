package net.telepathicgrunt.bumblezone.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;
import net.minecraft.util.ResourceLocation;
import net.telepathicgrunt.bumblezone.Bumblezone;
import net.telepathicgrunt.bumblezone.blocks.BzBlocks;
import net.telepathicgrunt.bumblezone.client.rendering.FluidRender;
import net.telepathicgrunt.bumblezone.client.rendering.HoneySlimeRendering;
import net.telepathicgrunt.bumblezone.dimension.BzSkyProperty;
import net.telepathicgrunt.bumblezone.entities.BzEntities;
import net.telepathicgrunt.bumblezone.mixin.SkyPropertiesAccessor;

@Environment(EnvType.CLIENT)
public class BumblezoneClient implements ClientModInitializer {
    public static final ResourceLocation FLUID_STILL = new ResourceLocation(Bumblezone.MODID + ":block/sugar_water_still");
    public static final ResourceLocation FLUID_FLOWING = new ResourceLocation(Bumblezone.MODID + ":block/sugar_water_flow");

    @Override
    public void onInitializeClient() {
        FluidRender.setupFluidRendering(BzBlocks.SUGAR_WATER_FLUID, BzBlocks.SUGAR_WATER_FLUID_FLOWING, FLUID_STILL, FLUID_FLOWING);
        BzBlocks.registerRenderLayers();

        EntityRendererRegistry.INSTANCE.register(BzEntities.HONEY_SLIME, (dispatcher, context) -> new HoneySlimeRendering(dispatcher));
        SkyPropertiesAccessor.getBY_ResourceLocation().put(new ResourceLocation(Bumblezone.MODID, "sky_property"), new BzSkyProperty());
    }
}
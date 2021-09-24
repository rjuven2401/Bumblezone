package com.telepathicgrunt.the_bumblezone.mixin.client;

import com.telepathicgrunt.the_bumblezone.modinit.BzFluids;
import com.telepathicgrunt.the_bumblezone.tags.BzFluidTags;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(FogRenderer.class)
public class FogRendererClientMixin {

    @Shadow
    private static float fogRed;

    @Shadow
    private static float fogGreen;

    @Shadow
    private static float fogBlue;

    @Shadow
    private static long biomeChangedTime = -1L;

    // make honey fluid have orange fog
    @Inject(method = "setupColor(Lnet/minecraft/client/renderer/ActiveRenderInfo;FLnet/minecraft/client/world/ClientWorld;IF)V",
            at = @At(value = "INVOKE", target = "net/minecraft/client/world/ClientWorld$ClientWorldInfo.getClearColorScale()D"))
    private static void thebumblezone_setupHoneyFogColor(ActiveRenderInfo activeRenderInfo, float j, ClientWorld clientWorld, int l, float i1, CallbackInfo ci) {
        FluidState fluidstate = activeRenderInfo.getFluidInCamera();
        if(fluidstate.is(BzFluidTags.BZ_HONEY_FLUID)) {
            float brightness = (float) Math.pow(activeRenderInfo.getEntity().getBrightness(), 2D);
            fogRed = 0.6F * brightness;
            fogGreen = 0.3F * brightness;
            fogBlue = 0.0F;
            biomeChangedTime = -1L;
        }
    }
}
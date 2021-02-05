package io.github.ultimateboomer.memfix.mixin;

import net.minecraft.client.texture.MipmapHelper;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MipmapHelper.class)
public class MipmapHelperMixin {
//    @Redirect(method = "getMipmapLevelsImages", at = @At(value = "NEW",
//            target = "net/minecraft/client/texture/NativeImage"))
//    private static NativeImage onGetMipmapImages(int width, int height, boolean useStb) {
//        return MemFix.nativeImagePool.new PooledNativeImage(NativeImage.Format.ABGR, width, height, useStb);
//    }
}

package io.github.ultimateboomer.memfix.mixin;

import net.minecraft.client.font.UnicodeTextureFont;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(UnicodeTextureFont.class)
public class UnicodeTextureFontMixin {
//    @Redirect(method = "*", at = @At(value = "INVOKE",
//            target = "Lnet/minecraft/client/texture/NativeImage;" +
//                    "read(Lnet/minecraft/client/texture/NativeImage$Format;Ljava/io/InputStream;)" +
//                    "Lnet/minecraft/client/texture/NativeImage;"))
//    private NativeImage onReadNativeImage(NativeImage.Format format, InputStream inputStream) throws IOException {
//        return MemFix.nativeImagePool.read(format, inputStream);
//    }
}

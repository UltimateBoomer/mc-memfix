package io.github.ultimateboomer.memfix.mixin;

import io.github.ultimateboomer.memfix.MemFix;
import net.minecraft.client.font.UnicodeTextureFont;
import net.minecraft.client.texture.NativeImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.IOException;
import java.io.InputStream;

@Mixin(UnicodeTextureFont.class)
public class UnicodeTextureFontMixin {
    @Redirect(method = "*", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/texture/NativeImage;" +
                    "read(Lnet/minecraft/client/texture/NativeImage$Format;Ljava/io/InputStream;)" +
                    "Lnet/minecraft/client/texture/NativeImage;"))
    private NativeImage onReadNativeImage(NativeImage.Format format, InputStream inputStream) throws IOException {
        return MemFix.nativeImagePool.read(format, inputStream);
    }
}

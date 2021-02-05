package io.github.ultimateboomer.memfix.mixin;

import net.minecraft.client.texture.SpriteAtlasTexture;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = {SpriteAtlasTexture.class}, priority = 2000)
public class SpriteAtlasTextureMixin {
//    @Inject(method = "stitch", at = @At("RETURN"))
//    private void onStitch(CallbackInfoReturnable<SpriteAtlasTexture.Data> ci) {
//        SpriteAtlasTexture.Data data = ci.getReturnValue();
//        // Partially fix resource reload memory leak
//        SpriteAtlasTexture.Data old = MemFix.dataMap.get(this.id);
//
//        if (old != null && !old.equals(data)) {
//            for (Sprite sp : old.sprites) {
//                sp.close();
//            }
//            old.sprites.clear();
//
//            MemFix.LOGGER.info("Closed old SpriteAtlasTexture Data");
//        }
//
//        MemFix.dataMap.put(id, data);
//    }

//    @Redirect(method = "*", at = @At(value = "INVOKE",
//            target = "Lnet/minecraft/client/texture/NativeImage;read(Ljava/io/InputStream;)" +
//                    "Lnet/minecraft/client/texture/NativeImage;"))
//    private NativeImage onReadNativeImage(InputStream inputStream) throws IOException {
////        NativeImage image = NativeImage.read(inputStream);
////        MemFix.closeOnReload.add(image);
////
////        return image;
//
//        return MemFix.nativeImagePool.read(NativeImage.Format.ABGR, inputStream);
//    }
}

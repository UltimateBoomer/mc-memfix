package io.github.ultimateboomer.memfix.mixin;

import io.github.ultimateboomer.memfix.MemFix;
import net.minecraft.client.texture.NativeImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NativeImage.class)
public abstract class NativeImageMixin {
    @Shadow public abstract String toString();

    @Shadow public long pointer;

    @Inject(method = "<init>*", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/texture/NativeImage;pointer:J"))
    private void onInit(CallbackInfo ci) {
        MemFix.nativeImageList.add((NativeImage) (Object) this);
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void onClose(CallbackInfo ci) {
        assert MemFix.nativeImageList.contains(this);

        MemFix.nativeImageList.remove(this);
    }

//    @Redirect(method = "getBytes", at = @At(value = "INVOKE",
//            target = "Lnet/minecraft/client/texture/NativeImage;write(Ljava/nio/channels/WritableByteChannel;)Z"))
//    private boolean onGetBytes(NativeImage nativeImage, WritableByteChannel writableByteChannel) {
//        return true;
//    }
}

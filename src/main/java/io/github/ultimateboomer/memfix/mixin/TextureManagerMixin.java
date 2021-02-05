package io.github.ultimateboomer.memfix.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.SplashScreen;
import net.minecraft.client.texture.TextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureManager.class)
public class TextureManagerMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onTick(CallbackInfo ci) {
        if (MinecraftClient.getInstance().overlay instanceof SplashScreen) {
            ci.cancel();
        }
    }

//    @Redirect(method = "reload", at = @At(value = "INVOKE",
//            target = "Ljava/util/concurrent/CompletableFuture;allOf([Ljava/util/concurrent/CompletableFuture;)" +
//                    "Ljava/util/concurrent/CompletableFuture;"))
//    private CompletableFuture<Void> onReload(CompletableFuture<Void>[] cfs) {
//        return CompletableFuture.allOf(ArrayUtils.add(cfs, 0, CompletableFuture.runAsync(() -> {
//
//        })));
//    }
}

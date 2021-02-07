package io.github.ultimateboomer.memfix.mixin;

import io.github.ultimateboomer.memfix.MemFix;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.SplashScreen;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Util;
import org.apache.commons.lang3.ArrayUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;

@Mixin(TextureManager.class)
public class TextureManagerMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onTick(CallbackInfo ci) {
        if (MinecraftClient.getInstance().overlay instanceof SplashScreen) {
            ci.cancel();
        }
    }

    @Redirect(method = "reload", at = @At(value = "INVOKE",
            target = "Ljava/util/concurrent/CompletableFuture;allOf([Ljava/util/concurrent/CompletableFuture;)" +
                    "Ljava/util/concurrent/CompletableFuture;"))
    private CompletableFuture<Void> onReload(CompletableFuture<Void>[] cfs) {
        return CompletableFuture.allOf(ArrayUtils.add(cfs, 0, CompletableFuture.runAsync(() -> {
            MemFix.closeOnReload.forEach(image -> image.close());
            MemFix.closeOnReload.clear();
        }, Util.getMainWorkerExecutor())));
    }
}

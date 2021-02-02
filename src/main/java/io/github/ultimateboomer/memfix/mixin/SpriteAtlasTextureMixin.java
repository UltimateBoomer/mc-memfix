package io.github.ultimateboomer.memfix.mixin;

import io.github.ultimateboomer.memfix.MemFix;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpriteAtlasTexture.class)
public class SpriteAtlasTextureMixin {
    @Shadow @Final private Identifier id;

    @Inject(method = "stitch", at = @At("RETURN"))
    private void onStitch(CallbackInfoReturnable<SpriteAtlasTexture.Data> ci) {
        SpriteAtlasTexture.Data data = ci.getReturnValue();
        // Partially fix resource reload memory leak
        SpriteAtlasTexture.Data old = MemFix.dataMap.get(this.id);

        if (old != null && !old.equals(data)) {
            for (Sprite sp : old.sprites) {
                sp.close();
            }
            old.sprites.clear();

            MemFix.LOGGER.info("Closed old SpriteAtlasTexture Data");
        }

        MemFix.dataMap.put(id, data);
    }

    private void onTick() {

    }
}

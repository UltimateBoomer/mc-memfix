package io.github.ultimateboomer.memfix.mixin;

import io.github.ultimateboomer.memfix.MemFix;
import io.github.ultimateboomer.memfix.MemoryHandle;
import net.minecraft.client.texture.NativeImage;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

@Mixin(NativeImage.class)
public class NativeImageMixin implements MemoryHandle {
    @Shadow @Final
    private long sizeBytes;

    @Shadow
    public long pointer;

    @Unique
    public long offset;

    @Redirect(method = "<init>(Lnet/minecraft/client/texture/NativeImage$Format;IIZ)V",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/system/MemoryUtil;nmemAlloc(J)J"))
    private long onInit(long size) {
        initImage(size, true, (byte) 0);
        return this.pointer;
    }

    @Redirect(method = "<init>(Lnet/minecraft/client/texture/NativeImage$Format;IIZ)V",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/system/MemoryUtil;nmemCalloc(JJ)J"))
    private long onInit(long num, long size) {
        initImage(size, true, (byte) 0);
        return this.pointer;
    }

    private void initImage(long size, boolean init, byte num) {
        getPoolReference().allocate(this);

        if (init) {
            for (long i = this.pointer; i < this.pointer + this.sizeBytes; ++i) {
                MemoryUtil.memPutByte(i, num);
            }
        }

        MemFix.nativeImageList.add((NativeImage) (Object) this);
    }

    @Inject(method = "<init>(Lnet/minecraft/client/texture/NativeImage$Format;IIZJ)V", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        throw new UnsupportedOperationException();
    }

    @Inject(method = "close", at = @At("HEAD"), cancellable = true)
    public void close(CallbackInfo ci) {
        getPoolReference().closeHandle(this);
        this.markClosed();
        ci.cancel();
    }

    @Override
    public void markClosed() {
        this.offset = -1;
        this.pointer = 0;
        MemFix.nativeImageList.remove(this);
    }

    @Override
    public void recalculatePointer(long poolPointer) {
        if (offset != -1) {
            this.pointer = poolPointer + this.offset;
        } else {
            throw new IllegalStateException("Image not allocated");
        }
    }

    /**
     * @author UltimateBoomer
     * @reason Delete STBImage pointer immediately after creation
     */
    @Overwrite
    public static NativeImage read(NativeImage.Format format, ByteBuffer buffer) throws IOException {
        if (format != null && !format.isWriteable()) {
            throw new UnsupportedOperationException("Don't know how to read format " + format);
        } else if (MemoryUtil.memAddress(buffer) == 0L) {
            throw new IllegalArgumentException("Invalid buffer");
        } else {
            synchronized (NativeImage.class) {
                MemoryStack stack = MemoryStack.stackPush();
                NativeImage image;
                try {
                    IntBuffer w = stack.mallocInt(1);
                    IntBuffer h = stack.mallocInt(1);
                    IntBuffer f = stack.mallocInt(1);
                    ByteBuffer imageBuffer = STBImage.stbi_load_from_memory(buffer, w, h, f,
                            format == null ? 0 : format.getChannelCount());
                    if (imageBuffer == null) {
                        throw new IOException("Could not load image: " + STBImage.stbi_failure_reason());
                    }
                    long imageAddress = MemoryUtil.memAddress(imageBuffer);

                    image = new NativeImage(format == null ? NativeImage.Format.getFormat(f.get(0)) :
                            format, w.get(0), h.get(0), true);

                    MemoryUtil.memCopy(imageAddress, MemFix.sharedMemoryPool.getPoolPointer() +
                            ((MemoryHandle) (Object) image).getOffset(), image.sizeBytes);

                    STBImage.stbi_image_free(imageBuffer);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                } finally {
                    stack.close();
                }

                return image;
            }
        }
    }

    @Redirect(method = "*", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/texture/NativeImage;pointer:J", opcode = Opcodes.GETFIELD))
    private long onGetPointer(NativeImage image) {
        if (offset != -1) {
            return getPoolReference().getPoolPointer() + this.offset;
        } else {
            return 0;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NativeImageMixin that = (NativeImageMixin) o;
        return offset == that.offset;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public void setOffset(long offset) {
        this.offset = offset;
    }

    @Override
    public long getSize() {
        return this.sizeBytes;
    }
}

package io.github.ultimateboomer.memfix.mixin;

import io.github.ultimateboomer.memfix.MemFix;
import net.minecraft.client.texture.NativeImage;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;

@Mixin(NativeImage.class)
public class NativeImageMixin {
    @Shadow @Final private long sizeBytes;

    @Shadow public long pointer;

    @Unique
    public long offset;

    @Unique
    private static long poolPointer;

    @Unique
    private static long poolSize;

    @Unique
    private static SortedSet<NativeImage> pooledNativeImageSet;

    @Inject(method = "<clinit>", at = @At("HEAD"))
    private static void onClInit(CallbackInfo ci) {
        poolSize = 1L << 32;

        poolPointer = MemoryUtil.nmemAlloc(poolSize);
        for (long i = poolPointer; i < poolPointer + poolSize; ++i) {
            MemoryUtil.memPutByte(i, (byte) 0);
        }

        pooledNativeImageSet = new ConcurrentSkipListSet<>(
                Comparator.comparingLong(o -> ((NativeImageMixin) (Object) o).offset));
    }

    private static synchronized long allocate(long size) {
        long offset = findAddress(size);

        if (offset == -1) {
            // Remove gaps in allocation
            compress();
            offset = findAddress(size);

            while (offset == -1) {
                resize();
            }
        }

        return offset;
    }

    private static synchronized void resize() {
        long oldSize = poolSize;
        poolSize = poolSize << 1;
        MemFix.LOGGER.warn("Resizing pool to {} bytes", poolSize);

        long old = poolPointer;
        poolPointer = MemoryUtil.nmemRealloc(poolPointer, poolSize);

        if (poolPointer != old) {
            pooledNativeImageSet.forEach(image -> {
                ((NativeImageMixin) (Object) image).recalculatePointer();
            });
        }

        for (long i = poolPointer + oldSize; i < poolPointer + poolSize; ++i) {
            MemoryUtil.memPutByte(i, (byte) 0);
        }
    }

    private static synchronized long findAddress(long size) {
        // Find address to allocate
        if (pooledNativeImageSet.isEmpty()) {
            return 0;
        }

        long blockAddress = 0;
        long blockEnd;
        boolean found = false;

        for (NativeImage image : pooledNativeImageSet) {
            blockEnd = ((NativeImageMixin) (Object) image).offset;

            if (blockEnd - blockAddress > size) {
                found = true;
                break;
            }

            blockAddress = ((NativeImageMixin) (Object) image).offset + image.sizeBytes;
        }

        if (!found) {
            blockEnd = poolSize;

            if (blockEnd - blockAddress > size) {
                found = true;
            }
        }

        if (found) {
            return blockAddress;
        } else {
            return -1;
        }
    }

    private static synchronized void compress() {
        MemFix.LOGGER.info("Compressing pool");

        long offset = 0;
        for (NativeImage image : pooledNativeImageSet) {
            if (((NativeImageMixin) (Object) image).offset != offset) {
                MemoryStack stack = MemoryStack.stackPush();
                long tmp = stack.nmalloc((int) image.sizeBytes);

                MemoryUtil.memCopy(image.pointer, tmp, image.sizeBytes);
                MemoryUtil.memCopy(tmp, poolPointer + offset, image.sizeBytes);

                ((NativeImageMixin) (Object) image).offset = offset;
                ((NativeImageMixin) (Object) image).recalculatePointer();

                stack.pop();

            }
            offset += image.sizeBytes;
        }
    }

    public synchronized void clear() {
        pooledNativeImageSet.forEach(image -> {
            ((NativeImageMixin) (Object) image).markClosed();
        });
        pooledNativeImageSet.clear();
    }


    @Redirect(method = "<init>(Lnet/minecraft/client/texture/NativeImage$Format;IIZ)V",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/system/MemoryUtil;nmemAlloc(J)J"))
    private long onInit(long size) {
        initImage(size, true, (byte) 0);
        pooledNativeImageSet.add(((NativeImage) (Object) this));
        return this.pointer;
    }

    @Redirect(method = "<init>(Lnet/minecraft/client/texture/NativeImage$Format;IIZ)V",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/system/MemoryUtil;nmemCalloc(JJ)J"))
    private long onInit(long num, long size) {
        initImage(size, true, (byte) 1);
        pooledNativeImageSet.add(((NativeImage) (Object) this));
        return this.pointer;
    }

    private long initImage(long size, boolean init, byte num) {
        this.offset = allocate(size);
        recalculatePointer();

        if (init) {
            for (long i = this.pointer; i < this.pointer + this.sizeBytes; ++i) {
                MemoryUtil.memPutByte(i, num);
            }
        }

        MemFix.nativeImageList.add((NativeImage) (Object) this);

        return this.pointer;
    }

    @Inject(method = "<init>(Lnet/minecraft/client/texture/NativeImage$Format;IIZJ)V", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        throw new UnsupportedOperationException();
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void onClose(CallbackInfo ci) {
        MemFix.nativeImageList.remove(this);
    }


    /**
     * @author UltimateBoomer
     */
    @Overwrite
    public void close() {
        this.offset = -1;
        this.pointer = 0;

        pooledNativeImageSet.remove(this);
        MemFix.nativeImageList.remove(this);
    }

    private void markClosed() {
        this.pointer = 0;
    }

    public void recalculatePointer() {
        if (offset != -1) {
            this.pointer = poolPointer + this.offset;
        } else {
            throw new IllegalStateException("Image not allocated");
        }
    }

    /**
     * @author UltimateBoomer
     */
    @Overwrite
    public static NativeImage read(NativeImage.Format format, ByteBuffer buffer) throws IOException {
        if (format != null && !format.isWriteable()) {
            throw new UnsupportedOperationException("Don't know how to read format " + format);
        } else if (MemoryUtil.memAddress(buffer) == 0L) {
            throw new IllegalArgumentException("Invalid buffer");
        } else {
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

                MemoryUtil.memCopy(imageAddress, image.pointer, image.sizeBytes);

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

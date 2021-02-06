package io.github.ultimateboomer.memfix.mixin;

import io.github.ultimateboomer.memfix.MemFix;
import io.github.ultimateboomer.memfix.MemoryHandle;
import io.github.ultimateboomer.memfix.MemoryPool;
import net.minecraft.client.texture.NativeImage;
import org.apache.commons.lang3.tuple.Pair;
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
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;

@Mixin(NativeImage.class)
public class NativeImageMixin implements MemoryHandle {
    @Shadow @Final
    private long sizeBytes;

    @Shadow
    public long pointer;

    @Unique
    public long offset;

    @Unique
    private static long poolPointer;

    @Unique
    private static long poolSize;

    @Unique
    private static SortedSet<NativeImage> pooledNativeImageSet;

    // Left: Size   Right: Offset
    private static SortedSet<Pair<Long, Long>> availableBlocks;

    @Inject(method = "<clinit>", at = @At("HEAD"))
    private static void onClInit(CallbackInfo ci) {
        poolSize = 1L << 32;

        poolPointer = MemoryUtil.nmemAlloc(poolSize);
        for (long i = poolPointer; i < poolPointer + poolSize; ++i) {
            MemoryUtil.memPutByte(i, (byte) 0);
        }

        pooledNativeImageSet = new ConcurrentSkipListSet<>(
                Comparator.comparingLong(o -> ((NativeImageMixin) (Object) o).offset));

        availableBlocks = new ConcurrentSkipListSet<>();
        availableBlocks.add(Pair.of(poolSize, 0L));
    }

    private static long allocate(long size) {
        synchronized (NativeImage.class) {
            long offset = findAddress(size);

            if (offset == -1) {
                // Remove gaps in allocation
                compress();
                offset = findAddress(size);

                while (offset == -1) {
                    resize();
                    offset = findAddress(size);
                }
            }

            return offset;
        }
    }

    private static void resize() {
        synchronized (NativeImage.class) {
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
    }

    private static long findAddress(long size) {
        synchronized (NativeImage.class) {
            // Find address to allocate
            for (Iterator<Pair<Long, Long>> itr = availableBlocks.iterator(); itr.hasNext(); ) {
                Pair<Long, Long> block = itr.next();
                long diff = block.getLeft() - size;
                if (diff >= 0) {
                    itr.remove();
                    if (diff > 0) {
                        availableBlocks.add(Pair.of(diff, block.getRight() + size));
                    }

                    return block.getRight();
                }
            }
            return -1;
        }
    }

    private static void compress() {
        MemFix.LOGGER.info("Compressing pool");

        synchronized (NativeImage.class) {
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

            availableBlocks.clear();
            availableBlocks.add(Pair.of(poolSize - offset, offset));
        }
    }

    private static void clear() {
        synchronized (NativeImage.class) {
            pooledNativeImageSet.forEach(image -> {
                ((NativeImageMixin) (Object) image).markClosed();
            });
            pooledNativeImageSet.clear();

            availableBlocks.clear();
            availableBlocks.add(Pair.of(poolSize, 0L));
        }
    }

    private static void cleanAvailableBlocks() {
        synchronized (NativeImage.class) {
            Iterator<Pair<Long, Long>> iterator = availableBlocks.iterator();
            Pair<Long, Long> prev = iterator.next();
            for (; iterator.hasNext(); ) {
                Pair<Long, Long> next = iterator.next();

                if (prev.getLeft() + prev.getRight() == next.getLeft()) {
                    iterator.remove();
                    availableBlocks.remove(prev);
                    availableBlocks.add(Pair.of(prev.getLeft() + next.getLeft(), prev.getRight()));
                }

                prev = next;
            }
        }
    }

    @Redirect(method = "<init>(Lnet/minecraft/client/texture/NativeImage$Format;IIZ)V",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/system/MemoryUtil;nmemAlloc(J)J"))
    private long onInit(long size) {
        initImage(size, false, (byte) 0);
        return this.pointer;
    }

    @Redirect(method = "<init>(Lnet/minecraft/client/texture/NativeImage$Format;IIZ)V",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/system/MemoryUtil;nmemCalloc(JJ)J"))
    private long onInit(long num, long size) {
        initImage(size, true, (byte) 0);
        return this.pointer;
    }

    private long initImage(long size, boolean init, byte num) {
       synchronized (NativeImage.class) {
           this.offset = allocate(size);
           recalculatePointer();

           if (init) {
               for (long i = this.pointer; i < this.pointer + this.sizeBytes; ++i) {
                   MemoryUtil.memPutByte(i, num);
               }
           }

           MemFix.nativeImageList.add((NativeImage) (Object) this);
           pooledNativeImageSet.add(((NativeImage) (Object) this));

           return this.pointer;
       }
    }

    @Inject(method = "<init>(Lnet/minecraft/client/texture/NativeImage$Format;IIZJ)V", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        throw new UnsupportedOperationException();
    }

//    @Inject(method = "close", at = @At("HEAD"))
//    private void onClose(CallbackInfo ci) {
//        MemFix.nativeImageList.remove(this);
//    }

    @Inject(method = "close", at = @At("HEAD"), cancellable = true)
    public void close(CallbackInfo ci) {
        synchronized (NativeImage.class) {
            //pooledNativeImageSet.remove(this);
            MemFix.nativeImageList.remove(this);

            availableBlocks.add(Pair.of(this.sizeBytes, this.offset));

            this.offset = -1;
            this.pointer = 0;
            ci.cancel();
        }
    }

    private void markClosed() {
        this.pointer = 0;
        MemFix.nativeImageList.remove(this);
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

                    MemoryUtil.memCopy(imageAddress, poolPointer + ((NativeImageMixin) (Object) image).offset,
                            image.sizeBytes);

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
        return poolPointer + this.offset;
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
    public void setPoolReference(MemoryPool pool) {

    }
}

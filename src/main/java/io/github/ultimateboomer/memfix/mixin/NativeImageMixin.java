package io.github.ultimateboomer.memfix.mixin;

import io.github.ultimateboomer.memfix.MemFix;
import net.minecraft.client.texture.NativeImage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NativeImage.class)
public class NativeImageMixin {
    @Shadow @Final private long sizeBytes;

    @Shadow public long pointer;

//    public boolean closeOnReload = false;

//    private boolean willClose = true;

    @Inject(method = "<init>*", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/texture/NativeImage;pointer:J"))
    private void onInit(CallbackInfo ci) {
        MemFix.nativeImageList.add((NativeImage) (Object) this);
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void onClose1(CallbackInfo ci) {
        assert MemFix.nativeImageList.contains(this);

        MemFix.nativeImageList.remove(this);
    }

//    @Redirect(method = "<init>(Lnet/minecraft/client/texture/NativeImage$Format;IIZ)V",
//            at = @At(value = "INVOKE", target = "Lorg/lwjgl/system/MemoryUtil;nmemAlloc(J)J"))
//    private long onInit(long size) {
//        this.willClose = false;
//        long pointer = poolPointer(size);
//        if (pointer == 0) {
//            pointer = MemoryUtil.nmemAlloc(size);
//        }
//
//        return pointer;
//    }
//
//    @Redirect(method = "<init>(Lnet/minecraft/client/texture/NativeImage$Format;IIZ)V",
//            at = @At(value = "INVOKE", target = "Lorg/lwjgl/system/MemoryUtil;nmemCalloc(JJ)J"))
//    private long onInit(long num, long size) {
//        this.willClose = false;
//        long pointer = poolPointer(this.sizeBytes);
//        if (pointer == 0) {
//            pointer = MemoryUtil.nmemCalloc(num, size);
//        }
//
//        return pointer;
//    }
//
//    @Inject(method = "close", at = @At("HEAD"), cancellable = true)
//    private void onClose(CallbackInfo ci) {
//        if (!willClose) {
//            Queue<Long> pointerQueue = MemFix.pointerPool.get(this.sizeBytes);
//
//            if (pointerQueue == null) {
//                pointerQueue = Queues.newPriorityBlockingQueue();
//                MemFix.pointerPool.put(this.sizeBytes, pointerQueue);
//            }
//
//            pointerQueue.add(this.pointer);
//
//            this.pointer = 0;
//            ci.cancel();
//        }
//    }
//
//    private static long poolPointer(long sizeBytes) {
//        Queue<Long> pointerQueue = MemFix.pointerPool.get(sizeBytes);
//        if (pointerQueue == null || pointerQueue.isEmpty()) {
//            return 0;
//        }
//
//        return pointerQueue.poll();
//    }
//
//    /**
//     * @author UltimateBoomer
//     */
//    @Overwrite
//    public static NativeImage read(@Nullable NativeImage.Format format, ByteBuffer byteBuffer) throws IOException {
//        if (format != null && !format.isWriteable()) {
//            throw new UnsupportedOperationException("Don't know how to read format " + format);
//        } else if (MemoryUtil.memAddress(byteBuffer) == 0L) {
//            throw new IllegalArgumentException("Invalid buffer");
//        } else {
//            MemoryStack memoryStack = MemoryStack.stackPush();
//            Throwable var3 = null;
//
//            NativeImage var8;
//            try {
//                IntBuffer intBuffer = memoryStack.mallocInt(1);
//                IntBuffer intBuffer2 = memoryStack.mallocInt(1);
//                IntBuffer intBuffer3 = memoryStack.mallocInt(1);
//                ByteBuffer byteBuffer2 = STBImage.stbi_load_from_memory(byteBuffer, intBuffer, intBuffer2, intBuffer3,
//                        format == null ? 0 : format.getChannelCount());
//                if (byteBuffer2 == null) {
//                    throw new IOException("Could not load image: " + STBImage.stbi_failure_reason());
//                }
//                long bufferPointer = MemoryUtil.memAddress(byteBuffer2);
//                long bufferSize = byteBuffer2.remaining();
//
//                long pooledPointer = poolPointer(bufferSize);
//
//                if (pooledPointer == 0) {
//                    var8 = new NativeImage(format == null ? NativeImage.Format.getFormat(intBuffer3.get(0)) : format,
//                            intBuffer.get(0), intBuffer2.get(0), true, bufferPointer);
//                } else {
//                    MemoryUtil.memCopy(bufferPointer, pooledPointer, bufferSize);
//                    STBImage.stbi_image_free(byteBuffer2);
//
//                    var8 = new NativeImage(format == null ? NativeImage.Format.getFormat(intBuffer3.get(0)) : format,
//                            intBuffer.get(0), intBuffer2.get(0), true, pooledPointer);
//                    ((NativeImageMixin) (Object) var8).willClose = false;
//                }
//
//            } catch (Throwable var17) {
//                var3 = var17;
//                throw var17;
//            } finally {
//                if (var3 != null) {
//                    try {
//                        memoryStack.close();
//                    } catch (Throwable var16) {
//                        var3.addSuppressed(var16);
//                    }
//                } else {
//                    memoryStack.close();
//                }
//
//            }
//
//            return var8;
//        }
//    }
}

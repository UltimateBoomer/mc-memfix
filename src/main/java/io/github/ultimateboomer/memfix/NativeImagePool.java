package io.github.ultimateboomer.memfix;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.TextureUtil;
import org.apache.commons.io.IOUtils;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;

public class NativeImagePool implements AutoCloseable {
    public final long poolPointer;

    public long poolSize;

    public final SortedSet<PooledNativeImage> pooledNativeImageSet = new ConcurrentSkipListSet<>(
            Comparator.comparingLong(o -> o.pointer));

    public NativeImagePool(long poolSize) {
        this.poolSize = poolSize;

        poolPointer = MemoryUtil.nmemAlloc(poolSize);
        for (long i = poolPointer; i < poolPointer + poolSize; ++i) {
            MemoryUtil.memPutByte(i, (byte) 0);
        }
//        try {
//            CompletableFuture.runAsync(() -> LongStream.range(poolPointer, poolPointer + poolSize).parallel().forEach(
//                    i -> MemoryUtil.memPutByte(i, (byte) 0))).get();
//        } catch (Exception e) {
//            throw new IllegalStateException(e);
//        }
    }

    public synchronized long allocate(long size) {
        // Find address to allocate
        if (pooledNativeImageSet.isEmpty()) {
            return poolPointer;
        }

        long blockAddress = poolPointer;
        long blockEnd;
        boolean found = false;

        for (PooledNativeImage image : pooledNativeImageSet) {
            blockEnd = image.pointer;

            if (blockEnd - blockAddress > size) {
                found = true;
                break;
            }

            blockAddress = image.pointer + image.sizeBytes;
        }

        blockEnd = poolPointer + poolSize;

        if (blockEnd - blockAddress > size) {
            found = true;
        }

        if (found) {
            return blockAddress;
        } else {
            return 0;
        }
    }

    public PooledNativeImage read(NativeImage.Format format, InputStream inputStream) throws IOException {
        ByteBuffer byteBuffer = null;

        PooledNativeImage var3;
        try {
            byteBuffer = TextureUtil.readAllToByteBuffer(inputStream);
            byteBuffer.rewind();
            var3 = read(format, byteBuffer);
        } finally {
            MemoryUtil.memFree(byteBuffer);
            IOUtils.closeQuietly(inputStream);
        }

        return var3;
    }

    public PooledNativeImage read(NativeImage.Format format, ByteBuffer byteBuffer) throws IOException {
        if (format != null && !format.isWriteable()) {
            throw new UnsupportedOperationException("Don't know how to read format " + format);
        } else if (MemoryUtil.memAddress(byteBuffer) == 0L) {
            throw new IllegalArgumentException("Invalid buffer");
        } else {
            MemoryStack stack = MemoryStack.stackPush();
            Throwable err = null;

            PooledNativeImage pooledNativeImage;
            try {
                IntBuffer imageFormat = stack.mallocInt(1);
                IntBuffer width = stack.mallocInt(1);
                IntBuffer height = stack.mallocInt(1);
                ByteBuffer imageBuffer = STBImage.stbi_load_from_memory(byteBuffer, imageFormat, width, height,
                        format == null ? 0 : format.getChannelCount());
                if (imageBuffer == null) {
                    throw new IOException("Could not load image: " + STBImage.stbi_failure_reason());
                }
                long imageAddress = MemoryUtil.memAddress(imageBuffer);

                pooledNativeImage = new PooledNativeImage(format == null ? NativeImage.Format.getFormat(height.get(0)) :
                        format, imageFormat.get(0), width.get(0), true);

                MemoryUtil.memCopy(imageAddress, pooledNativeImage.pointer,
                        pooledNativeImage.sizeBytes);

                STBImage.stbi_image_free(imageBuffer);

            } catch (Throwable var17) {
                err = var17;
                throw var17;
            } finally {
                if (stack != null) {
                    if (err != null) {
                        try {
                            stack.close();
                        } catch (Throwable var16) {
                            err.addSuppressed(var16);
                        }
                    } else {
                        stack.close();
                    }
                }

            }

            return pooledNativeImage;
        }
    }

    @Override
    public void close() {
        pooledNativeImageSet.forEach(PooledNativeImage::markClosed);

        MemoryUtil.nmemFree(poolPointer);
    }

    public class PooledNativeImage extends NativeImage {
        public PooledNativeImage(int width, int height, boolean useStb) {
            this(Format.ABGR, width, height, useStb);
        }

        public PooledNativeImage(NativeImage.Format format, int width, int height, boolean useStb) {
            super(format, width, height, useStb, 0);
            this.pointer = allocate(this.sizeBytes);

            if (this.pointer == 0) {
                throw new IllegalStateException("NativeImagePool out of memory");
            }

            pooledNativeImageSet.add(this);
        }

        @Override
        public void close() {
            this.pointer = 0;

            pooledNativeImageSet.remove(this);
        }

        protected void markClosed() {
            this.pointer = 0;
        }
    }
}

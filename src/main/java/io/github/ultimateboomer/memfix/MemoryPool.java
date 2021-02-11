package io.github.ultimateboomer.memfix;

import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.system.MemoryUtil;

import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Simple memory pool implementation
 */
public class MemoryPool {
    private long poolPointer;
    private long poolSize;

    private SortedSet<MemoryHandle> handles;

    private SortedSet<Pair<Long, Long>> availableBlocks;

    public MemoryPool(long initialSize) {
        this.poolSize = initialSize;

        // Allocate
        this.poolPointer = MemoryUtil.nmemAlloc(this.poolSize);
        for (long i = this.poolPointer; i < this.poolPointer + this.poolSize; ++i) {
            MemoryUtil.memPutByte(i, (byte) 0);
        }

        this.handles = new ConcurrentSkipListSet<>(
                Comparator.comparingLong(MemoryHandle::getOffset));

        this.availableBlocks = new ConcurrentSkipListSet<>();
        this.availableBlocks.add(Pair.of(this.poolSize, 0L));
    }

    public synchronized long allocate(MemoryHandle handle) {
        long size = handle.getSize();
        long offset = this.findAddress(size);

        if (offset == -1) {
            this.compress();
            offset = this.findAddress(size);

            while (offset == -1) {
                this.resize();
                offset = this.findAddress(size);
            }
        }

        handle.setOffset(offset);
        handle.recalculatePointer(this.poolPointer);
        handles.add(handle);
        return offset;
    }

    public synchronized void closeHandle(MemoryHandle handle) {
        if (this.handles.remove(handle)) {
            availableBlocks.add(Pair.of(handle.getSize(), handle.getOffset()));
        }
    }

    public synchronized void resize() {
        throw new OutOfMemoryError("Not enough memory for shared memory pool");

//        long oldSize = this.poolSize;
//        this.poolSize <<= 1;
//        MemFix.LOGGER.warn("Resizing pool to {} bytes", this.poolSize);
//
//        long old = this.poolPointer;
//        this.poolPointer = MemoryUtil.nmemRealloc(this.poolPointer, this.poolSize);
//
//        if (poolPointer != old) {
//            handles.forEach(handle -> handle.recalculatePointer(this.poolPointer));
//        }
//
//        for (long i = poolPointer + oldSize; i < poolPointer + poolSize; ++i) {
//            MemoryUtil.memPutByte(i, (byte) 0);
//        }
    }

    private synchronized long findAddress(long size) {
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

    public synchronized void compress() {
        MemFix.LOGGER.info("Compressing pool");
        throw new OutOfMemoryError("Not enough memory for shared memory pool");
//        long offset = 0;
//        for (MemoryHandle handle : handles) {
//            if (handle.getOffset() != offset) {
//                MemoryStack stack = MemoryStack.stackPush();
//                long tmp = stack.nmalloc((int) handle.getSize());
//
//                MemoryUtil.memCopy(this.poolPointer + handle.getOffset(), tmp, handle.getSize());
//                MemoryUtil.memCopy(tmp, poolPointer + offset, handle.getSize());
//
//                handle.setOffset(offset);
//                handle.recalculatePointer(this.poolPointer);
//
//                stack.pop();
//
//            }
//            offset += handle.getSize();
//        }
//
//        availableBlocks.clear();
//        availableBlocks.add(Pair.of(poolSize - offset, offset));
    }

    public synchronized void clear() {
        handles.forEach(MemoryHandle::markClosed);
        handles.clear();

        availableBlocks.clear();
        availableBlocks.add(Pair.of(poolSize, 0L));
    }

    public synchronized void cleanAvailableBlocks() {
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

    public long getPoolPointer() {
        return poolPointer;
    }

    public long getPoolSize() {
        return poolSize;
    }

    public long getUsage() {
        return availableBlocks.last().getRight();
    }

    public long getFill() {
        return handles.stream().mapToLong(MemoryHandle::getSize).sum();
    }

    public int getHandleCount() {
        return handles.size();
    }
}

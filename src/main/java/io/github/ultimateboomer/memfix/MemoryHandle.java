package io.github.ultimateboomer.memfix;

public interface MemoryHandle {
    long getOffset();
    void setOffset(long offset);

    long getSize();

    default void recalculatePointer(long poolPointer) {}

    default void markClosed() {}

    default MemoryPool getPoolReference() {
        return MemFix.getSharedMemoryPool();
    }
}

package io.github.ultimateboomer.memfix;

public interface MemoryHandle {
    long getOffset();
    void setOffset(long offset);

    void setPoolReference(MemoryPool pool);
}

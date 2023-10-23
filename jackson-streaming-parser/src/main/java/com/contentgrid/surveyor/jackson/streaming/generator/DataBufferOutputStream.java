package com.contentgrid.surveyor.jackson.streaming.generator;

import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Consumer;
import lombok.NonNull;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;

public class DataBufferOutputStream extends OutputStream {

    public DataBufferOutputStream(@NonNull DataBufferFactory bufferFactory, @NonNull Consumer<DataBuffer> finishedBuffer) {
        this.bufferFactory = bufferFactory;
        this.finishedBuffer = finishedBuffer;
    }

    private DataBufferFactory bufferFactory;
    private Consumer<DataBuffer> finishedBuffer;
    private DataBuffer currentBuffer;

    private DataBuffer newBuffer(int minSize) throws IOException {
        if(bufferFactory == null) {
            throw new IOException("Stream closed");
        }
        return bufferFactory.allocateBuffer(Math.min(minSize, 1024)); // Buffers of 1k minimum
    }

    private DataBuffer ensureBuffer(int minSize) throws IOException {
        if(currentBuffer == null) {
            currentBuffer = newBuffer(minSize);
        } else if(currentBuffer.writableByteCount() < minSize) {
            finishedBuffer.accept(currentBuffer);
            currentBuffer = newBuffer(minSize);
        }
        return currentBuffer;
    }

    @Override
    public void write(int b) throws IOException {
        ensureBuffer(1).write((byte)b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        ensureBuffer(b.length).write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        var buf = ensureBuffer(len);

        for(int i = off; i < off + len; i++) {
            buf.write(b[i]);
        }
    }

    @Override
    public void flush() throws IOException {
        if(finishedBuffer == null) {
            throw new IOException("Stream closed");
        }
        finishedBuffer.accept(currentBuffer);
        currentBuffer = null;
    }

    @Override
    public void close() throws IOException {
        flush();
        bufferFactory = null;
        finishedBuffer = null;
    }
}

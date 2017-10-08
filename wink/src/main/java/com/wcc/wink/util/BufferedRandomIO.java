package com.wcc.wink.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by wenbiao.xie on 2016/8/25.
 */
public class BufferedRandomIO extends RandomAccessFile {
    private final static int DEFAULT_BUFFER_SIZE = 1024 * 8;

    private byte[] bufferBytes;
    private int buffered;
    private long seekPosition;
    private final boolean reading;
    private long fileLength;
    private long filePosition;

    public BufferedRandomIO(File file, String mode, int bufsize, boolean reading) throws FileNotFoundException {
        super(file, mode);
        if (bufsize <= 0 )
            throw new IllegalArgumentException("parameter bufsize should > 0");

        fileLength = file.length();
        bufferBytes = new byte[bufsize];
        this.reading = reading;
    }

    public BufferedRandomIO(File file, String mode, boolean reading) throws FileNotFoundException {
        this(file, mode, DEFAULT_BUFFER_SIZE, reading);
    }

    public BufferedRandomIO(String fileName, String mode, boolean reading) throws FileNotFoundException {
        this(new File(fileName), mode, DEFAULT_BUFFER_SIZE, reading);
    }

    @Override
    public void close() throws IOException {
        if (!reading) {
            flush();
        }
        super.close();
    }

    private void flush() throws IOException {
        if (buffered == 0) {
            return;
        }

        if (!reading) {
            super.write(bufferBytes, 0, buffered);
            seekPosition += buffered;
        }

        buffered = 0;
    }

    @Override
    public long length() throws IOException {
        return fileLength;
    }

    private int updateBuffer() throws IOException {
        buffered = super.read(bufferBytes, 0, bufferBytes.length);
        if (buffered > 0) {
            seekPosition += buffered;
        }
        return buffered;
    }

    @Override
    public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
        if (byteOffset >= buffer.length || byteCount <= 0)
            throw new IllegalArgumentException("byteOffset or byteCount illegal");

        if (!reading) {
            return super.read(buffer, byteOffset, byteCount);
        }

        if (byteOffset < 0) byteOffset = 0;
        if (byteCount + byteOffset > buffer.length)
            byteCount = buffer.length - byteOffset;

        if (byteCount == 0)
            return 0;

        int max = buffered;
        long available = fileLength - filePosition;
        if (available <= 0 && max == 0)
            return -1;

        if (byteCount > available)
            byteCount = (int) available;

        if ( max <= 0) {
            max = updateBuffer();
            if (max < 0)
                return max;
        }

        if (byteCount > max)
            byteCount = max;

        if (byteCount > 0) {
            System.arraycopy(bufferBytes, 0, buffer, byteOffset, byteCount);
            buffered -= byteCount;
            filePosition += byteCount;
        }

        return byteCount;
    }

    @Override
    public void seek(long offset) throws IOException {
        if (offset < 0) {
            throw new IOException("offset < 0: " + offset);
        }

        if (seekPosition == offset)
            return;

        flush();

        if (seekPosition == offset)
            return;

        seekPosition = offset;
        if (offset > fileLength)
            seekPosition = fileLength;
        else if (offset < 0)
            seekPosition = 0;

        filePosition = seekPosition;
        super.seek(seekPosition);
    }

    @Override
    public void setLength(long newLength) throws IOException {
        super.setLength(newLength);
        fileLength = newLength;
    }

    @Override
    public void write(byte[] buffer, int byteOffset, int byteCount) throws IOException {
        if (byteOffset >= buffer.length || byteCount <= 0)
            return;

        if (reading) {
            super.write(buffer, byteOffset, byteCount);
            return;
        }

        if (byteOffset < 0) byteOffset = 0;
        if (byteCount + byteOffset > buffer.length)
            byteCount = buffer.length - byteOffset;

        int from = byteOffset;
        int remaining= byteCount;
        int max = bufferBytes.length - buffered;

        while (remaining > 0) {
            int added = Math.min(remaining, max);
            System.arraycopy(buffer, from, bufferBytes, buffered, added);
            remaining -= added;
            max -= added;
            buffered += added;
            from += added;
            filePosition += added;

            if (filePosition > fileLength) {
                fileLength = filePosition;
            }

            if (max == 0 || filePosition == fileLength) {
                flush();
                max = bufferBytes.length;
            }
        }

    }
}

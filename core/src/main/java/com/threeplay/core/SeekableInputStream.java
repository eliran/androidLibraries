package com.threeplay.core;

import android.support.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by eliranbe on 5/22/16.
 */
public class SeekableInputStream {
    private RandomAccessInput input;
    private long base, offset;
    private long maximumLength;

    public static SeekableInputStream fromBytes(byte[] bytes) {
        return new SeekableInputStream(new RandomAccessBytes(bytes));
    }

    public static SeekableInputStream fromFile(File file) {
        try {
            return new SeekableInputStream(new FileInput(new RandomAccessFile(file, "r")));
        } catch ( IOException e ) {
            e.printStackTrace();
            return null;
        }
    }
    public SeekableInputStream(RandomAccessInput input) {
        this(input, 0, input.size());
    }

    public SeekableInputStream(RandomAccessInput input, long baseOffset, long maximumLength) {
        this.input = input;
        this.base = baseOffset;
        this.maximumLength = maximumLength;
    }

    public SeekableInputStream substream() {
        return substream(offset, availableBytes());
    }

    public SeekableInputStream substream(long offset, long length) {
        return new SeekableInputStream(input, base + offset, length);
    }

    public InputStream getInputStream() {
        return new InputStream(this.substream());
    }

    public long availableBytes() {
        return maximumLength - offset;
    }

    public void seek(long newOffset) {
        if (newOffset > maximumLength) newOffset = maximumLength;
        offset = newOffset;
    }

    public void rewind(){
        offset = 0;
    }

    public long skip(long count) {
        long maxSkip = availableBytes();
        if (count > maxSkip) count = maxSkip;
        offset += count;
        return count;
    }

    public int nextBytes(byte[] buf, int bufOffset, int length) {
        long maxLength = availableBytes();
        if (length > maxLength) length = (int) maxLength;
        int byteCount = input.read(buf, bufOffset, base + offset, length);
        if (byteCount <= 0) return -1;
        offset += byteCount;
        return byteCount;

    }

    public int nextBytes(byte[] buf) {
        return nextBytes(buf, 0, buf.length);
    }

    public int nextByte() {
        byte[] by = new byte[1];
        if (nextBytes(by) != 1) return -1;
        return by[0];
    }

    public int next16LEbits() {
        byte[] by = new byte[2];
        if (nextBytes(by) != by.length) return -1;
        int result = ((int)by[1])&0xff;
        return result<<8 | (((int)by[0])&0xff);
    }

    public int next32LEbits() {
        byte[] by = new byte[4];
        if (nextBytes(by) != by.length) return -1;
        int result = ((int)by[3])&0xff;
        result = result<<8 | (((int)by[2])&0xff);
        result = result<<8 | (((int)by[1])&0xff);
        return result<<8 | (((int)by[0])&0xff);
    }

    public int next16BEbits() {
        byte[] by = new byte[2];
        if (nextBytes(by) != 2) return -1;
        int result = ((int)by[0])&0xff;
        return result << 8 | (((int)by[1])&0xff);
    }

    public int next32BEbits() {
        byte[] by = new byte[4];
        if (nextBytes(by) != by.length) return -1;
        int result = ((int)by[0])&0xff;
        result = result<<8 | (((int)by[1])&0xff);
        result = result<<8 | (((int)by[2])&0xff);
        return result<<8 | (((int)by[3])&0xff);
    }

    static class InputStream extends java.io.InputStream {
        private final SeekableInputStream stream;
        public InputStream(SeekableInputStream stream){
            this.stream = stream;
        }

        @Override
        public int available() throws IOException {
            return (int)stream.availableBytes();
        }

        @Override
        public int read(@NonNull byte[] buffer) throws IOException {
            return stream.nextBytes(buffer);
        }

        @Override
        public int read(@NonNull byte[] buffer, int byteOffset, int byteCount) throws IOException {
            return stream.nextBytes(buffer, byteOffset, byteCount);
        }

        @Override
        public long skip(long byteCount) throws IOException {
            return stream.skip(byteCount);
        }

        @Override
        public int read() throws IOException {
            return stream.nextByte();
        }
    }

    static class RandomAccessBytes implements RandomAccessInput {
        private byte[] bytes;
        public RandomAccessBytes(byte[] bytes){
            this.bytes = bytes;
        }

        @Override
        public int read(byte[] buf, int bufOffset, long inputOffset, int length) {
            long maxLength = Math.min(length, size() - inputOffset);
            if ( maxLength > 0 ) {
                System.arraycopy(bytes, (int) inputOffset, buf, bufOffset, length);
                return (int)maxLength;
            }
            return -1;
        }

        @Override
        public long size() {
            return bytes.length;
        }
    }

    static class FileInput implements RandomAccessInput {
        private final RandomAccessFile file;

        public FileInput(RandomAccessFile file){
            this.file = file;
        }

        @Override
        public int read(byte[] buf, int bufOffset, long inputOffset, int length) {
            try {
                file.seek(inputOffset);
                return file.read(buf, bufOffset, length);
            } catch ( IOException ok ) {
                return -1;
            }
        }

        @Override
        public long size() {
            try { return file.length(); } catch ( IOException ok ) {};
            return 0;
        }
    }
}

package com.threeplay.core;

/**
 * Created by eliranbe on 5/21/16.
 */
public interface RandomAccessInput {
    int read(byte[] buf, int bufOffset, long inputOffset, int length);
    long size();
}

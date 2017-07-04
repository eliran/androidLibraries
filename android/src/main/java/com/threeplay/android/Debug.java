package com.threeplay.android;

import android.util.Log;

import java.text.NumberFormat;

/**
 * Created by eliranbe on 12/29/16.
 */

public class Debug {
    private static NumberFormat numberFormat = NumberFormat.getNumberInstance();

    public static void logMemory(String Tag, String when) {
        Runtime rt = Runtime.getRuntime();
        long maxMemory = rt.maxMemory();
        long totalMemory = rt.totalMemory();
        long freeMemory = rt.freeMemory();
        Log.d(Tag+"-Memory", "When: '" + when +
                "' Max " + numberFormat.format(maxMemory) +
                " Total " + numberFormat.format(totalMemory) +
                " Free " + numberFormat.format(freeMemory) +
                " Native: Alloc " + numberFormat.format(android.os.Debug.getNativeHeapAllocatedSize()) +
                " Free " + numberFormat.format(android.os.Debug.getNativeHeapFreeSize()) +
                " Total " + numberFormat.format(android.os.Debug.getNativeHeapSize())
        );
    }

}

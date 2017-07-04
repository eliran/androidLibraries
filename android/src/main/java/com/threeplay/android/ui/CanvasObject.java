package com.threeplay.android.ui;

import android.graphics.Canvas;
import android.graphics.RectF;

/**
 * Created by eliranbe on 3/8/17.
 */

public interface CanvasObject {
    void drawOnCanvas(Canvas canvas, RectF contentBounds);
}

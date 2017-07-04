package com.threeplay.android.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by eliranbe on 2/9/17.
 */
public class EmptyDivider extends DividerItemDecoration {
    private int spacing;

    public EmptyDivider(Context context, int orientation) {
        super(context, orientation);
    }

    @Override
    public void setOrientation(int orientation) {
        super.setOrientation(orientation);
    }

    @Override
    public void setDrawable(@NonNull Drawable drawable) {
        super.setDrawable(drawable);
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        if (outRect.left != 0) {
            outRect.left = spacing;
        }
        if (outRect.right != 0) {
            outRect.right = spacing;
        }
        if (outRect.top != 0) {
            outRect.top = spacing;
        }
        if (outRect.bottom != 0) {
            outRect.bottom = spacing;
        }
    }

    public void setSpacing(int spacing) {
        this.spacing = spacing;
    }
}

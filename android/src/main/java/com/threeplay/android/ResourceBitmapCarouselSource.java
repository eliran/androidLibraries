package com.threeplay.android;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.threeplay.core.Promise;

/**
 * Created by eliranbe on 11/29/16.
 */

public class ResourceBitmapCarouselSource extends BaseCarouselSource<Bitmap> {

    private final Resources resources;
    private final int[] ids;

    public ResourceBitmapCarouselSource(Resources resources, int[] ids){
        super(ids.length);
        this.resources = resources;
        this.ids = ids;
    }

    @Override
    protected Promise<Bitmap[]> retrieve(int offset, int count) {
        final Bitmap[] result = new Bitmap[count];
        for ( int i = 0; i < count; i++ ) {
            result[i] = loadBitmap((offset+i)%length);
        }
        return Promise.withResult(result);
    }

    private Bitmap loadBitmap(int index){
        return BitmapFactory.decodeResource(resources, ids[index]);
    }
}

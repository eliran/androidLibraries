package com.threeplay.android;

import android.graphics.Bitmap;

import com.threeplay.android.fetcher.ResourceFetcher;
import com.threeplay.core.Promise;

public class BitmapCarouselSource extends BaseCarouselSource<Bitmap> {
    private ResourceFetcher<Bitmap> fetcher;
    private String fileNameFormat;

    public BitmapCarouselSource(ResourceFetcher<Bitmap> bitmapFetcher, String fileNameFormat, int count){
        super(count);
        this.fetcher = bitmapFetcher;
        this.fileNameFormat = fileNameFormat;
    }

    @Override
    protected Promise<Bitmap[]> retrieve(int startOffset, final int countOfBitmaps){
        Promise<Bitmap>[] promises = new Promise[countOfBitmaps];
        for ( int i = 0; i < countOfBitmaps; i++ ) {
            promises[i] = fetcher.fetch(downloadPath(startOffset));
            startOffset = (startOffset+1)%length;
        }
        return Promise.allAs(Bitmap.class, promises);
    }

    private String downloadPath(int offset){
        return String.format(fileNameFormat, offset);
    }
}

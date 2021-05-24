package com.nmmedit.glide.avif;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;


public class AvifBitmapResource implements Resource<Bitmap> {
    private final BitmapPool bitmapPool;
    private final Bitmap mBitmap;

    public AvifBitmapResource(BitmapPool bitmapPool, Bitmap mBitmap) {
        this.bitmapPool = bitmapPool;
        this.mBitmap = mBitmap;
    }

    @NonNull
    @Override
    public Class<Bitmap> getResourceClass() {
        return Bitmap.class;
    }

    @NonNull
    @Override
    public Bitmap get() {
        return mBitmap;
    }


    @Override
    public int getSize() {
        return mBitmap.getByteCount();
    }

    @Override
    public void recycle() {
        bitmapPool.put(mBitmap);
    }
}

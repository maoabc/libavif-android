package com.nmmedit.glide.avif;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.nmmedit.glide.ImageDetector;
import com.nmmedit.glide.MyOptions;

import java.io.IOException;
import java.nio.ByteBuffer;

import libavif.AvifDecoder;
import libavif.AvifSequenceDrawable;

public class ByteBufferAvifSequenceDecoder implements ResourceDecoder<ByteBuffer, AvifSequenceDrawable> {

    private static final String TAG = "ByteBufferAvifSeqDecoder";

    private final AvifSequenceDrawable.BitmapProvider bitmapProvider;

    public ByteBufferAvifSequenceDecoder(@NonNull final BitmapPool bitmapPool) {
        bitmapProvider = new AvifSequenceDrawable.BitmapProvider() {
            @Override
            public Bitmap acquireBitmap(int minWidth, int minHeight) {
                return bitmapPool.getDirty(minWidth, minHeight, Bitmap.Config.ARGB_8888);
            }

            @Override
            public void releaseBitmap(Bitmap bitmap) {
                bitmapPool.put(bitmap);
            }
        };
    }

    @Override
    public boolean handles(@NonNull ByteBuffer source, @NonNull Options options) throws IOException {
        if (options.get(MyOptions.DISABLE_ANIMATION)) {
            return false;
        }
        return ImageDetector.isAvifs(source);
    }

    @Nullable
    @Override
    public Resource<AvifSequenceDrawable> decode(@NonNull ByteBuffer source, int width, int height, @NonNull Options options) throws IOException {

        final AvifDecoder avifDecoder = AvifDecoder.fromByteBuffer(source);
        if (avifDecoder == null) {
            return null;
        }

        final AvifSequenceDrawable drawable = new AvifSequenceDrawable(avifDecoder, bitmapProvider);

        if (options.get(MyOptions.LOOP_ONCE)) {
            drawable.setLoopBehavior(AvifSequenceDrawable.LOOP_FINITE);
            drawable.setLoopCount(1);
        } else {
            drawable.setLoopBehavior(AvifSequenceDrawable.LOOP_DEFAULT);
        }

        return new AvifSequenceDrawableResource(drawable);
    }

}

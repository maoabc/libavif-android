package com.nmmedit.glide.avif;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.nmmedit.glide.ImageDetector;

import java.io.IOException;
import java.nio.ByteBuffer;

import libavif.AvifDecoder;
import libavif.AvifImage;

public class ByteBufferAvifDecoder implements ResourceDecoder<ByteBuffer, Bitmap> {

    public static final String TAG = "ByteBufferAvifDecoder";

    @NonNull
    private final BitmapPool bitmapPool;

    public ByteBufferAvifDecoder(@NonNull final BitmapPool bitmapPool) {
        this.bitmapPool = bitmapPool;

    }

    @Override
    public boolean handles(@NonNull ByteBuffer source, @NonNull Options options) throws IOException {

        return ImageDetector.isAvif(source);
    }

    @Nullable
    @Override
    public Resource<Bitmap> decode(@NonNull ByteBuffer source, int width, int height, @NonNull Options options) throws IOException {

        final AvifDecoder decoder = AvifDecoder.fromByteBuffer(source);
        if (decoder == null) {
            return null;
        }
        if (!decoder.nextImage()) {
            return null;
        }

        final AvifImage image = decoder.getImage();
        final Bitmap bitmap = bitmapPool.getDirty(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
        decoder.getFrame(bitmap);

        decoder.destroy();

        return new AvifBitmapResource(bitmapPool, bitmap);

    }

}

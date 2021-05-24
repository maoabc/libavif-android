package com.nmmedit.glide.avif;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.nmmedit.glide.ImageDetector;
import com.nmmedit.glide.MyOptions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import libavif.AvifDecoder;
import libavif.AvifSequenceDrawable;

public class StreamAvifSequenceDecoder implements ResourceDecoder<InputStream, AvifSequenceDrawable> {

    public static final String TAG = "StreamAvifSeqDecoder";

    @NonNull
    private final ArrayPool byteArrayPool;
    private final AvifSequenceDrawable.BitmapProvider bitmapProvider;

    public StreamAvifSequenceDecoder(@NonNull final BitmapPool bitmapPool, @NonNull ArrayPool byteArrayPool) {
        this.byteArrayPool = byteArrayPool;
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
    public boolean handles(@NonNull InputStream source, @NonNull Options options) throws IOException {
        if (options.get(MyOptions.DISABLE_ANIMATION)) {
            return false;
        }
        return ImageDetector.isAvifs(source);
    }

    @Nullable
    @Override
    public Resource<AvifSequenceDrawable> decode(@NonNull InputStream source, int width, int height, @NonNull Options options) throws IOException {


        final byte[] bytes = inputStreamToBytes(source);
        if (bytes == null) {
            return null;
        }
        final AvifDecoder avifDecoder = AvifDecoder.fromByteArray(bytes);
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

    private byte[] inputStreamToBytes(InputStream is) {
        final int bufferSize = 16 * 1024;
        int dataLen;
        try {
            dataLen = is.available();
        } catch (IOException ignored) {
            dataLen = bufferSize;
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(dataLen);
        try {
            int nRead;
            byte[] data = byteArrayPool.get(bufferSize, byte[].class);
            while ((nRead = is.read(data)) != -1) {
                buffer.write(data, 0, nRead);
            }
            byteArrayPool.put(data);
            buffer.close();
        } catch (IOException e) {
            return null;
        }
        return buffer.toByteArray();
    }
}

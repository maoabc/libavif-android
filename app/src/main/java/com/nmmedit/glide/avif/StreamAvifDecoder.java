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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import libavif.AvifDecoder;
import libavif.AvifImage;

public class StreamAvifDecoder implements ResourceDecoder<InputStream, Bitmap> {

    public static final String TAG = "StreamAvifDecoder";

    @NonNull
    private final BitmapPool bitmapPool;
    @NonNull
    private final ArrayPool byteArrayPool;

    public StreamAvifDecoder(@NonNull final BitmapPool bitmapPool, @NonNull ArrayPool byteArrayPool) {
        this.bitmapPool = bitmapPool;
        this.byteArrayPool = byteArrayPool;
    }

    @Override
    public boolean handles(@NonNull InputStream source, @NonNull Options options) throws IOException {
        return ImageDetector.isAvif(source);
    }

    @Nullable
    @Override
    public Resource<Bitmap> decode(@NonNull InputStream source, int width, int height, @NonNull Options options) throws IOException {

        final byte[] bytes = inputStreamToBytes(source);
        if (bytes == null) {
            return null;
        }
        final AvifDecoder decoder = AvifDecoder.fromByteArray(bytes);
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

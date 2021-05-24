package libavif;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;

public class AvifDecoder {

    static {
        System.loadLibrary("avif-jni");
    }

    private AvifDecoder(long nDecoder) {
        this.nDecoder = nDecoder;
    }

    //struct MyAvifDecoder
    private volatile long nDecoder;


    public static AvifDecoder fromByteArray(byte[] bytes) {
        return fromByteArray(bytes, 0, bytes.length);
    }

    public static AvifDecoder fromByteArray(byte[] bytes, int off, int len) {
        final long nDecoder = createDecoderByteArray0(bytes, off, len);
        if (nDecoder == 0) {
            return null;
        }
        return new AvifDecoder(nDecoder);
    }

    public static AvifDecoder fromByteBuffer(ByteBuffer buffer) {
        if (buffer == null) throw new IllegalArgumentException("buffer==null");
        if (!buffer.isDirect()) {
            if (buffer.hasArray()) {
                byte[] byteArray = buffer.array();
                return fromByteArray(byteArray, buffer.position(), buffer.remaining());
            } else {
                throw new IllegalArgumentException("Cannot have non-direct ByteBuffer with no byte array");
            }
        }
        final long nDecoder = createDecoderByteBuffer0(buffer, buffer.position(), buffer.remaining());
        if (nDecoder == 0) {
            return null;
        }
        return new AvifDecoder(nDecoder);
    }

    public boolean nextImage() {
        checkDecoder();
        return nextImage0(nDecoder);
    }

    @NonNull
    public AvifImage getImage() {
        checkDecoder();
        return new AvifImage(getImage0(nDecoder));
    }

    @NonNull
    public Bitmap getFrame() {
        checkDecoder();
        final AvifImage image = getImage();
        final Bitmap bitmap = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
        getFrame0(nDecoder, bitmap);
        return bitmap;
    }

    public int getFrame(@NonNull Bitmap bitmap) {
        checkDecoder();
        return getFrame0(nDecoder, bitmap);
    }

    public int getImageCount() {
        checkDecoder();
        return getImageCount0(nDecoder);
    }

    public int getImageIndex() {
        checkDecoder();
        return getImageIndex0(nDecoder);
    }

    public void reset() {
        checkDecoder();
        reset0(nDecoder);
    }

    private void checkDecoder() {
        if (nDecoder == 0) {
            throw new IllegalStateException("Native Decoder already destroyed");
        }
    }

    public void destroy() {
        synchronized (this) {
            if (nDecoder != 0) {
                destroy0(nDecoder);
                nDecoder = 0;
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            destroy();
        } finally {
            super.finalize();
        }
    }

    private static native long createDecoderByteArray0(@NonNull byte[] bytes, int off, int len);

    private static native long createDecoderByteBuffer0(@NonNull ByteBuffer buffer, int off, int len);

    private static native boolean nextImage0(long nDecoder);

    private static native int getImageCount0(long nDecoder);

    private static native int getImageIndex0(long nDecoder);

    private static native int getImageLimit0(long nDecoder);

    private static native long getImage0(long nDecoder);

    private static native int getFrame0(long nDecoder, @NonNull Bitmap outBitmap);

    private static native void reset0(long nDecoder);

    private static native void destroy0(long nDecoder);
}

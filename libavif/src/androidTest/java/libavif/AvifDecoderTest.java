package libavif;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import libavif.AvifDecoder;
import libavif.AvifImage;

@RunWith(AndroidJUnit4.class)
public class AvifDecoderTest {

    @Before
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @Test
    public void createDecoderByteArray() throws IOException {
        final InputStream input = getClass().getResourceAsStream("/test.avif");
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        copyStream(input, bos);
        final byte[] bytes = bos.toByteArray();
        System.out.println();
        final AvifDecoder avifDecoder = AvifDecoder.fromByteArray(bytes);
        while (avifDecoder.nextImage()) {
            final AvifImage image = avifDecoder.getImage();
            final Bitmap bitmap = avifDecoder.getFrame();
            System.out.println();
        }
    }

    @Test
    public void testAvifs() throws IOException {
        final InputStream input = getClass().getResourceAsStream("/test.avifs");
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        copyStream(input, bos);
        final byte[] bytes = bos.toByteArray();
        System.out.println();
        final AvifDecoder avifDecoder = AvifDecoder.fromByteArray(bytes);
        final AvifImage image1 = avifDecoder.getImage();
        final int width = image1.getWidth();
        final int height = image1.getHeight();
        final int imageCount = avifDecoder.getImageCount();
        Bitmap bitmap = avifDecoder.getFrame();
        while (avifDecoder.nextImage()) {
            final AvifImage image = avifDecoder.getImage();
             bitmap = avifDecoder.getFrame();
        }
    }

    public static void copyStream(InputStream is, OutputStream os)
            throws IOException {
        byte[] buff = new byte[4096];
        int rc;
        while ((rc = is.read(buff)) != -1) {
            os.write(buff, 0, rc);
        }
        os.flush();
    }
}
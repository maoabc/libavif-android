package libavif;

public class AvifImage {
    private final long nImage;

    public AvifImage(long nImage) {
        this.nImage = nImage;
    }

    public int getWidth() {
        return getWidth0(nImage);
    }

    public int getHeight() {
        return getHeight0(nImage);
    }

    public int getDepth() {
        return getDepth0(nImage);
    }

    private static native int getWidth0(long nImage);

    private static native int getHeight0(long nImage);

    private static native int getDepth0(long nImage);

    private static native int getYuvFormat0(long nImage);

    private static native int getYuvRange0(long nImage);
}

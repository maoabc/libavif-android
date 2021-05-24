/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package libavif;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class AvifSequenceDrawable extends Drawable implements Animatable {
    private static final String TAG = "AvifSequence";
    public static final boolean DEBUG = true;
    /**
     * These constants are chosen to imitate common browser behavior for WebP/GIF.
     * If other decoders are added, this behavior should be moved into the WebP/GIF decoders.
     * <p>
     * Note that 0 delay is undefined behavior in the GIF standard.
     */
    private static final long MIN_DELAY_MS = 20;
    private static final long DEFAULT_DELAY_MS = 100;

    private final ScheduledThreadPoolExecutor mExecutor = AvifRenderingExecutor.getInstance();


    private static final BitmapProvider sAllocatingBitmapProvider = new BitmapProvider() {
        @Override
        public Bitmap acquireBitmap(int minWidth, int minHeight) {
            return Bitmap.createBitmap(minWidth, minHeight, Bitmap.Config.ARGB_8888);
        }

        @Override
        public void releaseBitmap(Bitmap bitmap) {
        }
    };
    private final Handler mInvalidateHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            invalidateSelf();
        }
    };
    private final int mWidth;
    private final int mHeight;
    private long mNextFrameRenderTime = Long.MIN_VALUE;

    boolean mIsRunning = false;
    private ScheduledFuture<?> mDecodeTaskFuture;

    /**
     * Register a callback to be invoked when a FrameSequenceDrawable finishes looping.
     *
     * @see #setLoopBehavior(int)
     */
    public void setOnFinishedListener(OnFinishedListener onFinishedListener) {
        mOnFinishedListener = onFinishedListener;
    }

    /**
     * Loop a finite number of times, which can be set using setLoopCount. Default to loop once.
     */
    public static final int LOOP_FINITE = 1;

    /**
     * Loop continuously. The OnFinishedListener will never be called.
     */
    public static final int LOOP_INF = 2;

    /**
     * Use loop count stored in source data, or LOOP_ONCE if not present.
     */
    public static final int LOOP_DEFAULT = 3;

    /**
     * Define looping behavior of frame sequence.
     * <p>
     * Must be one of  LOOP_INF, LOOP_DEFAULT, or LOOP_FINITE.
     */
    public void setLoopBehavior(int loopBehavior) {
        mLoopBehavior = loopBehavior;
    }

    /**
     * Set the number of loops in LOOP_FINITE mode. The number must be a postive integer.
     */
    public void setLoopCount(int loopCount) {
        mLoopCount = loopCount;
    }

    private final AvifDecoder mAvifDecoder;

    private final Paint mPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Rect mSrcRect;

    //Protects the fields below
    private final Object mLock = new Object();

    private final BitmapProvider mBitmapProvider;
    private boolean mDestroyed = false;
    private final Bitmap mBitmap;

    private int mCurrentLoop;
    private int mLoopBehavior = LOOP_DEFAULT;
    private int mLoopCount = 1;


    private OnFinishedListener mOnFinishedListener;

    /**
     * Runs on decoding thread, only modifies mBackBitmap's pixels
     */
    private final Runnable mDecodeTask = new Runnable() {
        @Override
        public void run() {
            synchronized (mLock) {
                if (!mIsRunning || mDestroyed) return;

                final long start = SystemClock.uptimeMillis();

                if (!mAvifDecoder.nextImage()) {
                    return;
                }
                boolean exceptionDuringDecode = false;
                long invalidateTimeMs = 0;
                try {
                    invalidateTimeMs = mAvifDecoder.getFrame(mBitmap);
//                    Log.d(TAG, "run: invalidateMs " + invalidateTimeMs + "   " + mAvifDecoder.getImageIndex());
                } catch (Exception e) {
                    // Exception during decode: continue, but delay next frame indefinitely.
                    Log.e(TAG, "exception during decode: " + e);
                    exceptionDuringDecode = true;
                }

                if (invalidateTimeMs < MIN_DELAY_MS) {
                    invalidateTimeMs = DEFAULT_DELAY_MS;
                }
                mNextFrameRenderTime = exceptionDuringDecode ? Long.MIN_VALUE : invalidateTimeMs + start;
                final int imageIndex = mAvifDecoder.getImageIndex();
                if (imageIndex >= mAvifDecoder.getImageCount() - 1) {
                    mCurrentLoop++;
                    if ((mLoopBehavior == LOOP_FINITE && mCurrentLoop == mLoopCount)) {
                        scheduleSelf(mFinishedCallbackRunnable, 0);
                    } else {
                        mAvifDecoder.reset();
                    }
                }
                mInvalidateHandler.sendEmptyMessage(0);

            }
        }
    };

    private final Runnable mFinishedCallbackRunnable = new Runnable() {
        @Override
        public void run() {
            stop();
            if (mOnFinishedListener != null) {
                mOnFinishedListener.onFinished(AvifSequenceDrawable.this);
            }
        }
    };

    private static Bitmap acquireAndValidateBitmap(BitmapProvider bitmapProvider,
                                                   int minWidth, int minHeight) {
        Bitmap bitmap = bitmapProvider.acquireBitmap(minWidth, minHeight);

        if (bitmap.getWidth() < minWidth
                || bitmap.getHeight() < minHeight
                || bitmap.getConfig() != Bitmap.Config.ARGB_8888) {
            throw new IllegalArgumentException("Invalid bitmap provided");
        }

        return bitmap;
    }

    public AvifSequenceDrawable(AvifDecoder avifDecoder) {
        this(avifDecoder, sAllocatingBitmapProvider);
    }

    public AvifSequenceDrawable(AvifDecoder avifDecoder, BitmapProvider bitmapProvider) {
        if (avifDecoder == null || bitmapProvider == null) throw new IllegalArgumentException();

        this.mAvifDecoder = avifDecoder;
        avifDecoder.nextImage();
        final AvifImage image = avifDecoder.getImage();
        mWidth = image.getWidth();
        mHeight = image.getHeight();

        mBitmapProvider = bitmapProvider;
        mBitmap = acquireAndValidateBitmap(bitmapProvider, mWidth, mHeight);
        mSrcRect = new Rect(0, 0, mWidth, mHeight);

        avifDecoder.getFrame(mBitmap);
    }

    private void checkDestroyedLocked() {
        if (mDestroyed) {
            throw new IllegalStateException("Cannot perform operation on recycled drawable");
        }
    }


    private void scheduleNextRender() {
        if (isRunning() && mNextFrameRenderTime != Long.MIN_VALUE) {
            final long renderDelay = Math.max(0, mNextFrameRenderTime - SystemClock.uptimeMillis());
            mNextFrameRenderTime = Long.MIN_VALUE;
            mExecutor.remove(mDecodeTask);
            mDecodeTaskFuture = mExecutor.schedule(mDecodeTask, renderDelay, TimeUnit.MILLISECONDS);
        }
    }

    public boolean isDestroyed() {
        synchronized (mLock) {
            return mDestroyed;
        }
    }

    /**
     * Marks the drawable as permanently recycled (and thus unusable), and releases any owned
     * Bitmaps drawable to its BitmapProvider, if attached.
     * <p>
     * If no BitmapProvider is attached to the drawable, recycle() is called on the Bitmaps.
     */
    public void destroy() {
        if (mBitmapProvider == null) {
            throw new IllegalStateException("BitmapProvider must be non-null");
        }
        synchronized (mLock) {
            checkDestroyedLocked();

            mDestroyed = true;
        }
        mAvifDecoder.destroy();

        mBitmapProvider.releaseBitmap(mBitmap);
    }


    @Override
    public void draw(@NonNull Canvas canvas) {
        synchronized (mLock) {
            canvas.drawBitmap(mBitmap, mSrcRect, getBounds(), mPaint);
        }
    }


    @Override
    public void invalidateSelf() {
        super.invalidateSelf();

        scheduleNextRender();
    }

    @Override
    public void start() {
        synchronized (mLock) {
            if (mIsRunning) {
                return;
            }
            mIsRunning = true;
        }

        checkDestroyedLocked();
        mNextFrameRenderTime = SystemClock.uptimeMillis();
        mCurrentLoop = 0;
        scheduleNextRender();
    }

    @Override
    public void stop() {
        synchronized (mLock) {
            if (!mIsRunning) {
                return;
            }
            mIsRunning = false;
        }
        if (mDecodeTaskFuture != null) {
            mDecodeTaskFuture.cancel(false);
        }

        mCurrentLoop = 0;
        mAvifDecoder.reset();

    }

    @Override
    public boolean isRunning() {
        synchronized (mLock) {
            return mIsRunning && !mDestroyed;
        }
    }


    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        boolean changed = super.setVisible(visible, restart);
//        if (DEBUG) {
//            Log.d(TAG, "setVisible: " + visible + "   " + restart + "   " + changed);
//        }

        if (visible) {
            if (restart) {
                stop();
                start();
            }
            if (changed) {
                start();
            }
        } else if (changed) {
            stop();
        }

        return changed;
    }

    // drawing properties

    @Override
    public void setFilterBitmap(boolean filter) {
        mPaint.setFilterBitmap(filter);
    }

    @Override
    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        mPaint.setColorFilter(colorFilter);
    }

    @Override
    public int getIntrinsicWidth() {
        return mWidth;
    }

    @Override
    public int getIntrinsicHeight() {
        return mHeight;
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    public int getSize() {
        return mWidth * mHeight * 4;
    }

    /**
     * Default executor for rendering tasks - {@link ScheduledThreadPoolExecutor}
     * with 1 worker thread and {@link DiscardPolicy}.
     */
    static final class AvifRenderingExecutor extends ScheduledThreadPoolExecutor {

        private static final AvifRenderingExecutor INSTANCE = new AvifRenderingExecutor();

        static AvifRenderingExecutor getInstance() {
            return INSTANCE;
        }

        private AvifRenderingExecutor() {
            super(1, new DiscardPolicy());
        }
    }

    public interface OnFinishedListener {
        /**
         * Called when a FrameSequenceDrawable has finished looping.
         * <p>
         * Note that this is will not be called if the drawable is explicitly
         * stopped, or marked invisible.
         */
        public void onFinished(AvifSequenceDrawable drawable);
    }

    public interface BitmapProvider {
        /**
         * Called by FrameSequenceDrawable to aquire an 8888 Bitmap with minimum dimensions.
         */
        public Bitmap acquireBitmap(int minWidth, int minHeight);

        /**
         * Called by FrameSequenceDrawable to release a Bitmap it no longer needs. The Bitmap
         * will no longer be used at all by the drawable, so it is safe to reuse elsewhere.
         * <p>
         * This method may be called by FrameSequenceDrawable on any thread.
         */
        public void releaseBitmap(Bitmap bitmap);
    }
}

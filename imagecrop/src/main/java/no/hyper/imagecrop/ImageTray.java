package no.hyper.imagecrop;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

public class ImageTray extends View {

    private Context context;
    private Bitmap picture;
    private Point middle;
    private Paint bitmapPaint;
    private Rect cropRect;
    private int screenWidth;
    private int screenHeight;
    private int cropSize = ImageCropper.DEFAULT_SIZE;
    private float left;
    private float top;

    private ScaleGestureDetector mScaleDetector;
    private GestureDetectorCompat mGestureDetector;
    private float mScaleFactor = 1.f;

    public ImageTray(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.context = context;

        mScaleDetector = new ScaleGestureDetector(context, mScaleGestureListener);
        mGestureDetector = new GestureDetectorCompat(context, mGestureListener);

        bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);

        middle = new Point();
        cropRect = new Rect();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        screenHeight = getMeasuredHeight();
        screenWidth = getMeasuredWidth();
        middle.set(screenWidth / 2, screenHeight / 2);

        int[] dimens = getCropSquareDimens();
        cropRect.set(dimens[0], dimens[1], dimens[2], dimens[3]);

        calculatePicturePos(picture);
    }

    public void setPicture(Bitmap picture) {
        this.picture = picture;
    }

    private void calculatePicturePos(Bitmap picture) {
        mScaleFactor = 1f;
        while(picture.getWidth()*mScaleFactor < cropSize ||
                picture.getHeight()*mScaleFactor < cropSize) {
            mScaleFactor += 0.1;
        }


        left = (middle.x - (picture.getWidth()*mScaleFactor)/2)/mScaleFactor;
        top = (middle.y - (picture.getHeight()*mScaleFactor)/2)/mScaleFactor;
    }

    public void setCropSize(int size) {
        cropSize = size;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        canvas.scale(mScaleFactor, mScaleFactor);
        canvas.drawBitmap(picture, left, top, bitmapPaint);
        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        boolean retVal = mScaleDetector.onTouchEvent(ev);
        retVal = mGestureDetector.onTouchEvent(ev) || retVal;
        return retVal || super.onTouchEvent(ev);
    }

    public Bitmap getCroppedPicture() {
        int x = Math.round(cropRect.left) - Math.round(left);
        int y = Math.round(cropRect.top) - Math.round(top);

        Log.d("CROP", "rect: " + Math.round(cropRect.left) + ", left: " + Math.round(left));

        //** to prevent illegal state exception */
        int croppedSize = Math.round(cropSize / mScaleFactor);
        if (x + croppedSize > picture.getWidth()) {
            x = picture.getWidth() - croppedSize;
        }
        if (y + croppedSize > picture.getHeight()) {
            y = picture.getHeight() - croppedSize;
        }

        System.gc();
        Bitmap newBitmap;
        if (mScaleFactor == 1.0) {
            newBitmap =  Bitmap.createBitmap(picture, (middle.x - cropSize/2) - Math.round(left),
                    (middle.y - cropSize/2) - Math.round(top), croppedSize, croppedSize);
        } else {
            newBitmap = Bitmap.createBitmap(picture, (x > 0) ? x : 0, (y > 0) ? y : 0, croppedSize, croppedSize);
        }

        return Bitmap.createScaledBitmap(newBitmap, Math.round(cropSize), Math.round(cropSize), true);
    }

    private final ScaleGestureDetector.OnScaleGestureListener mScaleGestureListener
            = new ScaleGestureDetector.SimpleOnScaleGestureListener() {

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return super.onScaleBegin(detector);
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scale = detector.getScaleFactor();
            float fx = detector.getFocusX();
            float fy = detector.getFocusY();
            float sw = picture.getWidth()*scale;
            float sh = picture.getHeight()*scale;
            float dx = sw - picture.getWidth();
            float dy = sh - picture.getHeight();

            if(picture.getWidth()*mScaleFactor*scale < cropSize ||
                    picture.getHeight()*mScaleFactor*scale < cropSize) {
                return true;
            }

            mScaleFactor *= scale;
            int[] dimens = getCropSquareDimens();
            cropRect.set(dimens[0], dimens[1], dimens[2], dimens[3]);

            left = left - (fx / (picture.getWidth()*mScaleFactor))*dx;
            top = top - (fy / (picture.getHeight()*mScaleFactor))*dy;

            if(left >= cropRect.left) {
                left = cropRect.left;
            }

            if(top >= cropRect.top) {
                top = cropRect.top;
            }

            if(left + picture.getWidth() <= cropRect.right) {
                left += cropRect.right - (left + picture.getWidth());
            }

            if(top + picture.getHeight() <= cropRect.bottom) {
                top += cropRect.bottom - (top + picture.getHeight());
            }

            invalidate();

            return true;
        }

    };

    private final GestureDetector.SimpleOnGestureListener mGestureListener
            = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if(!mScaleDetector.isInProgress()) {

                float newX = left - distanceX/mScaleFactor;
                float newY = top - distanceY/mScaleFactor;

                float minX = (middle.x + cropSize /2)/mScaleFactor - picture.getWidth();
                float maxX = (middle.x - cropSize /2)/mScaleFactor;
                float minY = (middle.y + cropSize/2)/mScaleFactor - picture.getHeight();
                float maxY = (middle.y - cropSize/2)/mScaleFactor;

                if(newX > minX && newX < maxX) {
                    left = newX;
                }

                if(newY > minY && newY < maxY) {
                    top = newY;
                }

                invalidate();
            }
            return true;
        }
    };

    private int[] getCropSquareDimens() {
        return new int[]{
                Math.round((middle.x - cropSize/2)/mScaleFactor),
                Math.round((middle.y - cropSize/2)/mScaleFactor),
                Math.round((middle.x + cropSize/2)/mScaleFactor),
                Math.round((middle.y + cropSize/2)/mScaleFactor)
        };
    }

}

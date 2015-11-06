package no.hyper.imagecrop;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import no.hyper.imagecrop.R;

/**
 * Created by jean on 07/10/15.
 */
public class ImageCropper extends View {

    private Context context;
    private Bitmap picture;
    private Point middle;
    private Paint bitmapPaint;
    private Rect cropRect;
    private String picturePath;
    private int screenWidth;
    private int screenHeight;
    private int cropSize;
    private float left;
    private float top;

    private ScaleGestureDetector mScaleDetector;
    private GestureDetectorCompat mGestureDetector;
    private float mScaleFactor = 1.f;

    public ImageCropper(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.context = context;

        mScaleDetector = new ScaleGestureDetector(context, mScaleGestureListener);
        mGestureDetector = new GestureDetectorCompat(context, mGestureListener);
        TypedArray array = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.ImageCropper, 0, 0);

        try {
            cropSize = array.getInt(R.styleable.ImageCropper_crop_size, 500);
        } finally {
            array.recycle();
        }

        init();
    }

    private void init() {
        bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);

        screenHeight = Utils.ScreenSize.getHeight(context);
        screenWidth = Utils.ScreenSize.getWidth(context);

        middle = new Point(screenWidth / 2, screenHeight / 2);
        int[] dimens = getCropSquareDimens();
        cropRect = new Rect(dimens[0], dimens[1], dimens[2], dimens[3]);
    }

    public void setPicture(Bitmap picture) {
        mScaleFactor = 1f;
        while(picture.getWidth()*mScaleFactor < cropSize ||
                picture.getHeight()*mScaleFactor < cropSize) {
            mScaleFactor += 0.1;
        }

        this.picture = picture;
        left = (middle.x - (picture.getWidth()*mScaleFactor)/2)/mScaleFactor;
        top = (middle.y - (picture.getHeight()*mScaleFactor)/2)/mScaleFactor;

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

    public Bitmap createSafeBitmap(String picturePath) {
        this.picturePath = picturePath;
        BitmapFactory.Options options = Utils.Image.getImageInfo(picturePath);
        options.inJustDecodeBounds = false;
        options.inSampleSize = calculateInSampleSize(options,
                screenWidth, screenHeight);

        Bitmap bitmap = BitmapFactory.decodeFile(picturePath, options);
        if(bitmap != null) {
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), getRotationMatrix(), true);

            float scaleFactor = (float)screenWidth / (float)bitmap.getWidth();
            int newWidth = screenWidth;
            int newHeight = Math.round(bitmap.getHeight() * scaleFactor);
            bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
            return bitmap;
        } else {
            return null;
        }
    }

    public Bitmap getCroppedPicture() {
        int x = (Math.round(cropRect.left) - Math.round(left));
        int y = Math.round(cropRect.top) - Math.round(top);

        Bitmap newBitmap = Bitmap.createBitmap(picture, (x > 0) ? x : 0, (y > 0) ? y : 0,
                Math.round(cropSize / mScaleFactor), Math.round(cropSize / mScaleFactor));

        return Bitmap.createScaledBitmap(newBitmap, Math.round(cropSize),
                Math.round(cropSize), true);
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
            float scaledWidth = picture.getWidth() * scale;
            float scaledHeight = picture.getHeight() * scale;
            float scaledRightSide = (left * scale) + scaledWidth;
            float scaledBottomSide = (top * scale) + scaledHeight;

            float fx = detector.getFocusX();
            float fy = detector.getFocusY();
            float dx = scaledRightSide - (left + picture.getWidth());
            float dy = scaledBottomSide - (top + picture.getHeight());
            float temp = mScaleFactor*scale;

            if(temp >= 0.5f && temp <= 3.0f) {
                mScaleFactor *= scale;
                mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 3.0f));

                left = left - (fx / (picture.getWidth()*mScaleFactor))*dx;
                top = top - (fy / (picture.getHeight()*mScaleFactor))*dy;

                int[] dimens = getCropSquareDimens();
                cropRect.set(dimens[0], dimens[1], dimens[2], dimens[3]);
                invalidate();
            }

            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            super.onScaleEnd(detector);
            float scaledWidth = picture.getWidth() * mScaleFactor;
            float scaledHeight = picture.getHeight() * mScaleFactor;

            if(scaledWidth < cropSize || scaledHeight < cropSize) {

                while (scaledWidth < cropSize || scaledHeight < cropSize) {
                    mScaleFactor += 0.05f;
                    scaledWidth = picture.getWidth() * mScaleFactor;
                    scaledHeight = picture.getHeight() * mScaleFactor;
                }

                int[] dimens = getCropSquareDimens();
                cropRect.set(dimens[0], dimens[1], dimens[2], dimens[3]);
                left = (middle.x - (picture.getWidth()*mScaleFactor)/2)/mScaleFactor;
                top = (middle.y - (picture.getHeight()*mScaleFactor)/2)/mScaleFactor;
                invalidate();
            } else {
                if(left > cropRect.left) {
                    left = cropRect.left;
                    invalidate();
                }

                if(top > cropRect.top) {
                    top = cropRect.top;
                    invalidate();
                }

                if(left + picture.getWidth() < cropRect.right) {
                    left += cropRect.right - (left + picture.getWidth());
                    invalidate();
                }

                if(top + picture.getHeight() < cropRect.bottom) {
                    top += cropRect.bottom - (top + picture.getHeight());
                    invalidate();
                }
            }
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

                float minX = (middle.x + cropSize /2)/mScaleFactor
                        - picture.getWidth();
                float maxX = (middle.x - cropSize /2)/mScaleFactor;
                float minY = (middle.y + cropSize/2)/mScaleFactor
                        - picture.getHeight();
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

    private int getRotationValue() {
        int rotate = 0;
        try {
            ExifInterface exif = new ExifInterface(picturePath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rotate;
    }

    private Matrix getRotationMatrix() {
        Matrix matrix = new Matrix();
        matrix.postRotate(getRotationValue());
        return matrix;
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth,
                                             int reqHeight) {
        int inSampleSize = 2;
        int rotation = getRotationValue();
        int width = options.outWidth;
        int height = options.outHeight;
        if(rotation == 90 || rotation == 270) {
            width = options.outHeight;
            height = options.outWidth;
        }

        while (width / inSampleSize > reqWidth ||  height / inSampleSize > reqHeight) {
            inSampleSize *= 2;
        }

        return inSampleSize;
    }

}


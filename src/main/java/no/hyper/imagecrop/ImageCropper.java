package no.hyper.imagecrop;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

/**
 * Created by jean on 07/10/15.
 */
public class ImageCropper extends View {

    private Context context;
    private Bitmap picture;
    private Paint picturePaint;
    private Paint rectPaint;
    private float cropWidth;
    private float cropHeight;

    private float left;
    private float top;

    private ScaleGestureDetector mScaleDetector;
    private GestureDetectorCompat mGestureDetector;
    private float mScaleFactor = 1.f;
    private float[] screenCenter = new float[2];

    public ImageCropper(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.context = context;

        mScaleDetector = new ScaleGestureDetector(context, mScaleGestureListener);
        mGestureDetector = new GestureDetectorCompat(context, mGestureListener);
        TypedArray array = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.ImageCropper, 0, 0);

        try {
            cropWidth = (float) array.getInt(R.styleable.ImageCropper_crop_width, 500) / 2;
            cropHeight = (float) array.getInt(R.styleable.ImageCropper_crop_height, 500) / 2;
        } finally {
            array.recycle();
        }

        init();
    }

    private void init() {
        picturePaint = new Paint(Paint.FILTER_BITMAP_FLAG);
        rectPaint = new Paint();
        rectPaint.setColor(Color.WHITE);
        rectPaint.setStyle(Paint.Style.STROKE);

        screenCenter[0] = (float) Utils.ScreenSize.getWidth(getContext()) / 2;
        screenCenter[1] = (float) Utils.ScreenSize.getHeight(getContext()) / 2;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        canvas.scale(mScaleFactor, mScaleFactor);
        canvas.drawBitmap(picture, left, top, picturePaint);
        canvas.drawRect(getCropSquare(), rectPaint);
        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        boolean retVal = mScaleDetector.onTouchEvent(ev);
        retVal = mGestureDetector.onTouchEvent(ev) || retVal;
        return retVal || super.onTouchEvent(ev);
    }

    public void setPicture(Bitmap picture) {
        this.picture = picture;
        left = screenCenter[0] - picture.getWidth() / 2;
        top = screenCenter[1] - picture.getHeight() / 2;
        mScaleFactor = 1f;
        invalidate();
    }

    private final ScaleGestureDetector.OnScaleGestureListener mScaleGestureListener
            = new ScaleGestureDetector.SimpleOnScaleGestureListener() {

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return super.onScaleBegin(detector);
        }

        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
            float tempScale = scaleGestureDetector.getScaleFactor();

            float rightSide = (left * tempScale) + (picture.getWidth() * tempScale);
            float bottomSide = (top * tempScale) + (picture.getHeight() * tempScale);
            Rect cropSquare = getCropSquare();

            float dx = rightSide - (left + picture.getWidth());
            float dy = bottomSide - (top + picture.getHeight());

            if(left < cropSquare.left && rightSide > cropSquare.right
                    && top < cropSquare.top && bottomSide > cropSquare.bottom) {
                mScaleFactor *= tempScale;
                mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 10.0f));
                left -= dx/2;
                top -= dy/2;
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

                float newX = left - distanceX*(1/mScaleFactor);
                float newY = top - distanceY*(1/mScaleFactor);

                float minX = (screenCenter[0] + cropWidth)*(1/mScaleFactor)
                        - picture.getWidth();
                float maxX = (screenCenter[0] - cropWidth)*(1/mScaleFactor);
                float minY = (screenCenter[1] + cropHeight)*(1/mScaleFactor)
                        - picture.getHeight();
                float maxY = (screenCenter[1] - cropHeight)*(1/mScaleFactor);

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

    private Rect getCropSquare() {
        return new Rect(
            Math.round((screenCenter[0] - cropWidth)*(1/mScaleFactor)),
            Math.round((screenCenter[1] - cropHeight)*(1/mScaleFactor)),
            Math.round((screenCenter[0] + cropWidth)*(1/mScaleFactor)),
            Math.round((screenCenter[1] + cropHeight)*(1/mScaleFactor))
        );
    }

    public Bitmap getCroppedPicture() {
        Rect rect = getCropSquare();
        Bitmap scaled = Bitmap.createScaledBitmap(
                picture,
                Math.round(picture.getWidth()*mScaleFactor),
                Math.round(picture.getHeight()*mScaleFactor),
                true
        );

        int x = Math.round(rect.left*mScaleFactor) - Math.round(left*mScaleFactor);
        int y = Math.round(rect.top*mScaleFactor) - Math.round(top*mScaleFactor);

        picture.recycle();
        return Bitmap.createBitmap(scaled, (x > 0) ? x : 0, (y > 0) ? y : 0,
                Math.round(cropWidth * 2), Math.round(cropHeight * 2));
    }
}

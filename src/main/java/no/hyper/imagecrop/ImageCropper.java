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
import android.graphics.Rect;
import android.media.ExifInterface;
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
    private Paint rectMask;
    private Point middle;
    private float cropSize;
    private boolean mask = false;
    private String picturePath;

    private int screenWidth;
    private int screenHeight;

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
            cropSize = (float) array.getInt(R.styleable.ImageCropper_crop_size, 500);
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
        rectMask = new Paint();
        rectMask.setColor(Color.parseColor("#3E464F"));
        rectMask.setStyle(Paint.Style.FILL_AND_STROKE);
        rectMask.setAlpha(200);

        middle = new Point(Utils.ScreenSize.getWidth(getContext()) / 2,
                Utils.ScreenSize.getHeight(getContext()) / 2);
        screenHeight = Utils.ScreenSize.getHeight(context);
        screenWidth = Utils.ScreenSize.getWidth(context);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        canvas.scale(mScaleFactor, mScaleFactor);
        canvas.drawBitmap(picture, left, top, picturePaint);
        if(mask) {
            canvas.drawRect(getCropSquare(), rectMask);
        } else {
            canvas.drawRect(getCropSquare(), rectPaint);
        }
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
        left = middle.x - picture.getWidth() / 2;
        top = middle.y - picture.getHeight() / 2;
        mScaleFactor = 1f;

        while(picture.getWidth()*mScaleFactor < cropSize ||
                picture.getHeight()*mScaleFactor < cropSize) {
            mScaleFactor += 0.1;
        }

        invalidate();
    }

    public Bitmap createSafeBitmap(String picturePath) {
        this.picturePath = picturePath;
        int[] dimens = Utils.Image.getImageInfo(picturePath);
        int rotation = getRotationValue();
        float pictureWidth = (rotation != 0 && rotation != 270) ? dimens[1] : dimens[0];

        float scaleFactor = screenWidth / pictureWidth;
        float newX = dimens[0] * scaleFactor;
        float newY = dimens[1] * scaleFactor;

        Bitmap original = BitmapFactory.decodeFile(picturePath);
        original = Bitmap.createScaledBitmap(original, Math.round(newX), Math.round(newY), true);
        return Bitmap.createBitmap(original, 0, 0, original.getWidth(), original.getHeight(),
                getRotationMatrix(), true);
    }

    public void  setMask(boolean mask) {
        this.mask = mask;
    }

    public Bitmap getCroppedPicture() {
        Rect rect = getCropSquare();

        int x = (Math.round(rect.left) - Math.round(left));
        int y = Math.round(rect.top) - Math.round(top);

        Bitmap newBitmap = Bitmap.createBitmap(picture, (x > 0) ? x : 0, (y > 0) ? y : 0,
                Math.round(cropSize/mScaleFactor), Math.round(cropSize/mScaleFactor));

        return Bitmap.createScaledBitmap(newBitmap, Math.round(cropSize),
                Math.round(cropSize), true);
    }

    public void setBottomPanelSize(int size) {
        middle.y -= size;
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
                    && top < cropSquare.top && bottomSide > cropSquare.bottom
                    && mScaleFactor < 3.0f) {
                mScaleFactor *= tempScale;
                mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 3.0f));
                left -= dx/2;
                top -= dy/2;
                invalidate();
            }

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

                float minX = (middle.x + cropSize /2)*(1/mScaleFactor)
                        - picture.getWidth();
                float maxX = (middle.x - cropSize /2)*(1/mScaleFactor);
                float minY = (middle.y + cropSize/2)*(1/mScaleFactor)
                        - picture.getHeight();
                float maxY = (middle.y - cropSize/2)*(1/mScaleFactor);

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

        if(screenWidth < cropSize || screenHeight < cropSize) {
            cropSize = Math.min((float)screenWidth - 5, (float)screenHeight - 5);
        }

        return new Rect(
            Math.round((middle.x - cropSize/2)*(1/mScaleFactor)),
            Math.round((middle.y - cropSize/2)*(1/mScaleFactor)),
            Math.round((middle.x + cropSize/2)*(1/mScaleFactor)),
            Math.round((middle.y + cropSize/2)*(1/mScaleFactor))
        );
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

    public static float calculateInSampleSize(int width, int height,
                                            int reqWidth, int reqHeight) {
        float inSampleSize = 1;

        while ((height / inSampleSize) > reqHeight
                && (width / inSampleSize) > reqWidth) {
            inSampleSize += 0.1;
        }

        return inSampleSize;
    }

}


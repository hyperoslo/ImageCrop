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
    private Bitmap bitmapOverlay;
    private Point middle;
    private Rect cropRect;

    private Paint bitmapPaint;
    private Paint bitmapOverlayPaint;
    private Paint maskPaint;
    private Paint overlayRectPaint;
    private Paint cropRectPaint;

    private float cropSize;
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
        bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
        bitmapOverlayPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
        bitmapOverlayPaint.setAlpha(170);

        maskPaint = new Paint();
        maskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        overlayRectPaint = new Paint();
        overlayRectPaint.setColor(Color.BLACK);
        overlayRectPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        cropRectPaint = new Paint();
        cropRectPaint.setStyle(Paint.Style.STROKE);
        cropRectPaint.setColor(Color.WHITE);

        middle = new Point(Utils.ScreenSize.getWidth(getContext()) / 2,
                Utils.ScreenSize.getHeight(getContext()) / 2);
        screenHeight = Utils.ScreenSize.getHeight(context);
        screenWidth = Utils.ScreenSize.getWidth(context);

        if(screenWidth < cropSize || screenHeight < cropSize) {
            cropSize = Math.min((float)screenWidth - 5, (float)screenHeight - 5);
        }
        cropRect = new Rect();
        bitmapOverlay = setBitmapOverlay();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        canvas.scale(mScaleFactor, mScaleFactor);
        canvas.drawBitmap(picture, left, top, bitmapPaint);

        canvas.drawBitmap(bitmapOverlay, 0, 0, bitmapOverlayPaint);

        canvas.drawRect(cropRect.left, cropRect.top, cropRect.right, cropRect.bottom, cropRectPaint);

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

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = calculateInSampleSize(dimens[0], dimens[1],
                screenWidth, screenHeight);
        Bitmap original = BitmapFactory.decodeFile(picturePath, options);
        original = Bitmap.createScaledBitmap(original, Math.round(newX), Math.round(newY), true);
        return Bitmap.createBitmap(original, 0, 0, original.getWidth(), original.getHeight(),
                getRotationMatrix(), true);
    }

    public Bitmap getCroppedPicture() {
        int x = (Math.round(cropRect.left) - Math.round(left));
        int y = Math.round(cropRect.top) - Math.round(top);

        Bitmap newBitmap = Bitmap.createBitmap(picture, (x > 0) ? x : 0, (y > 0) ? y : 0,
                Math.round(cropSize/mScaleFactor), Math.round(cropSize/mScaleFactor));

        return Bitmap.createScaledBitmap(newBitmap, Math.round(cropSize),
                Math.round(cropSize), true);
    }

    public void setBottomPanelSize(int size) {
        middle.y -= size;
        int[] dimens = getCropSquareDimens();
        cropRect.set(dimens[0], dimens[1], dimens[2], dimens[3]);
        bitmapOverlay = setBitmapOverlay();
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

            float fx = scaleGestureDetector.getFocusX();
            float fy = scaleGestureDetector.getFocusY();
            float dx = rightSide - (left + picture.getWidth());
            float dy = bottomSide - (top + picture.getHeight());

            if(left < cropRect.left && rightSide > cropRect.right
                    && top < cropRect.top && bottomSide > cropRect.bottom
                    && mScaleFactor <= 3.0f) {

                mScaleFactor *= tempScale;
                mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 3.0f));

                left = left - (fx / (picture.getWidth()*mScaleFactor))*dx;
                top = top - (fy / (picture.getHeight()*mScaleFactor))*dy;

                int[] dimens = getCropSquareDimens();
                cropRect.set(dimens[0], dimens[1], dimens[2], dimens[3]);
                bitmapOverlay = setBitmapOverlay();

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

    private int[] getCropSquareDimens() {
        return new int[]{
                Math.round((middle.x - cropSize/2)/mScaleFactor),
                Math.round((middle.y - cropSize/2)/mScaleFactor),
                Math.round((middle.x + cropSize/2)/mScaleFactor),
                Math.round((middle.y + cropSize/2)/mScaleFactor)
        };
    }

    private Bitmap setBitmapOverlay() {
        Bitmap bitmap = Bitmap.createBitmap(
                Math.round(screenWidth/mScaleFactor),
                Math.round(screenHeight/mScaleFactor), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawRect(0, 0,
                Math.round(screenWidth/mScaleFactor), Math.round(screenHeight/mScaleFactor),
                overlayRectPaint);
        canvas.drawRect(cropRect.left, cropRect.top, cropRect.right, cropRect.bottom, maskPaint);
        return bitmap;
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

    public static int calculateInSampleSize(int width, int height,
                                            int reqWidth, int reqHeight) {
        int inSampleSize = 1;

        while ((height / inSampleSize) > reqHeight
                && (width / inSampleSize) > reqWidth) {
            inSampleSize += 1;
        }

        return inSampleSize;
    }

}


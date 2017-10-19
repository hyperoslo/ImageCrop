package no.hyper.imagecrop;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by jean on 05/11/15.
 */
class Overlay extends View {

    private Context context;
    private int cropSize = ImageCropper.DEFAULT_SIZE;
    private int screenHeight;
    private int screenWidth;
    private Point middle;
    private Rect cropRect;

    private Paint bitmapOverlayPaint;
    private Paint maskPaint;
    private Paint overlayRectPaint;
    private Paint cropRectPaint;

    private Bitmap overlay;

    public Overlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    private void init() {
        bitmapOverlayPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
        bitmapOverlayPaint.setAlpha(180);

        maskPaint = new Paint();
        maskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        overlayRectPaint = new Paint();
        overlayRectPaint.setColor(Color.BLACK);
        overlayRectPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        cropRectPaint = new Paint();
        cropRectPaint.setStyle(Paint.Style.STROKE);
        cropRectPaint.setColor(Color.WHITE);
        middle = new Point();
    }

    private void createOverlay() {
        while(screenWidth < cropSize || screenHeight < cropSize) {
            cropSize -= 50;
        }

        cropRect = new Rect(middle.x - cropSize/2, middle.y - cropSize/2,
                middle.x + cropSize/2, middle.y + cropSize/2);

        overlay = Bitmap.createBitmap(screenWidth + 5,
                screenHeight + 5, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(overlay);
        canvas.drawRect(0, 0, screenWidth + 5, screenHeight + 5, overlayRectPaint);
        canvas.drawCircle(
                (cropRect.left + cropRect.right) / 2,
                (cropRect.top + cropRect.bottom) / 2,
                (cropRect.right - cropRect.left) / 2,
                maskPaint
        );
    }

    public void setCropSize(int size) {
        cropSize = size;
        createOverlay();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        screenHeight = getMeasuredHeight();
        screenWidth = getMeasuredWidth();
        middle.set(screenWidth / 2, screenHeight / 2);

        createOverlay();
    }



    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        canvas.drawBitmap(overlay, 0, 0, bitmapOverlayPaint);
        canvas.drawCircle(
                (cropRect.left + cropRect.right) / 2,
                (cropRect.top + cropRect.bottom) / 2,
                (cropRect.right - cropRect.left) / 2,
                cropRectPaint
        );
        canvas.restore();
    }
}

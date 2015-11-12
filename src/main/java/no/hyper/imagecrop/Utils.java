package no.hyper.imagecrop;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.media.ExifInterface;
import android.view.Display;
import android.view.WindowManager;


/**
 * Created by chang@hyper.no on 5/26/2015.
 */
class Utils {

    public static Bitmap createSafeBitmap(Context context, String picturePath) {
        Point size = Utils.getSize(context);
        int screenWidth = size.x;
        int screenHeight = size.y;

        BitmapFactory.Options options = getOptions(picturePath, screenWidth, screenHeight);
        Bitmap bitmap = BitmapFactory.decodeFile(picturePath, options);
        if(bitmap != null) {
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                    bitmap.getHeight(), getRotationMatrix(picturePath), true);
            float scaleFactor = (float)screenWidth / (float)bitmap.getWidth();
            int newWidth = screenWidth;
            int newHeight = Math.round(bitmap.getHeight() * scaleFactor);
            bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
            return bitmap;
        } else {
            return null;
        }
    }

    public static Point getSize(Context context) {
        WindowManager wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    private static BitmapFactory.Options getOptions(
            String picturePath, int screenWidth, int screenHeight) {
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(picturePath, bmOptions);
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = calculateInSampleSize(picturePath, bmOptions.outWidth,
                bmOptions.outHeight, screenWidth, screenHeight);

        return bmOptions;
    }

    private static Matrix getRotationMatrix(String picturePath) {
        Matrix matrix = new Matrix();
        matrix.postRotate(getRotationValue(picturePath));
        return matrix;
    }

    private static int getRotationValue(String picturePath) {
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

    private static int calculateInSampleSize(String picturePath, int width, int height,
                                             int reqWidth, int reqHeight) {
        int inSampleSize = 2;
        int rotation = getRotationValue(picturePath);
        if(rotation == 90 || rotation == 270) {
            int temp = width;
            width = height;
            height = temp;
        }

        while (width / inSampleSize > reqWidth ||  height / inSampleSize > reqHeight) {
            inSampleSize *= 2;
        }

        return inSampleSize;
    }
}

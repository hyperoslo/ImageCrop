package no.hyper.imagecrop;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.view.Display;
import android.view.WindowManager;


/**
 * Created by chang@hyper.no on 5/26/2015.
 */
public class Utils {
    public static class ScreenSize {

        public static int getWidth(Context ctx) {
            WindowManager wm = (WindowManager)ctx.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);

            return size.x;
        }

        public static int getHeight(Context ctx) {
            WindowManager wm = (WindowManager)ctx.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);

            return size.y;
        }
    }

    public static class Image {

        public static BitmapFactory.Options getImageInfo(String path) {
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, bmOptions);

            return bmOptions;
        }
    }
}

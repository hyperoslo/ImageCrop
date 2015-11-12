package no.hyper.imagecrop;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * Created by jean on 11/11/15.
 */
public class ImageCropper extends FrameLayout {

    private Context context;
    private int cropSize;
    private ImageTray imageTray;

    public ImageCropper(Context context) {
        super(context);
    }

    public ImageCropper(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.context = context;
        TypedArray array = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.ImageCropper, 0, 0);

        try {
            cropSize = array.getInt(R.styleable.ImageCropper_crop_size, 500);
        } finally {
            array.recycle();
        }

        inflate(context, R.layout.crop_layout, this);

        Overlay overlay = (Overlay) findViewById(R.id.overlay);
        overlay.setCropSize(cropSize);

        imageTray = (ImageTray) findViewById(R.id.image_tray);
        imageTray.setCropSize(cropSize);
    }

    public boolean setPicture(String path) {
        Bitmap bitmap = Utils.createSafeBitmap(context, path);
        if(bitmap != null) {
            imageTray.setPicture(bitmap);
            return true;
        } else {
            return false;
        }
    }

    public Bitmap getCroppedPicture() {
        return imageTray.getCroppedPicture();
    }

}

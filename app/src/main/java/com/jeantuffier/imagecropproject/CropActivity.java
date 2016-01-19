package com.jeantuffier.imagecropproject;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import no.hyper.imagecrop.ImageCropper;


public class CropActivity extends AppCompatActivity {
    public  final static String IMAGE_PATH = "image_path";

    private String imagePath;
    private ImageCropper imageCropper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop);

        imagePath = getIntent().getStringExtra(IMAGE_PATH);
        final ImageView imageView = (ImageView) findViewById(R.id.image_view);

        Button cancel = (Button) findViewById(R.id.cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        final Button finish = (Button) findViewById(R.id.finish);
        finish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish.setEnabled(false);
                finish.setTextColor(Color.DKGRAY);
                Bitmap cropped = imageCropper.getCroppedPicture();
                imageCropper.setVisibility(View.GONE);
                imageView.setImageBitmap(cropped);
                imageView.setVisibility(View.VISIBLE);
            }
        });

        imageCropper = (ImageCropper) findViewById(R.id.image_cropper);
        boolean isSet = imageCropper.setPicture(imagePath);
        if(!isSet) {
            Toast.makeText(getApplicationContext(), "bitmap == null", Toast.LENGTH_LONG).show();
            finish();
        }
    }

}

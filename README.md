# ImageCrop
android library providing ui element for zooming, panning and cropping pictures.

# Gradle
`compile 'no.hyper.imagecrop:imagecrop:0.1'`

# Usage
Declare in XML

```xml
<FrameLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:custom="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#000000">

  <no.hyper.imagecrop.ImageCropper
          android:id="@+id/image_cropper"
          android:layout_width="match_parent"
          android:layout_height="match_parent"
          custom:crop_size="200"/>
  
  <no.hyper.imagecrop.Overlay
      android:layout_width="match_parent"
      android:layout_height="match_parent"
      custom:crop_size="200"/>
      
</FrameLayout>
```
`crop_size` define in pixel the size of the square use to crop pictures.

To set the picture into the UI element:
```Java
ImageCropper imageCropper = (ImageCropper) findViewById(R.id.image_cropper);
Bitmap bitmap = imageCropper.createSafeBitmap(imagePath);
if(bitmap != null) {
    imageCropper.setPicture(bitmap);
} else {
    Toast.makeText(getApplicationContext(), "bitmap == null", Toast.LENGTH_LONG).show();
    finish();
}
```
`createSafeBitmap(String path)` allows you to get a scaled down bitmap fitting the screen size and managing OutOfMemory exceptions.  

To get the cropped picture:
```Java
Bitmap cropped = imageCropper.getCroppedPicture();
```

# Liscense
```
The MIT License (MIT)

Copyright (c) 2015 Hyper Interaktiv



Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:



The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.



THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
```

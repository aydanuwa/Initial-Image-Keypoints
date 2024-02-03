package com.example.initialImageKeypoints;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.ViewGroup;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    public int maxRes = 1360; // We scale our image to the resolution of maximum 1000 pixels
    public double [][] greyC = new double[maxRes][maxRes]; // Current grey image
    public double [][] maskS0 = new double[5][5]; // Mask with the Gaussian blur function's values
    public double [][] maskS1 = new double[5][5]; // Mask with the Gaussian blur function's values
    public double [][] maskS2 = new double[7][7]; // Mask with the Gaussian blur function's values
    public double [][] maskS3 = new double[9][9]; // Mask with the Gaussian blur function's values
    public double [][] maskS4 = new double[11][11]; // Mask with the Gaussian blur function's values
    public double [][] maskS = new double[137][137]; // Mask with the Gaussian blur function's values
    public double [][] octave1000First = new double[maxRes][maxRes]; // Here, the figure will have the white border
    public double [][] octave1000Second = new double[maxRes][maxRes]; // Here, the figure will have the white border
    public double [][] octave1000Third = new double[maxRes][maxRes]; // Here, the figure will have the white border
    public double [][] octave1000Fourth = new double[maxRes][maxRes]; // Here, the figure will have the white border
    public double [][] octave1000Fifth = new double[maxRes][maxRes]; // Here, the figure will have the white border
    public double [][] DoG1000First = new double[maxRes][maxRes];
    public double [][] DoG1000Second = new double[maxRes][maxRes];
    public double [][] DoG1000Third = new double[maxRes][maxRes];
    public double [][] DoG1000Fourth = new double[maxRes][maxRes];
    public double [][] Hessian = new double [2][2]; // 2x2 Hessian matrix
    public int [][] keypoints1000 = new int [3000][2]; // Info about keypoints
    public int radius0, radius1, radius2, radius3, radius4, MatrixBorder, flagMax, flagMin, nk, maxNoKeyPoints=100; // maxNoKeyPoints - maximum number of keypoints
    public double maxFirst, maxSecond, minThird, maxThird, minFourth, maxFourth;
    public double minFirst, minSecond, sigma0, sigma1, sigma2, sigma3, sigma4, max, min, trace, det, threshold = 7.65; // 7.65 = 255 * 0.03;
    public double [] xk = new double [83]; // Coordinates of keypoints' net: 25 keypoints (1st level) + 58 keypoints (2nd level; 4 points, border, is included on the 1st level)
    public double [] yk = new double [83]; // Coordinates of keypoints' net
    public double [] IC = new double [83]; // Average intensities of in the circles around keypoints in the descriptor
    public int [][] ICdif = new int [83][83]; // Array with number of the point(s); differences are in the following array
    public double [][] ICdifDouble = new double [83][83]; // Array with number of the point(s); differences are in the following array

    public int x, y, i, j, width, height, pixel, k, i1, i2;
    public String fileSeparator = System.getProperty("file.separator");
    File file;
    Bitmap bmOut, bmOut1, bmOut2, bmOut3, bmOut4;
    OutputStream out;

    private static final String TAG = MainActivity.class.getSimpleName();
    private int REQUEST_CODE_PERMISSIONS = 101;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.READ_EXTERNAL_STORAGE"};
    TextureView textureView;
    MediaPlayer mp=new MediaPlayer(); // MediaPlayer

    ThreadOctave0 t0 = new ThreadOctave0();
    ThreadOctave1 t1 = new ThreadOctave1();
    ThreadOctave2 t2 = new ThreadOctave2();
    ThreadOctave3 t3 = new ThreadOctave3();
    DoGFirst t5 = new DoGFirst();
    DoGSecond t6 = new DoGSecond();
    DoGThird t7 = new DoGThird();
    SIFTkeypointsSecond t8 = new SIFTkeypointsSecond();
    public int [][] keypoints1000Second = new int [1500][2]; // Info about keypoints: Second, i.e. thread, part
    public int nkSecond;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textureView = findViewById(R.id.view_finder);

//        MediaPlayer mp=new MediaPlayer(); // We play mp3 here
        try {
            mp.setDataSource(getApplicationContext().getExternalFilesDir(null).getAbsolutePath() + fileSeparator + "DeviceIsStarting.mp3");//Write your location here
            mp.prepare();
            mp.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        maxRes = 180;
        sigma0 = 0.707107;
        radius0 = 2; // radius0 is the radius of the matrix for the Gaussian blur for the current scale
        sigma1 = 1;
        radius1 = 3; // radius1 is the radius of the matrix for the Gaussian blur for the current scale
        sigma2 = 1.414214;
        radius2 = 5; // radius2 is the radius of the matrix for the Gaussian blur for the current scale
        sigma3 = 2;
        radius3 = 6; // radius3 is the radius of the matrix for the Gaussian blur for the current scale
        sigma4 = 2.828427;
        radius4 = 9; // radius4 is the radius of the matrix for the Gaussian blur for the maximum scale
        startFREAK();

        maxRes = 340;
        sigma0 = 1.414214;
        radius0 = 5; // radius0 is the radius of the matrix for the Gaussian blur for the current scale
        sigma1 = 2;
        radius1 = 6; // radius1 is the radius of the matrix for the Gaussian blur for the current scale
        sigma2 = 2.828427;
        radius2 = 9; // radius2 is the radius of the matrix for the Gaussian blur for the current scale
        sigma3 = 4;
        radius3 = 12; // radius3 is the radius of the matrix for the Gaussian blur for the current scale
        sigma4 = 5.656854;
        radius4 = 17; // radius4 is the radius of the matrix for the Gaussian blur for the maximum scale
        startFREAK();

        maxRes = 680;
        sigma0 = 2.828427;
        radius0 = 9; // radius0 is the radius of the matrix for the Gaussian blur for the current scale
        sigma1 = 4;
        radius1 = 12; // radius1 is the radius of the matrix for the Gaussian blur for the current scale
        sigma2 = 5.656854;
        radius2 = 17; // radius2 is the radius of the matrix for the Gaussian blur for the current scale
        sigma3 = 8;
        radius3 = 24; // radius3 is the radius of the matrix for the Gaussian blur for the current scale
        sigma4 = 11.313708;
        radius4 = 34; // radius4 is the radius of the matrix for the Gaussian blur for the maximum scale
        startFREAK();

        maxRes = 1360;
        sigma0 = 5.636854;
        radius0 = 17; // radius0 is the radius of the matrix for the Gaussian blur for the current scale
        sigma1 = 8;
        radius1 = 24; // radius1 is the radius of the matrix for the Gaussian blur for the current scale
        sigma2 = 11.313708;
        radius2 = 34; // radius2 is the radius of the matrix for the Gaussian blur for the current scale
        sigma3 = 16;
        radius3 = 48; // radius3 is the radius of the matrix for the Gaussian blur for the current scale
        sigma4 = 22.627417;
        radius4 = 68; // radius4 is the radius of the matrix for the Gaussian blur for the maximum scale
        startFREAK();
    }

    private void startCamera() {

        CameraX.unbindAll();

        Rational aspectRatio = new Rational (textureView.getWidth(), textureView.getHeight()); // This is original line
        Size screen = new Size(textureView.getWidth(), textureView.getHeight()); //size of the screen

        PreviewConfig pConfig = new PreviewConfig.Builder()
                .setTargetAspectRatio(aspectRatio)
                .setTargetResolution(screen)
                //.setLensFacing(CameraX.LensFacing.FRONT)
                .build();
        Preview preview = new Preview(pConfig);

        preview.setOnPreviewOutputUpdateListener(
                new Preview.OnPreviewOutputUpdateListener() {
                    @Override
                    public void onUpdated(Preview.PreviewOutput output){
                        ViewGroup parent = (ViewGroup) textureView.getParent();
                        parent.removeView(textureView);
                        parent.addView(textureView, 0);

                        textureView.setSurfaceTexture(output.getSurfaceTexture());
                        updateTransform();
                    }
                });


        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder().setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
                .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).build();
        final ImageCapture imgCap = new ImageCapture(imageCaptureConfig);

        findViewById(R.id.imgCapture).setOnClickListener(v -> {
            imgCap.takePicture(new ImageCapture.OnImageCapturedListener() {
                @Override
                public void onCaptureSuccess(ImageProxy image, int rotationDegrees) {
                    try {

                   } catch (Exception e) {
                        Log.i(TAG, "Exception  " + e);
                    }
                }

                @Override
                public void onError(ImageCapture.UseCaseError error, String message, @Nullable Throwable cause) {
                    Log.i(TAG, "We have NOT got bitmap :(");
//                    findViewById(R.id.imgCapture).performClick(); // Call this view's OnClickListener, if it is defined. Performs all normal actions associated with clicking: reporting accessibility event, playing a sound, etc.
                }
            });
        });

        //bind to lifecycle:
        CameraX.bindToLifecycle((LifecycleOwner)this, preview, imgCap);
    }

    private void updateTransform(){
        Matrix mx = new Matrix();
        float w = textureView.getMeasuredWidth();
        float h = textureView.getMeasuredHeight();

        float cX = w / 2f;
        float cY = h / 2f;

        int rotationDgr;
        int rotation = (int)textureView.getRotation();

        switch(rotation){
            case Surface.ROTATION_0:
                rotationDgr = 0;
                break;
            case Surface.ROTATION_90:
                rotationDgr = 90;
                break;
            case Surface.ROTATION_180:
                rotationDgr = 180;
                break;
            case Surface.ROTATION_270:
                rotationDgr = 270;
                break;
            default:
                return;
        }

        mx.postRotate((float)rotationDgr, cX, cY);
        textureView.setTransform(mx);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if(requestCode == REQUEST_CODE_PERMISSIONS){
            if(allPermissionsGranted()){
                startCamera();
            } else{
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private boolean allPermissionsGranted(){

        for(String permission : REQUIRED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }

    public void startFREAK(){
        Log.i(TAG, "Time at the beginning = " + Calendar.getInstance().getTime());

        try {
            nk=0; // Number of keypoints equals 0 initially
            // Now, we open current image to find the pattern
            file = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+fileSeparator+"MyFaces"+fileSeparator+"OInput.jpg");
            bmOut = BitmapFactory.decodeFile(file.getPath());

            Log.i(TAG, "We have bitmap :)");
            // Now, we save the bitmap onto disk to see what we have from camera :)
            width = bmOut.getWidth(); height = bmOut.getHeight();
            Log.i(TAG, "Width =  " + width + "   Height = " + height);
            // Now, we scale an image to the maxRes :)
            if (height<width) { // Here, we make the smallest size equals maxRes=271+4 pixels
                height=Math.round(maxRes * height / width); width=maxRes;
            } else {
                width=Math.round(maxRes * width / height); height=maxRes;
            }
            Log.i(TAG, "New width =  " + width + "   New height = " + height);
            bmOut = Bitmap.createScaledBitmap(bmOut, width, height, true); // Here, we scale bitmap to maxRes pixels; true means that we use bilinear filtering for better image
            for (x = 0; x < width; x++)
                for (y = 0; y < height; y++) {
                    // get pixel color
                    pixel = bmOut.getPixel(x, y);
                    greyC[x][y] = 0.21 * Color.red(pixel) + 0.72 * Color.green(pixel) + 0.07 * Color.blue(pixel); // We're more sensitive to green than other colors, so green is weighted most heavily
                }
            // create output bitmap
            bmOut1 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888); // Here, we use 4 bytes to store ARGB info for every pixel
            bmOut2 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bmOut3 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bmOut4 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Log.i(TAG, "bmOut was created :)");
//-----------------------------------------------------------------------------------------------------------------------------------
            // We start calculation of scales for the first octave
            for (x=-radius0;x<=radius0;x++)
                for (y=-radius0;y<=radius0;y++) {
                    maskS[x + radius0][y + radius0] = Math.exp(-(x * x + y * y) / (2.0 * sigma0 * sigma0)) / (2.0 * Math.PI * sigma0 * sigma0);
                    //Log.i(TAG, "Gaussian function for bluring [" +x + "," + y + "]  " + maskS[x + radius0][y + radius0]);
                }
            for (i=radius4;i<width-radius4;i++) { // we use radius4 to boost the performance
                for (j = radius4; j < height - radius4; j++) {
                    octave1000First[i][j] = 0;
                    for (x = -radius0; x <= radius0; x++)
                        for (y = -radius0; y <= radius0; y++)
                            octave1000First[i][j] = octave1000First[i][j] + maskS[x + radius0][y + radius0] * greyC[i + x][j + y];
                    x=(int)Math.round(octave1000First[i][j]);
                    bmOut1.setPixel(i, j, Color.argb(255, x, x, x));
                }
            }
            file = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+fileSeparator+"Temp"+fileSeparator+"Octave"+maxRes+"First.jpg");
            out = new FileOutputStream(file);
            bmOut1.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush(); out.close();

            for (x=-radius1;x<=radius1;x++)
                for (y=-radius1;y<=radius1;y++) {
                    maskS[x + radius1][y + radius1] = Math.exp(-(x * x + y * y) / (2.0 * sigma1 * sigma1)) / (2.0 * Math.PI * sigma1 * sigma1);
                    //Log.i(TAG, "Gaussian function for bluring [" +x + "," + y + "]  " + maskS[x + radius0][y + radius0]);
                }
            for (i=radius4;i<width-radius4;i++) { // we use radius4 to boost the performance
                for (j = radius4; j < height - radius4; j++) {
                    octave1000Second[i][j] = 0;
                    for (x = -radius1; x <= radius1; x++)
                        for (y = -radius1; y <= radius1; y++)
                            octave1000Second[i][j] = octave1000Second[i][j] + maskS[x + radius1][y + radius1] * greyC[i + x][j + y];
                    x=(int)Math.round(octave1000Second[i][j]);
                    bmOut1.setPixel(i, j, Color.argb(255, x, x, x));
                }
            }
            file = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+fileSeparator+"Temp"+fileSeparator+"Octave"+maxRes+"Second.jpg");
            out = new FileOutputStream(file);
            bmOut1.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush(); out.close();

            for (x=-radius2;x<=radius2;x++)
                for (y=-radius2;y<=radius2;y++) {
                    maskS[x + radius2][y + radius2] = Math.exp(-(x * x + y * y) / (2.0 * sigma2 * sigma2)) / (2.0 * Math.PI * sigma2 * sigma2);
                    //Log.i(TAG, "Gaussian function for bluring [" +x + "," + y + "]  " + maskS[x + radius0][y + radius0]);
                }
            for (i=radius4;i<width-radius4;i++) { // we use radius4 to boost the performance
                for (j = radius4; j < height - radius4; j++) {
                    octave1000Third[i][j] = 0;
                    for (x = -radius2; x <= radius2; x++)
                        for (y = -radius2; y <= radius2; y++)
                            octave1000Third[i][j] = octave1000Third[i][j] + maskS[x + radius2][y + radius2] * greyC[i + x][j + y];
                    x=(int)Math.round(octave1000Third[i][j]);
                    bmOut1.setPixel(i, j, Color.argb(255, x, x, x));
                }
            }
            file = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+fileSeparator+"Temp"+fileSeparator+"Octave"+maxRes+"Third.jpg");
            out = new FileOutputStream(file);
            bmOut1.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush(); out.close();

            for (x=-radius3;x<=radius3;x++)
                for (y=-radius3;y<=radius3;y++) {
                    maskS[x + radius3][y + radius3] = Math.exp(-(x * x + y * y) / (2.0 * sigma3 * sigma3)) / (2.0 * Math.PI * sigma3 * sigma3);
                    //Log.i(TAG, "Gaussian function for bluring [" +x + "," + y + "]  " + maskS[x + radius0][y + radius0]);
                }
            for (i=radius4;i<width-radius4;i++) { // we use radius4 to boost the performance
                for (j = radius4; j < height - radius4; j++) {
                    octave1000Fourth[i][j] = 0;
                    for (x = -radius3; x <= radius3; x++)
                        for (y = -radius3; y <= radius3; y++)
                            octave1000Fourth[i][j] = octave1000Fourth[i][j] + maskS[x + radius3][y + radius3] * greyC[i + x][j + y];
                    x=(int)Math.round(octave1000Fourth[i][j]);
                    bmOut1.setPixel(i, j, Color.argb(255, x, x, x));
                }
            }
            file = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+fileSeparator+"Temp"+fileSeparator+"Octave"+maxRes+"Fourth.jpg");
            out = new FileOutputStream(file);
            bmOut1.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush(); out.close();

            for (x=-radius4;x<=radius4;x++)
                for (y=-radius4;y<=radius4;y++) {
                    maskS[x + radius4][y + radius4] = Math.exp(-(x * x + y * y) / (2.0 * sigma4 * sigma4)) / (2.0 * Math.PI * sigma4 * sigma4);
                    //Log.i(TAG, "Gaussian function for bluring [" +x + "," + y + "]  " + maskS[x + radius0][y + radius0]);
                }
            for (i=radius4;i<width-radius4;i++) { // we use radius4 to boost the performance
                for (j = radius4; j < height - radius4; j++) {
                    octave1000Fifth[i][j] = 0;
                    for (x = -radius4; x <= radius4; x++)
                        for (y = -radius4; y <= radius4; y++)
                            octave1000Fifth[i][j] = octave1000Fifth[i][j] + maskS[x + radius4][y + radius4] * greyC[i + x][j + y];
                    x=(int)Math.round(octave1000Fifth[i][j]);
                    bmOut1.setPixel(i, j, Color.argb(255, x, x, x));
                }
            }
            file = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+fileSeparator+"Temp"+fileSeparator+"Octave"+maxRes+"Fifth.jpg");
            out = new FileOutputStream(file);
            bmOut1.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush(); out.close();
            // We finished calculation of scales for the first octave
//-----------------------------------------------------------------------------------------------------------------------------------
            // We start calculation of DoG
            MatrixBorder=radius4; // The maximum border equals maximum radius, i.e. it is 68 for the 512x512 pixel picture
//
            minFirst=1000; maxFirst=-1000; minSecond=1000; maxSecond=-1000; minThird=1000; maxThird=-1000; minFourth=1000; maxFourth=-1000;
            for (i=MatrixBorder;i<width-MatrixBorder;i++)
                for (j=MatrixBorder;j<height-MatrixBorder;j++) {
                    DoG1000First[i][j]= octave1000First[i][j] - octave1000Second[i][j];
                    if (DoG1000First[i][j]>maxFirst) maxFirst=DoG1000First[i][j];
                    else if (DoG1000First[i][j]<minFirst) minFirst=DoG1000First[i][j];
                    x=(int)Math.round(DoG1000First[i][j]);
                    bmOut1.setPixel(i, j, Color.argb(255, x, x, x));
                    DoG1000Second[i][j]= octave1000Second[i][j] - octave1000Third[i][j];
                    if (DoG1000Second[i][j]>maxSecond) maxSecond=DoG1000Second[i][j];
                    else if (DoG1000Second[i][j]<minSecond) minSecond=DoG1000Second[i][j];
                    x=(int)Math.round(DoG1000Second[i][j]);
                    bmOut2.setPixel(i, j, Color.argb(255, x, x, x));
                    DoG1000Third[i][j]= octave1000Third[i][j] - octave1000Fourth[i][j];
                    if (DoG1000Third[i][j]>maxThird) maxThird=DoG1000Third[i][j];
                    else if (DoG1000Third[i][j]<minThird) minThird=DoG1000Third[i][j];
                    x=(int)Math.round(DoG1000Third[i][j]);
                    bmOut3.setPixel(i, j, Color.argb(255, x, x, x));
                    DoG1000Fourth[i][j]= octave1000Fourth[i][j] - octave1000Fifth[i][j];
                    if (DoG1000Fourth[i][j]>maxFourth) maxFourth=DoG1000Fourth[i][j];
                    else if (DoG1000Fourth[i][j]<minFourth) minFourth=DoG1000Fourth[i][j];
                    x=(int)Math.round(DoG1000Fourth[i][j]);
                    bmOut4.setPixel(i, j, Color.argb(255, x, x, x));
                }

            file = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+fileSeparator+"Temp"+fileSeparator+"DoG1000First.jpg");
            out = new FileOutputStream(file);
            bmOut1.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush(); out.close();
            file = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+fileSeparator+"Temp"+fileSeparator+"DoG1000Second.jpg");
            out = new FileOutputStream(file);
            bmOut2.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush(); out.close();
            file = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+fileSeparator+"Temp"+fileSeparator+"DoG1000Third.jpg");
            out = new FileOutputStream(file);
            bmOut3.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush(); out.close();
            file = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+fileSeparator+"Temp"+fileSeparator+"DoG1000Fourth.jpg");
            out = new FileOutputStream(file);
            bmOut4.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush(); out.close();
//-----------------------------------------------------------------------------------------------------------------------------------
            // We look for SIFT keypoints
            // The following loop is for the SECOND DoG
            for (i=MatrixBorder+1;i<width-MatrixBorder-1;i++)
                for (j=MatrixBorder+1;j<height-MatrixBorder-1;j++) {
                    if (Math.abs(DoG1000Second[i][j]) >= threshold) {// We exclude extrema of low contrast: |D(X)| must be gt 0.03, here pixels are between 0 and 1
                        // We check for maximum/minimum
                        flagMax = 1; flagMin = 1; max = -1000; min = 1000;
                        for (x = -1; x <= 1; x++)
                            for (y = -1; y <= 1; y++) {
                                if (DoG1000Second[i][j] <= DoG1000First[i + x][j + y]) flagMax = 0;
                                if (DoG1000Second[i][j] >= DoG1000First[i + x][j + y]) flagMin = 0;
                                if (DoG1000First[i + x][j + y] > max) max = DoG1000First[i + x][j + y];
                                else if (DoG1000First[i + x][j + y] < min) min = DoG1000First[i + x][j + y];
                            }
                        if ((flagMax == 1 && DoG1000Second[i][j] > max) || (flagMin == 1 && DoG1000Second[i][j] < min)) {
                            for (x = -1; x <= 1; x++)
                                for (y = -1; y <= 1; y++) {
                                    if (DoG1000Second[i][j] <= DoG1000Third[i + x][j + y]) flagMax = 0;
                                    if (DoG1000Second[i][j] >= DoG1000Third[i + x][j + y]) flagMin = 0;
                                    if (DoG1000Third[i + x][j + y] > max) max = DoG1000Third[i + x][j + y];
                                    else if (DoG1000Third[i + x][j + y] < min) min = DoG1000Third[i + x][j + y];
                                }
                            if ((flagMax == 1 && DoG1000Second[i][j] > max) || (flagMin == 1 && DoG1000Second[i][j] < min)) {
                                for (x = -1; x <= 1; x++)
                                    for (y = -1; y <= 1; y++)
                                        if (x != 0 && y != 0) {
                                            if (DoG1000Second[i][j] <= DoG1000Second[i + x][j + y])
                                                flagMax = 0;
                                            if (DoG1000Second[i][j] >= DoG1000Second[i + x][j + y])
                                                flagMin = 0;
                                            if (DoG1000Second[i + x][j + y] > max)
                                                max = DoG1000Second[i + x][j + y];
                                            else if (DoG1000Second[i + x][j + y] < min)
                                                min = DoG1000Second[i + x][j + y];
                                        }
                                if ((flagMax == 1 && DoG1000Second[i][j] > max) || (flagMin == 1 && DoG1000Second[i][j] < min)) {
                                    // Now, we eliminate the edges
                                    Hessian[0][0] = DoG1000Second[i + 1][j] + DoG1000Second[i - 1][j] - 2.0 * DoG1000Second[i][j];
                                    Hessian[1][1] = DoG1000Second[i][j + 1] + DoG1000Second[i][j - 1] - 2.0 * DoG1000Second[i][j];
                                    Hessian[0][1] = (DoG1000Second[i + 1][j + 1] - DoG1000Second[i + 1][j - 1] - DoG1000Second[i - 1][j + 1] + DoG1000Second[i - 1][j - 1]) * 0.25;
                                    trace = Hessian[0][0] + Hessian[1][1]; //Trace of a matrix
                                    det = Hessian[0][0] * Hessian[1][1] - Hessian[0][1] * Hessian[0][1]; // Determinant of a matrix
                                    trace = trace * trace / det;
                                    // It was demonstrated that r = 10 is a good ratio, i.e. sqr(r+1)/r=121/10=12.1
                                    if (trace < 12.1 && trace > 0) {// r=10 here
                                        keypoints1000[nk][0]=i; keypoints1000[nk][1]=j;
                                        nk++;
                                        for (x = -10; x <= 10; x++) {
                                            bmOut.setPixel(i + x, j - 1, Color.argb(255, 255, 0, 255)); // fuchsia: rgb(255,0,255)
                                            bmOut.setPixel(i + x, j, Color.argb(255, 255, 0, 255)); // fuchsia: rgb(255,0,255)
                                            bmOut.setPixel(i + x, j + 1, Color.argb(255, 255, 0, 255)); // fuchsia: rgb(255,0,255)
                                            bmOut.setPixel(i - 1, j + x, Color.argb(255, 255, 0, 255)); // fuchsia: rgb(255,0,255)
                                            bmOut.setPixel(i, j + x, Color.argb(255, 255, 0, 255)); // fuchsia: rgb(255,0,255)
                                            bmOut.setPixel(i + 1, j + x, Color.argb(255, 255, 0, 255)); // fuchsia: rgb(255,0,255)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            // The following loop is for the THIRD DoG
            for (i=MatrixBorder+1;i<width-MatrixBorder-1;i++)
                for (j=MatrixBorder+1;j<height-MatrixBorder-1;j++) {
                    if (Math.abs(DoG1000Third[i][j]) >= threshold) {// We exclude extrema of low contrast: |D(X)| must be gt 0.03, here pixels are between 0 and 1
                        // First, we check for maximum/minimum
                        flagMax = 1; flagMin = 1;
                        max = -1000; min = 1000;
                        for (x = -1; x <= 1; x++)
                            for (y = -1; y <= 1; y++) {
                                if (DoG1000Third[i][j] <= DoG1000Second[i + x][j + y]) flagMax = 0;
                                if (DoG1000Third[i][j] >= DoG1000Second[i + x][j + y]) flagMin = 0;
                                if (DoG1000Second[i + x][j + y] > max) max = DoG1000Second[i + x][j + y];
                                else if (DoG1000Second[i + x][j + y] < min) min = DoG1000Second[i + x][j + y];
                            }
                        if ((flagMax == 1 && DoG1000Third[i][j] > max) || (flagMin == 1 && DoG1000Third[i][j] < min)) {
                            for (x = -1; x <= 1; x++)
                                for (y = -1; y <= 1; y++) {
                                    if (DoG1000Third[i][j] <= DoG1000Fourth[i + x][j + y]) flagMax = 0;
                                    if (DoG1000Third[i][j] >= DoG1000Fourth[i + x][j + y]) flagMin = 0;
                                    if (DoG1000Fourth[i + x][j + y] > max) max = DoG1000Fourth[i + x][j + y];
                                    else if (DoG1000Fourth[i + x][j + y] < min) min = DoG1000Fourth[i + x][j + y];
                                }
                            if ((flagMax == 1 && DoG1000Third[i][j] > max) || (flagMin == 1 && DoG1000Third[i][j] < min)) {
                                for (x = -1; x <= 1; x++)
                                    for (y = -1; y <= 1; y++)
                                        if (x != 0 && y != 0) {
                                            if (DoG1000Third[i][j] <= DoG1000Third[i + x][j + y]) flagMax = 0;
                                            if (DoG1000Third[i][j] >= DoG1000Third[i + x][j + y]) flagMin = 0;
                                            if (DoG1000Third[i + x][j + y] > max) max = DoG1000Third[i + x][j + y];
                                            else if (DoG1000Third[i + x][j + y] < min) min = DoG1000Third[i + x][j + y];
                                        }
                                if ((flagMax == 1 && DoG1000Third[i][j] > max) || (flagMin == 1 && DoG1000Third[i][j] < min)) {
                                    // Now, we eliminate the edges
                                    Hessian[0][0] = DoG1000Third[i + 1][j] + DoG1000Third[i - 1][j] - 2.0 * DoG1000Third[i][j];
                                    Hessian[1][1] = DoG1000Third[i][j + 1] + DoG1000Third[i][j - 1] - 2.0 * DoG1000Third[i][j];
                                    Hessian[0][1] = (DoG1000Third[i + 1][j + 1] - DoG1000Third[i + 1][j - 1] - DoG1000Third[i - 1][j + 1] + DoG1000Third[i - 1][j - 1]) * 0.25;
                                    trace = Hessian[0][0] + Hessian[1][1]; //Trace of a matrix
                                    det = Hessian[0][0] * Hessian[1][1] - Hessian[0][1] * Hessian[0][1]; // Determinant of a matrix
                                    trace = trace * trace / det;
                                    // It was demonstrated that r = 10 is a good ratio, i.e. sqr(r+1)/r=121/10=12.1
                                    if (trace < 12.1 && trace > 0) {// r=10 here
                                        keypoints1000[nk][0]=i; keypoints1000[nk][1]=j;
                                        nk++;
                                        for (x = -10; x <= 10; x++) {
                                            bmOut.setPixel(i + x, j - 1, Color.argb(255, 64, 224, 208)); // turquoise color: rgb(64,224,208)
                                            bmOut.setPixel(i + x, j, Color.argb(255, 64, 224, 208)); // turquoise color: rgb(64,224,208)
                                            bmOut.setPixel(i + x, j + 1, Color.argb(255, 64, 224, 208)); // turquoise color: rgb(64,224,208)
                                            bmOut.setPixel(i - 1, j + x, Color.argb(255, 64, 224, 208)); // turquoise color: rgb(64,224,208)
                                            bmOut.setPixel(i, j + x, Color.argb(255, 64, 224, 208)); // turquoise color: rgb(64,224,208)
                                            bmOut.setPixel(i + 1, j + x, Color.argb(255, 64, 224, 208)); // turquoise color: rgb(64,224,208)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            file = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+fileSeparator+"Temp"+fileSeparator+"KeyPoints"+ maxRes + ".jpg");
            out = new FileOutputStream(file);
            bmOut.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush(); out.close();

            // We write info about keypoints into the text file
            file = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+fileSeparator+"Temp"+fileSeparator+"KeyPoints"+maxRes+".txt");
            file.createNewFile();
            //second argument of FileOutputStream constructor indicates whether to append or create new file if one exists
            FileOutputStream outputStream = new FileOutputStream(file, false);
            PrintWriter writer = new PrintWriter(outputStream);
            writer.println(Integer.toString(nk)); writer.println("");
            for (i=0;i<nk;i++) { // nk is the number of keypoints
                writer.println(keypoints1000[i][0]); // X coordinate
                writer.println(keypoints1000[i][1]); // Y coordinate
                writer.println("");
            }
            writer.flush(); writer.close();

            Log.i(TAG, "Five scales of octave done. Resolution : " + maxRes);
        } catch (Exception e) {
            Log.i(TAG, "Exception  " + e);
        }
        Log.i(TAG, "Time at the end = " + Calendar.getInstance().getTime());
    }

    public void XkYk1st(){
        xk[13] = xk[7] + sigma1; yk[13] = yk[7] - sigma0;
        xk[12] = xk[13] + sigma0; yk[12] = yk[13] + sigma1;
        xk[14] = xk[13] - sigma0; yk[14] = yk[13] - sigma1;
        xk[15] = xk[14] - sigma0; yk[15] = yk[14] - sigma1;
        xk[16] = xk[15] - sigma0; yk[16] = yk[15] - sigma1;
        xk[17] = xk[16] - sigma0; yk[17] = yk[16] - sigma1;
        xk[1] = xk[0] - sigma0; yk[1] = yk[0] - sigma1;
        xk[2] = xk[1] - sigma0; yk[2] = yk[1] - sigma1;
        xk[3] = xk[2] - sigma0; yk[3] = yk[2] - sigma1;
        xk[4] = xk[3] - sigma0; yk[4] = yk[3] - sigma1;
        xk[8] = xk[7] - sigma0; yk[8] = yk[7] - sigma1;
        xk[9] = xk[8] - sigma0; yk[9] = yk[8] - sigma1;
        xk[18] = xk[13] + sigma1; yk[18] = yk[13] - sigma0;
        xk[19] = xk[14] + sigma1; yk[19] = yk[14] - sigma0;
        xk[20] = xk[15] + sigma1; yk[20] = yk[15] - sigma0;
        xk[21] = xk[16] + sigma1; yk[21] = yk[16] - sigma0;
        xk[22] = xk[19] + sigma1; yk[22] = yk[19] - sigma0;
        xk[23] = xk[20] + sigma1; yk[23] = yk[20] - sigma0;
        xk[24] = xk[22] + sigma1 - sigma0*0.5; yk[24] = yk[22] - sigma0 - sigma1*0.5;
        sigma4=Math.sqrt((xk[10]-xk[7])*(xk[10]-xk[7])+(yk[10]-yk[7])*(yk[10]-yk[7]))/24; // radius of the circle around the point, 1st and 2nd levels
    }

    public void XkYk2nd(){
        sigma0=sigma0*0.5; sigma1=sigma1*0.5;
        xk[25] = xk[0] - sigma0; yk[25] = yk[0] - sigma1;
        xk[26] = xk[1] - sigma0; yk[26] = yk[1] - sigma1;
        xk[27] = xk[2] - sigma0; yk[27] = yk[2] - sigma1;
        xk[28] = xk[3] - sigma0; yk[28] = yk[3] - sigma1;
        xk[29] = xk[4] - sigma0; yk[29] = yk[4] - sigma1;
        xk[30] = xk[6] - sigma1; yk[30] = yk[6] + sigma0;
        xk[31] = xk[30] - sigma0; yk[31] = yk[30] - sigma1;
        xk[32] = xk[31] - sigma0; yk[32] = yk[31] - sigma1;
        xk[33] = xk[32] - sigma0; yk[33] = yk[32] - sigma1;
        xk[34] = xk[33] - sigma0; yk[34] = yk[33] - sigma1;
        xk[35] = xk[34] - sigma0; yk[35] = yk[34] - sigma1;
        xk[36] = xk[35] - sigma0; yk[36] = yk[35] - sigma1;
        xk[37] = xk[36] - sigma0; yk[37] = yk[36] - sigma1;
        xk[38] = xk[37] - sigma0; yk[38] = yk[37] - sigma1;
        xk[39] = xk[38] - sigma0; yk[39] = yk[38] - sigma1;
        xk[40] = xk[39] - sigma0; yk[40] = yk[39] - sigma1;
        xk[41] = xk[6] - sigma0; yk[41] = yk[6] - sigma1;
        xk[42] = xk[7] - sigma0; yk[42] = yk[7] - sigma1;
        xk[43] = xk[8] - sigma0; yk[43] = yk[8] - sigma1;
        xk[44] = xk[9] - sigma0; yk[44] = yk[9] - sigma1;
        xk[45] = xk[10] - sigma0; yk[45] = yk[10] - sigma1;
        xk[46] = xk[12] - sigma1; yk[46] = yk[12] + sigma0;
        xk[47] = xk[46] - sigma0; yk[47] = yk[46] - sigma1;
        xk[48] = xk[47] - sigma0; yk[48] = yk[47] - sigma1;
        xk[49] = xk[48] - sigma0; yk[49] = yk[48] - sigma1;
        xk[50] = xk[49] - sigma0; yk[50] = yk[49] - sigma1;
        xk[51] = xk[50] - sigma0; yk[51] = yk[50] - sigma1;
        xk[52] = xk[51] - sigma0; yk[52] = yk[51] - sigma1;
        xk[53] = xk[52] - sigma0; yk[53] = yk[52] - sigma1;
        xk[54] = xk[53] - sigma0; yk[54] = yk[53] - sigma1;
        xk[55] = xk[54] - sigma0; yk[55] = yk[54] - sigma1;
        xk[56] = xk[55] - sigma0; yk[56] = yk[55] - sigma1;
        xk[57] = xk[12] - sigma0; yk[57] = yk[12] - sigma1;
        xk[58] = xk[13] - sigma0; yk[58] = yk[13] - sigma1;
        xk[59] = xk[14] - sigma0; yk[59] = yk[14] - sigma1;
        xk[60] = xk[15] - sigma0; yk[60] = yk[15] - sigma1;
        xk[61] = xk[16] - sigma0; yk[61] = yk[16] - sigma1;
        xk[62] = xk[57] + sigma1; yk[62] = yk[57] - sigma0;
        xk[63] = xk[62] - sigma0; yk[63] = yk[62] - sigma1;
        xk[64] = xk[63] - sigma0; yk[64] = yk[63] - sigma1;
        xk[65] = xk[64] - sigma0; yk[65] = yk[64] - sigma1;
        xk[66] = xk[65] - sigma0; yk[66] = yk[65] - sigma1;
        xk[67] = xk[66] - sigma0; yk[67] = yk[66] - sigma1;
        xk[68] = xk[67] - sigma0; yk[68] = yk[67] - sigma1;
        xk[69] = xk[68] - sigma0; yk[69] = yk[68] - sigma1;
        xk[70] = xk[69] - sigma0; yk[70] = yk[69] - sigma1;
        xk[71] = xk[18] - sigma0; yk[71] = yk[18] - sigma1;
        xk[72] = xk[19] - sigma0; yk[72] = yk[19] - sigma1;
        xk[73] = xk[20] - sigma0; yk[73] = yk[20] - sigma1;
        xk[74] = xk[71] + sigma1; yk[74] = yk[71] - sigma0;
        xk[75] = xk[74] - sigma0; yk[75] = yk[74] - sigma1;
        xk[76] = xk[75] - sigma0; yk[76] = yk[75] - sigma1;
        xk[77] = xk[76] - sigma0; yk[77] = yk[76] - sigma1;
        xk[78] = xk[77] - sigma0; yk[78] = yk[77] - sigma1;
        xk[79] = xk[22] - sigma0; yk[79] = yk[22] - sigma1;
        xk[80] = xk[22] + sigma1; yk[80] = yk[22] - sigma0;
        xk[81] = xk[80] - sigma0; yk[81] = yk[80] - sigma1;
        xk[82] = xk[81] - sigma0; yk[82] = yk[81] - sigma1;
    }

//    public class ThreadX1 extends Thread { // This thread is used to find the object
//    }

    class ThreadOctave0 extends Thread { // This thread is used to speed up SIFT
        private int i, j, x, y;
        public void run (){
            // we use radius4 to boost the performance
            for (i=radius4;i<width-radius4;i++)
                for (j = radius4; j < height - radius4; j++) {
                    octave1000First[i][j] = 0;
                    for (x = -radius0; x <= radius0; x++)
                        for (y = -radius0; y <= radius0; y++)
                            octave1000First[i][j] = octave1000First[i][j] + maskS0[x + radius0][y + radius0] * greyC[i + x][j + y];
//                    x=(int)Math.round(octave1000First[i][j]);
//                  bmOut1.setPixel(i, j, Color.argb(255, x, x, x));
                }

        }
    }

    class ThreadOctave1 extends Thread { // This thread is used to speed up SIFT
        private int i, j, x, y;
        public void run (){
            // we use radius4 to boost the performance
            for (i=radius4;i<width-radius4;i++)
                for (j = radius4; j < height - radius4; j++) {
                    octave1000Second[i][j] = 0;
                    for (x = -radius1; x <= radius1; x++)
                        for (y = -radius1; y <= radius1; y++)
                            octave1000Second[i][j] = octave1000Second[i][j] + maskS1[x + radius1][y + radius1] * greyC[i + x][j + y];

                }

        }
    }

    class ThreadOctave2 extends Thread { // This thread is used to speed up SIFT
        private int i, j, x, y;
        public void run (){
            // we use radius4 to boost the performance
            for (i=radius4;i<width-radius4;i++)
                for (j = radius4; j < height - radius4; j++) {
                    octave1000Third[i][j] = 0;
                    for (x = -radius2; x <= radius2; x++)
                        for (y = -radius2; y <= radius2; y++)
                            octave1000Third[i][j] = octave1000Third[i][j] + maskS2[x + radius2][y + radius2] * greyC[i + x][j + y];

                }

        }
    }

    class ThreadOctave3 extends Thread { // This thread is used to speed up SIFT
        private int i, j, x, y;
        public void run (){
            // we use radius4 to boost the performance
            for (i=radius4;i<width-radius4;i++)
                for (j = radius4; j < height - radius4; j++) {
                    octave1000Fourth[i][j] = 0;
                    for (x = -radius3; x <= radius3; x++)
                        for (y = -radius3; y <= radius3; y++)
                            octave1000Fourth[i][j] = octave1000Fourth[i][j] + maskS3[x + radius3][y + radius3] * greyC[i + x][j + y];

                }

        }
    }

    class DoGFirst extends Thread { // This thread is used to speed up SIFT
        private int i, j;
        public void run (){
            for (i=MatrixBorder;i<width-MatrixBorder;i++)
                for (j=MatrixBorder;j<height-MatrixBorder;j++) {
                    DoG1000First[i][j]= octave1000First[i][j] - octave1000Second[i][j];

                }
        }
    }

    class DoGSecond extends Thread { // This thread is used to speed up SIFT
        private int i, j;
        public void run (){
            for (i=MatrixBorder;i<width-MatrixBorder;i++)
                for (j=MatrixBorder;j<height-MatrixBorder;j++) {
                    DoG1000Second[i][j]= octave1000Second[i][j] - octave1000Third[i][j];

                }
        }
    }

    class DoGThird extends Thread { // This thread is used to speed up SIFT
        private int i, j;
        public void run (){
            for (i=MatrixBorder;i<width-MatrixBorder;i++)
                for (j=MatrixBorder;j<height-MatrixBorder;j++) {
                    DoG1000Third[i][j]= octave1000Third[i][j] - octave1000Fourth[i][j];

                }
        }
    }

    class SIFTkeypointsSecond extends Thread { // This thread is used to speed up SIFT
        private int i, j, flagMax, flagMin, x, y;
        private double min, max, trace, det;
        private double [][] Hessian = new double [2][2]; // 2x2 Hessian matrix
        public void run (){
            nkSecond=0; // Number of keypoints in the 2nd part equals 0 initially
            for (i=MatrixBorder+1;i<width-MatrixBorder-1;i++)
                for (j=MatrixBorder+1;j<height-MatrixBorder-1;j++) {
                    // The following condition is for the THIRD DoG
                    if (Math.abs(DoG1000Third[i][j]) >= threshold) {// We exclude extrema of low contrast: |D(X)| must be gt 0.03, here pixels are between 0 and 1
                        // First, we check for maximum/minimum
                        flagMax = 1; flagMin = 1;
                        max = -1000; min = 1000;
                        for (x = -1; x <= 1; x++) {
                            for (y = -1; y <= 1; y++) {
                                if (DoG1000Third[i][j] <= DoG1000Second[i + x][j + y]) flagMax = 0;
                                if (DoG1000Third[i][j] >= DoG1000Second[i + x][j + y]) flagMin = 0;
                                if (DoG1000Second[i + x][j + y] > max) max = DoG1000Second[i + x][j + y];
                                else if (DoG1000Second[i + x][j + y] < min) min = DoG1000Second[i + x][j + y];
                            }
                            if (flagMax==0 && flagMin==0) break;
                        }
                        if ((flagMax == 1 && DoG1000Third[i][j] > max) || (flagMin == 1 && DoG1000Third[i][j] < min)) {
                            for (x = -1; x <= 1; x++) {
                                for (y = -1; y <= 1; y++) {
                                    if (DoG1000Third[i][j] <= DoG1000Fourth[i + x][j + y]) flagMax = 0;
                                    if (DoG1000Third[i][j] >= DoG1000Fourth[i + x][j + y]) flagMin = 0;
                                    if (DoG1000Fourth[i + x][j + y] > max) max = DoG1000Fourth[i + x][j + y];
                                    else if (DoG1000Fourth[i + x][j + y] < min) min = DoG1000Fourth[i + x][j + y];
                                }
                                if (flagMax==0 && flagMin==0) break;
                            }
                            if ((flagMax == 1 && DoG1000Third[i][j] > max) || (flagMin == 1 && DoG1000Third[i][j] < min)) {
                                for (x = -1; x <= 1; x++){
                                    for (y = -1; y <= 1; y++)
                                        if (x != 0 && y != 0) {
                                            if (DoG1000Third[i][j] <= DoG1000Third[i + x][j + y]) flagMax = 0;
                                            if (DoG1000Third[i][j] >= DoG1000Third[i + x][j + y]) flagMin = 0;
                                            if (DoG1000Third[i + x][j + y] > max) max = DoG1000Third[i + x][j + y];
                                            else if (DoG1000Third[i + x][j + y] < min) min = DoG1000Third[i + x][j + y];
                                        }
                                    if (flagMax==0 && flagMin==0) break;
                                }
                                if ((flagMax == 1 && DoG1000Third[i][j] > max) || (flagMin == 1 && DoG1000Third[i][j] < min)) {
                                    // Now, we eliminate the edges
                                    Hessian[0][0] = DoG1000Third[i + 1][j] + DoG1000Third[i - 1][j] - 2.0 * DoG1000Third[i][j];
                                    Hessian[1][1] = DoG1000Third[i][j + 1] + DoG1000Third[i][j - 1] - 2.0 * DoG1000Third[i][j];
                                    Hessian[0][1] = (DoG1000Third[i + 1][j + 1] - DoG1000Third[i + 1][j - 1] - DoG1000Third[i - 1][j + 1] + DoG1000Third[i - 1][j - 1]) * 0.25;
                                    trace = Hessian[0][0] + Hessian[1][1]; //Trace of a matrix
                                    det = Hessian[0][0] * Hessian[1][1] - Hessian[0][1] * Hessian[0][1]; // Determinant of a matrix
                                    // It was demonstrated that r = 10 is a good ratio, i.e. sqr(r+1)/r=121/10=12.1
                                    if (trace * trace / det < 12.1) {// r=10 here
                                        keypoints1000Second[nkSecond][0]=i; keypoints1000Second[nkSecond][1]=j;
                                        nkSecond++;
                                    }
                                }
                            }
                        }
                    }
                }
        }
    }
}
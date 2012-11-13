package org.apache.cordova.camera;

import java.io.File;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.cordova.R;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

@SuppressLint("NewApi")
public class CameraActivity extends Activity {

    protected static final String TAG = "CameraActivity";
    private Camera mCamera;
    private Preview mPreview;
    private CameraActivity that;
    private boolean debounce = false;
    private ZoomListener mZoom;
    Slider zoomSlider;
    
    //Here are the two views
    RelativeLayout previewView;
    RelativeLayout cameraView;
    
    //This is for saving
    Bitmap previewImage;
    Uri fileUri;
    private byte[] image;

    class AFCallback implements Camera.AutoFocusCallback {
        public void onAutoFocus(boolean success, Camera camera)
        {
            Log.d(TAG, "AutoFocus has completed");
            camera.takePicture(null, null, mPicture);
            debounce = true;
        }
    }
    
    class ZoomListener implements Slider.SliderPositionListener {
        Camera.Parameters params; 
        int limit; 
        
        ZoomListener()
        {
            params = mCamera.getParameters();
            if(params.isZoomSupported())
            {
                limit = params.getMaxZoom();
            }
        }
        
        public void onPositionChange(double value) {
            Log.d(TAG, "We are zooming in to: " + Double.toString(value));
            if(params.isZoomSupported())
            {
                int zoomVal = (int) (value * limit);
                params.setZoom(zoomVal);
                mCamera.setParameters(params);
            }
        }
    }

    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "In CameraActivity");
        Log.d(TAG, "explode layout");
        that = this;
        
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        /*
         * Do the initialization of the interface here
         */
        
        setContentView(R.layout.capture);
        previewView = (RelativeLayout) findViewById(R.id.previewView);
        previewView.setVisibility(View.INVISIBLE);

        // Create an instance of Camera
        Log.d(TAG, "get instance of camera");
        mCamera = getCameraInstance();

        if(mCamera == null)
        {
            // There's no camera here, finish this application
            // Note: We shouldn't get here if the developer specifies
            // the permissions right in the Android Manifest.
            this.finish();
        }
        else
        {
            // Create our Preview view and set it as the content of our activity.
            Log.d(TAG, "create preview");
            mPreview = new Preview(this, mCamera);
            FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
            preview.addView(mPreview);
            if(android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.HONEYCOMB_MR2)
                preview.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
            
            
            // Add a listener to the Capture button
            Log.d(TAG, "setup button listener");
            Button captureButton = (Button) findViewById(R.id.button_capture);
            captureButton.setBackgroundResource(R.drawable.bt_camera);
            captureButton.setOnClickListener(
                new View.OnClickListener() {
    
                    //@Override
                    public void onClick(View v) {
                        if(!debounce)
                        {
                            mCamera.autoFocus(new AFCallback());
                        }
                    }
                }
            );
            
            zoomSlider  = (Slider) findViewById(R.id.zoom_slider);
            mZoom = new ZoomListener();
            zoomSlider.setPositionListener(mZoom);
            
            Button zoomOut = (Button) findViewById(R.id.zoom_out);
            zoomOut.setBackgroundResource(R.drawable.bt_zoom_out);
            zoomOut.setOnClickListener(
               new View.OnClickListener() {
                    public void onClick(View v) {
                        zoomSlider.decrement();
                    }
               }
            );
            Button zoomIn = (Button) findViewById(R.id.zoom_in);
            zoomIn.setBackgroundResource(R.drawable.bt_zoom_in);
            zoomIn.setOnClickListener(
               new View.OnClickListener() {
                    public void onClick(View v) {
                        zoomSlider.increment();
                    }
               }
            );
            Button saveImage = (Button) findViewById(R.id.usePhoto);
            saveImage.setOnClickListener(
                    new View.OnClickListener() {
                        
                        public void onClick(View v) {
                            saveAndExit();
                        }
                    }
            );
            Button redoImage = (Button) findViewById(R.id.redoPhoto);
            redoImage.setOnClickListener(
                    new View.OnClickListener() {
                        
                        public void onClick(View v) {
                            previewView.setVisibility(View.INVISIBLE);
                            cameraView.setVisibility(View.VISIBLE);
                            debounce = false;
                            mCamera.startPreview();
                        }
                    }
            );
            Button cancelImage = (Button) findViewById(R.id.cancel);
            cancelImage.setOnClickListener(
                    new View.OnClickListener() {
                        
                        public void onClick(View v) {
                            setResult(RESULT_CANCELED);
                            finish();
                        }
                    }
            );
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "camera got paused");
        if (mCamera != null){
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
        if(android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.HONEYCOMB_MR2)
            mPreview.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        super.onPause();
    }    
    
    
    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return c; // returns null if camera is unavailable
    }
    
    private PictureCallback mPicture = new PictureCallback() {

        //@Override
        @TargetApi(8)
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.d(TAG, "in onpicturetaken");
            
            image = data;
            cameraView = (RelativeLayout) findViewById(R.id.cameraView);
            previewView.setVisibility(View.VISIBLE);
            cameraView.setVisibility(View.INVISIBLE);
            
            int rotation = that.getWindowManager().getDefaultDisplay().getRotation();
            Log.d(TAG, "Rotation is = " + rotation);

            fileUri = (Uri) getIntent().getExtras().get(MediaStore.EXTRA_OUTPUT);
            
        }
    };
    
    
    private void saveAndExit()
    {
        File pictureFile = new File(fileUri.getPath());

        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(image);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
        
        // Recycle the image and send the result back
        if(previewImage != null)
            previewImage.recycle();
        setResult(RESULT_OK);
        finish();
    }
}
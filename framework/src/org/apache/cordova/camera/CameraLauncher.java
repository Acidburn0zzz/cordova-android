package org.apache.cordova.camera;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.cordova.ExifHelper;
import org.apache.cordova.api.CordovaInterface;
import org.apache.cordova.api.LOG;
import org.apache.cordova.api.Plugin;
import org.apache.cordova.api.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;


/**
 * This class launches the camera view, allows the user to take a picture, closes the camera view,
 * and returns the captured image.  When the camera view is closed, the screen displayed before 
 * the camera view was shown is redisplayed.
 */
public class CameraLauncher extends Plugin {

    private static final int DATA_URL = 0;              // Return base64 encoded string
    private static final int FILE_URI = 1;              // Return file uri (content://media/external/images/media/2 for Android)

    private static final int PHOTOLIBRARY = 0;          // Choose image from picture library (same as SAVEDPHOTOALBUM for Android)
    private static final int CAMERA = 1;                // Take picture from camera
    private static final int SAVEDPHOTOALBUM = 2;       // Choose image from picture library (same as PHOTOLIBRARY for Android)

    private static final int PICTURE = 0;               // allow selection of still pictures only. DEFAULT. Will return format specified via DestinationType
    private static final int VIDEO = 1;                 // allow selection of video only, ONLY RETURNS URL
    private static final int ALLMEDIA = 2;              // allow selection from all media types

    private static final int JPEG = 0;                  // Take a picture of type JPEG
    private static final int PNG = 1;                   // Take a picture of type PNG
    private static final String GET_PICTURE = "Get Picture";
    private static final String GET_VIDEO = "Get Video";
    private static final String GET_All = "Get All";

    private static final String LOG_TAG = "CameraLauncher";

    private int mQuality;                   // Compression quality hint (0-100: 0=low quality & high compression, 100=compress of max quality)
    private int targetWidth;                // desired width of the image
    private int targetHeight;               // desired height of the image
    private Uri imageUri;                   // Uri of captured image
    private int encodingType;               // Type of encoding to use
    private int mediaType;                  // What type of media to retrieve
    private boolean saveToPhotoAlbum;       // Should the picture be saved to the device's photo album
    private boolean correctOrientation;     // Should the pictures orientation be corrected
    private boolean allowEdit;              // Should we allow the user to crop the image

    public String callbackId;
    private int numPics;

    private MediaScannerConnection conn;    // Used to update gallery app with newly-written files
    private Uri scanMe;                     // Uri of image to be added to content store

    private File photo;
    
    private static final String _DATA = "_data";    
    
    /**
     * Constructor.
     */
    public CameraLauncher() {
    }

    /**
     * Executes the request and returns PluginResult.
     * 
     * @param action        The action to execute.
     * @param args          JSONArry of arguments for the plugin.
     * @param callbackId    The callback id used when calling back into JavaScript.
     * @return              A PluginResult object with a status and message.
     */
    public PluginResult execute(String action, JSONArray args, String callbackId) {
        PluginResult.Status status = PluginResult.Status.OK;
        String result = "";
        this.callbackId = callbackId;

        try {
            if (action.equals("takePicture")) {
                int srcType = CAMERA;
                int destType = FILE_URI;
                this.saveToPhotoAlbum = false;
                this.targetHeight = 0;
                this.targetWidth = 0;
                this.encodingType = JPEG;
                this.mediaType = PICTURE;
                this.mQuality = 80;

                this.mQuality = args.getInt(0);
                destType = args.getInt(1);
                srcType = args.getInt(2);
                this.targetWidth = args.getInt(3);
                this.targetHeight = args.getInt(4);
                this.encodingType = args.getInt(5);
                this.mediaType = args.getInt(6);
                this.allowEdit = args.getBoolean(7);
                this.correctOrientation = args.getBoolean(8);
                this.saveToPhotoAlbum = args.getBoolean(9);

                // If the user specifies a 0 or smaller width/height
                // make it -1 so later comparisons succeed
                if (this.targetWidth < 1) {
                    this.targetWidth = -1;
                }
                if (this.targetHeight < 1) {
                    this.targetHeight = -1;
                }

                if (srcType == CAMERA) {
                    this.takePicture(destType, encodingType);
                }
                else if ((srcType == PHOTOLIBRARY) || (srcType == SAVEDPHOTOALBUM)) {
                    this.getImage(srcType, destType);
                }
                PluginResult r = new PluginResult(PluginResult.Status.NO_RESULT);
                r.setKeepCallback(true);
                return r;
            }
            return new PluginResult(status, result);
        } catch (JSONException e) {
            e.printStackTrace();
            return new PluginResult(PluginResult.Status.JSON_EXCEPTION);
        }
    }
    
    //--------------------------------------------------------------------------
    // LOCAL METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Take a picture with the camera.
     * When an image is captured or the camera view is cancelled, the result is returned
     * in CordovaActivity.onActivityResult, which forwards the result to this.onActivityResult.
     * 
     * The image can either be returned as a base64 string or a URI that points to the file.
     * To display base64 string in an img tag, set the source to:
     *      img.src="data:image/jpeg;base64,"+result;
     * or to display URI in an img tag
     *      img.src=result;
     * 
     */
    public void takePicture(int returnType, int encodingType) {
        Log.d(LOG_TAG, "In takePicture method");
        // Save the number of images currently on disk for later
        this.numPics = queryImgDB().getCount();

        Intent intent = new Intent(this.cordova.getActivity(), CameraActivity.class);
        this.photo = createCaptureFile();
        this.imageUri = Uri.fromFile(photo);
        Log.d(LOG_TAG, "Using URI = " + this.imageUri.toString());
        intent.putExtra(MediaStore.EXTRA_OUTPUT, this.imageUri);
                
        Log.d(LOG_TAG, "calling start activity");
        this.cordova.startActivityForResult((Plugin) this, intent, 1);      
    }
    
    /**
     * Get image from photo library.
     *
     * @param quality           Compression quality hint (0-100: 0=low quality & high compression, 100=compress of max quality)
     * @param srcType           The album to get image from.
     * @param returnType        Set the type of image to return.
     */
    // TODO: Images selected from SDCARD don't display correctly, but from CAMERA ALBUM do!
    public void getImage(int srcType, int returnType) {
        Intent intent = new Intent();
        String title = GET_PICTURE;
        if (this.mediaType == PICTURE) {
            intent.setType("image/*");
        }
        else if (this.mediaType == VIDEO) {
            intent.setType("video/*");
            title = GET_VIDEO;
        }
        else if (this.mediaType == ALLMEDIA) {
            // I wanted to make the type 'image/*, video/*' but this does not work on all versions
            // of android so I had to go with the wildcard search.
            intent.setType("*/*");
            title = GET_All;
        }

        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        if (this.cordova != null) {
            this.cordova.startActivityForResult((Plugin) this, Intent.createChooser(intent,
                    new String(title)), (srcType + 1) * 16 + returnType + 1);
        }
    }

    /**
     * Create a file in the applications temporary directory based upon the supplied encoding.
     * 
     * @return a File object pointing to the temporary picture
     */
    private File createCaptureFile() {
        File photo = new File(getTempDirectoryPath(cordova.getActivity()),  "Pic.jpg");
        return photo;
    }
 
    /**
     * Called when the camera view exits. 
     * 
     * @param requestCode       The request code originally supplied to startActivityForResult(), 
     *                          allowing you to identify who this result came from.
     * @param resultCode        The integer result code returned by the child activity through its setResult().
     * @param intent            An Intent, which can return result data to the caller (various data can be attached to Intent "extras").
     */
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.d(LOG_TAG, "cameralauncher onactivityresult");
        // If image available
        if (resultCode == Activity.RESULT_OK) {
            Log.d(LOG_TAG, "Just returning the path to the image with no manipulation");
            this.success(new PluginResult(PluginResult.Status.OK, this.imageUri.toString()), this.callbackId);
        }
        // If cancelled
        else if (resultCode == Activity.RESULT_CANCELED) {
            this.failPicture("Camera cancelled.");
        }

        // If something else
        else {
            this.failPicture("Did not complete!");
        }
    }
    
    /**
     * Scales the bitmap according to the requested size.
     * 
     * @param bitmap        The bitmap to scale.
     * @return Bitmap       A new Bitmap object of the same bitmap after scaling. 
     */
    public Bitmap scaleBitmap(Bitmap bitmap) {
        int newWidth = this.targetWidth;
        int newHeight = this.targetHeight;
        int origWidth = bitmap.getWidth();
        int origHeight = bitmap.getHeight();

        // If no new width or height were specified return the original bitmap
        if (newWidth <= 0 && newHeight <= 0) {
            return bitmap;
        }
        // Only the width was specified
        else if (newWidth > 0 && newHeight <= 0) {
            newHeight = (newWidth * origHeight) / origWidth;
        }
        // only the height was specified
        else if (newWidth <= 0 && newHeight > 0) {
            newWidth = (newHeight * origWidth) / origHeight;
        }
        // If the user specified both a positive width and height
        // (potentially different aspect ratio) then the width or height is
        // scaled so that the image fits while maintaining aspect ratio.
        // Alternatively, the specified width and height could have been
        // kept and Bitmap.SCALE_TO_FIT specified when scaling, but this
        // would result in whitespace in the new image.
        else {
            double newRatio = newWidth / (double)newHeight;
            double origRatio = origWidth / (double)origHeight;

            if (origRatio > newRatio) {
                newHeight = (newWidth * origHeight) / origWidth;
            } else if (origRatio < newRatio) {
                newWidth = (newHeight * origWidth) / origHeight;
            }
        }
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    /**
     * Creates a cursor that can be used to determine how many images we have.
     * 
     * @return a cursor
     */
    private Cursor queryImgDB() {
        return this.cordova.getActivity().getContentResolver().query(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[] { MediaStore.Images.Media._ID },
                null,
                null,
                null);
    }
    
    /**
     * Used to find out if we are in a situation where the Camera Intent adds to images
     * to the content store. If we are using a FILE_URI and the number of images in the DB 
     * increases by 2 we have a duplicate, when using a DATA_URL the number is 1.
     */
    private void checkForDuplicateImage() {
        int diff = 2;
        Cursor cursor = queryImgDB();
        int currentNumOfImages = cursor.getCount();
        
        // delete the duplicate file if the difference is 2 for file URI or 1 for Data URL
        if ((currentNumOfImages - numPics) == diff) {
            cursor.moveToLast();
            int id = Integer.valueOf(cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media._ID))) - 1;                    
            Uri uri = Uri.parse(MediaStore.Images.Media.EXTERNAL_CONTENT_URI + "/" + id);
            this.cordova.getActivity().getContentResolver().delete(uri, null, null);
        }
    }
    
    /**
     * Determine if we can use the SD Card to store the temporary file.  If not then use 
     * the internal cache directory.
     * 
     * @return the absolute path of where to store the file
     */
    private String getTempDirectoryPath(Context ctx) {
        File cache = null;
        
        // SD Card Mounted
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            cache = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + 
                    "/Android/data/" + ctx.getPackageName() + "/cache/");
        } 
        // Use internal storage
        else {
            cache = ctx.getCacheDir();
        }

        // Create the cache directory if it doesn't exist
        if (!cache.exists()) {
            cache.mkdirs();
        }

        return cache.getAbsolutePath();
    }
    
    /**
     * Queries the media store to find out what the file path is for the Uri we supply
     *
     * @param contentUri the Uri of the audio/image/video
     * @param ctx the current applicaiton context
     * @return the full path to the file
     */
    private String getRealPathFromURI(Uri contentUri, CordovaInterface ctx) {
        String[] proj = { _DATA };
        Cursor cursor = this.cordova.getActivity().managedQuery(contentUri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(_DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }    

    /**
     * Send error message to JavaScript.
     *
     * @param err
     */
    public void failPicture(String err) {
        this.error(new PluginResult(PluginResult.Status.ERROR, err), this.callbackId);
    }
}
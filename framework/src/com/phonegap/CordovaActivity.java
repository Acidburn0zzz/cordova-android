package com.phonegap;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import com.phonegap.LinearLayoutSoftKeyboardDetect;
import com.phonegap.api.IPlugin;
import com.phonegap.api.PluginManager;

public class CordovaActivity extends Activity {

    CordovaView appView;
    private LinearLayoutSoftKeyboardDetect root;
    private int backgroundColor = Color.BLACK;
    private static int ACTIVITY_STARTING = 0;
    private static int ACTIVITY_RUNNING = 1;
    private static int ACTIVITY_EXITING = 2;
    private int activityState = 0;  // 0=starting, 1=running (after 1st resume), 2=shutting down
    private boolean keepRunning;
    private boolean activityResultKeepRunning;
    private PluginManager pluginManager;
    
    // Draw a splash screen using an image located in the drawable resource directory.
    // This is not the same as calling super.loadSplashscreen(url)
    protected int splashscreen = 0;
    private ProgressDialog spinnerDialog;
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        // This builds the view.  We could probably get away with NOT having a LinearLayout, but I like having a bucket!

        Display display = getWindowManager().getDefaultDisplay(); 
        int width = display.getWidth();
        int height = display.getHeight();
        
        root = new LinearLayoutSoftKeyboardDetect(this, width, height);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(this.backgroundColor);
        root.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 
                ViewGroup.LayoutParams.FILL_PARENT, 0.0F));
        
        appView = new CordovaView(this);

        root.addView(appView);
        pluginManager = appView.appCode.pluginManager;
        setContentView(root);
    }
    

    @Override
    /**
     * Called when the system is about to start resuming a previous activity. 
     */
    protected void onPause() {
        super.onPause();
        
        // Don't process pause if shutting down, since onDestroy() will be called
        if (this.activityState == ACTIVITY_EXITING) {
            return;
        }

        if (this.appView == null) {
            return;
        }

        // Send pause event to JavaScript
        this.appView.loadUrl("javascript:try{PhoneGap.fireDocumentEvent('pause');}catch(e){};");

        // Forward to plugins
        this.pluginManager.onPause(this.keepRunning);

        // If app doesn't want to run in background
        if (!this.keepRunning) {
            // Pause JavaScript timers (including setInterval)
            this.appView.pauseTimers();
        }
    }

    public void endActivity() {
        this.activityState = ACTIVITY_EXITING;
        this.finish();
    }
    
    
    /**
     * Get boolean property for activity.
     * 
     * @param name
     * @param defaultValue
     * @return
     */
    public boolean getBooleanProperty(String name, boolean defaultValue) {
        Bundle bundle = this.getIntent().getExtras();
        if (bundle == null) {
            return defaultValue;
        }
        Boolean p = (Boolean)bundle.get(name);
        if (p == null) {
            return defaultValue;
        }
        return p.booleanValue();
    }

    /**
     * Get int property for activity.
     * 
     * @param name
     * @param defaultValue
     * @return
     */
    public int getIntegerProperty(String name, int defaultValue) {
        Bundle bundle = this.getIntent().getExtras();
        if (bundle == null) {
            return defaultValue;
        }
        Integer p = (Integer)bundle.get(name);
        if (p == null) {
            return defaultValue;
        }
        return p.intValue();
    }

    /**
     * Get string property for activity.
     * 
     * @param name
     * @param defaultValue
     * @return
     */
    public String getStringProperty(String name, String defaultValue) {
        Bundle bundle = this.getIntent().getExtras();
        if (bundle == null) {
            return defaultValue;
        }
        String p = bundle.getString(name);
        if (p == null) {
            return defaultValue;
        }
        return p;
    }

    /**
     * Get double property for activity.
     * 
     * @param name
     * @param defaultValue
     * @return
     */
    public double getDoubleProperty(String name, double defaultValue) {
        Bundle bundle = this.getIntent().getExtras();
        if (bundle == null) {
            return defaultValue;
        }
        Double p = (Double)bundle.get(name);
        if (p == null) {
            return defaultValue;
        }
        return p.doubleValue();
    }

    /**
     * Set boolean property on activity.
     * 
     * @param name
     * @param value
     */
    public void setBooleanProperty(String name, boolean value) {
        this.getIntent().putExtra(name, value);
    }
    
    /**
     * Set int property on activity.
     * 
     * @param name
     * @param value
     */
    public void setIntegerProperty(String name, int value) {
        this.getIntent().putExtra(name, value);
    }
    
    /**
     * Set string property on activity.
     * 
     * @param name
     * @param value
     */
    public void setStringProperty(String name, String value) {
        this.getIntent().putExtra(name, value);
    }

    /**
     * Set double property on activity.
     * 
     * @param name
     * @param value
     */
    public void setDoubleProperty(String name, double value) {
        this.getIntent().putExtra(name, value);
    }

    
    /**
     * Look at activity parameters and process them.
     * This must be called from the main UI thread.
     */
    void handleActivityParameters() {

        // If backgroundColor
        this.backgroundColor = this.getIntegerProperty("backgroundColor", Color.BLACK);
        this.root.setBackgroundColor(this.backgroundColor);

        // If spashscreen
        this.splashscreen = this.getIntegerProperty("splashscreen", 0);
        if ((this.appView.urls.size() == 0) && (this.splashscreen != 0)) {
            root.setBackgroundResource(this.splashscreen);
        }

        
        // If keepRunning
        this.keepRunning = this.getBooleanProperty("keepRunning", true);
    }
    
    @Override
    /**
     * Called when the activity will start interacting with the user. 
     */
    protected void onResume() {
        super.onResume();
        
        if (this.activityState == ACTIVITY_STARTING) {
            this.activityState = ACTIVITY_RUNNING;
            return;
        }

        if (this.appView == null) {
            return;
        }

        // Send resume event to JavaScript
        this.appView.loadUrl("javascript:try{PhoneGap.fireDocumentEvent('resume');}catch(e){};");

        // Forward to plugins
        this.pluginManager.onResume(this.keepRunning || this.activityResultKeepRunning);

        // If app doesn't want to run in background
        if (!this.keepRunning || this.activityResultKeepRunning) {

            // Restore multitasking state
            if (this.activityResultKeepRunning) {
                this.keepRunning = this.activityResultKeepRunning;
                this.activityResultKeepRunning = false;
            }

            // Resume JavaScript timers (including setInterval)
            this.appView.resumeTimers();
        }
    }
    

    @Override
    /**
     * The final call you receive before your activity is destroyed. 
     */
    public void onDestroy() {
        super.onDestroy();
        
        if (this.appView != null) {
            // Send destroy event to JavaScript
            this.appView.loadUrl("javascript:try{PhoneGap.onDestroy.fire();}catch(e){};");

            // Load blank page so that JavaScript onunload is called
            this.appView.loadUrl("about:blank");

            // Forward to plugins
            this.pluginManager.onDestroy();
        }
        else {
            this.endActivity();
        }
    }
    
    public void loadUrl(String url)
    {
        appView.loadUrl(url);
    }
    
    @Override
    /**
     * Called when an activity you launched exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it. 
     * 
     * @param requestCode       The request code originally supplied to startActivityForResult(), 
     *                          allowing you to identify who this result came from.
     * @param resultCode        The integer result code returned by the child activity through its setResult().
     * @param data              An Intent, which can return result data to the caller (various data can be attached to Intent "extras").
     */
     protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
         super.onActivityResult(requestCode, resultCode, intent);
         appView.appCode.onPluginResult(requestCode, resultCode, intent);
     }
     
     /**
      * Called when a key is pressed.
      * 
      * @param keyCode
      * @param event
      */
     @Override
     public boolean onKeyDown(int keyCode, KeyEvent event) {
         if (this.appView == null) {
             return super.onKeyDown(keyCode, event);
         }

         // If back key
         if (keyCode == KeyEvent.KEYCODE_BACK) {

             // If back key is bound, then send event to JavaScript
             if (this.appView.checkBackKey()) {
                 this.appView.loadUrl("javascript:PhoneGap.fireDocumentEvent('backbutton');");
                 return true;
             }

             // If not bound
             else {

                 // Go to previous page in webview if it is possible to go back
                 if (this.appView.backHistory()) {
                     return true;
                 }
                 // If not, then invoke behavior of super class
                 else {
                     this.activityState = ACTIVITY_EXITING;
                     return super.onKeyDown(keyCode, event);
                 }
             }
         }

         // If menu key
         else if (keyCode == KeyEvent.KEYCODE_MENU) {
             this.appView.loadUrl("javascript:PhoneGap.fireDocumentEvent('menubutton');");
             return super.onKeyDown(keyCode, event);
         }

         // If search key
         else if (keyCode == KeyEvent.KEYCODE_SEARCH) {
             this.appView.loadUrl("javascript:PhoneGap.fireDocumentEvent('searchbutton');");
             return true;
         }

         return false;
     }
     

     private void postMessage(String id, Object data) {
         // Forward to plugins
         this.appView.postMessage(id, data);
     }
     

     
     /* 
      * Hook in DroidGap for menu plugins
      * 
      */
     
     @Override
     public boolean onCreateOptionsMenu(Menu menu)
     {
         this.postMessage("onCreateOptionsMenu", menu);
         return super.onCreateOptionsMenu(menu);
     }
     
     
    @Override
     public boolean onPrepareOptionsMenu(Menu menu)
     {
         this.postMessage("onPrepareOptionsMenu", menu);
         return super.onPrepareOptionsMenu(menu);
     }
     
     @Override
     public boolean onOptionsItemSelected(MenuItem item)
     {
         this.postMessage("onOptionsItemSelected", item);
         return true;
     }

     /**
      * Show the spinner.  Must be called from the UI thread.
      * 
      * @param title         Title of the dialog
      * @param message       The message of the dialog
      */
     public void spinnerStart(final String title, final String message) {
         if (this.spinnerDialog != null) {
             this.spinnerDialog.dismiss();
             this.spinnerDialog = null;
         }
         final CordovaActivity me = this;
         this.spinnerDialog = ProgressDialog.show(CordovaActivity.this, title , message, true, true, 
                 new DialogInterface.OnCancelListener() { 
             public void onCancel(DialogInterface dialog) {
                 me.spinnerDialog = null;
             }
         });
     }

     /**
      * Stop spinner.
      */
     public void spinnerStop() {
         if (this.spinnerDialog != null) {
             this.spinnerDialog.dismiss();
             this.spinnerDialog = null;
         }
     }
}

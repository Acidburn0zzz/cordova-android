/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
*/
package org.apache.cordova;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.cordova.PreferenceNode;
import org.apache.cordova.PreferenceSet;
import org.apache.cordova.api.IPlugin;
import org.apache.cordova.api.LOG;
import org.apache.cordova.api.CordovaInterface;
import org.apache.cordova.api.PluginManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;


/*
 * DroidGap will soon be renamed to CordovaActivity for Cordova 2.0
 * 
 */

/**
 * This class is the main Android activity that represents the Cordova
 * application.  It should be extended by the user to load the specific
 * html file that contains the application.
 * 
 * As an example:
 * 
 *     package org.apache.cordova.examples;
 *     import android.app.Activity;
 *     import android.os.Bundle;
 *     import org.apache.cordova.*;
 *     
 *     public class Examples extends DroidGap {
 *       @Override
 *       public void onCreate(Bundle savedInstanceState) {
 *         super.onCreate(savedInstanceState);
 *                  
 *         // Set properties for activity
 *         super.setStringProperty("loadingDialog", "Title,Message"); // show loading dialog
 *         super.setStringProperty("errorUrl", "file:///android_asset/www/error.html"); // if error loading file in super.loadUrl().
 *
 *         // Initialize activity
 *         super.init();
 *         
 *         // Clear cache if you want
 *         super.appView.clearCache(true);
 *         
 *         // Load your application
 *         super.setIntegerProperty("splashscreen", R.drawable.splash); // load splash.jpg image from the resource drawable directory
 *         super.loadUrl("file:///android_asset/www/index.html", 3000); // show splash screen 3 sec before loading app
 *       }
 *     }
 *
 * Properties: The application can be configured using the following properties:
 * 
 *      // Display a native loading dialog when loading app.  Format for value = "Title,Message".  
 *      // (String - default=null)
 *      super.setStringProperty("loadingDialog", "Wait,Loading Demo...");
 * 
 *      // Display a native loading dialog when loading sub-pages.  Format for value = "Title,Message".  
 *      // (String - default=null)
 *      super.setStringProperty("loadingPageDialog", "Loading page...");
 *  
 *      // Load a splash screen image from the resource drawable directory.
 *      // (Integer - default=0)
 *      super.setIntegerProperty("splashscreen", R.drawable.splash);
 *
 *      // Set the background color.
 *      // (Integer - default=0 or BLACK)
 *      super.setIntegerProperty("backgroundColor", Color.WHITE);
 * 
 *      // Time in msec to wait before triggering a timeout error when loading
 *      // with super.loadUrl().  (Integer - default=20000)
 *      super.setIntegerProperty("loadUrlTimeoutValue", 60000);
 * 
 *      // URL to load if there's an error loading specified URL with loadUrl().  
 *      // Should be a local URL starting with file://. (String - default=null)
 *      super.setStringProperty("errorUrl", "file:///android_asset/www/error.html");
 * 
 *      // Enable app to keep running in background. (Boolean - default=true)
 *      super.setBooleanProperty("keepRunning", false);
 *      
 * Cordova.xml configuration:
 *      Cordova uses a configuration file at res/xml/cordova.xml to specify the following settings.
 *      
 *      Approved list of URLs that can be loaded into DroidGap
 *          <access origin="http://server regexp" subdomains="true" />
 *      Log level: ERROR, WARN, INFO, DEBUG, VERBOSE (default=ERROR)
 *          <log level="DEBUG" />
 *
 * Cordova plugins:
 *      Cordova uses a file at res/xml/plugins.xml to list all plugins that are installed.
 *      Before using a new plugin, a new element must be added to the file.
 *          name attribute is the service name passed to Cordova.exec() in JavaScript
 *          value attribute is the Java class name to call.
 *      
 *      <plugins>
 *          <plugin name="App" value="org.apache.cordova.App"/>
 *          ...
 *      </plugins>
 */
public class DroidGap extends Activity {

    CordovaWebView appView;
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
    private PreferenceSet preferences;
    private ArrayList<Pattern> whiteList = new ArrayList<Pattern>();
    private String TAG = "Cordova";

    
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        preferences = new PreferenceSet();

        // Load Cordova configuration:
        //      white list of allowed URLs
        //      debug setting
        this.loadConfiguration();

        if (preferences.prefMatches("fullscreen","true")) {
          getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                  WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
          getWindow().setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN,
                  WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        }
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        if (preferences.prefMatches("fullscreen","true")) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        }

        // This builds the view.  We could probably get away with NOT having a LinearLayout, but I like having a bucket!
        Display display = getWindowManager().getDefaultDisplay(); 
        int width = display.getWidth();
        int height = display.getHeight();
        
        root = new LinearLayoutSoftKeyboardDetect(this, width, height);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(this.backgroundColor);
        root.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 
                ViewGroup.LayoutParams.FILL_PARENT, 0.0F));
        
        appView = new CordovaWebView(this);

        root.addView(appView);
        
        setContentView(root);
        appView.init();
        pluginManager = appView.appCode.pluginManager;
    }
    
    /**
     * Load PhoneGap configuration from res/xml/phonegap.xml.
     * Approved list of URLs that can be loaded into DroidGap
     *    <access origin="http://server regexp" subdomains="true" />
     * Log level: ERROR, WARN, INFO, DEBUG, VERBOSE (default=ERROR)
     *      <log level="DEBUG" />
     */
    private void loadConfiguration() {
        int id = getResources().getIdentifier("phonegap", "xml", getPackageName());
        if (id == 0) {
            LOG.i("PhoneGapLog", "phonegap.xml missing. Ignoring...");
            return;
        }
        XmlResourceParser xml = getResources().getXml(id);
        int eventType = -1;
        while (eventType != XmlResourceParser.END_DOCUMENT) {
            if (eventType == XmlResourceParser.START_TAG) {
                String strNode = xml.getName();
                if (strNode.equals("access")) {
                    String origin = xml.getAttributeValue(null, "origin");
                    String subdomains = xml.getAttributeValue(null, "subdomains");
                    if (origin != null) {
                        this.addWhiteListEntry(origin, (subdomains != null) && (subdomains.compareToIgnoreCase("true") == 0));
                    }
                }
                else if (strNode.equals("log")) {
                    String level = xml.getAttributeValue(null, "level");
                    LOG.i("PhoneGapLog", "Found log level %s", level);
                    if (level != null) {
                        LOG.setLogLevel(level);
                    }
                }
               else if(strNode.equals("render")) {
                    String enabled = xml.getAttributeValue(null, "enabled");
                    if(enabled != null)
                    {
                        //this.classicRender = enabled.equals("true");
                    }
                }
                else if (strNode.equals("preference")) {
                    String name = xml.getAttributeValue(null, "name");
                    String value = xml.getAttributeValue(null, "value");
                    String readonlyString = xml.getAttributeValue(null, "readonly");

                    boolean readonly = (readonlyString != null &&
                                        readonlyString.equals("true"));

                    LOG.i("PhoneGapLog", "Found preference for %s", name);
                    preferences.add(new PreferenceNode(name, value, readonly));
                }
            }
            try {
                eventType = xml.next();
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    

    /**
     * Add entry to approved list of URLs (whitelist)
     * 
     * @param origin        URL regular expression to allow
     * @param subdomains    T=include all subdomains under origin
     */
    private void addWhiteListEntry(String origin, boolean subdomains) {
      try {
        // Unlimited access to network resources
        if(origin.compareTo("*") == 0) {
            LOG.d(TAG , "Unlimited access to network resources");
            whiteList.add(Pattern.compile(".*"));
        } else { // specific access
          // check if subdomains should be included
          // TODO: we should not add more domains if * has already been added
          if (subdomains) {
              // XXX making it stupid friendly for people who forget to include protocol/SSL
              if(origin.startsWith("http")) {
                whiteList.add(Pattern.compile(origin.replaceFirst("https?://", "^https?://(.*\\.)?")));
              } else {
                whiteList.add(Pattern.compile("^https?://(.*\\.)?"+origin));
              }
              LOG.d(TAG, "Origin to allow with subdomains: %s", origin);
          } else {
              // XXX making it stupid friendly for people who forget to include protocol/SSL
              if(origin.startsWith("http")) {
                whiteList.add(Pattern.compile(origin.replaceFirst("https?://", "^https?://")));
              } else {
                whiteList.add(Pattern.compile("^https?://"+origin));
              }
              LOG.d(TAG, "Origin to allow: %s", origin);
          }    
        }
      } catch(Exception e) {
        LOG.d(TAG, "Failed to add origin %s", origin);
      }
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
       // Send pause event to JavaScript
        if (this.appView == null) {
            return;
        }

        // Send pause event to JavaScript
        this.appView.loadUrl("javascript:try{require('cordova/channel').onPause.fire();}catch(e){console.log('exception firing pause event from native');};");

        // Forward to plugins
        this.pluginManager.onPause(this.keepRunning);

        // If app doesn't want to run in background
        if (!this.keepRunning) {

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
        this.appView.loadUrl("javascript:try{require('cordova/channel').onResume.fire();}catch(e){console.log('exception firing resume event from native');};");

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
    
    /*
     * This allows us to move assets out of the android_assets directory into the jail.  The main benefit of this
     * is that we can then access interactive elements, and also not have to worry about our application breaking due to 
     * poor input handling by the Android API.
     */
    
    public boolean areAssetsInJail()
    {
        String jailPath = "/data/data/" + this.getPackageName() + "/www-data/";
        File f = new File(jailPath);
        return f.exists();
    }
    
    public void moveAssetsToJail()
    {
        ArrayList<String> fileList = new ArrayList<String>();
        AssetManager myAssets = this.getAssets();
        String jailPath = "/data/data/" + this.getPackageName() + "/www-data/";
        String [] files = null;
        try {
           files = myAssets.list("");
           for(String filename : files)
           {
               fileList.add(filename);
           }
        }
        catch (IOException e)
        {
            //return fail;
            LOG.d("CordoaActivity", "Unable to find assets");
        }
        for(int i = 0; i < fileList.size(); ++i)
        {
            String filename = fileList.get(i);
            InputStream in = null;
            OutputStream out = null;
            try
            {
                String fullPath = jailPath + filename;
                File testFile = new File(fullPath);
                boolean test = testFile.mkdirs();
                if(testFile.isDirectory())
                {
                    String [] childFiles = myAssets.list(filename);
                    for(String childName : childFiles)
                    {
                        fileList.add(filename + "/" + childName);
                    }
                }
                else
                {
                    in = myAssets.open(filename);
                    out = new FileOutputStream(fullPath);
                    copyFile(in, out);
                }
            }
            catch (IOException e)
            {
                LOG.d("CordovaActivity", "Unable to copy files");
            }
        }
    }
    
    
    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        in.close();
        in = null;
        out.flush();
        out.close();
        out = null;
    }

    @Override
    /**
     * The final call you receive before your activity is destroyed. 
     */
    public void onDestroy() {
        super.onDestroy();
        
        if (this.appView != null) {
            // Send destroy event to JavaScript
            this.appView.loadUrl("javascript:try{require('cordova/channel').onDestroy.fire();}catch(e){console.log('exception firing destroy event from native');};");

            // Load blank page so that JavaScript onunload is called
            this.appView.loadUrl("about:blank");

            // Forward to plugins
            this.pluginManager.onDestroy();
        }
        else {
            this.endActivity();
        }
    }
    
    public void loadJailedFile(String file)
    {
        String jailPath = "/data/data/" + this.getPackageName() + "/www-data/" + file;
        appView.loadUrlIntoView("file://" + jailPath);

    }
    
    public void loadJailedFile(String file, int time)
    {
        String jailPath = "/data/data/" + this.getPackageName() + "/www-data/" + file;
        appView.loadUrlIntoView("file://" + jailPath, time);
    }
    
    public void loadUrl(String url)
    {
        appView.loadUrlIntoView(url);
    }
    
    public void loadUrl(String url, int time)
    {
        appView.loadUrlIntoView(url, time);
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
                this.appView.loadUrl("javascript:require('cordova').fireDocumentEvent('backbutton');");
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
            this.appView.loadUrl("javascript:require('cordova').fireDocumentEvent('menubutton');");
            return super.onKeyDown(keyCode, event);
        }

        // If search key
        else if (keyCode == KeyEvent.KEYCODE_SEARCH) {
            this.appView.loadUrl("javascript:require('cordova').fireDocumentEvent('searchbutton');");
            return true;
        }
        return activityResultKeepRunning;
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

    public void setActivityResultCallback(IPlugin plugin) {
         this.activityResultCallback = plugin;
     }

     /**
      * Report an error to the host application. These errors are unrecoverable (i.e. the main resource is unavailable). 
      * The errorCode parameter corresponds to one of the ERROR_* constants.
      *
      * @param errorCode    The error code corresponding to an ERROR_* value.
      * @param description  A String describing the error.
      * @param failingUrl   The url that failed to load. 
      */
     public void onReceivedError(final int errorCode, final String description, final String failingUrl) {
         final DroidGap me = this;

         // If errorUrl specified, then load it
         final String errorUrl = me.getStringProperty("errorUrl", null);
         if ((errorUrl != null) && (!failingUrl.equals(errorUrl))) {

             // Load URL on UI thread
             me.runOnUiThread(new Runnable() {
                 public void run() {
                     me.appView.loadUrl(errorUrl); 
                 }
             });
         }
         // If not, then display error dialog
         else {
             me.runOnUiThread(new Runnable() {
                 public void run() {
                     me.appView.setVisibility(View.GONE);
                     me.displayError("Application Error", description + " ("+failingUrl+")", "OK", true);
                 }
             });
         }
     }
     /**
      * Display an error dialog and optionally exit application.
      * 
      * @param title
      * @param message
      * @param button
      * @param exit
      */
     public void displayError(final String title, final String message, final String button, final boolean exit) {
         final DroidGap me = this;
         me.runOnUiThread(new Runnable() {
             public void run() {
                 AlertDialog.Builder dlg = new AlertDialog.Builder(me);
                 dlg.setMessage(message);
                 dlg.setTitle(title);
                 dlg.setCancelable(false);
                 dlg.setPositiveButton(button,
                         new AlertDialog.OnClickListener() {
                     public void onClick(DialogInterface dialog, int which) {
                         dialog.dismiss();
                         if (exit) {
                             me.endActivity();
                         }
                     }
                 });
                 dlg.create();
                 dlg.show();
             }
         });
     }

    protected void sendJavascript(String statement) {
        appView.appCode.sendJavascript(statement);
    }

}

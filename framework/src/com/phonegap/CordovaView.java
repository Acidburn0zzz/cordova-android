package com.phonegap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xmlpull.v1.XmlPullParserException;

import com.phonegap.api.LOG;
import com.phonegap.api.PluginManager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.XmlResourceParser;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebSettings.LayoutAlgorithm;


public class CordovaView extends WebView {

    private static final String TAG = null;
    GapClient appCode;
    CordovaClient viewClient;
    Activity app;
    private boolean classicRender;
    private ArrayList<Pattern> whiteList = new ArrayList<Pattern>();
    private HashMap<String, Boolean> whiteListCache = new HashMap<String,Boolean>();
    String url = null;
    Stack<String> urls = new Stack<String>();
    private String initUrl;
    private Object baseUrl;
    private boolean cancelLoadUrl;
    protected long loadUrlTimeoutValue = 20000;
    protected int loadUrlTimeout = 0;

    
    public CordovaView(Context context)
    {
        super(context);
        app = (Activity) context;
        //Set the view as invisible when we create it, then bring it out when we load the URL
        this.setVisibility(View.INVISIBLE);
    }
    
    public CordovaView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        app = (Activity) context;
        init();
    }
    
    public CordovaView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        app = (Activity) context;
        init();
    }
    
    public CordovaView(Context context, AttributeSet attrs, int defStyle,
            boolean privateBrowsing) {
        super(context, attrs, defStyle, privateBrowsing);
        init();
    }
    
    public GapClient getGapClient()
    {
        return appCode;
    }
    
    public CordovaClient getCordovaClient()
    {
        return viewClient;
    }
    
    public void onPause()
    {
        appCode.pluginManager.onPause(true);
    }
    
    public void onResume()
    {
        appCode.pluginManager.onResume(true);
    }
    
    public void sendJavascript(String command)
    {
        appCode.sendJavascript(command);
    }
    
    public void onDestroy()
    {
        appCode.onDestroy();
    }
    
    @SuppressWarnings("deprecation")
    public void init()
    {
        setInitialScale(0);
        setVerticalScrollBarEnabled(false);
        requestFocusFromTouch();
        // Enable JavaScript
        WebSettings settings = getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setLayoutAlgorithm(LayoutAlgorithm.NORMAL);
        
        //Set the nav dump for HTC
        settings.setNavDump(true);

        // Enable database
        settings.setDatabaseEnabled(true);
        String databasePath = getContext().getDir("database", Context.MODE_PRIVATE).getPath(); 
        settings.setDatabasePath(databasePath);

        // Enable DOM storage
        WebViewReflect.setDomStorage(settings);
        
        // Enable built-in geolocation
        WebViewReflect.setGeolocationEnabled(settings, true);
        
        //Initalize the other parts of the application
        appCode = new GapClient(this, this.getContext());
        viewClient = new CordovaClient(app, this);
        
    }

    
    private void loadConfiguration() {
        int id = app.getResources().getIdentifier("phonegap", "xml", app.getPackageName());
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
                        this.classicRender = enabled.equals("true");
                    }
                    
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
    public void addWhiteListEntry(String origin, boolean subdomains) {
      try {
        // Unlimited access to network resources
        if(origin.compareTo("*") == 0) {
            LOG.d(TAG, "Unlimited access to network resources");
            whiteList.add(Pattern.compile("*"));
        } else { // specific access
          // check if subdomains should be included
          // TODO: we should not add more domains if * has already been added
          if (subdomains) {
              // XXX making it stupid friendly for people who forget to include protocol/SSL
              if(origin.startsWith("http")) {
                whiteList.add(Pattern.compile(origin.replaceFirst("https{0,1}://", "^https{0,1}://.*")));
              } else {
                whiteList.add(Pattern.compile("^https{0,1}://.*"+origin));
              }
              LOG.d(TAG, "Origin to allow with subdomains: %s", origin);
          } else {
              // XXX making it stupid friendly for people who forget to include protocol/SSL
              if(origin.startsWith("http")) {
                whiteList.add(Pattern.compile(origin.replaceFirst("https{0,1}://", "^https{0,1}://")));
              } else {
                whiteList.add(Pattern.compile("^https{0,1}://"+origin));
              }
              LOG.d(TAG, "Origin to allow: %s", origin);
          }    
        }
      } catch(Exception e) {
        LOG.d(TAG, "Failed to add origin %s", origin);
      }
    }
    

    /**
     * Determine if URL is in approved list of URLs to load.
     * 
     * @param url
     * @return
     */
    private boolean isUrlWhiteListed(String url) {

        // Check to see if we have matched url previously
        if (whiteListCache.get(url) != null) {
            return true;
        }

        // Look for match in white list
        Iterator<Pattern> pit = whiteList.iterator();
        while (pit.hasNext()) {
            Pattern p = pit.next();
            Matcher m = p.matcher(url);

            // If match found, then cache it to speed up subsequent comparisons
            if (m.find()) {
                whiteListCache.put(url, true);
                return true;
            }
        }
        return false;
    }

    public PluginManager getPluginManager() {
        // TODO Auto-generated method stub
        return appCode.pluginManager;
    }
    
    /**
     * Go to previous page in history.  (We manage our own history)
     * 
     * @return true if we went back, false if we are already at top
     */
    public boolean backHistory() {

        // Check webview first to see if there is a history
        // This is needed to support curPage#diffLink, since they are added to appView's history, but not our history url array (JQMobile behavior)
        if (this.canGoBack()) {
            this.goBack();  
            return true;
        }

        // If our managed history has prev url
        if (this.urls.size() > 1) {
            this.urls.pop();                // Pop current url
            String url = this.urls.pop();   // Pop prev url that we want to load, since it will be added back by loadUrl()
            this.loadUrl(url);
            return true;
        }
        
        return false;
    }

    public boolean checkBackKey() {
        // TODO Auto-generated method stub
        return appCode.isBackButtonBound();
    }
    
    /**
     * Load the url into the webview after waiting for period of time.
     * This is used to display the splashscreen for certain amount of time.
     * 
     * @param url
     * @param time              The number of ms to wait before loading webview
     */
    public void loadUrl(final String url, int time) {
        
        // If first page of app, then set URL to load to be the one passed in
        if (this.initUrl == null || (this.urls.size() > 0)) {
            this.loadUrlIntoView(url, time);
        }
        // Otherwise use the URL specified in the activity's extras bundle
        else {
            this.loadUrlIntoView(this.initUrl);
        }
    }
    

    /**
     * Load the url into the webview after waiting for period of time.
     * This is used to display the splashscreen for certain amount of time.
     * 
     * @param url
     * @param time              The number of ms to wait before loading webview
     */
    public void loadUrlIntoView(final String url, final int time) {
        // Clear cancel flag
        this.cancelLoadUrl = false;
        
        // If not first page of app, then load immediately
        if (this.urls.size() > 0) {
            this.loadUrl(url);
        }
        
        if (!url.startsWith("javascript:")) {
            LOG.d(TAG, "DroidGap.loadUrl(%s, %d)", url, time);
        }
        final CordovaView me = this;

        // Handle activity parameters if we're using the activity!
        app.runOnUiThread(new Runnable() {
            public void run() {
                String className = app.getClass().getSuperclass().getName();
                if(className.contains("CordovaActivity"))
                    ((CordovaActivity)app).handleActivityParameters();
            }
        });

        Runnable runnable = new Runnable() {
            public void run() {
                try {
                    synchronized(this) {
                        this.wait(time);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (!me.cancelLoadUrl) {
                    me.loadUrl(url);
                }
                else{
                    me.cancelLoadUrl = false;
                    LOG.d(TAG, "Aborting loadUrl(%s): Another URL was loaded before timer expired.", url);
                }
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }
    
    /**
     * Load the url into the webview.
     * 
     * @param url
     */
    public void loadUrlIntoView(final String url) {
        if (!url.startsWith("javascript:")) {
            LOG.d(TAG, "DroidGap.loadUrl(%s)", url);
        }

        this.url = url;
        if (this.baseUrl == null) {
            int i = url.lastIndexOf('/');
            if (i > 0) {
                this.baseUrl = url.substring(0, i+1);
            }
            else {
                this.baseUrl = this.url + "/";
            }
        }
        if (!url.startsWith("javascript:")) {
            LOG.d(TAG, "DroidGap: url=%s baseUrl=%s", url, baseUrl);
        }
        
        // Load URL on UI thread
        final CordovaView me = this;
        app.runOnUiThread(new Runnable() {
            public void run() {
                
                // Handle activity parameters
                // Track URLs loaded instead of using appView history
                me.clearHistory();
                
                String className = app.getClass().getSuperclass().getName();
                if(className.contains("Cordova"))
                {
                    CordovaActivity properApp = (CordovaActivity) app;
                    properApp.handleActivityParameters();
                    
                    // If loadingDialog property, then show the App loading dialog for first page of app
                    String loading = null;
                    if (me.urls.size() == 1) {
                        loading = properApp.getStringProperty("loadingDialog", null);
                    }
                    else {
                        loading = properApp.getStringProperty("loadingPageDialog", null);
                    }
                    if (loading != null) {

                        String title = "";
                        String message = "Loading Application...";

                        if (loading.length() > 0) {
                            int comma = loading.indexOf(',');
                            if (comma > 0) {
                                title = loading.substring(0, comma);
                                message = loading.substring(comma+1);
                            }
                            else {
                                title = "";
                                message = loading;
                            }
                        }
                        properApp.spinnerStart(title, message);
                    }
                }

                // Create a timeout timer for loadUrl
                final int currentLoadUrlTimeout = me.loadUrlTimeout;
                Runnable runnable = new Runnable() {
                    public void run() {
                        try {
                            synchronized(this) {
                                wait(me.loadUrlTimeoutValue);
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        // If timeout, then stop loading and handle error
                        if (me.loadUrlTimeout == currentLoadUrlTimeout) {
                            me.stopLoading();
                            LOG.e(TAG, "DroidGap: TIMEOUT ERROR! - calling webViewClient");
                            me.viewClient.onReceivedError(me, -6, "The connection to the server was unsuccessful.", url);
                        }
                    }
                };
                Thread thread = new Thread(runnable);
                thread.start();
                me.loadUrl(url);
            }
        });
    }
    
    @Override
    public void clearHistory()
    {
        super.clearHistory();
        this.urls.clear();
        
        // Leave current url on history stack
        if (this.url != null) {
            this.urls.push(this.url);
        }
        
    }
    

    /**
     * Load the specified URL in the PhoneGap webview or a new browser instance.
     * 
     * NOTE: If openExternal is false, only URLs listed in whitelist can be loaded.
     *
     * @param url           The url to load.
     * @param openExternal  Load url in browser instead of PhoneGap webview.
     * @param clearHistory  Clear the history stack, so new page becomes top of history
     * @param params        DroidGap parameters for new app
     */
    public void showWebPage(String url, boolean openExternal, boolean clearHistory, HashMap<String, Object> params) { //throws android.content.ActivityNotFoundException {
        LOG.d(TAG, "showWebPage(%s, %b, %b, HashMap", url, openExternal, clearHistory);
        
        // If clearing history
        if (clearHistory) {
            this.clearHistory();
        }
        
        // If loading into our webview
        if (!openExternal) {
            
            // Make sure url is in whitelist
            if (url.startsWith("file://") || isUrlWhiteListed(url)) {
                // TODO: What about params?
                
                // Clear out current url from history, since it will be replacing it
                if (clearHistory) {
                    this.urls.clear();
                }
                
                // Load new URL
                this.loadUrlIntoView(url);
            }
            // Load in default viewer if not
            else {
                LOG.w(TAG, "showWebPage: Cannot load URL into webview since it is not in white list.  Loading into browser instead. (URL="+url+")");
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    app.startActivity(intent);
                } catch (android.content.ActivityNotFoundException e) {
                    LOG.e(TAG, "Error loading url "+url, e);
                }
            }
        }
        
        // Load in default view intent
        else {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                app.startActivity(intent);
            } catch (android.content.ActivityNotFoundException e) {
                LOG.e(TAG, "Error loading url "+url, e);
            }
        }
    }

    public void cancelLoadUrl() {
        // TODO Auto-generated method stub
        this.cancelLoadUrl = true;
    }

    public void postMessage(String id, Object data) {
        appCode.pluginManager.postMessage(id, data);
    }
}

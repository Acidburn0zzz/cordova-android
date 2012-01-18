package com.phonegap;

import org.openqa.selenium.android.library.WebViewFactory;

import android.app.Activity;
import android.webkit.WebView;

public class CordovaViewFactory implements WebViewFactory {
    
    public WebView createNewView(Activity arg0) {
        // TODO Auto-generated method stub
        return new CordovaView(arg0);
    }

}

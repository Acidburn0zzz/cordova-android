package com.phonegap.test.activities;

import com.phonegap.CordovaView;
import com.phonegap.R;

import android.app.Activity;
import android.os.Bundle;

public class PhoneGapViewTestActivity extends Activity {
    
    CordovaView phoneGap;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        phoneGap = (CordovaView) findViewById(R.id.phoneGapView);
        phoneGap.loadUrl("file:///android_asset/index.html");
    }
    
    public void onDestroy()
    {
        super.onDestroy();
        phoneGap.onDestroy();
    }
}
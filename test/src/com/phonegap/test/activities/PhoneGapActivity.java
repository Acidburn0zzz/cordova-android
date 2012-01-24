package com.phonegap.test.activities;

import com.phonegap.CordovaActivity;

import android.app.Activity;
import android.os.Bundle;

public class PhoneGapActivity extends CordovaActivity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.loadUrl("file:///android_asset/index.html");
    }
}

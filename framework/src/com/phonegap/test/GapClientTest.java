package com.phonegap.test;

import com.phonegap.CordovaWebView;
import com.phonegap.CordovaChromeClient;
import com.phonegap.api.PluginManager;
import com.phonegap.test.activities.PhoneGapViewTestActivity;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public class GapClientTest extends ActivityInstrumentationTestCase2<PhoneGapViewTestActivity> {
	
	private PhoneGapViewTestActivity testActivity;
	private FrameLayout containerView;
	private LinearLayout innerContainer;
	private View testView;
	private String rString;
	private CordovaChromeClient appCode;

	public GapClientTest() {
		super("com.phonegap.test.activities",PhoneGapViewTestActivity.class);
	}
	
	protected void setUp() throws Exception{
		super.setUp();
		testActivity = this.getActivity();
		containerView = (FrameLayout) testActivity.findViewById(android.R.id.content);
		innerContainer = (LinearLayout) containerView.getChildAt(0);
		testView = innerContainer.getChildAt(0);
		appCode = ((CordovaWebView) testView).getGapClient();
		
	}
	
	public void testPreconditions(){
	    assertNotNull(innerContainer);
		assertNotNull(testView);
	}
	
	public void testForCordovaView() {
	    String className = testView.getClass().getSimpleName();
	    assertTrue(className.equals("CordovaView"));
	}
	
	public void testGetResources() {
	    Resources ls = testActivity.getResources();
	    Resources rs = appCode.getResources();
	    assertTrue(ls.equals(rs));
	}
	
	public void testGetPackageName() {
	    String ls = testActivity.getPackageName();
	    String rs = appCode.getPackageName();
	    assertTrue(ls.equals(rs));
	}
	
	public void testGetAssets() {
	    AssetManager ls = testActivity.getAssets();
	    AssetManager rs = testActivity.getAssets();
	    assertTrue(ls.equals(rs));
	}
	
	public void testGetBaseContext() {
	    Context ls = testActivity.getBaseContext();
	    Context rs = testActivity.getBaseContext();
	    assertTrue(ls.equals(rs));
	}
}

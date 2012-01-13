package com.phonegap.test;

import com.phonegap.CordovaView;
import com.phonegap.api.PluginManager;
import com.phonegap.test.activities.PhoneGapViewTestActivity;
import com.phonegap.R;

import android.test.ActivityInstrumentationTestCase2;
import android.view.View;

public class CordovaTest extends ActivityInstrumentationTestCase2<PhoneGapViewTestActivity> {
	
	private static final long TIMEOUT = 1000;
    private PhoneGapViewTestActivity testActivity;
	private View testView;
	private String rString;

	public CordovaTest() {
		super("com.phonegap.test.activities",PhoneGapViewTestActivity.class);
	}
	
	protected void setUp() throws Exception{
		super.setUp();
		testActivity = this.getActivity();
		testView = testActivity.findViewById(R.id.phoneGapView);
	}
	
	public void testPreconditions(){
		assertNotNull(testView);
	}
	
	public void testForCordovaView() {
	    String className = testView.getClass().getSimpleName();
	    assertTrue(className.equals("CordovaView"));
	}
	
	public void testForPluginManager() {
	    CordovaView v = (CordovaView) testView;
	    PluginManager p = v.getPluginManager();
	    assertNotNull(p);
	    String className = p.getClass().getSimpleName();
	    assertTrue(className.equals("PluginManager"));
	}

	public void testBackButton() {
	    CordovaView v = (CordovaView) testView;
	    assertFalse(v.checkBackKey());
	}
	
	public void testLoadUrl() {
	    CordovaView v = (CordovaView) testView;
	    v.loadUrlIntoView("file:///android_asset/index.html");
	    sleep();
	    String url = v.getUrl();
	    assertTrue(url.equals("file:///android_asset/index.html"));
	    int visible = v.getVisibility();
	    assertTrue(visible == View.VISIBLE);
	}
	
	public void testBackHistoryFalse() {
	    this.testLoadUrl();
        CordovaView v = (CordovaView) testView;
        //Move back in the history
        boolean test = v.backHistory();
        assertFalse(test);
	}
	
	//Make sure that we can go back
	public void testBackHistoryTrue()
	{
	    this.testLoadUrl();
        CordovaView v = (CordovaView) testView;
        v.loadUrlIntoView("file:///android_asset/compass/index.html");
        sleep();
        String url = v.getUrl();
        assertTrue(url.equals("file:///android_asset/compass/index.html"));
        //Move back in the history
        boolean test = v.backHistory();
        assertTrue(test);
        sleep();
        url = v.getUrl();
        assertTrue(url.equals("file:///android_asset/index.html"));
	}
	
	private void sleep() {
	    try {
            Thread.sleep(TIMEOUT);
        } catch (InterruptedException e) {
            fail("Unexpected Timeout");
        }
	}
}

package com.phonegap.test;

import com.phonegap.CordovaWebView;
import com.phonegap.api.PluginManager;
import com.phonegap.test.R;

import android.test.ActivityInstrumentationTestCase2;
import android.view.View;

public class CordovaTest extends
    ActivityInstrumentationTestCase2<PhoneGapViewTestActivity> {

  private static final long TIMEOUT = 1000;
  private PhoneGapViewTestActivity testActivity;
  private View testView;
  private String rString;

  public CordovaTest() {
    super("com.phonegap.test.activities", PhoneGapViewTestActivity.class);
  }

  protected void setUp() throws Exception {
    super.setUp();
    testActivity = this.getActivity();
    testView = testActivity.findViewById(R.id.phoneGapView);
  }

  public void testPreconditions() {
    assertNotNull(testView);
  }

  public void testForCordovaView() {
    String className = testView.getClass().getSimpleName();
    assertTrue(className.equals("CordovaWebView"));
  }

  public void testForPluginManager() {
    CordovaWebView v = (CordovaWebView) testView;
    PluginManager p = v.getPluginManager();
    assertNotNull(p);
    String className = p.getClass().getSimpleName();
    assertTrue(className.equals("PluginManager"));
  }

  public void testBackButton() {
    CordovaWebView v = (CordovaWebView) testView;
    assertFalse(v.checkBackKey());
  }

  public void testLoadUrl() {
    CordovaWebView v = (CordovaWebView) testView;
    v.loadUrlIntoView("file:///android_asset/www/index.html");
    sleep();
    String url = v.getUrl();
    boolean result = url.equals("file:///android_asset/www/index.html");
    assertTrue(result);
    int visible = v.getVisibility();
    assertTrue(visible == View.VISIBLE);
  }

  public void testBackHistoryFalse() {
    this.testLoadUrl();
    CordovaWebView v = (CordovaWebView) testView;
    // Move back in the history
    boolean test = v.backHistory();
    assertFalse(test);
  }

  // Make sure that we can go back
  public void testBackHistoryTrue() {
    this.testLoadUrl();
    CordovaWebView v = (CordovaWebView) testView;
    v.loadUrlIntoView("file:///android_asset/compass/index.html");
    sleep();
    String url = v.getUrl();
    assertTrue(url.equals("file:///android_asset/compass/index.html"));
    // Move back in the history
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

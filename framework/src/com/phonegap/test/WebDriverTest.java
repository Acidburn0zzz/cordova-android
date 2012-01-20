package com.phonegap.test;

import com.phonegap.CordovaClient;
import com.phonegap.CordovaView;
import com.phonegap.GapClient;
import com.phonegap.api.PluginManager;
import com.phonegap.test.activities.CordovaDriverAction;
import com.phonegap.R;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.android.library.AndroidWebDriver;

import android.test.ActivityInstrumentationTestCase2;
import android.view.View;

public class WebDriverTest extends ActivityInstrumentationTestCase2<CordovaDriverAction> {
	
	private static final long TIMEOUT = 1000;
    private CordovaDriverAction testActivity;
	private CordovaView testView;
    private CordovaViewFactory viewFactory;
    private GapClient appCode;
    private CordovaClient viewHandler;
    private AndroidWebDriver testDriver;

	public WebDriverTest() {
		super("com.phonegap.test.activities",CordovaDriverAction.class);
	}
	
	protected void setUp() throws Exception{
		super.setUp();
		testActivity = this.getActivity();
		viewFactory = new CordovaViewFactory();
		appCode = new GapClient(testActivity);
		viewHandler = new CordovaClient(testActivity);
		testDriver = new AndroidWebDriver(testActivity, viewFactory, viewHandler, appCode);
		testView = (CordovaView) testDriver.getWebView();
		viewHandler.setCordovaView(testView);
		appCode.testInit(testView);
	}
	
	public void testPreconditions(){
		assertNotNull(testView);
	}
	
	public void testWebLoad() {
	    testDriver.get("file:///android_asset/www/index.html");
	    sleep();
	    String url = testView.getUrl();
	    //Check the sanity!
	    assertTrue(url.equals("file:///android_asset/www/index.html"));
	    WebElement platformSpan = testDriver.findElement(By.id("platform"));
	    String text = platformSpan.getText();
	    assertTrue(text.equals("Android"));
	}
	
	
	private void sleep() {
	    try {
            Thread.sleep(TIMEOUT);
        } catch (InterruptedException e) {
            fail("Unexpected Timeout");
        }
	}
}

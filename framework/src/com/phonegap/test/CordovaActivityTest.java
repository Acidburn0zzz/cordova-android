package com.phonegap.test;

import com.phonegap.CordovaView;
import com.phonegap.api.PluginManager;
import com.phonegap.test.activities.PhoneGapActivity;

import android.test.ActivityInstrumentationTestCase2;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public class CordovaActivityTest extends ActivityInstrumentationTestCase2<PhoneGapActivity> {

    private PhoneGapActivity testActivity;
    private FrameLayout containerView;
    private LinearLayout innerContainer;
    private CordovaView testView;
    
    public CordovaActivityTest()
    {
        super("com.phonegap.test.activities",PhoneGapActivity.class);
    }
    
    protected void setUp() throws Exception {
        super.setUp();
        testActivity = this.getActivity();
        containerView = (FrameLayout) testActivity.findViewById(android.R.id.content);
        innerContainer = (LinearLayout) containerView.getChildAt(0);
        testView = (CordovaView) innerContainer.getChildAt(0);
        
    }
    
    public void testPreconditions(){
        assertNotNull(innerContainer);
        assertNotNull(testView);
    }
    

    public void testForCordovaView() {
        String className = testView.getClass().getSimpleName();
        assertTrue(className.equals("CordovaView"));
    }
    
    public void testForLinearLayout() {
        String className = innerContainer.getClass().getSimpleName();
        assertTrue(className.equals("LinearLayoutSoftKeyboardDetect"));
    }
    
    public void testForPluginManager() {
        CordovaView v = (CordovaView) testView;
        PluginManager p = v.getPluginManager();
        assertNotNull(p);
        String className = p.getClass().getSimpleName();
        assertTrue(className.equals("PluginManager"));
    }

}

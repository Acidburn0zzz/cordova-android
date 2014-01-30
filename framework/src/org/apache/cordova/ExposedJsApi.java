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

import android.webkit.JavascriptInterface;
import org.apache.cordova.PluginManager;
import org.json.JSONException;

/**
 * Contains APIs that the JS can call. All functions in here should also have
 * an equivalent entry in CordovaChromeClient.java, and be added to
 * cordova-js/lib/android/plugin/android/promptbasednativeapi.js
 */
/* package */ class ExposedJsApi {
    
    private PluginManager pluginManager;
    private NativeToJsMessageQueue jsMessageQueue;
    private static String TAG = "ExposedJsApi";
    
    public ExposedJsApi(PluginManager pluginManager, NativeToJsMessageQueue jsMessageQueue) {
        this.pluginManager = pluginManager;
        this.jsMessageQueue = jsMessageQueue;
    }

    @JavascriptInterface
    public String exec(String service, String action, String callbackId, String arguments, String secureToken) throws JSONException {        // If the arguments weren't received, send a message back to JS.  It will switch bridge modes and try again.  See CB-2666.
        // We send a message meant specifically for this case.  It starts with "@" so no other message can be encoded into the same string.
        if (arguments == null) {
            return "@Null arguments.";
        }

        jsMessageQueue.setPaused(true);
        
        
        
        // Get the CapabilityManagerImpl and make sure that the SecureToken is correct.
        if(!checkToken(secureToken)) {
            LOG.e(TAG, secureToken + " is NOT the correct secure token!");
            jsMessageQueue.setPaused(false);
            return null;
        }
        
        try {
            // Tell the resourceApi what thread the JS is running on.
            CordovaResourceApi.jsThread = Thread.currentThread();
            
            pluginManager.exec(service, action, callbackId, arguments);
            String ret = "";
            if (!NativeToJsMessageQueue.DISABLE_EXEC_CHAINING) {
                ret = jsMessageQueue.popAndEncode(false);
                NoFrakStore.add(secureToken, callbackId, ret);
            }
            return ret;
        } catch (Throwable e) {
            e.printStackTrace();
            return "";
        } finally {
            jsMessageQueue.setPaused(false);
        }
    }
    
    @JavascriptInterface
    public void setNativeToJsBridgeMode(String secureToken, int value) {
        if(!checkToken(secureToken)) {
             LOG.e(TAG, secureToken + " is NOT the correct secure token!");
             return;
        }
        
        //If we're executing this via addJavascriptInterface, we already lost!
        jsMessageQueue.setBridgeMode(value);
    }
    
    @JavascriptInterface
    public String retrieveJsMessages(String secureToken, boolean fromOnlineEvent) {
        if(checkToken(secureToken))
        {
            String msg =  jsMessageQueue.popAndEncode(fromOnlineEvent);
            NoFrakStore.putMsg(msg);
            String result = NoFrakStore.getMsg(secureToken);
            return result;
        }
        else
        {
            return "";
        }
    }
    
    boolean checkToken(String secureToken)
    {
        CapabilityManagerImpl capabilityManager = CapabilityManagerImpl.getInstance(null);
        return capabilityManager.isACorrectSecureToken(secureToken);
    }
}

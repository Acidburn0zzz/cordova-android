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

import android.util.Log;

/**
 * This class represents a single NoFrakEntryReturnResult.
 *
 * @author
 * @since 7/27/2013
 */
public class NoFrakEntryReturnResult {
    
    private String TAG = "NoFrakEntryReturnResult";
    
    private String reference;
    private String result;
    
    /**
     * CONSTRUCTOR
     *
     * This constructor represents a NoFrakEntryReturnResult.
     * @param reference : the reference id associated with the API call.
     * @param result : the result of the API call.
     */
    protected NoFrakEntryReturnResult(String reference, String result) {
        this.reference = reference;
        this.result = result;
        
        Log.d(TAG, "Reference=" + reference + " Result=" + result);
    }
    
    /**
     * Get the reference id associated with the API call.
     */
    protected String getReference() {
        return this.reference;
    }
    
    /**
     * Set the reference id associated with the API call.
     * @param reference : the reference id associated with the API call.
     */
    protected void setReference(String reference) {
        this.reference = reference;
    }
    
    /**
     * Get the result of the API call.
     */
    protected String getResult() {
        return this.result;
    }
    
    /**
     * Set the result of the API call.
     * @param result : the result of the API call.
     */
    protected void setResult(String result) {
        this.result = result;
    }
}
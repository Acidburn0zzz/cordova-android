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

import java.util.ArrayList;

import android.util.Log;

/**
 * This class represents a single NoFrakEntry.
 *
 * @author
 * @since 7/23/2013
 */
public class NoFrakEntry {
    
    private String TAG = "NoFrakEntry";
    
    private String origin;
    private String capability;
    private ArrayList<NoFrakEntryReturnResult> list;
 
    /**
     * CONSTRUCTOR
     *
     * This constructor represents a NoFrakEntry.
     *
     * @param origin : the origin
     * @param capability : the capability associated with the origin.
     */
    protected NoFrakEntry(String origin, String capability) {
        this.origin = origin;
        this.capability = capability;
        this.list = new ArrayList<NoFrakEntryReturnResult>();
        
        Log.d(TAG, "Origin=" + origin + " Capability=" + capability);
    }
    
    /**
     * Get the origin.
     */
    protected String getOrigin() {
        return this.origin;
    }
    
    /**
     * Set the origin.
     * @param origin : the origin.
     */
    protected void setOrigin(String origin) {
        this.origin = origin;
    }
    
    /**
     * Get the capability associated with the origin.
     */
    protected String getCapability() {
        return this.capability;
    }
    
    /**
     * Set the capability associated with the origin.
     * @param capability : the capability associated with the origin.
     */
    protected void setCapability(String capability) {
        this.capability = capability;
    }
    
    /**
     * Get the list of return results for this domain.
     */
    protected ArrayList<NoFrakEntryReturnResult> getResultList() {
        return this.list;
    }
    
    /**
     * Set the list of return results for this domain.
     * @param list : the list of return results for this domain.
     */
    protected void setResultList(ArrayList<NoFrakEntryReturnResult> list) {
        this.list = list;
    }
}
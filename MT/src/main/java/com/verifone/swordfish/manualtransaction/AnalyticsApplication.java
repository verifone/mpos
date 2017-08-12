/**
 * Copyright (C) 2016,2017 Verifone, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * VERIFONE, INC. BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Except as contained in this notice, the name of Verifone, Inc. shall not be
 * used in advertising or otherwise to promote the sale, use or other dealings
 * in this Software without prior written authorization from Verifone, Inc.
 */

package com.verifone.swordfish.manualtransaction;


import android.app.Application;
import android.util.Log;

import com.bugsee.library.Bugsee;
import com.flurry.android.FlurryAgent;
import com.flurry.android.FlurryAgentListener;

import java.util.HashMap;


/**
 * This is a subclass of {@link Application} used to provide shared objects for this app.
 */
public class AnalyticsApplication extends Application {

    private FlurryAgentListener listener;
    @Override
    public void onCreate() {
        super.onCreate();

        // Bugsee
        HashMap<String, Object> options = new HashMap<>();
        options.put(Bugsee.Option.VideoEnabled, false);
        Bugsee.launch(this, "30e05f57-d56d-4cb7-9c18-0cae1985f2bc", options);

        // Flurry
        new FlurryAgent.Builder()
                .withLogEnabled(true)
                .withCaptureUncaughtExceptions(true)
                .withLogEnabled(true)
                .withLogLevel(Log.VERBOSE)
                .build(this, "3PGC7GZ47C3CSCHZZRJ7");

    }
}

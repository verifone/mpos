package com.verifone.swordfish.manualtransaction;


import android.app.Application;

import com.verifone.utilities.Log;


/**
 * Copyright (C) 2016,2017 Verifone, Inc.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * VERIFONE, INC. BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * <p>
 * Except as contained in this notice, the name of Verifone, Inc. shall not be
 * used in advertising or otherwise to promote the sale, use or other dealings
 * in this Software without prior written authorization from Verifone, Inc.
 * <p>
 * <p>
 * <p>
 * This is a subclass of {@link Application} used to provide shared objects for this app.
 */
public class ManualTransactionApplication extends Application {

    private static CarbonBridge sCarbonBridge;
    private static TransactionStorage sTransactionStorage;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.initialize(this);

        sCarbonBridge = new CarbonBridge(this);
        sTransactionStorage = new TransactionStorage(this);
    }

    public static CarbonBridge getCarbonBridge() {
        sCarbonBridge.waitForTransactionManagerInit();
        return sCarbonBridge;
    }

    public static TransactionStorage getTransactionStorage() {
        return sTransactionStorage;
    }
}

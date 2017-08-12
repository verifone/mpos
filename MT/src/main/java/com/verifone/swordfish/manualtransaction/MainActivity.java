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

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.bugsee.library.Bugsee;
import com.verifone.swordfish.manualtransaction.MTDataModel.MTTransactionHistory;
import com.verifone.swordfish.manualtransaction.System.PaymentTerminal;
import com.verifone.swordfish.manualtransaction.System.SyncManager;
import com.verifone.swordfish.manualtransaction.Tools.MposLogger;
import com.verifone.swordfish.manualtransaction.Tools.PrinterUtility;


public class MainActivity extends FragmentActivity {

    private ServiceConnection serviceConnection;
    private Bugsee mBugseeAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Code for initial setup
        MposLogger.getInstance();
        MTTransactionHistory.getInstance();
        SyncManager.getInstance().setContext(this);
        //Starting payment service
        PaymentTerminal.getInstance().setContext(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        PrinterUtility printerUtility = PrinterUtility.getInstance();
        serviceConnection = printerUtility.getServiceConnection();
        Intent intent = new Intent();
        intent.setComponent(
                new ComponentName("com.verifone.swordfish.print.service",
                        "com.verifone.swordfish.print.service.DirectPrintService"));
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
        printerUtility.serviceBindPrintService(serviceConnection);

    }

    @Override
    protected void onPause() {
        super.onPause();
        PrinterUtility printerUtility = PrinterUtility.getInstance();
        unbindService(serviceConnection);
        printerUtility.serviceUnbind();
    }
}

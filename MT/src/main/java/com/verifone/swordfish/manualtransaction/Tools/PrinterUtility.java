package com.verifone.swordfish.manualtransaction.tools;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;

import com.verifone.commerce.entities.Payment;
import com.verifone.commerce.entities.Receipt;
import com.verifone.peripherals.IDirectPrintListener;
import com.verifone.peripherals.IDirectPrintService;
import com.verifone.peripherals.Printer;
import com.verifone.swordfish.manualtransaction.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;


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
 */

public class PrinterUtility {

    private static String TAG = PrinterUtility.class.getSimpleName();

    public static final String AMOUNT_PLACEHOLDER = "AMOUNT_PLACEHOLDER";

    private static PrinterUtility instance;

    /**
     * Local reference to the print service, so that we can perform any number of calls.
     */
    private IDirectPrintService mPrintService;

    public static PrinterUtility getInstance() {
        if (instance == null) {
            instance = new PrinterUtility();
        }
        return instance;
    }

    /**
     * Manages the connection to the service.
     */
    private ServiceConnection mServiceConnection;

    private ServiceConnection getServiceConnection() {
        return new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                // We must use this to expose the service methods.
                mPrintService = IDirectPrintService.Stub.asInterface(iBinder);
                MposLogger.getInstance().debug(TAG, "Service connected.");
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                MposLogger.getInstance().debug(TAG, "Service disconnected.");
                mPrintService = null;
            }
        };
    }

    public void bindPrintService(Context applicationContext) {
        MposLogger.getInstance().debug(TAG, "Binding to print service.");

        if (mPrintService == null) {
            mServiceConnection = getServiceConnection();
            Intent intent = new Intent();
            intent.setComponent(
                    new ComponentName("com.verifone.peripherals.service",
                            "com.verifone.peripherals.service.DirectPrintService"));

            applicationContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        }

    }

    public void unbindPrintService(Context applicationContext) {
        MposLogger.getInstance().debug(TAG, "Unbinding to print service.");

        if (mServiceConnection != null) {
            try {
                applicationContext.unbindService(mServiceConnection);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Service can not be unbound. ", e);
            }
        }
        mPrintService = null;
        mServiceConnection = null;
    }

    public void printReceipt(Receipt receipt, IDirectPrintListener listener) {

        if (receipt == null) {
            MposLogger.getInstance().error(TAG, " receipt is null");
            return;
        }

        printHtml(receipt.getAsHtml(), listener);
    }

    public void printHtml(String html, IDirectPrintListener listener) {

        if (this.mPrintService == null) {
            MposLogger.getInstance().error(TAG, " print service unbind");
            return;
        }

        if (TextUtils.isEmpty(html)) {
            MposLogger.getInstance().error(TAG, " html is null");
            return;
        }

        String printerId;
        Printer defaultPrinter = null;
        try {
            defaultPrinter = mPrintService.getDefaultPrinter();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        if (defaultPrinter != null) {
            printerId = defaultPrinter.getLocalId();
        } else {
            MposLogger.getInstance().debug(TAG, "Printer not found");
            return;
        }

        try {
            mPrintService.printString(listener, html, printerId, Printer.PRINTER_FULL_CUT);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void printUnknownReceipt(FragmentActivity activity, Payment payment, IDirectPrintListener printListener) {
        InputStream inputStream = activity.getResources().openRawResource(R.raw.unknown_receipt);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder receiptBuilder = new StringBuilder();
        try {
            String readLine;
            while ((readLine = reader.readLine()) != null) {
                receiptBuilder.append(readLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        int amountStart = receiptBuilder.indexOf(AMOUNT_PLACEHOLDER);
        receiptBuilder.replace(amountStart, amountStart + AMOUNT_PLACEHOLDER.length(),
                new DecimalFormat("0.00").format(payment.getPaymentAmount().doubleValue()));

        PrinterUtility.getInstance().printHtml(receiptBuilder.toString(), printListener);
    }

}

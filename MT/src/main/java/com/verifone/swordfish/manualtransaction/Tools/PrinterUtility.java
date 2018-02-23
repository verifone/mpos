package com.verifone.swordfish.manualtransaction.tools;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;

import com.verifone.commerce.entities.Receipt;
import com.verifone.peripherals.IDirectPrintListener;
import com.verifone.peripherals.IDirectPrintService;
import com.verifone.peripherals.Printer;
import com.verifone.utilities.Log;

import java.util.concurrent.TimeUnit;


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
    public static final String PRINTER__JOB_STATUS_ACTION = "com.verifone.swordfish" +
            ".manualtransaction.printer_job_status_action";
    public static final String PRINTER__JOB_STATUS_MSG_KEY = "PRINTER__JOB_STATUS_MSG_KEY";

    private static PrinterUtility instance;
    private static String TAG = PrinterUtility.class.getSimpleName();

    /**
     * Local reference to the print service, so that we can perform any number of calls.
     */
    private IDirectPrintService mPrintService;

    private static final int TEXT_SIZE = 24;

    /**
     * Number of carbon-printers-dependent points in one symbol for 24 text size
     */
    private static final double LINE_LENGTH_MULTIPLIER = 14.3;

    private int mLineLength;
    private StringBuilder message;
    /**
     * The text view dislpaying the list of available printers.
     */
    private String mPrinterList;
    private long mPrintStartTime;

    public static PrinterUtility getInstance() {
        if (instance == null) {
            instance = new PrinterUtility();
        }
        return instance;
    }

    /**
     * Method generate print listener with possibility to show Toast messages
     *
     * @param context activity or context where show Toast
     * @return listener object
     */
    private IDirectPrintListener createPrintListener(final Context context) {
        return new IDirectPrintListener.Stub() {
            private Context mAppCtx = context.getApplicationContext();

            private void sendBroadcast(String message) {
                if (mAppCtx != null) {
                    Intent intent = new Intent(PRINTER__JOB_STATUS_ACTION);
                    intent.putExtra(PRINTER__JOB_STATUS_MSG_KEY, message);
                    mAppCtx.sendBroadcast(intent);
                }
            }

            @Override
            public void started(String printId) throws RemoteException {
                displayMessage("Print started for " + printId);
            }

            /** Called when the print job cannot continue, but could be resumed later. */
            @Override
            public void block(String printId, String errorMessage) throws RemoteException {
                displayMessage("Print blocked for " + printId + ". " + errorMessage);
            }

            /** Called when the print job has finished being cancelled. This is the final message. */
            @Override
            public void cancel(String printId) throws RemoteException {
                displayMessage("Print cancelled for " + printId);
            }

            @Override
            public void failed(String printId, String errorMessage) throws RemoteException {
                String printJobId = !TextUtils.isEmpty(printId) ? printId : "unknown print job id";
                String message = errorMessage == null ? "Print failed" : errorMessage;

                displayMessage("Print failed for " + printJobId + ". " + errorMessage);

                sendBroadcast(message + " for " + printJobId);
            }

            @Override
            public void complete(String printId) throws RemoteException {
                String printJobId = !TextUtils.isEmpty(printId) ? printId : "unknown print job id";
                sendBroadcast("Print done for " + printJobId);

                long totalTimeMillis = System.currentTimeMillis() - mPrintStartTime;
                long second = TimeUnit.MILLISECONDS.toSeconds(totalTimeMillis);
                long minute = TimeUnit.MILLISECONDS.toMinutes(totalTimeMillis);
                long millis = totalTimeMillis % 1000;
                displayMessage("Print completed for " + printId + " within " +
                        String.format("%02d:%02d.%d", minute, second, millis) + " minutes.");
            }
        };
    }

    /**
     * The listener which receives the callbacks from the print service for print job status
     * updates.
     */
    private final IDirectPrintListener mPrintListener = new IDirectPrintListener.Stub() {
        /** Called when a print job has moved from the queue and is being processed. */
        @Override
        public void started(String printId) throws RemoteException {
            displayMessage("Print started for " + printId);
        }

        /** Called when the print job cannot continue, but could be resumed later. */
        @Override
        public void block(String printId, String errorMessage) throws RemoteException {
            displayMessage("Print blocked for " + printId + ". " + errorMessage);
        }

        /** Called when the print job has finished being cancelled. This is the final message. */
        @Override
        public void cancel(String printId) throws RemoteException {
            displayMessage("Print cancelled for " + printId);
        }

        /** Called when the print job has failed, and cannot be resumed. This is the final message. */
        @Override
        public void failed(String printId, String errorMessage) throws RemoteException {
            String printJobId = !TextUtils.isEmpty(printId) ? printId : "unknown print job id";
            displayMessage("Print failed for " + printJobId + ". " + errorMessage);
        }

        /** Called when the print job is complete. */
        @Override
        public void complete(String printId) throws RemoteException {
            long totalTimeMillis = System.currentTimeMillis() - mPrintStartTime;
            long second = TimeUnit.MILLISECONDS.toSeconds(totalTimeMillis);
            long minute = TimeUnit.MILLISECONDS.toMinutes(totalTimeMillis);
            long millis = totalTimeMillis % 1000;
            displayMessage("Print completed for " + printId + " within " +
                    String.format("%02d:%02d.%d", minute, second, millis) + " minutes.");
        }
    };

    /**
     * Manages the connection to the service.
     */
    private ServiceConnection mServiceConnection;

    public ServiceConnection getServiceConnection() {
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

    /**
     * Retrieves available printers from the service and displays them.
     */
    private void displayPrinterList() {
        StringBuilder printerDisplayText = new StringBuilder("");
        try {
            Printer[] printers = mPrintService.getAvailablePrinters();
            for (Printer printer : printers) {
                if (printer != null) {
                    StringBuilder printerText = new StringBuilder();
                    printerText.append(printer.getLocalId());
                    printerText.append("\t");
                    printerText.append(printer.getName());
                    printerText.append("\t");
                    printerText.append(printer.getAddress());
                    printerText.append("\t");
                    printerText.append(printer.isDefault());
                    printerText.append("\t\t");
                    printerText.append(printer.getUpdatedAt());
                    printerText.append("\n");
                    printerDisplayText.append(printerText);
                }
            }
            if (printerDisplayText.length() == 0) {
                MposLogger.getInstance().error(TAG, "No printers found.");
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            MposLogger.getInstance().error(TAG, "Remote exception when getting available printers.");
        }
        final String displayText = printerDisplayText.toString();
        mPrinterList = displayText;
    }

    public void printReceipt(Receipt receipt, IDirectPrintListener listener) {
        if (this.mPrintService == null) {
            MposLogger.getInstance().error(TAG, " print service unbind");
            return;
        }

        if (receipt == null) {
            MposLogger.getInstance().error(TAG, " receipt is null");
            return;
        }

        if (mPrinterList == null) {
            displayPrinterList();
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
            displayMessage("Printer not found");
            return;
        }

        try {
            mPrintService.printString(listener, receipt.getAsHtml(), printerId, Printer.PRINTER_FULL_CUT);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void initLineLength(int paperWidth) {
        mLineLength = (int) Math.round(paperWidth / LINE_LENGTH_MULTIPLIER);
    }

    public void printImage(Bitmap bitmap) {
        try {
            int paperWidth;
            String printerId;
            Printer defaultPrinter = mPrintService.getDefaultPrinter();
            if (defaultPrinter != null) {
                paperWidth = defaultPrinter.getPaperWidth();
                printerId = defaultPrinter.getLocalId();
            } else {
                displayMessage("Printer not found");
                return;
            }

            final TextPaint textPaint = new TextPaint();
            textPaint.setTextSize(TEXT_SIZE);
            textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            final StaticLayout staticLayout = new StaticLayout(message,
                    textPaint, paperWidth,
                    Layout.Alignment.ALIGN_CENTER,
                    1, 0, false);
            final Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.WHITE);
            staticLayout.draw(canvas);
            mPrintStartTime = System.currentTimeMillis();

            mPrintService.printBitmap(mPrintListener, bitmap, printerId, Printer.PRINTER_FULL_CUT);
        } catch (RemoteException e) {
            e.printStackTrace();
            displayMessage("Failed to print.");
        }

    }

    private String printEmptyLine(SpannableStringBuilder ssb) {
        return printLine("", "");
    }

    private String printLine(String left, String right) {
        int space = mLineLength - left.length() - right.length();
        if (!TextUtils.isEmpty(right) && space < 1) {
            space = 1;
        }

        if (left.length() + space + right.length() > mLineLength) {
            left = left.substring(0, mLineLength - right.length() - space);
        }

        StringBuilder builder = (new StringBuilder()).append(left);

        for (int i = 0; i < space; ++i) {
            builder.append(' ');
        }

        builder.append(right);
        builder.append("\n");
        return builder.toString();
    }

    private String printLine(SpannableStringBuilder ssb, String s) {
        ssb.append(s + "\n");
        return ssb.toString();
    }

    public String printDividerLine(SpannableStringBuilder ssb) {
        StringBuilder dividerBuilder = new StringBuilder();

        for (int i = 0; i < mLineLength; ++i) {
            dividerBuilder.append('=');
        }

        return printLine(ssb, dividerBuilder.toString());
    }

    private void displayMessage(String message) {
        MposLogger.getInstance().debug(TAG, message);
    }


    public void printTransaction(Context context, Receipt receipt) {
        /// String asHtml = receipt.getAsHtml();
        receipt.print(context);

    }
}

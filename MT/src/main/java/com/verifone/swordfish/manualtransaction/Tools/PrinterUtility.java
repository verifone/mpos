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

package com.verifone.swordfish.manualtransaction.Tools;

import android.app.Activity;
import android.content.ComponentName;
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

import com.verifone.commerce.entities.Merchandise;
import com.verifone.commerce.entities.Payment;
import com.verifone.peripherals.IDirectPrintListener;
import com.verifone.peripherals.IDirectPrintService;
import com.verifone.peripherals.Printer;
import com.verifone.swordfish.manualtransaction.HistoryFragments.TransactionStatus;
import com.verifone.swordfish.manualtransaction.MTDataModel.MTTransaction;
import com.verifone.swordfish.manualtransaction.System.PaymentTerminal;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


public class PrinterUtility {
    private static PrinterUtility instance;
    private static String TAG = PrinterUtility.class.getSimpleName();

    /**
     * Local reference to the print service, so that we can perform any number of calls.
     */
    private IDirectPrintService mPrintService;
    private static final int SHORT_LINE_LENGTH = 36;
    private static final int LONG_LINE_LENGTH = 40;
    private static int mLineLength = 40;
    private StringBuilder message;
    /**
     * The text view dislpaying the list of available printers.
     */
    private String mPrinterList;
    private long mPrintStartTime;


    protected PrinterUtility() {

    }

    public static PrinterUtility getInstance() {
        if (instance == null) {
            instance = new PrinterUtility();
        }
        return instance;
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
    private ServiceConnection mServiceConnection = new ServiceConnection() {
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

    public ServiceConnection getServiceConnection() {
        return mServiceConnection;
    }

    public void serviceBindPrintService(ServiceConnection serviceConnection) {
        MposLogger.getInstance().debug(TAG, "Binding to print service.");
        this.mServiceConnection = serviceConnection;
    }

    public void serviceUnbind() {
        MposLogger.getInstance().debug(TAG, "Unbinding to print service.");
        mPrintService = null;
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

    public void printTransaction(MTTransaction transaction, Activity activity) {
        if (this.mPrintService == null) {
            MposLogger.getInstance().error(TAG, " print service unbind");
            return;
        }
        if (transaction == null && transaction.transactionMerchandises() == null) {
            MposLogger.getInstance().error(TAG, " transaction and/or merchandise are null");
            return;
        }
        if (mPrinterList == null) {
            displayPrinterList();
        }

        message = new StringBuilder();
        final SpannableStringBuilder ssb = new SpannableStringBuilder();
        String transactionID = PaymentTerminal.getInstance().transactionID();
        if (transactionID == null) {
            transactionID = "TBD";
        }
        //Creating header

        message.append("mPOS by Verifone Inc. \n");
        message.append(printEmptyLine(ssb));
        message.append("88 W Plumeria Dr. \n");
        message.append("San Jose, CA 95134 \n");
        message.append(printEmptyLine(ssb));
        message.append(printEmptyLine(ssb));


        message.append(printLine(ssb, "Transaction id:", "#" + transactionID));
        String reportDate;
        SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a");
        Date today = Calendar.getInstance().getTime();
        reportDate = df.format(today);

        message.append(printLine(ssb, "Date:" + reportDate, ""));

        BigDecimal transactionAmount = new BigDecimal("0.00");
        BigDecimal transactionTax = new BigDecimal("0.00");
        BigDecimal transactionDiscount = new BigDecimal("0.00");

        LocalizeCurrencyFormatter formatter = LocalizeCurrencyFormatter.getInstance();
        for (Merchandise item : transaction.transactionMerchandises()) {
            int qty = item.getQuantity();
            String desc = item.getDescription();
            //discountString = item.getLocalizedItemDiscount();
            BigDecimal tax = item.getTax();
            BigDecimal itemTotal = item.getAmount();
            String sign = " ";
            String discountSign = "- ";
/*
            if (item.isItemRefund()) {
                sign = "-";
                discountSign = "+ ";
                desc = desc + " (Item refunded)";
            }
*/
            message.append(printLine(ssb, qty + " " + desc, sign + formatter.getCurrencyFormat().format(itemTotal.doubleValue())));
            if (item.getDiscount() != null && !Objects.equals(item.getDiscount(), BigDecimal.ZERO)) {
                message.append(printLine(ssb, "Discount: ", " - " + String.format(Locale.getDefault(), "$%.2f", item.getDiscount().doubleValue())));
                transactionDiscount = transactionDiscount.add(item.getDiscount());
            }
            if (item.getTax() != null && !Objects.equals(item.getTax(), BigDecimal.ZERO)) {
                message.append(printLine(ssb, "", "Tax: " + String.format(Locale.getDefault(), "$%.2f", item.getTax().doubleValue())));
                transactionTax = transactionTax.add(item.getTax());
            }
            transactionAmount = transactionAmount.add(item.getUnitPrice().multiply(BigDecimal.valueOf(qty)));


        }

        //discountString = transaction.getLocalizedCurrencyDiscount();
        message.append(printEmptyLine(ssb));
        message.append(printEmptyLine(ssb));

        String taxString = formatter.getCurrencyFormat().format(transaction.getTransactionTax().doubleValue());
        String discountString = formatter.getCurrencyFormat().format(transaction.getTransactionDiscount().doubleValue());
        String amountString = formatter.getCurrencyFormat().format(transaction.getTransactionTotal().doubleValue());
        message.append(printDividerLine(ssb));
        message.append(printLine(ssb, "Tax:", taxString));
        message.append(printLine(ssb, "Discount:", "- " + discountString));
        message.append(printLine(ssb, "Total:", amountString));
        message.append(printDividerLine(ssb));

        if (transaction.transactionPayments() != null) {
            for (Payment payment : transaction.transactionPayments().getPayments()) {

                if (payment.getPaymentType() != Payment.PaymentType.CASH) {
                    String sign = " ";
                    if (payment.getCardInformation().getCardHolderName() != null) {
                        message.append(printLine(ssb, "Card: ", payment.getCardInformation().getCardHolderName()));
                    }
                    if (payment.getCardInformation().getPanLast4() != null) {
                        message.append(printLine(ssb, "CCNum: ", "****" + payment.getCardInformation().getPanLast4()));
                    }
                    String label;
                    label = "Amount:";
                    if (transaction.getStatus() == TransactionStatus.voided) {
                        label = "Voided amount:";
                    } else {
                        if (transaction.getStatus() == TransactionStatus.refunded) {
                            label = "Refund amount:";
                            sign = "-";
                        } else {
                            label = "Amount:";
                        }
                    }

                    message.append(printLine(ssb, label, sign + formatter.getCurrencyFormat().format(payment.getPaymentAmount().doubleValue())));

                } else {
                    String label;
                    String sign = " ";
                    if (transaction.getStatus() == TransactionStatus.refunded) {
                        label = "Refund amount:";
                        sign = "-";
                    } else {
                        label = "Sale (Cash):";
                    }

                    message.append(printLine(ssb, label, sign + formatter.getCurrencyFormat().format(payment.getPaymentAmount().doubleValue())));
                }
                message.append(printEmptyLine(ssb));

            }
        }
        MposLogger.getInstance().debug(TAG, "Formatted receipt:\n" + ssb.toString());

        message.append(printEmptyLine(ssb));
        message.append("We thank you for your business!!! \n");
        message.append(printEmptyLine(ssb));
        message.append("Powered by Carbon \n");
        message.append("http://www.verifone.com");

        // Start creating a bitmap from the text.
        final TextPaint textPaint = new TextPaint();
        textPaint.setTextSize(24);
        textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        final StaticLayout staticLayout = new StaticLayout(message,
                textPaint, 576,
                Layout.Alignment.ALIGN_CENTER,
                1, 0, false);
        final Bitmap bitmap = Bitmap.createBitmap(staticLayout.getWidth(),
                staticLayout.getHeight(),
                Bitmap.Config.RGB_565);
        final Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        staticLayout.draw(canvas);
        mPrintStartTime = System.currentTimeMillis();
        try {

            mPrintService.printBitmap(mPrintListener, bitmap);
        } catch (RemoteException e) {
            e.printStackTrace();
            displayMessage("Failed to print.");
        }
    }

    public void printImage(Bitmap bitmap) {
        final TextPaint textPaint = new TextPaint();
        textPaint.setTextSize(24);
        textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        final StaticLayout staticLayout = new StaticLayout(message,
                textPaint, 576,
                Layout.Alignment.ALIGN_CENTER,
                1, 0, false);
        final Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        staticLayout.draw(canvas);
        mPrintStartTime = System.currentTimeMillis();
        try {

            mPrintService.printBitmap(mPrintListener, bitmap);
        } catch (RemoteException e) {
            e.printStackTrace();
            displayMessage("Failed to print.");
        }

    }

    private String printEmptyLine(SpannableStringBuilder ssb) {
        return printLine(ssb, "", "");
    }

    private String printLine(SpannableStringBuilder ssb, String left, String right) {
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


}

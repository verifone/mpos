package com.verifone.swordfish.manualtransaction.gui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.verifone.commerce.entities.Payment;
import com.verifone.commerce.entities.Receipt;
import com.verifone.commerce.entities.Transaction;
import com.verifone.peripherals.IDirectPrintListener;
import com.verifone.swordfish.manualtransaction.CarbonBridge;
import com.verifone.swordfish.manualtransaction.IBridgeListener;
import com.verifone.swordfish.manualtransaction.ITransactionHistoryListener;
import com.verifone.swordfish.manualtransaction.ManualTransactionApplication;
import com.verifone.swordfish.manualtransaction.R;
import com.verifone.swordfish.manualtransaction.TransactionStorage;
import com.verifone.swordfish.manualtransaction.tools.PrinterUtility;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Copyright (C) 2016,2017,2018 Verifone, Inc.
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
 * Created by romans1 on 01/23/2018.
 */


public class TransactionHistoryActivity extends BaseActivity implements IBridgeListener, ITransactionHistoryListener {

    private Thread mLoadingTransactionsThread;
    private AlertDialog mAlertDialogRefundRequest;

    private SimpleDateFormat mSimpleDateFormatFrom;
    private SimpleDateFormat mSyncDateSimpleDateFormat;

    private enum TransactionState {NONE, REFUND, VOID}

    private TransactionState mTransactionState = TransactionState.NONE;

    private IDirectPrintListener.Stub mPrintListener;

    private RecyclerView mRecyclerView;

    private Transaction mTransactionForRefund;
    private Payment mPaymentForRefund;

    public TransactionHistoryActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_history_controller);

        setTitle(R.string.tab_two);

        mSimpleDateFormatFrom = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.getDefault());
        mSyncDateSimpleDateFormat = new SimpleDateFormat("MM/dd hh:mm", Locale.getDefault());

        mRecyclerView = findViewById(R.id.listViewTH);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mRecyclerView.setAdapter(new TransactionsAdapter(new ArrayList<Transaction>(), TransactionHistoryActivity.this));
    }

    @Override
    protected void onStart() {
        super.onStart();

        mPrintListener = createPrintListener(this);
        PrinterUtility.getInstance().bindPrintService(getApplicationContext());

        refreshTransactionList();
    }

    @Override
    protected void onStop() {
        stopRefreshThread();

        ((TransactionsAdapter) mRecyclerView.getAdapter()).setTransactions(new ArrayList<Transaction>());

        PrinterUtility.getInstance().unbindPrintService(getApplicationContext());

        if (mAlertDialogRefundRequest != null) {
            mAlertDialogRefundRequest.dismiss();
        }

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        ManualTransactionApplication.getCarbonBridge().setListener(null);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.transaction_history_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        long lastReconcileDate = TransactionStorage.getLastReconcileDate(getApplicationContext());
        String lastSyncDate = lastReconcileDate > 0
                ? mSyncDateSimpleDateFormat.format(new Date(lastReconcileDate))
                : getString(R.string.never);

        MenuItem menuItemLastSync = menu.findItem(R.id.menu_item_last_sync);
        menuItemLastSync.setTitle(getString(R.string.caption_menu_last_sync, lastSyncDate));

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_reconcile:
                showDialogWithMessage(getString(R.string.title_reconcile_in_progress), false);
                performReconcileBatchProcess();
                return true;
            case R.id.menu_item_last_sync:
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void refreshTransactionList() {
        showDialogWithMessage(getString(R.string.loading_history), false);

        stopRefreshThread();
        mLoadingTransactionsThread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (Thread.interrupted()) {
                    return;
                }

                final List<Transaction> transactions = ManualTransactionApplication.getTransactionStorage().readAllTransactions();

                if (Thread.interrupted()) {
                    return;
                }

                new Handler(getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if (TransactionHistoryActivity.this.isDestroyed() || TransactionHistoryActivity.this.isFinishing()) {
                            return;
                        }

                        ((TransactionsAdapter) mRecyclerView.getAdapter()).setTransactions(transactions);

                        hideDialog();
                    }
                });
            }
        });
        mLoadingTransactionsThread.start();
    }

    private void stopRefreshThread() {
        if (mLoadingTransactionsThread != null && mLoadingTransactionsThread.isAlive()) {
            mLoadingTransactionsThread.interrupt();
            try {
                mLoadingTransactionsThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public void onBackPressed() {
        switch (mTransactionState) {
            case REFUND:
            case VOID:
                showDialogWithMessage(getString(R.string.title_ending_session), false);
                ManualTransactionApplication.getCarbonBridge().stopSession();
                break;
            case NONE:
            default:
                super.onBackPressed();
                break;
        }
    }

    @Override
    public void onRefund(final Transaction transaction) {
        DialogInterface.OnClickListener onOkClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ArrayList<Payment> payments = transaction.getPayments();
                if (payments != null && payments.size() > 0) {
                    for (Payment payment : payments) {
                        Payment.AuthorizationResult authResult = payment.getAuthResult();
                        switch (authResult) {
                            case AUTHORIZED_ONLINE:
                            case AUTHORIZED_OFFLINE:
                            case AUTHORIZED_EXTERNALLY:
                            case CASH_VERIFIED:
                                mTransactionForRefund = transaction;
                                mPaymentForRefund = payment;
                                Payment.PaymentType paymentType = payment.getPaymentType();
                                switch (paymentType) {
                                    case CASH:
                                        mTransactionState = TransactionState.REFUND;
                                        startTerminalSession();
                                        break;
                                    case CREDIT:
                                    case DEBIT:
                                    case STORED_VALUE:
                                    case EBT:
                                        Date parsedDate = null;
                                        try {
                                            String timestamp = payment.getTimestamp();
                                            if (!TextUtils.isEmpty(timestamp)) {
                                                parsedDate = mSimpleDateFormatFrom.parse(timestamp);
                                            }
                                        } catch (ParseException ignored) {
                                        }
                                        long lastReconcileDate = TransactionStorage.getLastReconcileDate(getApplicationContext());
                                        if (parsedDate == null || parsedDate.getTime() <= lastReconcileDate) {
                                            mTransactionState = TransactionState.REFUND;
                                        } else {
                                            mTransactionState = TransactionState.VOID;
                                        }
                                        startTerminalSession();
                                        break;
                                    case ALTERNATE:
                                    case CHECK:
                                        break;
                                }
                                break;
                            default:
                                break;
                        }

                    }
                }
            }
        };

        if (mAlertDialogRefundRequest != null) {
            mAlertDialogRefundRequest.dismiss();
        }
        mAlertDialogRefundRequest = new AlertDialog.Builder(this)
                .setMessage(R.string.void_transaction_label)
                .setPositiveButton(R.string.str_yes, onOkClickListener)
                .setNegativeButton(R.string.str_no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                }).create();

        mAlertDialogRefundRequest.show();
    }

    @Override
    public void onPrint(Transaction transaction) {
        ArrayList<Payment> payments = transaction.getPayments();
        if (payments != null && payments.size() > 0) {
            for (Payment payment : payments) {
                Receipt receipt = payment.getReceipt();
                if (receipt != null) {
                    PrinterUtility.getInstance().printReceipt(receipt, mPrintListener);
                }
            }
        }
    }

    private void startTerminalSession() {
        ManualTransactionApplication.getCarbonBridge().setListener(this);

        switch (mTransactionState) {
            case NONE:
                break;
            case REFUND:
                showDialogWithMessage(getString(R.string.refund_performing), false);
                ManualTransactionApplication.getCarbonBridge().startRefundSession();
                break;
            case VOID:
                showDialogWithMessage(getString(R.string.void_performing), false);
                ManualTransactionApplication.getCarbonBridge().startVoidSession();
                break;
        }
    }

    private void performReconcileBatchProcess() {
        ManualTransactionApplication.getCarbonBridge().setListener(this);

        ManualTransactionApplication.getCarbonBridge().reconcileTransactions();
    }

    @Override
    public void sessionStarted() {
        CarbonBridge carbonBridge = ManualTransactionApplication.getCarbonBridge();

        switch (mTransactionState) {
            case NONE:
                break;
            case REFUND:
                carbonBridge.refundPayment(mPaymentForRefund);
                break;
            case VOID:
                carbonBridge.voidTransaction(mTransactionForRefund);
                break;
        }

    }

    @Override
    public void sessionStopped() {
        mTransactionState = TransactionState.NONE;
        hideDialog();
    }

    @Override
    public void onTransactionCanceled() {
        showDialogWithMessage(getString(R.string.title_closing_session), false);
        ManualTransactionApplication.getCarbonBridge().stopSession();
    }

    @Override
    public void onTransactionEnded(final boolean isSuccessful) {
        if (mTransactionState == TransactionState.NONE) {
            return;
        }

        CarbonBridge carbonBridge = ManualTransactionApplication.getCarbonBridge();

        Transaction transaction = carbonBridge.getTransaction();
        if (transaction != null) {
            ArrayList<Payment> payments = transaction.getPayments();
            if (payments != null) {
                Payment paymentRefunded = payments.get(0);
                Receipt receipt = paymentRefunded.getReceipt();
                if (receipt != null) {
                    PrinterUtility.getInstance().printReceipt(receipt, mPrintListener);
                }
                mTransactionForRefund.getPayments().add(paymentRefunded);
                ManualTransactionApplication.getTransactionStorage()
                        .updateTransactionPayments(mTransactionForRefund.getInvoiceId(), mTransactionForRefund.getPayments());
            }
        }

        carbonBridge.stopSession();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                hideDialog();
                Toast.makeText(TransactionHistoryActivity.this,
                        mTransactionState.name() + " is: " + (isSuccessful ? "SUCCESS" : "FAILED"), Toast.LENGTH_LONG).show();
                mTransactionState = TransactionState.NONE;
                refreshTransactionList();
            }
        });
    }

    @Override
    public void sessionStartFailed() {

    }

    @Override
    public void reconcileSuccess() {
        hideDialog();
        Toast.makeText(this, R.string.title_reconcile_successful, Toast.LENGTH_LONG).show();
        TransactionStorage.saveLastReconcile(getApplicationContext());
        supportInvalidateOptionsMenu();
        if (mRecyclerView != null && mRecyclerView.getAdapter() != null) {
            mRecyclerView.getAdapter().notifyDataSetChanged();
        }
    }

    @Override
    public void reconcileFailed(String message) {
        hideDialog();
        Toast.makeText(this, getString(R.string.title_reconcile_failed, message), Toast.LENGTH_LONG).show();
    }

    private IDirectPrintListener.Stub createPrintListener(final Context context) {
        return new IDirectPrintListener.Stub() {

            @Override
            public void started(String printId) throws RemoteException {
            }

            @Override
            public void block(String printId, String errorMessage) throws RemoteException {
            }

            @Override
            public void cancel(String printId) throws RemoteException {
            }

            @Override
            public void failed(String printId, final String errorMessage) throws RemoteException {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String message = errorMessage == null ? "Print failed" : errorMessage;
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                    }
                });

            }

            @Override
            public void complete(String printId) throws RemoteException {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "Print completed", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        };
    }

}

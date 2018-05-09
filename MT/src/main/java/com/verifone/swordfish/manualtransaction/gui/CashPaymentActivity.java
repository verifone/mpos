package com.verifone.swordfish.manualtransaction.gui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.verifone.commerce.entities.Payment;
import com.verifone.commerce.entities.Transaction;
import com.verifone.swordfish.manualtransaction.IBridgeListener;
import com.verifone.swordfish.manualtransaction.ManualTransactionApplication;
import com.verifone.swordfish.manualtransaction.R;
import com.verifone.swordfish.manualtransaction.tools.PrinterUtility;
import com.verifone.swordfish.manualtransaction.tools.Utils;

import java.math.BigDecimal;

import static com.verifone.commerce.entities.Receipt.DELIVERY_METHOD_EMAIL;
import static com.verifone.commerce.entities.Receipt.DELIVERY_METHOD_NONE;
import static com.verifone.commerce.entities.Receipt.DELIVERY_METHOD_PRINT;
import static com.verifone.commerce.entities.Receipt.DELIVERY_METHOD_SMS;

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
 * Created by abey on 1/5/2018.
 */

public class CashPaymentActivity extends BaseListenerActivity
        implements IBridgeListener, View.OnClickListener, FinishFragment.IFinishFragmentListener {

    private View mMainRightView;
    private View mFinishRightView;

    private TextView mTotalAmountValue;
    private NumericEditText mCashReceivedValue;
    private TextView mBalanceTitle;
    private TextView mBalanceValue;

    private Button mBackBtn;
    private Button mCancelTransactionBtn;
    private Button mTenderBtn;

    private BigDecimal mAmount = BigDecimal.ZERO;
    private BigDecimal mCashReceiveAmount = BigDecimal.ZERO;
    private BigDecimal mChangeDueAmount = BigDecimal.ZERO;
    private BigDecimal mBalanceAmount = BigDecimal.ZERO;

    private TextWatcher textWatcher = getTextWatcher();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cash_payment);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        if (getIntent().getExtras() != null) {
            mAmount = (BigDecimal) getIntent().getExtras().getSerializable(PaymentActivity.AMOUNT_KEY);
        }
        if (mAmount == null) {
            throw new RuntimeException("Amount for mTransaction is not specified");
        }

        mMainRightView = findViewById(R.id.cash_payment_right_side);
        mFinishRightView = findViewById(R.id.final_fragment_container);
        mTotalAmountValue = findViewById(R.id.textViewAmountToPayLabel);
        mCashReceivedValue = findViewById(R.id.textViewCashReceivedAmount);
        mBalanceTitle = findViewById(R.id.textBalanceView);
        mBalanceValue = findViewById(R.id.textRemainBalanceView);

        mTenderBtn = findViewById(R.id.btn_right);
        mBackBtn = findViewById(R.id.btn_middle);
        mCancelTransactionBtn = findViewById(R.id.btn_left);

        mCashReceivedValue.setRepresentationType(NumericEditText.RepresentationType.CURRENCY);

        mTenderBtn.setText(R.string.tender_label);
        mTenderBtn.setAllCaps(true);
        mBackBtn.setText(R.string.back_label);
        mBackBtn.setAllCaps(true);
        mCancelTransactionBtn.setText(R.string.cancel_transaction_btn);
        mCancelTransactionBtn.setAllCaps(true);

        mTenderBtn.setOnClickListener(this);
        mBackBtn.setOnClickListener(this);
        mCancelTransactionBtn.setOnClickListener(this);

        mBalanceAmount = new BigDecimal(mAmount.toString());

        ManualTransactionApplication.getCarbonBridge().setListener(this);

        mTotalAmountValue.setText(Utils.getLocalizedAmount(mAmount));
        mBalanceValue.setText(Utils.getLocalizedAmount(mBalanceAmount));
    }

    @Override
    protected void onStart() {
        super.onStart();
        PrinterUtility.getInstance().bindPrintService(getApplicationContext());
        mCashReceivedValue.addTextChangedListener(textWatcher);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCashReceivedValue.requestFocus();
    }

    @Override
    protected void onStop() {
        PrinterUtility.getInstance().unbindPrintService(getApplicationContext());
        mCashReceivedValue.removeTextChangedListener(textWatcher);
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        ManualTransactionApplication.getCarbonBridge().setListener(this);

        if (requestCode == StartActivity.GENERAL_REQUEST_CODE) {
            switch (resultCode) {
                case RESULT_OK:
                    break;
                case StartActivity.RESULT_TRANSACTION_CANCELED:
                    setResult(StartActivity.RESULT_TRANSACTION_CANCELED);
                    finish();
                    break;
            }
        }
    }

    @Override
    public void onBackPressed() {
        FinishFragment fragment = getFinishFragment();
        if (fragment != null) {
            onFinishOrder();
        } else {
            super.onBackPressed();
        }
    }

    private void goBack() {
        finish();
    }

    private void cancelTransaction() {
        showDialogWithMessage(getString(R.string.title_ending_session), false);
        ManualTransactionApplication.getCarbonBridge().cancelTransaction();
        ManualTransactionApplication.getCarbonBridge().stopSession();
    }

    private void tender() {
        if (mBalanceAmount.floatValue() == BigDecimal.ZERO.floatValue()) {

            Transaction transaction = ManualTransactionApplication.getCarbonBridge().getTransaction();
            transaction.setTransactionType(Transaction.PAYMENT_TYPE);
            ManualTransactionApplication.getCarbonBridge().updateTransaction(transaction);

            Payment payments = new Payment();
            payments.setPaymentMethod("Cash");
            payments.setPaymentType(Payment.PaymentType.CASH);
            payments.setPaymentAmount(mCashReceiveAmount);

            showDialogWithMessage(getString(R.string.title_making_payment), false);

            ManualTransactionApplication.getCarbonBridge().startPayment(payments, false);
        }
    }

    private void calcAmounts(String sumStr) {
        if (!TextUtils.isEmpty(sumStr)) {
            mCashReceiveAmount = new BigDecimal(sumStr);
            mBalanceAmount = mAmount.subtract(mCashReceiveAmount);
            if (mBalanceAmount.floatValue() < 0) {
                mBalanceAmount = BigDecimal.ZERO;
            }
            mBalanceValue.setText(Utils.getLocalizedAmount(mBalanceAmount));
        } else {
            mBalanceAmount = new BigDecimal(mAmount.toString());
            mCashReceiveAmount = BigDecimal.ZERO;
            mBalanceValue.setText(Utils.getLocalizedAmount(mAmount));
        }
        mChangeDueAmount = mAmount.subtract(mCashReceiveAmount);
        if (mBalanceAmount.floatValue() == BigDecimal.ZERO.floatValue()) {
            mTenderBtn.setVisibility(View.VISIBLE);
            mBalanceTitle.setText(R.string.change_due);
            mBalanceValue.setText(Utils.getLocalizedAmount(mChangeDueAmount));
        } else {
            mTenderBtn.setVisibility(View.INVISIBLE);
            mBalanceTitle.setText(R.string.balance_label);
            mBalanceTitle.setAllCaps(true);
            mBalanceValue.setText(Utils.getLocalizedAmount(mBalanceAmount));
        }
    }

    private TextWatcher getTextWatcher() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calcAmounts(mCashReceivedValue.getValue());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_right:
                if (view.getVisibility() == View.VISIBLE) {
                    tender();
                }
                break;
            case R.id.btn_middle:
                goBack();
                break;
            case R.id.btn_left:
                cancelTransaction();
                break;
        }
    }

    /**
     * Carbon bridge listener
     */

    @Override
    public void sessionStopped() {
        setResult(StartActivity.RESULT_TRANSACTION_CANCELED);
        finish();
    }

    @Override
    public void onPaymentSuccess(final Payment payment) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                FinishFragment fragment = (FinishFragment) getSupportFragmentManager().findFragmentById(R.id.finish_fragment);
                if (fragment != null) {
                    fragment.setPayment(payment);
                } else {
                    Intent data = new Intent();
                    data.putExtra(PaymentActivity.AMOUNT_KEY, mAmount);
                    setResult(StartActivity.RESULT_OK, data);
                    finish();
                }
            }
        });
    }

    @Override
    public void onPaymentDecline() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(CashPaymentActivity.this).setMessage("Need to implement " +
                        "payment declined process")
                        .setTitle("Payment declined")
                        .setCancelable(false)
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                                setResult(RESULT_CANCELED);
                                CashPaymentActivity.this.finish();
                            }
                        }).show();
            }
        });
    }

    @Override
    public void onPaymentFailure() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(CashPaymentActivity.this).setMessage("Need to implement " +
                        "payment failure process")
                        .setTitle("Payment failure")
                        .setCancelable(false)
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                                setResult(RESULT_CANCELED);
                                CashPaymentActivity.this.finish();
                            }
                        }).show();
            }
        });
    }

    @Override
    public void onPaymentCanceled() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(CashPaymentActivity.this)
                        .setTitle(R.string.title_payment_cancelled)
                        .setMessage(R.string.message_payment_cancelled)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                                CashPaymentActivity.this.setResult(RESULT_CANCELED);
                                CashPaymentActivity.this.finish();
                            }
                        }).show();
            }
        });
    }

    @Override
    public void onReceiptMethodSelected(final int methodId, final String recipient) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                hideDialog();

                mMainRightView.setVisibility(View.GONE);
                mFinishRightView.setVisibility(View.VISIBLE);

                switch (methodId) {
                    case DELIVERY_METHOD_EMAIL:
                        new AlertDialog.Builder(CashPaymentActivity.this).setMessage("Send email" +
                                " to " + recipient)
                                .setTitle("Email Receipt")
                                .setCancelable(false)
                                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                    }
                                }).show();
                        break;
                    case DELIVERY_METHOD_SMS:
                        new AlertDialog.Builder(CashPaymentActivity.this).setMessage("Send sms" +
                                " to " + recipient)
                                .setTitle("SMS Receipt")
                                .setCancelable(false)
                                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                    }
                                }).show();
                        break;
                    case DELIVERY_METHOD_PRINT:
                        FinishFragment fragment = getFinishFragment();
                        if (fragment != null) {
                            fragment.print();
                        }
                        break;
                    case DELIVERY_METHOD_NONE:
                    default:
                        break;
                }
            }
        });
    }

    /**
     * Finish fragment callback
     */
    @Override
    public void onFinishOrder() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent data = new Intent();
                data.putExtra(PaymentActivity.AMOUNT_KEY, mAmount);
                setResult(StartActivity.RESULT_OK, data);
                finish();
            }
        });
    }

}

package com.verifone.swordfish.manualtransaction.gui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
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
 * Created by abey on 1/11/2018.
 */

public class CardPaymentActivity extends BaseActivity
        implements FinishFragment.IFinishFragmentListener, IBridgeListener {

    public static final String MANUAL_PAYMENT_KEY = "MANUAL_PAYMENT_KEY";

    private View mMainRightView;
    private View mFinishRightView;
    private TextView mPaymentTotalTV;
    private TextView mCardNumberValue;
    private TextView mCardExpirationValue;
    private TextView mCardHolderNameValue;
    private TextView mAuthCodeTV;

    private BigDecimal mAmount = BigDecimal.ZERO;
    private boolean mIsManualPayment = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_charge_payment);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mAmount = (BigDecimal) extras.getSerializable(PaymentActivity.AMOUNT_KEY);
            mIsManualPayment = extras.getBoolean(MANUAL_PAYMENT_KEY);
        }
        if (mAmount == null) {
            throw new RuntimeException("Amount for mTransaction is not specified");
        }

        mMainRightView = findViewById(R.id.waitLlinearProgress);
        mFinishRightView = findViewById(R.id.final_fragment_container);
        mPaymentTotalTV = findViewById(R.id.paymentHeadTotal);
        mCardNumberValue = findViewById(R.id.textViewCardNumber);
        mCardExpirationValue = findViewById(R.id.textViewCardExpiration);
        mCardHolderNameValue = findViewById(R.id.textViewCardHolder);
        mAuthCodeTV = findViewById(R.id.textViewCardAuthCode);

        ManualTransactionApplication.getCarbonBridge().setListener(this);

        updateAmount();

        startPayment();
    }

    private void updateAmount() {
        mPaymentTotalTV.setText(Utils.getLocalizedAmount(mAmount));
    }

    @Override
    protected void onStart() {
        super.onStart();
        PrinterUtility.getInstance().bindPrintService(getApplicationContext());
    }

    @Override
    protected void onStop() {
        PrinterUtility.getInstance().unbindPrintService(getApplicationContext());
        super.onStop();
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

    private void startPayment() {
        Payment payment = new Payment();
        Transaction transaction = ManualTransactionApplication.getCarbonBridge().getTransaction();
        if (mAmount.floatValue() >= 20)
            payment.setAuthorizationMethod(Payment.AuthorizationMethod.SIGNATURE);
        payment.setGratuityAmount(BigDecimal.ZERO);
        payment.setRequestedPaymentAmount(mAmount);

        transaction.setTransactionType(Transaction.PAYMENT_TYPE);

        ManualTransactionApplication.getCarbonBridge().updateTransaction(transaction);
        ManualTransactionApplication.getCarbonBridge().startPayment(payment, mIsManualPayment);
    }

    /**
     * Finish fragment callback
     */
    @Override
    public void onFinishOrder() {
        Intent data = new Intent();
        data.putExtra(PaymentActivity.AMOUNT_KEY, mAmount);
        setResult(StartActivity.RESULT_OK, data);
        finish();
    }

    /**
     * Carbon bridge callbacks
     */

    @Override
    public void onPaymentSuccess(final Payment payment) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (payment != null) {
                    if (payment.getCardInformation() != null) {
                        mCardNumberValue.setText(payment.getCardInformation().getCardToken());
                        mCardExpirationValue.setText(payment.getCardInformation().getCardExpiry());
                        mCardHolderNameValue.setText(payment.getCardInformation().getBankUserData());
                        mAuthCodeTV.setText(payment.getAuthCode());
                    }
                }

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
                new AlertDialog.Builder(CardPaymentActivity.this)
                        .setTitle(R.string.title_payment_declined)
                        .setMessage(R.string.message_payment_declined)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                                CardPaymentActivity.this.setResult(RESULT_CANCELED);
                                CardPaymentActivity.this.finish();
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
                new AlertDialog.Builder(CardPaymentActivity.this)
                        .setTitle(R.string.title_payment_failure)
                        .setMessage(R.string.message_payment_declined)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                                CardPaymentActivity.this.setResult(RESULT_CANCELED);
                                CardPaymentActivity.this.finish();
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
                new AlertDialog.Builder(CardPaymentActivity.this)
                        .setTitle(R.string.title_payment_cancelled)
                        .setMessage(R.string.message_payment_cancelled)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                                CardPaymentActivity.this.setResult(RESULT_CANCELED);
                                CardPaymentActivity.this.finish();
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
                mMainRightView.setVisibility(View.GONE);
                mFinishRightView.setVisibility(View.VISIBLE);

                switch (methodId) {
                    case DELIVERY_METHOD_EMAIL:
                        new AlertDialog.Builder(CardPaymentActivity.this).setMessage("Send email" +
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
                        new AlertDialog.Builder(CardPaymentActivity.this).setMessage("Send sms" +
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
                        FinishFragment fragment = (FinishFragment) getSupportFragmentManager().findFragmentById(R.id.finish_fragment);
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

    @Override
    public void onAmountAdded(@NonNull BigDecimal addedAmount) {
        mAmount = mAmount.add(addedAmount);
        updateAmount();
    }

}

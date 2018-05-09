package com.verifone.swordfish.manualtransaction.gui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.verifone.swordfish.manualtransaction.IBridgeListener;
import com.verifone.swordfish.manualtransaction.ManualTransactionApplication;
import com.verifone.swordfish.manualtransaction.R;
import com.verifone.swordfish.manualtransaction.tools.Utils;
import com.verifone.utilities.ConversionUtility;

import java.math.BigDecimal;

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
 * Created by abey on 1/12/2018.
 */

public class SplitPaymentActivity extends BaseListenerActivity implements View.OnClickListener, IBridgeListener {

    private static final int REQUEST_CODE_CASH = 12101;
    private static final int REQUEST_CODE_CHARGE = 12102;

    private TextView mTotalAmountValue;
    private TextView mCashReceivedValue;
    private TextView mCreditChargedValue;
    private TextView mBalanceTitle;
    private TextView mBalanceValue;
    private NumericEditText mEnteredValue;

    private Button mChargeBtn;
    private Button mBackBtn;
    private Button mCashBtn;

    private BigDecimal mInitialAmount = BigDecimal.ZERO;
    private BigDecimal mCashReceivedAmount = BigDecimal.ZERO;
    private BigDecimal mCreditChargedAmount = BigDecimal.ZERO;
    private BigDecimal mEnteredAmount = BigDecimal.ZERO;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_split_payment);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        if (savedInstanceState != null) {
            mInitialAmount = (BigDecimal) savedInstanceState.getSerializable(PaymentActivity.AMOUNT_KEY);
        } else if (getIntent().getExtras() != null) {
            mInitialAmount = (BigDecimal) getIntent().getExtras().getSerializable(PaymentActivity.AMOUNT_KEY);
        }
        if (mInitialAmount == null) {
            throw new RuntimeException("Amount for mTransaction is not specified");
        }

        mTotalAmountValue = findViewById(R.id.textViewAmountToPayLabel);
        mCashReceivedValue = findViewById(R.id.textViewCashReceivedAmount);
        mCreditChargedValue = findViewById(R.id.textViewCreditChargedAmount);
        mBalanceTitle = findViewById(R.id.textBalanceView);
        mBalanceValue = findViewById(R.id.textRemainBalanceView);
        mEnteredValue = findViewById(R.id.textViewEnteredAmount);

        mBackBtn = findViewById(R.id.btn_left);
        mCashBtn = findViewById(R.id.btn_right);
        mChargeBtn = findViewById(R.id.btn_middle);
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) mCashBtn.getLayoutParams();
        layoutParams.setMarginEnd(0);
        layoutParams.setMargins(0, 0, 0, 0);
        mChargeBtn.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL | Gravity.END);
        mChargeBtn.setLayoutParams(layoutParams);

        mBackBtn.setText(R.string.back_label);
        mBackBtn.setAllCaps(true);
        mCashBtn.setText(R.string.cash_label);
        mCashBtn.setAllCaps(true);
        mChargeBtn.setText(R.string.charge_label);
        mChargeBtn.setAllCaps(true);


        mBackBtn.setOnClickListener(this);
        mCashBtn.setOnClickListener(this);
        mChargeBtn.setOnClickListener(this);

        mEnteredValue.addTextChangedListener(getTextWatcher());
        mEnteredValue.setRepresentationType(NumericEditText.RepresentationType.CURRENCY);

        ManualTransactionApplication.getCarbonBridge().setListener(this);

        refreshUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mEnteredValue.requestFocus();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(PaymentActivity.AMOUNT_KEY, mInitialAmount);
        super.onSaveInstanceState(outState);
    }

    public TextWatcher getTextWatcher() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    mEnteredAmount = ConversionUtility.parseAmount(mEnteredValue.getValue());
                } catch (NumberFormatException e) {
                    mEnteredAmount = BigDecimal.ZERO;
                }
                refreshUI();
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
                    cashPayment();
                }
                break;
            case R.id.btn_middle:
                if (view.getVisibility() == View.VISIBLE) {
                    chargePayment();
                }
                break;
            case R.id.btn_left:
                goBack();
                break;
        }
    }

    private void cashPayment() {
        Intent cashIntent = new Intent(this, CashPaymentActivity.class);
        cashIntent.putExtra(PaymentActivity.AMOUNT_KEY, mEnteredAmount);
        startActivityForResult(cashIntent, REQUEST_CODE_CASH);
    }

    private void chargePayment() {
        Intent chargeIntent = new Intent(this, CardPaymentActivity.class);
        chargeIntent.putExtra(PaymentActivity.AMOUNT_KEY, mEnteredAmount);
        startActivityForResult(chargeIntent, REQUEST_CODE_CHARGE);
    }

    private void goBack() {
        finish();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        ManualTransactionApplication.getCarbonBridge().setListener(this);

        if (requestCode == REQUEST_CODE_CASH || requestCode == REQUEST_CODE_CHARGE) {
            switch (resultCode) {
                case RESULT_OK:
                    mEnteredAmount = BigDecimal.ZERO;

                    BigDecimal receivedAmount = (BigDecimal) data.getSerializableExtra(PaymentActivity.AMOUNT_KEY);

                    switch (requestCode) {
                        case REQUEST_CODE_CASH:
                            mCashReceivedAmount = mCashReceivedAmount.add(receivedAmount);
                            break;
                        case REQUEST_CODE_CHARGE:
                            mCreditChargedAmount = mCreditChargedAmount.add(receivedAmount);
                            break;
                        default:
                            throw new IllegalStateException("Received type of payment not supported by Split payment!");
                    }

                    refreshUI();

                    if (getBalanceAmount().floatValue() == 0f) {
                        setResult(StartActivity.RESULT_OK);
                        finish();
                    }
                    break;
                case StartActivity.RESULT_TRANSACTION_CANCELED:
                    setResult(StartActivity.RESULT_TRANSACTION_CANCELED);
                    finish();
                    break;
            }
        }
    }

    private BigDecimal getBalanceAmount() {
        return mInitialAmount.subtract(mCashReceivedAmount).subtract(mCreditChargedAmount);
    }

    private void refreshUI() {
        mTotalAmountValue.setText(Utils.getLocalizedAmount(mInitialAmount));

        BigDecimal currentBalance = getBalanceAmount().subtract(mEnteredAmount);

        mBalanceTitle.setText(R.string.balance_label);
        mBalanceTitle.setAllCaps(true);
        mBalanceValue.setText(Utils.getLocalizedAmount(currentBalance));

        mCashReceivedValue.setText(Utils.getLocalizedAmount(mCashReceivedAmount));
        mCreditChargedValue.setText(Utils.getLocalizedAmount(mCreditChargedAmount));

        if (currentBalance.floatValue() < 0f || mEnteredAmount.floatValue() == 0f) {
            mCashBtn.setEnabled(false);
            mChargeBtn.setEnabled(false);
        } else {
            mCashBtn.setEnabled(true);
            mChargeBtn.setEnabled(true);
        }
    }

    @Override
    public void sessionStopped() {
        setResult(StartActivity.RESULT_TRANSACTION_CANCELED);
        finish();
    }

}

package com.verifone.swordfish.manualtransaction.gui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import com.verifone.swordfish.manualtransaction.IBridgeListener;
import com.verifone.swordfish.manualtransaction.ManualTransactionApplication;
import com.verifone.swordfish.manualtransaction.R;
import com.verifone.swordfish.manualtransaction.tools.BasketUtils;

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

public class PaymentActivity extends BaseListenerActivity
        implements IBridgeListener, View.OnClickListener {

    public static final String AMOUNT_KEY = "amount";

    private Button mBackBtn;
    private Button mCancelTransactionBtn;
    private Button mSplitBtn;
    private Button mCashBtn;
    private Button mChargeBtn;
    private Button mPhoneBtn;

    private OrderListFragment mOrderListFragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        ManualTransactionApplication.getCarbonBridge().setListener(this);

        mBackBtn = findViewById(R.id.btn_right);
        mSplitBtn = findViewById(R.id.btn_middle);
        mCancelTransactionBtn = findViewById(R.id.btn_left);
        mCashBtn = findViewById(R.id.cashButton);
        mChargeBtn = findViewById(R.id.chargeCreditButton);
        mPhoneBtn = findViewById(R.id.phoneOrderButton);

        mBackBtn.setText(R.string.back_label);
        mBackBtn.setAllCaps(true);
        mSplitBtn.setText(R.string.split_label);
        mSplitBtn.setAllCaps(true);
        mCancelTransactionBtn.setText(R.string.cancel_transaction_btn);
        mCancelTransactionBtn.setAllCaps(true);

        mBackBtn.setOnClickListener(this);
        mSplitBtn.setOnClickListener(this);
        mCancelTransactionBtn.setOnClickListener(this);
        mCashBtn.setOnClickListener(this);
        mChargeBtn.setOnClickListener(this);
        mPhoneBtn.setOnClickListener(this);

        mOrderListFragment = OrderListFragment.getInstance(false);
        getSupportFragmentManager().beginTransaction().add(R.id.container, mOrderListFragment).commit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        ManualTransactionApplication.getCarbonBridge().setListener(this);

        if (requestCode == StartActivity.GENERAL_REQUEST_CODE) {
            switch (resultCode) {
                case RESULT_OK:
                    setResult(StartActivity.RESULT_OK);
                    finish();
                    break;
                case StartActivity.RESULT_TRANSACTION_CANCELED:
                    setResult(StartActivity.RESULT_TRANSACTION_CANCELED);
                    finish();
                    break;
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_right:
                //back button
                goBack();
                break;
            case R.id.btn_middle:
                //split button
                splitPayment();
                break;
            case R.id.btn_left:
                //cancel transaction button
                cancelTransaction();
                break;
            case R.id.cashButton:
                cashPayment();
                break;
            case R.id.chargeCreditButton:
                chargePayment();
                break;
            case R.id.phoneOrderButton:
                phonePayment();
                break;
        }
    }

    private void goBack() {
        finish();
    }

    private void splitPayment() {
        Intent splitIntent = new Intent(this, SplitPaymentActivity.class);
        splitIntent.putExtra(AMOUNT_KEY, BasketUtils.calculateTotalAmount());
        startActivityForResult(splitIntent, StartActivity.GENERAL_REQUEST_CODE);
    }

    private void cancelTransaction() {
        showDialogWithMessage(getString(R.string.title_ending_session), false);
        ManualTransactionApplication.getCarbonBridge().cancelTransaction();
        ManualTransactionApplication.getCarbonBridge().stopSession();
    }

    private void cashPayment() {
        Intent cashIntent = new Intent(this, CashPaymentActivity.class);
        cashIntent.putExtra(AMOUNT_KEY, BasketUtils.calculateTotalAmount());
        startActivityForResult(cashIntent, StartActivity.GENERAL_REQUEST_CODE);
    }

    private void chargePayment() {
        Intent chargeIntent = new Intent(this, CardPaymentActivity.class);
        chargeIntent.putExtra(AMOUNT_KEY, BasketUtils.calculateTotalAmount());
        startActivityForResult(chargeIntent, StartActivity.GENERAL_REQUEST_CODE);
    }

    private void phonePayment() {
        Intent chargeIntent = new Intent(this, CardPaymentActivity.class);
        chargeIntent.putExtra(AMOUNT_KEY, BasketUtils.calculateTotalAmount());
        chargeIntent.putExtra(CardPaymentActivity.MANUAL_PAYMENT_KEY, true);
        startActivityForResult(chargeIntent, StartActivity.GENERAL_REQUEST_CODE);
    }

    /**
     * Carbon bridge listener
     */

    @Override
    public void sessionStopped() {
        hideDialog();
        setResult(StartActivity.RESULT_TRANSACTION_CANCELED);
        finish();
    }

    @Override
    public void merchandiseAdded() {
        mOrderListFragment.updateBasket();
    }

    @Override
    public void merchandiseUpdated() {
        mOrderListFragment.updateBasket();
    }

    @Override
    public void merchandiseDeleted() {
        mOrderListFragment.updateBasket();
    }

}

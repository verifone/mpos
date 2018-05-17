package com.verifone.swordfish.manualtransaction.gui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.annotation.NonNull;

import com.verifone.commerce.entities.Payment;
import com.verifone.swordfish.manualtransaction.IBridgeListener;
import com.verifone.swordfish.manualtransaction.ManualTransactionApplication;
import com.verifone.swordfish.manualtransaction.R;

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
 * Created by evgeniag1 on 03/26/2018.
 */

public abstract class BaseListenerActivity extends BaseActivity implements IBridgeListener {

    private AlertDialog mSessionErrorDialog;

    @Override
    protected void onDestroy() {
        if (mSessionErrorDialog != null && mSessionErrorDialog.isShowing()) {
            mSessionErrorDialog.dismiss();
        }
        super.onDestroy();
    }

    /**
     * Carbon bridge listener. Override necessary callbacks in child classes.
     */

    @Override
    public void sessionStarted() {

    }

    @Override
    public void sessionStopped() {

    }

    @Override
    public void merchandiseAdded() {

    }

    @Override
    public void merchandiseUpdated() {

    }

    @Override
    public void merchandiseDeleted() {

    }

    @Override
    public void basketAdjusted() {

    }

    @Override
    public void basketFinalized() {

    }

    @Override
    public void onPaymentSuccess(Payment payment) {

    }

    @Override
    public void onPaymentDecline() {

    }

    @Override
    public void onPaymentFailure() {

    }

    @Override
    public void onPaymentCanceled() {

    }

    @Override
    public void onReceiptMethodSelected(int methodId, String recipient) {

    }

    @Override
    public void onTransactionCanceled() {

    }

    @Override
    public void onAmountAdded(@NonNull BigDecimal addedAmount) {

    }

    @Override
    public void onTransactionEnded(boolean isSuccessful) {

    }

    @Override
    public void sessionStartFailed() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSessionErrorDialog = new AlertDialog.Builder(BaseListenerActivity.this)
                        .setTitle(R.string.title_payment_seesion_error)
                        .setMessage(R.string.message_payment_session_failed)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                sessionStopped();
                                ManualTransactionApplication.getCarbonBridge().stopSession();
                            }
                        }).create();
                hideDialog();
                mSessionErrorDialog.show();
            }
        });
    }

    @Override
    public void reconcileSuccess() {

    }

    @Override
    public void reconcileFailed(String message) {

    }

}

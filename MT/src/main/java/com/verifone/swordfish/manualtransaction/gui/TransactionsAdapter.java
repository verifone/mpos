package com.verifone.swordfish.manualtransaction.gui;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.verifone.commerce.entities.Payment;
import com.verifone.commerce.entities.Transaction;
import com.verifone.swordfish.manualtransaction.ITransactionHistoryListener;
import com.verifone.swordfish.manualtransaction.R;
import com.verifone.swordfish.manualtransaction.TransactionStorage;
import com.verifone.swordfish.manualtransaction.tools.LocalizeCurrencyFormatter;

import java.math.BigDecimal;
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

class TransactionsAdapter extends RecyclerView.Adapter<TransactionsAdapter.ViewHolder> {

    private List<Transaction> mTransactions;

    private SimpleDateFormat mSimpleDateFormatFrom = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.getDefault());
    private SimpleDateFormat mSimpleDateFormatTo = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss", Locale.getDefault());
    private SimpleDateFormat mSimpleDateFormatToShort = new SimpleDateFormat("MM/dd hh:mm", Locale.getDefault());

    private ResourcesHolder mResourcesHolder;

    private ITransactionHistoryListener mTransactionHistoryListener;

    static class ResourcesHolder {
        String mVoidCaption;
        String mRefundCaption;
        String mRejectedCaption;
        String mVoidDeclined;
        String mRefundDeclined;
        String mNotAvailable;
        String mRefund;
        String mVoid;

        SharedPreferences mStatusInfoPrefs;

        public ResourcesHolder(Resources resources, SharedPreferences statusInfoPrefs) {
            mVoidCaption = resources.getString(R.string.void_label);
            mRefundCaption = resources.getString(R.string.refund_title_textView);
            mRejectedCaption = resources.getString(R.string.rejected_label);
            mVoidDeclined = resources.getString(R.string.void_declined_label);
            mRefundDeclined = resources.getString(R.string.refund_declined_label);
            mNotAvailable = resources.getString(R.string.not_available);
            mRefund = resources.getString(R.string.str_refund);
            mVoid = resources.getString(R.string.void_label);

            mStatusInfoPrefs = statusInfoPrefs;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView mTextViewPaymentType;
        TextView mTextViewDate;
        TextView mTextViewTransactionId;
        TextView mTextViewDetailedDate;
        TextView mTextViewTotal;

        Button mButtonPrintReceipt;
        Button mButtonRefund;

        ViewHolder(View v) {
            super(v);

            mTextViewPaymentType = v.findViewById(R.id.textViewPaymentType);
            mTextViewDate = v.findViewById(R.id.textViewTHDate);
            mTextViewTransactionId = v.findViewById(R.id.textViewTransactionID);
            mTextViewDetailedDate = v.findViewById(R.id.textViewDetailDate);
            mTextViewTotal = v.findViewById(R.id.textViewDetailTotal);
            mButtonPrintReceipt = v.findViewById(R.id.historyPrintButton);
            mButtonRefund = v.findViewById(R.id.historyRefundButton);
        }
    }


    public TransactionsAdapter(List<Transaction> transactions, ITransactionHistoryListener iTransactionHistoryListener) {
        mTransactions = transactions;
        mTransactionHistoryListener = iTransactionHistoryListener;
    }

    public void setTransactions(List<Transaction> transactions) {
        mTransactions = transactions;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        View inflatedView = LayoutInflater.from(context).inflate(R.layout.transaction_history_cell, parent, false);

        SharedPreferences statusInfoPrefs = TransactionStorage.getStatusInfoPrefs(parent.getContext().getApplicationContext());
        mResourcesHolder = new ResourcesHolder(context.getResources(), statusInfoPrefs);

        return new ViewHolder(inflatedView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.mButtonRefund.setEnabled(false);

        Transaction transaction = mTransactions.get(position);
        ArrayList<Payment> payments = transaction.getPayments();
        if (payments != null && payments.size() > 0) {
            Payment payment = payments.get(payments.size() - 1);

            LocalizeCurrencyFormatter formatter = LocalizeCurrencyFormatter.getInstance();
            StringBuilder paymentTypeSB = new StringBuilder();
            StringBuilder amountBuilder = new StringBuilder();
            StringBuilder descriptionBuilder = new StringBuilder();

            //for (Payment payment : payments) {
            BigDecimal paymentAmount = payment.getPaymentAmount();
            paymentAmount = paymentAmount == null ? payment.getRequestedPaymentAmount() : paymentAmount;
            paymentAmount = paymentAmount != null ? paymentAmount : BigDecimal.ZERO;
            amountBuilder.append(amountBuilder.length() == 0 ? "" : "|").append(formatter.getCurrencyFormat().format(paymentAmount));

            Payment.PaymentType paymentType = payment.getPaymentType();
            paymentTypeSB.append(paymentTypeSB.length() == 0 ? "" : " | ").append(paymentType.name());

            Date parsedDate = null;
            try {
                String timestamp = payment.getTimestamp();
                if (!TextUtils.isEmpty(timestamp)) {
                    parsedDate = mSimpleDateFormatFrom.parse(timestamp);
                }
            } catch (ParseException ignored) {
            }

            String descriptionText = transaction.getInvoiceId();
            String authResult = null;

            String refundCaption = mResourcesHolder.mRefund;

            switch (payment.getAuthResult()) {
                case AUTHORIZED_ONLINE:
                case AUTHORIZED_OFFLINE:
                case AUTHORIZED_EXTERNALLY:
                case CASH_VERIFIED:
                    holder.mButtonRefund.setEnabled(true);

                    switch (paymentType) {
                        case CASH:
                            refundCaption = mResourcesHolder.mRefund;
                            break;
                        case CREDIT:
                        case DEBIT:
                        case STORED_VALUE:
                        case EBT:
                            long lastReconcileDate = TransactionStorage.getLastReconcileDate(mResourcesHolder.mStatusInfoPrefs);
                            if (parsedDate == null || parsedDate.getTime() <= lastReconcileDate) {
                                refundCaption = mResourcesHolder.mRefund;
                            } else {
                                refundCaption = mResourcesHolder.mVoid;
                            }
                            break;
                        case ALTERNATE:
                        case CHECK:
                            break;
                    }
                    break;
                case REJECTED_ONLINE:
                case REJECTED_OFFLINE:
                case USER_CANCELLED:
                case CANCELLED_EXTERNALLY:
                    authResult = mResourcesHolder.mRejectedCaption;
                    break;
                case VOIDED:
                    authResult = mResourcesHolder.mVoidCaption;
                    break;
                case REFUNDED:
                    authResult = mResourcesHolder.mRefundCaption;
                    break;
                case VOID_DECLINED:
                    authResult = mResourcesHolder.mVoidDeclined;
                    break;
                case REFUND_DECLINED:
                    authResult = mResourcesHolder.mRefundDeclined;
                    break;
            }

            descriptionText = descriptionText + (authResult != null ? ("(" + authResult + ")") : "");
            descriptionBuilder.append(amountBuilder.length() == 0 ? "" : "|").append(descriptionText);
            //}


            holder.mTextViewPaymentType.setText(paymentTypeSB.toString());


            if (parsedDate != null) {
                holder.mTextViewDate.setText(mSimpleDateFormatToShort.format(parsedDate));
                holder.mTextViewDetailedDate.setText(mSimpleDateFormatTo.format(parsedDate));
            } else {
                holder.mTextViewDate.setText(mResourcesHolder.mNotAvailable);
                holder.mTextViewDetailedDate.setText(mResourcesHolder.mNotAvailable);
            }


            holder.mTextViewTransactionId.setText(descriptionBuilder.toString());

            holder.mTextViewTotal.setText(amountBuilder.toString());

            holder.mButtonPrintReceipt.setTag(position);
            holder.mButtonPrintReceipt.setOnClickListener(getOnPrintButtonClickListener());

            holder.mButtonRefund.setTag(position);
            holder.mButtonRefund.setOnClickListener(getOnRefundClickListener());
            holder.mButtonRefund.setText(refundCaption);
        }
    }

    private View.OnClickListener getOnRefundClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Object tag = view.getTag();
                if (tag == null) {
                    return;
                }

                int position = (int) tag;

                if (mTransactions != null && position < mTransactions.size() && mTransactionHistoryListener != null) {
                    Transaction transaction = mTransactions.get(position);
                    mTransactionHistoryListener.onRefund(transaction);
                }

            }
        };
    }

    private View.OnClickListener getOnPrintButtonClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Object tag = view.getTag();
                if (tag == null) {
                    return;
                }

                int position = (int) tag;

                if (mTransactions != null && position < mTransactions.size() && mTransactionHistoryListener != null) {
                    Transaction transaction = mTransactions.get(position);
                    mTransactionHistoryListener.onPrint(transaction);
                }
            }
        };
    }


    @Override
    public int getItemCount() {
        return mTransactions.size();
    }
}

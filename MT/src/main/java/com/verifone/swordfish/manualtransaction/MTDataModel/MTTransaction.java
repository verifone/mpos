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

package com.verifone.swordfish.manualtransaction.MTDataModel;

import android.support.annotation.NonNull;
import android.util.Log;

import com.verifone.commerce.entities.AmountTotals;
import com.verifone.commerce.entities.Merchandise;
import com.verifone.commerce.entities.Payment;
import com.verifone.commerce.entities.Transaction;
import com.verifone.swordfish.manualtransaction.HistoryFragments.TransactionStatus;
import com.verifone.swordfish.manualtransaction.System.PaymentTerminal;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;


public class MTTransaction {

    private static final String TAG = MTTransaction.class.getSimpleName();
    private List<Merchandise> merchandises;
    private BigDecimal transactionTotal = BigDecimal.ZERO;
    private BigDecimal transactionTax = BigDecimal.ZERO;
    private BigDecimal transactionDiscount = BigDecimal.ZERO;
    private MTPayments mtPayment;
    private TransactionStatus transactionStatus = TransactionStatus.inProgress;
    private Transaction associatedTransaction;
    private String mNote;
    private String mTransactionId;
    private String mTransactionDate;
    private String mTransactionVoidRefundDate;
    private String mInvoiceId;

    public void setStatus(TransactionStatus status) {
        transactionStatus = status;
    }

    public void setStatus(String status) {
        TransactionStatus txStatus = TransactionStatus.completed;
        if (status.equals("completed")) {
            txStatus = TransactionStatus.completed;
        }
        if (status.equals("voided")) {
            txStatus = TransactionStatus.voided;
        }
        if (status.equals("refunded")) {
            txStatus = TransactionStatus.refunded;
        }
        if (status.equals("completed")) {
            txStatus = TransactionStatus.completed;
        }
        this.transactionStatus = txStatus;
    }

    public TransactionStatus getStatus() {
        return transactionStatus;
    }

    public void setNote(String note) {
        mNote = note;
    }

    public String getNote() {
        return mNote;
    }

    public void setTransactionId(String transactionId) {
        mTransactionId = transactionId;
    }

    public String getTransactionId() {
        return mTransactionId;
    }

    public void setInvoiceId(String invoice) {
        mInvoiceId = invoice;
    }

    public String getInvoiceId() {
        return mInvoiceId;
    }

    void setDate() {
        SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a", Locale.getDefault());
        Date today = Calendar.getInstance().getTime();
        mTransactionDate = df.format(today);
    }

    public void setDate(String date) {
        mTransactionDate = date;
    }

    public String getDate() {
        return mTransactionDate;
    }

    public void addMerchandise(Merchandise merchandise) {
        if (merchandises == null) {
            merchandises = new ArrayList<Merchandise>();
        }
        if (transactionTotal == null) {
            transactionTotal = new BigDecimal("0.00");
        }
        if (transactionTax == null) {
            transactionTax = new BigDecimal("0.00");
        }
        if (transactionDiscount == null) {
            transactionDiscount = new BigDecimal("0.00");
        }
        if (merchandise.getTax() != null) {
            transactionTax = transactionTax.add(merchandise.getTax());
        }
        if (merchandise.getDiscount() != null) {
            transactionDiscount = transactionDiscount.add(merchandise.getDiscount());
        }
        int qty = merchandise.getQuantity();
        transactionTotal = transactionTotal.add(merchandise.getUnitPrice().multiply(new BigDecimal(qty)));
        merchandises.add(merchandise);
    }

    public void updateMerchandise(Merchandise merchandise) {

        BigDecimal newTotal = BigDecimal.ZERO;
        if (merchandise != null) {
            BigDecimal quantity = new BigDecimal(merchandise.getQuantity());

            if (merchandise.getAmount() != null)
                newTotal = newTotal.add(merchandise.getUnitPrice().multiply(quantity));

            if (merchandise.getDiscount() != null && !Objects.equals(merchandise.getDiscount(), BigDecimal.ZERO)) {
                newTotal = newTotal.subtract(merchandise.getDiscount());
            }
            if (merchandise.getTax() != null && !Objects.equals(merchandise.getTax(), BigDecimal.ZERO)) {
                transactionTotal = newTotal.add(merchandise.getTax());
            }
        }
    }

    public void removeMerchandise(Merchandise merchandise) {
        if (merchandise.getTax() != null) {
            transactionTax = transactionTax.subtract(merchandise.getTax());
        }
        if (merchandise.getDiscount() != null) {
            transactionDiscount = transactionDiscount.subtract(merchandise.getDiscount());
        }
        int qty = merchandise.getQuantity();
        transactionTotal = transactionTotal.subtract(merchandise.getUnitPrice().multiply(new BigDecimal(qty)));
        merchandises.remove(merchandise);
    }

    private void updateBasket(List<Merchandise> merchandize) {
        Merchandise[] m = merchandize.toArray(new Merchandise[merchandize.size()]);
        PaymentTerminal.getInstance().setMerchandizes(m);
    }


    public Merchandise[] getConvertedMerchandise(MTTransaction merch) {
        return merch.transactionMerchandises().toArray
                (new Merchandise[merch.transactionMerchandises().size()]);
    }

    public void addPayment(Payment payments) {
        if (mtPayment == null) {
            mtPayment = new MTPayments();
        }
        mtPayment.addPayment(payments);
        if (mTransactionId == null) {
            PaymentTerminal paymentTerminal = PaymentTerminal.getInstance();
            String txId = paymentTerminal.transactionID();
            if (txId != null) {
                mTransactionId = txId;
            }
        }
    }

    public void removePayment(Payment payments) {
        mtPayment.removePayment(payments);

    }

    public MTPayments transactionPayments() {
        return mtPayment;
    }

    public List<Merchandise> transactionMerchandises() {
        return merchandises;
    }

    public void setTransactionTotal(BigDecimal total) {
        transactionTotal = total;
    }

    public BigDecimal getTransactionTotal() {
        if (merchandises == null || merchandises.size() == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Merchandise merchandise : merchandises) {
            BigDecimal quantity = new BigDecimal(merchandise.getQuantity());
            total = total.add(merchandise.getUnitPrice().multiply(quantity));
            if (merchandise.getDiscount() != null && !Objects.equals(merchandise.getDiscount(), BigDecimal.ZERO)) {
                total = total.subtract(merchandise.getDiscount());
            }
            if (merchandise.getTax() != null && !Objects.equals(merchandise.getTax(), BigDecimal.ZERO)) {
                total = total.add(merchandise.getTax());
            }
        }
        transactionTotal = total;
        return transactionTotal;
    }

    public void setTransactionTax(String tax) {
        transactionTax = new BigDecimal(tax);
    }

    public void setTransactionDiscount(String discount) {
        transactionDiscount = new BigDecimal(discount);
    }

    public void setVoidDate(String date) {
        this.mTransactionVoidRefundDate = date;
    }

    public String getVoidDate() {
        return this.mTransactionVoidRefundDate;
    }

    public BigDecimal getTransactionTax() {
        return transactionTax;
    }

    public BigDecimal getTransactionDiscount() {
        return transactionDiscount;
    }

    public AmountTotals runningTotals() {
        return generateAmountTotals();
    }

    @NonNull
    private AmountTotals generateAmountTotals() {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;
        if (merchandises != null) {
            int index;
            for (index = 0; index < merchandises.size(); index++) {
                Merchandise merchandise = merchandises.get(index);
                if (merchandise.getAmount() != null)
                    total = total.add(merchandise.getUnitPrice().multiply(new BigDecimal(merchandise.getQuantity())));
                if (merchandise.getExtendedPrice() != null)
                    subtotal = subtotal.add(merchandise.getExtendedPrice().multiply(new BigDecimal(merchandise.getQuantity())));
                if (merchandise.getTax() != null)
                    tax = tax.add(merchandise.getTax());
                    tax = setScale(1, tax.toString());
                if (merchandise.getDiscount() != null && !Objects.equals(merchandise.getDiscount(), BigDecimal.ZERO)) {
                    discount = discount.add(merchandise.getDiscount());
                    discount = setScale(2, discount.toString());

                }
            }
        }

        transactionTotal = total.subtract(discount).add(tax);
        if (!Objects.equals(tax, BigDecimal.ZERO)) {
            transactionTax = tax;
        }
        if (!Objects.equals(discount, BigDecimal.ZERO)) {
            transactionDiscount = discount;
        }
        AmountTotals amountTotals = new AmountTotals();
        amountTotals.setRunningTotal(transactionTotal);
        amountTotals.setRunningTax(tax);
        amountTotals.setRunningSubtotal(subtotal);
        return amountTotals;
    }


    private static BigDecimal setScale(final int type, final String input) {
        BigDecimal bd = new BigDecimal(input);
        //bd.setScale(2, BigDecimal.ROUND_HALF_UP);   bd.setScale does not change bd
        bd = bd.setScale(type, BigDecimal.ROUND_HALF_UP);
        return bd;
    }

}

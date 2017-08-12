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

import com.verifone.swordfish.manualtransaction.HistoryFragments.TransactionStatus;

import java.util.ArrayList;
import java.util.List;


public class MTTransactionHistory {

    private static MTTransactionHistory instance = null;
    private static String TAG = MTTransactionHistory.class.getSimpleName();
    private List<MTTransaction> transactions;
    private MTTransaction pendingToAdd;

    protected MTTransactionHistory() {

    }

    public static MTTransactionHistory getInstance() {
        if (instance == null) {
            instance = new MTTransactionHistory();
        }
        return instance;
    }

    public void addTransaction(MTTransaction transaction) {
        if (transactions == null) {
            transactions = new ArrayList<>();
        }
        if (transaction.getDate() == null) {
            transaction.setDate();
        }
        transaction.setStatus(TransactionStatus.completed);
        pendingToAdd = transaction;
        transactions.add(transaction);
    }

    public List<MTTransaction> getTransactions() {
        return transactions;
    }
}

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

package com.verifone.swordfish.manualtransaction.HistoryFragments;

import java.util.ArrayList;
import java.util.List;


public enum TransactionStatus {

    inProgress(false, false, true, true, false),
    completed(true, true, false, false, true),

    splitInProgress(false, false, true, true, false),
    splitCompleted(true, true, false, false, true),

    voided(false, false, false, false, true),
    splitVoided(true, true, false, false, true),

    refunded(false, false, false, false, true),
    partiallyRefunded(false, true, false, false, true),

    refundHasStarted(false, false, true, true, false),
    refund(true, false, false, false, true),
    partialRefund(true, false, false, false, true);

    private boolean voidable;
    private boolean refundable;
    private boolean cancelable;
    private boolean payable;
    private boolean searchable;

    private TransactionStatus(boolean voidable, boolean refundable, boolean cancelable, boolean payable, boolean searchable) {
        this.voidable = voidable;
        this.refundable = refundable;
        this.cancelable = cancelable;
        this.payable = payable;
        this.searchable = searchable;
    }

    public boolean canBeVoided() {
        return this.voidable;
    }

    public boolean canBeRefunded() {
        return this.refundable;
    }

    public boolean canBeCanceled() {
        return this.cancelable;
    }

    public boolean canBePayed() {
        return this.payable;
    }

    public boolean isSearchable() {
        return this.searchable;
    }

    public static List<TransactionStatus> getSearchables() {
        List<TransactionStatus> result = new ArrayList();

        for (TransactionStatus status : values()) {
            if (status.isSearchable()) {
                result.add(status);
            }
        }
        return result;
    }

    public boolean isRefundAndCanBeProceededWithRefund() {
        return this == TransactionStatus.refundHasStarted || this == TransactionStatus.partialRefund;
    }

}

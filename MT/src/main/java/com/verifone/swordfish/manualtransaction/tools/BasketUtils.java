package com.verifone.swordfish.manualtransaction.tools;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.verifone.commerce.entities.Merchandise;
import com.verifone.commerce.entities.Offer;
import com.verifone.swordfish.manualtransaction.ManualTransactionApplication;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
 * Created by romans1 on 01/25/2018.
 */

public class BasketUtils {

    /**
     * Calculates total amount based on merchandises in current valid basket
     *
     * @return non-null total amount. BigDecimal.ZERO if 0.00 or can not be calculated.
     */
    @NonNull
    public static BigDecimal calculateTotalAmount() {
        BigDecimal total = BigDecimal.ZERO;
        List<Merchandise> merchandises = ManualTransactionApplication.getCarbonBridge().getMerchandises();
        if (merchandises != null) {
            for (Merchandise merchandise : merchandises) {
                total = total.add(merchandise.getAmount());
            }
        }
        return total;
    }

    /**
     * Helps to calculate new subtotal with applied offers.
     */
    @NonNull
    public static BigDecimal applyOffersFromEvent(@Nullable ArrayList<Offer> offersToApply, @NonNull BigDecimal currentSubtotal) {
        if (offersToApply == null) {
            return currentSubtotal;
        }

        Iterator<Offer> iterator = offersToApply.iterator();
        while (iterator.hasNext()) {
            Offer offer = iterator.next();
            BigDecimal amountToAdjustTotal = BigDecimal.ZERO;
            BigDecimal amount = offer.getOfferDiscount();
            BigDecimal percent = offer.getOfferPercentDiscount();

            if (amount != null) {
                // Use amount to update the total
                amountToAdjustTotal = amountToAdjustTotal.add(amount);
            } else if (percent != null) {
                // Use the percent to update the total.
                amountToAdjustTotal = amountToAdjustTotal.add(currentSubtotal.multiply(percent).negate());
            } else {
                iterator.remove();
                continue;
            }

            offer.setAmount(amountToAdjustTotal);

            // use the amountToAdjustTotal to update the displayed amount.
            currentSubtotal = currentSubtotal.add(amountToAdjustTotal);
        }
        return currentSubtotal;
    }
}

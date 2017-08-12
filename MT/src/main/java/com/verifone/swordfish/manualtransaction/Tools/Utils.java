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


package com.verifone.swordfish.manualtransaction.Tools;

import android.util.Log;

import com.verifone.commerce.entities.Merchandise;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.NumberFormat;

/**
 * Created by praweenk on 8/7/17.
 * Represents helper utility method.
 */

public class Utils {

    private static final String TAG = Utils.class.getSimpleName();

    public static final String STR_DOLLAR = "$";
    public static final String STR_PERCENT = "%";

    public static final String RADIO_BUTTON_MONEY_KEY = "MoneyChecked";
    public static final String RADIO_BUTTON_PERCENT_KEY = "PercentChecked";


    public static String getLocalizedAmount(BigDecimal amount) {
        NumberFormat formatter = LocalizeCurrencyFormatter.getInstance().getCurrencyFormat();
        return formatter.format(amount.doubleValue());
    }

    public static BigDecimal getDiscountPercentPrice(Merchandise item, String amount) {
        BigDecimal discount = new BigDecimal(amount, MathContext.DECIMAL32);
        discount = discount.divide(new BigDecimal("100"));
        Log.i(TAG, "discount : " + discount.toString());
        BigDecimal qty = new BigDecimal(item.getQuantity());
        BigDecimal value = item.getUnitPrice().multiply(qty).multiply(discount);
        value = value.setScale(2, BigDecimal.ROUND_HALF_UP);
        Log.i(TAG, "getDiscountPercentPrice : " + value.toString());
        return value;
    }

    public static BigDecimal getDiscountValueFromSalePrice(Merchandise item) {
        //Calculate
        BigDecimal originalPrice = item.getAmount();
        //BigDecimal originalPrice = currentItem.getExtendedPrice(); //20.00
        BigDecimal discount = item.getDiscount(); //2.00
        int quantity = item.getQuantity();

        BigDecimal salePrice = originalPrice.subtract(discount); // originalPrice - discount

        BigDecimal divisionValue = new BigDecimal("1").subtract(salePrice.divide(originalPrice, RoundingMode.CEILING));
        Log.i(TAG, "divisionValue : " + divisionValue.toString());
        BigDecimal disValue = divisionValue.multiply(new BigDecimal("100"));
        disValue = disValue.setScale(2, BigDecimal.ROUND_HALF_UP);
        Log.i(TAG, "getDiscountValueFromSalePrice : " + disValue.toString());
        return disValue;
    }
}

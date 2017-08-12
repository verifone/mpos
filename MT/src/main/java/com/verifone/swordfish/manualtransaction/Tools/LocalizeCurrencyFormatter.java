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

import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;


public class LocalizeCurrencyFormatter {
    private static LocalizeCurrencyFormatter instance;
    private NumberFormat currencyFormat;
    private String localCurrencySymbol;

    protected LocalizeCurrencyFormatter() {

    }

    public static LocalizeCurrencyFormatter getInstance() {
        if (instance == null) {
            instance = new LocalizeCurrencyFormatter();

        }
        return instance;
    }

    public void initFormatter() {
        Currency localCurrency = Currency.getInstance(Locale.getDefault());

        localCurrencySymbol = localCurrency.getSymbol();
        currencyFormat = NumberFormat.getCurrencyInstance();


        // We then tell our formatter to use this symbol.
        DecimalFormatSymbols decimalFormatSymbols = ((java.text.DecimalFormat) currencyFormat).getDecimalFormatSymbols();
        decimalFormatSymbols.setCurrencySymbol(localCurrencySymbol);
        ((java.text.DecimalFormat) currencyFormat).setDecimalFormatSymbols(decimalFormatSymbols);

    }

    public NumberFormat getCurrencyFormat() {
        if (currencyFormat == null) {
            initFormatter();
        }
        return currencyFormat;
    }

    public String getLocalCurrencyCodeSymbol() {
        return Currency.getInstance(Locale.getDefault()).getCurrencyCode();
    }

    public String getLocalCurrencySymbol() {
        return Currency.getInstance(Locale.getDefault()).getSymbol();
    }

}

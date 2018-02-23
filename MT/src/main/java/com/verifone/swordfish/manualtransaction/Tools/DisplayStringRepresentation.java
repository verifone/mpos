package com.verifone.swordfish.manualtransaction.tools;

import android.util.Log;

import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;
import java.util.Objects;

/**
 * Copyright (C) 2016,2017 Verifone, Inc.
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
 */


public class DisplayStringRepresentation {

    private String internalRepresentation;
    private boolean start = true;
    private String result;

    public void attachValue(String newValue, String type) {

        CharSequence attachedValue = null;

        if (!start) {
            //code for special characters

            if (internalRepresentation == null) {
                internalRepresentation = "0";
            }
            if (newValue.equals("Del") && internalRepresentation.length() > 0) {
                String newString = internalRepresentation;
                internalRepresentation = newString.substring(0, newString.length() - 1);
                attachedValue = convertToDisplay(internalRepresentation, type);

            } else if (newValue.equals("")) {
                internalRepresentation = newValue;
                attachedValue = convertToDisplay(internalRepresentation, type);

            } else {
                if (internalRepresentation.length() > 0) {
                    internalRepresentation = internalRepresentation + newValue;
                    attachedValue = convertToDisplay(internalRepresentation, type);
                }
            }
        } else {
            if (!newValue.equals("Del")) {
                internalRepresentation = newValue;
                attachedValue = convertToDisplay(internalRepresentation, type);
                start = false;
            } else {
                attachedValue = null;
            }
        }
        if (attachedValue != null && attachedValue.length() > 0) {
        } else {
            internalRepresentation = "";
            start = true;
        }

    }

    public String currentString() {
        return result;
    }

    private String convertToDisplay(String text, String type) {
        //Log.i("TYPE IS : ", type);

        if (!Objects.equals(type, "%")) {
            int counter = text.length();
            switch (counter) {
                case 1:
                    result = ".0" + text;
                    break;
                case 2:
                    result = "." + text;
                    break;
                case 0:
                    result = "";
                    break;
                default:
                    result = text.substring(0, text.length() - 2) + "." + text.substring(text.length() - 2, text.length());
                    break;
            }
        } else {

            result = text.length() > 0 ? text.substring(0, text.length()) + "%" : "";
            Log.i("DISPLAY STRING RESP", "result is : " + result);
        }
        return result;
    }

    public String formatLocalCurrencyAmount(float amount) {
        Currency localCurrency = Currency.getInstance(Locale.getDefault());
        String localCurrencySymbol = localCurrency.getSymbol();
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
        // We then tell our formatter to use this symbol.
        DecimalFormatSymbols decimalFormatSymbols = ((java.text.DecimalFormat) currencyFormat).getDecimalFormatSymbols();
        decimalFormatSymbols.setCurrencySymbol(localCurrencySymbol);
        ((java.text.DecimalFormat) currencyFormat).setDecimalFormatSymbols(decimalFormatSymbols);
        currencyFormat.setMaximumFractionDigits(0);
        return currencyFormat.format(amount);
    }
}

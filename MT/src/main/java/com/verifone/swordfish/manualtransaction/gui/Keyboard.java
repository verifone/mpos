package com.verifone.swordfish.manualtransaction.gui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.TableLayout;

import com.verifone.swordfish.manualtransaction.R;
import com.verifone.swordfish.manualtransaction.gui.NumericEditText.RepresentationType;

import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Currency;
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
 * Created by evgeniag1 on 03/26/2018.
 */

public abstract class Keyboard extends TableLayout {


    protected InputTextListener mInputTextListener;
    protected RepresentationType mRepresentationType;

    private boolean mStartPosition = true;
    private String mIntermediateResult = "";

    public Keyboard(Context context) {
        super(context);
    }

    public Keyboard(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    abstract void init();

    protected void setCurrentEditText(InputTextListener inputTextListener, NumericEditText.RepresentationType representationType) {
        mInputTextListener = inputTextListener;
        mRepresentationType = representationType;
        clearTempValues();
    }

    public void setRepresentationType(NumericEditText.RepresentationType representationType) {
        mRepresentationType = representationType;
    }

    @NonNull
    protected String getInputValue(String newButtonValue) {
        CharSequence temp = null;
        if (mStartPosition) {
            if (!newButtonValue.equals(getResources().getString(R.string.keyboard_btn_del))) {
                mIntermediateResult = newButtonValue;
                temp = newButtonValue;
                mStartPosition = false;
            } else {
                temp = null;
            }
        } else {
            if (newButtonValue.equals(getResources().getString(R.string.keyboard_btn_del)) && mIntermediateResult.length() > 0) {
                mIntermediateResult = mIntermediateResult.substring(0, mIntermediateResult.length() - 1);
                temp = mIntermediateResult;
            } else {
                if (mIntermediateResult.length() > 0) {
                    mIntermediateResult = mIntermediateResult + newButtonValue;
                    temp = mIntermediateResult;
                }
            }
        }

        if (temp != null && temp.length() > 0) {
        } else {
            mIntermediateResult = "";
            mStartPosition = true;
        }
        return mIntermediateResult;
    }

    @NonNull
    protected String convertToFloatingPointValue(String value) {
        String convertedResult;
        int counter = value.length();
        switch (counter) {
            case 1:
                convertedResult = "0.0" + value;
                break;
            case 2:
                convertedResult = "0." + value;
                break;
            case 0:
                convertedResult = "";
                break;
            default:
                convertedResult = value.substring(0, value.length() - 2) + "." + value.substring(value.length() - 2, value.length());
                break;
        }
        return convertedResult;
    }

    @NonNull
    protected String trimZeros(String value) {
        String trimmedResult = "";
        if (!TextUtils.isEmpty(value)) {
            trimmedResult = String.valueOf((int) Float.parseFloat(value));
        }
        return trimmedResult;
    }

    protected String formatLocalCurrencyAmount(String value) {
        Currency localCurrency = Currency.getInstance(Locale.getDefault());
        String localCurrencySymbol = localCurrency.getSymbol();
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
        // We then tell our formatter to use this symbol.
        DecimalFormatSymbols decimalFormatSymbols = ((java.text.DecimalFormat) currencyFormat).getDecimalFormatSymbols();
        decimalFormatSymbols.setCurrencySymbol(localCurrencySymbol);
        ((java.text.DecimalFormat) currencyFormat).setDecimalFormatSymbols(decimalFormatSymbols);
        currencyFormat.setMaximumFractionDigits(0);
        return currencyFormat.format(Double.valueOf(value));
    }

    protected void clearTempValues() {
        mIntermediateResult = "";
        mStartPosition = true;
    }

}

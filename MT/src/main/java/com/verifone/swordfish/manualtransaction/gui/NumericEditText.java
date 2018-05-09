package com.verifone.swordfish.manualtransaction.gui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatEditText;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;

import com.verifone.swordfish.manualtransaction.R;
import com.verifone.swordfish.manualtransaction.tools.LocalizeCurrencyFormatter;

import java.text.NumberFormat;
import java.util.ArrayList;
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
 * Created by evgeniag1 on 03/23/2018.
 */

public class NumericEditText extends AppCompatEditText implements InputTextListener {

    public enum RepresentationType {

        CURRENCY("0"),
        PERCENT("0"),
        QUANTITY("1"),
        PLAIN_TEXT("");

        private String defaultMinValue;

        RepresentationType(String defaultMinValue) {
            this.defaultMinValue = defaultMinValue;
        }

        public String getDefaultMinValueString() {
            return defaultMinValue;
        }
    }

    private RepresentationType mRepresentationType = RepresentationType.PLAIN_TEXT;

    private List<Keyboard> mKeyboards = new ArrayList<>();

    private String mValue = mRepresentationType.getDefaultMinValueString();
    private String mMinValue;

    public NumericEditText(Context context) {
        super(context);
        init();
    }

    public NumericEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setShowSoftInputOnFocus(false);
        disableContextMenu();
    }

    private void disableContextMenu() {
        setLongClickable(false);
        setCustomSelectionActionModeCallback(new android.view.ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(android.view.ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onPrepareActionMode(android.view.ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(android.view.ActionMode mode, MenuItem item) {
                return false;
            }

            @Override
            public void onDestroyActionMode(android.view.ActionMode mode) {

            }
        });
    }

    public void setRepresentationType(RepresentationType representationType) {
        this.mRepresentationType = representationType;
        clearValue();
    }

    public RepresentationType getRepresentationType() {
        return mRepresentationType;
    }

    public void setMinValue(@NonNull String minValue) {
        this.mMinValue = minValue;
    }

    @NonNull
    public String getValue() {
        return mValue;
    }

    public void clearValue() {
        setText(mRepresentationType.getDefaultMinValueString());
        if (!mKeyboards.isEmpty()) {
            for (Keyboard keyboard : mKeyboards) {
                keyboard.setRepresentationType(mRepresentationType);
                keyboard.clearTempValues();
            }
        }
    }

    public void setText(@NonNull String value) {
        NumberFormat numberFormat;
        String displayString;

        attachValue(value);

        switch (mRepresentationType) {
            case CURRENCY:
                numberFormat = LocalizeCurrencyFormatter.getInstance().getCurrencyFormat();
                displayString = numberFormat.format(Double.parseDouble(mValue));
                break;
            case PERCENT:
                numberFormat = NumberFormat.getPercentInstance(Locale.getDefault());
                displayString = numberFormat.format(Double.parseDouble(mValue));
                break;
            case QUANTITY:
            default:
                displayString = mValue;
                break;
        }
        super.setText(displayString);
    }

    private void attachValue(@NonNull String inputValue) {
        String valueToAttach;
        String minValue = mMinValue == null ? mRepresentationType.getDefaultMinValueString() : mMinValue;
        if (TextUtils.isEmpty(minValue)) {
            valueToAttach = inputValue;
        } else if (TextUtils.isEmpty(inputValue)) {
            valueToAttach = minValue;
        } else {
            boolean inRange = Float.valueOf(inputValue) >= Float.valueOf(minValue);
            valueToAttach = inRange ? inputValue : minValue;
        }
        mValue = valueToAttach;
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        if (mKeyboards.isEmpty()) {
            Keyboard numericKeyboard = ((Activity) getContext()).findViewById(R.id.numeric_keyboard);
            Keyboard moneyAmountKeyboard = ((Activity) getContext()).findViewById(R.id.money_amount_keyboard);
            if (numericKeyboard != null) {
                mKeyboards.add(numericKeyboard);
            }
            if (moneyAmountKeyboard != null) {
                mKeyboards.add(moneyAmountKeyboard);
            }
        }
        if (focused && (!mKeyboards.isEmpty())) {
            for (Keyboard keyboard : mKeyboards) {
                keyboard.setCurrentEditText(this, mRepresentationType);
            }
        }
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
    }

    @Override
    public void onSelectionChanged(int start, int end) {
        CharSequence text = getText();
        if (text != null) {
            int length = text.length();
            if (start != length || end != length) {
                setSelection(length, length);
                return;
            }
        }
        super.onSelectionChanged(start, end);
    }

    @Override
    public void onValueInputted(String inputValue) {
        setText(inputValue);
    }

}

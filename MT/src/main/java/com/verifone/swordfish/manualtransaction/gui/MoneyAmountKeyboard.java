package com.verifone.swordfish.manualtransaction.gui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

import com.verifone.swordfish.manualtransaction.R;

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

public class MoneyAmountKeyboard extends Keyboard {

    public MoneyAmountKeyboard(Context context) {
        super(context);
        init();
    }

    public MoneyAmountKeyboard(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    void init() {
        inflate(getContext(), R.layout.money_amount_keyboard, this);
        int[] buttonIds = new int[]{
                R.id.buttonThree, R.id.buttonFive,
                R.id.buttonTen, R.id.buttonTwenty,
                R.id.buttonFifty, R.id.buttonOneHundred};

        OnClickListener onMoneyAmountClickListener = getOnMoneyAmountClickListener();
        for (int id : buttonIds) {
            Button button = findViewById(id);
            button.setText(formatLocalCurrencyAmount((String) button.getTag()));
            button.setOnClickListener(onMoneyAmountClickListener);
        }
        setId(R.id.money_amount_keyboard);
    }

    public OnClickListener getOnMoneyAmountClickListener() {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mInputTextListener != null) {
                    String newButtonValue = (String) v.getTag();
                    String inputValue = getInputValue(newButtonValue);
                    mInputTextListener.onValueInputted(convertToFloatingPointValue(inputValue));
                    clearTempValues();
                }
            }
        };
    }

    @Override
    @NonNull
    protected String convertToFloatingPointValue(String value) {
        return value + ".00";
    }

}

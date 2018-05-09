package com.verifone.swordfish.manualtransaction.tools;

import android.content.Context;
import android.content.SharedPreferences;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

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

public class Utils {

    private static final String TAG = Utils.class.getSimpleName();

    public static final String PREFS_NAME = "configs";
    public static final String PREFS_KEY_IP = "configs_ip";
    public static final String PREFS_KEY_PORT = "configs_port";

    public static String getLocalizedAmount(BigDecimal amount) {
        NumberFormat formatter = LocalizeCurrencyFormatter.getInstance().getCurrencyFormat();
        return formatter.format(amount.doubleValue());
    }

    public static String getTerminalIP(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, 0);
        return sharedPreferences.getString(PREFS_KEY_IP, "192.168.50.2");
    }

    public static void saveTerminalIP(Context context, String terminalIP) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, 0);
        sharedPreferences.edit().putString(PREFS_KEY_IP, terminalIP).apply();
    }

    public static String getTerminalPort(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, 0);
        return sharedPreferences.getString(PREFS_KEY_PORT, null);
    }

    public static void saveTerminalPort(Context context, String terminalPort) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, 0);
        sharedPreferences.edit().putString(PREFS_KEY_PORT, terminalPort).apply();
    }

    public static CharSequence getCurrencySymbol() {
        return Currency.getInstance(Locale.getDefault()).getSymbol();
    }
}

package com.verifone.swordfish.manualtransaction;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.reflect.TypeToken;
import com.verifone.commerce.entities.Payment;
import com.verifone.commerce.entities.Transaction;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;

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
 * Created by romans1 on 01/23/2018.
 */

public class TransactionStorage {

    private static final String TAG = TransactionStorage.class.getSimpleName();

    private static final String STORAGE_PATH_NAME = "Transactions";

    private static final String STATUS_INFO_PREFS_NAME = "StatusInfoPrefs";
    private static final String KEY_RECONCILE_TIME = "RECONCILE_TIME";

    private final Type mTransactionListType;
    private final Gson mGson;
    private final Context mContext;

    public TransactionStorage(Context appContext) {
        mContext = appContext;

        ExclusionStrategy exclusionStrategy = new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes fieldAttributes) {
                if (fieldAttributes.getName().equals("mSignature")) {
                    return true;
                }
                return false;
            }

            @Override
            public boolean shouldSkipClass(Class<?> aClass) {
                return false;
            }
        };

        mGson = new GsonBuilder()
                .addSerializationExclusionStrategy(exclusionStrategy)
                .addDeserializationExclusionStrategy(exclusionStrategy)
                .disableHtmlEscaping().create();

        mTransactionListType = new TypeToken<Collection<Transaction>>() {
        }.getType();
    }

    @NonNull
    private File getStoragePath(@NonNull Context applicationContext) {
        return new File(applicationContext.getCacheDir().getPath() + "/" + STORAGE_PATH_NAME);
    }

    public synchronized void saveTransaction(@NonNull Transaction transaction) {
        ArrayList<Transaction> transactions = readAllTransactions();
        transactions.add(transaction);

        writeToStorage(transactions);
    }

    @NonNull
    public synchronized ArrayList<Transaction> readAllTransactions() {
        System.gc();
        ArrayList<Transaction> transactions = null;

        try (Reader reader = new FileReader(getStoragePath(mContext))) {
            transactions = mGson.fromJson(reader, mTransactionListType);
        } catch (IOException | JsonIOException e) {
            Log.e(TAG, "Error to read transactions from file!", e);
        }

        return transactions == null ? new ArrayList<Transaction>() : transactions;
    }

    public synchronized void updateTransactionPayments(@NonNull String invoiceId, @Nullable ArrayList<Payment> payments) {
        ArrayList<Transaction> transactions = readAllTransactions();
        for (Transaction transaction : transactions) {
            if (invoiceId.equals(transaction.getInvoiceId())) {
                transaction.setPayments(payments);
                break;
            }
        }

        writeToStorage(transactions);
    }

    private synchronized void writeToStorage(@NonNull ArrayList<Transaction> transactions) {
        try (Writer writer = new FileWriter(getStoragePath(mContext))) {
            mGson.toJson(transactions, writer);
        } catch (IOException | JsonIOException e) {
            Log.e(TAG, "Error to write transactions to file!", e);
        }
    }

    public static void saveLastReconcile(Context applicationContext) {
        SharedPreferences sharedPreferences = applicationContext.getSharedPreferences(STATUS_INFO_PREFS_NAME, Context.MODE_PRIVATE);
        sharedPreferences.edit().putLong(KEY_RECONCILE_TIME, System.currentTimeMillis()).apply();
    }

    public static long getLastReconcileDate(Context applicationContext) {
        return getLastReconcileDate(getStatusInfoPrefs(applicationContext));
    }

    public static long getLastReconcileDate(SharedPreferences sharedPreferences) {
        return sharedPreferences.getLong(KEY_RECONCILE_TIME, 0);
    }

    public static SharedPreferences getStatusInfoPrefs(Context applicationContext) {
        return applicationContext.getSharedPreferences(STATUS_INFO_PREFS_NAME, Context.MODE_PRIVATE);
    }

}

package com.verifone.swordfish.manualtransaction.gui;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.verifone.swordfish.manualtransaction.BuildConfig;
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
 * Created by romans1 on 01/25/2018.
 */

public abstract class BaseActivity extends AppCompatActivity {

    private static final String TAG = BaseActivity.class.getSimpleName();

    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mProgressDialog = new ProgressDialog(this);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setSubtitle(getApplicationName());
        }
    }

    @Override
    protected void onDestroy() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        super.onDestroy();
    }

    private String getApplicationName() {
        return getString(R.string.app_name_extended) + " - " + BuildConfig.VERSION_NAME;
    }

    /**
     * Shows loading dialog with endless progress bar, call {@link #hideDialog()} to close it
     *
     * @param title        title of dialog
     * @param isCancelable is this dialog can be cancelled by user
     */
    protected void showDialogWithMessage(final String title, final boolean isCancelable) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mProgressDialog != null) {
                    mProgressDialog.setTitle(title);
                    mProgressDialog.setCancelable(isCancelable);
                    mProgressDialog.show();
                } else {
                    Log.w(TAG, "showDialogWithMessage(): Progress dialog unavailable, incorrect activity state!");
                }
            }
        });
    }

    /**
     * Hides loading dialog
     */
    protected void hideDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mProgressDialog != null) {
                    mProgressDialog.hide();
                } else {
                    Log.w(TAG, "hideDialog(): Progress dialog unavailable, incorrect activity state!");
                }
            }
        });
    }

    @Nullable
    protected FinishFragment getFinishFragment() {
        return (FinishFragment) getSupportFragmentManager().findFragmentById(R.id.finish_fragment);
    }

}

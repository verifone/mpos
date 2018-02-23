package com.verifone.swordfish.manualtransaction.gui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.verifone.swordfish.manualtransaction.R;
import com.verifone.swordfish.manualtransaction.tools.Utils;

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
 * Created by abey on 1/4/2018.
 */

public class StartActivity extends AppCompatActivity implements View.OnClickListener {

    public static final int GENERAL_REQUEST_CODE = 10000;

    public static final int RESULT_TRANSACTION_CANCELED = 8;

    /**
     * Actual values of terminal IP and PORT
     */
    private String mTerminalIP;
    private String mTerminalPort;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        mTerminalIP = Utils.getTerminalIP(this);
        mTerminalPort = Utils.getTerminalPort(this);

        Button changeIPButton = findViewById(R.id.changeIpAddressButton);
        changeIPButton.setText(getString(R.string.change_ip, mTerminalIP));
    }

    /**
     * Handles activity buttons clicks and starts necessary activities
     */
    @Override
    public void onClick(View view) {
        Intent newActivityIntent = null;
        switch (view.getId()) {
            case R.id.new_order:
                newActivityIntent = new Intent(this, OrderCreateActivity.class);
                break;
            case R.id.history:
                newActivityIntent = new Intent(this, TransactionHistoryActivity.class);
                break;
            case R.id.changeIpAddressButton:
                buildAndShowChangeIPDialog(view);
            default:
                break;
        }
        if (newActivityIntent != null) {
            startActivity(newActivityIntent);
        }
    }

    /**
     * Creates and shows dialog with input fields to set IP and PORT for communication
     *
     * @param changeIpButton clicked view
     */
    private void buildAndShowChangeIPDialog(final View changeIpButton) {
        View inflatedLayout = LayoutInflater.from(StartActivity.this).inflate(R.layout.dilog_view_change_ip_and_port, null);

        final EditText editTextIP = inflatedLayout.findViewById(R.id.input_ip);
        final EditText editTextPort = inflatedLayout.findViewById(R.id.input_port);

        editTextIP.setText(mTerminalIP);
        editTextPort.setText(mTerminalPort);

        editTextIP.setFilters(getInputFiltersForIP());

        new AlertDialog.Builder(changeIpButton.getContext())
                .setMessage(R.string.terminalIpTitle)
                .setView(inflatedLayout)
                .setPositiveButton(R.string.str_save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Context context = changeIpButton.getContext();

                        Toast.makeText(context, "Changed!", Toast.LENGTH_SHORT).show();

                        mTerminalIP = editTextIP.getText().toString();
                        mTerminalPort = editTextPort.getText().toString();

                        Utils.saveTerminalIP(context, mTerminalIP);
                        Utils.saveTerminalPort(context, mTerminalPort);

                        ((Button) changeIpButton).setText(getString(R.string.change_ip, mTerminalIP));
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        dialog.dismiss();
                    }
                }).show();
    }

    /**
     * Filters for some basic validation of entered IP address
     */
    @NonNull
    private InputFilter[] getInputFiltersForIP() {
        final InputFilter[] filters = new InputFilter[1];
        filters[0] = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                if (end > start) {
                    String destTxt = dest.toString();
                    String resultingTxt = destTxt.substring(0, dstart) + source.subSequence(start, end) + destTxt.substring(dend);
                    if (!resultingTxt.matches("^\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3})?)?)?)?)?)?")) {
                        return "";
                    } else {
                        String[] splits = resultingTxt.split("\\.");
                        for (String split : splits) {
                            if (Integer.valueOf(split) > 255) {
                                return "";
                            }
                        }
                    }
                }
                return null;
            }
        };
        return filters;
    }
}

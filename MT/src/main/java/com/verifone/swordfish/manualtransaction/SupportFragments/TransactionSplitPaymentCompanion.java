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

package com.verifone.swordfish.manualtransaction.SupportFragments;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.verifone.swordfish.manualtransaction.R;
import com.verifone.swordfish.manualtransaction.System.PaymentTerminal;
import com.verifone.swordfish.manualtransaction.Tools.LocalizeCurrencyFormatter;
import com.verifone.swordfish.manualtransaction.Tools.MposLogger;

import java.text.NumberFormat;



/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link TransactionSplitPaymentCompanion.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link TransactionSplitPaymentCompanion#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TransactionSplitPaymentCompanion extends Fragment {
    private static final String TAG = TransactionSplitPaymentCompanion.class.getSimpleName();
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;
    private TextView changeDue;
    private LinearLayout changeDueLinear;
    private LinearLayout startLinear;
    private LinearLayout settleLinear;

    public TransactionSplitPaymentCompanion() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment TransactionSplitPaymentCompanion.
     */
    // TODO: Rename and change types and number of parameters
    public static TransactionSplitPaymentCompanion newInstance(String param1, String param2) {
        TransactionSplitPaymentCompanion fragment = new TransactionSplitPaymentCompanion();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View myView = inflater.inflate(R.layout.fragment_transaction_split_payment_companion, container, false);
        Button cashButton = (Button) myView.findViewById(R.id.cashButton);
        Button creditButton = (Button) myView.findViewById(R.id.creditButton);

        cashButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onCashSelected();
            }
        });

        creditButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onCreditSelected();
            }
        });
        changeDue = (TextView) myView.findViewById(R.id.changeDue);
        changeDueLinear = (LinearLayout) myView.findViewById(R.id.changeDueLinear);
        startLinear = (LinearLayout) myView.findViewById(R.id.startLinear);
        settleLinear = (LinearLayout) myView.findViewById(R.id.settleLinear);
        Button emailReceipt = (Button) myView.findViewById(R.id.emailReceipt);
        emailReceipt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onSplitEmailReceipt();
            }
        });
        Button printReceipt = (Button) myView.findViewById(R.id.printReceipt);
        printReceipt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onSplitPrintReceipt();
            }
        });
        Button noReceipt = (Button) myView.findViewById(R.id.noReceipt);
        noReceipt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onSplitNoReceipt();
            }
        });
        return myView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void setmListener(TransactionSplitPaymentCompanion.OnFragmentInteractionListener listener) {
        mListener = listener;
    }

    public void onBackToSelection() {
        startLinear.setVisibility(View.VISIBLE);
        changeDueLinear.setVisibility(View.INVISIBLE);
        settleLinear.setVisibility(View.INVISIBLE);
    }

    public void partialSettle(float pending) {
        startLinear.setVisibility(View.INVISIBLE);
        settleLinear.setVisibility(View.INVISIBLE);
        changeDueLinear.setVisibility(View.VISIBLE);
        LocalizeCurrencyFormatter formatter = LocalizeCurrencyFormatter.getInstance();
        NumberFormat format = formatter.getCurrencyFormat();
        String pendingAmount = format.format(pending);
        changeDue.setText(pendingAmount);

    }

    public void transactionSettle() {
        startLinear.setVisibility(View.INVISIBLE);
        changeDueLinear.setVisibility(View.INVISIBLE);
        settleLinear.setVisibility(View.VISIBLE);
        final PaymentTerminal paymentTerminal = PaymentTerminal.getInstance();
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(final Void... params) {
                // Do your loading here. Don't touch any views from here, and then return null
                try {
                    paymentTerminal.stopSession();
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                    MposLogger.getInstance().error(TAG, " Error stoping session payment device " + e.toString());
                }
                ;
                return null;
            }
        }.execute();
    }

    public void goNext() {
        startLinear.setVisibility(View.VISIBLE);
        changeDueLinear.setVisibility(View.INVISIBLE);
        settleLinear.setVisibility(View.INVISIBLE);
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnFragmentInteractionListener {
        void onCashSelected();

        void onCreditSelected();

        void onSplitPrintReceipt();

        void onSplitEmailReceipt();

        void onSplitNoReceipt();
    }
}

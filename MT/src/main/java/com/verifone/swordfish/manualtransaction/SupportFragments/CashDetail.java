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
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.verifone.commerce.entities.Payment;
import com.verifone.swordfish.manualtransaction.MTDataModel.MTTransaction;
import com.verifone.swordfish.manualtransaction.R;
import com.verifone.swordfish.manualtransaction.Tools.DisplayStringRepresentation;
import com.verifone.swordfish.manualtransaction.Tools.LocalizeCurrencyFormatter;
import com.verifone.swordfish.manualtransaction.Tools.MposLogger;

import java.math.BigDecimal;
import java.text.NumberFormat;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link CashDetail.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link CashDetail#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CashDetail extends Fragment {
    private static final String TAG = CashDetail.class.getSimpleName();
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;
    private TextView totalTextView;
    private TextView balanceTextView;

    private MTTransaction currentTransaction;
    private DisplayStringRepresentation internalRepresentation;
    private BigDecimal transactionTotal;
    private TableLayout cashReceipt;
    private TextView textBalanceView;
    private TableRow initialRow;
    private EditText enterAmountEditText;

    double total = 0.0;
    double balance = 0.0;
    boolean settle;
    boolean start;

    public CashDetail() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment CashDetail.
     */
    // TODO: Rename and change types and number of parameters
    public static CashDetail newInstance(String param1, String param2) {
        CashDetail fragment = new CashDetail();
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
        View myView = inflater.inflate(R.layout.fragment_cash_detail, container, false);
        totalTextView = (TextView) myView.findViewById(R.id.textViewAmountToPayLabel);
        balanceTextView = (TextView) myView.findViewById(R.id.textRemainBalanceView);
        cashReceipt = (TableLayout) myView.findViewById(R.id.cashReceipt);
        textBalanceView = (TextView) myView.findViewById(R.id.textBalanceView);

        initialRow = (TableRow) inflater.inflate(R.layout.cash_payment_cell_1, cashReceipt, false);
        TextView currencySymbol = (TextView) initialRow.findViewById(R.id.textViewCurrencySymbol);
        currencySymbol.setVisibility(View.INVISIBLE);

        enterAmountEditText = (EditText) initialRow.findViewById(R.id.textViewAmountReceived);
        enterAmountEditText.setText(LocalizeCurrencyFormatter.getInstance().getCurrencyFormat().format(0.0f));
        enterAmountEditText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int inType = enterAmountEditText.getInputType(); // backup the input type
                enterAmountEditText.setInputType(InputType.TYPE_NULL); // disable soft input
                enterAmountEditText.onTouchEvent(event); // call native handler
                enterAmountEditText.setInputType(inType); // restore input type
                enterAmountEditText.setFocusable(true);
                return true;

            }
        });
        total = 0.0;
        balance = 0.0;
        settle = false;
        internalRepresentation = new DisplayStringRepresentation();
        start = true;
        setCurrentTransaction(currentTransaction);
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

    public void attachValue(String newValue) {

        if (internalRepresentation == null) {
            internalRepresentation = new DisplayStringRepresentation();
        }
        internalRepresentation.attachValue(newValue, null);
        if (internalRepresentation.currentString().length() > 0) {
            BigDecimal newtotal = new BigDecimal(internalRepresentation.currentString());
            //float newTotal = Float.parseFloat((String) attachedValue.toString());
            NumberFormat format = NumberFormat.getCurrencyInstance();
            enterAmountEditText.setText(format.format(newtotal.floatValue()));
            pay(newtotal.doubleValue() - total);
        } else {
            if (total == 0) {
                enterAmountEditText.setText(getResources().getText(R.string.addItem));
            }
        }

        enterAmountEditText.setSelection(enterAmountEditText.getText().length());
    }


    public void setTotal(double total) {
        transactionTotal = new BigDecimal(total);
    }

    public void setCurrentTransaction(MTTransaction transaction) {
        currentTransaction = transaction;
        if (currentTransaction != null && currentTransaction.getTransactionTotal() != null) {
            balance = currentTransaction.getTransactionTotal().doubleValue();
        } else
            balance = BigDecimal.ZERO.doubleValue();
        LocalizeCurrencyFormatter formatter = LocalizeCurrencyFormatter.getInstance();
        String displayTotay = formatter.getCurrencyFormat().format(transactionTotal.doubleValue());
        if (totalTextView != null) {
            totalTextView.setText(displayTotay);
        }
        if (balanceTextView != null) {
            balanceTextView.setText(displayTotay);

        }
        if (cashReceipt != null) {
            cashReceipt.removeAllViews();
            cashReceipt.addView(initialRow);
        }
    }

    /**
     * This method check if amount enter is enough to pay for transaction total
     * and display values onto the view.
     *
     * @param amount transaction amount to  to be paid by cash.
     */
    public void pay(double amount) {
        total += amount;
        balance = transactionTotal.doubleValue() - total;

        if (start) {
            start = false;
        } else {
            if (balance <= 0.0f) {
                start = true;
            }
        }
        MposLogger.getInstance().debug("CD Pay ", String.format("total: $%.2f balance: $%.2f", total, balance));
        if (balance <= 0 && !settle) {
            settle = true;
            cashReceipt.removeView(initialRow);
            textBalanceView.setText("CHANGE DUE:");
            TableRow finalRow = (TableRow) getActivity().getLayoutInflater().inflate(R.layout.casp_payment_cell_2, cashReceipt, false);
            TextView currencySymbol = (TextView) finalRow.findViewById(R.id.textViewCashReceivedMoneyCurrency);
            currencySymbol.setText("- ");
            TextView totalAmount = (TextView) finalRow.findViewById(R.id.textViewCashReceivedAmount);
            NumberFormat currencyFormat = LocalizeCurrencyFormatter.getInstance().getCurrencyFormat();
            totalAmount.setText(currencyFormat.format(total));
            cashReceipt.addView(finalRow);
            if (balance == 0) {
                double newAmount = 0;
                balanceTextView.setText(currencyFormat.format(newAmount));
            } else {
                balanceTextView.setText(currencyFormat.format(balance * -1));
            }
            mListener.onCashDetail();
        } else {
            enterAmountEditText.setText(LocalizeCurrencyFormatter.getInstance().getCurrencyFormat().format(total));
            balanceTextView.setText(LocalizeCurrencyFormatter.getInstance().getCurrencyFormat().format(balance));
        }
        //internalRepresentation = enterAmountEditText.getText().toString().replace(".", "");
    }

    public String tenderedAmount() {
        return String.format("%.2f", total);
    }

    public boolean isSettle() {
        return (transactionTotal.doubleValue() - total <= 0) ? true : false;
    }

    public void addPayment() {
        Payment payments = new Payment();
        payments.setPaymentMethod("Cash");
        payments.setPaymentType(Payment.PaymentType.CASH);
        payments.setPaymentAmount(transactionTotal);
        currentTransaction.addPayment(payments);

    }

    public void setOnFragmentInteractionListener(CashDetail.OnFragmentInteractionListener listener) {
        mListener = listener;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnFragmentInteractionListener {
        void onCashDetail();
    }
}

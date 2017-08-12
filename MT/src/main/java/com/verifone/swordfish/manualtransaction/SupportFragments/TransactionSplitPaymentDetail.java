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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.verifone.commerce.entities.Payment;
import com.verifone.swordfish.manualtransaction.R;
import com.verifone.swordfish.manualtransaction.Tools.DisplayStringRepresentation;
import com.verifone.swordfish.manualtransaction.Tools.LocalizeCurrencyFormatter;
import com.verifone.swordfish.manualtransaction.Tools.MposLogger;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link TransactionSplitPaymentDetail.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link TransactionSplitPaymentDetail#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TransactionSplitPaymentDetail extends Fragment {
    private static final String TAG = TransactionSplitPaymentDetail.class.getSimpleName();
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;
    private TextView totalTextView;
    private TextView balanceTextView;
    private EditText cashValue;
    private EditText chargeAmount;
    private ListView detailList;
    private List<LinearLayout> transactions;
    private List<Payment> transactionPayments;
    private LinearLayout cashPayment;
    private LinearLayout partialCashSettle;
    private LinearLayout partialCardSettle;
    private LinearLayout chargePayment;
    private TextView textTotalCashReceived;
    private TextView textTotalCardReceived;
    private TextView textAmountToPay;
    private DisplayStringRepresentation internalRepresentation;
    private CheckBox checkBox;
    private boolean start = true;
    private boolean cashOrCredit;
    private boolean sameAsReceived;
    private BigDecimal balance;
    private ViewGroup myContainer;
    private int cashHeight = 0;


    private float totalPay = 0.0f;
    private float totalPayed = 0.0f;
    private float amoutToPay = 0.0f;
    private boolean overcharged = false;
    private boolean editMode;
    private boolean isOpenTransactionNeedIt = false;
    private BigDecimal transactionTotal;
    private BigDecimal changeDue;

    public TransactionSplitPaymentDetail() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment TransactionSplitPaymentDetail.
     */
    public static TransactionSplitPaymentDetail newInstance(String param1, String param2) {
        TransactionSplitPaymentDetail fragment = new TransactionSplitPaymentDetail();
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
        Log.i(TAG, "OnCreateView");
        // Inflate the layout for this fragment
        myContainer = container;
        View myView = inflater.inflate(R.layout.fragment_transaction_split_payment_detail, container, false);
        totalTextView = (TextView) myView.findViewById(R.id.amountToPay);
        totalTextView.setText(getLocalizedCurrencyTotal());
        balanceTextView = (TextView) myView.findViewById(R.id.balanceTotal);
        balanceTextView.setText(getLocalizedCurrencyTotal());
        detailList = (ListView) myView.findViewById(R.id.transactionList);

        cashPayment = (LinearLayout) inflater.inflate(R.layout.split_cash_payment, container, false);
        cashValue = (EditText) cashPayment.findViewById(R.id.cashReceived);
        cashValue.setInputType(InputType.TYPE_NULL);
        cashValue.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    MposLogger.getInstance().debug(TAG, "focus");
                    sameAsReceived = true;
                    resetInternalRepresentation(totalPay);
                    if (internalRepresentation.currentString().length() > 0) {
                        start = false;
                    }
                    editMode = true;
                }
            }
        });
        checkBox = (CheckBox) cashPayment.findViewById(R.id.checkBox);
        checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCheckboxClicked(v);
            }
        });
        checkBox.setEnabled(false);
        sameAsReceived = checkBox.isChecked();
        textAmountToPay = (TextView) cashPayment.findViewById(R.id.amountToPayTxt);
        textAmountToPay.setFocusable(true);
        textAmountToPay.setOnTouchListener(new View.OnTouchListener() {
            //@Override
            public boolean onTouch(View v, MotionEvent event) {
                int inType = textAmountToPay.getInputType(); // backup the input type
                textAmountToPay.setInputType(InputType.TYPE_NULL); // disable soft input
                textAmountToPay.onTouchEvent(event); // call native handler
                textAmountToPay.setInputType(inType); // restore input type
                textAmountToPay.setFocusable(true);
                if (!checkBox.isChecked()) {
                    sameAsReceived = false;
                    resetInternalRepresentation(amoutToPay);
                }
                cashValue.clearFocus();
                return true;
            }
        });

        textAmountToPay.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                MposLogger.getInstance().debug(TAG, "focus");
                int inType = textAmountToPay.getInputType(); // backup the input type
                textAmountToPay.setInputType(InputType.TYPE_NULL); // disable soft input
                textAmountToPay.setInputType(inType); // restore input type
                if (hasFocus) {
                    sameAsReceived = false;
                    resetInternalRepresentation(amoutToPay);
                }
            }
        });
        partialCashSettle = (LinearLayout) inflater.inflate(R.layout.split_partial_settle, container, false);
        textTotalCashReceived = (TextView) partialCashSettle.findViewById(R.id.textTotalReceived);
        partialCardSettle = (LinearLayout) inflater.inflate(R.layout.split_partial_settle, container, false);
        textTotalCardReceived = (TextView) partialCardSettle.findViewById(R.id.textTotalReceived);
        TextView typeOfPayLabel = (TextView) partialCardSettle.findViewById(R.id.typeOfReceivePayment);
        typeOfPayLabel.setText(getActivity().getResources().getString(R.string.split_card_received_label));
        chargePayment = (LinearLayout) inflater.inflate(R.layout.split_card_entry_layout, container, false);

        chargeAmount = (EditText) chargePayment.findViewById(R.id.chargeAmount);
        chargeAmount.setInputType(InputType.TYPE_NULL);
        chargeAmount.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int inType = chargeAmount.getInputType(); // backup the input type
                chargeAmount.setInputType(InputType.TYPE_NULL); // disable soft input
                chargeAmount.onTouchEvent(event); // call native handler
                chargeAmount.setInputType(inType); // restore input type
                chargeAmount.setFocusable(true);
                return true;
            }
        });
        start = true;
        transactions = new ArrayList<LinearLayout>();
        transactionPayments = new ArrayList<Payment>();
        PaymentDetailAdapter<LinearLayout> arrayAdapter = new PaymentDetailAdapter<LinearLayout>(getActivity().getBaseContext(), transactions, 0);

        detailList.setAdapter(arrayAdapter);
        balance = transactionTotal; // new BigDecimal(transaction.getTransactionTotal());
        editMode = false;
        overcharged = false;
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

    private String getLocalizedCurrencyTotal() {
        LocalizeCurrencyFormatter formatter = LocalizeCurrencyFormatter.getInstance();
        return formatter.getCurrencyFormat().format(transactionTotal != null?
                transactionTotal.doubleValue() : getActivity().getString(R.string.str_dollar_zero));
    }

    public void onCheckboxClicked(View view) {
        // Is the view now checked?

        sameAsReceived = ((CheckBox) view).isChecked();
        internalRepresentation = new DisplayStringRepresentation();
        if (!sameAsReceived) {
            cashValue.setFocusable(false);
            MposLogger.getInstance().debug(TAG, "amount to pay");
            textAmountToPay.setFocusable(true);
            textAmountToPay.requestFocus();
            amoutToPay = 0.0f;
            NumberFormat format = NumberFormat.getCurrencyInstance();

            textAmountToPay.setText(format.format(amoutToPay));
            resetInternalRepresentation(amoutToPay);
        } else {
            MposLogger.getInstance().debug(TAG, "cash value");
            textAmountToPay.setText(cashValue.getText());
            textAmountToPay.setFocusable(false);
            cashValue.setFocusable(true);
            cashValue.requestFocus();
            resetInternalRepresentation(totalPay);
        }

    }

    private void resetInternalRepresentation(float amount) {
        //TODO check this method
        String txtAmount = String.format("%.2f", amount);
        txtAmount = txtAmount.replace(".", "");
        txtAmount = txtAmount.replace(",", "");
        if (txtAmount.length() > 0) {
            internalRepresentation = new DisplayStringRepresentation();
            int index;
            for (index = 0; index < txtAmount.length(); index++) {
                internalRepresentation.attachValue(txtAmount.substring(index, index), null);
            }
        }
    }


    public void setmListener(OnFragmentInteractionListener listener) {
        mListener = listener;
    }

    public void setTotal(BigDecimal total) {
        transactionTotal = total;
    }

    public void onBackToSelection() {
        if (totalPayed == 0) {
            if (cashOrCredit) {
                transactions.remove(cashPayment);
            } else {
                transactions.remove(chargePayment);
            }
            PaymentDetailAdapter adapter = (PaymentDetailAdapter) detailList.getAdapter();
            adapter.notifyDataSetChanged();

        }
    }

    public void onCashSelected() {
        Log.i(TAG, "onCashSelected");
        if (cashHeight != 0) {
            ViewGroup.LayoutParams params = cashPayment.getLayoutParams();
            params.height = cashHeight;
            cashPayment.setLayoutParams(params);

        }
        totalPay = 0.0f;
        internalRepresentation = new DisplayStringRepresentation();
        LayoutInflater inflater = getActivity().getLayoutInflater();
        LinearLayout partial = (LinearLayout) inflater.inflate(R.layout.split_partial_settle, myContainer, false);
        textTotalCashReceived = (TextView) partial.findViewById(R.id.textTotalReceived);

        partialCashSettle = partial;
        transactions.add(cashPayment);
        mListener.onSplitCashPayment();
        NumberFormat format = NumberFormat.getCurrencyInstance();
        cashValue.setText(format.format(0.0f));
        textAmountToPay.setText(cashValue.getText());
        cashOrCredit = true;
        PaymentDetailAdapter adapter = (PaymentDetailAdapter) detailList.getAdapter();
        adapter.notifyDataSetChanged();
    }

    public void onCreditSelected() {
        Log.i(TAG, "onCreditSelected");
        LayoutInflater inflater = getActivity().getLayoutInflater();
        LinearLayout partial = (LinearLayout) inflater.inflate(R.layout.split_partial_settle, myContainer, false);
        textTotalCardReceived = (TextView) partial.findViewById(R.id.textTotalReceived);
        TextView typeOfPayLabel = (TextView) partial.findViewById(R.id.typeOfReceivePayment);
        typeOfPayLabel.setText(getActivity().getResources().getString(R.string.split_card_received_label));
        partialCardSettle = partial;
        totalPay = 0.0f;
        internalRepresentation = new DisplayStringRepresentation();
        cashOrCredit = false;
        transactions.add(chargePayment);
        NumberFormat format = NumberFormat.getCurrencyInstance();
        chargeAmount.setText(format.format(0.0f));
        //mListener.onSplitCardPayment();
        PaymentDetailAdapter adapter = (PaymentDetailAdapter) detailList.getAdapter();
        adapter.notifyDataSetChanged();

    }

    public void attachValue(String newValue) {

        CharSequence attachedValue = null;
        if (internalRepresentation == null) {
            internalRepresentation = new DisplayStringRepresentation();
        }

        internalRepresentation.attachValue(newValue, null);
        attachedValue = internalRepresentation.currentString();
        NumberFormat format = NumberFormat.getCurrencyInstance();

        if (!start && attachedValue.length() > 0) {
            //code for special characters

            if (cashOrCredit) {
                if (sameAsReceived) {
                    totalPay = Float.parseFloat((String) attachedValue.toString());
                    String amountToText = format.format(totalPay);
                    cashValue.setText(amountToText);
                    if (checkBox.isChecked()) {
                        textAmountToPay.setText(cashValue.getText());
                        if (totalPay > 0.0f) {
                            checkBox.setEnabled(true);
                        }
                    }

                    if (!editMode) {
                        if (totalPay > 0.0f) {
                            checkBox.setEnabled(true);
                        }
                    }

                } else {
                    amoutToPay = Float.parseFloat((String) attachedValue.toString());
                    textAmountToPay.setText(format.format(amoutToPay));
                    if (!verifyAmountToPay()) {
                        textAmountToPay.setTextColor(getActivity().getResources().getColor(R.color.vermillion));
                    } else {
                        textAmountToPay.setTextColor(getActivity().getResources().getColor(R.color.grey));
                    }
                }
            } else {
                totalPay = Float.parseFloat((String) attachedValue.toString());
                chargeAmount.setText(format.format(totalPay));
                chargeAmount.setSelection(chargeAmount.getText().length());
                textAmountToPay.setText(chargeAmount.getText());
            }
        } else {
            start = false;
            if (attachedValue.length() > 0) {
                totalPay = Float.parseFloat((String) attachedValue.toString());
            } else {
                totalPay = 0.0f;
            }
            chargeAmount.setText(format.format(totalPay));
            chargeAmount.setSelection(chargeAmount.getText().length());
            textAmountToPay.setText(chargeAmount.getText());
            cashValue.setText(format.format(totalPay));

        }

    }

    public boolean verifyAmountToPay() {
        BigDecimal difference = balance.subtract(new BigDecimal(amoutToPay));

        if (difference.doubleValue() < -0.001 || amoutToPay > totalPay) {
            String toastMessage;
            if (difference.doubleValue() < -0.001) {
                toastMessage = getActivity().getResources().getString(R.string.split_error_amount);
            } else {
                toastMessage = getActivity().getResources().getString(R.string.split_error_cash);
                ;
            }
            Toast soon = Toast.makeText(getActivity().getApplicationContext(), toastMessage, Toast.LENGTH_LONG);
            soon.show();
            return false;
        } else {
            return true;
        }

    }


    public void onCashKeyboard(int amount) {
        NumberFormat format = NumberFormat.getCurrencyInstance();
        if (cashOrCredit) {
            if (sameAsReceived) {
                totalPay = totalPay + (float) amount;
                cashValue.setText(format.format(totalPay));
                textAmountToPay.setText(cashValue.getText());
                checkBox.setEnabled(true);
            } else {
                amoutToPay += (float) amount;
                textAmountToPay.setText(format.format(amoutToPay));
                if (!verifyAmountToPay()) {
                    textAmountToPay.setTextColor(getActivity().getResources().getColor(R.color.vermillion));
                } else {
                    textAmountToPay.setTextColor(getActivity().getResources().getColor(R.color.grey));
                }
            }
        } else {
            totalPay = totalPay + (float) amount;
            chargeAmount.setText(format.format(totalPay));
        }

    }

    public String amountToBePaid() {
        return String.format("%.2f", totalPay);
    }

    public boolean totalPaid() {
        float amount = 0.0f;
        if (sameAsReceived) {
            amount = totalPay;
        } else {
            amount = amoutToPay;
        }

        if (amount > 0) {
            return true;
        } else {
            return false;
        }
    }

    public boolean totalToCharge() {
        if (totalPay > 0) {
            return true;
        } else {
            return false;
        }
    }

    public void cashSettle(Payment payment) {

        transactionPayments.add(payment);
        if (sameAsReceived || (!sameAsReceived && verifyAmountToPay())) {
            if (payment.getPaymentAmount().floatValue() > 0) {
                SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a");
                Date today = Calendar.getInstance().getTime();
                String reportDate = df.format(today);

                NumberFormat format = NumberFormat.getCurrencyInstance();

                if (checkBox.isChecked()) {
                    textTotalCashReceived.setText("-" + cashValue.getText().toString());
                } else {
                    textTotalCashReceived.setText("-" + textAmountToPay.getText().toString());
                }
                balance = balance.subtract(payment.getPaymentAmount());
                balance.setScale(2, BigDecimal.ROUND_HALF_EVEN);
                String zeroTest = String.format("%.2f", balance.floatValue());
                if (zeroTest.equals("-0.00")) {
                    balanceTextView.setText(format.format(0.00f));
                } else {
                    balanceTextView.setText(format.format(balance.doubleValue()));
                }
                totalPayed = +payment.getPaymentAmount().floatValue();
                checkBox.setSelected(true);
                totalPay = 0.0f;
                amoutToPay = 0.0f;
                internalRepresentation = new DisplayStringRepresentation();
            }
        }
/*
        ViewGroup.LayoutParams params = cashPayment.getLayoutParams();
        cashHeight = params.height;
        if (transactions.contains(cashPayment)) {
            transactions.remove(cashPayment);
        }
        params.height = 0;
        cashPayment.setLayoutParams(params);
        transactions.add(transactions.size(), partialCashSettle);
        PaymentDetailAdapter adapter = (PaymentDetailAdapter) detailList.getAdapter();
        adapter.notifyDataSetChanged();
*/
        checkBox.setChecked(true);
        checkBox.setEnabled(false);
        cashValue.setFocusable(true);
        textAmountToPay.setFocusable(false);
        sameAsReceived = true;
        editMode = false;
        updateList();

    }

    public void creditSettle(Payment payment) {
        transactionPayments.add(payment);
        if (totalPay > 0) {
            totalPayed += totalPay;

            balance = balance.subtract(new BigDecimal(totalPay));
            balance.setScale(2, BigDecimal.ROUND_HALF_EVEN);
            NumberFormat format = NumberFormat.getCurrencyInstance();
            textTotalCardReceived.setText("-" + format.format(totalPay));
            String remainingBalance = String.format("%.2f", transactionTotal.doubleValue() - totalPayed);
            MposLogger.getInstance().debug(TAG, "Split balance: " + remainingBalance);
            MposLogger.getInstance().debug(TAG, "Big D balance" + balance.toString());
            totalPay = 0.0f;
            amoutToPay = 0.0f;
            balanceTextView.setText(format.format(balance.floatValue()));
            chargeAmount.setTextColor(getActivity().getResources().getColor(R.color.black));
            internalRepresentation = new DisplayStringRepresentation();
        }
        updateList();
        //transactions.remove(chargePayment);
        //transactions.add(transactions.size(), partialCardSettle);
        //PaymentDetailAdapter adapter = (PaymentDetailAdapter) detailList.getAdapter();
        //adapter.notifyDataSetChanged();
    }

    private void updateList() {
        transactions.clear();
        NumberFormat format = NumberFormat.getCurrencyInstance();
        LayoutInflater layoutInflater = LayoutInflater.from(this.getContext());
        for (Payment payment : transactionPayments) {
            LinearLayout linearLayout = (LinearLayout) layoutInflater.inflate(R.layout.split_partial_settle, null, false);
            TextView cashReceive = (TextView) linearLayout.findViewById(R.id.textTotalReceived);
            cashReceive.setText("-" + format.format(payment.getPaymentAmount().doubleValue()));
            TextView typeOfPayLabel = (TextView) linearLayout.findViewById(R.id.typeOfReceivePayment);
            if (payment.getPaymentType() == Payment.PaymentType.CASH) {
                typeOfPayLabel.setText(getActivity().getResources().getString(R.string.split_cash_received_label));
            } else {
                typeOfPayLabel.setText(getActivity().getResources().getString(R.string.split_card_received_label));
            }
            transactions.add(linearLayout);
        }
        PaymentDetailAdapter adapter = (PaymentDetailAdapter) detailList.getAdapter();
        adapter.notifyDataSetChanged();
    }

    public void resetView() {
        NumberFormat format = NumberFormat.getCurrencyInstance();
        String remainingBalance = String.format("%.2f", transactionTotal.doubleValue() - totalPayed);
        MposLogger.getInstance().debug(TAG, "Split balance: " + remainingBalance);
        MposLogger.getInstance().debug(TAG, "Big D balance" + balance.toString());
        totalPay = 0.0f;
        amoutToPay = 0.0f;
        balanceTextView.setText(format.format(balance.floatValue()));
        chargeAmount.setTextColor(getActivity().getResources().getColor(R.color.black));
        internalRepresentation = new DisplayStringRepresentation();

    }

    public void onCreditPaymentCancel() {
        resetView();
    }

    public void onCreditPaymentRejected() {
        resetView();
    }

    public void overCharged() {
        chargeAmount.setTextColor(getActivity().getResources().getColor(R.color.vermillion));
        overcharged = true;
    }

    public float balanceAfterCharge() {
        if (cashOrCredit) {
            return balance.floatValue();
        } else {
            BigDecimal balanceToBeCharge = transactionTotal;//new BigDecimal(transaction.getTransactionTotal());
            balanceToBeCharge = balanceToBeCharge.subtract(new BigDecimal(totalPayed));
            balanceToBeCharge = balanceToBeCharge.subtract(new BigDecimal(totalPay));

            balanceToBeCharge.setScale(2, BigDecimal.ROUND_CEILING);
            double newBalance = balanceToBeCharge.doubleValue();
            String result = String.format("%.2f", newBalance);
            return Float.parseFloat(result);
        }
    }

    public BigDecimal totalForCashSettle() {
        if (sameAsReceived) {
            changeDue = BigDecimal.ZERO;
            return new BigDecimal(totalPay);
        } else {
            BigDecimal received = new BigDecimal(amoutToPay);
            changeDue = new BigDecimal(totalPay).subtract(received);
            return received;
        }
    }

    public BigDecimal amountToCharge() {
        return new BigDecimal(totalPay);
    }

    public BigDecimal getChangeDue() {
        return changeDue;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnFragmentInteractionListener {
        void onSplitCashPayment();
    }

    public class PaymentDetailAdapter<T> extends ArrayAdapter<LinearLayout> {
        private final Context context;
        private int type;

        public PaymentDetailAdapter(Context context, List<LinearLayout> transactions, int forType) {
            super(context, -1, transactions);
            this.context = context;
            this.type = forType;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            return transactions.get(position);
        }
    }

}

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

package com.verifone.swordfish.manualtransaction.HistoryFragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.verifone.commerce.entities.Payment;
import com.verifone.swordfish.manualtransaction.R;
import com.verifone.swordfish.manualtransaction.SupportFragments.ButtonsFragment;
import com.verifone.swordfish.manualtransaction.Tools.LocalizeCurrencyFormatter;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link HistoryRefundCompanionConfirmation.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link HistoryRefundCompanionConfirmation#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HistoryRefundCompanionConfirmation extends Fragment implements
        ButtonsFragment.OnFragmentInteractionListener {

    private static final String TAG = HistoryRefundCompanionConfirmation.class.getSimpleName();
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;
    private Payment.PaymentType mPaymentType;
    private Payment mPayment;
    private ButtonsFragment buttonsFragment;

    public HistoryRefundCompanionConfirmation() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HistoryRefundCompanionConfirmation.
     */
    public static HistoryRefundCompanionConfirmation newInstance(String param1, String param2) {
        HistoryRefundCompanionConfirmation fragment = new HistoryRefundCompanionConfirmation();
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

        Log.d(TAG, "onCreateView");
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_history_refund_companion_confirmation, container, false);
        LinearLayout paymentTypeLL = (LinearLayout) view.findViewById(R.id.historyRefundTypeLL);
        LinearLayout paymentView = (LinearLayout) inflater.inflate(R.layout.history_cash_total, null);
        LocalizeCurrencyFormatter formatter = LocalizeCurrencyFormatter.getInstance();
        switch (mPayment.getPaymentType()) {
            case CASH: {
                TextView totalAmount = (TextView) paymentView.findViewById(R.id.historyCashTotalLL);
                totalAmount.setText(formatter.getCurrencyFormat().format(mPayment.getPaymentAmount().doubleValue()));
                TextView subHeader = (TextView) paymentView.findViewById(R.id.historyCashChangeLL);
                subHeader.setVisibility(View.INVISIBLE);
                break;
            }
            case CREDIT:
            case DEBIT: {
                paymentView = (LinearLayout) inflater.inflate(R.layout.history_card_total, null);
                if (mPayment.getPaymentType() == Payment.PaymentType.DEBIT) {
                    TextView cardType = (TextView) paymentView.findViewById(R.id.historyCardType);
                    cardType.setText(getActivity().getResources().getString(R.string.history_card_type_debit));

                }
                if (mPayment.getCardInformation().getCardHolderName() != null) {
                    TextView cardHolder = (TextView) paymentView.findViewById(R.id.historyCardHolder);
                    cardHolder.setText(" **** " + mPayment.getCardInformation().getPanLast4());
                }
                if (mPayment.getAuthCode() != null) {
                    TextView cardHolder = (TextView) paymentView.findViewById(R.id.historyCardOwner);
                    cardHolder.setText(mPayment.getCardInformation().getCardHolderName());
                }
                if (mPayment.getCardInformation().getCardExpiry() != null) {
                    TextView cardExp = (TextView) paymentView.findViewById(R.id.historyCardExp);
                    cardExp.setText(getActivity().getResources().getString(R.string.history_car_exp_date) + mPayment.getCardInformation().getCardExpiry());
                }
                TextView totalAmount = (TextView) paymentView.findViewById(R.id.historyCardTotal);
                totalAmount.setText(formatter.getCurrencyFormat().format(mPayment.getPaymentAmount().doubleValue()));
                break;
            }
            default:
                break;

        }
        paymentTypeLL.addView(paymentView);
        buttonsFragment = new ButtonsFragment();
        buttonsFragment.setListener(this);
        //FragmentManager fragmentManager = this.getFragmentManager();
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction
                .add(R.id.refundIssueButtons, buttonsFragment)
                .commit();
        return view;
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

    public void setListener(HistoryRefundCompanionConfirmation.OnFragmentInteractionListener listener) {
        mListener = listener;
    }

    public void setPaymentType(Payment.PaymentType paymentType) {
        mPaymentType = paymentType;
    }

    public void setPayment(Payment payment) {
        mPayment = payment;
    }

    @Override
    public void onButtonPress(int buttonID) {
        switch (buttonID) {
            case 0:
                mListener.onCancelRefund();
                break;
            case 2:
                mListener.onIssueRefund();
                break;
            default:
                break;
        }
    }

    @Override
    public void readyToConfigureButtons() {
        buttonsFragment.onConfigureButton(0, true, getActivity().getString(R.string.buttonCancelRefund));
        buttonsFragment.onConfigureButton(1, false, null);
        buttonsFragment.onConfigureButton(2, true, getActivity().getString(R.string.buttonIssueRefund));

    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnFragmentInteractionListener {
        void onCancelRefund();

        void onIssueRefund();
    }
}

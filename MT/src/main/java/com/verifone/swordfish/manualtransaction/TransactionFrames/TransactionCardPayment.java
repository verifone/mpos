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

package com.verifone.swordfish.manualtransaction.TransactionFrames;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.verifone.commerce.entities.Payment;
import com.verifone.swordfish.manualtransaction.MTDataModel.MTTransaction;
import com.verifone.swordfish.manualtransaction.R;
import com.verifone.swordfish.manualtransaction.SupportFragments.PaymentByCardList;
import com.verifone.swordfish.manualtransaction.SupportFragments.PaymentCardController;

import java.math.BigDecimal;



/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link TransactionCardPayment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link TransactionCardPayment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TransactionCardPayment extends Fragment implements PaymentCardController.OnFragmentInteractionListener {

    private static final String TAG = TransactionCardPayment.class.getSimpleName();
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;
    private PaymentByCardList paymentByCardList;
    private PaymentCardController paymentCardController;
    private BigDecimal total;
    private boolean isDeclined = false;
    private MTTransaction currentTransaction;
    private BigDecimal mTip;
    private boolean isSplit;
    private boolean isManual = false;

    private OnFragmentInteractionListener mListener;

    public TransactionCardPayment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment TransactionCardPayment.
     */
    public static TransactionCardPayment newInstance(String param1, String param2) {
        TransactionCardPayment fragment = new TransactionCardPayment();
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
        View myView = inflater.inflate(R.layout.fragment_transaction_card_payment, container, false);

        paymentByCardList = new PaymentByCardList();
        paymentByCardList.setTotal(currentTransaction.getTransactionTotal());

        paymentCardController = new PaymentCardController();
        paymentCardController.setListener(this);
        paymentCardController.setTransaction(currentTransaction);
        paymentCardController.setManualPayment(this.isManual);
        FragmentManager manager = getActivity().getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.add(R.id.paymentList, paymentByCardList)
                .add(R.id.cardCompanion, paymentCardController)
                .commit();

        return myView;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
//        mListener = null;
    }


    public void setListener(TransactionCardPayment.OnFragmentInteractionListener listener) {
        mListener = listener;
    }

    public void setTotal(BigDecimal transactionTotal) {
        total = transactionTotal;
    }

    public void setTransaction(MTTransaction transaction) {
        currentTransaction = transaction;
    }

    public void setSplit(boolean split) {
        isSplit = split;
    }

    public void setManual(boolean manual) {
        this.isManual = manual;
    }

    @Override
    public void onPaymentControllerCancel() {
        mListener.cancelTransaction();
    }

    @Override
    public void onPaymentControllerBack() {
        mListener.goBack();
    }

    @Override
    public void onPaymentControllerPaymentInfo(Payment payment) {
        paymentByCardList.setAuthCode(payment.getAuthCode());
        paymentByCardList.setPan(payment.getCardInformation().getCardPan());
        paymentByCardList.setHolder(payment.getCardInformation().getCardHolderName());
        paymentByCardList.setExpDate(payment.getCardInformation().getCardExpiry());
        currentTransaction.addPayment(payment);
    }

    @Override
    public void onPaymentControllerSettle() {
        mListener.settle();

    }

    @Override
    public void onPaymentControllerDenied() {
        mListener.goBack();
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void cancelTransaction();

        void goBack();

        void settle();
    }
}

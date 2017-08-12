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
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bugsee.library.Bugsee;
import com.verifone.commerce.entities.Payment;
import com.verifone.swordfish.manualtransaction.MTDataModel.MTTransaction;
import com.verifone.swordfish.manualtransaction.MTDataModel.MTTransactionHistory;
import com.verifone.swordfish.manualtransaction.PaymentState;
import com.verifone.swordfish.manualtransaction.R;
import com.verifone.swordfish.manualtransaction.TransactionFrames.WaitForCardProcess;

import java.math.BigDecimal;
import java.util.HashMap;

import static com.verifone.commerce.entities.Receipt.DELIVERY_METHOD_PRINT;



/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link PaymentCardController.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link PaymentCardController#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PaymentCardController extends Fragment implements
        GratuitySelection.OnFragmentInteractionListener,
        WaitCardPresent.OnFragmentInteractionListener,
        WaitForCardProcess.OnFragmentInteractionListener,
        TransactionComplete.OnFragmentInteractionListener,
        CardDeclined.OnFragmentInteractionListener {

    private static final String TAG = PaymentCardController.class.getSimpleName();
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;
    private PaymentState paymentState;
    private MTTransaction currentTransaction;
    private TransactionComplete transactionComplete;
    private BigDecimal mTip;
    private BigDecimal mSplitAmount;
    private boolean mSplit;
    private boolean replace = false;
    private boolean isRestaurant = false;
    private boolean firstStep = false;
    private boolean manualPayment = false;
    private int receipt;

    public PaymentCardController() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment PaymentCardController.
     */
    public static PaymentCardController newInstance(String param1, String param2) {
        PaymentCardController fragment = new PaymentCardController();
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
        // set the state
        paymentState = PaymentState.waitingForCardPresent;
        replace = false;
        return inflater.inflate(R.layout.fragment_payment_card_controller, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        Bugsee.trace("On Resume", MTTransactionHistory.getInstance());
        if (!manualPayment) {
            paymentState = PaymentState.waitingForCardPresent;
        } else {
            paymentState = PaymentState.processingPayment;
            isRestaurant = false;
        }
        paymentWorkflow();

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

    private void registerScreenName(PaymentState state) {
        Log.i(TAG, "registerScreenName : " + state.toString());
        HashMap<String, Object> params = new HashMap<>();
        params.put("ITEM_CATEGORY", "Screen");
        params.put("ITEM_NAME", "Screen name");
        params.put("Action", state.toString());
        Bugsee.event("Payment Card Controller event", params);
    }

    private void paymentWorkflow() {
        registerScreenName(paymentState);
        switch (paymentState) {
            /*case gratuitySelection:
                presentGratuitySelection();
                break;*/
            case waitingForCardPresent:
                presentWaitingCardPresent();
                break;
            case processingPayment:
                presentProcessPayment();
                break;
            case waitingForReceiptOption:
                presentReceiptOption();
                break;
            case paymentRejected:
                presentCardDeclined();
                break;
        }
    }

    private void presentGratuitySelection() {
        Log.i(TAG,"presentGratuitySelection entry ");
        // Adding fragment dynamically to another fragment; use getChildFragmentManager()
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        GratuitySelection gratuitySelection = new GratuitySelection();
        gratuitySelection.setListener(this);
        if (replace) {
            transaction.replace(R.id.paymentControllerFrame, gratuitySelection);
        } else {
            transaction.add(R.id.paymentControllerFrame, gratuitySelection);
        }
        transaction.commit();

    }

    private void presentWaitingCardPresent() {
        Log.i(TAG,"presentWaitingCardPresent entry ");
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        WaitCardPresent waitCardPresent = new WaitCardPresent();
        waitCardPresent.setListener(this);
        transaction
                .replace(R.id.paymentControllerFrame, waitCardPresent)
                .commit();
    }

    private void presentProcessPayment() {
        Log.i(TAG,"presentProcessPayment entry ");
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        WaitForCardProcess waitCardPresent = new WaitForCardProcess();
        waitCardPresent.setListener(this);
        waitCardPresent.setTransaction(currentTransaction);
        if (isRestaurant) {
            waitCardPresent.setStep(isRestaurant);
            waitCardPresent.setStep(firstStep);
        }
        waitCardPresent.setTip(mTip);
        waitCardPresent.setSplit(mSplit);
        if (mSplit) {
            waitCardPresent.splitAmount(mSplitAmount);
        }
        if (manualPayment) {
            waitCardPresent.manualPayment(manualPayment);
        }
        transaction
                .replace(R.id.paymentControllerFrame, waitCardPresent)
                .commit();

    }

    private void presentReceiptOption() {
        Log.i(TAG,"presentReceiptOption entry ");
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transactionComplete = new TransactionComplete();
        transactionComplete.setListener(this);
        transactionComplete.setTransaction(currentTransaction);
        transaction
                .replace(R.id.paymentControllerFrame, transactionComplete)
                .commit();

    }

    private void presentCardDeclined() {
        Log.i(TAG,"presentCardDeclined entry ");
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        CardDeclined cardDeclined = new CardDeclined();
        cardDeclined.setListener(this);
        transaction
                .replace(R.id.paymentControllerFrame, cardDeclined)
                .commit();

    }

    @Override
    public void onGratuityCancel() {
        mListener.onPaymentControllerCancel();
    }

    @Override
    public void onGratuityBack() {
        mListener.onPaymentControllerBack();
    }

    @Override
    public void onGratuitySelect(BigDecimal tip) {
        mTip = tip;
        paymentState = PaymentState.processingPayment;
        firstStep = false;
        paymentWorkflow();
    }

    public void setListener(PaymentCardController.OnFragmentInteractionListener listener) {
        mListener = listener;
    }

    public void setTransaction(MTTransaction transaction) {
        currentTransaction = transaction;
    }

    public void setSplit(boolean split) {
        mSplit = split;
    }

    public void setSplitAmount(BigDecimal amount) {
        mSplitAmount = amount;
    }

    public void setManualPayment(boolean manual) {
        this.manualPayment = manual;
    }

    @Override
    public void waitCardCancel() {
        mListener.onPaymentControllerCancel();
    }

    @Override
    public void waitCardBack() {
        mListener.onPaymentControllerBack();
    }

    @Override
    public void waitTender(boolean type) {
        isRestaurant = type;
        if (isRestaurant) {
            firstStep = true;
        }
        paymentState = PaymentState.processingPayment;
        paymentWorkflow();

    }

    @Override
    public void onWaitCardDeinied() {
        paymentState = PaymentState.paymentRejected;
        paymentWorkflow();
    }

    @Override
    public void onWaitCancel() {
        mListener.onPaymentControllerCancel();

    }

    @Override
    public void onWaitSettle(int method, Payment payment) {

        receipt = method;
        mListener.onPaymentControllerPaymentInfo(payment);
        paymentState = PaymentState.waitingForReceiptOption;
        paymentWorkflow();
    }

    @Override
    public void transactionComplete() {
        /*if (isRestaurant && firstStep) {
            paymentState = PaymentState.gratuitySelection;
            paymentWorkflow();

        } else {*/
        mListener.onPaymentControllerSettle();
        //}
    }

    // Hide or flow alternative button after payment complete.
    @Override
    public void fragmentReady() {
        switch (receipt) {
            case DELIVERY_METHOD_PRINT:
                //changed to display both buttons.
                transactionComplete.setButtons(true, true);
                break;
            default: {
                transactionComplete.setButtons(true, true);
            }
            break;
        }
    }

    @Override
    public void onDeclineCancel() {
        mListener.onPaymentControllerCancel();
    }

    @Override
    public void onDeclineChangeMethod() {
        mListener.onPaymentControllerDenied();
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnFragmentInteractionListener {
        void onPaymentControllerCancel();

        void onPaymentControllerBack();

        void onPaymentControllerPaymentInfo(Payment payment);

        void onPaymentControllerSettle();

        void onPaymentControllerDenied();
    }
}

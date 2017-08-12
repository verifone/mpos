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

import com.bugsee.library.Bugsee;
import com.flurry.android.FlurryAgent;
import com.verifone.commerce.entities.Payment;
import com.verifone.swordfish.manualtransaction.MTDataModel.MTTransaction;
import com.verifone.swordfish.manualtransaction.MTDataModel.MTTransactionHistory;
import com.verifone.swordfish.manualtransaction.PaymentSplitState;
import com.verifone.swordfish.manualtransaction.R;
import com.verifone.swordfish.manualtransaction.SupportFragments.CardAmountEntry;
import com.verifone.swordfish.manualtransaction.SupportFragments.CashCompanion;
import com.verifone.swordfish.manualtransaction.SupportFragments.PaymentCardController;
import com.verifone.swordfish.manualtransaction.SupportFragments.SplitCashChangeDue;
import com.verifone.swordfish.manualtransaction.SupportFragments.SplitMethodSelection;
import com.verifone.swordfish.manualtransaction.SupportFragments.TransactionComplete;
import com.verifone.swordfish.manualtransaction.SupportFragments.TransactionSplitPaymentCompanion;
import com.verifone.swordfish.manualtransaction.SupportFragments.TransactionSplitPaymentDetail;
import com.verifone.swordfish.manualtransaction.Tools.MposLogger;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Objects;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link TransactionSplitPayment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link TransactionSplitPayment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TransactionSplitPayment extends Fragment implements
        TransactionSplitPaymentDetail.OnFragmentInteractionListener,
        TransactionSplitPaymentCompanion.OnFragmentInteractionListener,
        SplitMethodSelection.OnFragmentInteractionListener,
        CashCompanion.OnFragmentInteractionListener,
        SplitCashChangeDue.OnFragmentInteractionListener,
        TransactionComplete.OnFragmentInteractionListener,
        PaymentCardController.OnFragmentInteractionListener,
        CardAmountEntry.OnFragmentInteractionListener {

    private static final String TAG = TransactionSplitPayment.class.getSimpleName();
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;
    private TransactionSplitPaymentDetail transactionSplitPaymentDetail;
    private BigDecimal transactionTotal;
    private BigDecimal mSplitAmount;
    private boolean cashSelected = false;
    private boolean firstTime = true;
    private PaymentSplitState paymentSplitState;
    private int paymentsDone = 0;
    private MTTransaction currentTransaction;
    private Bugsee mBugseeAnalytics;
    private BigDecimal changeDue;

    public TransactionSplitPayment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment TransactionSplitPayment.
     */
    public static TransactionSplitPayment newInstance(String param1, String param2) {
        TransactionSplitPayment fragment = new TransactionSplitPayment();
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
        View view = inflater.inflate(R.layout.fragment_transaction_split_payment, container, false);
        if (transactionSplitPaymentDetail == null) {
            transactionSplitPaymentDetail = new TransactionSplitPaymentDetail();
            transactionSplitPaymentDetail.setmListener(this);
            transactionSplitPaymentDetail.setTotal(transactionTotal);
            paymentSplitState = PaymentSplitState.methodSelection;
        }
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

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "OnResume");
        //FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
        if (!transactionSplitPaymentDetail.isAdded()) {
            Log.i(TAG, "Inside isAdded");
            fragmentTransaction.add(R.id.splitList, transactionSplitPaymentDetail).commit();
        }
        Bugsee.trace("On Resume", MTTransactionHistory.getInstance());
        workflow();

    }

    private void registerScreenName(PaymentSplitState state) {
        Log.i(TAG, "registerScreenName : " + state.toString());
        HashMap<String, String> params = new HashMap<>();
        params.put("ITEM_CATEGORY", "Screen");
        params.put("ITEM_NAME", "Screen name");
        params.put("Action", state.toString());
        //Bugsee.event("Transaction Split Payment event", params);
        FlurryAgent.logEvent("Transaction Split Payment event", params);
    }

    private void registerEvent(PaymentSplitState state) {
        MposLogger.getInstance().debug(TAG, state.toString());

    }

    private void workflow() {
        Log.d(TAG, "Inside WorkFlow");
        registerScreenName(paymentSplitState);
        registerEvent(paymentSplitState);
        switch (paymentSplitState) {
            case methodSelection:
                presentMethodSelection();
                break;
            case cashPayment:
                presentMethodCash();
                break;
            case cashChange:
                presentCashChange();
                break;
            case cardAmountInput:
                presentCardAmount();
                break;
            case cardPayment:
                presentCardCharge();
                break;
            case splitSettle:
                presentTransactionSettle();
                break;
        }
    }

    private void presentMethodSelection() {
        Log.d(TAG, "Inside presentMethodSelection");
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        SplitMethodSelection splitMethodSelection = new SplitMethodSelection();
        splitMethodSelection.setListener(this);
        if (paymentsDone == 0) {
            splitMethodSelection.setCancelButton(true);
        } else {
            splitMethodSelection.setCancelButton(false);
        }
        if (firstTime) {
            fragmentTransaction.add(R.id.splitCompanion, splitMethodSelection)
                    .commit();
            firstTime = false;
        } else {
            fragmentTransaction.replace(R.id.splitCompanion, splitMethodSelection)
                    .commit();

        }
    }

    private void presentMethodCash() {
        Log.d(TAG, "Inside presentMethodCash");
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        CashCompanion cashCompanion = new CashCompanion();
        cashCompanion.setListener(this);
        if (paymentsDone == 0) {
            cashCompanion.setCancelButton(true);
        } else {
            cashCompanion.setCancelButton(false);
        }
        cashCompanion.setTransaction(currentTransaction);
        cashCompanion.setTenderButton(true);
        //cashCompanion.setBackButton(true);
        fragmentTransaction.replace(R.id.splitCompanion, cashCompanion)
                .commit();
        transactionSplitPaymentDetail.onCashSelected();
        cashSelected = true;

    }

    private void presentCashChange() {
        Log.d(TAG, "Inside presentCashChange");
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        SplitCashChangeDue cashCompanion = new SplitCashChangeDue();
        cashCompanion.setListener(this);
        cashCompanion.setChangeDue(changeDue);
        fragmentTransaction.replace(R.id.splitCompanion, cashCompanion)
                .commit();
    }

    private void presentCardAmount() {
        Log.d(TAG, "Inside presentCardAmount");
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        CardAmountEntry cardAmountEntry = new CardAmountEntry();
        cardAmountEntry.setListener(this);
        boolean canCancel = false;
        if (currentTransaction.transactionPayments() != null && currentTransaction.transactionPayments().getPayments().size() == 0)
            canCancel = true;
        cardAmountEntry.setCancelButton(canCancel);
        fragmentTransaction.replace(R.id.splitCompanion, cardAmountEntry)
                .commit();

    }

    private void presentCardCharge() {
        Log.d(TAG, "Inside presentCardCharge");
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        PaymentCardController paymentCardController = new PaymentCardController();
        paymentCardController.setListener(this);
        paymentCardController.setTransaction(currentTransaction);
        paymentCardController.setSplit(true);
        paymentCardController.setSplitAmount(mSplitAmount);
        fragmentTransaction.replace(R.id.splitCompanion, paymentCardController)
                .commit();


    }

    private void presentTransactionSettle() {
        Log.d(TAG, "Inside presentTransactionSettle");
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        TransactionComplete transactionComplete = new TransactionComplete();
        transactionComplete.setListener(this);
        fragmentTransaction.replace(R.id.splitCompanion, transactionComplete)
                .commit();
        //transactionSplitPaymentDetail.onCashSelected();

    }

    @Override
    public void onSplitCashPayment() {

    }


    @Override
    public void onMethodCancel() {
        mListener.onSplitCancel();
    }

    @Override
    public void onMethodBack() {
        mListener.onSplitBack();
    }

    @Override
    public void onCashSelected() {
        paymentSplitState = PaymentSplitState.cashPayment;
        workflow();
    }

    @Override
    public void onCardSelected() {
        Log.d(TAG, "onCardSelected");
        paymentSplitState = PaymentSplitState.cardAmountInput;
        transactionSplitPaymentDetail.onCreditSelected();
        workflow();

    }

    @Override
    public void onCreditSelected() {
        transactionSplitPaymentDetail.onCreditSelected();
    }

    @Override
    public void onSplitPrintReceipt() {

    }

    @Override
    public void onSplitEmailReceipt() {

    }


    @Override
    public void onSplitNoReceipt() {

    }

    @Override
    public void onButtonPress(int buttonID) {
        switch (buttonID) {
            case 0:
                mListener.onSplitCancel();
                break;
            case 1:
                break;
            case 2:
                if (cashSelected) {
                    goToPaySelection();
                    break;
                }
                mListener.onSplitBack();
                break;
            default:
                break;
        }

    }


    @Override
    public void onKeyboardButtonPress(String title) {
        transactionSplitPaymentDetail.attachValue(title);
    }

    @Override
    public void onButtonPress(View view) {
        int amount;
        if (Integer.parseInt((String) view.getTag()) > -1) {
            amount = Integer.parseInt((String) view.getTag());
        } else {
            amount = view.getId();
        }

        transactionSplitPaymentDetail.onCashKeyboard(amount);
    }

    public void setListener(TransactionSplitPayment.OnFragmentInteractionListener listener) {
        mListener = listener;
    }

    public void setTotal(BigDecimal total) {
        transactionTotal = total;
    }

    public void setTransaction(MTTransaction transaction) {
        currentTransaction = transaction;
    }

    private void goToPaySelection() {
        Log.d(TAG, "goToPaymentSelection");
        cashSelected = false;
        Payment cashPayment = new Payment();
        cashPayment.setPaymentType(Payment.PaymentType.CASH);
        cashPayment.setPaymentAmount(transactionSplitPaymentDetail.totalForCashSettle());
        paymentsDone++;
        currentTransaction.addPayment(cashPayment);
        if (!checkTransactionSettle()) {
            if (Objects.equals(transactionSplitPaymentDetail.getChangeDue(), BigDecimal.ZERO)) {
                paymentSplitState = PaymentSplitState.methodSelection;
            } else {
                changeDue = transactionSplitPaymentDetail.getChangeDue();
                paymentSplitState = PaymentSplitState.cashChange;
            }
        } else {
            paymentSplitState = PaymentSplitState.splitSettle;
        }
        transactionSplitPaymentDetail.cashSettle(cashPayment);
        transactionSplitPaymentDetail.resetView();
        workflow();
    }

    private boolean checkTransactionSettle() {
        Log.i(TAG, "checkTransactionSettle");
        BigDecimal amountPaid = BigDecimal.ZERO;
        for (Payment payment : currentTransaction.transactionPayments().getPayments()) {
            amountPaid = amountPaid.add(payment.getPaymentAmount());
        }
        return (amountPaid.doubleValue() >= currentTransaction.getTransactionTotal().doubleValue());
    }

    private BigDecimal pendingBalance() {
        BigDecimal amountPaid = BigDecimal.ZERO;
        if (currentTransaction == null || currentTransaction.transactionPayments() == null) {
            return currentTransaction.getTransactionTotal();
        }
        for (Payment payment : currentTransaction.transactionPayments().getPayments()) {
            amountPaid = amountPaid.add(payment.getPaymentAmount());
        }
        return currentTransaction.getTransactionTotal().subtract(amountPaid);

    }

    @Override
    public void onNextSelected() {
        paymentSplitState = PaymentSplitState.methodSelection;
        workflow();
    }

    @Override
    public void transactionComplete() {
        mListener.onSplitTransactionSettle();
    }

    @Override
    public void fragmentReady() {

    }

    @Override
    public void onPaymentControllerCancel() {
        paymentSplitState = PaymentSplitState.methodSelection;
        workflow();
    }

    @Override
    public void onPaymentControllerBack() {
        paymentSplitState = PaymentSplitState.methodSelection;
        workflow();
    }

    @Override
    public void onPaymentControllerPaymentInfo(Payment payment) {

        currentTransaction.addPayment(payment);
        transactionSplitPaymentDetail.creditSettle(payment);
        transactionSplitPaymentDetail.resetView();
    }

    @Override
    public void onPaymentControllerSettle() {
        if (!checkTransactionSettle()) {
            paymentSplitState = PaymentSplitState.methodSelection;
        } else {
            paymentSplitState = PaymentSplitState.splitSettle;
        }
        workflow();

    }

    @Override
    public void onPaymentControllerDenied() {
        paymentSplitState = PaymentSplitState.methodSelection;
        workflow();
    }

    @Override
    public void cardKeyboardEntry(String title) {
        transactionSplitPaymentDetail.attachValue(title);
    }

    @Override
    public void cardCancel() {
        mListener.onSplitCancel();
    }

    @Override
    public void cardBack() {
        paymentSplitState = PaymentSplitState.methodSelection;
        workflow();

    }

    @Override
    public void cardCharge() {
        mSplitAmount = transactionSplitPaymentDetail.amountToCharge();
        if (mSplitAmount.doubleValue() > pendingBalance().doubleValue()) {
            transactionSplitPaymentDetail.overCharged();
        } else {
            paymentSplitState = PaymentSplitState.cardPayment;
            workflow();
        }

    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnFragmentInteractionListener {
        void onSplitTransactionSettle();

        void onSplitCancel();

        void onSplitBack();
    }
}

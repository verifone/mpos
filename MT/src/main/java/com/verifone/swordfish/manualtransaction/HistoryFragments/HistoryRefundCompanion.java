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

import com.verifone.commerce.entities.Payment;
import com.verifone.swordfish.manualtransaction.MTDataModel.MTTransaction;
import com.verifone.swordfish.manualtransaction.PaymentRefundState;
import com.verifone.swordfish.manualtransaction.R;
import com.verifone.swordfish.manualtransaction.SupportFragments.TransactionComplete;
import com.verifone.swordfish.manualtransaction.System.PaymentTerminal;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link HistoryRefundCompanion.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link HistoryRefundCompanion#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HistoryRefundCompanion extends Fragment implements
        HistoryRefundCompanionSelection.OnFragmentInteractionListener,
        HistoryRefundCompanionType.OnFragmentInteractionListener,
        HistoryRefundCompanionConfirmation.OnFragmentInteractionListener,
        TransactionComplete.OnFragmentInteractionListener {

    private static final String TAG = HistoryRefundCompanion.class.getSimpleName();
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;
    private PaymentRefundState paymentRefundState;
    private Payment mPayment;
    private Payment.PaymentType refundSelection;
    private MTTransaction mCurrentTransaction;

    public HistoryRefundCompanion() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HistoryRefundCompanion.
     */
    public static HistoryRefundCompanion newInstance(String param1, String param2) {
        HistoryRefundCompanion fragment = new HistoryRefundCompanion();
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
        return inflater.inflate(R.layout.fragment_history_refund_companion, container, false);
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onResume() {
        super.onResume();
        paymentRefundState = PaymentRefundState.selection;
        mPayment = mCurrentTransaction.transactionPayments().getPayments().get(0);
        workflow();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void moveToState(PaymentRefundState state) {
        paymentRefundState = state;
        workflow();
    }

    private void registerScreenName(PaymentRefundState state) {
        Log.i(TAG, "registerScreenName : " + state.toString());
    }

    private void workflow() {
        registerScreenName(paymentRefundState);
        switch (paymentRefundState) {
            case selection:
                presentSelection();
                break;
            case typeSelection:
                presentTypeSelection();
                break;
            case issueConfirmation:
                presentIssueConfirmation();
                break;
            case cancelSelected:
                presentCancelSelected();
                break;
            case processingRefund:
                presentProcessingRefund();
                break;
            case refundCompleted:
                presentRefundCompleted();
                break;
            default:
                break;
        }
    }

    private void presentSelection() {
        HistoryRefundCompanionSelection selection = new HistoryRefundCompanionSelection();
        selection.setListener(this);
        refundSelection = mPayment.getPaymentType();
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction
                .add(R.id.refundCompanionFrame, selection)
                .commit();

    }

    private void presentTypeSelection() {
        HistoryRefundCompanionType refundCompanionType = new HistoryRefundCompanionType();
        refundCompanionType.setListener(this);
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction
                .replace(R.id.refundCompanionFrame, refundCompanionType)
                .commit();
    }

    private void presentIssueConfirmation() {
        HistoryRefundCompanionConfirmation confirmation = new HistoryRefundCompanionConfirmation();
        confirmation.setListener(this);
        confirmation.setPaymentType(refundSelection);
        confirmation.setPayment(mCurrentTransaction.transactionPayments().getPayments().get(0));
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction
                .replace(R.id.refundCompanionFrame, confirmation)
                .commit();
    }

    private void presentCancelSelected() {
        mListener.onCancelRefund();
    }

    private void presentProcessingRefund() {
        VoidCompanionProcessing processing = new VoidCompanionProcessing();
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction
                .replace(R.id.refundCompanionFrame, processing)
                .commit();
    }

    private void presentRefundCompleted() {
        mListener.onRefundComplete();
        TransactionComplete complete = new TransactionComplete();
        complete.setListener(this);
        complete.setTransaction(mCurrentTransaction);
        complete.setButtons(true, true);
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction
                .replace(R.id.refundCompanionFrame, complete)
                .commit();

    }

    //Refund selection listener methods
    @Override
    public void onCancelRefund() {
        mListener.onCancelRefund();
    }

    @Override
    public void onIssueRefund() {
        switch (refundSelection) {
            case CASH:
                paymentRefundState = PaymentRefundState.refundCompleted;
                break;
            case DEBIT: {
                paymentRefundState = PaymentRefundState.processingRefund;
                PaymentTerminal paymentTerminal = PaymentTerminal.getInstance();
                PaymentTerminal.PaymentTerminalPaymentEvents events = new PaymentTerminal.PaymentTerminalPaymentEvents() {
                    @Override
                    public void paymentStarted() {

                    }

                    @Override
                    public void onSuccess(Payment paymentInfo, int method, String customer) {
                        mCurrentTransaction.setStatus(TransactionStatus.refunded);
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                paymentRefundState = PaymentRefundState.refundCompleted;
                                workflow();
                            }
                        });

                    }

                    @Override
                    public void onVoiceAuthorizationRequest(String message) {

                    }

                    @Override
                    public void onSignatureRequest() {

                    }

                    @Override
                    public void onReceiptRequest() {

                    }

                    @Override
                    public void onSuccessEmailRequest(String email) {

                    }

                    @Override
                    public void onNoReceipt() {

                    }

                    @Override
                    public void onDeclined() {

                    }

                    @Override
                    public void onCancel() {

                    }

                    @Override
                    public void onFailure() {

                    }

                    @Override
                    public void onTimeOut() {

                    }
                };
                paymentTerminal.refundTransaction(mPayment);
            }
            default:
                break;
        }
        workflow();

    }


    @Override
    public void onRefund() {

        switch (mPayment.getPaymentType()) {
            case CASH:
                paymentRefundState = PaymentRefundState.issueConfirmation;
                break;
            case DEBIT:
                paymentRefundState = PaymentRefundState.typeSelection;
                break;
            default:
                break;
        }
        workflow();
    }

    //Refund type selection listener methods
    @Override
    public void onCashSelected() {
        paymentRefundState = PaymentRefundState.issueConfirmation;
        refundSelection = Payment.PaymentType.CASH;
        workflow();
    }

    @Override
    public void onCreditSelected() {
        //todo implement refund with payment terminal (voidCompanionProcessing?)
        refundSelection = Payment.PaymentType.DEBIT;
        paymentRefundState = PaymentRefundState.issueConfirmation;
        workflow();

    }

    @Override
    public void onBack() {
        paymentRefundState = PaymentRefundState.selection;
        workflow();
    }

    //Transaction complete listener methods
    @Override
    public void transactionComplete() {
        mListener.onRefundSettle();
    }

    @Override
    public void fragmentReady() {

    }

    public void setListener(HistoryRefundCompanion.OnFragmentInteractionListener listener) {
        mListener = listener;
    }

    public void setTransaction(MTTransaction transaction) {
        mCurrentTransaction = transaction;
    }


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnFragmentInteractionListener {

        void onPaymentState(PaymentRefundState state);

        void onCancelRefund();

        void onRefundComplete();

        void onRefundSettle();
    }
}

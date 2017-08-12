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

package com.verifone.swordfish.manualtransaction;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.flurry.android.FlurryAgent;
import com.verifone.commerce.entities.Merchandise;
import com.verifone.swordfish.manualtransaction.MTDataModel.MTTransaction;
import com.verifone.swordfish.manualtransaction.MTDataModel.MTTransactionHistory;
import com.verifone.swordfish.manualtransaction.State.PaymentSessionModel;
import com.verifone.swordfish.manualtransaction.SupportFragments.CancelTransactionFragment;
import com.verifone.swordfish.manualtransaction.SupportFragments.WaitForPT;
import com.verifone.swordfish.manualtransaction.System.PaymentTerminal;
import com.verifone.swordfish.manualtransaction.Tools.MposLogger;
import com.verifone.swordfish.manualtransaction.TransactionFrames.TransactionCardPayment;
import com.verifone.swordfish.manualtransaction.TransactionFrames.TransactionCashPayment;
import com.verifone.swordfish.manualtransaction.TransactionFrames.TransactionEntry;
import com.verifone.swordfish.manualtransaction.TransactionFrames.TransactionPaymentSelection;
import com.verifone.swordfish.manualtransaction.TransactionFrames.TransactionSplitPayment;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Set;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link TransactionController.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link TransactionController#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TransactionController extends Fragment implements
        TransactionEntry.TransactionEntryAPI,
        TransactionPaymentSelection.OnFragmentInteractionListener,
        TransactionCardPayment.OnFragmentInteractionListener,
        TransactionCashPayment.OnFragmentInteractionListener,
        CancelTransactionFragment.OnFragmentInteractionListener,
        TransactionSplitPayment.OnFragmentInteractionListener {

    private static final String TAG = TransactionController.class.getSimpleName();
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private CancelTransactionFragment cancelTransactionFragment;
    private TransactionState transactionState;
    private PaymentTerminal paymentTerminal;
    private MTTransaction currentTransaction;
    PaymentTerminal.PaymentTerminalSessionEvents listner;
    PaymentSessionModel mPaymentSessionModel;


    public TransactionController() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment TransactionController.
     */
    public static TransactionController newInstance(String param1, String param2) {
        TransactionController fragment = new TransactionController();
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
            getArguments().getString(ARG_PARAM1);
            getArguments().getString(ARG_PARAM2);
        }
        mPaymentSessionModel = ViewModelProviders.of(getActivity()).get(PaymentSessionModel.class);
        transactionState = TransactionState.notStarted;
        paymentTerminal = PaymentTerminal.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_transaction_controller, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onResume() {
        super.onResume();
        mPaymentSessionModel.setReplaceState(true);
        FlurryAgent.logEvent("On Resume" + MTTransactionHistory.getInstance());
        transactionWorkflow();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        paymentTerminal.stopSession();
    }

    private void registerNewSession() {
        FlurryAgent.logEvent("SET NEW SESSION");
    }

    private void registerScreenName(TransactionState state) {
        Log.i(TAG, "registerScreenName : " + state.toString());

        HashMap<String, String> params = new HashMap<>();
        params.put("ITEM_CATEGORY", "Screen");
        params.put("ITEM_NAME", "Screen name");
        params.put("Action", state.toString());
        FlurryAgent.logEvent("Transaction Controller event", params);
    }

    private void registerEvent(String event) {
        MposLogger.getInstance().debug(TAG, event);
    }

    private void transactionWorkflow() {
        registerScreenName(transactionState);
        switch (transactionState) {
            case notStarted:
                startPaymentManager();
                break;
            case dataEntry:
                presentDataEntry();
                break;
            case paymentSelection:
                paymentSelection();
                break;
            case cardPayment:
                cardPayment();
                break;
            case manualCardPayment:
                manualCardPayment();
                break;
            case cashPayment:
                cashPayment();
                break;
            case splitPayment:
                splitPayment();
                break;
            case paymentAccepted:
                transactionSettle();
                break;
            case paymentDeclined:
                paymentSelection();
                break;
            default:
                break;
        }
    }

    private void paymentSessionConfiguration() {
        Log.d(TAG, "paymentSessionConfiguration");
        listner = new PaymentTerminal.PaymentTerminalSessionEvents() {
            @Override
            public void sessionStarted() {
                if (!mPaymentSessionModel.isSessionStarted()) {
                    currentTransaction = new MTTransaction();
                    mPaymentSessionModel.setSettleState(false);
                    mPaymentSessionModel.setSessionStarted(true);
                    transactionState = TransactionState.dataEntry;
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            registerNewSession();
                            transactionWorkflow();
                        }
                    });}
            }

            @Override
            public void processStart() {
                Log.d(TAG, "processStart");
            }

            @Override
            public void processEnds() {
                Log.d(TAG, "processEnds");
                if (mPaymentSessionModel.isSessionStarted()) {
                    //started = false;
                    mPaymentSessionModel.setSessionStarted(false);
                    if (mPaymentSessionModel.isSettleState()) {
                        MTTransactionHistory transactionHistory = MTTransactionHistory.getInstance();
                        currentTransaction.setTransactionTotal(mPaymentSessionModel.getTxTotal());
                        transactionHistory.addTransaction(currentTransaction);
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            transactionState = TransactionState.notStarted;
                            transactionWorkflow();

                        }
                    });
                }
            }

            @Override
            public void onFailure() {
                //since session failed, we try to open again
                transactionState = TransactionState.notStarted;
                transactionWorkflow();

            }

            @Override
            public void onTimeOut() {

            }
        };
    }

    private void startPaymentManager() {
        Log.d(TAG, "startPaymentManager");
        if (listner == null) {
            paymentSessionConfiguration();
            paymentTerminal.setSessionListener(listner);
        }
        // Do not start the session here. Instead move to lineItem call so payment can kick-in the idle time.
        paymentTerminal.startSession();
    }

    // Progressbar displayed waiting for payment terminal
    private void presentWaitView() {
        FragmentManager manager = getActivity().getSupportFragmentManager();
        WaitForPT waitForPT = new WaitForPT();
        manager.beginTransaction()
                .replace(R.id.transactionEntryFrame, waitForPT)
                .commit();

    }

    private void presentDataEntry() {
        Log.d(TAG, "presentDataEntry ");
        FragmentManager manager = getActivity().getSupportFragmentManager();
        TransactionEntry transactionEntry = new TransactionEntry();
        transactionEntry.setListener(this);
        transactionEntry.setTransaction(currentTransaction);
        if (!mPaymentSessionModel.isReplaceState()) {
            Log.d(TAG, "Add transaction entry");
            manager.beginTransaction()
                    .add(R.id.transactionEntryFrame, transactionEntry)
                    .commit();
        } else {
            Log.d(TAG, "Replace transaction entry");
            manager.beginTransaction()
                    .replace(R.id.transactionEntryFrame, transactionEntry)
                    .commit();
            mPaymentSessionModel.setReplaceState(false);
        }
    }

    private void paymentSelection() {
        Log.d(TAG, "paymentSelection entry ");
        FragmentManager manager = getActivity().getSupportFragmentManager();
        TransactionPaymentSelection transactionPaymentSelection = new TransactionPaymentSelection();
        transactionPaymentSelection.setListener(this);
        transactionPaymentSelection.setTotal(currentTransaction.getTransactionTotal());
        transactionPaymentSelection.setTransaction(currentTransaction);
        manager.beginTransaction()
                .replace(R.id.transactionEntryFrame, transactionPaymentSelection)
                .commit();

    }

    private void cardPayment() {
        Log.d(TAG, "cardPayment entry ");
        FragmentManager manager = getActivity().getSupportFragmentManager();
        TransactionCardPayment transactionCardPayment = new TransactionCardPayment();
        transactionCardPayment.setListener(this);
        transactionCardPayment.setTotal(currentTransaction.getTransactionTotal());
        transactionCardPayment.setTransaction(currentTransaction);
        transactionCardPayment.setSplit(false);
        transactionCardPayment.setManual(false);
        manager.beginTransaction()
                .replace(R.id.transactionEntryFrame, transactionCardPayment)
                .commit();

    }

    private void manualCardPayment() {
        Log.d(TAG, "manualCardPayment entry ");
        FragmentManager manager = getActivity().getSupportFragmentManager();
        TransactionCardPayment transactionCardPayment = new TransactionCardPayment();
        transactionCardPayment.setListener(this);
        transactionCardPayment.setTotal(currentTransaction.getTransactionTotal());
        transactionCardPayment.setTransaction(currentTransaction);
        transactionCardPayment.setSplit(false);
        transactionCardPayment.setManual(true);
        manager.beginTransaction()
                .replace(R.id.transactionEntryFrame, transactionCardPayment)
                .commit();

    }

    private void cashPayment() {
        Log.d(TAG, "cashPayment entry ");
        FragmentManager manager = getActivity().getSupportFragmentManager();
        TransactionCashPayment transactionCashPayment = new TransactionCashPayment();
        transactionCashPayment.setListener(this);
        transactionCashPayment.setTransaction(currentTransaction);
        transactionCashPayment.setTotal(currentTransaction.getTransactionTotal());
        manager.beginTransaction()
                .replace(R.id.transactionEntryFrame, transactionCashPayment)
                .commit();

    }

    private void splitPayment() {
        Log.d(TAG, "splitPayment entry");
        FragmentManager manager = getActivity().getSupportFragmentManager();
        TransactionSplitPayment transactionSplitPayment = new TransactionSplitPayment();
        transactionSplitPayment.setListener(this);
        transactionSplitPayment.setTotal(currentTransaction.getTransactionTotal());
        transactionSplitPayment.setTransaction(currentTransaction);
        manager.beginTransaction()
                .replace(R.id.transactionEntryFrame, transactionSplitPayment)
                .commit();
    }

    private void transactionSettle() {
        mPaymentSessionModel.setReplaceState(true);
        paymentTerminal.stopSession();
        //presentWaitView();
    }

    @Override
    public void transactionCancel() {
        presentCancelFragment();
    }

    @Override
    public void payTransaction(Set<Merchandise> items, BigDecimal transactionTotal) {
        transactionState = TransactionState.paymentSelection;
        //txTotal = transactionTotal;
        mPaymentSessionModel.setTxTotal(transactionTotal);
        currentTransaction.setTransactionTotal(transactionTotal);
        //presentWaitView();
        transactionWorkflow();
    }

    @Override
    public void cancelTransaction() {
        presentCancelFragment();
    }

    @Override
    public void splitTransaction() {
        transactionState = TransactionState.splitPayment;
        transactionWorkflow();
    }

    @Override
    public void goBack() {
        switch (transactionState) {
            case paymentSelection:
                transactionState = TransactionState.dataEntry;
                //replace = true;
                break;
            case cardPayment:
            case cashPayment:
            case splitPayment:
                transactionState = TransactionState.paymentSelection;
                //replace = true;
                break;
            default:
                break;
        }
        mPaymentSessionModel.setReplaceState(true);
        transactionWorkflow();
    }

    @Override
    public void settle() {
        Log.d(TAG, "settle");
        registerEvent("Card payment accepted");
        transactionState = TransactionState.paymentAccepted;
        mPaymentSessionModel.setSettleState(true);
        transactionWorkflow();
    }

    @Override
    public void payByCash() {
        Log.d(TAG, "payByCash");
        transactionState = TransactionState.cashPayment;
        mPaymentSessionModel.setSettleState(true);
        transactionWorkflow();

    }

    @Override
    public void payByCard() {
        Log.d(TAG, "payByCard");
        transactionState = TransactionState.cardPayment;
        transactionWorkflow();

    }

    @Override
    public void payByManualEntry() {
        Log.d(TAG, "payByManualEntry");
        transactionState = TransactionState.manualCardPayment;
        transactionWorkflow();

    }

    @Override
    public void cashSettle() {
        Log.d(TAG, "cashSettle");
        transactionState = TransactionState.paymentAccepted;
        registerEvent("Cash payment accepted");
        //presentWaitView();
        //the transaction is add to the history in the process end listener of the payment terminal
        transactionWorkflow();
    }

    @Override
    public void cashGoBack() {
        Log.d(TAG, "cashGoBack");
        goBack();
    }

    @Override
    public void cashCancelTransaction() {
        Log.d(TAG, "cashCancelTransaction");
        presentCancelFragment();
    }

    @Override
    public void abortCancelFragment() {
        dismissCancelFragment();
    }

    @Override
    public void proceedCancelFragment() {
        Log.d(TAG, "Transaction cancel accepted");
        registerEvent("Transaction cancel accepted");
        dismissCancelFragment();
        //replace = true;
        mPaymentSessionModel.setReplaceState(true);
        paymentTerminal.deleteAllItems();
        //isCancel = true;
        paymentTerminal.stopSession();
        //presentWaitView();
    }

    private void presentCancelFragment() {
        Log.d(TAG, "presentCancelFragment");
        cancelTransactionFragment = new CancelTransactionFragment();
        cancelTransactionFragment.setmListener(this);
        cancelTransactionFragment.setMessageToShow(getActivity().getResources().getString(R.string.cancelTransactionLabel));
        FragmentManager manager = getActivity().getSupportFragmentManager();
        manager.beginTransaction()
                .add(R.id.transactionEntryFrame, cancelTransactionFragment)
                .commit();
    }

    private void dismissCancelFragment() {
        FragmentManager manager = getActivity().getSupportFragmentManager();
        manager.beginTransaction()
                .remove(cancelTransactionFragment)
                .commit();
    }

    @Override
    public void onSplitTransactionSettle() {
        registerEvent("Split payment accepted");
        transactionState = TransactionState.paymentAccepted;
        mPaymentSessionModel.setSettleState(true);
        transactionWorkflow();
    }

    @Override
    public void onSplitCancel() {
        presentCancelFragment();
    }

    @Override
    public void onSplitBack() {
        goBack();
    }
}

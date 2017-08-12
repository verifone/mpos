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

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.bugsee.library.Bugsee;
import com.verifone.commerce.CommerceEvent;
import com.verifone.commerce.CommerceListener;
import com.verifone.commerce.CommerceResponse;
import com.verifone.commerce.entities.Payment;
import com.verifone.commerce.payment.reports.ReconciliationEvent;
import com.verifone.swordfish.manualtransaction.HistoryFragments.HistoryAddNote;
import com.verifone.swordfish.manualtransaction.HistoryFragments.HistoryDetail;
import com.verifone.swordfish.manualtransaction.HistoryFragments.HistoryReceiptRequest;
import com.verifone.swordfish.manualtransaction.HistoryFragments.HistoryRefundController;
import com.verifone.swordfish.manualtransaction.HistoryFragments.HistorySendReceipt;
import com.verifone.swordfish.manualtransaction.HistoryFragments.HistoryState;
import com.verifone.swordfish.manualtransaction.HistoryFragments.HistoryVoid;
import com.verifone.swordfish.manualtransaction.HistoryFragments.TransactionStatus;
import com.verifone.swordfish.manualtransaction.MTDataModel.MTTransaction;
import com.verifone.swordfish.manualtransaction.MTDataModel.MTTransactionHistory;
import com.verifone.swordfish.manualtransaction.SupportFragments.CancelTransactionFragment;
import com.verifone.swordfish.manualtransaction.SupportFragments.WaitForPT;
import com.verifone.swordfish.manualtransaction.System.PaymentTerminal;
import com.verifone.swordfish.manualtransaction.Tools.LocalizeCurrencyFormatter;
import com.verifone.swordfish.manualtransaction.Tools.MposLogger;
import com.verifone.swordfish.manualtransaction.Tools.PrinterUtility;

import java.util.ArrayList;
import java.util.List;

import static com.verifone.swordfish.manualtransaction.HistoryFragments.HistoryState.reconcileCancel;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link HistoryController.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link HistoryController#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HistoryController extends Fragment implements
        HistoryDetail.OnFragmentInteractionListener,
        HistoryAddNote.OnFragmentInteractionListener,
        HistoryReceiptRequest.OnFragmentInteractionListener,
        HistorySendReceipt.OnFragmentInteractionListener,
        CancelTransactionFragment.OnFragmentInteractionListener,
        HistoryVoid.OnFragmentInteractionListener,
        HistoryRefundController.OnFragmentInteractionListener {

    private static final String TAG = HistoryController.class.getSimpleName();
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;
    private FrameLayout detailFrameLayout;
    private View previousSelected;
    private LinearLayout mainLinearLayout;
    private FrameLayout historyDetailLayout;
    private ListView transactionListView;
    private List<MTTransaction> transactions;
    private ViewGroup mtContainer;
    private Spinner spinner;
    private MTTransaction[] transactionsToDisplay;
    private MTTransactionHistory transactionHistory;
    private Object historyDetailCashListener;
    private HistoryState historyState;
    private MTTransaction mCurrentTransaction;
    private HistoryAddNote historyAddNote;
    private HistoryReceiptRequest historyReceiptRequest;
    private HistorySendReceipt historySendReceipt;
    private CancelTransactionFragment cancelTransactionFragment;
    private WaitForPT waitTransactionFragment;
    private HistoryVoid historyVoid;
    private HistoryRefundController refundController;
    private HistoryController mController;
    private CommerceListener mCommerceListener;
    private PaymentTerminal mPaymentTerminal;
    boolean isVisible = false;
    boolean onStart = true;
    boolean onRefundTx = false;
    boolean refundOrVoid = false;
    boolean mReconcile = false;


    public HistoryController() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HistoryController.
     */
    public static HistoryController newInstance(String param1, String param2) {
        HistoryController fragment = new HistoryController();
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
        View view = inflater.inflate(R.layout.fragment_history_controller, container, false);
        mtContainer = container;
        String optionsArray[] = getActivity().getResources().getStringArray(R.array.transaction_history_options);

        spinner = (Spinner) view.findViewById(R.id.spinnerTransactionOptions);
        spinner.setPrompt(optionsArray[0]);

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.transaction_history_options, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new SearchSelectedListener());

        transactionListView = (ListView) view.findViewById(R.id.listViewTH);
        transactionListView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                MposLogger.getInstance().debug("TH onClick: ", view.toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        previousSelected = null;
        detailFrameLayout = (FrameLayout) view.findViewById(R.id.frame_history_detail);

        FrameLayout refundFrameLayout = (FrameLayout) view.findViewById(R.id.fragmentRefund);
        mainLinearLayout = (LinearLayout) view.findViewById(R.id.transactioHistoryMainLL);
        historyDetailLayout = detailFrameLayout;
        historyDetailCashListener = this;
        onRefundTx = false;
        historyState = HistoryState.presentingHistory;
        Button reconcile = (Button) view.findViewById(R.id.batchButton);
        reconcile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reconcileBatch();
            }
        });

        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onResume() {
        super.onResume();
        Bugsee.trace("On Resume", MTTransactionHistory.getInstance());
    }

    public void refresh() {
        mController = this;
        historyState = HistoryState.presentingHistory;
        workflow();
    }

    public void restore() {
        switch (historyState) {
            case addingNote:
                dismissNote();
                break;

            case presentingReceiptOptions:
                dismissReceipt();
                break;

            case emailSelected:
            case smsSelected:
                dismissSMSEmail();
                dismissReceipt();
                break;

            case voidConfirmation:
                dismissVoidConfirmation();
                break;

            case voidSelected:
                dismissVoidController();
                break;

            case refundSelected:
                dismissRefundController();
                break;

            default:
                break;
        }
        historyState = HistoryState.presentingHistory;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private void registerEvent(HistoryState state) {
        MposLogger.getInstance().debug(TAG, state.toString());
    }

    private void registerScreenName(HistoryState state) {
        Log.i(TAG, "registerScreenName : " + state.toString());
    }

    private void workflow() {
        registerEvent(historyState);
        registerScreenName(historyState);
        switch (historyState) {
            case presentingHistory:
                presentHistory();
                break;

            case addingNote:
                presentAddNote();
                break;

            case backFromNote:
                dismissNote();
                break;

            case presentingReceiptOptions:
                presentReceiptOptions();
                break;

            case backFromReceipt:
                dismissReceipt();
                break;

            case smsSelected:
                presentSMS();
                break;

            case emailSelected:
                presentEmail();
                break;

            case backFromSMSEmail:
                dismissSMSEmail();
                break;

            case voidConfirmation:
                presentVoidConfirmation();
                break;

            case voidCanceled:
                dismissVoidConfirmation();
                break;

            case voidSelected:
                presentVoidController();
                break;

            case voidSucess:
            case voidBack:
            case voidFail:
                dismissVoidController();
                break;

            case refundSelected:
                presentRefundController();
                break;

            case refundCancel:
            case refundProcess:
                dismissRefundController();
                break;

            case reconcileConfirmation:
                presentReconcileConfirm();
                break;

            case reconcileCancel:
                dimissRenconcileConfirm();
                break;

            case reconcileSelected:
                presentWaitReconcile();
                break;

            case reconcileProcess:
                dismissWaitReconcile();
            default:
                break;
        }
    }

    private void presentHistory() {
        spinner.setSelection(0);
        if (transactionHistory == null) {
            transactionHistory = MTTransactionHistory.getInstance();
        }
        transactions = transactionHistory.getTransactions();
        if (transactions != null) {
            transactionsToDisplay = new MTTransaction[transactions.size()];
            int index = 0;
            for (index = 0; index < transactions.size(); index++) {
                transactionsToDisplay[index] = transactions.get(index);
            }
        } else {
            transactionsToDisplay = new MTTransaction[0];
        }
        HistoryArrayAdapter<MTTransaction> arrayAdapter = new HistoryArrayAdapter<MTTransaction>(getActivity().getBaseContext(), transactionsToDisplay);
        transactionListView.setAdapter(arrayAdapter);
    }

    private void presentAddNote() {
        historyAddNote = new HistoryAddNote();
        historyAddNote.setListener(this);
        FragmentManager fragmentManager = this.getFragmentManager();
        fragmentManager.beginTransaction()
                .add(R.id.mainHistoryDetailFrameLayout, historyAddNote)
                .commit();
    }

    private void dismissNote() {
        FragmentManager fragmentManager = this.getFragmentManager();
        fragmentManager.beginTransaction()
                .remove(historyAddNote)
                .commit();
    }

    private void presentReceiptOptions() {
        historyReceiptRequest = new HistoryReceiptRequest();
        historyReceiptRequest.setListener(this);
        FragmentManager fragmentManager = this.getFragmentManager();
        fragmentManager.beginTransaction()
                .add(R.id.mainHistoryDetailFrameLayout, historyReceiptRequest)
                .commit();
    }

    private void dismissReceipt() {
        FragmentManager fragmentManager = this.getFragmentManager();
        fragmentManager.beginTransaction()
                .remove(historyReceiptRequest)
                .commit();
    }

    private void presentSMS() {
        presentSendReceipt(0);
    }

    private void presentEmail() {
        presentSendReceipt(1);
    }

    private void presentSendReceipt(int option) {
        historySendReceipt = new HistorySendReceipt();
        historySendReceipt.setType(option);
        historySendReceipt.setListener(this);
        FragmentManager fragmentManager = this.getFragmentManager();
        fragmentManager.beginTransaction()
                .add(R.id.mainHistoryDetailFrameLayout, historySendReceipt)
                .commit();
    }

    private void dismissSMSEmail() {
        FragmentManager fragmentManager = this.getFragmentManager();
        fragmentManager.beginTransaction()
                .remove(historySendReceipt)
                .commit();
    }

    private void presentVoidConfirmation() {
        cancelTransactionFragment = new CancelTransactionFragment();
        cancelTransactionFragment.setMessageToShow(getActivity().getResources().getString(R.string.void_transaction_label));
        cancelTransactionFragment.setmListener(this);
        FragmentManager fragmentManager = this.getFragmentManager();
        fragmentManager.beginTransaction()
                .add(R.id.historyControllerFragment, cancelTransactionFragment)
                .commit();
    }

    private void dismissVoidConfirmation() {
        FragmentManager fragmentManager = this.getFragmentManager();
        fragmentManager.beginTransaction()
                .remove(cancelTransactionFragment)
                .commit();
    }

    private void presentVoidController() {
        historyVoid = new HistoryVoid();
        historyVoid.setListener(this);
        historyVoid.setTransaction(mCurrentTransaction);
        Payment payment = mCurrentTransaction.transactionPayments().getPayments().get(0);
        historyVoid.setPayment(payment);
        FragmentManager fragmentManager = this.getFragmentManager();
        fragmentManager.beginTransaction()
                .add(R.id.historyControllerFragment, historyVoid)
                .commit();
    }

    private void dismissVoidController() {
        FragmentManager fragmentManager = this.getFragmentManager();
        fragmentManager.beginTransaction()
                .remove(historyVoid)
                .commit();
        historyState = HistoryState.presentingHistory;
        workflow();

    }

    private void presentRefundController() {
        refundController = new HistoryRefundController();
        refundController.setListener(this);
        refundController.setTransaction(mCurrentTransaction);
        FragmentManager fragmentManager = this.getFragmentManager();
        fragmentManager.beginTransaction()
                .add(R.id.historyControllerFragment, refundController)
                .commit();
    }

    private void dismissRefundController() {
        FragmentManager fragmentManager = this.getFragmentManager();
        fragmentManager.beginTransaction()
                .remove(refundController)
                .commit();
        historyState = HistoryState.presentingHistory;
        workflow();
    }

    private void presentReconcileConfirm() {
        mReconcile = true;
        cancelTransactionFragment = new CancelTransactionFragment();
        cancelTransactionFragment.setMessageToShow(getActivity().getResources().getString(R.string.reconcile_confirm_label));
        cancelTransactionFragment.setmListener(this);
        FragmentManager fragmentManager = this.getFragmentManager();
        fragmentManager.beginTransaction()
                .add(R.id.historyControllerFragment, cancelTransactionFragment)
                .commit();
    }

    private void dimissRenconcileConfirm() {
        mReconcile = false;
        FragmentManager fragmentManager = this.getFragmentManager();
        fragmentManager.beginTransaction()
                .remove(cancelTransactionFragment)
                .commit();
        historyState = HistoryState.presentingHistory;
        workflow();
    }

    private void presentWaitReconcile() {
        mReconcile = false;
        waitTransactionFragment = new WaitForPT();
        FragmentManager fragmentManager = this.getFragmentManager();
        fragmentManager.beginTransaction()
                .remove(cancelTransactionFragment)
                .add(R.id.historyControllerFragment, waitTransactionFragment)
                .commit();
        mPaymentTerminal = PaymentTerminal.getInstance();
        if (mCommerceListener == null) {
            mCommerceListener = new CommerceListener() {
                @Override
                public CommerceResponse handleEvent(CommerceEvent commerceEvent) {
                    switch (commerceEvent.getType()) {
                        case CommerceEvent.STATUS_SUCCESS:
                        case CommerceEvent.STATUS_ERROR:
                            break;
                        case ReconciliationEvent.TYPE:
                            final ReconciliationEvent reconciliationEvent = (ReconciliationEvent) commerceEvent;
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    historyState = HistoryState.reconcileProcess;
                                    workflow();
                                    Toast soon = null;
                                    if (reconciliationEvent.getStatus() == 0) {
                                        soon = Toast.makeText(getActivity().getApplicationContext(), "Reconciled", Toast.LENGTH_LONG);
                                    } else {
                                        soon = Toast.makeText(getActivity().getApplicationContext(), "Reconcile failed", Toast.LENGTH_LONG);
                                    }
                                    soon.show();
                                    refresh();
                                }
                            });
                            MposLogger.getInstance().debug(TAG, " reconciliation event");
                            break;
                    }
                    return null;
                }
            };
            mPaymentTerminal.addGeneralListenr(mCommerceListener);
        }
        mPaymentTerminal.getReportManager();
    }

    private void dismissWaitReconcile() {
        FragmentManager fragmentManager = this.getFragmentManager();
        fragmentManager.beginTransaction()
                .remove(waitTransactionFragment)
                .commit();
        historyState = HistoryState.presentingHistory;
        workflow();

    }

    public void setListener(HistoryController.OnFragmentInteractionListener listener) {
        mListener = listener;
    }

    //Listener methods for HistoryDetail fragment
    @Override
    public void onClickSaveNote(String note) {
        mCurrentTransaction.setNote(note);

        historyState = HistoryState.backFromNote;
        workflow();

    }

    @Override
    public void onClickBackButton() {
        historyState = HistoryState.backFromNote;
        workflow();

    }

    @Override
    public void onPresentNote() {
        historyState = HistoryState.addingNote;
        workflow();
    }

    @Override
    public void onPrintTransaction() {
        confirm();
        PrinterUtility printerUtility = PrinterUtility.getInstance();
        printerUtility.printTransaction(mCurrentTransaction, getActivity());
    }

    @Override
    public void onSendReceipt() {
        historyState = HistoryState.presentingReceiptOptions;
        workflow();
    }

    @Override
    public void onVoidSelected() {
        historyState = HistoryState.voidConfirmation;
        mListener.hideDetailView("historyDetail");
        workflow();
    }

    @Override
    public void onRefundSelected() {
        historyState = HistoryState.refundSelected;
        mListener.hideDetailView("historyDetail");
        workflow();
    }

    @Override
    public void onSelectEmail() {
        historyState = HistoryState.emailSelected;
        workflow();

    }

    @Override
    public void onSelectSMS() {
        historyState = HistoryState.smsSelected;
        workflow();

    }

    @Override
    public void onBack() {
        historyState = HistoryState.backFromReceipt;
        workflow();

    }

    //Listener methods for HistorySendReceipt
    @Override
    public void onReceiptBackButton() {
        historyState = HistoryState.backFromSMSEmail;
        workflow();
    }

    @Override
    public void onSendSMS(String address) {
        //TODO implement code to send sms
        MposLogger.getInstance().debug(TAG, " " + address);
        confirm();
        historyState = HistoryState.backFromSMSEmail;
        workflow();


    }

    @Override
    public void onSendEmail(String address) {
        //TODO implement code to send email
        MposLogger.getInstance().debug(TAG, " " + address);
        confirm();
        historyState = HistoryState.backFromSMSEmail;
        workflow();
    }

    private void confirm() {
        Toast soon = Toast.makeText(getActivity().getApplicationContext(), "Sent", Toast.LENGTH_LONG);
        soon.show();
        refresh();

    }

    //Listener methods for CancelTransactionFragment (Use for void transaction confirmation)
    @Override
    public void abortCancelFragment() {
        if (!mReconcile) {
            historyState = HistoryState.voidCanceled;
        } else {
            historyState = reconcileCancel;
        }
        workflow();
    }

    @Override
    public void proceedCancelFragment() {
        if (!mReconcile) {
            historyState = HistoryState.voidSelected;
        } else {
            historyState = HistoryState.reconcileSelected;
        }
        workflow();
    }

    @Override
    public void onVoidSuccess(Payment payment) {
        mCurrentTransaction.setStatus(TransactionStatus.voided);
        historyState = HistoryState.voidSucess;
        workflow();
    }

    @Override
    public void onVoidBack() {
        historyState = HistoryState.voidBack;
        workflow();

    }

    @Override
    public void onVoidFail() {
        historyState = HistoryState.voidFail;
        workflow();

    }

    @Override
    public void onCancelRefund() {
        historyState = HistoryState.refundCancel;
        workflow();
    }

    @Override
    public void onRefundIssued() {
        mCurrentTransaction.setStatus(TransactionStatus.refunded);
        historyState = HistoryState.refundProcess;
        workflow();

    }

    private void reconcileBatch() {
        historyState = HistoryState.reconcileConfirmation;
        workflow();
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void presentDetailView(MTTransaction t, HistoryDetail h);

        void hideDetailView(String label);
    }

    private class HistoryArrayAdapter<T> extends ArrayAdapter<MTTransaction> {
        private final Context context;
        private final MTTransaction[] currentTransactions;
        private String previousDate;

        HistoryArrayAdapter(Context context, MTTransaction[] transactions) {
            super(context, -1, transactions);
            this.context = context;
            this.currentTransactions = transactions;
            this.previousDate = null;
        }

        @NonNull
        @Override
        public View getView(final int position, View convertView, @NonNull ViewGroup parent) {

            LayoutInflater inflater = getActivity().getLayoutInflater();
            TableRow row = (TableRow) inflater.inflate(R.layout.transaction_history_cell, parent, false);
            final MTTransaction transaction = currentTransactions[position];
            row.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            if (transaction != null) {
                TextView dateTextView = (TextView) row.findViewById(R.id.textViewTHDate);
                String dateToPresent = "     ";
                if (previousDate != null && position != 0) {
                    if (!previousDate.equals(transaction.getDate().substring(0, 5))) {
                        previousDate = transaction.getDate().substring(0, 5);
                        dateToPresent = previousDate;
                    }
                } else {
                    if (transaction.getDate() != null) {
                        previousDate = transaction.getDate().substring(0, 5);
                        dateToPresent = previousDate;
                    }
                }
                dateTextView.setText(dateToPresent);

                LinearLayout historyDetail = (LinearLayout) row.findViewById(R.id.linearHistoryDetail);
                historyDetail.setBackgroundColor(Color.TRANSPARENT);
                ImageView iconNote = (ImageView) row.findViewById(R.id.imageViewIconNote);

                if (transaction.getNote() != null && transaction.getNote().length() > 0) {
                    iconNote.setVisibility(View.VISIBLE);
                }

                TextView description = (TextView) row.findViewById(R.id.textViewTransactionID);

                String descriptionText = "";
                if (transaction.transactionPayments() != null) {
                    List<Payment> payments = transaction.transactionPayments().getPayments();
                    if (payments != null) {
                        if (transaction.getStatus() == null) {
                            transaction.setStatus(TransactionStatus.completed);
                        }
                        if (transaction.getStatus() == TransactionStatus.refund || transaction.getStatus() == TransactionStatus.refunded) {
                            descriptionText = transaction.getInvoiceId() + " (" + getActivity().getResources().getString(R.string.refund_title_textView) + ")";
                        } else {
                            if (transaction.getStatus() == TransactionStatus.voided) {
                                descriptionText = transaction.getInvoiceId() + " (" + getActivity().getResources().getString(R.string.void_label) + ")";
                            } else {
                                if (transaction.getStatus() == TransactionStatus.partialRefund || transaction.getStatus() == TransactionStatus.partiallyRefunded) {
                                    descriptionText = transaction.getInvoiceId() + " (" + getActivity().getResources().getString(R.string.partial_refund_label) + ")";
                                } else {
                                    descriptionText = transaction.getInvoiceId();
                                }
                            }
                        }
                    }

                    description.setText(descriptionText);

                    TextView txDate = (TextView) row.findViewById(R.id.textViewDetailDate);
                    txDate.setText(transaction.getDate().substring(12, transaction.getDate().length()));

                    TextView paymentType = (TextView) row.findViewById(R.id.textViewPaymentType);

                    String type = "";

                    //int payBy = transaction.getPaymentStatus();
                    Payment txPayment = payments != null ? payments.get(0) : null;
                    if (payments != null && transaction.transactionPayments().getPayments().size() == 1) {
                        if (txPayment.getPaymentType() == Payment.PaymentType.CASH) {
                            type = "Cash";
                        } else {
                            if (txPayment.getPaymentType() == Payment.PaymentType.DEBIT) {
                                type = "Debit Card";
                            } else {
                                type = "Credit Card";
                            }
                        }

                    } else {
                        type = "Split";
                    }
                    paymentType.setText(type);
                    TextView amount = (TextView) row.findViewById(R.id.textViewDetailTotal);

//            String totalAmount = localCurrencySymbol + " " + String.format("%.2f", transaction.getTransactionTotal());
                    LocalizeCurrencyFormatter formatter = LocalizeCurrencyFormatter.getInstance();
                    amount.setText(formatter.getCurrencyFormat().format(transaction.getTransactionTotal()));
                    if (payments != null) {
                        if (transaction.getStatus() == TransactionStatus.refunded) {
                            amount.setTextColor(getActivity().getResources().getColor(R.color.warm_grey));
                            paymentType.setTextColor(getActivity().getResources().getColor(R.color.warm_grey));
                            description.setTextColor(getActivity().getResources().getColor(R.color.warm_grey));
                        }
                    }
                }


                final boolean[] isSelected = {false};
                row.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!isSelected[0]) {
                            if (previousSelected != null) {
                                previousSelected.setBackgroundColor(Color.TRANSPARENT);
                            }
                            previousSelected = v;
                            isSelected[0] = true;
                            v.setBackground(getActivity().getResources().getDrawable(R.drawable.linear_layout_round_corners));
                            HistoryDetail historyDetail = new HistoryDetail();
                            historyDetail.setListener(mController);
                            mCurrentTransaction = transaction;
                            historyDetail.setTransaction(transaction);
                            mListener.presentDetailView(transaction, historyDetail);
                        } else {
                            isSelected[0] = false;
                            v.setBackgroundColor(Color.TRANSPARENT);
                            mListener.hideDetailView("historyDetail");
                            //historyDetailLayout.setVisibility(View.INVISIBLE);
                        }
                    }
                });
/*                        LinearLayout historyDetail = (LinearLayout) v.findViewById(R.id.linearHistoryDetail);
                        if (previousSelected != null) {
                            previousSelected.setBackgroundColor(Color.TRANSPARENT);
                            mListener.hideDetailView(fragmentToReturn);
                        }
                        MTTransaction transactionToShow = transaction;
                        //Payments payments = transactionToShow.getPayment();
                        if (previousSelected != historyDetail ) {
                            historyDetail.setBackground(getActivity().getResources().getDrawable(R.drawable.linear_layout_round_corners));

                            previousSelected = historyDetail;
                            isVisible = true;

                            HistoryDetailCash historyDetailCash = new HistoryDetailCash();
                            currentTransaction = transactionToShow;
                            historyDetailCash.setTransaction(transaction);
                            historyDetailCash.setListener((HistoryDetailCash.OnFragmentInteractionListener) historyDetailCashListener);
                            mListener.presentDetailView(transaction, historyDetailCash);

                            if (!transaction.getDate().equals(fragmentToReturn) || previousSelected == null) {
                                fragmentToReturn = transaction.getDate();
                                //replaceFragment(R.id.mainHistoryDetailFrameLayout, historyDetailCash, fragmentToReturn);
                                previousSelected = historyDetail;

                            }
                        } else {
                            if (isVisible) {
                                detailFrameLayout.setVisibility(View.INVISIBLE);
                                mListener.hideDetailView(fragmentToReturn);
                                isVisible = false;
                                previousSelected = null;
                            } else {
                                detailFrameLayout.setVisibility(View.VISIBLE);
                                mListener.presentDetailView(transaction, historyDetailCash);
                                isVisible = true;
                            }
                        }

                    }
                });
                if (position == 0 && onStart) {
                    detailFrameLayout.setVisibility(View.VISIBLE);
                    HistoryDetailCash historyDetailCash = new HistoryDetailCash();
                    VLog.debug("TH: ", "showing transaction");
                    historyDetailCash.setTransaction(transaction);
                    historyDetailCash.setListener((HistoryDetailCash.OnFragmentInteractionListener) historyDetailCashListener);
                    mListener.presentDetailView(transaction, historyDetailCash);
                    historyDetail.setBackground(getActivity().getResources().getDrawable(R.drawable.linear_layout_round_corners));

                    previousSelected = historyDetail;
                    isVisible = true;
                    onStart = false;
                }
*/
            } else {
                row.setVisibility(View.INVISIBLE);
            }
            return row;
        }

    }

    static class ViewHolderItem {
        TextView dateTextView;
        LinearLayout historyDetail;
        ImageView iconNote;
        TextView description;
        TextView txDate;
        TextView paymentType;
        TextView amount;

    }

    private class SearchSelectedListener implements AdapterView.OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
/*            Toast.makeText(parent.getContext(),
                    "OnItemSelectedListener : " + parent.getItemAtPosition(pos).toString(),
                    Toast.LENGTH_SHORT).show();*/
            if (parent.getChildAt(0) != null) {
                TextView textView = (TextView) parent.getChildAt(0);
                textView.setTextAppearance(getActivity().getApplicationContext(), R.style.fontForTextViewTransactionHistoryDetailTotal);
                textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

            }
            detailFrameLayout.setVisibility(View.INVISIBLE);
//            removeFragment();
            previousSelected = null;

            int index;
            int indexToFilter = 0;
            List<MTTransaction> filterTransactions = new ArrayList<>();
            if (transactions != null) {
                for (index = 0; index < transactions.size(); index++) {
                    MTTransaction transaction = transactions.get(index);
                    boolean addit = false;
                    for (Payment payment : transaction.transactionPayments().getPayments()) {
                        if (includeTransaction(transaction, pos, payment) && !addit) {
                            filterTransactions.add(transaction);
                            addit = true;
                        }
                    }
                }
                if (filterTransactions.size() >= 0) {
                    MTTransaction[] filteredTransaction = new MTTransaction[filterTransactions.size()];
                    index = 0;
                    for (MTTransaction transaction : filterTransactions) {
                        filteredTransaction[index++] = transaction;
                    }
                    HistoryArrayAdapter<MTTransaction> arrayAdapter = new HistoryArrayAdapter<MTTransaction>(getActivity().getBaseContext(), filteredTransaction);
                    transactionListView.setAdapter(arrayAdapter);
                    arrayAdapter.notifyDataSetChanged();
                }

 /*               MTTransaction[] filteredTransaction = new MTTransaction[indexToFilter];
                indexToFilter = 0;
                for (index = 0; index < transactions.size(); index++) {
                    MTTransaction transaction = transactions.get(index);
                    if (includeTransaction(transaction, pos)) {
                        filteredTransaction[indexToFilter++] = transaction;
                    }
                    HistoryArrayAdapter<MTTransaction> arrayAdapter = new HistoryArrayAdapter<MTTransaction>(getActivity().getBaseContext(), filteredTransaction);
                    transactionListView.setAdapter(arrayAdapter);
                    arrayAdapter.notifyDataSetChanged();
                }
*/
            }

        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
        }

        private boolean includeTransaction(MTTransaction transaction, int test, Payment payments) {
            boolean result = false;
            if (payments == null) {
                result = false;
            } else {
                switch (test) {
                    case 0:
                        result = true;
                        break;
                    case 1:
                        if (transaction.getStatus() == TransactionStatus.completed) {
                            result = true;
                        }
                        break;
                    case 2:
                        if (payments.getPaymentType() == Payment.PaymentType.CASH) {
                            result = true;
                        }
                        break;
                    case 3:
                        if (payments.getPaymentType() == Payment.PaymentType.CREDIT) {
                            result = true;
                        }
                        break;
                    case 4:
                        if (payments.getPaymentType() == Payment.PaymentType.DEBIT) {
                            result = true;
                        }
                        break;
                    case 5:
                        if (transaction.getStatus() == TransactionStatus.voided) {
                            result = true;
                        }
                        break;
                    case 6:
                        if (transaction.getStatus() == TransactionStatus.refunded) {
                            result = true;
                        }
                        break;
                    case 7:
                        if (transaction.getStatus() == TransactionStatus.partialRefund || transaction.getStatus() == TransactionStatus.partiallyRefunded) {
                            result = true;
                        }
                        break;
                    case 8:
                        if (transaction.getNote() != null && transaction.getNote().length() > 0) {
                            result = true;
                        }
                        break;
                    default:
                        break;
                }
            }

            return result;
        }

    }

}

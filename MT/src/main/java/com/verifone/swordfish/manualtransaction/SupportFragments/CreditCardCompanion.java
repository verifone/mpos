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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.print.PrintJob;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.print.PrintHelper;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.verifone.commerce.entities.Payment;
import com.verifone.commerce.entities.Receipt;
import com.verifone.swordfish.manualtransaction.MTDataModel.MTTransaction;
import com.verifone.swordfish.manualtransaction.R;
import com.verifone.swordfish.manualtransaction.System.PaymentTerminal;
import com.verifone.swordfish.manualtransaction.Tools.MposLogger;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.List;

import static com.verifone.commerce.entities.Receipt.DELIVERY_METHOD_EMAIL;
import static com.verifone.commerce.entities.Receipt.DELIVERY_METHOD_PRINT;
import static com.verifone.commerce.entities.Receipt.DELIVERY_METHOD_SMS;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link CreditCardCompanion.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link CreditCardCompanion#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CreditCardCompanion extends Fragment implements ButtonsFragment.OnFragmentInteractionListener {
    private static final String TAG = CreditCardCompanion.class.getSimpleName();
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    private LinearLayout linearError;
    private LinearLayout linearCompleted;
    private LinearLayout swipeLinear;
    private LinearLayout linearReadyForSignature;
    private LinearLayout linearPrintReceipt;
    private TextView errorMessage;
    private TextView subErrorMessage;
    private ProgressBar progressCircle;
    private LinearLayout linearProgress;
    private ImageButton printReceipt;
    private ImageButton noReceipt;
    private TextView receiptOption;
    private WebView mPrintWebView;
    //    private PaymentInfo paymentInformation;
    private PrintHelper photoPrinter;
    private PaymentTerminal.PaymentTerminalPaymentEvents paymentTerminalEvents;
    private PaymentTerminal paymentTerminal;
    private Context myContext;
    private String splitAmount;
    boolean processing;
    boolean transactionStart = false;
    boolean isContextSet = false;
    boolean isSplit = false;
    boolean isPrinting = false;
    private PrintJob mPrintJob;
    private CountDownTimer mCountDownTimer = null;
    //TransactionCardPayment.OnFragmentInteractionListener mpListener;
    private BigDecimal transactionTotal;
    private MTTransaction currentTransaction;
    private ButtonsFragment buttonsFragment;
    private BigDecimal mTip;


    public CreditCardCompanion() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment CreditCardCompanion.
     */
    // TODO: Rename and change types and number of parameters
    public static CreditCardCompanion newInstance(String param1, String param2) {
        CreditCardCompanion fragment = new CreditCardCompanion();
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
        View myView = inflater.inflate(R.layout.fragment_credit_card_companion, container, false);
        linearCompleted = (LinearLayout) myView.findViewById(R.id.linearCompleted);
        noReceipt = (ImageButton) myView.findViewById(R.id.imageButtonNoReceipt);
        printReceipt = (ImageButton) myView.findViewById(R.id.imageButtonPrint);
//        linearPrintReceipt = (LinearLayout) myView.findViewById(R.id.linearPrintReceipt);
//        mPrintWebView = (WebView) myView.findViewById(R.id.printWebView) ;
        noReceipt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.settle();
            }
        });
        final Context context = this.myContext;
        printReceipt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<Payment> payments = currentTransaction.transactionPayments().getPayments();
                Payment payment = payments.get(0);
                if (!payment.getPaymentType().equals(Payment.PaymentType.CASH)) {
                    Receipt receipt = payment.getReceipt();
                    if (receipt != null) {
                        printReceipt.setEnabled(false);
                        noReceipt.setEnabled(false);
                        subErrorMessage.setText("Printing .....");
                        MposLogger.getInstance().debug(TAG, "html to print: " + receipt.getAsHtml());
                        mPrintJob = receipt.print(context);
                        checkPrintJob();
                    } else {
                        //mpListener.printReceiptRequested();

                    }
                }

//                mpListener.printReceiptRequested();
//                mpListener.transactionApprove();
            }
        });
        printReceipt.setEnabled(false);
        printReceipt.setVisibility(View.INVISIBLE);
        noReceipt.setEnabled(false);
        noReceipt.setVisibility(View.INVISIBLE);
        linearError = (LinearLayout) myView.findViewById(R.id.linearError);
        linearReadyForSignature = (LinearLayout) myView.findViewById(R.id.linearReadyForSignature);
        Button errorButton = (Button) linearError.findViewById(R.id.errorButton);
        errorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.cancelTransaction();
            }
        });
        swipeLinear = (LinearLayout) myView.findViewById(R.id.swipeLinear);
        errorMessage = (TextView) myView.findViewById(R.id.tvErrorReading);
        subErrorMessage = (TextView) myView.findViewById(R.id.tvSwipeAgain);
        progressCircle = (ProgressBar) myView.findViewById(R.id.progressCircle);
        linearProgress = (LinearLayout) myView.findViewById(R.id.linearProgress);
        receiptOption = (TextView) myView.findViewById(R.id.receiptOption);
        isContextSet = false;
        transactionStart = false;
        isPrinting = false;
        myContext = getContext();
        FragmentManager manager = getActivity().getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        buttonsFragment = new ButtonsFragment();
        buttonsFragment.setListener(this);
        transaction.replace(R.id.creditButtonsFrame, buttonsFragment);
        transaction.commit();

        return myView;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (myContext != null) {
            if (!isPrinting) {
                isContextSet = true;
                transactionStart = true;
                processPayment();
                //set payment terminal listener
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        paymentTerminal = null;
        paymentTerminalEvents = null;
    }

    public void setListener(CreditCardCompanion.OnFragmentInteractionListener listener) {
        mListener = listener;
    }

    public void setTransactionTotal(BigDecimal total) {
        transactionTotal = total;
    }

    public void setTransaction(MTTransaction transaction) {
        currentTransaction = transaction;
    }

    public void setSplit(boolean split) {
        isSplit = split;
    }

    public void setTip(BigDecimal tip) {
        mTip = tip;
    }

    public void startProcessPayment() {
        //mListener.configureButtonsForProcess();
        showCircular();

        //paymentTerminal.createBasket();
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(final Void... params) {
                // Do your loading here. Don't touch any views from here, and then return null
                try {
                    startCreditTransaction();
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                }
                ;
                return null;
            }
        }.execute();

    }

    private void processPayment() {
        paymentTerminalEvents = new PaymentTerminal.PaymentTerminalPaymentEvents() {

            @Override
            public void paymentStarted() {

            }

            @Override
            public void onSuccess(Payment tempInfo, final int method, final String customer) {
                final Payment paymentInfo = tempInfo;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Your code to run in GUI thread here
                        //Payments payment = new Payments();
                        //payment.setType(com.verifone.swordfish.manualtransaction.enums.PaymentType.creditCard);
                        //payment.setStatus(PaymentStatus.completed);
                        //payment.setCardPayment(paymentInfo);
                        //currentTransaction.setStatus(TransactionStatus.completed);
                        //currentTransaction.setType(TransactionType.credit);
                        if (paymentInfo != null) {
 /*                           if (paymentInfo.getCardInformation() != null) {
                                if (paymentInfo.getCardInformation().getCardHolderName().equals("VISA")) {
                                    payment.setCardIssuer("Visa");
                                } else {
                                    payment.setCardIssuer(paymentInfo.getCardInformation().getCardHolderName());
                                }
                            }
*//*
                            if (!isSplit) {
                                payment.setAmount(new BigDecimal(currentTransaction.getAmount()));
                            } else {
                                payment.setAmount(new BigDecimal(splitAmount));
                            }
*/
                            currentTransaction.addPayment(paymentInfo);
                            if (paymentInfo.getCardInformation() != null) {
                                //payment.setCredtCard4Digits(paymentInfo.getCardInformation().getCardPan());
                                mListener.setCCDigits(paymentInfo.getCardInformation().getCardPan());
                            }
                            if (paymentInfo.getCardInformation() != null) {
                                //payment.setCardHolder(paymentInfo.getCardInformation().getCardHolderName());
                                mListener.setCCOwner(paymentInfo.getCardInformation().getCardHolderName());
                            }
                            if (paymentInfo.getCardInformation().getCardExpiry() != null) {
                                //payment.setMonthYear(paymentInfo.getCardInformation().getCardExpiry() );
                                mListener.setCCExpDate(paymentInfo.getCardInformation().getCardExpiry());
                            }
                            if (paymentInfo.getAuthCode() != null) {
                                //payment.setAuthCode(paymentInfo.getAuthCode());
                                mListener.setCCAuthCode(paymentInfo.getAuthCode());
                            }

                            if (paymentInfo.getReceipt() != null) {
                                //payment.setReceipt(paymentInfo.getReceipt());
                            }
                            if (paymentInfo.getAppSpecificData() != null && paymentInfo.getAppSpecificData().contains("CTROUTD")) {
                                String appData = paymentInfo.getAppSpecificData();
                                JSONObject obj = null;
                                try {
                                    obj = new JSONObject(appData);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    obj = null;
                                }

                            }
                        }

                        onApproval();
                        //TODO delivery method
                        switch (method) {
                            case DELIVERY_METHOD_EMAIL:
                                emailReceipt(paymentInfo.getReceipt(), customer);
                                break;
                            case DELIVERY_METHOD_SMS:
                                smsReceipt(paymentInfo.getReceipt(), customer);
                                break;
                            case DELIVERY_METHOD_PRINT:
                                printReceipt(paymentInfo.getReceipt());
                                break;
                            default: {
                                mListener.settle();
                            }
                            break;
                        }

                    }
                });

            }

            @Override
            public void onVoiceAuthorizationRequest(final String message) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog.Builder builder = new AlertDialog.Builder(myContext);
                        builder.setTitle(message);

                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(myContext);
                                builder.setTitle("Enter authorization code: ");

                                final EditText input = new EditText(myContext);
                                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL);
                                builder.setView(input);
                                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        paymentTerminal.requestVoiceAuthorization(input.getText().toString(), Float.toString(transactionTotal.floatValue()));
                                    }
                                });
                                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        processResult(9);
                                        dialog.cancel();
                                    }
                                });

                                builder.show();
                                paymentTerminal.requestVoiceAuthorization(input.getText().toString(), Float.toString(transactionTotal.floatValue()));
                            }
                        });
                        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                processResult(9);
                                dialog.cancel();
                            }
                        });

                        builder.show();
                    }
                });
            }

            @Override
            public void onSignatureRequest() {

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Your code to run in GUI thread here
                        showWaitSignature();
                        //processResult(2);
                    }
                });
            }

            @Override
            public void onReceiptRequest() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Your code to run in GUI thread here
                        showTransactionEnds();
                    }
                });
            }

            @Override
            public void onSuccessEmailRequest(String email) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Your code to run in GUI thread here
                        printReceipt.setEnabled(true);
                        printReceipt.setVisibility(View.VISIBLE);
                        receiptOption.setText(" ");
                    }
                });

            }

            @Override
            public void onNoReceipt() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Your code to run in GUI thread here
                        noReceipt.setEnabled(true);
                        noReceipt.setVisibility(View.VISIBLE);
                        receiptOption.setText(" ");
                    }
                });

            }


            @Override
            public void onDeclined() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Your code to run in GUI thread here
                        processResult(9);
                        //processResult(2);
                    }
                });
                //paymentTerminal.stopSession();

            }

            @Override
            public void onCancel() {

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Your code to run in GUI thread here
                        processResult(0);
                        //processResult(2);
                    }
                });
                //paymentTerminal.stopSession();
            }

            @Override
            public void onFailure() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Your code to run in GUI thread here
                        processResult(9);
                        //processResult(2);
                    }
                });
                //paymentTerminal.stopSession();
            }

            @Override
            public void onTimeOut() {
                new AsyncTask<Void, Void, Void>() {

                    @Override
                    protected Void doInBackground(final Void... params) {
                        // Do your loading here. Don't touch any views from here, and then return null
                        try {
                            //paymentTerminal.stopSession();
                        } catch (Exception e) {
                            Thread.currentThread().interrupt();
                        }
                        ;
                        return null;
                    }
                }.execute();
                processResult(8);
            }
        };
    }

    private void startCreditTransaction() {
        String amountToCharge;
        if (!isSplit) {
            amountToCharge = String.format("%.2f", transactionTotal.floatValue());
        } else {
            amountToCharge = splitAmount;
        }
        if (paymentTerminal == null) {
            paymentTerminal = PaymentTerminal.getInstance();
            processPayment();
            paymentTerminal.setPaymentListener(paymentTerminalEvents);

        }
        if (mTip == BigDecimal.ZERO) {
            paymentTerminal.startTransaction(amountToCharge);
        } else {
            paymentTerminal.preAuthTransaction(amountToCharge);
        }

    }

    private void showCircular() {
        swipeLinear.setVisibility(View.INVISIBLE);
        linearProgress.setVisibility(View.VISIBLE);
        progressCircle.setVisibility(View.VISIBLE);

    }


    private void processResult(int result) {

        MposLogger.getInstance().debug("CreditCardCompanion", "processResult: " + Integer.toString(result));
        if (swipeLinear != null) {
            swipeLinear.setVisibility(View.INVISIBLE);
        }
        switch (result) {
            case 2:
                mListener.settle();
                //TODO print receipt
//                mpListener.printReceiptRequested();
                if (linearCompleted != null) {
                    linearCompleted.setVisibility(View.VISIBLE);
                }
                if (progressCircle != null) {
                    linearProgress.setVisibility(View.INVISIBLE);
                    progressCircle.setVisibility(View.INVISIBLE);
                }
                break;
            case 9:
                //mpListener.transactionDenied();
                mListener.configureButtonsForDeclined();
                errorMessage.setText(R.string.card_declined);
                subErrorMessage.setText(R.string.choose_another_card);
                linearError.setVisibility(View.VISIBLE);
                linearProgress.setVisibility(View.INVISIBLE);
                progressCircle.setVisibility(View.INVISIBLE);
                break;
            case 0:
                mListener.cancelTransaction();
                errorMessage.setText(R.string.transaction_cancelled);
//                subErrorMessage.setText(R.string.choose_another_card);
                linearError.setVisibility(View.VISIBLE);
                linearProgress.setVisibility(View.INVISIBLE);
                progressCircle.setVisibility(View.INVISIBLE);
                break;
            case 8:
                //mpListener.trasactionTimeout();
                linearProgress.setVisibility(View.INVISIBLE);
                progressCircle.setVisibility(View.INVISIBLE);
                swipeLinear.setVisibility(View.VISIBLE);
                break;
            case 1:
                mListener.settle();
                linearCompleted.setVisibility(View.VISIBLE);
                linearProgress.setVisibility(View.INVISIBLE);
                progressCircle.setVisibility(View.INVISIBLE);
                break;
            case 3:
                mListener.settle();
                //TODO email request
                //mpListener.emailRequested();
                linearCompleted.setVisibility(View.VISIBLE);
                linearProgress.setVisibility(View.INVISIBLE);
                progressCircle.setVisibility(View.INVISIBLE);
                break;
            case 4:
                mListener.settle();
                //TODO sms request
                //mpListener.smsRequested();
                linearCompleted.setVisibility(View.VISIBLE);
                linearProgress.setVisibility(View.INVISIBLE);
                progressCircle.setVisibility(View.INVISIBLE);
                break;
            default:
                //mpListener.trasactionTimeout();
                linearError.setVisibility(View.VISIBLE);
                linearProgress.setVisibility(View.INVISIBLE);
                progressCircle.setVisibility(View.INVISIBLE);
                break;


        }
    }


    private void printReceipt(Receipt receipt) {
        if (receipt != null) {
            printReceipt.setEnabled(false);
            noReceipt.setEnabled(true);
            printReceipt.setVisibility(View.INVISIBLE);
            noReceipt.setVisibility(View.INVISIBLE);
            subErrorMessage.setText("Printing .....");
            MposLogger.getInstance().debug(TAG, "html to print: " + receipt.getAsHtml());
            mPrintJob = receipt.print(myContext);
            checkPrintJob();
            //mpListener.printReceiptRequested();
            //mpListener.transactionApprove();
        } else {
            //TODO print request
            //mpListener.printReceiptRequested();

        }

    }

    private void emailReceipt(Receipt receipt, final String customer) {

        /*final CreditCardCompanion presentContext = this;
        final com.verifone.swordfish.manualtransaction.MTDataModel.TransactionHistory transactionHistory;
        transactionHistory = com.verifone.swordfish.manualtransaction.MTDataModel.TransactionHistory.getInstance();
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(final Void... params) {
                // Do your loading here. Don't touch any views from here, and then return null
                try {
                    transactionHistory.sendEmail(currentTransaction, customer, presentContext);
                } catch  (Exception e) {
                    Thread.currentThread().interrupt();
                };
                return null;
            }
        }.execute();

        mListener.settle();*/

    }

    private void smsReceipt(Receipt receipt, final String customer) {
        /*final CreditCardCompanion presentContext = this;
        final com.verifone.swordfish.manualtransaction.MTDataModel.TransactionHistory transactionHistory;
        transactionHistory = com.verifone.swordfish.manualtransaction.MTDataModel.TransactionHistory.getInstance();
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(final Void... params) {
                // Do your loading here. Don't touch any views from here, and then return null
                try {
                    transactionHistory.sendSMS(currentTransaction, customer, presentContext);
                } catch  (Exception e) {
                    Thread.currentThread().interrupt();
                };
                return null;
            }
        }.execute();

        mListener.settle();*/

    }

    private void onApproval() {
        MposLogger.getInstance().debug("CreditCardCompanion", " showTransactionEnds");
        linearReadyForSignature.setVisibility(View.INVISIBLE);
        linearCompleted.setVisibility(View.VISIBLE);
        progressCircle.setVisibility(View.INVISIBLE);
        linearProgress.setVisibility(View.INVISIBLE);
        //mpListener.hideButtons();
        noReceipt.setEnabled(true);
        noReceipt.setVisibility(View.VISIBLE);
        printReceipt.setEnabled(true);
        printReceipt.setVisibility(View.VISIBLE);
        receiptOption.setText(" ");

    }


    private void showTransactionEnds() {
        MposLogger.getInstance().debug("CreditCardCompanion", " showTransactionEnds");
        linearReadyForSignature.setVisibility(View.INVISIBLE);
        linearCompleted.setVisibility(View.VISIBLE);
        progressCircle.setVisibility(View.INVISIBLE);
        linearProgress.setVisibility(View.INVISIBLE);
        //mpListener.hideButtons();

    }

    private void showWaitSignature() {
        linearReadyForSignature.setVisibility(View.VISIBLE);
        linearCompleted.setVisibility(View.INVISIBLE);
        progressCircle.setVisibility(View.INVISIBLE);
        linearProgress.setVisibility(View.INVISIBLE);
        //mpListener.hideButtons();

    }

    private void checkPrintJob() {
        MposLogger.getInstance().debug(TAG, "check print job");
        if (mCountDownTimer == null) {
            mCountDownTimer = new CountDownTimer(180000, 5000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Your code to run in GUI thread here
                            if (mPrintJob.isCompleted()) {

                                MposLogger.getInstance().debug(TAG, "on tick");
                                mCountDownTimer.cancel();
                                mListener.settle();
                            }
                            //processResult(2);
                        }
                    });
                }

                @Override
                public void onFinish() {

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Your code to run in GUI thread here
                            MposLogger.getInstance().debug(TAG, " on finish");
                            mListener.settle();

                            //processResult(2);
                        }
                    });
                }
            };
            isPrinting = true;
            mCountDownTimer.start();
        }

    }

    @Override
    public void onButtonPress(int buttonID) {
        switch (buttonID) {
            case 0:
                mListener.cancelTransaction();
                break;
            case 1:
                mListener.companionBack();
                break;
            case 2:
                buttonsFragment.onConfigureButton(0, false, null);
                buttonsFragment.onConfigureButton(1, false, null);
                buttonsFragment.onConfigureButton(2, false, null);
                startProcessPayment();
                break;
            default:
                break;
        }
    }

    @Override
    public void readyToConfigureButtons() {
        buttonsFragment.onConfigureButton(0, true, getActivity().getString(R.string.buttonCancelTx));
        buttonsFragment.onConfigureButton(1, true, getActivity().getString(R.string.buttonBack));
        buttonsFragment.onConfigureButton(2, true, getActivity().getString(R.string.buttonCharge));

    }

/*
    @Override
    public void onSucess() {
        Toast soon =  Toast.makeText(getActivity().getApplicationContext(), "Sent", Toast.LENGTH_LONG);
        soon.show();

    }

    @Override
    public void onFailure() {
        Toast soon =  Toast.makeText(getActivity().getApplicationContext(), "Fail to send, try again later", Toast.LENGTH_LONG);
        soon.show();

    }
*/

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
        // TODO: Update argument type and name
        void settle();

        void cancelTransaction();

        void companionReady();

        void setCCDigits(String digits);

        void setCCExpDate(String expDate);

        void setCCOwner(String name);

        void setCCAuthCode(String code);

        void configureButtonsForProcess();

        void configureButtonsForDeclined();

        void companionBack();
    }
}

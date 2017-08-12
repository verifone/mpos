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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.verifone.commerce.entities.Payment;
import com.verifone.swordfish.manualtransaction.MTDataModel.MTPayments;
import com.verifone.swordfish.manualtransaction.MTDataModel.MTTransaction;
import com.verifone.swordfish.manualtransaction.R;
import com.verifone.swordfish.manualtransaction.System.PaymentTerminal;

import java.math.BigDecimal;
import java.util.Locale;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link WaitForCardProcess.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link WaitForCardProcess#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WaitForCardProcess extends Fragment {

    private static final String TAG = WaitForCardProcess.class.getSimpleName();
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;
    private MTTransaction currentTransaction;
    private MTPayments currentPayment;
    private PaymentTerminal paymentTerminal;
    private PaymentTerminal.PaymentTerminalPaymentEvents paymentTerminalEvents;
    private BigDecimal mTip;
    private Context myContext;
    private boolean isSplit;
    private boolean isRestaurant = false;
    private boolean firstStep = false;
    private boolean mIsManual = false;
    private BigDecimal splitAmount;
    private ProgressBar progressCircle;
    private LinearLayout linearProgress;

    public WaitForCardProcess() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment WaitForCardProcess.
     */
    public static WaitForCardProcess newInstance(String param1, String param2) {
        WaitForCardProcess fragment = new WaitForCardProcess();
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
        myContext = this.getContext();
        View view = inflater.inflate(R.layout.fragment_wait_for_card_process, container, false);
        progressCircle = (ProgressBar) view.findViewById(R.id.waitProgressCircle);
        linearProgress = (LinearLayout) view.findViewById(R.id.waitLlinearProgress);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (currentTransaction == null && mTip == null) {
            mListener.onWaitCardDeinied();
        } else {
            paymentTerminal = PaymentTerminal.getInstance();
            startPaymentRequest();
        }
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

    public void setTransaction(MTTransaction transaction) {
        currentTransaction = transaction;
    }

    public void setTip(BigDecimal tip) {
        mTip = tip;
    }

    public void setListener(WaitForCardProcess.OnFragmentInteractionListener listener) {
        mListener = listener;
    }

    public void setSplit(boolean split) {
        isSplit = split;
    }

    public void setRestaurant(boolean type) {
        isRestaurant = type;
    }

    public void setStep(boolean step) {
        firstStep = step;
    }

    public void splitAmount(BigDecimal amount) {
        splitAmount = amount;
    }

    public void manualPayment(boolean isManual) {
        mIsManual = isManual;
    }

    private void startPaymentRequest() {
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
                        mListener.onWaitSettle(method, paymentInfo);
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
                                input.setInputType(InputType.TYPE_CLASS_TEXT);
                                builder.setView(input);
                                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        paymentTerminal.requestVoiceAuthorization(input.getText().toString(), Float.toString(currentTransaction.getTransactionTotal().floatValue()));
                                    }
                                });
                                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.cancel();
                                    }
                                });

                                builder.show();
                                paymentTerminal.requestVoiceAuthorization(input.getText().toString(), Float.toString(currentTransaction.getTransactionTax().floatValue()));
                            }
                        });
                        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
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
                        //showWaitSignature();
                    }
                });
            }

            @Override
            public void onReceiptRequest() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Your code to run in GUI thread here
                        //printReceipt();
                    }
                });
            }

            @Override
            public void onSuccessEmailRequest(String email) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Your code to run in GUI thread here
                        //printReceipt();
                    }
                });
            }

            @Override
            public void onNoReceipt() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Your code to run in GUI thread here
                        //noReceipt();
                    }
                });
            }


            @Override
            public void onDeclined() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Your code to run in GUI thread here
                        mListener.onWaitCardDeinied();
                    }
                });

            }

            @Override
            public void onCancel() {

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Your code to run in GUI thread here
                        mListener.onWaitCancel();
                    }
                });
            }

            @Override
            public void onFailure() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Your code to run in GUI thread here
                        mListener.onWaitCancel();
                    }
                });
            }

            @Override
            public void onTimeOut() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Your code to run in GUI thread here
                        mListener.onWaitCancel();

                    }
                });
            }
        };
        String amountToCharge;
        if (!isSplit) {
            amountToCharge = String.format(Locale.getDefault(), "%.2f", currentTransaction.getTransactionTotal().floatValue());
        } else {
            amountToCharge = String.format(Locale.getDefault(), "%.2f", splitAmount.floatValue());
        }
        if (paymentTerminal == null) {
            paymentTerminal = PaymentTerminal.getInstance();
        }
        paymentTerminal.setPaymentListener(paymentTerminalEvents);
        if (!isRestaurant && !mIsManual) {
            paymentTerminal.startTransaction(amountToCharge);
        } else {
            if (mIsManual) {
                paymentTerminal.startManualTransaction(amountToCharge);
            } else {
                if (firstStep) {
                    paymentTerminal.preAuthTransaction(amountToCharge);
                } else {
                    paymentTerminal.finalAuth(amountToCharge, mTip);
                }
            }
        }


    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnFragmentInteractionListener {

        void onWaitCardDeinied();

        void onWaitCancel();

        void onWaitSettle(int receipt, Payment payment);
    }
}

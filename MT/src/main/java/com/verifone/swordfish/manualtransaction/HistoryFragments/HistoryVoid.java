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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.verifone.commerce.entities.Payment;
import com.verifone.swordfish.manualtransaction.MTDataModel.MTTransaction;
import com.verifone.swordfish.manualtransaction.R;
import com.verifone.swordfish.manualtransaction.System.PaymentTerminal;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link HistoryVoid.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link HistoryVoid#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HistoryVoid extends Fragment implements VoidCompanion.OnFragmentInteractionListener {

    private static final String TAG = HistoryVoid.class.getSimpleName();
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;
    private VoidList voidList;
    private VoidCompanion voidCompanion;
    private MTTransaction mCurrentTransaction;
    private Payment mPayment;
    PaymentTerminal.PaymentTerminalPaymentEvents paymentEvents;

    public HistoryVoid() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HistoryVoid.
     */
    public static HistoryVoid newInstance(String param1, String param2) {
        HistoryVoid fragment = new HistoryVoid();
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
        return inflater.inflate(R.layout.fragment_history_void, container, false);
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onResume() {
        super.onResume();
        FragmentManager fragmentManager = this.getFragmentManager();
        voidList = new VoidList();
        voidList.setTransaction(mCurrentTransaction);
        voidCompanion = new VoidCompanion();
        voidCompanion.setListener(this);
        fragmentManager.beginTransaction()
                .add(R.id.historyVoidList, voidList)
                .add(R.id.voidCompanion, voidCompanion)
                .commit();
        setPaymentListener();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private void setPaymentListener() {
        paymentEvents = new PaymentTerminal.PaymentTerminalPaymentEvents() {
            @Override
            public void paymentStarted() {

            }

            @Override
            public void onSuccess(Payment paymentInfo, int method, String customer) {
                mCurrentTransaction.setStatus(TransactionStatus.voided);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        voidList.setVoidedHeader();
                        voidCompanion.presentFinal();
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
                mListener.onVoidFail();
            }

            @Override
            public void onCancel() {
                mListener.onVoidBack();
            }

            @Override
            public void onFailure() {
                mListener.onVoidFail();
            }

            @Override
            public void onTimeOut() {
                mListener.onVoidBack();
            }
        };
        PaymentTerminal.getInstance().setPaymentListener(paymentEvents);
    }

    public void setListener(HistoryVoid.OnFragmentInteractionListener listener) {
        mListener = listener;
    }

    public void setTransaction(MTTransaction transaction) {
        mCurrentTransaction = transaction;
    }

    public void setPayment(Payment payment) {
        mPayment = payment;
    }

    @Override
    public void onBack() {
        mListener.onVoidBack();
    }

    @Override
    public void processVoid() {
        PaymentTerminal.getInstance().voidTransaction(mPayment);
    }

    @Override
    public void VoidSucess() {
        mListener.onVoidSuccess(mPayment);
    }


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnFragmentInteractionListener {
        void onVoidSuccess(Payment payment);

        void onVoidBack();

        void onVoidFail();
    }
}

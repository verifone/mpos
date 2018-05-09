package com.verifone.swordfish.manualtransaction.gui;

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.verifone.commerce.entities.Payment;
import com.verifone.commerce.entities.Receipt;
import com.verifone.peripherals.IDirectPrintListener;
import com.verifone.swordfish.manualtransaction.R;
import com.verifone.swordfish.manualtransaction.tools.PrinterUtility;

/**
 * Copyright (C) 2016,2017,2018 Verifone, Inc.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * VERIFONE, INC. BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * <p>
 * Except as contained in this notice, the name of Verifone, Inc. shall not be
 * used in advertising or otherwise to promote the sale, use or other dealings
 * in this Software without prior written authorization from Verifone, Inc.
 * <p>
 * <p>
 * Created by abey on 1/5/2018.
 */

public class FinishFragment extends Fragment implements View.OnClickListener {

    private Button mPrintBtn;
    private Button mFinishBtn;

    private Payment mPayment;

    private IFinishFragmentListener mListener;
    private IDirectPrintListener.Stub mPrintListener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof IFinishFragmentListener) {
            mListener = (IFinishFragmentListener) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_final, container, false);

        mPrintBtn = view.findViewById(R.id.print);
        mFinishBtn = view.findViewById(R.id.end);

        mPrintBtn.setOnClickListener(this);
        mFinishBtn.setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.print:
                print();
                break;
            case R.id.end:
                if (mListener != null) {
                    mListener.onFinishOrder();
                }
                break;
        }
    }

    public void setPayment(Payment payment) {
        this.mPayment = payment;
    }

    public void print() {
        if (mPayment != null) {
            Receipt receipt = mPayment.getReceipt();

            if (mPrintListener == null) {
                mPrintListener = createPrintListener();
            }

            FragmentActivity activity = getActivity();
            if (receipt == null && activity != null) {
                PrinterUtility.getInstance().printUnknownReceipt(activity, mPayment, mPrintListener);

                Toast.makeText(activity, R.string.receipt_unavailable_message, Toast.LENGTH_LONG).show();
            } else {
                PrinterUtility.getInstance().printReceipt(receipt, mPrintListener);
            }
        }
    }

    private IDirectPrintListener.Stub createPrintListener() {
        return new IDirectPrintListener.Stub() {

            @Override
            public void started(String printId) throws RemoteException {

            }

            /** Called when the print job cannot continue, but could be resumed later. */
            @Override
            public void block(String printId, String errorMessage) throws RemoteException {
            }

            /** Called when the print job has finished being cancelled. This is the final message. */
            @Override
            public void cancel(String printId) throws RemoteException {
            }

            @Override
            public void failed(String printId, String errorMessage) throws RemoteException {
                final FragmentActivity activity = getActivity();
                if (activity != null) {
                    final String message = errorMessage == null ? getString(R.string.print_failed_message) : errorMessage;
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void complete(String printId) throws RemoteException {
                final FragmentActivity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getActivity(), R.string.print_completed_message, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        };
    }

    public interface IFinishFragmentListener {
        void onFinishOrder();
    }
}

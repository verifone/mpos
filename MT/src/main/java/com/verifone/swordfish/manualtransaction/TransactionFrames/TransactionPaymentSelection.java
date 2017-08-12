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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.verifone.commerce.entities.Merchandise;
import com.verifone.swordfish.manualtransaction.MTDataModel.MTTransaction;
import com.verifone.swordfish.manualtransaction.R;
import com.verifone.swordfish.manualtransaction.SupportFragments.ButtonsFragment;
import com.verifone.swordfish.manualtransaction.SupportFragments.PaymentSelectionCompanion;
import com.verifone.swordfish.manualtransaction.SupportFragments.TransactionShowList;

import java.math.BigDecimal;
import java.util.Set;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link TransactionPaymentSelection.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link TransactionPaymentSelection#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TransactionPaymentSelection extends Fragment implements
        PaymentSelectionCompanion.OnFragmentInteractionListener,
        ButtonsFragment.OnFragmentInteractionListener {

    private static final String TAG = TransactionPaymentSelection.class.getSimpleName();
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;
    private PaymentSelectionCompanion paymentSelectionCompanion;
    private ButtonsFragment buttonsFragment;
    private Set<Merchandise> entryItemsSet;
    private BigDecimal transactionTotal;
    private MTTransaction currentTransaction;

    public TransactionPaymentSelection() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment TransactionPaymentSelection.
     */
    public static TransactionPaymentSelection newInstance(String param1, String param2) {
        TransactionPaymentSelection fragment = new TransactionPaymentSelection();
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
        View myView = inflater.inflate(R.layout.fragment_transaction_payment_selection, container, false);

        FragmentManager manager = getActivity().getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        TransactionShowList showList = new TransactionShowList();
        showList.setTransaction(currentTransaction);
        transaction.add(R.id.itemList, showList);
        paymentSelectionCompanion = new PaymentSelectionCompanion();
        paymentSelectionCompanion.setListener(this);
        transaction.add(R.id.paymentSelectionFrameLayout, paymentSelectionCompanion);
        buttonsFragment = new ButtonsFragment();
        transaction.add(R.id.paymentsButtonsFrame, buttonsFragment);
        buttonsFragment.setListener(this);
        transaction.commit();

        return myView;
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
    public void cashTransaction() {

        if (mListener != null) {
            mListener.payByCash();
        }
    }

    @Override
    public void creditTransaction() {
        if (mListener != null) {
            mListener.payByCard();
        }
    }

    @Override
    public void manualEntryTransaction() {
        if (mListener != null) {
            mListener.payByManualEntry();
        }
    }

    @Override
    public void onButtonPress(int buttonID) {
        switch (buttonID) {
            case 0:
                if (mListener != null) {
                    mListener.cancelTransaction();
                }
                break;
            case 1:
                if (mListener != null) {
                    mListener.splitTransaction();
                }
                break;
            case 2:
                if (mListener != null) {
                    mListener.goBack();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void readyToConfigureButtons() {
        buttonsFragment.onConfigureButton(0, true, getActivity().getString(R.string.buttonCancelTx));
        buttonsFragment.onConfigureButton(1, true, getActivity().getString(R.string.split_button_label));
        buttonsFragment.onConfigureButton(2, true, getActivity().getString(R.string.buttonBack));

    }

    public void setListener(TransactionPaymentSelection.OnFragmentInteractionListener listener) {
        mListener = listener;
    }

    public void setList(Set<Merchandise> items) {
        entryItemsSet = items;
    }

    public void setTotal(BigDecimal total) {
        transactionTotal = total;
    }

    public void setTransaction(MTTransaction transaction) {
        currentTransaction = transaction;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnFragmentInteractionListener {
        void cancelTransaction();

        void splitTransaction();

        void goBack();

        void payByCash();

        void payByCard();

        void payByManualEntry();
    }
}

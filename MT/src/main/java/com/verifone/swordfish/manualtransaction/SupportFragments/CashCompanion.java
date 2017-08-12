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

import com.verifone.swordfish.manualtransaction.MTDataModel.MTTransaction;
import com.verifone.swordfish.manualtransaction.R;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link CashCompanion.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link CashCompanion#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CashCompanion extends Fragment implements
        NumericKeyboard.OnFragmentInteractionListener,
        AmountKeyboard.OnFragmentInteractionListener,
        ButtonsFragment.OnFragmentInteractionListener {

    private static final String TAG = CashCompanion.class.getSimpleName();
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;
    private NumericKeyboard numericKeyboard;
    private AmountKeyboard amountKeyboard;
    private ButtonsFragment buttonsFragment;
    private MTTransaction transaction;
    private boolean mCancelable = true;
    private boolean mTenderPresent = false;

    public CashCompanion() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment CashCompanion.
     */
    public static CashCompanion newInstance(String param1, String param2) {
        CashCompanion fragment = new CashCompanion();
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
        Log.i(TAG, "OnCreateView");
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_cash_companion, container, false);
        numericKeyboard = new NumericKeyboard();
        numericKeyboard.setListener(this);

        amountKeyboard = new AmountKeyboard();
        if (transaction.getTransactionTotal().doubleValue() > 3.0) {
            amountKeyboard.setFreeButtonText(transaction.getTransactionTotal().floatValue());
        }
        amountKeyboard.setOnFragmentInteractionListener(this);

        buttonsFragment = new ButtonsFragment();
        buttonsFragment.setListener(this);

        FragmentManager manager = getActivity().getSupportFragmentManager();
        final FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.add(R.id.cashNumericKeyboard, numericKeyboard)
                .add(R.id.cashCompAmountKeyboard, amountKeyboard)
                .add(R.id.cashCompButtonsCashFrame, buttonsFragment)
                .commit();
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
    public void onKeyboardButtonPress(String title) {
        mListener.onKeyboardButtonPress(title);
    }

    @Override
    public void onButtonPress(int buttonID) {
        mListener.onButtonPress(buttonID);
    }

    @Override
    public void readyToConfigureButtons() {
        buttonsFragment.onConfigureButton(0, mCancelable, getContext().getString(R.string.buttonCancelTx));
        buttonsFragment.onConfigureButton(1, true, getContext().getString(R.string.buttonBack));
        buttonsFragment.onConfigureButton(2, mTenderPresent, getContext().getString(R.string.tender_button_label));
    }

    @Override
    public void onButtonPress(View view) {
        mListener.onButtonPress(view);
    }

    public void setListener(CashCompanion.OnFragmentInteractionListener listener) {
        mListener = listener;
    }

    public void onConfigureButton(int button, boolean active, String label) {
        buttonsFragment.onConfigureButton(button, active, label);
    }

    public void setTransaction(MTTransaction currentTransaction) {
        transaction = currentTransaction;
    }

    public void setCancelButton(boolean cancelButton) {
        mCancelable = cancelButton;
    }

    public void setTenderButton(boolean tenderButton) {
        mTenderPresent = tenderButton;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnFragmentInteractionListener {
        void onKeyboardButtonPress(String title);

        void onButtonPress(int buttonID);

        void onButtonPress(View view);
    }
}

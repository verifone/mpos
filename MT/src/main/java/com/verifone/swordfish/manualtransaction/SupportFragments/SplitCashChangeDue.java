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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.verifone.swordfish.manualtransaction.R;
import com.verifone.swordfish.manualtransaction.Tools.LocalizeCurrencyFormatter;

import java.math.BigDecimal;
import java.text.NumberFormat;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link SplitCashChangeDue.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link SplitCashChangeDue#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SplitCashChangeDue extends Fragment implements ButtonsFragment.OnFragmentInteractionListener {

    private static final String TAG = SplitCashChangeDue.class.getSimpleName();
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;
    private BigDecimal changeDue = BigDecimal.ZERO;
    private ButtonsFragment buttonsFragment;

    public SplitCashChangeDue() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SplitCashChangeDue.
     */
    // TODO: Rename and change types and number of parameters
    public static SplitCashChangeDue newInstance(String param1, String param2) {
        SplitCashChangeDue fragment = new SplitCashChangeDue();
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
        View view = inflater.inflate(R.layout.fragment_split_cash_change_due, container, false);
        TextView changeText = (TextView) view.findViewById(R.id.changeDue);
        LocalizeCurrencyFormatter localizeCurrencyFormatter = LocalizeCurrencyFormatter.getInstance();
        NumberFormat numberFormat = localizeCurrencyFormatter.getCurrencyFormat();
        changeText.setText(numberFormat.format(changeDue.doubleValue()));
        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onResume() {
        super.onResume();
        buttonsFragment = new ButtonsFragment();
        buttonsFragment.setListener(this);
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.changeDueButtonsFrame, buttonsFragment)
                .commit();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onButtonPress(int buttonID) {
        switch (buttonID) {
            case 2:
                mListener.onNextSelected();
                break;
            default:
                break;
        }
    }

    @Override
    public void readyToConfigureButtons() {
        buttonsFragment.onConfigureButton(0, false, null);
        buttonsFragment.onConfigureButton(1, false, null);
        buttonsFragment.onConfigureButton(2, true, getActivity().getString(R.string.button_next));
    }

    public void setListener(SplitCashChangeDue.OnFragmentInteractionListener listener) {
        mListener = listener;
    }

    public void setChangeDue(BigDecimal change) {
        changeDue = change;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnFragmentInteractionListener {
        void onNextSelected();
    }
}

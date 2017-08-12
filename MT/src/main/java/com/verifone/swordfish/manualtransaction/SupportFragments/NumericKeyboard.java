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

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.verifone.swordfish.manualtransaction.R;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link NumericKeyboard.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link NumericKeyboard#newInstance} factory method to
 * create an instance of this fragment.
 */
public class NumericKeyboard extends Fragment {

    private static final String TAG = NumericKeyboard.class.getSimpleName();
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;
    private OnFragmentInteractionListener mListener;

    public NumericKeyboard() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment NumericKeyboard.
     */
    public static NumericKeyboard newInstance(String param1, String param2) {
        NumericKeyboard fragment = new NumericKeyboard();
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
        View myView = inflater.inflate(R.layout.fragment_numeric_keyboard, container, false);
        configureButtonListener((ImageButton) myView.findViewById(R.id.keyboardButton));
        configureButtonListener((ImageButton) myView.findViewById(R.id.button2));
        configureButtonListener((ImageButton) myView.findViewById(R.id.button3));
        configureButtonListener((ImageButton) myView.findViewById(R.id.button4));
        configureButtonListener((ImageButton) myView.findViewById(R.id.button5));
        configureButtonListener((ImageButton) myView.findViewById(R.id.button6));
        configureButtonListener((ImageButton) myView.findViewById(R.id.button7));
        configureButtonListener((ImageButton) myView.findViewById(R.id.button8));
        configureButtonListener((ImageButton) myView.findViewById(R.id.button9));
        configureButtonListener((ImageButton) myView.findViewById(R.id.button0));
        configureButtonListener((ImageButton) myView.findViewById(R.id.button00));
        configureButtonListener((ImageButton) myView.findViewById(R.id.buttonDelete));

        return myView;
    }

    private void configureButtonListener(ImageButton button) {
        if (button != null) {
            button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ImageButton myButton = (ImageButton) v;
                    onButtonPressed(myButton.getTag().toString());
                }
            });
        }

    }

    public void onButtonPressed(String title) {
        if (mListener != null) {
            mListener.onKeyboardButtonPress(title);
        }
    }

    public void setListener(NumericKeyboard.OnFragmentInteractionListener listener) {
        mListener = listener;
    }

    @Override
    public void onAttach(Activity context) {
        super.onAttach(context);
/*
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
*/
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private void keyButtonPress(View v) {
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnFragmentInteractionListener {
        void onKeyboardButtonPress(String title);
    }
}

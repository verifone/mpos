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
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.verifone.swordfish.manualtransaction.R;
import com.verifone.swordfish.manualtransaction.Tools.MposLogger;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OperationsKeyboard.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link OperationsKeyboard#newInstance} factory method to
 * create an instance of this fragment.
 */
public class OperationsKeyboard extends Fragment {

    private static final String TAG = OperationsKeyboard.class.getSimpleName();
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;
    private String internalRepresentation;
    private EditText quantityEditText;
    ImageButton decreaseButton;
    ImageButton increaseButton;
    boolean start = true;
    int quantity;
    int finalQuantity;

    public OperationsKeyboard() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment OperationsKeyboard.
     */
    public static OperationsKeyboard newInstance(String param1, String param2) {
        OperationsKeyboard fragment = new OperationsKeyboard();
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
    public void onAttach(Activity context) {
        super.onAttach(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View myView = inflater.inflate(R.layout.fragment_operations_keyboard, container, false);
        quantityEditText = (EditText) myView.findViewById(R.id.quantityField);
        increaseButton = (ImageButton) myView.findViewById(R.id.imageButtonAdd);
        decreaseButton = (ImageButton) myView.findViewById(R.id.imageButtonSubstract);

        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(quantityEditText.getWindowToken(), 0);
        ImageButton enterButton = (ImageButton) myView.findViewById(R.id.buttonEnter);
        if (enterButton != null) {
            enterButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (quantityEditText.getText().toString().equals("") ||
                            quantityEditText.getText().toString().equals("0") ||
                            quantityEditText.getText().toString().equals("1")) {
                        quantityEditText.setText("1");
                        quantity = 1;
                        mListener.onTimesButtonPress(quantityEditText.getText().toString());
                    } else {
                        if (!quantityEditText.getText().toString().equals("1")) {
                            finalQuantity = Integer.parseInt(quantityEditText.getText().toString());
                            mListener.onTimesButtonPress(quantityEditText.getText().toString());
                            quantityEditText.setText("1");
                            resetValues();
                        }
                    }
                    mListener.onAddButtonPress();
                }

            });
        }

        quantityEditText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int inType = quantityEditText.getInputType(); // backup the input type
                quantityEditText.setInputType(InputType.TYPE_NULL); // disable soft input
                quantityEditText.onTouchEvent(event); // call native handler
                quantityEditText.setInputType(inType); // restore input type
                quantityEditText.setFocusable(true);
                quantityEditText.setText("");
                quantityEditText.setCursorVisible(true);
                internalRepresentation = quantityEditText.getText().toString();
                mListener.focusOnQuantityOpKeyboard();
                return false;
            }
        });

        quantityEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                quantityEditText.setSelection(quantityEditText.length());
                return true;
            }
        });


        quantityEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
    /* When focus is lost check that the text field
    * has valid values.
    */
                EditText editText = (EditText) v;
                if (!hasFocus && editText != null) {
                    String currentValue = editText.getText().toString();
                    if (currentValue.length() > 0) {
                        try {
                            int newValue = Integer.getInteger(currentValue);
                            setQuantity(newValue);
                        } catch (Exception e) {
                            MposLogger.getInstance().error("Exception set qty", e.toString());
                        }
                    }
                }
                quantityEditText.setSelection(quantityEditText.length());
            }
        });

        if (increaseButton != null) {
            increaseButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    increaseQuantity();
                }
            });
        }

        if (decreaseButton != null) {
            decreaseButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    decreaseQuantity();
                }
            });
        }

        return myView;
    }

    private void resetValues() {
        quantity = 1;
        quantityEditText.setText(Integer.toString(quantity));
        quantityEditText.setSelection(1);
        setColorFlag(1);
    }

    private void increaseQuantity() {
        if (quantityEditText.getText().length() >= 1) {
            String currentQuantity = quantityEditText.getText().toString();
            int actualQuantity = Integer.parseInt(currentQuantity);
            quantity = actualQuantity + 1;
            setColorFlag(quantity);
            quantityEditText.setText(String.format("%d", quantity));
            quantityEditText.setSelection(quantityEditText.length());
        } else {
            quantity = 1;
            quantityEditText.setText(String.format("%d", quantity));
        }


    }

    private void decreaseQuantity() {
        if (quantityEditText.getText().length() >= 1) {
            int actualQuantity = Integer.parseInt(quantityEditText.getText().toString());
            if (actualQuantity > 1) {
                quantity = actualQuantity - 1;
                setColorFlag(quantity);
                quantityEditText.setText(String.format("%d", quantity));
                quantityEditText.setSelection(quantityEditText.length());
            }
        } else {
            quantity = 1;
            quantityEditText.setText(String.format("%d", quantity));
        }
    }

    private void setColorFlag(int quantity) {
        if (quantity >= 2) {
            decreaseButton.setImageResource(R.drawable.icon_qty_down_on);
        } else {
            decreaseButton.setImageResource(R.drawable.icon_qty_down_off);
        }
    }


    public int getQuantity() {
        if (finalQuantity == 0) {
            finalQuantity = quantity;
        }
        return finalQuantity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * Use this  method to append new digit to the quantityEditText field
     *
     * @param newValue Parameter 1.
     */
    public void attachValue(String newValue) {

        if (!newValue.contains("D")) {
            int qty = Integer.valueOf(newValue);
            if ((qty >= 0) && (qty <= 9)) {
                quantityEditText.setText(quantityEditText.getText() + newValue);
            }
        } else {
            if (quantityEditText.getText().length() > 0) {
                quantityEditText.setText(quantityEditText.getText().toString().substring(0, quantityEditText.getText().length() - 1));
            }
        }
        quantityEditText.setSelection(quantityEditText.length());
        quantityEditText.setCursorVisible(true);

    }

    public void reset() {
        quantity = 1;
        finalQuantity = 1;
        if (quantityEditText != null) {
            quantityEditText.setText("1");
        }
    }

    private void setQuantity(int newValue) {
        quantity = newValue;
    }

    public void setListener(OperationsKeyboard.OnFragmentInteractionListener listener) {
        mListener = listener;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnFragmentInteractionListener {
        void onAddButtonPress();

        void onTimesButtonPress(String title);

        void focusOnQuantityOpKeyboard();
    }
}

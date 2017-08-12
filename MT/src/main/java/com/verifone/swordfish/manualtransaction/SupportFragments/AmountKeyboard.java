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

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.verifone.swordfish.manualtransaction.R;

import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link AmountKeyboard.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link AmountKeyboard#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AmountKeyboard extends Fragment {
    private static final String TAG = AmountKeyboard.class.getSimpleName();
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;
    private ImageButton freeButton;
    private String freeButtonLabel;
    private TextView freeButtonTextView;
    private String freeButtonLabelTag;
    private AmountKeyboard.OnFragmentInteractionListener mListener;

    public AmountKeyboard() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment AmountKeyboard.
     */
    public static AmountKeyboard newInstance(String param1, String param2) {
        AmountKeyboard fragment = new AmountKeyboard();
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
        View myView = inflater.inflate(R.layout.fragment_amount_keyboard, container, false);
        freeButton = (ImageButton) myView.findViewById(R.id.btnCalculatedAmount);

        Currency localCurrency = Currency.getInstance(Locale.getDefault());
        TextView freeTextView = (TextView) myView.findViewById(R.id.textViewCalculatedAmount);
        if (freeButtonLabel != null) {
            freeButton.setTag("3");
            //freeTextView.setText("3");
            float amount = (float) 3.0;//Float.parseFloat(freeButtonLabel.substring(2));
            configureTextView(freeTextView, amount);
        } else {
            freeButton.setVisibility(View.INVISIBLE);
        }
        freeButtonTextView = freeTextView;
        configureButtonListener(freeButton);
        configureButtonListener((ImageButton) myView.findViewById(R.id.buttonFive));
        configureButtonListener((ImageButton) myView.findViewById(R.id.buttonTen));
        configureButtonListener((ImageButton) myView.findViewById(R.id.buttonTwenty));
        configureButtonListener((ImageButton) myView.findViewById(R.id.buttonFifty));
        configureButtonListener((ImageButton) myView.findViewById(R.id.buttonOneHundred));
        if (freeButtonLabel == null) {
            configureTextView(freeButtonTextView, 0);
        } else {
            configureTextView(freeButtonTextView, 3f);
        }
        configureTextView((TextView) myView.findViewById(R.id.textView5), 5f);
        configureTextView((TextView) myView.findViewById(R.id.textView10), 10f);
        configureTextView((TextView) myView.findViewById(R.id.textView20), 20f);
        configureTextView((TextView) myView.findViewById(R.id.textView50), 50f);
        configureTextView((TextView) myView.findViewById(R.id.textView100), 100f);

        return myView;
    }

    private void configureButtonListener(ImageButton button) {
        if (button != null) {
            button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ImageButton myButton = (ImageButton) v;
                    mListener.onButtonPress(myButton);
                }
            });
        }

    }

    private void configureTextView(TextView textView, float amount) {
        textView.setText(formatLocalCurrencyAmount(amount));
    }

    private String formatLocalCurrencyAmount(float amount) {
        Currency localCurrency = Currency.getInstance(Locale.getDefault());
        String localCurrencySymbol = localCurrency.getSymbol();
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
        // We then tell our formatter to use this symbol.
        DecimalFormatSymbols decimalFormatSymbols = ((java.text.DecimalFormat) currencyFormat).getDecimalFormatSymbols();
        decimalFormatSymbols.setCurrencySymbol(localCurrencySymbol);
        ((java.text.DecimalFormat) currencyFormat).setDecimalFormatSymbols(decimalFormatSymbols);
        currencyFormat.setMaximumFractionDigits(0);
        return currencyFormat.format(amount);

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

    @SuppressLint("DefaultLocale")
    public void setFreeButtonText(float amount) {
        if (freeButton != null) {
            freeButton.setVisibility(View.VISIBLE);
            freeButtonLabelTag = String.format("%.0f", amount);
            freeButton.setId((int) amount);
            configureTextView(freeButtonTextView, amount);
            //freeButton.setText(formatLocalCurrencyAmount(amount));
        }
        freeButtonLabel = formatLocalCurrencyAmount(amount);
    }

    public void hideFreeButtonText() {
        if (freeButton != null) {
            freeButton.setVisibility(View.INVISIBLE);
        }
        freeButtonLabel = null;
    }

    public void setOnFragmentInteractionListener(AmountKeyboard.OnFragmentInteractionListener listener) {
        mListener = listener;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnFragmentInteractionListener {
        void onButtonPress(View view);
    }
}

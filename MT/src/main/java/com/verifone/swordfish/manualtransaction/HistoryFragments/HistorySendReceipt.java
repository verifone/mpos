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
import android.telephony.PhoneNumberUtils;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.verifone.swordfish.manualtransaction.R;

import java.util.Locale;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link HistorySendReceipt.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link HistorySendReceipt#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HistorySendReceipt extends Fragment {

    private static final String TAG = HistorySendReceipt.class.getSimpleName();
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;
    private int typeOfReceipt;
    private EditText senderAddress;
    private String phoneNumberUnformatted;

    public HistorySendReceipt() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HistorySendReceipt.
     */
    public static HistorySendReceipt newInstance(String param1, String param2) {
        HistorySendReceipt fragment = new HistorySendReceipt();
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
        View view = inflater.inflate(R.layout.fragment_history_send_receipt, container, false);
        ImageButton backButton = (ImageButton) view.findViewById(R.id.buttonBack);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onReceiptBackButton();
            }
        });

        final Button saveNoteButton = (Button) view.findViewById(R.id.buttonSaveNote);
        saveNoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (typeOfReceipt == 0) {
                    mListener.onSendSMS(phoneNumberUnformatted);
                } else {
                    mListener.onSendEmail(senderAddress.getText().toString());
                }

                //mListener.onSaveNote();
            }
        });
        saveNoteButton.setEnabled(false);

        senderAddress = (EditText) view.findViewById(R.id.editTextAddNote);
        TextView title = (TextView) view.findViewById(R.id.textViewNoteTitle);
        TextView subTitle = (TextView) view.findViewById(R.id.textViewNoteSubtitle);
        if (typeOfReceipt == 0) {
            //for phone number
            String titleLabel = getActivity().getResources().getString(R.string.sms_receipt_title);
            String subTitleLabel = getActivity().getResources().getString(R.string.sms_receipt_subtitle);
            title.setText(titleLabel);
            subTitle.setText(subTitleLabel);
            senderAddress.setInputType(InputType.TYPE_CLASS_PHONE);
            senderAddress.setHint("Phone number");
        } else {
            //for email
            String titleLabel = getActivity().getResources().getString(R.string.email_receipt_title);
            String subTitleLabel = getActivity().getResources().getString(R.string.email_receipt_subtitle);
            title.setText(titleLabel);
            subTitle.setText(subTitleLabel);
            senderAddress.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            senderAddress.setHint("Email");
        }

        senderAddress.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    // do your stuff here
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

                    if (typeOfReceipt == 0) {
                        PhoneNumberUtils utils = new PhoneNumberUtils();
                        Locale locale = Locale.getDefault();
                        String iso = locale.getCountry();
                        phoneNumberUnformatted = senderAddress.getText().toString();
                        String phoneNumber = utils.formatNumber(senderAddress.getText().toString(), iso);
                        if (phoneNumber != null) {
                            senderAddress.setText(phoneNumber);
                            senderAddress.setTextColor(getActivity().getResources().getColor(R.color.greyish));
                            saveNoteButton.setEnabled(true);
                        } else {
                            senderAddress.setTextColor(getActivity().getResources().getColor(R.color.vermillion));
                            saveNoteButton.setEnabled(false);
                        }
                    } else {
                        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
                        if (!senderAddress.getText().toString().matches(emailPattern) && senderAddress.getText().length() > 0) {
                            senderAddress.setTextColor(getActivity().getResources().getColor(R.color.vermillion));
                            saveNoteButton.setEnabled(false);

                        } else {
                            saveNoteButton.setEnabled(true);
                            senderAddress.setTextColor(getActivity().getResources().getColor(R.color.greyish));
                        }
                    }
                }
                //v.setEnabled(false);
                return false;
            }
        });

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

    public void setType(int newType) {
        typeOfReceipt = newType;
    }

    public void setListener(OnFragmentInteractionListener listener) {
        mListener = listener;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnFragmentInteractionListener {
        void onReceiptBackButton();

        void onSendSMS(String address);

        void onSendEmail(String address);
    }
}

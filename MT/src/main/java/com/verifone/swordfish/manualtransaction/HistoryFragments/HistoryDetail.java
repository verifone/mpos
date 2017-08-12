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
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.verifone.commerce.entities.Merchandise;
import com.verifone.commerce.entities.Payment;
import com.verifone.swordfish.manualtransaction.MTDataModel.MTTransaction;
import com.verifone.swordfish.manualtransaction.R;
import com.verifone.swordfish.manualtransaction.Tools.LocalizeCurrencyFormatter;
import com.verifone.swordfish.manualtransaction.Tools.MposLogger;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link HistoryDetail.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link HistoryDetail#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HistoryDetail extends Fragment {

    private static final String TAG = HistoryDetail.class.getSimpleName();
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;

    private SurfaceView headerColoredView;
    private LinearLayout headerLinearLayout;
    private LinearLayout subHeaderLinearLayout;
    private ListView listView;
    private ViewGroup mContainer;
    private MTTransaction currentTransaction;

    private OnFragmentInteractionListener mListener;

    public HistoryDetail() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HistoryDetail.
     */
    public static HistoryDetail newInstance(String param1, String param2) {
        HistoryDetail fragment = new HistoryDetail();
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
        this.mContainer = container;
        View view = inflater.inflate(R.layout.fragment_history_detail, container, false);
        headerColoredView = (SurfaceView) view.findViewById(R.id.historyHeaderView);
        headerLinearLayout = (LinearLayout) view.findViewById(R.id.historyHeader);
        ViewGroup.LayoutParams params = headerLinearLayout.getLayoutParams();
        int width = params.width;
        int height = params.height;
        Button actionButton = (Button) view.findViewById(R.id.typeOfTxButton);
        TextView upperHeaderLabel = (TextView) view.findViewById(R.id.historyDetailUpperLabel);
        TextView lowerHeaderLabel = (TextView) view.findViewById(R.id.historyDetailLowerLabel);
        TextView txIdLabel = (TextView) view.findViewById(R.id.historyTransactionId);
        TransactionStatus status = currentTransaction.getStatus();

        final int numPayments = currentTransaction.transactionPayments() != null ? currentTransaction.transactionPayments().getPayments().size() : 0;
        switch (status) {
            case voided: {
                headerColoredView.setBackgroundColor(getResources().getColor(R.color.vermillion));
                height = 125;
                upperHeaderLabel.setText(getResources().getString(R.string.voided_history_label));
                lowerHeaderLabel.setText(getResources().getString(R.string.upper_history_tx_label) + currentTransaction.getTransactionId());
                actionButton.setVisibility(View.INVISIBLE);
                break;
            }
            case refunded: {
                headerColoredView.setBackgroundColor(getResources().getColor(R.color.squash));
                height = 125;
                upperHeaderLabel.setText(getResources().getString(R.string.refund_history_label));
                lowerHeaderLabel.setText(getResources().getString(R.string.upper_history_tx_label) + currentTransaction.getTransactionId());
                actionButton.setVisibility(View.INVISIBLE);
                break;
            }
            case partiallyRefunded: {
                headerColoredView.setBackgroundColor(getResources().getColor(R.color.pumpkin_orange));
                height = 125;
                upperHeaderLabel.setText(getResources().getString(R.string.partial_refund_history_label));
                lowerHeaderLabel.setText(getResources().getString(R.string.upper_history_tx_label) + currentTransaction.getTransactionId());
                break;
            }
            default:
                headerColoredView.setBackgroundColor(getResources().getColor(R.color.white_two));
                height = 0;
                headerLinearLayout.setVisibility(View.INVISIBLE);
                actionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (numPayments == 1) {
                            Payment payment = currentTransaction.transactionPayments().getPayments().get(0);
                            if (payment != null) {
                                switch (payment.getPaymentType()) {
                                    case DEBIT:
                                    case CASH: {
                                        mListener.onRefundSelected();
                                        break;
                                    }
                                    case CREDIT: {
                                        mListener.onVoidSelected();
                                        break;
                                    }
                                    default:
                                        break;
                                }
                            }
                        }
                    }
                });
                break;

        }
        txIdLabel.setText(getResources().getString(R.string.lower_history_tx_label, currentTransaction.getInvoiceId()));
        params.height = height;
        headerLinearLayout.setLayoutParams(params);
        listView = (ListView) view.findViewById(R.id.historyTransactionList);
        List<Merchandise> merchandises = currentTransaction.transactionMerchandises();
        List<LinearLayout> items = new ArrayList<>();
        LocalizeCurrencyFormatter formatter = LocalizeCurrencyFormatter.getInstance();
        MposLogger.getInstance().debug(TAG, Integer.toString(merchandises.size()));

        for (Merchandise merchandise : merchandises) {
            LinearLayout itemView = (LinearLayout) inflater.inflate(R.layout.history_detail_item, null);
            TextView descriptionView = (TextView) itemView.findViewById(R.id.historyItemDescription);

            if (merchandise.getDescription() != null && merchandise.getDescription().length() > 0) {
                descriptionView.setText(merchandise.getDescription());
            } else {
                descriptionView.setText(" ");
            }

            TextView totalView = (TextView) itemView.findViewById(R.id.historyItemPrice);
            totalView.setText(formatter.getCurrencyFormat().format(merchandise.getAmount().doubleValue()));
            items.add(itemView);

            if (merchandise.getDiscount() != null && !Objects.equals(merchandise.getDiscount(), BigDecimal.ZERO)) {
                LinearLayout itemModView = (LinearLayout) inflater.inflate(R.layout.history_detail_item_modifier, null);
                TextView textViewDiscount = (TextView) itemModView.findViewById(R.id.itemModifier);
                textViewDiscount.setText(getString(R.string.str_discount_value,
                        formatter.getCurrencyFormat().format(merchandise.getDiscount().doubleValue())));
                items.add(itemModView);
            }

            if (merchandise.getTax() != null && !Objects.equals(merchandise.getTax(), BigDecimal.ZERO)) {
                LinearLayout itemModView = (LinearLayout) inflater.inflate(R.layout.history_detail_item_modifier, null);
                TextView textViewDiscount = (TextView) itemModView.findViewById(R.id.itemModifier);
                textViewDiscount.setText(getContext().getString(R.string.str_tax_value,
                        formatter.getCurrencyFormat().format(merchandise.getTax())));
                items.add(itemModView);
            }

            if (merchandise.getQuantity() > 1) {
                LinearLayout itemModView = (LinearLayout) inflater.inflate(R.layout.history_detail_item_modifier, null);
                TextView textViewDiscount = (TextView) itemModView.findViewById(R.id.itemModifier);
                textViewDiscount.setText(getContext().getString(R.string.str_quantity_value, Integer.toString(merchandise.getQuantity())));
                items.add(itemModView);
            }
            LinearLayout dividerView = (LinearLayout) inflater.inflate(R.layout.history_detail_item_divider, null);
            items.add(dividerView);
        }

        LinearLayout totalView = (LinearLayout) inflater.inflate(R.layout.history_detail_item_total, null);
        TextView totalLabel = (TextView) totalView.findViewById(R.id.historyTxTotalLabel);
        totalLabel.setText(getResources().getString(R.string.history_detail_tx_total_label));
        String tax;
        if (currentTransaction.getTransactionTax() != null && !Objects.equals(currentTransaction.getTransactionTax(), BigDecimal.ZERO)) {
            tax = formatter.getCurrencyFormat().format(currentTransaction.getTransactionTax());
        } else {
            tax = getContext().getString(R.string.str_dollar_zero);
        }

        TextView taxLabel = (TextView) totalView.findViewById(R.id.historyTxTotalTaxLabel);
        taxLabel.setText(getResources().getString(R.string.history_detail_tx_total_tax_label, tax));
        TextView txTotalLabel = (TextView) totalView.findViewById(R.id.historyTxTotalAmount);
        txTotalLabel.setText(formatter.getCurrencyFormat().format(currentTransaction.getTransactionTotal()));
        items.add(totalView);

        for (Payment payment : currentTransaction.transactionPayments().getPayments()) {
            LinearLayout paymentView = null;
            if (payment.getPaymentType() == null) {
                payment.setPaymentType(Payment.PaymentType.CREDIT);
            }
            switch (payment.getPaymentType()) {
                case CASH: {
                    paymentView = (LinearLayout) inflater.inflate(R.layout.history_cash_total, null);
                    if (currentTransaction.getStatus() == TransactionStatus.refunded) {
                        TextView subHeader = (TextView) paymentView.findViewById(R.id.historyCashChangeLL);
                        subHeader.setText(getActivity().getResources().getString(R.string.history_cash_refunded));
                    } else {
                        if (numPayments == 1) {
                            actionButton.setText(getActivity().getResources().getString(R.string.refund_title_textView));
                        }
                    }
                    TextView totalAmount = (TextView) paymentView.findViewById(R.id.historyCashTotalLL);
                    totalAmount.setText(formatter.getCurrencyFormat().format(payment.getPaymentAmount().doubleValue()));
                }
                break;
                case CREDIT:
                case DEBIT: {
                    paymentView = (LinearLayout) inflater.inflate(R.layout.history_card_total, null);
                    if (payment.getPaymentType() == Payment.PaymentType.DEBIT) {
                        TextView cardType = (TextView) paymentView.findViewById(R.id.historyCardType);
                        cardType.setText(getActivity().getResources().getString(R.string.history_card_type_debit));

                    }
                    if (payment.getCardInformation().getCardHolderName() != null) {
                        TextView cardHolder = (TextView) paymentView.findViewById(R.id.historyCardHolder);
                        cardHolder.setText(" **** " + payment.getCardInformation().getPanLast4());
                    }
                    if (payment.getAuthCode() != null) {
                        TextView cardHolder = (TextView) paymentView.findViewById(R.id.historyCardOwner);
                        cardHolder.setText(payment.getCardInformation().getCardHolderName());
                    }
                    if (payment.getCardInformation().getCardExpiry() != null) {
                        TextView cardExp = (TextView) paymentView.findViewById(R.id.historyCardExp);
                        cardExp.setText(getActivity().getResources().getString(R.string.history_car_exp_date) + payment.getCardInformation().getCardExpiry());
                    }
                    if (numPayments == 1) {
                        actionButton.setText(getActivity().getResources().getString(R.string.void_label));
                    }
                    TextView totalAmount = (TextView) paymentView.findViewById(R.id.historyCardTotal);
                    totalAmount.setText(formatter.getCurrencyFormat().format(payment.getPaymentAmount().doubleValue()));
                    break;
                }
                default:
                    break;

            }
            if (paymentView != null) {
                items.add(paymentView);
            }

        }
        TransactionDetailArrayAdapter adapter = new TransactionDetailArrayAdapter(getActivity().getBaseContext(), items);
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        Button printButton = (Button) view.findViewById(R.id.historyPrintButton);
        printButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onPrintTransaction();
            }
        });
        /*Button sendButton = (Button) view.findViewById(R.id.historySendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onSendReceipt();
            }
        });*/
        Button noteButton = (Button) view.findViewById(R.id.historyNoteButton);
        noteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onPresentNote();
            }
        });
        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onResume() {
        super.onResume();
//        LayoutInflater inflater = LayoutInflater.from(getActivity().getBaseContext());
//        View view = inflater.inflate(R.layout.fragment_history_detail, mContainer, false);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void setListener(HistoryDetail.OnFragmentInteractionListener listener) {
        mListener = listener;
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

        void onPresentNote();

        void onPrintTransaction();

        void onSendReceipt();

        void onVoidSelected();

        void onRefundSelected();
    }

    private class TransactionDetailArrayAdapter<T> extends ArrayAdapter<LinearLayout> {
        private final Context context;
        private final List<LinearLayout> itemsArray;

        TransactionDetailArrayAdapter(Context context, List<LinearLayout> items) {
            super(context, -1, items);
            this.context = context;
            this.itemsArray = items;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            return itemsArray.get(position);
        }
    }

}

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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.verifone.commerce.entities.Merchandise;
import com.verifone.swordfish.manualtransaction.MTDataModel.MTTransaction;
import com.verifone.swordfish.manualtransaction.R;
import com.verifone.swordfish.manualtransaction.Tools.LocalizeCurrencyFormatter;
import com.verifone.swordfish.manualtransaction.Tools.MposLogger;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link HistoryRefundDetail.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link HistoryRefundDetail#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HistoryRefundDetail extends Fragment {

    private static final String TAG = HistoryRefundDetail.class.getSimpleName();
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;
    private MTTransaction currentTransaction;
    private ListView listView;
    private ListView headerListView;
    private View mView;
    private BigDecimal refundTotal;
    private List<Merchandise> refundItems;
    private TextView txTotalLabel;

    public HistoryRefundDetail() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HistoryRefundDetail.
     */
    public static HistoryRefundDetail newInstance(String param1, String param2) {
        HistoryRefundDetail fragment = new HistoryRefundDetail();
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
        View view = inflater.inflate(R.layout.fragment_history_refund_detail, container, false);
        mView = view;
        listView = (ListView) view.findViewById(R.id.refundList);
        headerListView = (ListView) view.findViewById(R.id.refundHeaderList);
        List<LinearLayout> headers = new ArrayList<LinearLayout>();
        LinearLayout headerView = (LinearLayout) inflater.inflate(R.layout.void_list_header, null);
        TextView txNumber = (TextView) headerView.findViewById(R.id.voidHeaderTextView);
        txNumber.setText(getActivity().getResources().getString(R.string.lower_history_tx_label, currentTransaction.getTransactionId()));
        headers.add(headerView);
        TransactionDetailArrayAdapter headerAdapter = new TransactionDetailArrayAdapter(getActivity().getBaseContext(), headers);
        headerListView.setAdapter(headerAdapter);
        headerAdapter.notifyDataSetChanged();

        List<Merchandise> merchandises = currentTransaction.transactionMerchandises();
        List<LinearLayout> items = new ArrayList<>();
        refundItems = new ArrayList<>();
        final LocalizeCurrencyFormatter formatter = LocalizeCurrencyFormatter.getInstance();
        for (final Merchandise merchandise : merchandises) {
            refundItems.add(merchandise);
            LinearLayout itemView = (LinearLayout) inflater.inflate(R.layout.refund_cell, null);
            final Button refundButton = (Button) itemView.findViewById(R.id.refundItemButton);
            refundButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MposLogger.getInstance().debug(TAG, " merchandise removed" + formatter.getCurrencyFormat().format(merchandise.getAmount()));
                    view.setVisibility(View.INVISIBLE);
                    view.setEnabled(false);
                    refundItem(merchandise);
                }
            });
            refundButton.setVisibility(View.INVISIBLE);
            TextView descriptionView = (TextView) itemView.findViewById(R.id.refundItemDescription);
            if (merchandise.getDescription() != null && merchandise.getDescription().length() > 0) {
                descriptionView.setText(merchandise.getDescription());
            } else {
                descriptionView.setText("N/A");
            }
            TextView totalView = (TextView) itemView.findViewById(R.id.refundItemTotal);
            totalView.setText(formatter.getCurrencyFormat().format(merchandise.getAmount().doubleValue()));
            itemView.setEnabled(true);
            itemView.setClickable(true);
            items.add(itemView);
            if (merchandise.getDiscount() != null && !Objects.equals(merchandise.getDiscount(), BigDecimal.ZERO)) {
                LinearLayout itemModView = (LinearLayout) inflater.inflate(R.layout.history_detail_item_modifier, null);
                TextView textViewDiscount = (TextView) itemModView.findViewById(R.id.itemModifier);
                textViewDiscount.setText(getContext().getString(R.string.str_discount_value,
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
        String tax;
        if (currentTransaction.getTransactionTax() != null &&
                !Objects.equals(currentTransaction.getTransactionTax(), BigDecimal.ZERO)) {
            tax = formatter.getCurrencyFormat().format(currentTransaction.getTransactionTax());
        } else {
            tax = getContext().getString(R.string.str_dollar_zero);
        }
        TextView taxLabel = (TextView) view.findViewById(R.id.refundTaxTextView);
        taxLabel.setText(getResources().getString(R.string.history_detail_tx_total_tax_label, tax));
        txTotalLabel = (TextView) view.findViewById(R.id.refundTotalTextView);
        refundTotal = currentTransaction.getTransactionTotal();
        txTotalLabel.setText(formatter.getCurrencyFormat().format(currentTransaction.getTransactionTotal()));
        TransactionDetailArrayAdapter adapter = new TransactionDetailArrayAdapter(getActivity().getBaseContext(), items);
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();

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

    private void refundItem(Merchandise merchandise) {
        refundTotal = refundTotal.subtract(merchandise.getAmount());
        LocalizeCurrencyFormatter formatter = LocalizeCurrencyFormatter.getInstance();
        txTotalLabel.setText(formatter.getCurrencyFormat().format(refundTotal.doubleValue()));
        refundItems.remove(merchandise);
    }

    private void restoreItem(Merchandise merchandise) {
        refundItems.add(merchandise);
        refundTotal = refundTotal.add(merchandise.getAmount());
        LocalizeCurrencyFormatter formatter = LocalizeCurrencyFormatter.getInstance();
        txTotalLabel.setText(formatter.getCurrencyFormat().format(refundTotal.doubleValue()));
    }

    public void setListener(HistoryRefundDetail.OnFragmentInteractionListener listener) {
        mListener = listener;
    }

    public void setTransaction(MTTransaction transaction) {
        currentTransaction = transaction;
    }

    public void setVoidedHeader() {
        headerListView = (ListView) mView.findViewById(R.id.refundHeaderList);
        List<LinearLayout> headers = new ArrayList<LinearLayout>();
        LayoutInflater inflater = LayoutInflater.from(getActivity().getBaseContext());
        LinearLayout headerView = (LinearLayout) inflater.inflate(R.layout.void_final_header, null);
        LinearLayout coloredHV = (LinearLayout) headerView.findViewById(R.id.historyColoredHeaderLL);
        coloredHV.setBackgroundColor(getActivity().getResources().getColor(R.color.squash));
        TextView txDate = (TextView) headerView.findViewById(R.id.voidDateText);
        SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a", Locale.getDefault());
        Date today = Calendar.getInstance().getTime();
        String reportDate = df.format(today);
        txDate.setText("on " + reportDate);
        headers.add(headerView);
        LinearLayout subHeaderView = (LinearLayout) inflater.inflate(R.layout.void_list_header, null);
        TextView txNumber = (TextView) subHeaderView.findViewById(R.id.voidHeaderTextView);
        txNumber.setText(getActivity().getResources().getString(R.string.lower_history_tx_label, currentTransaction.getTransactionId()));
        headers.add(subHeaderView);
        TransactionDetailArrayAdapter headerAdapter = new TransactionDetailArrayAdapter(getActivity().getBaseContext(), headers);
        headerListView.setAdapter(headerAdapter);
        headerAdapter.notifyDataSetChanged();
        List<Merchandise> merchandises = currentTransaction.transactionMerchandises();
        List<LinearLayout> items = new ArrayList<LinearLayout>();
        refundItems = new ArrayList<Merchandise>();
        final LocalizeCurrencyFormatter formatter = LocalizeCurrencyFormatter.getInstance();
        for (final Merchandise merchandise : merchandises) {
            refundItems.add(merchandise);
            LinearLayout itemView = (LinearLayout) inflater.inflate(R.layout.refund_cell, null);
            final Button refundButton = (Button) itemView.findViewById(R.id.refundItemButton);
            refundButton.setVisibility(View.INVISIBLE);
            TextView descriptionView = (TextView) itemView.findViewById(R.id.refundItemDescription);
            if (merchandise.getDescription() != null && merchandise.getDescription().length() > 0) {
                descriptionView.setText(merchandise.getDescription());
            } else {
                descriptionView.setText("N/A");
            }
            TextView totalView = (TextView) itemView.findViewById(R.id.refundItemTotal);
            totalView.setText(formatter.getCurrencyFormat().format(merchandise.getAmount().doubleValue()));
            items.add(itemView);
            if (merchandise.getDiscount() != null && !Objects.equals(merchandise.getDiscount(), BigDecimal.ZERO)) {
                LinearLayout itemModView = (LinearLayout) inflater.inflate(R.layout.history_detail_item_modifier, null);
                TextView textViewDiscount = (TextView) itemModView.findViewById(R.id.itemModifier);
                textViewDiscount.setText((getContext().getResources().getString(R.string.str_discount_value,
                        formatter.getCurrencyFormat().format(merchandise.getDiscount().doubleValue()))));
                items.add(itemModView);
            }

            if (merchandise.getTax() != null && !Objects.equals(merchandise.getTax(), BigDecimal.ZERO)) {
                LinearLayout itemModView = (LinearLayout) inflater.inflate(R.layout.history_detail_item_modifier, null);
                TextView textViewDiscount = (TextView) itemModView.findViewById(R.id.itemModifier);
                textViewDiscount.setText(getContext().getResources().getString(R.string.str_tax_value,
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
        TransactionDetailArrayAdapter adapter = new TransactionDetailArrayAdapter(getActivity().getBaseContext(), items);
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();

    }


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
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

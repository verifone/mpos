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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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
 * {@link VoidList.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link VoidList#newInstance} factory method to
 * create an instance of this fragment.
 */
public class VoidList extends Fragment {

    private static final String TAG = VoidList.class.getSimpleName();
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;
    private MTTransaction currentTransaction;
    private ListView listView;
    private ListView headerListView;
    private View mView;

    public VoidList() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment VoidList.
     */
    public static VoidList newInstance(String param1, String param2) {
        VoidList fragment = new VoidList();
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
        View view = inflater.inflate(R.layout.fragment_void_list, container, false);
        mView = view;
        listView = (ListView) view.findViewById(R.id.voidList);
        headerListView = (ListView) view.findViewById(R.id.headerList);
        List<LinearLayout> headers = new ArrayList<LinearLayout>();
        LinearLayout headerView = (LinearLayout) inflater.inflate(R.layout.void_list_header, null);
        TextView txNumber = (TextView) headerView.findViewById(R.id.voidHeaderTextView);
        txNumber.setText(getActivity().getResources().getString(R.string.lower_history_tx_label , currentTransaction.getTransactionId()));
        headers.add(headerView);
        TransactionDetailArrayAdapter headerAdapter = new TransactionDetailArrayAdapter(getActivity().getBaseContext(), headers);
        headerListView.setAdapter(headerAdapter);
        headerAdapter.notifyDataSetChanged();

        List<Merchandise> merchandises = currentTransaction.transactionMerchandises();
        List<LinearLayout> items = new ArrayList<LinearLayout>();
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
            if (merchandise.getDiscount() != null && merchandise.getDiscount() != BigDecimal.ZERO) {
                LinearLayout itemModView = (LinearLayout) inflater.inflate(R.layout.history_detail_item_modifier, null);
                TextView textViewDiscount = (TextView) itemModView.findViewById(R.id.itemModifier);
                textViewDiscount.setText(getContext().getString(R.string.str_discount_value,
                        formatter.getCurrencyFormat().format(merchandise.getDiscount().doubleValue())));
                items.add(itemModView);
            }

            if (merchandise.getTax() != null && merchandise.getTax() != BigDecimal.ZERO) {
                LinearLayout itemModView = (LinearLayout) inflater.inflate(R.layout.history_detail_item_modifier, null);
                TextView textViewDiscount = (TextView) itemModView.findViewById(R.id.itemModifier);
                textViewDiscount.setText(getContext().getString(R.string.str_tax_value,
                        formatter.getCurrencyFormat().format(merchandise.getTax())));
                items.add(itemModView);
            }

            if (merchandise.getQuantity() > 1) {
                LinearLayout itemModView = (LinearLayout) inflater.inflate(R.layout.history_detail_item_modifier, null);
                TextView textViewDiscount = (TextView) itemModView.findViewById(R.id.itemModifier);
                textViewDiscount.setText(getContext().getString(R.string.str_quantity_value,
                        Integer.toString(merchandise.getQuantity())));
                items.add(itemModView);
            }
            LinearLayout dividerView = (LinearLayout) inflater.inflate(R.layout.history_detail_item_divider, null);
            items.add(dividerView);
        }
        String tax;
        if (currentTransaction.getTransactionTax() != null && !Objects.equals(currentTransaction.getTransactionTax(), BigDecimal.ZERO)) {
            tax = formatter.getCurrencyFormat().format(currentTransaction.getTransactionTax());
        } else {
            tax = getContext().getString(R.string.str_dollar_zero);
        }
        TextView taxLabel = (TextView) view.findViewById(R.id.voidTaxTextView);
        taxLabel.setText(getResources().getString(R.string.history_detail_tx_total_tax_label, tax));
        TextView txTotalLabel = (TextView) view.findViewById(R.id.voidTotalTextView);
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

    public void setListener(VoidList.OnFragmentInteractionListener listener) {
        mListener = listener;
    }

    public void setTransaction(MTTransaction transaction) {
        currentTransaction = transaction;
    }

    public void setVoidedHeader() {
        headerListView = (ListView) mView.findViewById(R.id.headerList);
        List<LinearLayout> headers = new ArrayList<LinearLayout>();
        LayoutInflater inflater = LayoutInflater.from(getActivity().getBaseContext());
        LinearLayout headerView = (LinearLayout) inflater.inflate(R.layout.void_final_header, null);
        TextView txDate = (TextView) headerView.findViewById(R.id.voidDateText);
        SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a", Locale.getDefault());
        Date today = Calendar.getInstance().getTime();
        String reportDate = df.format(today);
        txDate.setText(getContext().getString(R.string.history_str_on_date, reportDate));
        headers.add(headerView);
        LinearLayout subHeaderView = (LinearLayout) inflater.inflate(R.layout.void_list_header, null);
        TextView txNumber = (TextView) subHeaderView.findViewById(R.id.voidHeaderTextView);
        txNumber.setText(getActivity().getResources().getString(R.string.lower_history_tx_label, currentTransaction.getTransactionId()));
        headers.add(subHeaderView);
        TransactionDetailArrayAdapter headerAdapter = new TransactionDetailArrayAdapter(getActivity().getBaseContext(), headers);
        headerListView.setAdapter(headerAdapter);
        headerAdapter.notifyDataSetChanged();

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
        // TODO: Update argument type and name
    }

    public class TransactionDetailArrayAdapter<T> extends ArrayAdapter<LinearLayout> {
        private final Context context;
        private final List<LinearLayout> itemsArray;

        public TransactionDetailArrayAdapter(Context context, List<LinearLayout> items) {
            super(context, -1, items);
            this.context = context;
            this.itemsArray = items;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return itemsArray.get(position);
        }
    }

}

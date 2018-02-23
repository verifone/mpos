package com.verifone.swordfish.manualtransaction.gui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.verifone.commerce.entities.Merchandise;
import com.verifone.swordfish.manualtransaction.ManualTransactionApplication;
import com.verifone.swordfish.manualtransaction.R;
import com.verifone.swordfish.manualtransaction.tools.LocalizeCurrencyFormatter;
import com.verifone.swordfish.manualtransaction.tools.MposLogger;
import com.verifone.swordfish.manualtransaction.tools.Utils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Copyright (C) 2016,2017,2018 Verifone, Inc.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * VERIFONE, INC. BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * <p>
 * Except as contained in this notice, the name of Verifone, Inc. shall not be
 * used in advertising or otherwise to promote the sale, use or other dealings
 * in this Software without prior written authorization from Verifone, Inc.
 * <p>
 * <p>
 * Created by abey on 1/4/2018.
 */

public class OrderListFragment extends Fragment {

    private static final String EDITABLE_KEY = "editable";

    private EditText mCurrentValueET;
    private TextView mCurrentValueSymbolTV;
    private TextView mTaxValueTV;
    private TextView mTotalValueTV;

    private ListView mItemsList;

    private IOrderListFragmentListener mListener;

    private boolean bEditable;

    public static OrderListFragment getInstance(boolean isEditable) {
        OrderListFragment fragment = new OrderListFragment();
        Bundle args = new Bundle();
        args.putBoolean(EDITABLE_KEY, isEditable);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof IOrderListFragmentListener) {
            mListener = (IOrderListFragmentListener) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transaction_list, container, false);

        if (getArguments() != null) {
            bEditable = getArguments().getBoolean(EDITABLE_KEY, false);
        }

        mCurrentValueET = view.findViewById(R.id.addItem);
        mCurrentValueSymbolTV = view.findViewById(R.id.currencySymbol);
        mTaxValueTV = view.findViewById(R.id.taxTotalTextView);
        mTotalValueTV = view.findViewById(R.id.granTotalTextView);

        mItemsList = view.findViewById(R.id.listViewItems);

        setDisplayText(null);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateBasket();
    }

    public void setDisplayText(String amount) {
        if (amount != null && amount.length() > 0) {
            if (mCurrentValueSymbolTV.getVisibility() == View.INVISIBLE) {
                mCurrentValueSymbolTV.setVisibility(View.VISIBLE);
            }
            mCurrentValueET.setText(amount);
        } else {
            mCurrentValueET.setText(getContext().getString(R.string.str_new_item));
            mCurrentValueSymbolTV.setVisibility(View.INVISIBLE);
        }
    }

    public void updateBasket() {
        List<Merchandise> list = ManualTransactionApplication.getCarbonBridge().getMerchandises();
        if (list != null) {
            ArrayAdapter arrayAdapter = null;
            if (bEditable) {
                arrayAdapter = new TransactionDetailArrayAdapter
                        (getActivity(), new ArrayList<>(list));
            } else {
                arrayAdapter = new TransactionDetailArrayAdapterSimple
                        (getActivity(), new ArrayList<>(list));
            }
            mItemsList.setAdapter(arrayAdapter);

            BigDecimal tax = new BigDecimal("0");
            BigDecimal itemsSubtotal = new BigDecimal("0");
            BigDecimal itemsDiscount = new BigDecimal("0");

            for (Merchandise merchandise : list) {
                tax = tax.add(merchandise.getTax());
                itemsSubtotal = itemsSubtotal.add(merchandise.getQuantity().multiply(merchandise
                        .getUnitPrice()));
                itemsDiscount = itemsDiscount.add(merchandise.getDiscount());
            }
            BigDecimal total = itemsSubtotal.add(tax).subtract(itemsDiscount);

            mTotalValueTV.setText(Utils.getLocalizedAmount(total));
            mTaxValueTV.setText(Utils.getLocalizedAmount(tax));
        } else {
            mItemsList.setAdapter(null);
        }

    }


    private class TransactionDetailArrayAdapter extends ArrayAdapter<Merchandise> {
        private final ArrayList<Merchandise> itemsArray;

        TransactionDetailArrayAdapter(Context context, ArrayList<Merchandise> items) {
            super(context, -1, items);
            this.itemsArray = items;
        }

        //TODO: need to use viewholders, or change listview to recycler view.
        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            final Merchandise currentItem = this.itemsArray.get(position);
            LayoutInflater inflater = getActivity().getLayoutInflater();
            LinearLayout row = (LinearLayout) inflater.inflate(R.layout.transaction_detail_cell, parent, false);
            Button addButton = row.findViewById(R.id.buttonDetailAddNote);
            LinearLayout linearLayout = row.findViewById(R.id.linearLayoutDetailNotes);
            if ((currentItem.getDescription().equals(" ") || currentItem.getDescription().equals(""))
                    && (Objects.equals(currentItem.getDiscount(), BigDecimal.ZERO) && currentItem.getDiscount().toString().equals("0"))
                    && (Objects.equals(currentItem.getTax(), BigDecimal.ZERO) || currentItem.getTax().toString().equals("0"))) {

                linearLayout.setVisibility(View.INVISIBLE);
                addButton.setVisibility(View.VISIBLE);
                addButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        mListener.onItemClicked(currentItem);
                    }
                });
            } else {
                addButton.setVisibility(View.INVISIBLE);
                linearLayout.setVisibility(View.VISIBLE);
                TextView description = row.findViewById(R.id.textViewDetailNote);
                ImageButton edit = row.findViewById(R.id.imageButtonEditNote);
                description.setText(currentItem.getDescription());
                edit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mListener.onItemClicked(currentItem);
                    }
                });
            }
            LocalizeCurrencyFormatter formatter = LocalizeCurrencyFormatter.getInstance();

            TextView totalItem = row.findViewById(R.id.textViewDetailTotal);
            MposLogger.getInstance().debug("item: ", String.format(Locale.US,
                    "Unit Price: %.2f, Price: %.2f, tax: %.2f, discount: %.2f, qth: %.2f",
                    currentItem.getUnitPrice().floatValue(),
                    currentItem.getAmount().floatValue(),
                    currentItem.getTax().floatValue(),
                    currentItem.getDiscount().floatValue(),
                    currentItem.getQuantity().floatValue()));
//            Log.i(TAG, "Unit Price : " + currentItem.getUnitPrice().floatValue() +
//                    " Price : " + currentItem.getAmount().floatValue() +
//                    " Tax : " + currentItem.getTax().floatValue() +
//                    " Discount : " + currentItem.getDiscount().floatValue() +
//                    " Quantity : " + currentItem.getQuantity().floatValue());
            if (currentItem.getDiscount() != null
                    && !Objects.equals(currentItem.getDiscount(), BigDecimal.ZERO)
                    && currentItem.getDiscount().doubleValue() > 0.0) {
                TextView textViewDiscount = row.findViewById(R.id.textViewDetailDiscount);
                ViewGroup.LayoutParams params = textViewDiscount.getLayoutParams();
                params.height = 24;
                textViewDiscount.setLayoutParams(params);
                String discountValue = formatter.getCurrencyFormat().format(currentItem.getDiscount().doubleValue());
                //String formattedString = getContext().getString(R.string.str_discount_value, discountValue);
                textViewDiscount.setText(getContext().getString(R.string.str_discount_value, discountValue));
            }

            if (currentItem.getTax() != null
                    && !Objects.equals(currentItem.getTax(), BigDecimal.ZERO)
                    && currentItem.getTax().doubleValue() > 0.0) {
                TextView textViewTax = row.findViewById(R.id.textViewDetailTax);
                ViewGroup.LayoutParams params = textViewTax.getLayoutParams();
                params.height = 24;
                String taxValue = formatter.getCurrencyFormat().format(currentItem.getTax().doubleValue());
                textViewTax.setText(getContext().getString(R.string.str_tax_value, taxValue));
            }

            if (currentItem.getQuantity().doubleValue() > 1d) {
                TextView textViewQuantity = row.findViewById(R.id.textViewDetailQuantity);
                ViewGroup.LayoutParams params = textViewQuantity.getLayoutParams();
                params.height = 24;
                textViewQuantity.setText(getContext().getString(R.string.str_quantity_value,
                        currentItem.getQuantity().toString()));
            }
            formatter.getCurrencyFormat().format(currentItem.getAmount().doubleValue());
            totalItem.setText(Utils.getLocalizedAmount(currentItem.getUnitPrice().multiply(currentItem.getQuantity())));
            return row;
        }
    }

    private class TransactionDetailArrayAdapterSimple extends ArrayAdapter<Merchandise> {
        private final ArrayList<Merchandise> itemsArray;

        TransactionDetailArrayAdapterSimple(Context context, ArrayList<Merchandise> items) {
            super(context, -1, items);
            this.itemsArray = items;
        }

        //TODO: need to use viewholders, or change listview to recycler view.
        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            final Merchandise currentItem = this.itemsArray.get(position);
            LayoutInflater inflater = getActivity().getLayoutInflater();
            LinearLayout row = (LinearLayout) inflater.inflate(R.layout.transaction_detail_cell, parent, false);
            LinearLayout linearLayout = row.findViewById(R.id.linearLayoutDetailNotes);
            linearLayout.setVisibility(View.INVISIBLE);
//            if ((currentItem.getDescription().equals(" ") || currentItem.getDescription().equals(""))
//                    && (Objects.equals(currentItem.getDiscount(), BigDecimal.ZERO) && currentItem.getDiscount().toString().equals("0"))
//                    && (Objects.equals(currentItem.getTax(), BigDecimal.ZERO) || currentItem.getTax().toString().equals("0"))) {
//
//                linearLayout.setVisibility(View.INVISIBLE);
//                addButton.setVisibility(View.VISIBLE);
//                addButton.setOnClickListener(new View.OnClickListener() {
//                    public void onClick(View v) {
//                        mListener.onItemClicked(currentItem);
//                    }
//                });
//            } else {
//                addButton.setVisibility(View.INVISIBLE);
//                linearLayout.setVisibility(View.VISIBLE);
//                TextView description = (TextView) row.findViewById(R.id.textViewDetailNote);
//                ImageButton edit = (ImageButton) row.findViewById(R.id.imageButtonEditNote);
//                description.setText(currentItem.getDescription());
//                edit.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        mListener.onItemClicked(currentItem);
//                    }
//                });
//            }
            LocalizeCurrencyFormatter formatter = LocalizeCurrencyFormatter.getInstance();

            TextView totalItem = row.findViewById(R.id.textViewDetailTotal);
            MposLogger.getInstance().debug("item: ", String.format(Locale.US,
                    "Unit Price: %.2f, Price: %.2f, tax: %.2f, discount: %.2f, qth: %.2f",
                    currentItem.getUnitPrice().floatValue(),
                    currentItem.getAmount().floatValue(),
                    currentItem.getTax().floatValue(),
                    currentItem.getDiscount().floatValue(),
                    currentItem.getQuantity().floatValue()));
//            Log.i(TAG, "Unit Price : " + currentItem.getUnitPrice().floatValue() +
//                    " Price : " + currentItem.getAmount().floatValue() +
//                    " Tax : " + currentItem.getTax().floatValue() +
//                    " Discount : " + currentItem.getDiscount().floatValue() +
//                    " Quantity : " + currentItem.getQuantity().floatValue());
            if (currentItem.getDiscount() != null
                    && !Objects.equals(currentItem.getDiscount(), BigDecimal.ZERO)
                    && currentItem.getDiscount().doubleValue() > 0.0) {
                TextView textViewDiscount = row.findViewById(R.id.textViewDetailDiscount);
                ViewGroup.LayoutParams params = textViewDiscount.getLayoutParams();
                params.height = 24;
                textViewDiscount.setLayoutParams(params);
                String discountValue = formatter.getCurrencyFormat().format(currentItem.getDiscount().doubleValue());
                //String formattedString = getContext().getString(R.string.str_discount_value, discountValue);
                textViewDiscount.setText(getContext().getString(R.string.str_discount_value, discountValue));
            }

            if (currentItem.getTax() != null
                    && !Objects.equals(currentItem.getTax(), BigDecimal.ZERO)
                    && currentItem.getTax().doubleValue() > 0.0) {
                TextView textViewTax = row.findViewById(R.id.textViewDetailTax);
                ViewGroup.LayoutParams params = textViewTax.getLayoutParams();
                params.height = 24;
                String taxValue = formatter.getCurrencyFormat().format(currentItem.getTax().doubleValue());
                textViewTax.setText(getContext().getString(R.string.str_tax_value, taxValue));
            }

            if (currentItem.getQuantity().doubleValue() > 1d) {
                TextView textViewQuantity = row.findViewById(R.id.textViewDetailQuantity);
                ViewGroup.LayoutParams params = textViewQuantity.getLayoutParams();
                params.height = 24;
                textViewQuantity.setText(getContext().getString(R.string.str_quantity_value,
                        currentItem.getQuantity().toString()));
            }
            formatter.getCurrencyFormat().format(currentItem.getAmount().doubleValue());
            totalItem.setText(Utils.getLocalizedAmount(currentItem.getUnitPrice().multiply(currentItem.getQuantity())));
            return row;
        }
    }

    public interface IOrderListFragmentListener {
        void onItemClicked(Merchandise merchandise);
    }
}

package com.verifone.swordfish.manualtransaction.gui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.verifone.commerce.entities.Merchandise;
import com.verifone.swordfish.manualtransaction.ManualTransactionApplication;
import com.verifone.swordfish.manualtransaction.R;
import com.verifone.swordfish.manualtransaction.tools.LocalizeCurrencyFormatter;
import com.verifone.swordfish.manualtransaction.tools.Utils;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

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

    private static final String TAG = OrderListFragment.class.getSimpleName();

    private static final String EDITABLE_KEY = "editable";

    private NumericEditText mCurrentValueET;
    private TextView mTaxValueTV;
    private TextView mTotalValueTV;

    private ListView mItemsList;

    private IOrderListFragmentListener mListener;

    private boolean mIsFragmentEditable;

    public static OrderListFragment getInstance(boolean isEditable) {
        OrderListFragment fragment = new OrderListFragment();
        Bundle args = new Bundle(1);
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
        mListener = null;
        super.onDetach();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transaction_list, container, false);

        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        if (getArguments() != null) {
            mIsFragmentEditable = getArguments().getBoolean(EDITABLE_KEY, false);
        }

        mCurrentValueET = view.findViewById(R.id.addItem);
        mTaxValueTV = view.findViewById(R.id.taxTotalTextView);
        mTotalValueTV = view.findViewById(R.id.granTotalTextView);
        mItemsList = view.findViewById(R.id.listViewItems);

        mCurrentValueET.setRepresentationType(NumericEditText.RepresentationType.CURRENCY);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateBasket();
        if (!mIsFragmentEditable) {
            mCurrentValueET.setVisibility(View.GONE);
        } else {
            mCurrentValueET.setVisibility(View.VISIBLE);
            mCurrentValueET.requestFocus();
        }
    }

    public void updateBasket() {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            Log.w(TAG, "updateBasket: fragment isn't attached, context is null!");
            return;
        }

        List<Merchandise> list = ManualTransactionApplication.getCarbonBridge().getMerchandises();

        BigDecimal tax = BigDecimal.ZERO;
        BigDecimal itemsSubtotal = BigDecimal.ZERO;
        BigDecimal itemsDiscount = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;

        if (list != null && !list.isEmpty()) {
            ListAdapter arrayAdapter = new TransactionDetailArrayAdapter(activity, new ArrayList<>(list), mIsFragmentEditable);
            mItemsList.setAdapter(arrayAdapter);

            for (Merchandise merchandise : list) {
                tax = tax.add(merchandise.getTax());
                itemsSubtotal = itemsSubtotal.add(merchandise.getExtendedPrice());
                itemsDiscount = itemsDiscount.add(merchandise.getDiscount());
                totalAmount = totalAmount.add(merchandise.getAmount());
            }
        } else {
            mItemsList.setAdapter(null);
        }

        mTotalValueTV.setText(Utils.getLocalizedAmount(totalAmount));
        mTaxValueTV.setText(Utils.getLocalizedAmount(tax));
    }

    public int getBasketSize() {
        return mItemsList.getCount();
    }

    public String getCurrentPrice() {
        return mCurrentValueET.getValue();
    }

    public void clearCurrentPriceValue() {
        mCurrentValueET.clearValue();
        mCurrentValueET.requestFocus();
    }

    private class TransactionDetailArrayAdapter extends ArrayAdapter<Merchandise> {
        private final ArrayList<Merchandise> itemsArray;

        private final boolean mIsEditable;

        TransactionDetailArrayAdapter(Context context, ArrayList<Merchandise> items, boolean isEditable) {
            super(context, -1, items);
            this.itemsArray = items;
            mIsEditable = isEditable;
        }

        //TODO: need to use viewholders, or change listview to recycler view.
        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            final Merchandise currentItem = this.itemsArray.get(position);
            LayoutInflater inflater = getActivity().getLayoutInflater();
            LinearLayout row = (LinearLayout) inflater.inflate(R.layout.transaction_detail_cell, parent, false);

            View editNoteButton = row.findViewById(R.id.imageButtonEditNote);
            View addNoteButton = row.findViewById(R.id.buttonDetailAddNote);

            LinearLayout linearLayout = row.findViewById(R.id.linearLayoutDetailNotes);
            TextView descriptionTextView = row.findViewById(R.id.textViewDetailNote);
            String description = currentItem.getDescription();
            if (TextUtils.isEmpty(description.trim())) {
                linearLayout.setVisibility(View.INVISIBLE);

                if (mIsEditable) {
                    addNoteButton.setVisibility(View.VISIBLE);
                    addNoteButton.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            mListener.onItemClicked(currentItem);
                        }
                    });
                } else {
                    addNoteButton.setVisibility(View.GONE);
                    linearLayout.setVisibility(View.VISIBLE);
                    editNoteButton.setVisibility(View.GONE);
                    descriptionTextView.setText(currentItem.getDisplayLine());
                }

                editNoteButton.setVisibility(View.GONE);
            } else {
                linearLayout.setVisibility(View.VISIBLE);

                addNoteButton.setVisibility(View.GONE);

                if (mIsEditable) {
                    editNoteButton.setVisibility(View.VISIBLE);
                    editNoteButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mListener.onItemClicked(currentItem);
                        }
                    });
                } else {
                    editNoteButton.setVisibility(View.GONE);
                }

                descriptionTextView.setText(description);
            }

            NumberFormat formatter = LocalizeCurrencyFormatter.getInstance().getCurrencyFormat();

            BigDecimal discount = currentItem.getDiscount();
            TextView textViewDiscount = row.findViewById(R.id.textViewDetailDiscount);
            if (discount != null && discount.floatValue() > 0) {
                textViewDiscount.setVisibility(View.VISIBLE);
                String discountValue = formatter.format(discount.doubleValue());
                textViewDiscount.setText(getContext().getString(R.string.str_discount_value, discountValue));
            } else {
                textViewDiscount.setVisibility(View.GONE);
            }

            BigDecimal tax = currentItem.getTax();
            TextView textViewTax = row.findViewById(R.id.textViewDetailTax);
            if (tax != null && tax.floatValue() > 0) {
                textViewTax.setVisibility(View.VISIBLE);
                textViewTax.setText(getContext().getString(R.string.str_tax_value, formatter.format(tax.doubleValue())));
            } else {
                textViewTax.setVisibility(View.GONE);
            }

            BigDecimal quantity = currentItem.getQuantity();
            TextView textViewQuantity = row.findViewById(R.id.textViewDetailQuantity);
            if (quantity.floatValue() > 1) {
                textViewQuantity.setVisibility(View.VISIBLE);
                textViewQuantity.setText(getContext().getString(R.string.str_quantity_value, String.valueOf(quantity.intValue())));
            } else {
                textViewQuantity.setVisibility(View.GONE);
            }

            TextView totalItem = row.findViewById(R.id.textViewDetailTotal);
            totalItem.setText(formatter.format(currentItem.getAmount().doubleValue()));

            return row;
        }
    }

    public interface IOrderListFragmentListener {
        void onItemClicked(Merchandise merchandise);
    }
}

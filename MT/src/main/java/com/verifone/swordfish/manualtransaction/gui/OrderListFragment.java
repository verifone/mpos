package com.verifone.swordfish.manualtransaction.gui;

import android.content.Context;
import android.graphics.Color;
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

import com.verifone.commerce.entities.BasketItem;
import com.verifone.commerce.entities.Donation;
import com.verifone.commerce.entities.Merchandise;
import com.verifone.commerce.entities.Offer;
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

        List<BasketItem> basketItems = new ArrayList<>();

        BigDecimal tax = BigDecimal.ZERO;
        BigDecimal itemsSubtotal = BigDecimal.ZERO;
        BigDecimal itemsDiscount = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;

        List<Merchandise> merchandiseList = ManualTransactionApplication.getCarbonBridge().getMerchandises();
        if (merchandiseList != null && !merchandiseList.isEmpty()) {
            for (Merchandise merchandise : merchandiseList) {
                tax = tax.add(merchandise.getTax());
                itemsSubtotal = itemsSubtotal.add(merchandise.getExtendedPrice());
                itemsDiscount = itemsDiscount.add(merchandise.getDiscount());
                totalAmount = totalAmount.add(merchandise.getAmount());
            }
            basketItems.addAll(merchandiseList);
        }

        List<Offer> offerList = ManualTransactionApplication.getCarbonBridge().getAdjustedOffers();
        if (offerList != null && !offerList.isEmpty()) {
            for (Offer offer : offerList) {
                totalAmount = totalAmount.add(offer.getAmount());
            }
            basketItems.addAll(offerList);
        }

//        List<Donation> donationList = ManualTransactionApplication.getCarbonBridge().getAdjustedDonations();
//        if (donationList != null && !donationList.isEmpty()) {
//            for (Donation donation : donationList) {
//                totalAmount = totalAmount.add(donation.getAmount());
//            }
//            basketItems.addAll(donationList);
//        }

        if (!basketItems.isEmpty()) {
            ListAdapter arrayAdapter = new TransactionDetailArrayAdapter(activity, new ArrayList<>(basketItems), mIsFragmentEditable);
            mItemsList.setAdapter(arrayAdapter);
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

    private enum BasketItemType {
        MERCHANDISE(1),
        OFFER(2),
        DONATION(3),
        DEFAULT(4);

        int value;

        BasketItemType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static BasketItemType getBasketItemType(int value) {
            for (BasketItemType type : BasketItemType.values()) {
                if (type.getValue() == value)
                    return type;
            }
            return DEFAULT;
        }
    }

    // TODO: complex adapter rework required for better managing of multiply items
    private class TransactionDetailArrayAdapter extends ArrayAdapter<BasketItem> {

        private final ArrayList<BasketItem> itemsArray;
        private final boolean mIsEditable;

        TransactionDetailArrayAdapter(Context context, ArrayList<BasketItem> items, boolean isEditable) {
            super(context, -1, items);
            this.itemsArray = items;
            mIsEditable = isEditable;
        }

        @Override
        public int getViewTypeCount() {
            return BasketItemType.values().length;
        }

        @Override
        public int getItemViewType(int position) {
            BasketItem basketItem = getItem(position);
            BasketItemType basketItemType = BasketItemType.DEFAULT;
            if (basketItem instanceof Merchandise) {
                basketItemType = BasketItemType.MERCHANDISE;
            } else if (basketItem instanceof Offer) {
                basketItemType = BasketItemType.OFFER;
            } /*else if (basketItem instanceof Donation) {
                basketItemType = BasketItemType.DONATION;
            }*/
            return basketItemType.getValue();
        }

        //TODO: need to use viewholders, or change listview to recycler view.
        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            final BasketItem currentItem = this.itemsArray.get(position);
            int basketItemType = getItemViewType(position);

            LayoutInflater inflater = getActivity().getLayoutInflater();
            LinearLayout row = (LinearLayout) inflater.inflate(R.layout.transaction_detail_cell, parent, false);

            View editNoteButton = row.findViewById(R.id.imageButtonEditNote);
            View addNoteButton = row.findViewById(R.id.buttonDetailAddNote);

            LinearLayout detailNoteLayout = row.findViewById(R.id.linearLayoutDetailNotes);
            TextView descriptionTextView = row.findViewById(R.id.textViewDetailNote);
            String description = currentItem.getDescription();
            if (description != null) {
                description = description.trim();
            }
            if (TextUtils.isEmpty(description)) {
                detailNoteLayout.setVisibility(View.INVISIBLE);
                if (mIsEditable && basketItemType == BasketItemType.MERCHANDISE.getValue()) {
                    addNoteButton.setVisibility(View.VISIBLE);
                    addNoteButton.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            mListener.onItemClicked((Merchandise) currentItem);
                        }
                    });
                } else {
                    addNoteButton.setVisibility(View.GONE);
                    detailNoteLayout.setVisibility(View.VISIBLE);
                    editNoteButton.setVisibility(View.GONE);
                }
                editNoteButton.setVisibility(View.GONE);
            } else {
                detailNoteLayout.setVisibility(View.VISIBLE);
                addNoteButton.setVisibility(View.GONE);

                if (mIsEditable && basketItemType == BasketItemType.MERCHANDISE.getValue()) {
                    editNoteButton.setVisibility(View.VISIBLE);
                    editNoteButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mListener.onItemClicked((Merchandise) currentItem);
                        }
                    });
                } else {
                    editNoteButton.setVisibility(View.GONE);
                }
            }

            NumberFormat formatter = LocalizeCurrencyFormatter.getInstance().getCurrencyFormat();

            if (basketItemType == BasketItemType.MERCHANDISE.getValue()) {
                BigDecimal discount = ((Merchandise) currentItem).getDiscount();
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

                BigDecimal quantity = ((Merchandise) currentItem).getQuantity();
                TextView textViewQuantity = row.findViewById(R.id.textViewDetailQuantity);
                if (quantity.floatValue() > 1) {
                    textViewQuantity.setVisibility(View.VISIBLE);
                    textViewQuantity.setText(getContext().getString(R.string.str_quantity_value, String.valueOf(quantity.intValue())));
                } else {
                    textViewQuantity.setVisibility(View.GONE);
                }
            }

            if (basketItemType == BasketItemType.DONATION.getValue()) {
                descriptionTextView.setTextColor(Color.GREEN);
            }

            TextView totalItem = row.findViewById(R.id.textViewDetailTotal);
            totalItem.setText(formatter.format(currentItem.getAmount().doubleValue()));
            descriptionTextView.setText(!TextUtils.isEmpty(description) ? description : currentItem.getDisplayLine());
            return row;
        }
    }

    public interface IOrderListFragmentListener {
        void onItemClicked(Merchandise merchandise);
    }
}

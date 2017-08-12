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

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.verifone.commerce.entities.Merchandise;
import com.verifone.commerce.payment.TransactionManager;
import com.verifone.swordfish.manualtransaction.MTDataModel.MTTransaction;
import com.verifone.swordfish.manualtransaction.R;
import com.verifone.swordfish.manualtransaction.System.PaymentTerminal;
import com.verifone.swordfish.manualtransaction.Tools.LocalizeCurrencyFormatter;
import com.verifone.swordfish.manualtransaction.Tools.MposLogger;
import com.verifone.swordfish.manualtransaction.Tools.Utils;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link TransactionList.OnFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class TransactionList extends Fragment {
    private static String TAG = TransactionList.class.getSimpleName();
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;
    private TextView display;
    private TextView granTotalTextView;
    private TextView totalTaxAmountTextView;
    private TextView currencySymbol;
    private ArrayList<LinearLayout> items = new ArrayList<>();
    private ArrayList<Merchandise> transactionItems = new ArrayList<>();
    private ListView transactionListView;
    private Merchandise editItem;
    private String itemDescription;
    private TransactionManager manager;
    private MTTransaction mTransaction;
    private boolean start = true;
    private static BigDecimal transactionTotal = null;

    public TransactionList() {
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_transaction_list, container, false);

        display = (TextView) root.findViewById(R.id.addItem);
        display.setCursorVisible(false);
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(root.getWindowToken(), 0);
        display.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View view, MotionEvent motionEvent) {
                Animation anim = new AlphaAnimation(0.0f, 1.0f);
                anim.setDuration(50); //You can manage the blinking time with this parameter
                anim.setStartOffset(20);
                anim.setRepeatMode(Animation.REVERSE);
                anim.setRepeatCount(10);
                display.startAnimation(anim);
                display.setFocusable(true);
                mListener.requestFocus();
                return false;
            }
        });
        display.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b) {
                    mListener.requestFocus();
                    display.setFocusable(false);
                } else {
                    display.setFocusable(true);
                }
            }
        });
        imm.hideSoftInputFromWindow(display.getWindowToken(), 0);
        granTotalTextView = (TextView) root.findViewById(R.id.granTotalTextView);
        NumberFormat format = LocalizeCurrencyFormatter.getInstance().getCurrencyFormat();

        Merchandise[] merchandises;
        if (mTransaction.transactionMerchandises() != null) {
            merchandises = mTransaction.getConvertedMerchandise(mTransaction);
            //Log.i(TAG, "MT print merchandises : " + merchandises.length);
        } else {
            merchandises = PaymentTerminal.getInstance().getMerchandizes();
            granTotalTextView.setText(getContext().getString(R.string.str_zero_value));
            transactionTotal = new BigDecimal(getContext().getString(R.string.str_zero_value));
        }

        if (merchandises != null) {
            BigDecimal txTotal = new BigDecimal(getContext().getString(R.string.str_zero_value));
            BigDecimal txTax = new BigDecimal(getContext().getString(R.string.str_zero_value));
            for (Merchandise merchandise : merchandises) {
                BigDecimal quantity = new BigDecimal(merchandise.getQuantity());
                txTotal = txTotal.add(merchandise.getUnitPrice().multiply(quantity));
                if (merchandise.getDiscount() != null && !Objects.equals(merchandise.getDiscount(), BigDecimal.ZERO)) {
                    txTotal = txTotal.subtract(merchandise.getDiscount());
                }
                if (merchandise.getTax() != null && !Objects.equals(merchandise.getTax(), BigDecimal.ZERO)) {
                    txTotal = txTotal.add(merchandise.getTax());
                }
                transactionItems.add(merchandise);
            }
            granTotalTextView.setText(format.format(txTotal.doubleValue()));
        }
        currencySymbol = (TextView) root.findViewById(R.id.currencySymbol);
        TransactionDetailArrayAdapter<Merchandise> arrayAdapter =
                new TransactionDetailArrayAdapter<>(getActivity().getBaseContext(), transactionItems);
        transactionListView = (ListView) root.findViewById(R.id.listViewItems);
        transactionListView.setAdapter(arrayAdapter);

        start = true;
        return root;
    }


    @Override
    public void onAttach(Context context) {
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

    private void updateMerchandiseList() {
        transactionItems.clear();
        NumberFormat format = LocalizeCurrencyFormatter.getInstance().getCurrencyFormat();

        Merchandise[] merchandises;
        if (mTransaction.transactionMerchandises() != null) {
            merchandises = mTransaction.getConvertedMerchandise(mTransaction);
            Log.i(TAG, "MT PRINT MERCHANDISES : " + merchandises.length);
        } else {
            merchandises = PaymentTerminal.getInstance().getMerchandizes();
        }
        if (merchandises != null) {
            BigDecimal txTotal = new BigDecimal(getContext().getString(R.string.str_zero_value));
            BigDecimal txTax = new BigDecimal(getContext().getString(R.string.str_zero_value));
            for (Merchandise merchandise : merchandises) {
                BigDecimal quantity = new BigDecimal(merchandise.getQuantity());
                txTotal = txTotal.add(merchandise.getUnitPrice().multiply(quantity));
                if (merchandise.getTax() != null) {
                    txTotal = txTotal.add(merchandise.getTax());
                }
                transactionItems.add(merchandise);
            }
            granTotalTextView.setText(format.format(txTotal.doubleValue()));
        }

        TransactionDetailArrayAdapter<Merchandise> arrayAdapter =
                new TransactionDetailArrayAdapter<Merchandise>(getActivity().getBaseContext(), transactionItems);
        transactionListView.setAdapter(arrayAdapter);

    }

    public void setDisplayText(String amount) {
        if (amount.length() > 0) {
            if (currencySymbol.getVisibility() == View.INVISIBLE) {
                currencySymbol.setVisibility(View.VISIBLE);
            }
            display.setText(amount);
        } else {
            display.setText(getContext().getString(R.string.str_new_item));
            currencySymbol.setVisibility(View.INVISIBLE);
        }
    }

    public String getDisplayData() {
        return display.getText().toString();
    }

    public void addItem(final Merchandise item) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Your code to run in GUI thread here
                MposLogger.getInstance().debug(TAG, "Line item added");
                Log.i(TAG, "AddItem");
                TransactionDetailArrayAdapter adapter = (TransactionDetailArrayAdapter) transactionListView.getAdapter();
                transactionItems.add(item);
                adapter.notifyDataSetChanged();
                NumberFormat numericFormatter = LocalizeCurrencyFormatter.getInstance().getCurrencyFormat();
                BigDecimal quantity = new BigDecimal(item.getQuantity());
                transactionTotal = transactionTotal.add(item.getAmount());
                granTotalTextView.setText(numericFormatter.format(transactionTotal.doubleValue()));
                BigDecimal total;
                total = PaymentTerminal.getInstance().transactionTaxTotal();
                if (total != null) {
                    totalTaxAmountTextView.setText(numericFormatter.format(total.doubleValue()));
                }
                display.setText(getContext().getString(R.string.str_new_item));
                currencySymbol.setVisibility(View.INVISIBLE);
            }
        });

    }

    public void updateItem(final Merchandise item) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Your code to run in GUI thread here
                MposLogger.getInstance().debug(TAG, "Line item updated");
                Log.i(TAG, "updateItem");
                TransactionDetailArrayAdapter adapter = (TransactionDetailArrayAdapter) transactionListView.getAdapter();
                adapter.notifyDataSetChanged();

                NumberFormat numericFormatter = LocalizeCurrencyFormatter.getInstance().getCurrencyFormat();
                BigDecimal quantity = new BigDecimal(item.getQuantity());
                BigDecimal newTotal = new BigDecimal(getContext().getString(R.string.str_zero_value));
                BigDecimal newTaxTotal;

                for (Merchandise merchandise : transactionItems) {
                    BigDecimal merchQuantity = new BigDecimal(merchandise.getQuantity());
                    newTotal = newTotal.add(merchandise.getUnitPrice().multiply(merchQuantity));
                    if (merchandise.getDiscount() != null && !Objects.equals(merchandise.getDiscount(), BigDecimal.ZERO)) {
                        newTotal = newTotal.subtract(merchandise.getDiscount());
                    }
                    if (merchandise.getTax() != null && !Objects.equals(merchandise.getTax(), BigDecimal.ZERO)) {
                        newTotal = newTotal.add(merchandise.getTax());
                    }
                }
                updateMerchandiseList();
                transactionTotal = newTotal;
                granTotalTextView.setText(numericFormatter.format(transactionTotal.doubleValue()));

                newTaxTotal = PaymentTerminal.getInstance().transactionTaxTotal();
                if (newTaxTotal != null) {
                    totalTaxAmountTextView.setText(numericFormatter.format(newTaxTotal.doubleValue()));
                }
                display.setText(getContext().getString(R.string.str_new_item));
                currencySymbol.setVisibility(View.INVISIBLE);
            }
        });
    }

    public void removeItem(final Merchandise item) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Your code to run in GUI thread here
                if (item != null) {
                    MposLogger.getInstance().debug(TAG, "Line item deleted");
                    Log.i(TAG, "RemoveItem");
                    TransactionDetailArrayAdapter adapter = (TransactionDetailArrayAdapter) transactionListView.getAdapter();
                    transactionItems.remove(item);
                    adapter.notifyDataSetChanged();

                    NumberFormat numericFormatter = LocalizeCurrencyFormatter.getInstance().getCurrencyFormat();
                    BigDecimal quantity = new BigDecimal(item.getQuantity());
                    transactionTotal = transactionTotal.subtract(item.getUnitPrice().multiply(quantity));
                    granTotalTextView.setText(numericFormatter.format(transactionTotal.doubleValue()));

                    BigDecimal total;
                    total = PaymentTerminal.getInstance().transactionTaxTotal();
                    if (total != null) {
                        totalTaxAmountTextView.setText(numericFormatter.format(total.doubleValue()));
                    }
                    display.setText(getContext().getString(R.string.str_new_item));
                    currencySymbol.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    public void setListener(TransactionList.OnFragmentInteractionListener listener) {
        mListener = listener;
    }

    // Required due to delete line item not updating the basket while switching back and forth
    // in fragment.
    public void setTransaction(MTTransaction transaction) {
        mTransaction = transaction;
    }

    private class TransactionDetailArrayAdapter<T> extends ArrayAdapter<Merchandise> {
        private final Context context;
        private final ArrayList<Merchandise> itemsArray;

        TransactionDetailArrayAdapter(Context context, ArrayList<Merchandise> items) {
            super(context, -1, items);
            this.context = context;
            this.itemsArray = items;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            final Merchandise currentItem = this.itemsArray.get(position);
            LayoutInflater inflater = getActivity().getLayoutInflater();
            LinearLayout row = (LinearLayout) inflater.inflate(R.layout.transaction_detail_cell, parent, false);
            Button addButton = (Button) row.findViewById(R.id.buttonDetailAddNote);
            LinearLayout linearLayout = (LinearLayout) row.findViewById(R.id.linearLayoutDetailNotes);
            if ((currentItem.getDescription().equals(" ") || currentItem.getDescription().equals(""))
                    && (Objects.equals(currentItem.getDiscount(), BigDecimal.ZERO) && currentItem.getDiscount().toString().equals("0"))
                    && (Objects.equals(currentItem.getTax(), BigDecimal.ZERO) || currentItem.getTax().toString().equals("0"))) {

                linearLayout.setVisibility(View.INVISIBLE);
                addButton.setVisibility(View.VISIBLE);
                addButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        Button myButton = (Button) v;
                        editItem = currentItem;
                        start = false;
                        mListener.onAddNoteDetail(currentItem);
                    }
                });

                itemDescription = "";
            } else {
                addButton.setVisibility(View.INVISIBLE);
                linearLayout.setVisibility(View.VISIBLE);
                TextView description = (TextView) row.findViewById(R.id.textViewDetailNote);
                ImageButton edit = (ImageButton) row.findViewById(R.id.imageButtonEditNote);
                description.setText(currentItem.getDescription());
                edit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        editItem = currentItem;
                        start = false;
                        mListener.onAddNoteDetail(currentItem);
                    }
                });
            }
            LocalizeCurrencyFormatter formatter = LocalizeCurrencyFormatter.getInstance();

            TextView totalItem = (TextView) row.findViewById(R.id.textViewDetailTotal);
            MposLogger.getInstance().debug("item: ", String.format(Locale.US,
                    "Unit Price: %.2f, Price: %.2f, tax: %.2f, discount: %.2f, qth: %d",
                    currentItem.getUnitPrice().floatValue(),
                    currentItem.getAmount().floatValue(),
                    currentItem.getTax().floatValue(),
                    currentItem.getDiscount().floatValue(),
                    currentItem.getQuantity()));
            Log.i(TAG, "Unit Price : " + currentItem.getUnitPrice().floatValue() +
                    " Price : " + currentItem.getAmount().floatValue() +
                    " Tax : " + currentItem.getTax().floatValue() +
                    " Discount : " + currentItem.getDiscount().floatValue() +
                    " Quantity : " + currentItem.getQuantity());
            if (currentItem.getDiscount() != null
                    && !Objects.equals(currentItem.getDiscount(), BigDecimal.ZERO)
                    && currentItem.getDiscount().doubleValue() > 0.0) {
                TextView textViewDiscount = (TextView) row.findViewById(R.id.textViewDetailDiscount);
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
                TextView textViewTax = (TextView) row.findViewById(R.id.textViewDetailTax);
                ViewGroup.LayoutParams params = textViewTax.getLayoutParams();
                params.height = 24;
                String taxValue = formatter.getCurrencyFormat().format(currentItem.getTax().doubleValue());
                textViewTax.setText(getContext().getString(R.string.str_tax_value, taxValue));
            }

            if (currentItem.getQuantity() > 1) {
                TextView textViewQuantity = (TextView) row.findViewById(R.id.textViewDetailQuantity);
                ViewGroup.LayoutParams params = textViewQuantity.getLayoutParams();
                params.height = 24;
                textViewQuantity.setText(getContext().getString(R.string.str_quantity_value,
                        Integer.toString(currentItem.getQuantity())));
            }
            formatter.getCurrencyFormat().format(currentItem.getAmount().doubleValue());
            totalItem.setText(Utils.getLocalizedAmount(currentItem.getUnitPrice().multiply(new BigDecimal(currentItem.getQuantity()))));
            return row;
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnFragmentInteractionListener {
        void requestFocus();

        void onAddNoteDetail(Merchandise merchandise);
    }

}

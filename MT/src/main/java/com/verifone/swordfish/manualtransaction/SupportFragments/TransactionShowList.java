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
import android.view.View;
import android.view.ViewGroup;
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
import com.verifone.swordfish.manualtransaction.Tools.LocalizeCurrencyFormatter;
import com.verifone.swordfish.manualtransaction.Tools.MposLogger;
import com.verifone.swordfish.manualtransaction.Tools.Utils;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * to handle interaction events.
 * Use the {@link TransactionShowList#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TransactionShowList extends Fragment {

    private static final String TAG = TransactionShowList.class.getSimpleName();
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;

    private TextView granTotalTextView;
    private TextView totalTaxAmountTextView;
    private TextView currencySymbol;
    private ArrayList<LinearLayout> items = new ArrayList<>();
    private List<Merchandise> transactionItems = new ArrayList<>();
    private ListView transactionListView;
    private Merchandise editItem;
    private String itemDescription;
    private TransactionManager manager;
    private boolean start = true;
    private BigDecimal transactionTotal;
    private MTTransaction currentTransaction;

    public TransactionShowList() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment TransactionShowList.
     */
    public static TransactionShowList newInstance(String param1, String param2) {
        TransactionShowList fragment = new TransactionShowList();
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
        Log.i(TAG, "onCreateView");
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_transaction_show_list, container, false);

        transactionTotal = new BigDecimal(getContext().getString(R.string.str_zero_value));
        granTotalTextView = (TextView) root.findViewById(R.id.granTotalTextView);
        //       totalTaxAmountTextView = (TextView)root.findViewById(R.id.textTaxAmount);
        //       if (currentTransaction == null) {
        //           totalTaxAmountTextView.setText(format.format(total));

        //       } else {
//            granTotalTextView.setText(currentTransaction.getLocalizedCurrencyTotal());
//            totalTaxAmountTextView.setText(currentTransaction.getLocalizedCurrencyTax());
        //       }



        if (currentTransaction != null) {
            BigDecimal totalTx = new BigDecimal(getContext().getString(R.string.str_zero_value));
            BigDecimal taxesTx = new BigDecimal(getContext().getString(R.string.str_zero_value));

            transactionItems = currentTransaction.transactionMerchandises();
            NumberFormat format = LocalizeCurrencyFormatter.getInstance().getCurrencyFormat();
            for (Merchandise merchandise : transactionItems) {
                BigDecimal quantity = new BigDecimal(merchandise.getQuantity());
                totalTx = totalTx.add(merchandise.getUnitPrice().multiply(quantity));
                if (merchandise.getDiscount() != null && !Objects.equals(merchandise.getDiscount(), BigDecimal.ZERO)) {
                    totalTx = totalTx.subtract(merchandise.getDiscount());
                }
                if (merchandise.getTax() != null && !Objects.equals(merchandise.getTax(), BigDecimal.ZERO)) {
                    totalTx = totalTx.add(merchandise.getTax());
                }
                //transactionItems.add(merchandise);
            }

            granTotalTextView.setText(format.format(totalTx.doubleValue()));
        }
        currencySymbol = (TextView) root.findViewById(R.id.currencySymbol);
        TransactionDetailArrayAdapter<Merchandise> arrayAdapter =
                new TransactionDetailArrayAdapter<>(getActivity().getBaseContext(), transactionItems);
        transactionListView = (ListView) root.findViewById(R.id.listViewShowItems);
        transactionListView.setAdapter(arrayAdapter);
        return root;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();

    }

    public void setTransaction(MTTransaction transaction) {
        currentTransaction = transaction;
    }

    private class TransactionDetailArrayAdapter<T> extends ArrayAdapter<Merchandise> {
        private final Context context;
        private final List<Merchandise> itemsArray;

        TransactionDetailArrayAdapter(Context context, List<Merchandise> items) {
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
                    && Objects.equals(currentItem.getDiscount(), BigDecimal.ZERO)
                    && Objects.equals(currentItem.getTax(), BigDecimal.ZERO)) {
                linearLayout.setVisibility(View.INVISIBLE);
                addButton.setVisibility(View.INVISIBLE);
                addButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                    }
                });

                itemDescription = "";
            } else {
                addButton.setVisibility(View.INVISIBLE);
                linearLayout.setVisibility(View.INVISIBLE);
                TextView description = (TextView) row.findViewById(R.id.textViewDetailNote);
                ImageButton edit = (ImageButton) row.findViewById(R.id.imageButtonEditNote);
                description.setText(currentItem.getDescription());
                edit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                    }
                });
            }
            LocalizeCurrencyFormatter formatter = LocalizeCurrencyFormatter.getInstance();

            TextView totalItem = (TextView) row.findViewById(R.id.textViewDetailTotal);
            MposLogger.getInstance().debug("item: ",
                    String.format(Locale.getDefault(), "Price: %.2f, tax: %.2f, discount: %.2f, qth: %d",
                    currentItem.getAmount().floatValue(),
                    currentItem.getTax().floatValue(),
                    currentItem.getDiscount().floatValue(),
                    currentItem.getQuantity()));
            if (!Objects.equals(currentItem.getDiscount(), BigDecimal.ZERO)) {
                TextView textViewDiscount = (TextView) row.findViewById(R.id.textViewDetailDiscount);
                ViewGroup.LayoutParams params = textViewDiscount.getLayoutParams();
                params.height = 24;
                textViewDiscount.setLayoutParams(params);
                textViewDiscount.setText(getContext().getString(R.string.str_discount_value,
                        formatter.getCurrencyFormat().format(currentItem.getDiscount().doubleValue())));
            }

            if (!Objects.equals(currentItem.getTax(), BigDecimal.ZERO)) {
                TextView textViewTax = (TextView) row.findViewById(R.id.textViewDetailTax);
                ViewGroup.LayoutParams params = textViewTax.getLayoutParams();
                params.height = 24;
                textViewTax.setText(getContext().getString(R.string.str_tax_value,
                        formatter.getCurrencyFormat().format(currentItem.getTax())));
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

}

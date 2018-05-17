package com.verifone.swordfish.manualtransaction.gui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.verifone.commerce.entities.Merchandise;
import com.verifone.swordfish.manualtransaction.R;
import com.verifone.swordfish.manualtransaction.tools.Utils;
import com.verifone.utilities.ConversionUtility;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Locale;

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
 * Created by abey on 1/8/2018.
 */

public class ItemDetailsFragment extends Fragment implements View.OnClickListener {

    private static final String MERCHANDISE_KEY = "Merchandise";

    private final static BigDecimal SALES_TAX = ConversionUtility.parseAmount(7.5d);
    private final static BigDecimal BIG_DECIMAL_HUNDRED = ConversionUtility.parseAmount(100d);

    private IDetailsFragmentListener mListener;

    private TextView mAmountText;

    private EditText mNoteEdit;
    private NumericEditText mQuantityEdit;
    private ImageButton mIncrementBtn;
    private ImageButton mDecrementBtn;

    private NumericEditText mDiscountEdit;
    private RadioGroup mDiscountTypeRG;

    private Switch mTaxSwitch;

    private NumericEditText mUpcEdit;
    private NumericEditText mSkuEdit;

    private Button mDeleteBtn;
    private Button mSaveBtn;
    private Button mCancelBtn;

    private Merchandise mItem;
    private Merchandise mWorkingItem;

    private TextWatcher textWatcher = getTextWatcher();

    public static ItemDetailsFragment getInstance(Merchandise merchandise) {
        ItemDetailsFragment fragment = new ItemDetailsFragment();
        Bundle args = new Bundle();
        args.putParcelable(MERCHANDISE_KEY, merchandise);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof IDetailsFragmentListener) {
            mListener = (IDetailsFragmentListener) context;
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
        View view = inflater.inflate(R.layout.fragment_transaction_detail_edit, container, false);

        if (getArguments() != null) {
            mItem = getArguments().getParcelable(MERCHANDISE_KEY);
            mWorkingItem = new Merchandise();
            if (mItem != null) {
                mWorkingItem.setBasketItemId(mItem.getBasketItemId());
                mWorkingItem.setName(mItem.getName());
                mWorkingItem.setDescription(mItem.getDescription());
                mWorkingItem.setDisplayLine(mItem.getDisplayLine());
                mWorkingItem.setQuantity(mItem.getQuantity());
                mWorkingItem.setAmount(mItem.getAmount());
                mWorkingItem.setUnitPrice(mItem.getUnitPrice());
                mWorkingItem.setExtendedPrice(mItem.getExtendedPrice());
                mWorkingItem.setTax(mItem.getTax());
                mWorkingItem.setDiscount(mItem.getDiscount());
                mWorkingItem.setUpc(mItem.getUpc());
                mWorkingItem.setSku(mItem.getSku());
            }
        }

        mAmountText = view.findViewById(R.id.amountTextView);
        mNoteEdit = view.findViewById(R.id.newNotesTextField);
        mQuantityEdit = view.findViewById(R.id.quantityTextField);
        mIncrementBtn = view.findViewById(R.id.increaseButton);
        mDecrementBtn = view.findViewById(R.id.decreaseButton);
        mDiscountEdit = view.findViewById(R.id.discountTextField);
        mDiscountTypeRG = view.findViewById(R.id.radioGroup);
        TextView taxCaption = view.findViewById(R.id.detailSalesTax);
        mTaxSwitch = view.findViewById(R.id.includeTax);
        mUpcEdit = view.findViewById(R.id.upcTextField);
        mSkuEdit = view.findViewById(R.id.skuTextField);
        mDeleteBtn = view.findViewById(R.id.deleteButton);
        mSaveBtn = view.findViewById(R.id.saveButton);
        mCancelBtn = view.findViewById(R.id.cancelButton);

        mQuantityEdit.setRepresentationType(NumericEditText.RepresentationType.QUANTITY);
        mDiscountEdit.setRepresentationType(NumericEditText.RepresentationType.CURRENCY);

        mAmountText.setText(Utils.getLocalizedAmount(mWorkingItem.getAmount()));
        String description = mWorkingItem.getDescription();
        mNoteEdit.setText((description != null && !TextUtils.isEmpty(description.trim())) ? description : mWorkingItem.getDisplayLine());
        mQuantityEdit.setText(String.valueOf(mWorkingItem.getQuantity().intValue()));
        mDiscountEdit.setText(String.valueOf(mWorkingItem.getDiscount()));
        mUpcEdit.setText(mWorkingItem.getUpc());
        mSkuEdit.setText(mWorkingItem.getSku());

        mIncrementBtn.setOnClickListener(this);
        mDecrementBtn.setOnClickListener(this);
        mDeleteBtn.setOnClickListener(this);
        mSaveBtn.setOnClickListener(this);
        mCancelBtn.setOnClickListener(this);

        taxCaption.setText(getString(R.string.str_sales_tax_percent, String.valueOf(SALES_TAX.floatValue())));
        mDiscountTypeRG.check(R.id.radioButtonMoney);
        ((RadioButton) view.findViewById(R.id.radioButtonMoney)).setText(Currency.getInstance(Locale.getDefault()).getSymbol());
        mTaxSwitch.setChecked(mWorkingItem.getTax() != null && mWorkingItem.getTax().floatValue() > 0);
        mTaxSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                recalculate();
            }
        });

        mDiscountTypeRG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int viewId) {
                switch (viewId) {
                    case R.id.radioButtonMoney:
                        mDiscountEdit.setRepresentationType(NumericEditText.RepresentationType.CURRENCY);
                        break;
                    case R.id.radioButtonPercentage:
                        mDiscountEdit.setRepresentationType(NumericEditText.RepresentationType.PERCENT);
                        break;
                }
                mDiscountEdit.setText(mDiscountEdit.getValue()); // Update representation of Discount EditText field after radio button checked
                recalculate();
            }
        });
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        mDiscountEdit.addTextChangedListener(textWatcher);
        mQuantityEdit.addTextChangedListener(textWatcher);
        mUpcEdit.addTextChangedListener(textWatcher);
        mSkuEdit.addTextChangedListener(textWatcher);
    }

    @Override
    public void onStop() {
        mDiscountEdit.removeTextChangedListener(textWatcher);
        mQuantityEdit.removeTextChangedListener(textWatcher);
        mUpcEdit.removeTextChangedListener(textWatcher);
        mSkuEdit.removeTextChangedListener(textWatcher);
        super.onStop();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.deleteButton:
                delete();
                break;
            case R.id.saveButton:
                save();
                break;
            case R.id.cancelButton:
                cancel();
                break;
            case R.id.increaseButton:
                incrementQuantity();
                break;
            case R.id.decreaseButton:
                decrementQuantity();
                break;
        }
    }

    private TextWatcher getTextWatcher() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                recalculate();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
    }

    private void recalculate() {

        BigDecimal quantity = ConversionUtility.parseAmount(mQuantityEdit.getValue());

        BigDecimal itemPrice = mWorkingItem.getUnitPrice();
        BigDecimal extendedPrice = itemPrice.multiply(quantity);

        BigDecimal discountValue = ConversionUtility.parseAmount(mDiscountEdit.getValue());
        discountValue = discountValue == null ? BigDecimal.ZERO : discountValue;

        if (discountValue.floatValue() > 0) {
            switch (mDiscountEdit.getRepresentationType()) {
                case PERCENT:
                    discountValue = extendedPrice.multiply(discountValue);
                    break;
                case CURRENCY:
                default:
                    break;
            }
        }

        BigDecimal taxValue = mTaxSwitch.isChecked()
                ? itemPrice.multiply(SALES_TAX.divide(BIG_DECIMAL_HUNDRED, RoundingMode.UNNECESSARY)).multiply(quantity)
                : BigDecimal.ZERO;

        mWorkingItem.setQuantity(quantity);
        mWorkingItem.setExtendedPrice(extendedPrice);
        mWorkingItem.setTax(taxValue);
        mWorkingItem.setDiscount(discountValue);
        mWorkingItem.setAmount(extendedPrice.add(taxValue).subtract(discountValue));
        mWorkingItem.setUpc(mUpcEdit.getValue());
        mWorkingItem.setSku(mSkuEdit.getValue());
        mAmountText.setText(Utils.getLocalizedAmount(mWorkingItem.getAmount()));
    }

    private void save() {
        recalculate();

        if (mWorkingItem.getDiscount().floatValue() >= mWorkingItem.getExtendedPrice().floatValue()) {
            Toast.makeText(getActivity().getApplicationContext(), "Discount can't be greater than amount for item", Toast.LENGTH_SHORT).show();
            return;
        }

        String itemDescription = mNoteEdit.getText().toString();
        mWorkingItem.setDescription(itemDescription);
        mWorkingItem.setDisplayLine(!TextUtils.isEmpty(itemDescription) ? itemDescription : mWorkingItem.getDisplayLine() );

        if (mListener != null) {
            mListener.onSave(mWorkingItem);
        }
    }

    private void cancel() {
        if (mListener != null) {
            mListener.onCancel();
        }
    }

    private void delete() {
        if (mListener != null) {
            mListener.onDelete(mItem);
        }
    }

    private void incrementQuantity() {
        BigDecimal incrementedQuantity = mWorkingItem.getQuantity().add(BigDecimal.ONE);
        mWorkingItem.setQuantity(incrementedQuantity);
        mQuantityEdit.setText(String.valueOf(incrementedQuantity.intValue()));
        recalculate();
    }

    private void decrementQuantity() {
        BigDecimal quantity = mWorkingItem.getQuantity();
        quantity = quantity.subtract(BigDecimal.ONE);
        quantity = quantity.floatValue() < 1 ? BigDecimal.ONE : quantity;
        mWorkingItem.setQuantity(quantity);
        mQuantityEdit.setText(String.valueOf(quantity.intValue()));
        recalculate();
    }

    public interface IDetailsFragmentListener {

        void onDelete(Merchandise merchandise);

        void onSave(Merchandise newMerchandise);

        void onCancel();

    }
}

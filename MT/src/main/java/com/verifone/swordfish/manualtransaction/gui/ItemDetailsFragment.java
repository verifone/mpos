package com.verifone.swordfish.manualtransaction.gui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.verifone.commerce.entities.Merchandise;
import com.verifone.swordfish.manualtransaction.R;
import com.verifone.swordfish.manualtransaction.tools.DisplayStringRepresentation;
import com.verifone.swordfish.manualtransaction.tools.Utils;

import java.math.BigDecimal;

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
    private static final int DISCOUNT_TYPE_MONEY = 0;
    private static final int DISCOUNT_TYPE_PERCENT = 1;

    private IDetailsFragmentListener mListener;

    private TextView mAmountText;
    private EditText mNoteEdit;
    private TextView mQuantityText;
    private ImageButton mIncrementBtn;
    private ImageButton mDecrementBtn;
    private EditText mDiscountEdit;
    private RadioGroup mDiscountTypeRG;
    private TextView mTaxTitle;
    private Switch mTaxSwitch;
    private Button mDeleteBtn;
    private Button mSaveBtn;
    private Button mCancelBtn;

    private Merchandise mItem;
    private Merchandise mWorkingItem;
    private DisplayStringRepresentation mDisplayStringRepresentation;

    private int mDiscountType = DISCOUNT_TYPE_MONEY;

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
            }
        }

        mAmountText = view.findViewById(R.id.amountTextView);
        mNoteEdit = view.findViewById(R.id.newNotesTextField);
        mQuantityText = view.findViewById(R.id.quantityTextField);
        mIncrementBtn = view.findViewById(R.id.increaseButton);
        mDecrementBtn = view.findViewById(R.id.decreaseButton);
        mDiscountEdit = view.findViewById(R.id.discountTextField);
        mDiscountTypeRG = view.findViewById(R.id.radioGroup);
        mTaxTitle = view.findViewById(R.id.detailSalesTax);
        mTaxSwitch = view.findViewById(R.id.includeTax);
        mDeleteBtn = view.findViewById(R.id.deleteButton);
        mSaveBtn = view.findViewById(R.id.saveButton);
        mCancelBtn = view.findViewById(R.id.cancelButton);

        mIncrementBtn.setOnClickListener(this);
        mDecrementBtn.setOnClickListener(this);
        mDeleteBtn.setOnClickListener(this);
        mSaveBtn.setOnClickListener(this);
        mCancelBtn.setOnClickListener(this);

        mDiscountEdit.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int inType = mDiscountEdit.getInputType(); // backup the input type
                mDiscountEdit.setInputType(InputType.TYPE_NULL); // disable soft input
                mDiscountEdit.onTouchEvent(event); // call native handler
                mDiscountEdit.setInputType(inType); // restore input type
                mDiscountEdit.setFocusable(true);
//                mListener.focusOnPercentageKeyboard();
                return true;
            }
        });

        mTaxSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                recalc();
            }
        });

        mDiscountTypeRG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int viewId) {
                switch (viewId) {
                    case R.id.radioButtonMoney:
                        mDiscountType = DISCOUNT_TYPE_MONEY;
                        break;
                    case R.id.radioButtonPercentage:
                        mDiscountType = DISCOUNT_TYPE_PERCENT;
                        break;
                }
                mDisplayStringRepresentation = new DisplayStringRepresentation();
                mDiscountEdit.setTextColor(getActivity().getResources().getColor(R.color.greyish));
                mDiscountEdit.setText("");
                calcDiscount("0");
            }
        });

        mAmountText.setText(Utils.getLocalizedAmount(mWorkingItem.getAmount()));
        mNoteEdit.setText(mWorkingItem.getDescription());
        mQuantityText.setText(String.valueOf(mWorkingItem.getQuantity().intValue()));
        mDiscountTypeRG.check(R.id.radioButtonMoney);
        mDiscountEdit.setText(Utils.getLocalizedAmount(mWorkingItem.getDiscount()));

        if (mWorkingItem.getTax() != null && !mWorkingItem.getTax().equals(BigDecimal.ZERO)
                && !mWorkingItem.getTax().equals(new BigDecimal("0"))) {
            mTaxSwitch.setChecked(true);
        } else {
            mTaxSwitch.setChecked(false);
        }

        mDisplayStringRepresentation = new DisplayStringRepresentation();

        return view;
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
                incrmentQuantity();
                break;
            case R.id.decreaseButton:
                decrementQuantity();
                break;
        }
    }

    public void onNumberClick(View view) {
        String str = (String) view.getTag();
        mDiscountEdit.setTextColor(getActivity().getResources().getColor(R.color.greyish));
        attachDiscount(str);
    }

    public void attachDiscount(String newValue) {
        String valueTxt = null;
        String value = null;
        switch (mDiscountType) {
            case DISCOUNT_TYPE_MONEY:
                mDisplayStringRepresentation.attachValue(newValue, Utils.STR_DOLLAR);
                valueTxt = Utils.STR_DOLLAR + mDisplayStringRepresentation.currentString();
                value = mDisplayStringRepresentation.currentString();
                break;
            case DISCOUNT_TYPE_PERCENT:
                mDisplayStringRepresentation.attachValue(newValue, Utils.STR_PERCENT);
                valueTxt = mDisplayStringRepresentation.currentString();
                value = mDisplayStringRepresentation.currentString().replace("%", "");
                break;
        }

        mDiscountEdit.setText(valueTxt);

        calcDiscount(value);
//        CharSequence attachedValue = "";
//        internalRepresentation.attachValue(newValue, mTransactionDetailEditModel.getDiscountType());
//
//        if (redFlagged) {
//            discountTextField.setTextColor(getActivity().getResources().getColor(R.color.greyish));
//        }
//
//        if (Objects.equals(mTransactionDetailEditModel.getDiscountType(), Utils.STR_DOLLAR)
//                && internalRepresentation.currentString().length() > 0) {
//            //String currencyValue = NumberFormat.getCurrencyInstance(Locale.getDefault()).format(internalRepresentation.currentString());
//            discountTextField.setText(Utils.STR_DOLLAR + internalRepresentation.currentString());
//        } else
//            discountTextField.setText(internalRepresentation.currentString());
//        discountTextField.setSelection(discountTextField.getText().length());
    }

    private void calcDiscount(String discountText) {
//        String discountText = mDiscountEdit.getText().toString();
        if (!TextUtils.isEmpty(discountText)) {
            BigDecimal moneyDiscount = null;
            if (mDiscountType == DISCOUNT_TYPE_PERCENT) {
                BigDecimal percent = new BigDecimal(discountText);
                BigDecimal hundredPercents = new BigDecimal("100");
                moneyDiscount = mWorkingItem.getUnitPrice().multiply(percent).divide
                        (hundredPercents);
            } else {
                moneyDiscount = new BigDecimal(discountText);
            }
            if (moneyDiscount.floatValue() >= mWorkingItem.getExtendedPrice().floatValue()) {
                mWorkingItem.setDiscount(new BigDecimal("0"));
                mDiscountEdit.setTextColor(getActivity().getResources().getColor(R.color.vermillion));
            }
            mWorkingItem.setDiscount(moneyDiscount);
        }
        recalc();
    }

    private void recalc() {
        BigDecimal itemPrice = mWorkingItem.getUnitPrice();
        BigDecimal quantity = mWorkingItem.getQuantity();
        BigDecimal extendedPrice = itemPrice.multiply(quantity);
        BigDecimal discount = mWorkingItem.getDiscount();
        BigDecimal tax = new BigDecimal("0");
        if (mTaxSwitch.isChecked()) {
            BigDecimal taxPercentage = new BigDecimal("0.0875");
            tax = extendedPrice.multiply(taxPercentage);
        }

        BigDecimal amount = extendedPrice.add(tax).subtract(discount);

        mWorkingItem.setExtendedPrice(extendedPrice);
        mWorkingItem.setTax(tax);
        mWorkingItem.setAmount(amount);
    }

//    private void recalculate() {
//        //Quantity
//        try {
//            int quantity = Integer.parseInt(mQuantityText.getText().toString());
//            mWorkingItem.setQuantity(quantity);
//        } catch (NumberFormatException e) {
//            //just use old value of quantity
//        }
//
//        //Discount
//        String discountText = mDiscountEdit.getText().toString();
//        if (!TextUtils.isEmpty(discountText)) {
//            if (mDiscountType == DISCOUNT_TYPE_PERCENT) {
//                BigDecimal percent = new BigDecimal(discountText);
//                BigDecimal hundredPercents = new BigDecimal("100");
//                BigDecimal moneyDiscount = mWorkingItem.getUnitPrice().multiply(percent).divide
//                        (hundredPercents);
//                mWorkingItem.setDiscount(moneyDiscount);
//            } else {
//                BigDecimal discount = new BigDecimal(discountText);
//                mWorkingItem.setDiscount(discount);
//            }
//        }
//
//        //tax
//        if (mTaxSwitch.isChecked()) {
//            BigDecimal tax = new BigDecimal("tax");
//            mWorkingItem.setTax(tax);
//        }
//
//    }

    private void save() {
        if (mWorkingItem.getDiscount().floatValue() >=
                mWorkingItem.getExtendedPrice().floatValue()) {
            Toast.makeText(getActivity().getApplicationContext(),
                    "Discount can't be greater than amount for item", Toast.LENGTH_SHORT).show();
            return;
        }
        if (mListener != null) {
            mListener.onSave(mItem, mWorkingItem);
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

    private void incrmentQuantity() {
        BigDecimal quantity = mWorkingItem.getQuantity();
        quantity = quantity.add(new BigDecimal("1"));
        mWorkingItem.setQuantity(quantity);
        mQuantityText.setText(String.valueOf(quantity.intValue()));
        recalc();
    }

    private void decrementQuantity() {
        BigDecimal quantity = mWorkingItem.getQuantity();
        if (quantity.intValue() > 1) {
            quantity = quantity.subtract(new BigDecimal("1"));
        }
        mWorkingItem.setQuantity(quantity);
        mQuantityText.setText(String.valueOf(quantity.intValue()));
        recalc();
    }

    public interface IDetailsFragmentListener {
        void onDelete(Merchandise merchandise);

        void onSave(Merchandise originalMerchandise, Merchandise newMerchandise);

        void onCancel();
    }
}

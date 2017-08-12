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

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
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
import com.verifone.swordfish.manualtransaction.State.TransactionDetailEditModel;
import com.verifone.swordfish.manualtransaction.Tools.DisplayStringRepresentation;
import com.verifone.swordfish.manualtransaction.Tools.LocalizeCurrencyFormatter;
import com.verifone.swordfish.manualtransaction.Tools.MposLogger;
import com.verifone.swordfish.manualtransaction.Tools.Utils;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Locale;
import java.util.Objects;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link TransactionDetailEdit.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link TransactionDetailEdit#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TransactionDetailEdit extends Fragment {
    private static final String TAG = TransactionDetailEdit.class.getSimpleName();
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private OnFragmentInteractionListener mListener;
    private TextView amountTextView;
    private TextView quantityTextView;
    private EditText discountTextField;
    private EditText notesTextField;
    private RadioButton amountRadioButton;
    private RadioButton percentageRadioButton;
    private RadioGroup radioGroup;
    private ImageButton decreaseButton;
    private ImageButton increaseButton;
    private Button cancelButton;
    private Button saveButton;
    private Button deleteButton;

    private Merchandise currentItem;
    private Switch includeTax;
    private DisplayStringRepresentation internalRepresentation;
    private SharedPreferences sharedPreferences;
    TransactionDetailEditModel mTransactionDetailEditModel;
    private int quantity = 1;

    boolean tax = false;
    private boolean redFlagged = false;
    //boolean discountSelection;
    //private String discountType;
    private String initialAmount;
    private String initialQuantity;

    public TransactionDetailEdit() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment TransactionDetailEdit.
     */
    public static TransactionDetailEdit newInstance(String param1, String param2) {
        TransactionDetailEdit fragment = new TransactionDetailEdit();
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
            getArguments().getString(ARG_PARAM1);
            getArguments().getString(ARG_PARAM2);
        }
        mTransactionDetailEditModel = ViewModelProviders.of(getActivity()).get(TransactionDetailEditModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_transaction_detail_edit, container, false);

        // TODO : Shared preference to be initialized
        //Utils.getDefaults("");

        amountTextView = (TextView) root.findViewById(R.id.amountTextView);
        quantityTextView = (TextView) root.findViewById(R.id.quantityTextField);
        notesTextField = (EditText) root.findViewById(R.id.newNotesTextField);
        discountTextField = (EditText) root.findViewById(R.id.discountTextField);

        increaseButton = (ImageButton) root.findViewById(R.id.increaseButton);
        decreaseButton = (ImageButton) root.findViewById(R.id.decreaseButton);
        cancelButton = (Button) root.findViewById(R.id.cancelButton);
        saveButton = (Button) root.findViewById(R.id.saveButton);
        deleteButton = (Button) root.findViewById(R.id.deleteButton);

        radioGroup = (RadioGroup) root.findViewById(R.id.radioGroup);
        amountRadioButton = (RadioButton) root.findViewById(R.id.radioButtonMoney);
        percentageRadioButton = (RadioButton) root.findViewById(R.id.radioButtonPercentage);
        includeTax = (Switch) root.findViewById(R.id.includeTax);

        // initial pre-condition
        mTransactionDetailEditModel.setDiscountType(Utils.STR_DOLLAR);
        mTransactionDetailEditModel.setDiscountSelection(true);
        amountRadioButton.setText(LocalizeCurrencyFormatter.getInstance().getCurrencyFormat().getCurrency().getSymbol());

        notesTextField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == 0) {
                    getActivity();
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(notesTextField.getWindowToken(), 0);
                }
                return false;
            }
        });

        discountTextField.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int inType = discountTextField.getInputType(); // backup the input type
                discountTextField.setInputType(InputType.TYPE_NULL); // disable soft input
                discountTextField.onTouchEvent(event); // call native handler
                discountTextField.setInputType(inType); // restore input type
                discountTextField.setFocusable(true);
                mListener.focusOnPercentageKeyboard();
                return true;
            }
        });

        discountTextField.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    mListener.focusOnPercentageKeyboard();
                } else {
                    mListener.focusOffPercentageKeyboard();
                }
            }
        });

        increaseButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                quantity = Integer.parseInt(
                        quantityTextView.getText().toString() == null ? "1" : quantityTextView.getText().toString());
                quantity++;
                setColorFlag(quantity);
                quantityTextView.setText(Integer.toString(quantity));
                amountTextView.setText(Utils.getLocalizedAmount(currentItem.getUnitPrice().multiply(new BigDecimal(quantity))));
            }
        });

        decreaseButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                quantity = Integer.parseInt(quantityTextView.getText().toString());
                if (quantity >= 2) {
                    quantity--;
                    setColorFlag(quantity);
                    quantityTextView.setText(Integer.toString(quantity));
                    amountTextView.setText(Utils.getLocalizedAmount(currentItem.getUnitPrice().multiply(new BigDecimal(quantity))));
                }
            }
        });



        saveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                boolean addOffer = false;
                currentItem.setQuantity(quantity);
                //currentItem.setQuantity(mTransactionDetailEditModel.getQuantity());
                if (discountTextField.getText().length() > 0) {
                    String currencySymbol = LocalizeCurrencyFormatter.getInstance().getLocalCurrencySymbol();
                    String amount = discountTextField.getText().toString().replace(currencySymbol, "");
                    // salePrice = originalPrice(1-percentOff)
                    //helper unit to refactor percentage figure.
                    amount = amount.replaceAll(Utils.STR_PERCENT, ""); // Check for the '%' symbol and replace it.
                    BigDecimal discount = new BigDecimal(amount, MathContext.DECIMAL32);

                    if (percentageRadioButton.isChecked()) {
                        currentItem.setDiscount(Utils.getDiscountPercentPrice(currentItem, amount));
                        sharedPreferences.edit().putBoolean(Utils.RADIO_BUTTON_PERCENT_KEY, true).commit();
                        sharedPreferences.edit().putBoolean(Utils.RADIO_BUTTON_MONEY_KEY, false).commit();
                    } else {
                        currentItem.setDiscount(discount);
                        sharedPreferences.edit().putBoolean(Utils.RADIO_BUTTON_MONEY_KEY, true).commit();
                        sharedPreferences.edit().putBoolean(Utils.RADIO_BUTTON_PERCENT_KEY, false).commit();
                    }
                } else {
                    currentItem.setDiscount(BigDecimal.ZERO);
                }

                //if (mTransactionDetailEditModel.isTax()) {
                if (tax) {
                    BigDecimal tax = new BigDecimal("0.0875");
                    tax = tax.multiply(currentItem.getAmount());
                    currentItem.setTax(tax);
                } else {
                    currentItem.setTax(BigDecimal.ZERO);
                }

                if (notesTextField.getText().length() > 0) {
                    currentItem.setDescription(notesTextField.getText().toString());
                    currentItem.setDisplayLine(notesTextField.getText().toString());
                } else {
                    currentItem.setDescription("");
                    currentItem.setDisplayLine("");
                }

                if (currentItem.getDiscount().doubleValue() <= (currentItem.getUnitPrice().multiply(new BigDecimal(quantity)).doubleValue())) {

                    //if (currentItem.getDiscount().doubleValue() <= (currentItem.getAmount().multiply(
                    //    new BigDecimal(mTransactionDetailEditModel.getQuantity())).doubleValue())) {
                    mListener.focusOffPercentageKeyboard();
                    mListener.onSaveAddNote(currentItem);
                    resetValues();
                } else {
                    discountTextField.setTextColor(getActivity().getResources().getColor(R.color.vermillion));
                    redFlagged = true;
                    //mTransactionDetailEditModel.setRedFlagged(true);
                    Toast.makeText(getActivity().getApplicationContext(),
                            "Discount can't be greater than amount for item", Toast.LENGTH_SHORT).show();
                }
            }
        });


        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onDeleteNote();
            }
        });

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // find which radio button is selected
                RadioButton button = (RadioButton) getActivity().findViewById(group.getCheckedRadioButtonId());

                boolean checked = button.isChecked();
                // Check which radio button was clicked
                switch (checkedId) {
                    case R.id.radioButtonMoney:
                        if (checked)
                            MposLogger.getInstance().debug("TDA: ", "Amount selected");
                        mTransactionDetailEditModel.setDiscountType(Utils.STR_DOLLAR);
                        internalRepresentation.attachValue("", mTransactionDetailEditModel.getDiscountType());
                        // Note - Without saving the change value, preference should not be set.
                        //sharedPreferences.edit().putBoolean(Utils.RADIO_BUTTON_MONEY_KEY, true).apply();
                        break;
                    case R.id.radioButtonPercentage:
                        if (checked)
                            MposLogger.getInstance().debug("TDA: ", "Discount selected");
                        mTransactionDetailEditModel.setDiscountType(Utils.STR_PERCENT);
                        internalRepresentation.attachValue("", mTransactionDetailEditModel.getDiscountType());
                        // Note - Without saving the change value, preference should not be set.
                        //sharedPreferences.edit().putBoolean(Utils.RADIO_BUTTON_PERCENT_KEY, true).apply();
                        break;
                    default:
                        break;
                }
                discountTextField.setText("");
            }
        });

        includeTax.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {
                    MposLogger.getInstance().debug("TDA", "tax selected");
                    tax = true;
                    //mTransactionDetailEditModel.setTax(true);
                } else {
                    MposLogger.getInstance().debug("TDA: ", "no tax selected");
                    tax = false;
                    //mTransactionDetailEditModel.setTax(false);
                }
            }
        });

        if (currentItem != null) {
            if (currentItem.getTax() != null
                    && !Objects.equals(currentItem.getTax(), BigDecimal.ZERO)
                    && !currentItem.getTax().toString().equals("0")) {
                includeTax.setSelected(true);
                includeTax.setChecked(true);
                tax = true;
                //mTransactionDetailEditModel.setTax(true);
            } else {
                includeTax.setChecked(false);
                tax = false;
                //mTransactionDetailEditModel.setTax(false);
            }
        }

        // Initial load as per Merchandise
        if (currentItem != null) {
            amountTextView.setText(Utils.getLocalizedAmount(currentItem.getAmount()));
            initialQuantity = Integer.toString(currentItem.getQuantity());
            //mTransactionDetailEditModel.setInitialQuantity(Integer.toString(currentItem.getQuantity()));
            quantity = currentItem.getQuantity();
            setColorFlag(quantity);

            if (currentItem.getDescription() != null && !currentItem.getDescription().equals(" ")) {
                notesTextField.setText(currentItem.getDescription());
            }

            if (currentItem.getTax() != null) {
                includeTax.setSelected(true);
            }

            if (currentItem.getQuantity() != 1) {
                quantityTextView.setText(Integer.toString(currentItem.getQuantity()));
            }
        }

        if (quantityTextView != null && initialQuantity != null) {
            quantityTextView.setText(initialQuantity);

            //if (quantityTextView != null && mTransactionDetailEditModel.getInitialQuantity() != null) {
            //quantityTextView.setText(mTransactionDetailEditModel.getInitialQuantity());
        }

        setBackAction(cancelButton);
        internalRepresentation = new DisplayStringRepresentation();
        TextView taxRate = (TextView) root.findViewById(R.id.detailSalesTax);
        String printTaxRate = String.format(Locale.getDefault(), "%.2f", 8.75);//item.getTaxRate());
        taxRate.setText(getContext().getString(R.string.str_sales_tax, printTaxRate));
        return root;
    }

    private void setColorFlag(int quantity) {
        if (quantity >= 2) {
            decreaseButton.setImageResource(R.drawable.icon_qty_down_on);
        } else {
            decreaseButton.setImageResource(R.drawable.icon_qty_down_off);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener.focusOffPercentageKeyboard();
        mListener = null;
    }

    private void loadSavedRadioPreferences() {
        sharedPreferences = getActivity().getPreferences(Context.MODE_PRIVATE);
        amountRadioButton.setChecked(sharedPreferences.getBoolean(Utils.RADIO_BUTTON_MONEY_KEY, false));
        percentageRadioButton.setChecked(sharedPreferences.getBoolean(Utils.RADIO_BUTTON_PERCENT_KEY, false));
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        //RadioButton amountRB = (RadioButton) radioGroup.findViewById(R.id.radioButtonMoney);
        //RadioButton percentageRB = (RadioButton) radioGroup.findViewById(R.id.radioButtonPercentage);
        loadSavedRadioPreferences();
        String amount;
        if (currentItem.getDiscount() != null && !Objects.equals(currentItem.getDiscount(), BigDecimal.ZERO)) {
            // logic to check percent vs amt :
            if (percentageRadioButton.isChecked()) {
                /*amount = getLocalizedAmount(currentItem.getDiscount());
                BigDecimal discount = new BigDecimal(amount, MathContext.DECIMAL64);
                discount = discount.multiply(new BigDecimal("100"));
                BigDecimal qty = new BigDecimal(currentItem.getQuantity());
                BigDecimal subtotal = currentItem.getAmount().multiply(qty).multiply(discount);*/
                currentItem.setDiscount(Utils.getDiscountValueFromSalePrice(currentItem));
                amount = currentItem.getDiscount().toString() + "%";
            } else {
                amount = Utils.getLocalizedAmount(currentItem.getDiscount());
            }

            discountTextField.setText(amount);
            //loadSavedRadioPreferences();
            /*amountRB.setSelected(true);
            radioGroup.check(R.id.radioButtonMoney);
            percentageRB.setSelected(false);*/
            mTransactionDetailEditModel.setDiscountSelection(true);
        }
        if (currentItem.getTax() != null && !Objects.equals(currentItem.getTax(), BigDecimal.ZERO)) {
            //includeTax.setChecked(true);
            includeTax.setSelected(true);
        }
    }


    private void setBackAction(Button button) {
        if (button != null) {
            button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    mListener.onCancelAddNote();
                }
            });
        }
    }

    private void resetValues() {
        amountTextView.setText("");
        quantityTextView.setText("");
        discountTextField.setText("");
        discountTextField.setTextColor(getActivity().getResources().getColor(R.color.greyish));
        notesTextField.setText("");
        if (amountRadioButton != null) {
            amountRadioButton.setSelected(true);
        }
        if (percentageRadioButton != null) {
            percentageRadioButton.setSelected(false);
        }
        includeTax.setSelected(false);
        mTransactionDetailEditModel.setDiscountSelection(true);
        quantity = 1;

    }

    //Method for initializer
    public void setInitialAmount(String amount) {
        String newAmount;

        if (currentItem != null) {
            newAmount = Utils.getLocalizedAmount(currentItem.getAmount()); //String.format("%.2f", currentItem.getItemTotal());
        } else {
            newAmount = amount;
        }
        if (amountTextView != null) {
            amountTextView.setText(newAmount);
        } else {
            initialAmount = newAmount;
            // mTransactionDetailEditModel.setInitialAmount(newAmount);
        }
    }

    public void setInitialQuantity(String strQuantity) {
        String newQuantity;
        if (currentItem != null) {
            newQuantity = String.format(Locale.getDefault(), "%d", currentItem.getQuantity());
        } else {
            newQuantity = strQuantity;
        }
        if (quantityTextView != null) {
            quantityTextView.setText(newQuantity);
        } else {
            initialQuantity = newQuantity;
            //mTransactionDetailEditModel.setInitialQuantity(newQuantity);
        }

        quantity = Integer.parseInt(strQuantity);
        mListener.focusOnPercentageKeyboard();
    }

    public void setInitialNotes(String notes) {
        if (notesTextField != null) {
            notesTextField.setText(notes);
        }
    }

    //Methods for saving and returning values
    public String getDiscount() {
        if (discountTextField.getText().length() > 0) {
            float discount = Float.parseFloat(discountTextField.getText().toString());

            MposLogger.getInstance().debug("TDA: discount: ",
                        String.format(Locale.getDefault(), "%.2f %b", discount, mTransactionDetailEditModel.isDiscountSelection()));
            if (mTransactionDetailEditModel.isDiscountSelection()) {
                return discountTextField.getText().toString();
            } else {
                double total = currentItem.getUnitPrice().doubleValue();
                double discounts = (total * quantity) * (discount / 100);
                return String.format(Locale.getDefault(), "%.2f", discounts);
            }
        } else {
            return "0";
        }
    }

    public double getDiscountAsFloat() {
        if (discountTextField.getText().length() > 0) {
            float discount = Float.parseFloat(discountTextField.getText().toString());

            MposLogger.getInstance().debug("TDA: discount: ",
                      String.format(Locale.getDefault(), "%.2f %b", discount, mTransactionDetailEditModel.isDiscountSelection()));
            if (mTransactionDetailEditModel.isDiscountSelection()) {
                return discount;
            } else {
                double total = currentItem.getAmount().doubleValue();
                return (total * quantity) * (discount / 100);
            }
        } else {
            return 0.0f;
        }

    }

    private BigDecimal getDiscountAsBigDecimal() {
        BigDecimal discounts;
        if (discountTextField.getText().length() > 0) {
            double discount = Double.parseDouble(discountTextField.getText().toString());
            discounts = new BigDecimal(discount);


            MposLogger.getInstance().debug("TDA: discount: ",
                    String.format(Locale.getDefault(), "%.2f %b", discount, mTransactionDetailEditModel.isDiscountSelection()));
            if (mTransactionDetailEditModel.isDiscountSelection()) {
                return discounts;
            } else {
                double total = currentItem.getUnitPrice().doubleValue();
                discounts = new BigDecimal((total * quantity) * (discount / 100));
                return discounts;
            }
        } else {
            return new BigDecimal(0.0f);
        }
    }

    public void attachDiscount(String newValue) {
        CharSequence attachedValue = "";
        internalRepresentation.attachValue(newValue, mTransactionDetailEditModel.getDiscountType());

        if (redFlagged) {
            discountTextField.setTextColor(getActivity().getResources().getColor(R.color.greyish));
        }

        if (Objects.equals(mTransactionDetailEditModel.getDiscountType(), Utils.STR_DOLLAR)
                && internalRepresentation.currentString().length() > 0) {
            //String currencyValue = NumberFormat.getCurrencyInstance(Locale.getDefault()).format(internalRepresentation.currentString());
            discountTextField.setText(Utils.STR_DOLLAR + internalRepresentation.currentString());
        } else
            discountTextField.setText(internalRepresentation.currentString());
        discountTextField.setSelection(discountTextField.getText().length());
    }


    public String getQuantity() {
        return Integer.toString(quantity);
    }

    public boolean getTax() {
        return tax;
    }

    public String notesAdded() {
        return notesTextField.getText().toString();
    }

    public void setCurrentItem(Merchandise newItem) {
        currentItem = newItem;
    }

    public OnFragmentInteractionListener getmListener() {
        return mListener;
    }

    public void setmListener(OnFragmentInteractionListener listener) {
        mListener = listener;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnFragmentInteractionListener {
        void onCancelAddNote();

        void onSaveAddNote(Merchandise merchandise);

        void onDeleteNote();

        void focusOnPercentageKeyboard();

        void focusOffPercentageKeyboard();
    }
}

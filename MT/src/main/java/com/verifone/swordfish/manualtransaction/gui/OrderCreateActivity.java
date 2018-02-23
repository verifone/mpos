package com.verifone.swordfish.manualtransaction.gui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import com.verifone.commerce.entities.Merchandise;
import com.verifone.swordfish.manualtransaction.CarbonBridge;
import com.verifone.swordfish.manualtransaction.IBridgeListener;
import com.verifone.swordfish.manualtransaction.ManualTransactionApplication;
import com.verifone.swordfish.manualtransaction.R;
import com.verifone.swordfish.manualtransaction.tools.DisplayStringRepresentation;
import com.verifone.utilities.Log;

import java.math.BigDecimal;
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

public class OrderCreateActivity extends BaseActivity implements View.OnClickListener,
        IBridgeListener, OrderListFragment.IOrderListFragmentListener, ItemDetailsFragment.IDetailsFragmentListener {

    private static final String TAG = OrderCreateActivity.class.getSimpleName();

    private Button mPayBtn;
    private Button mCancelTransactionBtn;
    private ImageButton mAddBtn;
    private ImageButton mIncrementBtn;
    private ImageButton mDecrementBtn;
    private EditText mQuantityTextView;

    private OrderListFragment mOrderListFragment;
    private DisplayStringRepresentation mDisplayStringRepresentation;

    private static int itemID = 0;
    private int quantity = 1;

    private ItemDetailsFragment mItemDetailsFragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_create);

        findViewById(R.id.btn_middle).setVisibility(View.INVISIBLE);

        mPayBtn = findViewById(R.id.btn_right);
        mCancelTransactionBtn = findViewById(R.id.btn_left);
        mAddBtn = findViewById(R.id.btn_add);
        mIncrementBtn = findViewById(R.id.btn_increment);
        mDecrementBtn = findViewById(R.id.btn_decrement);
        mQuantityTextView = findViewById(R.id.quantityField);

        mPayBtn.setText(R.string.str_pay);
        mCancelTransactionBtn.setText(R.string.cancel_transaction_btn);

        mPayBtn.setOnClickListener(this);
        mPayBtn.setEnabled(false);
        mCancelTransactionBtn.setOnClickListener(this);
        mAddBtn.setOnClickListener(this);
        mIncrementBtn.setOnClickListener(this);
        mDecrementBtn.setOnClickListener(this);

        mDisplayStringRepresentation = new DisplayStringRepresentation();

        mOrderListFragment = OrderListFragment.getInstance(true);
        getSupportFragmentManager().beginTransaction().add(R.id.container, mOrderListFragment).commit();

        ManualTransactionApplication.getCarbonBridge().setListener(this);
        startTerminalSession();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        CarbonBridge carbonBridge = ManualTransactionApplication.getCarbonBridge();
        carbonBridge.setListener(this);

        if (requestCode == StartActivity.GENERAL_REQUEST_CODE) {
            switch (resultCode) {
                case RESULT_OK:
                    showDialogWithMessage(getString(R.string.title_finishing_order), false);
                    ManualTransactionApplication.getTransactionStorage().saveTransaction(carbonBridge.getTransaction());
                    carbonBridge.stopSession();
                    break;
                case StartActivity.RESULT_TRANSACTION_CANCELED:
                    setResult(StartActivity.RESULT_TRANSACTION_CANCELED);
                    finish();
                    break;
            }
        }
    }

    @Override
    public void onBackPressed() {
        showDialogWithMessage(getString(R.string.title_ending_session), false);
        ManualTransactionApplication.getCarbonBridge().stopSession();
    }

    private void startTerminalSession() {
        showDialogWithMessage(getString(R.string.starting_transaction), false);
        ManualTransactionApplication.getCarbonBridge().startPaymentSession();
    }

    /**
     * Method which handle numeric keyboard clicks
     */
    public void onNumberClick(View view) {
        //If item details fragment is open - send keyboard actions there
        if (mItemDetailsFragment != null) {
            mItemDetailsFragment.onNumberClick(view);
        } else {
            mDisplayStringRepresentation.attachValue((String) view.getTag(), null);
            mOrderListFragment.setDisplayText(mDisplayStringRepresentation.currentString());
        }
    }

    private void increment() {
        quantity++;
        mQuantityTextView.setText(String.valueOf(quantity));
    }

    private void decrement() {
        if (quantity > 1) quantity--;
        mQuantityTextView.setText(String.valueOf(quantity));
    }

    private void addItem() {
        BigDecimal itemPrice = null;
        String priceStr = mDisplayStringRepresentation.currentString();//priceBuilder.toString();
        if (priceStr == null) return;
        try {
            itemPrice = new BigDecimal(priceStr);
        } catch (NumberFormatException e) {
            return;
        }
//        BigDecimal price = mOrderListFragment.getPrice();
        if (itemPrice.floatValue() == 0) return;
        //TODO add to basket

        BigDecimal extendedPrice = itemPrice.multiply(new BigDecimal(quantity));

        Merchandise merchandise = new Merchandise();
        merchandise.setBasketItemId(String.valueOf(++itemID));
        merchandise.setName(String.valueOf(itemID));
        // This should be different from the name.
        merchandise.setDescription(" ");
        merchandise.setDisplayLine("Display name " + itemID);
        if (quantity != 0)
            merchandise.setQuantity(quantity);
        merchandise.setAmount(itemPrice.multiply(new BigDecimal(quantity)));
        merchandise.setUnitPrice(itemPrice);
        merchandise.setExtendedPrice(extendedPrice);
        merchandise.setTax(BigDecimal.ZERO);
        merchandise.setDiscount(BigDecimal.ZERO);

        ManualTransactionApplication.getCarbonBridge().addMerchandise(merchandise);

        quantity = 1;
        mQuantityTextView.setText(String.valueOf(quantity));
        mDisplayStringRepresentation = new DisplayStringRepresentation();
        mOrderListFragment.setDisplayText(null);

    }

    private void updateBasket() {
        mOrderListFragment.updateBasket();

        List<Merchandise> merchandises = ManualTransactionApplication.getCarbonBridge().getMerchandises();
        if (merchandises != null && !merchandises.isEmpty()) {
            mPayBtn.setEnabled(true);
        }
    }

    private void cancelTransaction() {
        showDialogWithMessage(getString(R.string.title_ending_session), false);
        ManualTransactionApplication.getCarbonBridge().cancelTransaction();
        ManualTransactionApplication.getCarbonBridge().stopSession();
    }

    private void pay() {
        showDialogWithMessage(getString(R.string.basket_adjustment), true);
        ManualTransactionApplication.getCarbonBridge().finalizeBasket();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_right:
                pay();
                break;
            case R.id.btn_left:
                cancelTransaction();
                break;
            case R.id.btn_increment:
                increment();
                break;
            case R.id.btn_decrement:
                decrement();
                break;
            case R.id.btn_add:
                addItem();
                break;
        }
    }

    /**
     * Order list fragment callback
     */
    @Override
    public void onItemClicked(Merchandise merchandise) {
        mItemDetailsFragment = ItemDetailsFragment.getInstance(merchandise);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, mItemDetailsFragment)
                .commit();
    }

    /**
     * Item detail view fragment callbacks
     */
    @Override
    public void onDelete(Merchandise merchandise) {
        mItemDetailsFragment = null;
    }

    @Override
    public void onSave(Merchandise originalMerchandise, Merchandise newMerchandise) {
        getSupportFragmentManager().beginTransaction().replace(R.id.container,
                mOrderListFragment).commit();
        mItemDetailsFragment = null;
        ManualTransactionApplication.getCarbonBridge().updateMerchandise(newMerchandise);
    }

    @Override
    public void onCancel() {
        getSupportFragmentManager().beginTransaction().replace(R.id.container,
                mOrderListFragment).commit();
        mItemDetailsFragment = null;
    }

    /**
     * Carbon bridge listener
     */

    @Override
    public void sessionStarted() {
        Log.d(TAG, "sessionStarted()");
        hideDialog();
    }

    @Override
    public void sessionStopped() {
        Log.d(TAG, "sessionStopped()");
        hideDialog();
        finish();
    }

    @Override
    public void merchandiseAdded() {
        updateBasket();
    }

    @Override
    public void merchandiseUpdated() {
        if (mOrderListFragment.isAdded()) {
            updateBasket();
        }
    }

    @Override
    public void merchandiseDeleted() {
        updateBasket();
    }

    @Override
    public void basketFinalized() {
        hideDialog();
        Intent paymentIntent = new Intent(this, PaymentActivity.class);
        startActivityForResult(paymentIntent, StartActivity.GENERAL_REQUEST_CODE);
    }

    @Override
    public void onTransactionCanceled() {
        showDialogWithMessage(getString(R.string.title_closing_session), false);
        ManualTransactionApplication.getCarbonBridge().stopSession();
    }

}

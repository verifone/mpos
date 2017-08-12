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

package com.verifone.swordfish.manualtransaction.TransactionFrames;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.verifone.commerce.entities.Merchandise;
import com.verifone.commerce.entities.Offer;
import com.verifone.swordfish.manualtransaction.KeyboardEntryFocus;
import com.verifone.swordfish.manualtransaction.MTDataModel.MTTransaction;
import com.verifone.swordfish.manualtransaction.R;
import com.verifone.swordfish.manualtransaction.SupportFragments.ButtonsFragment;
import com.verifone.swordfish.manualtransaction.SupportFragments.NumericKeyboard;
import com.verifone.swordfish.manualtransaction.SupportFragments.OperationsKeyboard;
import com.verifone.swordfish.manualtransaction.SupportFragments.TransactionDetailEdit;
import com.verifone.swordfish.manualtransaction.SupportFragments.TransactionList;
import com.verifone.swordfish.manualtransaction.System.PaymentTerminal;
import com.verifone.swordfish.manualtransaction.Tools.DisplayStringRepresentation;
import com.verifone.swordfish.manualtransaction.Tools.LocalizeCurrencyFormatter;
import com.verifone.swordfish.manualtransaction.Tools.MposLogger;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;


public class TransactionEntry extends Fragment implements
        ButtonsFragment.OnFragmentInteractionListener,
        NumericKeyboard.OnFragmentInteractionListener,
        OperationsKeyboard.OnFragmentInteractionListener,
        TransactionList.OnFragmentInteractionListener,
        TransactionDetailEdit.OnFragmentInteractionListener {

    private static String TAG = TransactionEntry.class.getSimpleName();
    private ButtonsFragment buttonsFragment;
    private NumericKeyboard numericKeyboard;
    private OperationsKeyboard operationsKeyboard;
    private TransactionList transactionList;
    private TransactionDetailEdit transactionDetailEdit;
    private DisplayStringRepresentation internalRepresentation;
    private PaymentTerminal paymentTerminal;
    private PaymentTerminal.PaymentTerminalItemsEvents paymentTerminalItemsEvents;
    private KeyboardEntryFocus keyboadEntryFocus;
    private MTTransaction currentTransaction;
    private TransactionEntry.TransactionEntryAPI mListener;
    private Merchandise currentItem;
    private Set<Merchandise> entryItemsSet;
    private Merchandise[] merchandises;

    private boolean configureButtons = false;
    private boolean itemFocus = true;
    private boolean isOnEdit = false;
    private static int itemID;
    private boolean firstItem;
    private static BigDecimal transactionTotal;


    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View returnView = inflater.inflate(R.layout.mt_fragment_transaction_entry, container, false);

        transactionList = new TransactionList();
        transactionList.setListener(this);
        transactionList.setTransaction(currentTransaction);
        numericKeyboard = new NumericKeyboard();
        operationsKeyboard = new OperationsKeyboard();
        buttonsFragment = new ButtonsFragment();
        configureButtons = true;
        configurePaymentTerminal();

        entryItemsSet = new LinkedHashSet<>();
        if (currentTransaction.transactionMerchandises() != null) {
            merchandises = currentTransaction.getConvertedMerchandise(currentTransaction);
            transactionList.setTransaction(currentTransaction);
        } else {
            merchandises = PaymentTerminal.getInstance().getMerchandizes();
            itemID = 0;
            firstItem = true;
            currentItem = null;
            transactionTotal = new BigDecimal(getContext().getString(R.string.str_zero_value));
        }

        keyboadEntryFocus = KeyboardEntryFocus.transactionList;
        FragmentManager manager = getActivity().getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.add(R.id.itemList, transactionList)
                .add(R.id.numericKeyboard, numericKeyboard)
                .add(R.id.opKeyboard, operationsKeyboard)
                .add(R.id.buttonsFrame, buttonsFragment)
                .commit();
        return returnView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (merchandises != null) {
            Collections.addAll(entryItemsSet, merchandises);
            readyToConfigureButtons();
            buttonsFragment.onConfigureButton(0, true, getActivity().getString(R.string.buttonCancelTx));
            configureButtons = true;
            MposLogger.getInstance().debug(TAG, "items: " + Integer.toString(merchandises.length));
        }
        keyboadEntryFocus = KeyboardEntryFocus.transactionList;
        if (configureButtons) {
            buttonsFragment.setListener(this);
            numericKeyboard.setListener(this);
            operationsKeyboard.setListener(this);
            itemFocus = true;
        }
    }

    /*@Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause TransactionEntry!!!");

    }*/

    @Override
    public void onButtonPress(int buttonID) {
        MposLogger.getInstance().debug(TAG, " button selected: " + Integer.toString(buttonID));
        switch (buttonID) {
            case 0:
                if (mListener != null)
                    mListener.transactionCancel();
                break;
            case 1:
                //Not used here
                break;
            case 2:
                if (mListener != null) {
                    for (Merchandise merchandise : entryItemsSet) {
                        transactionTotal = transactionTotal.add(merchandise.getAmount());
                    }
                    mListener.payTransaction(entryItemsSet, transactionTotal);
                }
            default:
                break;
        }
    }

    @Override
    public void readyToConfigureButtons() {
        boolean buttonZero = false;
        if (entryItemsSet.size() > 0) {
            buttonZero = true;
        }
        buttonsFragment.onConfigureButton(0, buttonZero, getActivity().getString(R.string.buttonCancelTx));
        buttonsFragment.onConfigureButton(1, false, null);
        buttonsFragment.onConfigureButton(2, buttonZero, getActivity().getString(R.string.buttonPay));
        configureButtons = false;
        internalRepresentation = new DisplayStringRepresentation();

    }

    public void setListener(TransactionEntryAPI listener) {
        mListener = listener;
    }

    public void setTransaction(MTTransaction transaction) {
        currentTransaction = transaction;
    }

    //Numeric keyboard listener
    @Override
    public void onKeyboardButtonPress(String title) {

        switch (keyboadEntryFocus) {
            case transactionList:
                internalRepresentation.attachValue(title, null);
                transactionList.setDisplayText(internalRepresentation.currentString());
                break;
            case operationsQuantity:
                operationsKeyboard.attachValue(title);
                break;
            case transactionEdit:
                transactionDetailEdit.attachDiscount(title);
                break;
            default:
                break;
        }
    }

    //Operations keyboard listener
    @Override
    public void onAddButtonPress() {

        if (!isOnEdit) {
            boolean valid = true;
            if (transactionList.getDisplayData().equals(getActivity().getResources().getString(R.string.addItem))
                    || transactionList.getDisplayData().length() == 0) {
                valid = false;
                Log.e(TAG, "Data from Transaction List is not valid.");
            }
            if (valid) {
                buttonsFragment.onConfigureButton(0, true, getActivity().getString(R.string.buttonCancelTx));
                buttonsFragment.onConfigureButton(2, true, getActivity().getString(R.string.buttonPay));

                Merchandise merchandise = new Merchandise();
                merchandise.setBasketItemId(String.valueOf(++itemID));
                merchandise.setName(String.valueOf(itemID));
                // This should be different from the name.
                BigDecimal itemPrice = new BigDecimal(transactionList.getDisplayData());
                int quantity = operationsKeyboard.getQuantity();
                merchandise.setDescription(" ");
                merchandise.setDisplayLine(" ");

                if (quantity != 0)
                    merchandise.setQuantity(quantity);
                merchandise.setAmount(itemPrice.multiply(new BigDecimal(quantity)));
                merchandise.setUnitPrice(itemPrice);
                merchandise.setExtendedPrice(itemPrice);
                merchandise.setTax(BigDecimal.ZERO);
                merchandise.setDiscount(BigDecimal.ZERO);
                currentItem = merchandise;
                paymentTerminal.addItem(merchandise, currentTransaction.runningTotals());
                keyboadEntryFocus = KeyboardEntryFocus.transactionList;
            }
        }
    }

    @Override
    public void onTimesButtonPress(String title) {

    }

    @Override
    public void focusOnQuantityOpKeyboard() {
        keyboadEntryFocus = KeyboardEntryFocus.operationsQuantity;
        itemFocus = false;
    }

    @Override
    public void requestFocus() {
        keyboadEntryFocus = KeyboardEntryFocus.transactionList;
        itemFocus = true;
    }

    @Override
    public void onAddNoteDetail(Merchandise merchandise) {
        keyboadEntryFocus = KeyboardEntryFocus.transactionEdit;
        currentItem = merchandise;
        transactionDetailEdit = new TransactionDetailEdit();
        transactionDetailEdit.setCurrentItem(currentItem);
        transactionDetailEdit.setmListener(this);
        FragmentManager manager = getActivity().getSupportFragmentManager();
        final FragmentTransaction transaction = manager.beginTransaction();
        transaction.add(R.id.itemList, transactionDetailEdit)
                .commit();
        isOnEdit = false;

    }

    private void configurePaymentTerminal() {
        Log.d(TAG, "configurePaymentTerminal");
        paymentTerminal = PaymentTerminal.getInstance();
        paymentTerminalItemsEvents = new PaymentTerminal.PaymentTerminalItemsEvents() {
            @Override
            public void basketStarted() {
            }

            @Override
            public void basketReady() {
            }

            @Override
            public void onLineItemsAdded() {
                LocalizeCurrencyFormatter formatter = LocalizeCurrencyFormatter.getInstance();
                String amount = formatter.getCurrencyFormat().format(currentItem.getAmount());
                //Accessibility.getInstance().convertTextToSpeach("item add for: " + amount);

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!entryItemsSet.contains(currentItem)) {
                            currentTransaction.addMerchandise(currentItem);
                            transactionList.addItem(currentItem);
                            internalRepresentation = new DisplayStringRepresentation();
                            entryItemsSet.add(currentItem);
                            operationsKeyboard.reset();
                        }
                    }
                });
            }

            @Override
            public void onLineItemsAddError() {
                MposLogger.getInstance().error(TAG, " error adding line item");
            }

            @Override
            public void onLineItemUpdated() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        transactionList.updateItem(currentItem);
                    }
                });

            }

            @Override
            public void onLineItemsDeleted() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        currentTransaction.removeMerchandise(currentItem);
                        entryItemsSet.remove(currentItem);
                        transactionList.removeItem(currentItem);
                    }
                });
            }

            @Override
            public void onLineItemsDeletedFail() {
                MposLogger.getInstance().error(TAG, " error deleting line item");
            }

            @Override
            public void onFailure() {
                MposLogger.getInstance().error(TAG, " failure line item");
            }

            @Override
            public void onTimeOut() {
                MposLogger.getInstance().error(TAG, " timeout line item");
            }
        };
        paymentTerminal.setItemsListener(paymentTerminalItemsEvents);
    }

    @Override
    public void onCancelAddNote() {
        removeDetailEditFragment();
        isOnEdit = false;
    }

    @Override
    public void onSaveAddNote(Merchandise merchandise) {
        if (merchandise.getDiscount() != null) {
            Offer offer = new Offer();
            offer.setOfferDescription("Discount");
            offer.setReferenceBasketLineItemId(merchandise.getBasketItemId());
            offer.setOfferDiscount(merchandise.getDiscount());
            paymentTerminal.addOffer(offer);
        }
        currentItem = merchandise;
        // update Merchandise instance while updating item price/discount/tax.
        currentTransaction.updateMerchandise(currentItem);
        paymentTerminal.updateItem(currentItem, currentTransaction.runningTotals());
        removeDetailEditFragment();
        isOnEdit = false;

    }

    @Override
    public void onDeleteNote() {
        paymentTerminal.deleteItem(currentItem, currentTransaction.runningTotals());
        removeDetailEditFragment();
    }

    @Override
    public void focusOnPercentageKeyboard() {
        keyboadEntryFocus = KeyboardEntryFocus.transactionEdit;
    }

    @Override
    public void focusOffPercentageKeyboard() {
        keyboadEntryFocus = KeyboardEntryFocus.transactionList;
    }

    private void removeDetailEditFragment() {
        FragmentManager manager = getActivity().getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.remove(transactionDetailEdit).commit();
    }

    public interface TransactionEntryAPI {
        void transactionCancel();

        void payTransaction(Set<Merchandise> items, BigDecimal transactionTotal);
    }
}

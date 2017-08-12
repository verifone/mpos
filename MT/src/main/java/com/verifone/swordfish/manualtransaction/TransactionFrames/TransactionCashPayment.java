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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.verifone.peripherals.CashDrawer;
import com.verifone.peripherals.Peripheral;
import com.verifone.peripherals.PeripheralManager;
import com.verifone.peripherals.PeripheralStatusListener;
import com.verifone.swordfish.manualtransaction.MTDataModel.MTTransaction;
import com.verifone.swordfish.manualtransaction.R;
import com.verifone.swordfish.manualtransaction.SupportFragments.CashCompanion;
import com.verifone.swordfish.manualtransaction.SupportFragments.CashDetail;
import com.verifone.swordfish.manualtransaction.SupportFragments.TransactionComplete;
import com.verifone.swordfish.manualtransaction.Tools.MposLogger;

import java.math.BigDecimal;
import java.util.HashMap;



/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link TransactionCashPayment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link TransactionCashPayment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TransactionCashPayment extends Fragment implements
        CashDetail.OnFragmentInteractionListener,
        CashCompanion.OnFragmentInteractionListener,
        TransactionComplete.OnFragmentInteractionListener, PeripheralStatusListener {

    private static final String TAG = TransactionCashPayment.class.getSimpleName();
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;
    private BigDecimal transactionTotal;
    private CashDetail cashDetail;
    private CashCompanion cashCompanion;
    private MTTransaction currentTransaction;
    private CashDrawer drawer;
    private PeripheralManager mPeripheralManager;
    private TransactionComplete transactionComplete;


    public TransactionCashPayment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment TransactionCashPayment.
     */
    public static TransactionCashPayment newInstance(String param1, String param2) {
        TransactionCashPayment fragment = new TransactionCashPayment();
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

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_transaction_cash_payment, container, false);
        cashDetail = new CashDetail();
        cashDetail.setOnFragmentInteractionListener(this);
        cashDetail.setTotal(currentTransaction.getTransactionTotal().floatValue());
        cashDetail.setCurrentTransaction(currentTransaction);

        cashCompanion = new CashCompanion();
        cashCompanion.setListener(this);
        cashCompanion.setTransaction(currentTransaction);
        FragmentManager manager = getActivity().getSupportFragmentManager();
        final FragmentTransaction transaction = manager.beginTransaction();
        transaction.add(R.id.cashDetail, cashDetail)
                .add(R.id.cashCompanion, cashCompanion)
                .commit();
        mPeripheralManager = PeripheralManager.getManager(getActivity(), null);

        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onResume() {
        super.onResume();
        mPeripheralManager.addListener(this, CashDrawer.PERIPHERAL_TYPE);

    }

    @Override
    public void onPause() {
        super.onPause();
        mPeripheralManager.removeListener(this, CashDrawer.PERIPHERAL_TYPE);

    }
    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    @Override
    public void onButtonPress(int buttonID) {
        switch (buttonID) {
            case 0:
                mListener.cashCancelTransaction();
                break;
            case 1:
                mListener.cashGoBack();
                break;
            case 2: {
                if (cashDetail.isSettle()) {
                    cashDetail.addPayment();
                    transactionComplete = new TransactionComplete();
                    transactionComplete.setListener(this);
                    transactionComplete.setTransaction(currentTransaction);
                    FragmentManager manager = getActivity().getSupportFragmentManager();
                    final FragmentTransaction transaction = manager.beginTransaction();
                    transaction.remove(cashCompanion)
                            .add(R.id.cashCompanion, transactionComplete)
                            .commit();
                    openDrawer();
                }
            }
            break;
            default:
                break;
        }
    }


    public void setListener(TransactionCashPayment.OnFragmentInteractionListener listener) {mListener = listener; }
    public void setTotal(BigDecimal total) { transactionTotal = total; }
    public void setTransaction(MTTransaction transaction) { currentTransaction = transaction; }


    @Override
    public void onKeyboardButtonPress(String title) {
        cashDetail.attachValue(title);
        checkIsSettle();
    }

    @Override
    public void onButtonPress(View view) {
        int amount;
        if (Integer.parseInt((String) view.getTag()) > -1) {
            amount = Integer.parseInt((String) view.getTag());
         }
        else {
            amount = view.getId();
        }
        double pay = 0;
        pay = pay + amount;
        cashDetail.pay(pay);
        checkIsSettle();
    }

    private void checkIsSettle() {
        if (cashDetail.isSettle()) {
            cashCompanion.onConfigureButton(2, true, getActivity().getString(R.string.tender_button_label));
        }
    }

    @Override
    public void onCashDetail() {

    }

    @Override
    public void transactionComplete() {
        mListener.cashSettle();
    }

    @Override
    public void fragmentReady() {
        transactionComplete.setButtons(true, true);
    }

    private void openDrawer() {
        drawer = (CashDrawer) mPeripheralManager.getPeripheralsByType(CashDrawer.PERIPHERAL_TYPE).get(0);

        String status = drawer.getStatus();
        MposLogger.getInstance().debug(TAG, " drawer check status: " + status);

        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(final Void... params) {
                // Do your loading here. Don't touch any views from here, and then return null
                try {
                    drawer.open();
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                }
                ;
                return null;
            }
        }.execute();

    }

    /** The PeripheralStatusListener method, to be notified when a specific peripheral changes status. */
    public void onStatusChanged(Peripheral peripheral, String status, HashMap<String,Object> attributes) {
        MposLogger.getInstance().debug(TAG, "onStatusChanged  status:" + status);
         final String drawerStatus = status;
        String tempStatus = drawerStatus;
        if (drawerStatus.equals(Peripheral.STATUS_ERROR)) {
            tempStatus = getActivity().getResources().getString(R.string.cash_drawer_status_message);
        }
        if (    drawerStatus.equals(CashDrawer.STATUS_OPEN) ||
                drawerStatus.equals(CashDrawer.STATUS_CLOSED) ||
                drawerStatus.equals(CashDrawer.STATUS_DISCONNECTED_BY_UNDOCK)) {
            return;
        }

        final String currentStatus = tempStatus;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("Cash Drawer Error");
                builder.setMessage(currentStatus);
                builder.setCancelable(true);
                builder.setPositiveButton("Ok",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

                AlertDialog alertDlg = builder.create();
                alertDlg.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                // getting crash from cash-drawer status UNKNOWN even though drawer open successfully.
                // TODO : Need to check with peripheral library.
                if(!isVisible())
                    alertDlg.show();
            }
        });

    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnFragmentInteractionListener {
        void cashSettle();
        void cashGoBack();
        void cashCancelTransaction();
    }
}

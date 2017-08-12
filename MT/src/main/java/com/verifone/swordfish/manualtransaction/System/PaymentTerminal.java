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

package com.verifone.swordfish.manualtransaction.System;

import android.content.Context;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.verifone.commerce.CommerceConstants;
import com.verifone.commerce.CommerceEvent;
import com.verifone.commerce.CommerceListener;
import com.verifone.commerce.Status;
import com.verifone.commerce.entities.AmountTotals;
import com.verifone.commerce.entities.Basket;
import com.verifone.commerce.entities.Merchandise;
import com.verifone.commerce.entities.Offer;
import com.verifone.commerce.entities.Payment;
import com.verifone.commerce.entities.Transaction;
import com.verifone.commerce.payment.BasketEvent;
import com.verifone.commerce.payment.BasketManager;
import com.verifone.commerce.payment.PaymentCompletedEvent;
import com.verifone.commerce.payment.ReceiptDeliveryMethodEvent;
import com.verifone.commerce.payment.TransactionEndedEvent;
import com.verifone.commerce.payment.TransactionEvent;
import com.verifone.commerce.payment.TransactionManager;
import com.verifone.commerce.payment.reports.ReconciliationEvent;
import com.verifone.commerce.payment.reports.ReportManager;
import com.verifone.swordfish.manualtransaction.MTDataModel.MTTransaction;
import com.verifone.swordfish.manualtransaction.R;
import com.verifone.swordfish.manualtransaction.Tools.MposLogger;
import com.verifone.utilities.ConversionUtility;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

import static com.verifone.commerce.entities.Receipt.DELIVERY_METHOD_EMAIL;
import static com.verifone.commerce.entities.Receipt.DELIVERY_METHOD_NONE;
import static com.verifone.commerce.entities.Receipt.DELIVERY_METHOD_PRINT;
import static com.verifone.commerce.entities.Receipt.DELIVERY_METHOD_SMS;
import static com.verifone.commerce.payment.BasketEvent.BasketAction;


public class PaymentTerminal {
    private Context ctx;
    private static final String TAG = PaymentTerminal.class.getSimpleName();
    String ipaddress = "192.168.1.21";

    private PaymentTerminalSessionEvents paymentTerminalSessionEvents;
    private PaymentTerminalItemsEvents paymentTerminalItemsEvents;
    private PaymentTerminalPaymentEvents paymentTerminalPaymentEvents;

    private Payment payment;
    private CommerceListener listener;
    private TransactionManager transactionManager;
    private BasketManager basketManager;
    private boolean isRegister = false;
    private boolean deviceIsReady = false;
    private int operationType;
    private int totalItems = 0;
    private boolean isOpen = false;
    private boolean isOpenToClose = false;
    private boolean preAuth = false;
    private boolean isVoid = false;
    private ArrayList<QueueItems> queue;
    private static String ADDED = "added";
    private static String UPDATED = "updated";
    private static String REMOVED = "removed";
    private static final PaymentTerminal ourInstance = new PaymentTerminal();
    private Payment preAuthPayment = null;
    private BigDecimal mTip = null;
    private Status status;
    private boolean atomicFlag;

    public static PaymentTerminal getInstance() {
        return ourInstance;
    }

    private PaymentTerminal() {
    }

    public void setContext(Context context) {

        //configure operationType to use Payment Manager service
        //TODO add code for application settings, after is ready
        String protocol = "SCI";
        MposLogger.getInstance().debug(TAG, "Configure protocol: " + protocol);
        if (protocol.equals("SCI")) {
            operationType = 0;
        } else {
            operationType = 1;
        }
        queue = new ArrayList<>();
        ctx = context;
        operationType = 1;

        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(final Void... params) {
                // Do your loading here. Don't touch any views from here, and then return null
                try {
                    transactionManager = TransactionManager.getTransactionManager(ctx);
                    atomicFlag = true;
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                }
                return null;
            }
        }.execute();

    }

    public void startSession() {
        MposLogger.getInstance().debug(TAG, " start session");
        Log.i(TAG, "Payment Session starting");
        if (atomicFlag) {
            postToastMessage("Waiting for payment session to be configured...", "long");
            atomicFlag = false;
        }
        if (operationType == 1) {

            if (listener == null) {
                listener = new CommerceListener() {
                    @Override
                    public CommerceEvent.Response handleEvent(CommerceEvent commerceEvent) {
                        MposLogger.getInstance().debug(TAG, " processing: " + commerceEvent.getType());
                        switch (commerceEvent.getType()) {
                            //Session events
                            case CommerceEvent.SESSION_STARTED:
                                paymentTerminalSessionEvents.sessionStarted();
                                emptyQueue();
                                isOpen = true;
                                break;
                            case CommerceEvent.SESSION_START_FAILED:
                                isOpen = false;
                                transactionManager.endSession();
                                break;
                            case CommerceEvent.SESSION_CLOSED:
                            case CommerceEvent.SESSION_ENDED:
                                isOpen = false;
                                MposLogger.getInstance().debug(TAG, "sessionStopped");
                                if (paymentTerminalSessionEvents != null) {
                                    paymentTerminalSessionEvents.processEnds();
                                }
                                break;
                            case CommerceEvent.SESSION_END_FAILED:
                            case CommerceEvent.SESSION_ERROR:
                                paymentTerminalSessionEvents.onFailure();
                                postToastMessage("Payment session initialization failed!!!", "short");
                                break;
                            //Basket events
                            case BasketEvent.TYPE:
                                BasketEvent basketEvent = (BasketEvent) commerceEvent;
                                if (basketEvent.getBasketAction() == BasketAction.ADDED) {
                                    if ((--totalItems) == 0)
                                        basketManager.finalizeBasket();
                                    if (paymentTerminalItemsEvents != null)
                                        paymentTerminalItemsEvents.onLineItemsAdded();
                                } else if (basketEvent.getBasketAction() == BasketAction.MODIFIED) {
                                    if (paymentTerminalItemsEvents != null) {
                                        paymentTerminalItemsEvents.onLineItemUpdated();
                                    }
                                } else if (basketEvent.getBasketAction() == BasketAction.REMOVED) {
                                    //Not supported in ManualTransaction
                                    if (paymentTerminalItemsEvents != null) {
                                        paymentTerminalItemsEvents.onLineItemsDeleted();
                                    }
                                }
                                break;
                            //Transaction events
                            case TransactionEvent.TRANSACTION_PAYMENT_STARTED:
                                if (paymentTerminalPaymentEvents != null) {
                                    paymentTerminalPaymentEvents.paymentStarted();
                                }
                                break;
                            case TransactionEvent.TRANSACTION_PAYMENT_COMPLETED:
                                PaymentCompletedEvent paymentCompletedEvent = (PaymentCompletedEvent) commerceEvent;
                                CommerceEvent.Response paymentInfo = paymentCompletedEvent.generateResponse();
                                payment = paymentCompletedEvent.getPayment();

                                if (preAuth) {

                                    preAuthPayment = payment;
                                    String amount = String.format(Locale.getDefault(), "%.2f", payment.getPaymentAmount().doubleValue());
                                    preAuthPayment.setGratuityAmount(payment.getPaymentAmount().multiply(new BigDecimal("0.18")));
                                    startTransaction(amount);
                                } else {
                                    presentReceiptOptions();

                                }
                                break;
                            case TransactionEvent.TRANSACTION_ENDED:
                                TransactionEndedEvent.TransactionResult result = ((TransactionEndedEvent) commerceEvent).getTransactionResult();
                                if (isVoid) {
                                    switch (result) {
                                        case SUCCESS: {
                                            paymentTerminalPaymentEvents.onSuccess(null, 0, null);
                                            break;
                                        }
                                        case CANCELLED:
                                            paymentTerminalPaymentEvents.onCancel();
                                            break;
                                        case FAILED:
                                            paymentTerminalPaymentEvents.onFailure();
                                            break;
                                        default:
                                            break;
                                    }
                                    isVoid = false;
                                } else {
                                    if (result == TransactionEndedEvent.TransactionResult.CANCELLED) {
                                        MposLogger.getInstance().debug(TAG, " onCancel");
                                        if (paymentTerminalPaymentEvents != null) {

                                            paymentTerminalPaymentEvents.onCancel();
                                        }
                                    } else {
                                        Payment payment = null;
                                        MposLogger.getInstance().debug(TAG, " on approve");
                                    }
                                }
                                break;
                            case TransactionEvent.TRANSACTION_FAILED:
                            case TransactionEvent.TRANSACTION_ERROR:
                                paymentTerminalSessionEvents.onFailure();
                                break;
                            //Receipt events
                            case ReceiptDeliveryMethodEvent.TYPE:
                                ReceiptDeliveryMethodEvent receiptDeliveryMethodEvent = (ReceiptDeliveryMethodEvent) commerceEvent;
                                int method = receiptDeliveryMethodEvent.getDeliveryMethod();
                                String recipient = "";
                                switch (method) {
                                    case DELIVERY_METHOD_EMAIL: {
                                        recipient = receiptDeliveryMethodEvent.getCustomerEmail();
                                    }
                                    break;
                                    case DELIVERY_METHOD_SMS: {
                                        recipient = receiptDeliveryMethodEvent.getCustomerPhoneNumber();
                                    }
                                    break;
                                    default:
                                        break;
                                }
                                if (paymentTerminalPaymentEvents != null && payment != null) {

                                    switch (payment.getAuthResult()) {
                                        case AUTHORIZED_ONLINE:
                                        case AUTHORIZED_EXTERNALLY:
                                        case AUTHORIZED_OFFLINE: {
                                            //paymentTerminalEventsListener.onVoiceAuthorizationRequest(message);
                                            Transaction transaction = transactionManager.getTransaction();
                                            if (Objects.equals(transaction.getTransactionType(), Transaction.PRE_AUTHORISATION_COMPLETION_TYPE)) {

                                                preAuthPayment = payment;
                                                String amount = String.format(Locale.getDefault(), "%.2f", payment.getPaymentAmount().doubleValue());
                                                preAuthPayment.setGratuityAmount(payment.getPaymentAmount().multiply(new BigDecimal("0.18")));
                                                startTransaction(amount);
                                            } else {
                                                paymentTerminalPaymentEvents.onSuccess(payment, method, recipient);
                                            }

                                        }
                                        break;
                                        case REJECTED_ONLINE:
                                        case REJECTED_OFFLINE: {
                                            String messageForCashier = payment.getAuthResponseText();
                                            if (messageForCashier != null) {
                                                paymentTerminalPaymentEvents.onVoiceAuthorizationRequest(messageForCashier);
                                            } else {
                                                String message = ctx.getString(R.string.voiceAuthorization);
                                                paymentTerminalPaymentEvents.onVoiceAuthorizationRequest(message);
                                            }
                                        }
                                        break;
                                        default: {
                                            paymentTerminalPaymentEvents.onFailure();
                                        }
                                        break;
                                    }
                                } else {
                                    //Cash transaction
                                    switch (method) {
                                        case DELIVERY_METHOD_EMAIL: {
                                            recipient = receiptDeliveryMethodEvent.getCustomerEmail();
                                            paymentTerminalPaymentEvents.onSuccessEmailRequest(recipient);
                                        }
                                        break;
                                        case DELIVERY_METHOD_SMS: {
                                            if (paymentTerminalPaymentEvents != null) {
                                                paymentTerminalPaymentEvents.onNoReceipt();
                                            }
                                        }
                                        break;
                                        case DELIVERY_METHOD_PRINT: {
                                            if (paymentTerminalPaymentEvents != null) {
                                                paymentTerminalPaymentEvents.onReceiptRequest();
                                            }
                                        }
                                        break;
                                        case DELIVERY_METHOD_NONE: {
                                            if (paymentTerminalPaymentEvents != null) {
                                                paymentTerminalPaymentEvents.onNoReceipt();
                                            }
                                        }
                                        break;
                                        default:
                                            break;
                                    }
                                }
                                break;
                            case ReconciliationEvent.TYPE: {
                                ReconciliationEvent reconciliationEvent = (ReconciliationEvent) commerceEvent;
                                Log.e(TAG, reconciliationEvent.getReconciliationId());
                            }
                            default:
                                MposLogger.getInstance().error(TAG, " unsupported operation: " + commerceEvent.getType());
                        }
                        return commerceEvent.generateResponse();
                    }
                };
            }
            //We need this timer, since on tablet wakeup, usually the payment service take longer to be available
            if (transactionManager == null) {
                CountDownTimer countDownTimer = new CountDownTimer(100000, 500) {
                    @Override
                    public void onTick(long l) {
                        MposLogger.getInstance().debug(TAG, " check to transaction manager ready");
                        if (transactionManager != null) {
                            transactionManager.setDebugMode(CommerceConstants.MODE_DEVICE);//  MODE_DEVICE | MODE_STUBS_DEBUG
                            transactionManager.startSession(listener);
                            this.cancel();
                        }
                    }

                    @Override
                    public void onFinish() {
                        MposLogger.getInstance().error(TAG, " error starting transaction manager");
                        Toast.makeText(ctx, "Error starting TransactionManager. Please reboot the device!!!", Toast.LENGTH_LONG).show();
                    }
                };
                countDownTimer.start();
            } else {
                transactionManager.setDebugMode(CommerceConstants.MODE_DEVICE);
                transactionManager.startSession(listener);
            }
        }
    }

    private void postToastMessage(final String message, final String type) {
        Handler handler = new Handler(Looper.getMainLooper());
        final int value = type == "short" ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG;
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ctx, message, value).show();
            }
        });
    }

    private void emptyQueue() {
        MposLogger.getInstance().debug(TAG, " empty queue");
        for (QueueItems items : queue) {
            switch (items.getOperation()) {
                case QueueItems.QUEUE_OP_ADD: {
                    addItem(items.getItem(), null);
                }
                break;
                case QueueItems.QUEUE_OP_UPDATE: {
                    updateItem(items.getItem(), null);
                }
                break;
                case QueueItems.QUEUE_OP_DELETE: {
                    deleteItem(items.getItem(), null);
                }
                break;
                case QueueItems.QUEUE_OP_SALE: {
                    startTransaction(items.getAmount());
                }
                break;
                case QueueItems.QUEUE_OP_VOID: {
                    voidTransaction(items.getTransaction().transactionPayments().getPayments().get(0));
                }
                default:
                    break;
            }
        }
        queue.clear();
    }

    private void createBasket() {
        basketManager = transactionManager.getBasketManager();
    }

    public void addItems(Transaction transaction, float txTotal) {
        totalItems = transaction.getBasket().getMerchandises().length;
        for (Merchandise item : transaction.getBasket().getMerchandises()) {
            basketManager.addMerchandise(item);
        }
    }

    public void addItem(Merchandise item, AmountTotals amountTotals) {
        if (operationType == 1) {
            if (isOpen) {

                if (basketManager == null) {
                    createBasket();
                }
                BigDecimal finalValue = amountTotals.getRunningTotal().add(item.getUnitPrice().multiply(new BigDecimal(item.getQuantity())));
                amountTotals.setRunningTotal(finalValue);
                amountTotals.setRunningSubtotal(finalValue);
                basketManager.addMerchandise(item, amountTotals);

            } else {
                queue.add(new QueueItems(QueueItems.QUEUE_OP_ADD, null, null));
            }

        }
    }

    public void updateItem(Merchandise item, AmountTotals amountTotals) {
        if (isOpen) {
            if (basketManager == null) {
                createBasket();
            }
            basketManager.modifyMerchandise(item, amountTotals);
        } else {
            queue.add(new QueueItems(QueueItems.QUEUE_OP_UPDATE, item));
        }
    }

    public void deleteItem(Merchandise item, AmountTotals amountTotals) {
        if (isOpen) {
            if (basketManager == null) {
                createBasket();
            }
            amountTotals.setRunningTotal(amountTotals.getRunningTotal().subtract(item.getAmount()));
            if (item.getTax() != null && !Objects.equals(item.getTax(), BigDecimal.ZERO)) {
                amountTotals.setRunningTax(amountTotals.getRunningTax().subtract(item.getTax()));
            }

            basketManager.removeMerchandise(item, amountTotals);

        } else {
            queue.add(new QueueItems(QueueItems.QUEUE_OP_DELETE, item));
        }
    }


    public void deleteAllItems() {
    }

    public void stopSession() {
        if (isOpen) {
            MposLogger.getInstance().debug(TAG, "stopSession");
            if (operationType == 1) {
                transactionManager.endSession();
                basketManager = null;
            }
        }
    }

    public BigDecimal transactionTotal() {
        return transactionManager.getTransaction().getAmount();
    }

    public BigDecimal transactionTaxTotal() {
        return transactionManager.getTransaction().getTaxAmount();
    }

    public Merchandise[] getMerchandizes() {
        if (basketManager != null) {
            if (basketManager.getBasket() != null) {
                basketManager.getBasket().getMerchandises();
            }
        }
        return null;
    }

    public void setMerchandizes(Merchandise[] merchandises) {
        if (basketManager != null) {
            if (basketManager.getBasket() != null) {
                Basket basket = basketManager.getBasket();
                basket.setMerchandises(merchandises);
            }
        }
    }

    public String transactionID() {
        return transactionManager.getTransaction().getInvoiceId();
    }

    public void addOffer(Offer offer) {
        basketManager.addOffer(offer, new AmountTotals());
    }

    public void preAuthTransaction(String amount) {
        MposLogger.getInstance().debug(TAG, "pre-aut for amount of " + amount);

        if (isOpen) {
            if (operationType == 1) {
                Payment payment = new Payment();
                try {
                    if (Float.parseFloat(amount) >= 20)
                        payment.setAuthorizationMethod(Payment.AuthorizationMethod.SIGNATURE);
                } catch (NumberFormatException e) {
                    MposLogger.getInstance().error(TAG, "Unable to parse float from amount " + amount);
                }
                payment.setRequestedPaymentAmount(ConversionUtility.parseAmount(amount));
                payment.setGratuityAmount(BigDecimal.ZERO);
                Transaction transaction = transactionManager.getTransaction();
                transaction.setTransactionType(Transaction.PRE_AUTHORISATION_TYPE);
                transactionManager.updateTransaction(transaction);
                preAuth = true;
                transactionManager.startPayment(payment);
            }
        } else {
            queue.add(new QueueItems(QueueItems.QUEUE_OP_SALE, amount));
        }
    }

    public void finalAuth(String amount, BigDecimal tip) {
        if (isOpen) {
            if (operationType == 1) {
                Payment payment = new Payment();
                Transaction transaction = transactionManager.getTransaction();

                MposLogger.getInstance().debug(TAG, "final pre-Auth for amount of " + amount);
                payment.setAuthCode(preAuthPayment.getAuthCode());
                transaction.setTransactionType(Transaction.PRE_AUTHORISATION_COMPLETION_TYPE);
                payment.setGratuityAmount(tip);
                payment.setRequestedPaymentAmount(ConversionUtility.parseAmount(amount).add(mTip));
                transactionManager.updateTransaction(transaction);
                transactionManager.startPayment(payment);
                preAuth = false;
                preAuthPayment = null;
            }
        } else {
            queue.add(new QueueItems(QueueItems.QUEUE_OP_SALE, amount));
        }

    }

    public void startTransaction(String amount) {

        if (isOpen) {
            if (operationType == 1) {
                Payment payment = new Payment();
                Transaction transaction = transactionManager.getTransaction();
                MposLogger.getInstance().debug(TAG, "startTransaction for amount of " + amount);
                try {
                    if (Float.parseFloat(amount) >= 20)
                        payment.setAuthorizationMethod(Payment.AuthorizationMethod.SIGNATURE);
                } catch (NumberFormatException e) {
                    MposLogger.getInstance().error(TAG, "Unable to parse float from amount " + amount);
                }
                payment.setGratuityAmount(BigDecimal.ZERO);
                payment.setRequestedPaymentAmount(ConversionUtility.parseAmount(amount));
                transaction.setTransactionType(Transaction.PAYMENT_TYPE);

                transactionManager.updateTransaction(transaction);
                transactionManager.startPayment(payment);
                preAuth = false;
                preAuthPayment = null;
            }
        } else {
            queue.add(new QueueItems(QueueItems.QUEUE_OP_SALE, amount));
        }
    }

    public void startManualTransaction(String amount) {
        if (isOpen) {
            if (operationType == 1) {
                Payment payment = new Payment();
                Transaction transaction = transactionManager.getTransaction();
                MposLogger.getInstance().debug(TAG, "startTransaction for amount of " + amount);
                try {
                    if (Float.parseFloat(amount) >= 20)
                        payment.setAuthorizationMethod(Payment.AuthorizationMethod.SIGNATURE);
                } catch (NumberFormatException e) {
                    MposLogger.getInstance().error(TAG, "Unable to parse float from amount " + amount);
                }
                payment.setGratuityAmount(BigDecimal.ZERO);
                payment.setRequestedPaymentAmount(ConversionUtility.parseAmount(amount));
                transaction.setTransactionType(Transaction.PAYMENT_TYPE);
                transaction.setAllowsManualEntry(true);
                transactionManager.updateTransaction(transaction);
                transactionManager.startPayment(payment);
                preAuth = false;
                preAuthPayment = null;
            }
        } else {
            queue.add(new QueueItems(QueueItems.QUEUE_OP_SALE, amount));
        }
    }

    public void cancelTransaction() {
        transactionManager.cancelTransaction();
    }

    public void voidTransaction(Payment payment) {
        isVoid = true;
        Transaction transaction = transactionManager.getTransaction();
        transaction.setAmount(payment.getPaymentAmount());
        transactionManager.processVoid(transaction);
    }

    public void refundTransaction(Payment payment) {
        isVoid = true;
        Transaction transaction = transactionManager.getTransaction();
        transaction.setAmount(payment.getPaymentAmount());
        transactionManager.processRefund(payment);
    }

    public void requestVoiceAuthorization(String authorizationCode, String amount) {
        if (isOpen) {
            if (operationType == 1) {
                Payment payment = new Payment();
                try {
                    if (Float.parseFloat(amount) >= 20)
                        payment.setAuthorizationMethod(Payment.AuthorizationMethod.SIGNATURE);
                } catch (NumberFormatException e) {
                    MposLogger.getInstance().error(TAG, "Unable to parse float from amount " + amount);
                }
                payment.setRequestedPaymentAmount(ConversionUtility.parseAmount(amount));
                payment.setGratuityAmount(BigDecimal.ZERO);
                payment.setAuthResult(Payment.AuthorizationResult.AUTHORIZED_EXTERNALLY);
                payment.setAuthCode(authorizationCode);
                transactionManager.startPayment(payment);
            }
        } else {
            queue.add(new QueueItems(QueueItems.QUEUE_OP_SALE, amount));
        }
    }

    public void presentReceiptOptions() {
        int[] deliveryMethods = new int[]{
                DELIVERY_METHOD_PRINT,
                DELIVERY_METHOD_EMAIL,
                DELIVERY_METHOD_SMS,
                DELIVERY_METHOD_NONE};
        transactionManager.presentReceiptDeliveryOptions(deliveryMethods,
                "555-5555-5555",
                "john.doe@myMail.com");
    }

    public void getReportManager() {
        ReportManager manager;
        manager = transactionManager.getReportManager();
        if (manager != null) {
            status = manager.reconcileWithAcquirer(null);
        }
    }

    public void addGeneralListenr(CommerceListener l) {
        transactionManager.addGeneralListener(l);
    }

    //Events listener's
    public void setSessionListener(PaymentTerminalSessionEvents listener) {
        paymentTerminalSessionEvents = listener;
    }

    public void setItemsListener(PaymentTerminalItemsEvents listener) {
        paymentTerminalItemsEvents = listener;
    }

    public void setPaymentListener(PaymentTerminalPaymentEvents listener) {
        paymentTerminalPaymentEvents = listener;
    }

    public interface PaymentTerminalSessionEvents {

        public void sessionStarted();

        public void processStart();

        public void processEnds();

        public void onFailure();

        public void onTimeOut();
    }

    public interface PaymentTerminalItemsEvents {

        public void basketStarted();

        public void basketReady();

        public void onLineItemsAdded();

        public void onLineItemsAddError();

        public void onLineItemUpdated();

        public void onLineItemsDeleted();

        public void onLineItemsDeletedFail();

        public void onFailure();

        public void onTimeOut();
    }

    public interface PaymentTerminalPaymentEvents {

        public void paymentStarted();

        public void onSuccess(Payment paymentInfo, int method, String customer);

        public void onVoiceAuthorizationRequest(String message);

        public void onSignatureRequest();

        public void onReceiptRequest();

        public void onSuccessEmailRequest(String email);

        public void onNoReceipt();

        public void onDeclined();

        public void onCancel();

        public void onFailure();

        public void onTimeOut();
    }

    //QueueItems, private class to queue payment terminal operations
    private class QueueItems {
        private static final String QUEUE_OP_VOID = "void";
        private static final String QUEUE_OP_SALE = "sale";
        private static final String QUEUE_OP_DELETE = "delete";
        private static final String QUEUE_OP_UPDATE = "update";
        private static final String QUEUE_OP_ADD = "add";

        private String operation = null;
        private Merchandise item = null;
        private MTTransaction transaction = null;
        private String amount = null;

        protected QueueItems() {
        }

        QueueItems(String op, Merchandise i) {
            this.operation = op;
            this.item = i;
        }

        QueueItems(String op, MTTransaction t, Merchandise i) {
            this.operation = op;
            this.transaction = t;
            this.item = i;
        }

        public QueueItems(String op, MTTransaction t) {
            this.operation = op;
            this.transaction = t;
        }

        QueueItems(String op, String a) {
            this.operation = op;
            this.amount = a;
        }

        String getOperation() {
            return operation;
        }

        Merchandise getItem() {
            return item;
        }

        public MTTransaction getTransaction() {
            return transaction;
        }

        public String getAmount() {
            return amount;
        }
    }
}

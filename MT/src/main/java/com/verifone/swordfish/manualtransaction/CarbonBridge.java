package com.verifone.swordfish.manualtransaction;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.widget.Toast;

import com.verifone.commerce.CommerceConstants;
import com.verifone.commerce.CommerceEvent;
import com.verifone.commerce.CommerceListener;
import com.verifone.commerce.CommerceResponse;
import com.verifone.commerce.entities.AmountAdjustment;
import com.verifone.commerce.entities.AmountTotals;
import com.verifone.commerce.entities.BasketAdjustment;
import com.verifone.commerce.entities.Donation;
import com.verifone.commerce.entities.Merchandise;
import com.verifone.commerce.entities.Offer;
import com.verifone.commerce.entities.Payment;
import com.verifone.commerce.entities.Transaction;
import com.verifone.commerce.payment.AmountAdjustedEvent;
import com.verifone.commerce.payment.BasketAdjustedEvent;
import com.verifone.commerce.payment.BasketEvent;
import com.verifone.commerce.payment.BasketManager;
import com.verifone.commerce.payment.LoyaltyReceivedEvent;
import com.verifone.commerce.payment.PaymentCompletedEvent;
import com.verifone.commerce.payment.ReceiptDeliveryMethodEvent;
import com.verifone.commerce.payment.TransactionEndedEvent;
import com.verifone.commerce.payment.TransactionEvent;
import com.verifone.commerce.payment.TransactionManager;
import com.verifone.commerce.payment.reports.ReconciliationEvent;
import com.verifone.swordfish.manualtransaction.tools.BasketUtils;
import com.verifone.swordfish.manualtransaction.tools.Utils;
import com.verifone.utilities.ConversionUtility;
import com.verifone.utilities.Log;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.verifone.commerce.entities.Receipt.DELIVERY_METHOD_EMAIL;
import static com.verifone.commerce.entities.Receipt.DELIVERY_METHOD_NONE;
import static com.verifone.commerce.entities.Receipt.DELIVERY_METHOD_PRINT;
import static com.verifone.commerce.entities.Receipt.DELIVERY_METHOD_SMS;

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
 * Created by abey on 1/5/2018.
 */

public class CarbonBridge {
    private final static String TAG = CarbonBridge.class.getSimpleName();

    // Temporary minimum value for a payment
    private final static BigDecimal TENDS_TO_ZERO_BIG_DECIMAL = ConversionUtility.parseAmount(0.01f);

    // Thread for init transaction manager async
    private final HandlerThread mHandlerThread;

    // Handler to perform some actions in Main thread
    private Handler mMainHandler;

    // Transaction manager instance to work with payment terminal
    private TransactionManager mTransactionManager;

    // Basket manager instance to work with basket items
    private BasketManager mBasketManager;

    // Listener to get any events from transaction manager
    private CommerceListener mCommerceListener;

    private IBridgeListener mListener;
    private Context mContext;

    private BigDecimal mExtraAmount;
    private AtomicBoolean mSessionIsActive;
    private String mCurrentIpAddress;
    private String mCurrentPort;
    private AtomicBoolean mHasLoggedIn;

    CarbonBridge(Context appContext) {
        mContext = appContext;

        mExtraAmount = BigDecimal.ZERO;
        mSessionIsActive = new AtomicBoolean(false);
        mCurrentIpAddress = Utils.getTerminalIP(mContext);
        mCurrentPort = Utils.getTerminalPort(mContext);

        mMainHandler = new Handler(Looper.getMainLooper());

        mHandlerThread = new HandlerThread(CarbonBridge.class.getSimpleName() + ".worker");
        mHandlerThread.start();
        Handler workerHandler = new Handler(mHandlerThread.getLooper());

        mHasLoggedIn = new AtomicBoolean(false);

        //create listener
        mCommerceListener = new CommerceListener() {
            @Override
            public CommerceResponse handleEvent(final CommerceEvent commerceEvent) {
                final int status = commerceEvent.getStatus();
                Log.i(TAG, "Received " + (commerceEvent.getStatus() == 0 ? " success! " : " failure! ") + commerceEvent.getType() + " " + status + " " + commerceEvent.getMessage());
                switch (commerceEvent.getType()) {
                    // Session events
                    case CommerceEvent.SESSION_STARTED:
                        if (status == 0) {
                            mSessionIsActive.set(true);
                            mMainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (mListener != null) {
                                        mListener.sessionStarted();
                                        mBasketManager = null;
                                    }
                                }
                            });
                            break;
                        }
                    case CommerceEvent.SESSION_START_FAILED:
                        mSessionIsActive.set(false);
                        mMainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (mListener != null) {
                                    mListener.sessionStartFailed();
                                }
                            }
                        });
                        break;
                    case CommerceEvent.SESSION_ENDED:
                    case CommerceEvent.SESSION_CLOSED:
                        mSessionIsActive.set(false);
                        mMainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (mListener != null) {
                                    mListener.sessionStopped();
                                }
                            }
                        });
                        break;

                    // Basket events
                    case BasketEvent.TYPE:
                        BasketEvent basketEvent = (BasketEvent) commerceEvent;
                        switch (basketEvent.getBasketAction()) {
                            case ADDED:
                                mMainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (mListener != null) {
                                            mListener.merchandiseAdded();
                                        }
                                    }
                                });
                                break;
                            case MODIFIED:
                                mMainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (mListener != null) {
                                            mListener.merchandiseUpdated();
                                        }
                                    }
                                });
                                break;
                            case REMOVED:
                                mMainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (mListener != null) {
                                            mListener.merchandiseDeleted();
                                        }
                                    }
                                });
                                break;
                            case FINALIZED:
                                mMainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (mListener != null) {
                                            mListener.basketFinalized();
                                        }
                                    }
                                });
                                break;
                        }
                        break;

                    // Transaction events
                    case TransactionEvent.TRANSACTION_ERROR:
                        // Need to display the message to the user!
                        Toast.makeText(mContext, commerceEvent.getMessage(), Toast.LENGTH_LONG).show();
                        break;
                    case TransactionEvent.TRANSACTION_ENDED:
                        TransactionEndedEvent.TransactionResult result = ((TransactionEndedEvent) commerceEvent).getTransactionResult();
                        switch (result) {
                            case SUCCESS:
                                mMainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (mListener != null) {
                                            mListener.onTransactionEnded(true);
                                        }
                                    }
                                });
                                break;
                            case CANCELLED:
                                mMainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (mListener != null) {
                                            mListener.onPaymentCanceled();
                                            mListener.onTransactionEnded(false);
                                        }
                                    }
                                });
                                break;
                            case FAILED:
                                //TODO
                                mMainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (mListener != null) {
                                            mListener.onPaymentCanceled();
                                            mListener.onTransactionEnded(false);
                                        }
                                    }
                                });
                                break;
                            default:
                                break;
                        }
                        break;
                    case TransactionEvent.TRANSACTION_PAYMENT_STARTED:
                        break;
                    case TransactionEvent.TRANSACTION_PAYMENT_COMPLETED:
                        PaymentCompletedEvent paymentCompletedEvent = (PaymentCompletedEvent) commerceEvent;
                        CommerceEvent.Response paymentInfo = paymentCompletedEvent.generateResponse();
                        Payment payment = paymentCompletedEvent.getPayment();
                        if (payment.getAuthResult() == Payment.AuthorizationResult
                                .AUTHORIZED_ONLINE || payment.getAuthResult() == Payment
                                .AuthorizationResult
                                .AUTHORIZED_OFFLINE) {

                            if (mListener != null) {
                                mListener.onPaymentSuccess(payment);
                            }
                            presentReceiptOptions();

                        } else if (payment.getAuthResult() == Payment.AuthorizationResult
                                .REJECTED_ONLINE || payment.getAuthResult() == Payment
                                .AuthorizationResult
                                .REJECTED_OFFLINE) {

                            if (mListener != null) {
                                mListener.onPaymentDecline();
                            }
                        } else if (payment.getAuthResult() == Payment.AuthorizationResult.USER_CANCELLED) {
                            if (mListener != null) {
                                mListener.onPaymentCanceled();
                            }
                        } else if (payment.getAuthResult() == Payment
                                .AuthorizationResult
                                .CASH_VERIFIED) {
                            if (mListener != null) {
                                mListener.onPaymentSuccess(payment);
                            }
                            presentReceiptOptions();
                            // TODO: [SB-5007] 03.29.2018:  Temporary workaround for demo on non-carbon terminals
                            // TODO: Avoids payment failure. NOTE: payment card information is null
                        } else if (payment.getAuthResult() == null) {
                            if (mListener != null) {
                                mListener.onPaymentSuccess(payment);
                            }
                            presentReceiptOptions();
                        } else {
                            if (mListener != null) {
                                mListener.onPaymentFailure();
                            }
                        }

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
                        if (mListener != null) {
                            mListener.onReceiptMethodSelected(method, recipient);
                        }
                        break;
                    case BasketAdjustedEvent.TYPE:
                        BasketAdjustedEvent basketAdjustedEvent = (BasketAdjustedEvent) commerceEvent;
                        BasketAdjustment basketAdjustment = basketAdjustedEvent.getAdjustments();
                        /* temporary storing list of offers that will be applied for whole basket */
                        List<Offer> basketOffers = new ArrayList<>();
                        BasketAdjustment adjustmentToApply = new BasketAdjustment();
                        BigDecimal adjustmentAmount = ConversionUtility.parseAmount(0);

                        if (basketAdjustment != null) {
                            Offer[] basketAdjustmentOffers = basketAdjustment.getOffers();
                            if (basketAdjustmentOffers != null) {
                                for (Offer offer : basketAdjustmentOffers) {
                                    String associatedProductCode = offer.getAssociatedProductCode();
                                    if (!TextUtils.isEmpty(associatedProductCode)) {
                                        List<Merchandise> merchandises = getMerchandises();
                                        if (merchandises != null && !merchandises.isEmpty()) {
                                            for (Merchandise merchandise : merchandises) {
                                                String upc = merchandise.getUpc();
                                                String sku = merchandise.getSku();
                                                if (associatedProductCode.equalsIgnoreCase(upc) || associatedProductCode.equalsIgnoreCase(sku)) {
                                                    /* match with merchandise sku / upc occurred, applying offer for a specific merchandise according to it quantity in the basket*/
                                                    Offer applyingOffer = getOfferWithCalculatedAmount(offer, merchandise.getUnitPrice(), merchandise.getQuantity());
                                                    adjustmentAmount = adjustmentAmount.add(applyingOffer.getAmount());
                                                    adjustmentToApply.addOffer(applyingOffer);
                                                }
                                            }
                                        }
                                    } else {
                                        basketOffers.add(offer);
                                    }
                                }
                                /*if there is any basket offers - apply them to basket total with subtract of applied merchandise discounts */
                                if (!basketOffers.isEmpty()) {
                                    for (Offer offer : basketOffers) {
                                        Offer applyingOffer = getOfferWithCalculatedAmount(offer, BasketUtils.calculateMerchandisesTotalAmount().add(adjustmentAmount), BigDecimal.ONE);
                                        adjustmentAmount = adjustmentAmount.add(applyingOffer.getAmount());
                                        adjustmentToApply.addOffer(applyingOffer);
                                    }
                                }
                            }

                            Donation[] basketAdjustmentDonations = basketAdjustment.getDonations();
                            if (basketAdjustmentDonations != null) {
                                for (Donation donation : basketAdjustmentDonations) {
                                    adjustmentAmount = adjustmentAmount.add(donation.getAmount());
                                }
                                adjustmentToApply.addDonations(Arrays.asList(basketAdjustmentDonations));
                            }
                        }

                        if ((adjustmentToApply.getOffers() != null && adjustmentToApply.getOffers().length > 0)
                                || (adjustmentToApply.getDonations() != null && adjustmentToApply.getDonations().length > 0)) {

                            adjustmentToApply.setBasketAdjusted(true);

                            mMainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (mListener != null) {
                                        mListener.basketAdjusted();
                                    }
                                }
                            });
                        }

                        BasketAdjustedEvent.Response basketAdjustmentResponse = basketAdjustedEvent.generateResponse();
                        basketAdjustmentResponse.setFinalAdjustments(adjustmentToApply, getFinalAdjustmentTotals(adjustmentAmount));
                        mTransactionManager.sendEventResponse(basketAdjustmentResponse);
                        break;
                    case AmountAdjustedEvent.TYPE:
                        AmountAdjustedEvent amountAdjustedEvent = (AmountAdjustedEvent) commerceEvent;
                        AmountAdjustment[] amountAdjustments = amountAdjustedEvent.getAdjustments();
                        BigDecimal totalAdjustment = ConversionUtility.parseAmount(0);
                        if (amountAdjustments != null) {
                            for (AmountAdjustment adjustment : amountAdjustments) {
                                BigDecimal adjustmentValue = adjustment.getAdjustmentValue();
                                if (adjustmentValue != null) {
                                    totalAdjustment = totalAdjustment.add(adjustmentValue);
                                }
                            }
                        }

                        BigDecimal currentSubtotalForAmountAdjusted = BasketUtils.calculateBasketTotalAmount();
                        if (currentSubtotalForAmountAdjusted.add(totalAdjustment).floatValue() < TENDS_TO_ZERO_BIG_DECIMAL.floatValue()) {
                            totalAdjustment = TENDS_TO_ZERO_BIG_DECIMAL.subtract(currentSubtotalForAmountAdjusted);
                        }

                        applyAmountChange(totalAdjustment);

                        mMainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (mListener != null) {
                                    mListener.merchandiseUpdated();
                                }
                            }
                        });
                        AmountAdjustedEvent.Response amountAdjustedResponse = amountAdjustedEvent.generateResponse();
                        amountAdjustedResponse.applyAdjustments(amountAdjustedEvent.getAdjustments());

                        Payment runningPayment = amountAdjustedResponse.getPayment();
                        if (runningPayment != null) {
                            runningPayment.setRequestedPaymentAmount(BasketUtils.calculateBasketTotalAmount().add(mExtraAmount));
                        }

                        mTransactionManager.sendEventResponse(amountAdjustedResponse);
                        break;
                    case LoyaltyReceivedEvent.TYPE:
                        LoyaltyReceivedEvent loyaltyReceivedEvent = (LoyaltyReceivedEvent) commerceEvent;

                        BigDecimal currentSubtotal = BasketUtils.calculateBasketTotalAmount();
                        BigDecimal newSubtotal = BasketUtils.applyOffersFromEvent(loyaltyReceivedEvent.getLoyaltyOffersList(), currentSubtotal);
                        if (newSubtotal.floatValue() < TENDS_TO_ZERO_BIG_DECIMAL.floatValue()) {
                            newSubtotal = TENDS_TO_ZERO_BIG_DECIMAL;
                        }

                        LoyaltyReceivedEvent.Response loyaltyEventResponse = loyaltyReceivedEvent.generateResponse();
                        loyaltyEventResponse.setLoyaltyOffers(loyaltyReceivedEvent.getLoyaltyOffersList());
                        loyaltyEventResponse.getTransaction().setAmount(newSubtotal);
                        loyaltyEventResponse.getResponsePayment().setRequestedPaymentAmount(newSubtotal);

                        BigDecimal amountDiff = newSubtotal.subtract(currentSubtotal);

                        applyAmountChange(amountDiff);

                        Payment responsePayment = loyaltyEventResponse.getResponsePayment();
                        if (responsePayment != null) {
                            responsePayment.setRequestedPaymentAmount(BasketUtils.calculateBasketTotalAmount().add(mExtraAmount));
                        }

                        mTransactionManager.sendEventResponse(loyaltyEventResponse);
                        break;

                    case ReconciliationEvent.TYPE:
                        mTransactionManager.removeGeneralListener(mCommerceListener);
                        if (status == 0) {
                            mMainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (mListener != null) {
                                        mListener.reconcileSuccess();
                                    }
                                }
                            });
                        } else {
                            mMainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (mListener != null) {
                                        mListener.reconcileFailed(commerceEvent.getMessage());
                                    }
                                }
                            });
                        }
                        break;
                    case TransactionEvent.LOGIN_COMPLETED:
                        if (status != 0) {
                            // If logging in failed, reset this so that we try again.
                            mHasLoggedIn.set(false);
                        }
                        break;
                    case TransactionEvent.LOGOUT_COMPLETED:
                        break;
                }
                return commerceEvent.generateResponse();
            }
        };

        Runnable initTransactionManagerRunnable = new Runnable() {
            @Override
            public void run() {
                // get connection to terminal service
                mTransactionManager = TransactionManager.getTransactionManager(mContext);
                mTransactionManager.setDebugMode(CommerceConstants.MODE_DEVICE); //  MODE_DEVICE | MODE_STUBS_DEBUG
                mTransactionManager.enableCpTriggerHandling();
                initTerminalIP();
                mHandlerThread.quitSafely();
            }
        };
        workerHandler.post(initTransactionManagerRunnable);
    }

    void waitForTransactionManagerInit() {
        if (mTransactionManager == null && mHandlerThread.isAlive()) {
            try {
                mHandlerThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void applyAmountChange(final BigDecimal totalAdjustment) {
        mExtraAmount = mExtraAmount.add(totalAdjustment);
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.onAmountAdded(totalAdjustment);
                }
            }
        });
    }

    private void presentReceiptOptions() {
        int[] deliveryMethods = new int[]{
                DELIVERY_METHOD_PRINT,
                DELIVERY_METHOD_EMAIL,
                DELIVERY_METHOD_SMS,
                DELIVERY_METHOD_NONE};
        mTransactionManager.presentReceiptDeliveryOptions(deliveryMethods,
                "555-5555-5555",
                "john.doe@myMail.com");
    }

    public void setListener(IBridgeListener listener) {
        this.mListener = listener;
    }

    private void initTerminalIP() {
        final Map<String, Object> deviceParams = new HashMap<>();
        final String newIpAddress = Utils.getTerminalIP(mContext);
        final String newPort = Utils.getTerminalPort(mContext);
        if (!Objects.equals(mCurrentIpAddress, newIpAddress)) {
            mCurrentIpAddress = newIpAddress;
            deviceParams.put(TransactionManager.DEVICE_IP_ADDRESS_KEY, newIpAddress);
        }
        if (!Objects.equals(mCurrentPort, newPort)) {
            mCurrentPort = newPort;
            if (TextUtils.isEmpty(newPort)) {
                deviceParams.put(TransactionManager.DEVICE_PORT_KEY, null);
            } else {
                try {
                    deviceParams.put(TransactionManager.DEVICE_PORT_KEY, Integer.valueOf(newPort));
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Unable to parse the port number.", e);
                    Toast.makeText(mContext, "Port must be a number!", Toast.LENGTH_SHORT).show();
                }
            }
        }
        if (!deviceParams.isEmpty()) {
            Log.d(TAG, "initTerminalIP " + TextUtils.join(", ", deviceParams.values()));
            mTransactionManager.setDeviceParams(deviceParams);
            mHasLoggedIn.set(false);
        }
    }

    public void startPaymentSession() {
        initTerminalIP();
        if (!mHasLoggedIn.get()) {
            mHasLoggedIn.set(true);
            mTransactionManager.login(mCommerceListener, null, null, null);
        }
        Transaction transaction = new Transaction();
        transaction.setCurrency(Currency.getInstance(Locale.getDefault()).getCurrencyCode());
        boolean sessionStarted = mTransactionManager.startSession(mCommerceListener);
        if (!sessionStarted) {
            mSessionIsActive.set(false);
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mListener != null) {
                        mListener.sessionStartFailed();
                    }
                }
            });
        }
        if (isSessionAndBasketManagerAlive()) {
            mTransactionManager.getBasketManager().purgeBasket();
        }
    }

    public void startRefundSession() {
        initTerminalIP();
        Transaction transaction = new Transaction(Transaction.REFUND_TYPE);
        transaction.setCurrency(Currency.getInstance(Locale.getDefault()).getCurrencyCode());
        mTransactionManager.startSession(mCommerceListener, transaction);

    }

    public void startVoidSession() {
        initTerminalIP();
        Transaction transaction = new Transaction(Transaction.VOID_TYPE);
        transaction.setCurrency(Currency.getInstance(Locale.getDefault()).getCurrencyCode());
        mTransactionManager.startSession(mCommerceListener, transaction);
    }

    public void stopSession() {
        mTransactionManager.endSession();
        mBasketManager = null;
    }

    public Transaction getTransaction() {
        return mTransactionManager.getTransaction();
    }

    public void updateTransaction(Transaction transaction) {
        mTransactionManager.updateTransaction(transaction);
    }

    public void cancelTransaction() {
        if (isSessionAndBasketManagerAlive()) {
            mBasketManager.purgeBasket();
            mBasketManager = null;
        }
        mTransactionManager.abort();
    }

    public void finalizeBasket() {
        if (!isSessionAndBasketManagerAlive()) {
            return;
        }
        mBasketManager.finalizeBasket();
    }

    public void startPayment(Payment payment, boolean isManualPayment) {
        mExtraAmount = BigDecimal.ZERO;

        Transaction transaction = getTransaction();
        transaction.setAllowsManualEntry(isManualPayment);
        mTransactionManager.updateTransaction(transaction);

        mTransactionManager.startPayment(payment);
    }

    public void addMerchandise(Merchandise merchandise, AmountTotals amountTotals) {
        if (!isSessionAndBasketManagerAlive()) {
            return;
        }
        mBasketManager.addMerchandise(merchandise, amountTotals);
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.merchandiseAdded();
                }
            }
        });
    }

    public void addMerchandise(Merchandise merchandise) {
        addMerchandise(merchandise, null);
    }

    // TODO: [05.11.2018] For now there is no possibility to re-adjust basket when moving
    // TODO: back from PaymentActivity to OrderCreateActivity for editing merchandises.
    // TODO: If it need to be supported, need to implement logic of removing offers and donations
    // TODO: if corresponding merchandises changes (removed, changed SKU/UPC) or basket becomes empty during editing
    // TODO: and initiate BasketAdjustment process again

    public void updateMerchandise(Merchandise merchandise, AmountTotals amountTotals) {
        if (!isSessionAndBasketManagerAlive()) {
            return;
        }
        mBasketManager.modifyMerchandise(merchandise, amountTotals);
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.merchandiseUpdated();
                }
            }
        });
    }

    public void updateMerchandise(Merchandise merchandise) {
        updateMerchandise(merchandise, null);
    }

    public void deleteMerchandise(Merchandise merchandise, AmountTotals amountTotals) {
        if (!isSessionAndBasketManagerAlive()) {
            return;
        }
        mBasketManager.removeMerchandise(merchandise, amountTotals);
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.merchandiseDeleted();
                }
            }
        });
    }

    public void deleteMerchandise(Merchandise merchandise) {
        deleteMerchandise(merchandise, null);
    }

    @Nullable
    public List<Merchandise> getMerchandises() {
        if (!isSessionAndBasketManagerAlive()) {
            return null;
        }
        return mBasketManager.getBasket() == null ? null : Arrays.asList(mBasketManager.getBasket().getMerchandises());
    }

    @Nullable
    public List<Offer> getAdjustedOffers() {
        if (!isSessionAndBasketManagerAlive()) {
            return null;
        }
        return mBasketManager.getBasket() == null ? null : Arrays.asList(mBasketManager.getBasket().getOffers());
    }

    @Nullable
    public List<Donation> getAdjustedDonations() {
        if (!isSessionAndBasketManagerAlive()) {
            return null;
        }
        return mBasketManager.getBasket() == null ? null : Arrays.asList(mBasketManager.getBasket().getDonations());
    }

    private Offer getOfferWithCalculatedAmount(@NonNull Offer offer, @NonNull BigDecimal affectedAmount, @NonNull BigDecimal applyingQuantity) {

        /* note: offer does not have quantity field, whole offer amount for identical merchandises with quantity > 1 need to be calculated */

        BigDecimal discount = offer.getOfferDiscount();

        if (discount == null) {
            BigDecimal percentDiscount = offer.getOfferPercentDiscount();
            discount = affectedAmount.multiply(percentDiscount != null ? percentDiscount : BigDecimal.ZERO);
        }

        if (affectedAmount.add(discount).floatValue() < TENDS_TO_ZERO_BIG_DECIMAL.floatValue()) {
            discount = TENDS_TO_ZERO_BIG_DECIMAL.subtract(affectedAmount);
        }

        Offer calculatedOffer = new Offer();
        calculatedOffer.setOfferId(offer.getOfferId());
        calculatedOffer.setOfferType(offer.getOfferType());
        calculatedOffer.setDescription(offer.getDescription());
        calculatedOffer.setOfferRefundable(offer.getOfferRefundable());
        calculatedOffer.setOfferCombinable(offer.getOfferCombinable());
        calculatedOffer.setOfferDiscount(offer.getOfferDiscount());
        calculatedOffer.setOfferPercentDiscount(offer.getOfferPercentDiscount());
        calculatedOffer.setProgramId(offer.getProgramId());
        calculatedOffer.setMerchantOfferCode(offer.getMerchantOfferCode());
        calculatedOffer.setProductCode(offer.getProductCode());
        calculatedOffer.setAssociatedProductCode(offer.getAssociatedProductCode());
        calculatedOffer.setSpecialProductCode(offer.getSpecialProductCode());
        calculatedOffer.setQrCode(offer.getQrCode());
        calculatedOffer.setReferenceBasketLineItemId(offer.getReferenceBasketLineItemId());
        calculatedOffer.setAmount(discount.multiply(applyingQuantity));

        return calculatedOffer;
    }

    private AmountTotals getFinalAdjustmentTotals(BigDecimal adjustmentAmount) {
        BigDecimal transactionTotal = BasketUtils.calculateMerchandisesTotalAmount();
        AmountTotals finalAdjustmentTotals = AmountTotals.getUnsetAmountTotals();
        finalAdjustmentTotals.setRunningSubtotal(transactionTotal);
        finalAdjustmentTotals.setRunningTotal(transactionTotal.add(adjustmentAmount));
        return finalAdjustmentTotals;
    }

    public void refundPayment(Payment payment) {
        mTransactionManager.processRefund(payment);
    }

    public void voidTransaction(Transaction transaction) {
        mTransactionManager.processVoid(transaction);
    }

    public void reconcileTransactions() {
        mTransactionManager.startSession(mCommerceListener);
        mTransactionManager.addGeneralListener(mCommerceListener);
        mTransactionManager.getReportManager().reconcileWithAcquirer();
        mTransactionManager.endSession();
    }

    public void tearDown() {
        if (mHasLoggedIn.get()) {
            if (mSessionIsActive.get()) {
                // Abort whatever might have been happening already.
                mTransactionManager.abort();
                mTransactionManager.endSession();
            }
            mTransactionManager.logout();
        }
    }

    private boolean isSessionAndBasketManagerAlive() {
        if (!mSessionIsActive.get()) {
            return false;
        } else {
            if (mBasketManager == null) {
                mBasketManager = mTransactionManager.getBasketManager();
            }
            return mBasketManager != null;
        }
    }

}

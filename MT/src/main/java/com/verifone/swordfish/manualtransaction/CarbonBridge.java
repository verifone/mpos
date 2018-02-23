package com.verifone.swordfish.manualtransaction;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.text.TextUtils;

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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    // Transaction manager instance to work with payment terminal
    private TransactionManager mTransactionManager;

    // Basket manager instance to work with basket items
    private BasketManager mBasketManager;

    // Listener to get any events from transaction manager
    private CommerceListener mCommerceListener;

    // Handler to perform some actions in Main thread
    private Handler mMainHandler;

    // Thread for init transaction manager async
    private final HandlerThread mHandlerThread;

    private IBridgeListener mListener;
    private Context mContext;
    private BasketAdjustedEvent.Response mLatestBasketAdjustedResponse;

    private BigDecimal mExtraAmount = BigDecimal.ZERO;

    private static final BigDecimal TENDS_TO_ZERO_BIG_DECIMAL = ConversionUtility.parseAmount(0.01f);

    CarbonBridge(Context appContext) {
        mContext = appContext;

        mMainHandler = new Handler(Looper.getMainLooper());

        mHandlerThread = new HandlerThread(CarbonBridge.class.getSimpleName() + ".worker");
        mHandlerThread.start();
        Handler workerHandler = new Handler(mHandlerThread.getLooper());

        Runnable initTransactionManagerRunnable = new Runnable() {
            @Override
            public void run() {
                // get connection to terminal service
                mTransactionManager = TransactionManager.getTransactionManager(mContext);
                mTransactionManager.setDebugMode(CommerceConstants.MODE_DEVICE); //  MODE_DEVICE | MODE_STUBS_DEBUG
                mTransactionManager.enableCpTriggerHandling();

                mHandlerThread.quitSafely();
            }
        };
        workerHandler.post(initTransactionManagerRunnable);

        //create listener
        mCommerceListener = new CommerceListener() {
            @Override
            public CommerceResponse handleEvent(final CommerceEvent commerceEvent) {
                int status = commerceEvent.getStatus();
                switch (commerceEvent.getType()) {
                    // Session events
                    case CommerceEvent.SESSION_STARTED:
                        if (status == 0) {
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
                                if (mLatestBasketAdjustedResponse != null) {
                                    onBasketAdjustedCompleted();
                                }
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
                        if (basketAdjustment != null) {
                            Offer[] basketAdjustmentOffers = basketAdjustment.getOffers();
                            if (basketAdjustmentOffers != null) {
                                for (Offer offer : basketAdjustmentOffers) {
                                    BigDecimal amount = offer.getOfferDiscount();
                                    if (amount != null) {
                                        fillMerchandiseWithAdjustment(BasketUtils.calculateTotalAmount(),
                                                offer.getDescription(), amount, false);
                                    } else {
                                        BigDecimal offerPercentDiscount = offer.getOfferPercentDiscount();
                                        fillMerchandiseWithAdjustment(BasketUtils.calculateTotalAmount(),
                                                offer.getDescription(), offerPercentDiscount != null ? offerPercentDiscount : BigDecimal.ZERO, true);
                                    }
                                }
                            }
                            Donation[] basketAdjustmentDonations = basketAdjustment.getDonations();
                            if (basketAdjustmentDonations != null) {
                                for (Donation donation : basketAdjustmentDonations) {
                                    fillMerchandiseWithAdjustment(BasketUtils.calculateTotalAmount(),
                                            donation.getDescription(), donation.getDonationAmount(), false);
                                }
                            }
                        }

                        mLatestBasketAdjustedResponse = basketAdjustedEvent.generateResponse();
                        mLatestBasketAdjustedResponse.setFinalAdjustments(basketAdjustedEvent.getAdjustments(), AmountTotals.getUnsetAmountTotals());
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

                        BigDecimal currentSubtotalForAmountAdjusted = BasketUtils.calculateTotalAmount();
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
                            runningPayment.setRequestedPaymentAmount(BasketUtils.calculateTotalAmount().add(mExtraAmount));
                        }

                        mTransactionManager.sendEventResponse(amountAdjustedResponse);
                        break;
                    case LoyaltyReceivedEvent.TYPE:
                        LoyaltyReceivedEvent loyaltyReceivedEvent = (LoyaltyReceivedEvent) commerceEvent;

                        BigDecimal currentSubtotal = BasketUtils.calculateTotalAmount();
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
                            responsePayment.setRequestedPaymentAmount(BasketUtils.calculateTotalAmount().add(mExtraAmount));
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
                }
                return commerceEvent.generateResponse();
            }
        };
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

    private void onBasketAdjustedCompleted() {
        if (mLatestBasketAdjustedResponse != null) {
            BigDecimal transactionTotal = BasketUtils.calculateTotalAmount();
            AmountTotals newAmountTotals = AmountTotals.getUnsetAmountTotals();
            newAmountTotals.setRunningSubtotal(transactionTotal);
            newAmountTotals.setRunningTotal(transactionTotal);

            mLatestBasketAdjustedResponse.setFinalAdjustments(mLatestBasketAdjustedResponse.getFinalAdjustments(), newAmountTotals);
            mTransactionManager.sendEventResponse(mLatestBasketAdjustedResponse);
            mLatestBasketAdjustedResponse = null;
        }
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
        Map<String, String> deviceParams = new HashMap<>();
        deviceParams.put(TransactionManager.DEVICE_IP_ADDRESS_KEY, Utils.getTerminalIP(mContext));
        String terminalPort = Utils.getTerminalPort(mContext);
        if (!TextUtils.isEmpty(terminalPort)) {
            deviceParams.put(TransactionManager.DEVICE_IP_ADDRESS_KEY, terminalPort);
        }
        mTransactionManager.setDeviceParams(deviceParams);
    }

    public void startPaymentSession() {
        initTerminalIP();
        boolean sessionStarted = mTransactionManager.startSession(mCommerceListener);
        if (!sessionStarted) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mListener != null) {
                        mListener.sessionStartFailed();
                    }
                }
            });
        }
    }

    public void startRefundSession() {
        initTerminalIP();
        Transaction transaction = new Transaction(Transaction.REFUND_TYPE);
        mTransactionManager.startSession(mCommerceListener, transaction);

    }

    public void startVoidSession() {
        initTerminalIP();
        Transaction transaction = new Transaction(Transaction.VOID_TYPE);
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
        if (mBasketManager != null) {
            mBasketManager.purgeBasket();
            mBasketManager = null;
        }
        mTransactionManager.abort();
    }

    public void finalizeBasket() {
        if (mBasketManager != null) {
            mBasketManager.finalizeBasket();
        }
    }

    public void startPayment(Payment payment, boolean isManualPayment) {
        mExtraAmount = BigDecimal.ZERO;

        Transaction transaction = getTransaction();
        transaction.setAllowsManualEntry(isManualPayment);
        mTransactionManager.updateTransaction(transaction);

        mTransactionManager.startPayment(payment);
    }

    public void addMerchandise(Merchandise merchandise, AmountTotals amountTotals) {
        if (mBasketManager == null) {
            mBasketManager = mTransactionManager.getBasketManager();
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

    public void updateMerchandise(Merchandise merchandise, AmountTotals amountTotals) {
        if (mBasketManager == null) {
            mBasketManager = mTransactionManager.getBasketManager();
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
        if (mBasketManager == null) {
            mBasketManager = mTransactionManager.getBasketManager();
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

    public List<Merchandise> getMerchandises() {
        if (mBasketManager == null) {
            mBasketManager = mTransactionManager.getBasketManager();
        }
        if (mBasketManager == null || mBasketManager.getBasket() == null) return null;
        return Arrays.asList(mBasketManager.getBasket().getMerchandises());
    }

    /**
     * For now {@link BasketManager#addOffer(Offer, AmountTotals)} and {@link BasketManager#addDonation(Donation, AmountTotals)} not yet supported, adding
     * offers and donations as usual merchandises. For demo purposes all adjustments applies for whole basket (not from specific item).
     *
     * @param currentAmount current basket amount
     * @param description   description of item
     * @param itemAmount    amount for adjustment
     * @param isPercent     if this amount in percent representation (e.g. 0.2)
     */
    private void fillMerchandiseWithAdjustment(BigDecimal currentAmount, String description, BigDecimal itemAmount, boolean isPercent) {
        if (isPercent) {
            itemAmount = currentAmount.multiply(itemAmount);
        }

        if (currentAmount.add(itemAmount).floatValue() < TENDS_TO_ZERO_BIG_DECIMAL.floatValue()) {
            itemAmount = TENDS_TO_ZERO_BIG_DECIMAL.subtract(currentAmount);
        }
        Merchandise merchandise = new Merchandise();
        merchandise.setTax(BigDecimal.ZERO);
        merchandise.setDiscount(BigDecimal.ZERO);
        merchandise.setDescription(description);
        merchandise.setUnitPrice(itemAmount);
        merchandise.setExtendedPrice(itemAmount);
        merchandise.setAmount(itemAmount);
        merchandise.setQuantity(BigDecimal.ONE);
        merchandise.setBasketItemId(UUID.randomUUID().toString());
        merchandise.setDisplayLine(description);
        merchandise.setName(merchandise.getBasketItemId());

        addMerchandise(merchandise);
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

}

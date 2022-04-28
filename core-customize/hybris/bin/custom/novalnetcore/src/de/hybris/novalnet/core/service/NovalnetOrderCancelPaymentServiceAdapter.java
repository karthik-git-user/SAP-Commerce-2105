package de.hybris.novalnet.core.service;

import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.order.CalculationService;
import de.hybris.platform.order.exceptions.CalculationException;
import de.hybris.platform.ordercancel.OrderCancelPaymentServiceAdapter;
import de.hybris.platform.payment.PaymentService;
import de.hybris.platform.payment.model.PaymentTransactionEntryModel;
import de.hybris.platform.payment.model.PaymentTransactionModel;
import de.hybris.platform.servicelayer.model.ModelService;
import org.apache.log4j.Logger;

//~ import static com.adyen.v6.constants.Adyenv6coreConstants.PAYMENT_PROVIDER;
import static de.hybris.platform.core.enums.OrderStatus.CANCELLED;

/**
 * Used for cancellations by ImmediateCancelRequestExecutor
 */
public class NovalnetOrderCancelPaymentServiceAdapter implements OrderCancelPaymentServiceAdapter {
    private PaymentService paymentService;
    private ModelService modelService;
    private CalculationService calculationService;

    private static final Logger LOG = Logger.getLogger(NovalnetOrderCancelPaymentServiceAdapter.class);

    /**
     * Issues a cancel request for complete cancelled orders
     *
     * @param order
     */
    @Override
    public void recalculateOrderAndModifyPayments(final OrderModel order) {
        LOG.debug("recalculateOrderAndModifyPayments received for order: " + order.getCode() + ":"
                + order.getTotalPrice() + ":" + order.getStatus().getCode());
                int currentLine = new Throwable().getStackTrace()[0].getLineNumber();
                LOG.info("----------------------------------------" + currentLine);

        try {
            calculationService.recalculate(order);
        } catch (CalculationException e) {
            LOG.error(e);
        }
        
        currentLine = new Throwable().getStackTrace()[0].getLineNumber();
        LOG.info("----------------------------------------" + currentLine);

        //Send the cancel request only when the whole order is cancelled
        if (!CANCELLED.getCode().equals(order.getStatus().getCode())) {
            LOG.info("Partial cancellation - do nothing");
            return;
        }
        
        currentLine = new Throwable().getStackTrace()[0].getLineNumber();
        LOG.info("----------------------------------------" + currentLine);

        if(order.getPaymentTransactions().isEmpty()) {
            LOG.warn("No transaction found!");
            return;
        }
        
        currentLine = new Throwable().getStackTrace()[0].getLineNumber();
        LOG.info("----------------------------------------" + currentLine);
        
        final PaymentTransactionModel transaction = order.getPaymentTransactions().get(0);

        //Ignore non-Adyen payments
        //~ if (!PAYMENT_PROVIDER.equals(transaction.getPaymentProvider())) {
            //~ LOG.debug("Different Payment provider: " + transaction.getPaymentProvider());
            //~ return;
        //~ }
        
        LOG.info("----------------------------------------transaction " + transaction);

        if (transaction.getEntries().isEmpty()) {
            LOG.warn("Cannot find auth transaction!");
            return;
        }
        
        currentLine = new Throwable().getStackTrace()[0].getLineNumber();
        LOG.info("----------------------------------------" + currentLine);

        PaymentTransactionEntryModel authorizationTransaction = transaction.getEntries().get(0);

        PaymentTransactionEntryModel cancellationTransaction = paymentService.cancel(authorizationTransaction);

        LOG.info("Saving transaction " + cancellationTransaction.getRequestId()
                + ":" + cancellationTransaction.getTransactionStatus()
                + ":" + cancellationTransaction.getTransactionStatusDetails());
        modelService.save(cancellationTransaction);
    }

    public PaymentService getPaymentService() {
		int currentLine = new Throwable().getStackTrace()[0].getLineNumber();
                LOG.info("----------------------------------------" + currentLine);
        return paymentService;
    }

    public void setPaymentService(PaymentService paymentService) {
		int currentLine = new Throwable().getStackTrace()[0].getLineNumber();
                LOG.info("----------------------------------------" + currentLine);
        this.paymentService = paymentService;
    }

    public ModelService getModelService() {
		int currentLine = new Throwable().getStackTrace()[0].getLineNumber();
                LOG.info("----------------------------------------" + currentLine);
        return modelService;
    }

    public void setModelService(ModelService modelService) {
		int currentLine = new Throwable().getStackTrace()[0].getLineNumber();
                LOG.info("----------------------------------------" + currentLine);
        this.modelService = modelService;
    }

    public CalculationService getCalculationService() {
		int currentLine = new Throwable().getStackTrace()[0].getLineNumber();
                LOG.info("----------------------------------------" + currentLine);
        return calculationService;
    }

    public void setCalculationService(CalculationService calculationService) {
		int currentLine = new Throwable().getStackTrace()[0].getLineNumber();
                LOG.info("----------------------------------------" + currentLine);
        this.calculationService = calculationService;
    }
}

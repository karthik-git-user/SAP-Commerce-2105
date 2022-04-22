/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.novalnet.fulfilmentprocess.actions.order.cancel;

//~ import com.wirecard.hybris.core.service.WirecardPOExecutionService;
import de.hybris.platform.core.enums.OrderStatus;
import de.hybris.platform.core.model.order.OrderEntryModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.ordercancel.OrderCancelResponse;
import de.hybris.platform.ordercancel.model.OrderCancelRecordEntryModel;
import de.hybris.platform.orderprocessing.model.OrderProcessModel;
import de.hybris.platform.servicelayer.util.ServicesUtil;
import de.hybris.platform.yacceleratorordermanagement.actions.order.cancel.ProcessOrderCancellationAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

public class NovalnetProcessOrderCancellationAction extends ProcessOrderCancellationAction {

    private static final String START_MESSAGE = "Starting cancellation payment action ...";
    private static final String STATUS_MESSAGE = "Cancellation on order {} ended with status {}";
    private static final String PROCESS_MESSAGE = "Process: {} in step {}";

    private static final Logger LOG = LoggerFactory.getLogger(NovalnetProcessOrderCancellationAction.class);
    //~ private WirecardPOExecutionService wirecardPOExecutionService;

    @Override
    public String execute(OrderProcessModel process) throws Exception {

        LOG.info(START_MESSAGE);
        LOG.info("i am in novalnet file-------------------------------------------------------");
        ServicesUtil.validateParameterNotNullStandardMessage("process", process);
        LOG.info(PROCESS_MESSAGE, process.getCode(), getClass().getSimpleName());

        final OrderModel order = process.getOrder();
        ServicesUtil.validateParameterNotNullStandardMessage("order", order);

        final OrderCancelRecordEntryModel orderCancelRecordEntryModel = getOrderCancelService().getPendingCancelRecordEntry(order);
        final OrderCancelResponse orderCancelResponse = createOrderCancelResponseFromCancelRecordEntry(order,
                                                                                                       orderCancelRecordEntryModel);
        //~ getWirecardPOExecutionService().executeAuthorizationCancelOperation(order);

        getOrderCancelCallbackService().onOrderCancelResponse(orderCancelResponse);

        OrderStatus orderStatus = getUpdatedOrderStatus(order);
        order.setStatus(orderStatus);
        getModelService().save(order);

        LOG.info(STATUS_MESSAGE, order.getCode(), orderStatus.getCode());

        //Restricting Re-sourcing when an ON_HOLD order gets cancelled
        return calculateTransitionResult(order);
    }

    private String calculateTransitionResult(OrderModel order) {
        if (!OrderStatus.ON_HOLD.equals(order.getStatus()) && existsUnallocatedEntry(order)) {
            return Transition.SOURCING.toString();
        } else if (existsPendingEntry(order)) {
            return Transition.WAIT.toString();
        } else {
            return Transition.OK.toString();
        }

    }

    private OrderStatus getUpdatedOrderStatus(OrderModel order) {
        OrderStatus orderStatus = order.getStatus();
        if (allEntriesCancelled(order)) {
            orderStatus = OrderStatus.CANCELLED;
        } else if (!OrderStatus.ON_HOLD.equals(order.getStatus())) {
            if (existsUnallocatedEntry(order)) {
                orderStatus = OrderStatus.SUSPENDED;
            } else {
                orderStatus = OrderStatus.READY;
            }
        }
        return orderStatus;
    }

    private boolean allEntriesCancelled(OrderModel order) {
        return order.getEntries().stream()
                    .allMatch(entry -> entry.getQuantity() != null && entry.getQuantity() == 0);
    }

    private boolean existsUnallocatedEntry(OrderModel order) {
        return order.getEntries().stream()
                    .anyMatch(entry -> ((OrderEntryModel) entry).getQuantityUnallocated() > 0);

    }

    private boolean existsPendingEntry(OrderModel order) {
        return order.getEntries().stream()
                    .anyMatch(entry -> ((OrderEntryModel) entry).getQuantityPending() > 0);
    }

    //~ protected WirecardPOExecutionService getWirecardPOExecutionService() {
        //~ return wirecardPOExecutionService;
    //~ }

    //~ @Required
    //~ public void setWirecardPOExecutionService(WirecardPOExecutionService wirecardPOExecutionService) {
        //~ this.wirecardPOExecutionService = wirecardPOExecutionService;
    //~ }

}

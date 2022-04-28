package com.mirakl.hybris.core.fulfilment.strategies.impl;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.enums.OrderStatus;
import de.hybris.platform.core.model.order.AbstractOrderEntryModel;
import de.hybris.platform.payment.dto.TransactionStatus;
import de.hybris.platform.task.RetryLaterException;

/**
 * Copyright (C) 2017 Mirakl. www.mirakl.com - info@mirakl.com All Rights Reserved. Tous droits réservés.
 */

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class DefaultProcessOperatorPaymentStrategyTest extends AbstractProcessOperatorPaymentStrategyTest {

  @InjectMocks
  private DefaultProcessOperatorPaymentStrategy testObj;

  @Before
  public void setUp() throws Exception {
    super.setUp();
  }

  @Test
  public void shouldDoNothingForFullMarketplaceOrders() throws RetryLaterException, Exception {
    when(order.getOperatorEntries()).thenReturn(Collections.<AbstractOrderEntryModel>emptyList());
    when(order.getMarketplaceEntries()).thenReturn(marketplaceOrderEntries);

    boolean output = testObj.processPayment(order);

    assertThat(output).isTrue();
    verifyZeroInteractions(takePaymentService);
    verifyZeroInteractions(miraklCalculationService);
  }

  @Test
  public void shouldPerformPartialCaptureForMixedOrders() throws RetryLaterException, Exception {
    when(order.getOperatorEntries()).thenReturn(operatorOrderEntries);
    when(order.getMarketplaceEntries()).thenReturn(marketplaceOrderEntries);

    boolean output = testObj.processPayment(order);

    assertThat(output).isTrue();
    assertThat(operatorAmountArgumentCaptor.getValue()).isEqualTo(OPERATOR_AMOUNT);
    verify(miraklCalculationService).calculateOperatorAmount(order);
    verify(takePaymentService).partialCapture(order, OPERATOR_AMOUNT);
  }

  @Test
  public void shouldPerformFullCaptureForFullOperatorOrders() throws RetryLaterException, Exception {
    when(order.getOperatorEntries()).thenReturn(operatorOrderEntries);
    when(order.getMarketplaceEntries()).thenReturn(Collections.<AbstractOrderEntryModel>emptyList());

    boolean output = testObj.processPayment(order);

    assertThat(output).isTrue();
    verifyZeroInteractions(miraklCalculationService);
    verify(takePaymentService).fullCapture(order);
    verify(order).setStatus(OrderStatus.PAYMENT_CAPTURED);
  }

  @Test
  public void shouldFailOnRejectedCaptures() throws RetryLaterException, Exception {
    when(order.getOperatorEntries()).thenReturn(operatorOrderEntries);
    when(order.getMarketplaceEntries()).thenReturn(marketplaceOrderEntries);
    when(paymentTransactionEntry.getTransactionStatus()).thenReturn(TransactionStatus.REJECTED.name());

    boolean output = testObj.processPayment(order);

    assertThat(output).isFalse();
    verify(order).setStatus(OrderStatus.PAYMENT_NOT_CAPTURED);
  }
}

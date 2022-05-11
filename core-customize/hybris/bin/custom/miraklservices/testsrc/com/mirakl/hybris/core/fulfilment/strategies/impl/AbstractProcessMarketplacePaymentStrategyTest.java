package com.mirakl.hybris.core.fulfilment.strategies.impl;

import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.Ignore;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import com.mirakl.client.mmp.domain.order.state.MiraklOrderStatus;
import com.mirakl.client.mmp.domain.payment.debit.MiraklOrderPayment;
import com.mirakl.client.mmp.front.core.MiraklMarketplacePlatformFrontApi;
import com.mirakl.client.mmp.request.payment.debit.MiraklConfirmOrderDebitRequest;
import com.mirakl.hybris.core.model.MarketplaceConsignmentModel;
import com.mirakl.hybris.core.order.services.TakePaymentService;

import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.payment.dto.TransactionStatus;
import de.hybris.platform.payment.model.PaymentTransactionEntryModel;
import de.hybris.platform.servicelayer.model.ModelService;

/**
 * Copyright (C) 2017 Mirakl. www.mirakl.com - info@mirakl.com All Rights Reserved. Tous droits réservés.
 */
@Ignore
public abstract class AbstractProcessMarketplacePaymentStrategyTest {

  protected static final BigDecimal DEBIT_AMOUNT = BigDecimal.valueOf(100, 4);

  @Mock
  protected ModelService modelService;
  @Mock
  protected OrderModel order;
  @Mock
  protected MiraklOrderStatus miraklOrderStatus;
  @Mock
  protected MiraklOrderPayment miraklOrderPayment;
  @Mock
  protected MiraklMarketplacePlatformFrontApi miraklApi;
  @Mock
  protected TakePaymentService takePaymentService;
  @Mock
  protected PaymentTransactionEntryModel captureTxnEntry;
  @Captor
  protected ArgumentCaptor<MiraklConfirmOrderDebitRequest> confirmRequestCaptor;

  protected MarketplaceConsignmentModel marketplaceConsignment;

  public void setUp() {
    marketplaceConsignment = new MarketplaceConsignmentModel();
    marketplaceConsignment.setOrder(order);
    when(miraklOrderPayment.getAmount()).thenReturn(DEBIT_AMOUNT);
    when(takePaymentService.partialCapture(order, miraklOrderPayment.getAmount().doubleValue())).thenReturn(captureTxnEntry);
    when(takePaymentService.fullCapture(order)).thenReturn(captureTxnEntry);
    when(captureTxnEntry.getTransactionStatus()).thenReturn(TransactionStatus.ACCEPTED.name());
  }

}

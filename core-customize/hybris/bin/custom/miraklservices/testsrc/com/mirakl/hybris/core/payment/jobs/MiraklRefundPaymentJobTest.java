package com.mirakl.hybris.core.payment.jobs;

import static java.util.Arrays.asList;
import static org.fest.assertions.Assertions.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.mirakl.client.mmp.domain.payment.refund.MiraklOrderLineRefund;
import com.mirakl.client.mmp.front.core.MiraklMarketplacePlatformFrontApi;
import com.mirakl.client.mmp.request.payment.debit.MiraklConfirmOrderRefundRequest;
import com.mirakl.hybris.core.model.MiraklRefundPaymentCronJobModel;
import com.mirakl.hybris.core.returns.strategies.MiraklRefundLookupStrategy;
import com.mirakl.hybris.core.returns.strategies.MiraklRefundProcessingStrategy;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.model.c2l.CurrencyModel;
import de.hybris.platform.cronjob.enums.CronJobResult;
import de.hybris.platform.cronjob.enums.CronJobStatus;
import de.hybris.platform.payment.dto.TransactionStatus;
import de.hybris.platform.payment.model.PaymentTransactionEntryModel;
import de.hybris.platform.returns.model.RefundEntryModel;
import de.hybris.platform.servicelayer.cronjob.PerformResult;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import de.hybris.platform.servicelayer.model.ModelService;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class MiraklRefundPaymentJobTest {

  private static final String CURRENCY_ISO_CODE = "USD";

  @InjectMocks
  private MiraklRefundPaymentJob job;

  @Mock
  private ModelService modelService;
  @Mock
  private MiraklRefundLookupStrategy miraklRefundLookupStrategy;
  @Mock
  private MiraklRefundProcessingStrategy miraklRefundProcessingStrategy;
  @Mock
  private MiraklRefundPaymentCronJobModel cronJob;
  @Mock
  private MiraklMarketplacePlatformFrontApi miraklApi;
  @Mock
  private RefundEntryModel pendingRefund1, pendingRefund2, refundWaitingConfirmation;
  @Mock
  private PaymentTransactionEntryModel paymentTransactionEntry;
  @Mock
  private CurrencyModel currency;
  @Mock
  private Converter<RefundEntryModel, MiraklOrderLineRefund> miraklOrderLineRefundConverter;

  private List<RefundEntryModel> refundsWaitingConfirmation;

  @Before
  public void setUp() {
    when(miraklRefundLookupStrategy.getRefundEntriesPendingPayment()).thenReturn(asList(pendingRefund1, pendingRefund2));
    when(miraklRefundLookupStrategy.getProcessedRefundEntriesPendingConfirmation()).thenReturn(asList(refundWaitingConfirmation));
    when(refundWaitingConfirmation.getPaymentTransactionEntry()).thenReturn(paymentTransactionEntry);
    when(paymentTransactionEntry.getTransactionStatus()).thenReturn(TransactionStatus.ACCEPTED.name());
    when(refundWaitingConfirmation.getCurrency()).thenReturn(currency);
    when(currency.getIsocode()).thenReturn(CURRENCY_ISO_CODE);
  }


  @SuppressWarnings("unchecked")
  @Test
  public void performRefunds() {
    PerformResult performResult = job.perform(cronJob);

    verify(miraklRefundProcessingStrategy).processRefund(pendingRefund1);
    verify(miraklRefundProcessingStrategy).processRefund(pendingRefund2);
    verify(miraklApi).confirmOrderRefund(any(MiraklConfirmOrderRefundRequest.class));
    verify(refundWaitingConfirmation).setConfirmedToMirakl(true);
    verify(modelService).saveAll((Collection<RefundEntryModel>) argThat(hasItem(refundWaitingConfirmation)));

    assertThat(performResult.getResult()).isEqualTo(CronJobResult.SUCCESS);
    assertThat(performResult.getStatus()).isEqualTo(CronJobStatus.FINISHED);
  }


}

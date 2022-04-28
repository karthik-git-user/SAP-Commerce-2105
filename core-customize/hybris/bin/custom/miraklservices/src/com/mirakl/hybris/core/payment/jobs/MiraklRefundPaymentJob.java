package com.mirakl.hybris.core.payment.jobs;

import com.mirakl.client.mmp.domain.payment.refund.MiraklOrderLineRefund;
import com.mirakl.client.mmp.front.core.MiraklMarketplacePlatformFrontApi;
import com.mirakl.client.mmp.request.payment.debit.MiraklConfirmOrderRefundRequest;
import com.mirakl.hybris.core.model.MiraklRefundPaymentCronJobModel;
import com.mirakl.hybris.core.returns.strategies.MiraklRefundLookupStrategy;
import com.mirakl.hybris.core.returns.strategies.MiraklRefundProcessingStrategy;
import de.hybris.platform.cronjob.enums.CronJobResult;
import de.hybris.platform.cronjob.enums.CronJobStatus;
import de.hybris.platform.returns.model.RefundEntryModel;
import de.hybris.platform.servicelayer.cronjob.AbstractJobPerformable;
import de.hybris.platform.servicelayer.cronjob.PerformResult;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

public class MiraklRefundPaymentJob extends AbstractJobPerformable<MiraklRefundPaymentCronJobModel> {

  private static final Logger LOG = Logger.getLogger(MiraklRefundPaymentJob.class);

  protected MiraklRefundLookupStrategy refundLookupStrategy;

  protected MiraklRefundProcessingStrategy refundProcessingStrategy;

  protected MiraklMarketplacePlatformFrontApi miraklApi;

  protected Converter<RefundEntryModel, MiraklOrderLineRefund> miraklOrderLineRefundConverter;

  @Override
  public PerformResult perform(MiraklRefundPaymentCronJobModel cronJob) {
    processWaitingEntries();
    confirmRefundedEntries();

    return new PerformResult(CronJobResult.SUCCESS, CronJobStatus.FINISHED);
  }

  protected void processWaitingEntries() {
    List<RefundEntryModel> pendingRefundEntries = refundLookupStrategy.getRefundEntriesPendingPayment();

    if (isNotEmpty(pendingRefundEntries)) {
      if (LOG.isDebugEnabled()) {
        LOG.debug(format("Found [%d] refund entries waiting to be processed", pendingRefundEntries.size()));
      }

      for (RefundEntryModel refundEntry : pendingRefundEntries) {
        try {
          refundProcessingStrategy.processRefund(refundEntry);
        } catch (Exception e) {
          LOG.error(format("Unable to process refund entry [%s]", refundEntry.getMiraklRefundId()), e);
        }
      }
    }
  }

  protected void confirmRefundedEntries() {
    List<RefundEntryModel> processedRefunds = refundLookupStrategy.getProcessedRefundEntriesPendingConfirmation();
    if (isNotEmpty(processedRefunds)) {
      if (LOG.isDebugEnabled()) {
        LOG.debug(format("Found [%d] refund entries already processed and waiting confirmation", processedRefunds.size()));
      }

      List<MiraklOrderLineRefund> miraklOrderLineRefunds = new ArrayList<>();
      for (RefundEntryModel refundEntry : processedRefunds) {
        miraklOrderLineRefunds.add(miraklOrderLineRefundConverter.convert(refundEntry));
        refundEntry.setConfirmedToMirakl(true);
      }

      if (LOG.isDebugEnabled()) {
        LOG.debug(format("Sending [%d] refund confirmation to Mirakl", miraklOrderLineRefunds.size()));
      }
      miraklApi.confirmOrderRefund(new MiraklConfirmOrderRefundRequest(miraklOrderLineRefunds));
      modelService.saveAll(processedRefunds);
    }

  }

  @Required
  public void setRefundLookupStrategy(MiraklRefundLookupStrategy refundLookupStrategy) {
    this.refundLookupStrategy = refundLookupStrategy;
  }

  @Required
  public void setRefundProcessingStrategy(MiraklRefundProcessingStrategy refundProcessingStrategy) {
    this.refundProcessingStrategy = refundProcessingStrategy;
  }

  @Required
  public void setMiraklApi(MiraklMarketplacePlatformFrontApi miraklApi) {
    this.miraklApi = miraklApi;
  }

  @Required
  public void setMiraklOrderLineRefundConverter(
      Converter<RefundEntryModel, MiraklOrderLineRefund> miraklOrderLineRefundConverter) {
    this.miraklOrderLineRefundConverter = miraklOrderLineRefundConverter;
  }
}

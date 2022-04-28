package com.mirakl.hybris.core.product.jobs;

import java.util.Collection;
import java.util.Date;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;

import com.mirakl.client.core.exception.MiraklApiException;
import com.mirakl.hybris.core.model.MiraklImportOffersCronJobModel;
import com.mirakl.hybris.core.model.OfferModel;
import com.mirakl.hybris.core.product.services.OfferImportService;

import de.hybris.platform.cronjob.enums.CronJobResult;
import de.hybris.platform.cronjob.enums.CronJobStatus;
import de.hybris.platform.servicelayer.cronjob.AbstractJobPerformable;
import de.hybris.platform.servicelayer.cronjob.PerformResult;

public class MiraklImportOffersJob extends AbstractJobPerformable<MiraklImportOffersCronJobModel> {

  private static final Logger LOG = Logger.getLogger(MiraklImportOffersJob.class);

  protected OfferImportService offerImportService;

  @Override
  public PerformResult perform(MiraklImportOffersCronJobModel cronJob) {
    Date lastImportTime = cronJob.getLastImportTime();
    Date jobStartTime = cronJob.getStartTime();
    Collection<OfferModel> importedOffers;

    try {
      if (cronJob.isFullImport() || lastImportTime == null) {
        LOG.info("Performing a FULL offers import");
        importedOffers = offerImportService.importAllOffers(jobStartTime, cronJob.isIncludeInactiveOffers());
      } else {
        LOG.info(String.format("Importing offers updated after %s", lastImportTime));
        importedOffers = offerImportService.importOffersUpdatedSince(lastImportTime);
      }
      cronJob.setLastImportTime(jobStartTime);
    } catch (MiraklApiException e) {
      LOG.error("Exception occurred while importing offers", e);
      return new PerformResult(CronJobResult.ERROR, CronJobStatus.ABORTED);
    }

    LOG.info(String.format("Imported %d offers", importedOffers.size()));
    modelService.save(cronJob);

    return new PerformResult(CronJobResult.SUCCESS, CronJobStatus.FINISHED);
  }

  @Required
  public void setOfferImportService(OfferImportService offerImportService) {
    this.offerImportService = offerImportService;
  }

}

package com.mirakl.hybris.core.product.strategies.impl;

import com.mirakl.client.mmp.domain.offer.price.MiraklOfferPricing;
import com.mirakl.hybris.core.model.OfferModel;
import com.mirakl.hybris.core.product.services.OfferService;
import com.mirakl.hybris.core.product.strategies.OfferPricingSelectionStrategy;
import org.springframework.beans.factory.annotation.Required;

import java.util.List;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.isBlank;

public class DefaultOfferPricingSelectionStrategy implements OfferPricingSelectionStrategy {
  protected OfferService offerService;

  @Override
  public MiraklOfferPricing selectApplicableOfferPricing(OfferModel offer) {
    return selectApplicableOfferPricing(offerService.loadAllOfferPricings(offer));
  }

  @Override
  public MiraklOfferPricing selectApplicableOfferPricing(List<MiraklOfferPricing> offerPricings) {
    if (isEmpty(offerPricings)) {
      return null;
    }

    for (MiraklOfferPricing offerPricing : offerPricings) {
      if (isBlank(offerPricing.getChannelCode())) {
        return offerPricing;
      }
    }
    return null;
  }

  @Required
  public void setOfferService(OfferService offerService) {
    this.offerService = offerService;
  }

}

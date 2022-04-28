package com.mirakl.hybris.core.product.strategies;

import java.util.List;

import com.mirakl.client.mmp.domain.offer.price.MiraklOfferPricing;
import com.mirakl.hybris.core.model.OfferModel;

public interface OfferPricingSelectionStrategy {

  /**
   * Selects the applicable offer pricing among those stored within a given offer
   * 
   * @param offer
   * @return applicable offer pricing
   */
  MiraklOfferPricing selectApplicableOfferPricing(OfferModel offer);

  /**
   * Selects the applicable offer pricing
   * 
   * @param offerPricings
   * @return applicable offer pricing
   */
  MiraklOfferPricing selectApplicableOfferPricing(List<MiraklOfferPricing> offerPricings);

}

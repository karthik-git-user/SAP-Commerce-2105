package com.mirakl.hybris.channels.product.strategies.impl;

import static org.apache.commons.collections.CollectionUtils.isEmpty;

import java.util.List;

import org.springframework.beans.factory.annotation.Required;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.mirakl.client.mmp.domain.offer.price.MiraklOfferPricing;
import com.mirakl.hybris.channels.channel.services.MiraklChannelService;
import com.mirakl.hybris.channels.model.MiraklChannelModel;
import com.mirakl.hybris.core.model.OfferModel;
import com.mirakl.hybris.core.product.services.OfferService;
import com.mirakl.hybris.core.product.strategies.OfferPricingSelectionStrategy;

public class DefaultChannelOfferPricingSelectionStrategy implements OfferPricingSelectionStrategy {
  protected OfferService offerService;
  protected MiraklChannelService miraklChannelService;

  @Override
  public MiraklOfferPricing selectApplicableOfferPricing(OfferModel offer) {
    return selectApplicableOfferPricing(offerService.loadAllOfferPricings(offer));
  }

  @Override
  public MiraklOfferPricing selectApplicableOfferPricing(List<MiraklOfferPricing> offerPricings) {
    if (isEmpty(offerPricings)) {
      return null;
    }

    final MiraklChannelModel channel = miraklChannelService.getCurrentMiraklChannel();
    return FluentIterable.from(offerPricings).firstMatch(offerPricingChannelPredicate(channel)).orNull();
  }

  protected Predicate<MiraklOfferPricing> offerPricingChannelPredicate(final MiraklChannelModel channel) {
    return new Predicate<MiraklOfferPricing>() {

      @Override
      public boolean apply(MiraklOfferPricing miraklOfferPricing) {
        if (channel == null) {
          return miraklOfferPricing.getChannelCode() == null;
        }
        return channel.getCode().equals(miraklOfferPricing.getChannelCode());
      }
    };
  }

  @Required
  public void setOfferService(OfferService offerService) {
    this.offerService = offerService;
  }

  @Required
  public void setMiraklChannelService(MiraklChannelService miraklChannelService) {
    this.miraklChannelService = miraklChannelService;
  }

}

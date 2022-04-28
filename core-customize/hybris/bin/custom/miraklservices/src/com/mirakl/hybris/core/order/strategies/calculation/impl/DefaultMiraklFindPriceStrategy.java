package com.mirakl.hybris.core.order.strategies.calculation.impl;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isBlank;

import org.springframework.beans.factory.annotation.Required;

import com.mirakl.client.mmp.domain.offer.price.MiraklVolumePrice;
import com.mirakl.hybris.core.model.OfferModel;
import com.mirakl.hybris.core.product.services.MiraklPriceService;
import com.mirakl.hybris.core.product.services.OfferService;

import de.hybris.platform.core.model.order.AbstractOrderEntryModel;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.order.exceptions.CalculationException;
import de.hybris.platform.order.strategies.calculation.FindPriceStrategy;
import de.hybris.platform.search.restriction.SearchRestrictionService;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;
import de.hybris.platform.servicelayer.session.SessionExecutionBody;
import de.hybris.platform.servicelayer.session.SessionService;
import de.hybris.platform.util.PriceValue;

public class DefaultMiraklFindPriceStrategy implements FindPriceStrategy {

  protected OfferService offerService;
  protected SessionService sessionService;
  protected SearchRestrictionService searchRestrictionService;
  protected MiraklPriceService miraklPriceService;

  @Override
  public PriceValue findBasePrice(AbstractOrderEntryModel entry) throws CalculationException {
    final String offerId = entry.getOfferId();
    if (isBlank(offerId)) {
      throw new IllegalArgumentException(
          format("[%s] should only be used for marketplace order entries. No offer id was present on order entry [pk=%s]",
              this.getClass().getSimpleName(), entry.getPk()));
    }

    final AbstractOrderModel order = entry.getOrder();
    return sessionService.executeInLocalView(new SessionExecutionBody() {
      @Override
      public Object execute() {
        try {
          searchRestrictionService.disableSearchRestrictions();
          return createPriceValue(offerId, entry.getQuantity(), order.getNet().booleanValue());
        } catch (UnknownIdentifierException e) {
          throw new IllegalStateException(e);
        } finally {
          searchRestrictionService.enableSearchRestrictions();
        }
      }
    });

  }

  protected PriceValue createPriceValue(final String offerId, final long quantity, final boolean net) {
    OfferModel offer = offerService.getOfferForIdIgnoreSearchRestrictions(offerId);
    MiraklVolumePrice volumePrice = miraklPriceService.getVolumePriceForQuantity(offer, quantity);
    if (volumePrice == null) {
      return new PriceValue(offer.getCurrency().getIsocode(), offer.getEffectiveBasePrice().doubleValue(), net);
    }

    return new PriceValue(offer.getCurrency().getIsocode(), volumePrice.getPrice().doubleValue(), net);
  }

  @Required
  public void setOfferService(OfferService offerService) {
    this.offerService = offerService;
  }

  @Required
  public void setSessionService(SessionService sessionService) {
    this.sessionService = sessionService;
  }

  @Required
  public void setSearchRestrictionService(SearchRestrictionService searchRestrictionService) {
    this.searchRestrictionService = searchRestrictionService;
  }

  @Required
  public void setMiraklPriceService(MiraklPriceService miraklPriceService) {
    this.miraklPriceService = miraklPriceService;
  }

}

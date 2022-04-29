package com.mirakl.hybris.facades.product.impl;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNullStandardMessage;
import static java.lang.String.format;

import java.util.List;

import org.springframework.beans.factory.annotation.Required;

import com.mirakl.hybris.beans.OfferData;
import com.mirakl.hybris.core.model.OfferModel;
import com.mirakl.hybris.core.product.services.OfferService;
import com.mirakl.hybris.core.product.strategies.OfferCodeGenerationStrategy;
import com.mirakl.hybris.facades.product.OfferFacade;

import de.hybris.platform.servicelayer.dto.converter.Converter;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;

public class DefaultOfferFacade implements OfferFacade {

  protected Converter<OfferModel, OfferData> offerConverter;
  protected OfferService offerService;
  protected OfferCodeGenerationStrategy offerCodeGenerationStrategy;

  @Override
  public List<OfferData> getOffersForProductCode(String productCode) {
    return offerConverter.convertAll(offerService.getSortedOffersForProductCode(productCode));
  }

  @Override
  public OfferModel getOfferForCode(String offerCode) {
    validateParameterNotNullStandardMessage("code", offerCode);
    if (!offerCodeGenerationStrategy.isOfferCode(offerCode)) {
      throw new UnknownIdentifierException(format("[%s] is not an offer code", offerCode));
    }

    return offerService.getOfferForId(offerCodeGenerationStrategy.translateCodeToId(offerCode));
  }

  @Override
  public OfferModel getOfferForCodeIgnoreSearchRestrictions(String offerCode) {
    validateParameterNotNullStandardMessage("code", offerCode);
    if (!offerCodeGenerationStrategy.isOfferCode(offerCode)) {
      throw new UnknownIdentifierException(format("[%s] is not an offer code", offerCode));
    }
    String offerId = offerCodeGenerationStrategy.translateCodeToId(offerCode);

    return offerService.getOfferForIdIgnoreSearchRestrictions(offerId);
  }

  @Required
  public void setOfferConverter(Converter<OfferModel, OfferData> offerConverter) {
    this.offerConverter = offerConverter;
  }

  @Required
  public void setOfferService(OfferService offerService) {
    this.offerService = offerService;
  }

  @Required
  public void setOfferCodeGenerationStrategy(OfferCodeGenerationStrategy offerCodeGenerationStrategy) {
    this.offerCodeGenerationStrategy = offerCodeGenerationStrategy;
  }

}

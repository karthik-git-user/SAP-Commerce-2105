package com.mirakl.hybris.core.product.attributes;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Required;

import com.mirakl.hybris.core.model.OfferModel;
import com.mirakl.hybris.core.product.services.MiraklPriceService;

import de.hybris.platform.servicelayer.model.attribute.AbstractDynamicAttributeHandler;

public class DefaultEffectiveOriginPriceDynamicHandler extends AbstractDynamicAttributeHandler<BigDecimal, OfferModel> {

  protected MiraklPriceService miraklPriceService;

  @Override
  public BigDecimal get(OfferModel model) {
    return miraklPriceService.getOfferOriginPrice(model);
  }

  @Required
  public void setMiraklPriceService(MiraklPriceService miraklPriceService) {
    this.miraklPriceService = miraklPriceService;
  }

}

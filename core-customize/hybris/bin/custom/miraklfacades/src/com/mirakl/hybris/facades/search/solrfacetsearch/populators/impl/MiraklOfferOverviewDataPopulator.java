package com.mirakl.hybris.facades.search.solrfacetsearch.populators.impl;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNullStandardMessage;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Required;

import com.mirakl.hybris.beans.OfferOverviewData;
import com.mirakl.hybris.core.model.OfferModel;
import com.mirakl.hybris.core.product.strategies.OfferCodeGenerationStrategy;
import com.mirakl.hybris.facades.product.helpers.PriceDataFactoryHelper;

import de.hybris.platform.converters.Populator;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;

public class MiraklOfferOverviewDataPopulator implements Populator<OfferModel, OfferOverviewData> {

  protected static final int MIN_PURCHASABLE_QTY_ERROR = -1;
  protected static final int MIN_PURCHASABLE_QTY_DEFAULT = 1;

  protected OfferCodeGenerationStrategy offerCodeGenerationStrategy;
  protected PriceDataFactoryHelper priceDataFactoryHelper;

  @Override
  public void populate(OfferModel source, OfferOverviewData target) throws ConversionException {
    validateParameterNotNullStandardMessage("source", source);

    target.setCode(offerCodeGenerationStrategy.generateCode(source.getId()));
    target.setQuantity(source.getQuantity());
    target.setShopId(source.getShop().getId());
    target.setShopGrade(source.getShop().getGrade());
    target.setShopName(source.getShop().getName());
    target.setMinPurchasableQty(getMinPurchasableQty(source));
    target.setStateCode(source.getState() != null ? source.getState().getCode() : null);
    populatePrices(source, target);
  }

  protected void populatePrices(OfferModel source, OfferOverviewData target) {
    BigDecimal offerBasePrice = source.getEffectiveBasePrice();
    if (offerBasePrice != null) {
      target.setPrice(priceDataFactoryHelper.createPrice(offerBasePrice));
    }
    BigDecimal effectiveOriginPrice = source.getEffectiveOriginPrice();
    if (effectiveOriginPrice != null) {
      target.setOriginPrice(priceDataFactoryHelper.createPrice(effectiveOriginPrice));
    }
    if (source.getMinShippingPrice() != null) {
      target.setMinShippingPrice(priceDataFactoryHelper.createPrice(source.getMinShippingPrice()));
    }
    BigDecimal effectiveTotalPrice = source.getEffectiveTotalPrice();
    if (effectiveTotalPrice != null) {
      target.setTotalPrice(priceDataFactoryHelper.createPrice(effectiveTotalPrice));
    }
    target.setAllOfferPricingsJSON(source.getAllOfferPricingsJSON());
  }

  protected int getMinPurchasableQty(OfferModel offer) {
    int minPurchasableQty = MIN_PURCHASABLE_QTY_DEFAULT;
    if (greaterThanZero(offer.getMinOrderQuantity())) {
      minPurchasableQty = offer.getMinOrderQuantity();
    }
    if (greaterThanZero(offer.getPackageQuantity()) && minPurchasableQty % offer.getPackageQuantity() != 0) {
      minPurchasableQty = minPurchasableQty + offer.getPackageQuantity() - minPurchasableQty % offer.getPackageQuantity();
    }
    if (greaterThanZero(offer.getMaxOrderQuantity()) && offer.getMaxOrderQuantity() < minPurchasableQty) {
      return MIN_PURCHASABLE_QTY_ERROR;
    }
    return minPurchasableQty;
  }

  protected boolean greaterThanZero(Integer value) {
    return value != null && value > 0;
  }

  @Required
  public void setOfferCodeGenerationStrategy(OfferCodeGenerationStrategy offerCodeGenerationStrategy) {
    this.offerCodeGenerationStrategy = offerCodeGenerationStrategy;
  }

  @Required
  public void setPriceDataFactoryHelper(PriceDataFactoryHelper priceDataFactoryHelper) {
    this.priceDataFactoryHelper = priceDataFactoryHelper;
  }
}

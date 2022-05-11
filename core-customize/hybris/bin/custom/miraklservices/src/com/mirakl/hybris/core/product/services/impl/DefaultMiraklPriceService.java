package com.mirakl.hybris.core.product.services.impl;

import static java.util.Arrays.asList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Required;

import com.mirakl.client.mmp.domain.offer.price.MiraklOfferPricing;
import com.mirakl.client.mmp.domain.offer.price.MiraklVolumePrice;
import com.mirakl.hybris.beans.OfferOverviewData;
import com.mirakl.hybris.core.model.OfferModel;
import com.mirakl.hybris.core.product.services.MiraklPriceService;
import com.mirakl.hybris.core.product.services.MiraklProductService;
import com.mirakl.hybris.core.product.services.OfferService;
import com.mirakl.hybris.core.product.strategies.OfferPricingSelectionStrategy;

import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.europe1.jalo.PriceRow;
import de.hybris.platform.jalo.order.OrderManager;
import de.hybris.platform.jalo.order.price.PriceFactory;
import de.hybris.platform.jalo.order.price.PriceInformation;
import de.hybris.platform.jalo.user.User;
import de.hybris.platform.product.impl.DefaultPriceService;
import de.hybris.platform.servicelayer.user.UserService;
import de.hybris.platform.util.PriceValue;

public class DefaultMiraklPriceService extends DefaultPriceService implements MiraklPriceService {

  protected MiraklProductService miraklProductService;
  protected OfferService offerService;
  protected UserService userService;
  protected OfferPricingSelectionStrategy offerPricingSelectionStrategy;

  @Override
  public List<PriceInformation> getPriceInformationsForProduct(ProductModel product) {
    if (miraklProductService.isSellableByOperator(product)) {
      return super.getPriceInformationsForProduct(product);
    }

    List<OfferModel> sortedOffers = offerService.getSortedOffersForProductCode(product.getCode());
    if (isEmpty(sortedOffers)) {
      return Collections.emptyList();
    }

    return getPriceInformationsForOffer(sortedOffers.get(0));
  }

  @Override
  public List<PriceInformation> getPriceInformationsForOffer(OfferModel offer) {
    List<MiraklVolumePrice> volumePrices = getVolumePrices(offer);
    if (isEmpty(volumePrices)) {
      return asList(new PriceInformation(
          new PriceValue(offer.getCurrency().getIsocode(), offer.getEffectiveBasePrice().doubleValue(), isNet())));
    }

    List<PriceInformation> priceInformations = new ArrayList<>();

    for (MiraklVolumePrice miraklVolumePrice : volumePrices) {
      HashMap<Object, Object> qualifiers = new HashMap<>();
      if (miraklVolumePrice.getQuantityThreshold() != null) {
        qualifiers.put(PriceRow.MINQTD, miraklVolumePrice.getQuantityThreshold().longValue());
      }
      priceInformations.add(new PriceInformation(qualifiers,
          new PriceValue(offer.getCurrency().getIsocode(), miraklVolumePrice.getPrice().doubleValue(), isNet())));
    }
    return priceInformations;
  }

  @Override
  public List<MiraklVolumePrice> getVolumePrices(OfferModel offer) {
    MiraklOfferPricing applicableOfferPricing = offerPricingSelectionStrategy.selectApplicableOfferPricing(offer);

    if (applicableOfferPricing != null && isNotEmpty(applicableOfferPricing.getVolumePrices())
        && applicableOfferPricing.getVolumePrices().size() > 1) {
      return applicableOfferPricing.getVolumePrices();
    }

    return Collections.emptyList();
  }

  @Override
  public MiraklVolumePrice getVolumePriceForQuantity(OfferModel offer, long quantity) {
    return getVolumePriceForQuantity(getVolumePrices(offer), quantity);
  }

  @Override
  public MiraklVolumePrice getVolumePriceForQuantity(List<MiraklVolumePrice> volumePrices, long quantity) {
    if (isEmpty(volumePrices)) {
      return null;
    }
    MiraklVolumePrice applicableVolumePrice = null;
    for (MiraklVolumePrice volumePrice : volumePrices) {
      if (applicableVolumePrice == null || quantity >= volumePrice.getQuantityThreshold()) {
        applicableVolumePrice = volumePrice;
      }
    }
    return applicableVolumePrice;
  }

  @Override
  public BigDecimal getOfferBasePrice(OfferModel offer) {
    MiraklOfferPricing applicableOfferPricing = offerPricingSelectionStrategy.selectApplicableOfferPricing(offer);

    return applicableOfferPricing != null ? applicableOfferPricing.getPrice() : offer.getPrice();
  }

  @Override
  public BigDecimal getOfferBasePrice(OfferOverviewData offer) {
    MiraklOfferPricing applicableOfferPricing =
        offerPricingSelectionStrategy.selectApplicableOfferPricing(offerService.loadAllOfferPricings(offer));

    return applicableOfferPricing != null ? applicableOfferPricing.getPrice() : offer.getPrice().getValue();
  }

  @Override
  public BigDecimal getOfferTotalPrice(OfferModel offer) {
    if (offer.getMinShippingPrice() != null) {
      return getOfferBasePrice(offer).add(offer.getMinShippingPrice());
    }
    return getOfferBasePrice(offer);
  }

  @Override
  public BigDecimal getOfferTotalPrice(OfferOverviewData offer) {
    if (offer.getMinShippingPrice() != null) {
      return getOfferBasePrice(offer).add(offer.getMinShippingPrice().getValue());
    }
    return getOfferBasePrice(offer);
  }

  @Override
  public BigDecimal getOfferDiscountPrice(OfferModel offer) {
    MiraklOfferPricing applicableOfferPricing = offerPricingSelectionStrategy.selectApplicableOfferPricing(offer);

    return applicableOfferPricing != null ? applicableOfferPricing.getUnitDiscountPrice() : offer.getDiscountPrice();
  }

  @Override
  public BigDecimal getOfferOriginPrice(OfferModel offer) {
    MiraklOfferPricing applicableOfferPricing = offerPricingSelectionStrategy.selectApplicableOfferPricing(offer);

    return applicableOfferPricing != null ? applicableOfferPricing.getUnitOriginPrice() : offer.getOriginPrice();
  }

  @Override
  public BigDecimal getOfferOriginPrice(OfferOverviewData offer) {
    MiraklOfferPricing applicableOfferPricing =
        offerPricingSelectionStrategy.selectApplicableOfferPricing(offerService.loadAllOfferPricings(offer));

    if (applicableOfferPricing != null) {
      return applicableOfferPricing.getUnitOriginPrice();
    }

    return offer.getOriginPrice() != null ? offer.getOriginPrice().getValue() : null;
  }

  @Override
  public BigDecimal getOfferUnitPriceForQuantity(OfferModel offer, long quantity) {
    MiraklVolumePrice volumePrice = getVolumePriceForQuantity(offer, quantity);
    return volumePrice != null ? volumePrice.getPrice() : offer.getEffectiveBasePrice();
  }

  protected boolean isNet() {
    final UserModel currentUser = userService.getCurrentUser();
    final User userItem = getModelService().getSource(currentUser);
    final PriceFactory pricefactory = OrderManager.getInstance().getPriceFactory();
    return Boolean.valueOf(pricefactory.isNetUser(userItem));
  }

  @Required
  public void setMiraklProductService(MiraklProductService miraklProductService) {
    this.miraklProductService = miraklProductService;
  }

  @Required
  public void setOfferService(OfferService offerService) {
    this.offerService = offerService;
  }

  @Override
  @Required
  public void setUserService(UserService userService) {
    this.userService = userService;
  }

  @Required
  public void setOfferPricingSelectionStrategy(OfferPricingSelectionStrategy offerPricingSelectionStrategy) {
    this.offerPricingSelectionStrategy = offerPricingSelectionStrategy;
  }

}

package com.mirakl.hybris.core.product.services;

import java.math.BigDecimal;
import java.util.List;

import com.mirakl.client.mmp.domain.offer.price.MiraklVolumePrice;
import com.mirakl.hybris.beans.OfferOverviewData;
import com.mirakl.hybris.core.model.OfferModel;

import de.hybris.platform.jalo.order.price.PriceInformation;
import de.hybris.platform.product.PriceService;

public interface MiraklPriceService extends PriceService {

  /**
   * Transforms all Mirakl volume prices into {@link PriceInformation} for the given {@link OfferModel} and the current session
   * user.
   * 
   * @param offer
   * @return an empty list if no volume price exists.
   */
  List<PriceInformation> getPriceInformationsForOffer(OfferModel offer);

  /**
   * Returns all available {@link MiraklVolumePrice} for the given {@link OfferModel} and the current Mirakl channel.
   * 
   * @param offer
   * @return an empty list if no mirakl volume price exists for the offer and current Mirakl channel.
   */
  List<MiraklVolumePrice> getVolumePrices(OfferModel offer);

  /**
   * Returns the effective offer base price
   * 
   * @param offer
   * @return the current offer base price
   */
  BigDecimal getOfferBasePrice(OfferModel offer);

  /**
   * Returns the effective offer base price
   * 
   * @param offer
   * @return the current offer base price
   */
  BigDecimal getOfferBasePrice(OfferOverviewData offer);

  /**
   * Returns the effective offer total price. It is the sum of the base price and the minimum shipping cost
   * 
   * @param offer
   * @return the current offer total price
   */
  BigDecimal getOfferTotalPrice(OfferModel offer);

  /**
   * Returns the effective offer total price. It is the sum of the base price and the minimum shipping cost
   * 
   * @param offer
   * @return the current offer total price
   */
  BigDecimal getOfferTotalPrice(OfferOverviewData offer);

  /**
   * Returns the effective offer discount price
   * 
   * @param offer
   * @return the current offer discount price
   */
  BigDecimal getOfferDiscountPrice(OfferModel offer);

  /**
   * Returns the effectrive offer origin price
   * 
   * @param offer
   * @return the current offer origin price
   */
  BigDecimal getOfferOriginPrice(OfferModel offer);

  /**
   * Returns the effective offer origin price
   * 
   * @param offer
   * @return the current offer origin price
   */
  BigDecimal getOfferOriginPrice(OfferOverviewData offer);

  /**
   * Returns the volume price corresponding to a given quantity
   * 
   * @param offer
   * @param quantity
   * @return the volume price corresponding to the quantity or null if not found
   */
  MiraklVolumePrice getVolumePriceForQuantity(OfferModel offer, long quantity);

  /**
   * Returns the volume price corresponding to a given quantity
   * 
   * @param volumePrices
   * @param quantity
   * @return the volume price corresponding to the quantity or null if not found
   */
  MiraklVolumePrice getVolumePriceForQuantity(List<MiraklVolumePrice> volumePrices, long quantity);

  /**
   * Returns the offer unit price for a given quantity, taking into account the channels and volume prices
   * 
   * @param quantity
   * @param offer
   * @return a unitary price for an offer
   */
  BigDecimal getOfferUnitPriceForQuantity(OfferModel offer, long quantity);


}

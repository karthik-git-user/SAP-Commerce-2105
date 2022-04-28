package com.mirakl.hybris.core.product.daos;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.mirakl.hybris.core.enums.OfferState;
import com.mirakl.hybris.core.model.OfferModel;

import de.hybris.platform.core.model.c2l.CurrencyModel;
import de.hybris.platform.servicelayer.internal.dao.GenericDao;

public interface OfferDao extends GenericDao<OfferModel> {

  /**
   * Returns the offer with the specified id.
   * 
   * @param offerId the id of the offer
   * @return the offer with the specified id.
   * 
   */
  OfferModel findOfferById(String offerId);

  /**
   * Returns the offer with the specified id. Ignores all query decorators if any
   * 
   * @param offerId the id of the offer
   * @param ignoreQueryDecorators ignore additional filters added by query decorators
   * @return the offer with the specified id.
   * 
   */
  OfferModel findOfferById(String offerId, boolean ignoreQueryDecorators);

  /**
   * Returns all the un-deleted offers modified before the specified Date.
   * 
   * @param modificationDate the date of the last modification of the offer
   * @return a list of OfferModel. Empty if no offers are found.
   * 
   */
  List<OfferModel> findUndeletedOffersModifiedBeforeDate(Date modificationDate);

  /**
   * Returns all the offers for a specific productCode.
   * 
   * @param productCode
   * @return a list of OfferModel for the given productCode. Empty if no offers are found.
   *
   */
  List<OfferModel> findOffersForProductCode(String productCode);

  /**
   * Returns all the offers for a specific productCode and the given currency.
   * 
   * @param productCode
   * @param offerCurrency
   * @return a list of OfferModel. Empty if no offers are found.
   * 
   */
  List<OfferModel> findOffersForProductCodeAndCurrency(String productCode, CurrencyModel offerCurrency);

  /**
   * Returns a list of pairs of offer states and currencies for a product
   *
   * @param productCode
   * @return a list list of pairs of offer states and currencies. Empty if the product has no offers.
   *
   */
  List<Pair<OfferState, CurrencyModel>> findOfferStatesAndCurrencyForProductCode(String productCode);

  /**
   * Returns the number of offers for a specific productCode.
   * 
   * @param productCode
   * @return The number of offers as an int.
   *
   */
  int countOffersForProduct(String productCode);

  /**
   * Returns the number of offers for a specific productCode and currency.
   * 
   * @param productCode
   * @param currency
   * @return The number of offers as an int.
   *
   */
  int countOffersForProductAndCurrency(String productCode, CurrencyModel currency);
}

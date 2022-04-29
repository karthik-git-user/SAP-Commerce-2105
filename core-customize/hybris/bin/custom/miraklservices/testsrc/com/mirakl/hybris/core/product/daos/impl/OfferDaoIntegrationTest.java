package com.mirakl.hybris.core.product.daos.impl;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.mirakl.hybris.core.enums.OfferState;
import com.mirakl.hybris.core.i18n.services.CurrencyService;
import com.mirakl.hybris.core.model.OfferModel;
import com.mirakl.hybris.core.product.daos.OfferDao;

import de.hybris.bootstrap.annotations.IntegrationTest;
import de.hybris.platform.core.model.c2l.CurrencyModel;
import de.hybris.platform.impex.jalo.ImpExException;
import de.hybris.platform.servicelayer.ServicelayerTest;

@IntegrationTest
public class OfferDaoIntegrationTest extends ServicelayerTest {

  private static final String OFFER_ID_1 = "testOffer1";
  private static final String OFFER_ID_2 = "testOffer2";
  private static final String OFFER_ID_3 = "testOffer3";
  private static final String PRODUCT_CODE_1 = "testProduct1";
  private static final String PRODUCT_CODE_2 = "testProduct2";
  private static final String CURRENCY_EUR_ISOCODE = "EUR";
  private static final String CURRENCY_USD_ISOCODE = "USD";
  private static final String OFFRE_STATE_CODE_USED = "1";
  private static final String OFFRE_STATE_CODE_NEW = "11";


  @Resource
  private OfferDao offerDao;
  @Resource
  private CurrencyService currencyService;

  @Before
  public void setUp() throws ImpExException {
    importCsv("/miraklservices/test/testOffers.impex", "utf-8");
  }


  @Test
  public void findsOfferById() {
    OfferModel result = offerDao.findOfferById(OFFER_ID_1);

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(OFFER_ID_1);
  }

  @Test
  public void findsUndeletedOffersModifiedBeforeDate() {
    List<OfferModel> offers = offerDao.findUndeletedOffersModifiedBeforeDate(new DateTime().plusDays(1).toDate());

    assertThat(offers).hasSize(2);
    assertThat(offers).onProperty(OfferModel.ID).containsOnly(OFFER_ID_1, OFFER_ID_2);
  }

  @Test
  public void findsNoOffersModifiedBeforeDate() {
    List<OfferModel> offers = offerDao.findUndeletedOffersModifiedBeforeDate(new DateTime().minusDays(1).toDate());

    assertThat(offers).isEmpty();
  }

  @Test
  public void findsOffersForProductCode() {
    List<OfferModel> offers = offerDao.findOffersForProductCode(PRODUCT_CODE_2);

    assertThat(offers).hasSize(2);
    assertThat(offers).onProperty(OfferModel.ID).containsOnly(OFFER_ID_2, OFFER_ID_3);

    offers = offerDao.findOffersForProductCode(PRODUCT_CODE_1);

    assertThat(offers).hasSize(1);
    assertThat(offers).onProperty(OfferModel.ID).containsOnly(OFFER_ID_1);
  }

  @Test
  public void findsOffersForProductCodeAndCurrency() {
    List<OfferModel> offersUSD =
        offerDao.findOffersForProductCodeAndCurrency(PRODUCT_CODE_2, currencyService.getCurrencyForCode(CURRENCY_USD_ISOCODE));

    assertThat(offersUSD).hasSize(1);
    assertThat(offersUSD).onProperty(OfferModel.ID).containsOnly(OFFER_ID_3);

    List<OfferModel> offersEUR =
        offerDao.findOffersForProductCodeAndCurrency(PRODUCT_CODE_2, currencyService.getCurrencyForCode(CURRENCY_EUR_ISOCODE));

    assertThat(offersEUR).hasSize(1);
    assertThat(offersEUR).onProperty(OfferModel.ID).containsOnly(OFFER_ID_2);
  }


  @Test
  public void findsOfferStatesAndCurrencyForProductCode() {
    List<Pair<OfferState, CurrencyModel>> statesAndCurrencies = offerDao.findOfferStatesAndCurrencyForProductCode(PRODUCT_CODE_2);

    assertThat(statesAndCurrencies).hasSize(2);
    assertThat(findPair(OFFRE_STATE_CODE_USED, CURRENCY_USD_ISOCODE, statesAndCurrencies)).isNotNull();
    assertThat(findPair(OFFRE_STATE_CODE_NEW, CURRENCY_USD_ISOCODE, statesAndCurrencies)).isNull();
    assertThat(findPair(OFFRE_STATE_CODE_NEW, CURRENCY_EUR_ISOCODE, statesAndCurrencies)).isNotNull();
  }

  @Test
  public void countsOffersForProduct() {
    assertThat(offerDao.countOffersForProduct(PRODUCT_CODE_1)).isEqualTo(1);
    assertThat(offerDao.countOffersForProduct(PRODUCT_CODE_2)).isEqualTo(2);
  }

  @Test
  public void countsOffersForProductAndCurrency() {
    assertThat(
        offerDao.countOffersForProductAndCurrency(PRODUCT_CODE_1, currencyService.getCurrencyForCode(CURRENCY_USD_ISOCODE)))
            .isEqualTo(1);
    assertThat(
        offerDao.countOffersForProductAndCurrency(PRODUCT_CODE_1, currencyService.getCurrencyForCode(CURRENCY_EUR_ISOCODE)))
            .isEqualTo(0);
    assertThat(
        offerDao.countOffersForProductAndCurrency(PRODUCT_CODE_2, currencyService.getCurrencyForCode(CURRENCY_USD_ISOCODE)))
            .isEqualTo(1);
    assertThat(
        offerDao.countOffersForProductAndCurrency(PRODUCT_CODE_2, currencyService.getCurrencyForCode(CURRENCY_EUR_ISOCODE)))
            .isEqualTo(1);
  }


  private Pair<OfferState, CurrencyModel> findPair(String offerStateCode, String currencyIsocode,
      List<Pair<OfferState, CurrencyModel>> offerStatesAndCurrencies) {
    for (Pair<OfferState, CurrencyModel> pair : offerStatesAndCurrencies) {
      if (offerStateCode.equals(pair.getLeft().getCode()) && currencyIsocode.equals(pair.getRight().getIsocode())) {
        return pair;
      }
    }
    return null;
  }
}

package com.mirakl.hybris.channels.product.daos.impl;

import com.mirakl.hybris.channels.channel.services.MiraklChannelService;
import com.mirakl.hybris.core.enums.OfferState;
import com.mirakl.hybris.core.i18n.services.CurrencyService;
import com.mirakl.hybris.core.model.OfferModel;
import com.mirakl.hybris.core.product.daos.OfferDao;
import de.hybris.bootstrap.annotations.IntegrationTest;
import de.hybris.platform.core.model.c2l.CurrencyModel;
import de.hybris.platform.impex.jalo.ImpExException;
import de.hybris.platform.servicelayer.ServicelayerTest;
import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Resource;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

@IntegrationTest
public class OfferDaoChannelIntegrationTest extends ServicelayerTest {
  private static final String OFFER_ID_1 = "testOffer1";
  private static final String OFFER_ID_2 = "testOffer2";
  private static final String OFFER_ID_3 = "testOffer3";
  private static final String OFFER_ID_4 = "testOffer4";
  private static final String OFFER_ID_5 = "testOffer5";
  private static final String PRODUCT_CODE_1 = "testProduct1";
  private static final String PRODUCT_CODE_2 = "testProduct2";
  private static final String CURRENCY_EUR_ISOCODE = "EUR";
  private static final String CURRENCY_USD_ISOCODE = "USD";
  private static final String OFFRE_STATE_CODE_USED = "1";
  private static final String OFFRE_STATE_CODE_NEW = "11";
  private static final String CHANNEL_CODE_1 = "channel1";

  @Resource
  private OfferDao offerDao;
  @Resource
  private CurrencyService currencyService;
  @Resource
  private MiraklChannelService miraklChannelService;

  @Before
  public void setUp() throws ImpExException {
    importCsv("/miraklchannels/test/testChannelOffers.impex", "utf-8");
  }

  @Test
  public void findsOfferById() {
    OfferModel result = offerDao.findOfferById(OFFER_ID_1);

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(OFFER_ID_1);
  }

  @Test
  public void findsOfferByIdWithChannel() {
    miraklChannelService.setCurrentMiraklChannel(miraklChannelService.getMiraklChannelForCode(CHANNEL_CODE_1));

    assertThat(offerDao.findOfferById(OFFER_ID_1)).isNull();

    OfferModel result = offerDao.findOfferById(OFFER_ID_2);
    assertThat(result.getId()).isEqualTo(OFFER_ID_2);
  }

  @Test
  public void findsUndeletedOffersModifiedBeforeDate() {
    List<OfferModel> offers = offerDao.findUndeletedOffersModifiedBeforeDate(new DateTime().plusDays(1).toDate());

    assertThat(offers).hasSize(4);
    assertThat(offers).onProperty(OfferModel.ID).containsOnly(OFFER_ID_1, OFFER_ID_2, OFFER_ID_3, OFFER_ID_4);
  }

  @Test
  public void findsUndeletedOffersModifiedBeforeDateWithChannel() {
    miraklChannelService.setCurrentMiraklChannel(miraklChannelService.getMiraklChannelForCode(CHANNEL_CODE_1));

    List<OfferModel> offers = offerDao.findUndeletedOffersModifiedBeforeDate(new DateTime().plusDays(1).toDate());

    assertThat(offers).hasSize(1);
    assertThat(offers).onProperty(OfferModel.ID).containsOnly(OFFER_ID_2);
  }

  @Test
  public void findsNoOffersModifiedBeforeDate() {
    List<OfferModel> offers = offerDao.findUndeletedOffersModifiedBeforeDate(new DateTime().minusDays(1).toDate());

    assertThat(offers).isEmpty();
  }

  @Test
  public void findsNoOffersModifiedBeforeDateWithChannel() {
    miraklChannelService.setCurrentMiraklChannel(miraklChannelService.getMiraklChannelForCode(CHANNEL_CODE_1));

    List<OfferModel> offers = offerDao.findUndeletedOffersModifiedBeforeDate(new DateTime().minusDays(1).toDate());

    assertThat(offers).isEmpty();
  }

  @Test
  public void findsOffersForProductCode() {
    List<OfferModel> offers = offerDao.findOffersForProductCode(PRODUCT_CODE_2);

    assertThat(offers).hasSize(4);
    assertThat(offers).onProperty(OfferModel.ID).containsOnly(OFFER_ID_2, OFFER_ID_3, OFFER_ID_4, OFFER_ID_5);

    offers = offerDao.findOffersForProductCode(PRODUCT_CODE_1);

    assertThat(offers).hasSize(1);
    assertThat(offers).onProperty(OfferModel.ID).containsOnly(OFFER_ID_1);
  }

  @Test
  public void findsOffersForProductCodeWithChannel() {
    miraklChannelService.setCurrentMiraklChannel(miraklChannelService.getMiraklChannelForCode(CHANNEL_CODE_1));

    List<OfferModel> offers = offerDao.findOffersForProductCode(PRODUCT_CODE_2);

    assertThat(offers).hasSize(2);
    assertThat(offers).onProperty(OfferModel.ID).containsOnly(OFFER_ID_2, OFFER_ID_5);

    assertThat(offerDao.findOffersForProductCode(PRODUCT_CODE_1)).isEmpty();
  }

  @Test
  public void findsOffersForProductCodeAndCurrency() {
    List<OfferModel> offersUSD =
        offerDao.findOffersForProductCodeAndCurrency(PRODUCT_CODE_2, currencyService.getCurrencyForCode(CURRENCY_USD_ISOCODE));

    assertThat(offersUSD).hasSize(3);
    assertThat(offersUSD).onProperty(OfferModel.ID).containsOnly(OFFER_ID_2, OFFER_ID_3, OFFER_ID_5);

    List<OfferModel> offersEUR =
        offerDao.findOffersForProductCodeAndCurrency(PRODUCT_CODE_2, currencyService.getCurrencyForCode(CURRENCY_EUR_ISOCODE));

    assertThat(offersEUR).hasSize(1);
    assertThat(offersEUR).onProperty(OfferModel.ID).containsOnly(OFFER_ID_4);
  }

  @Test
  public void findsOffersForProductCodeAndCurrencyWithChannel() {
    miraklChannelService.setCurrentMiraklChannel(miraklChannelService.getMiraklChannelForCode(CHANNEL_CODE_1));
    List<OfferModel> offersUSD =
        offerDao.findOffersForProductCodeAndCurrency(PRODUCT_CODE_2, currencyService.getCurrencyForCode(CURRENCY_USD_ISOCODE));

    assertThat(offersUSD).hasSize(2);
    assertThat(offersUSD).onProperty(OfferModel.ID).containsOnly(OFFER_ID_2, OFFER_ID_5);

    List<OfferModel> offersEUR =
        offerDao.findOffersForProductCodeAndCurrency(PRODUCT_CODE_2, currencyService.getCurrencyForCode(CURRENCY_EUR_ISOCODE));

    assertThat(offersEUR).isEmpty();
  }

  @Test
  public void findsOfferStatesAndCurrencyForProductCode() {
    List<Pair<OfferState, CurrencyModel>> statesAndCurrencies = offerDao.findOfferStatesAndCurrencyForProductCode(PRODUCT_CODE_2);

    assertThat(statesAndCurrencies).hasSize(3);
    assertThat(findPair(OFFRE_STATE_CODE_USED, CURRENCY_USD_ISOCODE, statesAndCurrencies)).isNotNull();
    assertThat(findPair(OFFRE_STATE_CODE_NEW, CURRENCY_USD_ISOCODE, statesAndCurrencies)).isNotNull();
    assertThat(findPair(OFFRE_STATE_CODE_NEW, CURRENCY_EUR_ISOCODE, statesAndCurrencies)).isNotNull();
  }

  @Test
  public void findsOfferStatesAndCurrencyForProductCodeWithChannel() {
    miraklChannelService.setCurrentMiraklChannel(miraklChannelService.getMiraklChannelForCode(CHANNEL_CODE_1));

    List<Pair<OfferState, CurrencyModel>> statesAndCurrencies = offerDao.findOfferStatesAndCurrencyForProductCode(PRODUCT_CODE_2);

    assertThat(statesAndCurrencies).hasSize(1);
    assertThat(findPair(OFFRE_STATE_CODE_USED, CURRENCY_USD_ISOCODE, statesAndCurrencies)).isNotNull();
  }

  @Test
  public void countsOffersForProduct() {
    assertThat(offerDao.countOffersForProduct(PRODUCT_CODE_1)).isEqualTo(1);
    assertThat(offerDao.countOffersForProduct(PRODUCT_CODE_2)).isEqualTo(4);
  }

  @Test
  public void countsOffersForProductWithChannel() {
    miraklChannelService.setCurrentMiraklChannel(miraklChannelService.getMiraklChannelForCode(CHANNEL_CODE_1));

    assertThat(offerDao.countOffersForProduct(PRODUCT_CODE_1)).isEqualTo(0);
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
            .isEqualTo(3);
    assertThat(
        offerDao.countOffersForProductAndCurrency(PRODUCT_CODE_2, currencyService.getCurrencyForCode(CURRENCY_EUR_ISOCODE)))
            .isEqualTo(1);
  }

  @Test
  public void countsOffersForProductAndCurrencyWithChannel() {
    miraklChannelService.setCurrentMiraklChannel(miraklChannelService.getMiraklChannelForCode(CHANNEL_CODE_1));

    assertThat(
        offerDao.countOffersForProductAndCurrency(PRODUCT_CODE_1, currencyService.getCurrencyForCode(CURRENCY_USD_ISOCODE)))
            .isEqualTo(0);
    assertThat(
        offerDao.countOffersForProductAndCurrency(PRODUCT_CODE_1, currencyService.getCurrencyForCode(CURRENCY_EUR_ISOCODE)))
            .isEqualTo(0);
    assertThat(
        offerDao.countOffersForProductAndCurrency(PRODUCT_CODE_2, currencyService.getCurrencyForCode(CURRENCY_USD_ISOCODE)))
            .isEqualTo(2);
    assertThat(
        offerDao.countOffersForProductAndCurrency(PRODUCT_CODE_2, currencyService.getCurrencyForCode(CURRENCY_EUR_ISOCODE)))
            .isEqualTo(0);
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

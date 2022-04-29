package com.mirakl.hybris.facades.search.solrfacetsearch.populators.impl;

import static com.mirakl.hybris.facades.search.solrfacetsearch.populators.impl.MiraklOfferOverviewDataPopulator.MIN_PURCHASABLE_QTY_DEFAULT;
import static com.mirakl.hybris.facades.search.solrfacetsearch.populators.impl.MiraklOfferOverviewDataPopulator.MIN_PURCHASABLE_QTY_ERROR;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.mirakl.hybris.beans.OfferOverviewData;
import com.mirakl.hybris.core.enums.OfferState;
import com.mirakl.hybris.core.model.OfferModel;
import com.mirakl.hybris.core.model.ShopModel;
import com.mirakl.hybris.core.product.strategies.OfferCodeGenerationStrategy;
import com.mirakl.hybris.facades.product.helpers.PriceDataFactoryHelper;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.commercefacades.product.data.PriceData;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class MiraklOfferOverviewDataPopulatorTest {

  private static final String OFFER_STATE_CODE = "offer-state";
  private static final OfferState OFFER_STATE = OfferState.valueOf(OFFER_STATE_CODE);
  private static final String OFFER_ID = "id";
  private static final String OFFER_CODE = "offer_code";
  private static final BigDecimal PRICE = BigDecimal.valueOf(100);
  private static final BigDecimal TOTAL_PRICE = BigDecimal.valueOf(107.5);
  private static final BigDecimal ORIGIN_PRICE = BigDecimal.valueOf(150);
  private static final BigDecimal MIN_SHIPPING_PRICE = BigDecimal.valueOf(7.5);
  private static final String SHOP_ID = "shop-id";
  private static final String SHOP_NAME = "shop-name";
  private static final double SHOP_GRADE = 3;

  @InjectMocks
  private MiraklOfferOverviewDataPopulator populator;

  @Mock
  private OfferCodeGenerationStrategy offerCodeGenerationStrategy;
  @Mock
  private PriceDataFactoryHelper priceDataFactoryHelper;
  @Mock
  private OfferModel offer;
  @Mock
  private ShopModel shop;
  @Mock
  private PriceData priceData, totalPriceData, originPriceData, minShippingPriceData;

  @Before
  public void setUp() throws Exception {
    when(offerCodeGenerationStrategy.generateCode(OFFER_ID)).thenReturn(OFFER_CODE);

    when(offer.getId()).thenReturn(OFFER_ID);
    when(offer.getEffectiveBasePrice()).thenReturn(PRICE);
    when(offer.getEffectiveOriginPrice()).thenReturn(ORIGIN_PRICE);
    when(offer.getEffectiveTotalPrice()).thenReturn(TOTAL_PRICE);
    when(offer.getMinShippingPrice()).thenReturn(MIN_SHIPPING_PRICE);
    when(offer.getShop()).thenReturn(shop);
    when(offer.getState()).thenReturn(OFFER_STATE);
    when(shop.getId()).thenReturn(SHOP_ID);
    when(shop.getName()).thenReturn(SHOP_NAME);
    when(shop.getGrade()).thenReturn(SHOP_GRADE);

    when(priceDataFactoryHelper.createPrice(PRICE)).thenReturn(priceData);
    when(priceDataFactoryHelper.createPrice(TOTAL_PRICE)).thenReturn(totalPriceData);
    when(priceDataFactoryHelper.createPrice(ORIGIN_PRICE)).thenReturn(originPriceData);
    when(priceDataFactoryHelper.createPrice(MIN_SHIPPING_PRICE)).thenReturn(minShippingPriceData);
  }

  @Test
  public void shouldPopulateOfferSummary() {
    OfferOverviewData result = new OfferOverviewData();
    populator.populate(offer, result);

    assertThat(result.getCode()).isEqualTo(OFFER_CODE);
    assertThat(result.getPrice()).isEqualTo(priceData);
    assertThat(result.getOriginPrice()).isEqualTo(originPriceData);
    assertThat(result.getTotalPrice()).isEqualTo(totalPriceData);
    assertThat(result.getMinShippingPrice()).isEqualTo(minShippingPriceData);
    assertThat(result.getShopId()).isEqualTo(SHOP_ID);
    assertThat(result.getShopName()).isEqualTo(SHOP_NAME);
    assertThat(result.getShopGrade()).isEqualTo(SHOP_GRADE);
    assertThat(result.getMinPurchasableQty()).isEqualTo(MIN_PURCHASABLE_QTY_DEFAULT);
    assertThat(result.getStateCode()).isEqualTo(OFFER_STATE_CODE);
  }

  @Test
  public void shouldPopulateMinPurchasableQty() {
    when(offer.getMinOrderQuantity()).thenReturn(12);

    OfferOverviewData result = new OfferOverviewData();
    populator.populate(offer, result);

    assertThat(result.getMinPurchasableQty()).isEqualTo(12);
  }

  @Test
  public void shouldPopulateMinPurchasableQtyConsideringPackageQuantity() {
    when(offer.getMinOrderQuantity()).thenReturn(12);
    when(offer.getPackageQuantity()).thenReturn(5);

    OfferOverviewData result = new OfferOverviewData();
    populator.populate(offer, result);

    // The first multiple of 5 greater than 12 is 15
    assertThat(result.getMinPurchasableQty()).isEqualTo(15);
  }

  @Test
  public void shouldPopulateMinPurchasableQtyWithInvalidValue() {
    when(offer.getMinOrderQuantity()).thenReturn(12);
    when(offer.getPackageQuantity()).thenReturn(5);
    when(offer.getMaxOrderQuantity()).thenReturn(14);

    OfferOverviewData result = new OfferOverviewData();
    populator.populate(offer, result);

    // Impossible to find a multiple of 5 between 12 and 14
    assertThat(result.getMinPurchasableQty()).isEqualTo(MIN_PURCHASABLE_QTY_ERROR);
  }

  @Test
  public void shouldPopulateMinPurchasableQtyWithPackageQuantityOnly() {
    when(offer.getPackageQuantity()).thenReturn(5);

    OfferOverviewData result = new OfferOverviewData();
    populator.populate(offer, result);

    // The first multiple of 5 is 5
    assertThat(result.getMinPurchasableQty()).isEqualTo(5);
  }

}

package com.mirakl.hybris.core.product.services.impl;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.mirakl.hybris.core.model.OfferModel;
import com.mirakl.hybris.core.product.services.MiraklProductService;
import com.mirakl.hybris.core.product.services.OfferService;
import com.mirakl.hybris.core.product.strategies.OfferPricingSelectionStrategy;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.model.c2l.CurrencyModel;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.jalo.order.price.PriceInformation;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class DefaultMiraklPriceServiceTest {

  private static final boolean NET = true;
  private static final double MARKETPLACE_PRICE = 456.0;
  private static final String CURRENT_CURRENCY_ISOCODE = "USD";
  private static final String PRODUCT_CODE = "0987654321";

  @InjectMocks
  @Spy
  private DefaultMiraklPriceService testObj;

  @Mock
  private MiraklProductService miraklProductService;
  @Mock
  private OfferService offerService;
  @Mock
  private OfferPricingSelectionStrategy offerPricingSelectionStrategy;
  @Mock
  private ProductModel product;
  @Mock
  private OfferModel topOffer;
  @Mock
  private CurrencyModel currentCurrency;

  @Before
  public void setUp() throws Exception {
    doReturn(NET).when(testObj).isNet();
    when(product.getCode()).thenReturn(PRODUCT_CODE);
    when(offerService.getSortedOffersForProductCode(PRODUCT_CODE)).thenReturn(Collections.singletonList(topOffer));
    when(topOffer.getPrice()).thenReturn(BigDecimal.valueOf(MARKETPLACE_PRICE));
    when(topOffer.getEffectiveBasePrice()).thenReturn(BigDecimal.valueOf(MARKETPLACE_PRICE));
    when(topOffer.getCurrency()).thenReturn(currentCurrency);
    when(currentCurrency.getIsocode()).thenReturn(CURRENT_CURRENCY_ISOCODE);
  }

  @Test
  public void getPriceInformationsForProductWhenOfferHasBuyBox() throws Exception {
    when(miraklProductService.isSellableByOperator(product)).thenReturn(false);

    List<PriceInformation> priceInformations = testObj.getPriceInformationsForProduct(product);

    assertThat(priceInformations).hasSize(1);
    assertThat(priceInformations.get(0).getPriceValue().getValue()).isEqualTo(MARKETPLACE_PRICE);
    assertThat(priceInformations.get(0).getPriceValue().getCurrencyIso()).isEqualTo(CURRENT_CURRENCY_ISOCODE);
  }

}

package com.mirakl.hybris.core.product.populators;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.mirakl.client.mmp.domain.offer.MiraklOffer;
import com.mirakl.client.mmp.domain.offer.price.MiraklOfferPricing;
import com.mirakl.hybris.core.model.OfferModel;
import com.mirakl.hybris.core.util.services.JsonMarshallingService;

import de.hybris.bootstrap.annotations.UnitTest;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class MiraklOfferUpdatePopulatorTest {

  private static final String OFFER_PRICE_ADDITIONAL_INFO = "offerPriceAdditionalInfo";
  private static final String ALL_OFFER_PRICING_JSON = "{}";
  private static final int OFFER_QUANTITY = 999;
  private static final BigDecimal OFFER_PRICE = new BigDecimal(99.99);
  private static final BigDecimal OFFER_TOTAL_PRICE = new BigDecimal(109.99);

  @InjectMocks
  private MiraklOfferUpdatePopulator testObj = new MiraklOfferUpdatePopulator();
  @Mock
  private MiraklOffer miraklOffer;
  @Mock
  private OfferModel offerModel;
  @Mock
  private List<MiraklOfferPricing> allOfferPricings;
  @Mock
  private JsonMarshallingService jsonMarshallingService;

  @Before
  public void setUp() {
    when(miraklOffer.getQuantity()).thenReturn(OFFER_QUANTITY);
    when(miraklOffer.getPrice()).thenReturn(OFFER_PRICE);
    when(miraklOffer.getAllPrices()).thenReturn(allOfferPricings);
    when(jsonMarshallingService.toJson(allOfferPricings)).thenReturn(ALL_OFFER_PRICING_JSON);
    when(miraklOffer.getPriceAdditionalInfo()).thenReturn(OFFER_PRICE_ADDITIONAL_INFO);
    when(miraklOffer.getTotalPrice()).thenReturn(OFFER_TOTAL_PRICE);
  }

  @Test
  public void populatesAllOfferProperties() {
    testObj.populate(miraklOffer, offerModel);


    verify(offerModel).setQuantity(OFFER_QUANTITY);
    verify(offerModel).setPrice(OFFER_PRICE);
    verify(offerModel).setAllOfferPricingsJSON(ALL_OFFER_PRICING_JSON);
    verify(offerModel).setPriceAdditionalInfo(OFFER_PRICE_ADDITIONAL_INFO);
    verify(offerModel).setTotalPrice(OFFER_TOTAL_PRICE);
  }
}

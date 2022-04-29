package com.mirakl.hybris.facades.product;

import static java.util.Arrays.asList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.mirakl.hybris.beans.OfferData;
import com.mirakl.hybris.core.model.OfferModel;
import com.mirakl.hybris.core.product.services.OfferService;
import com.mirakl.hybris.core.product.strategies.OfferCodeGenerationStrategy;
import com.mirakl.hybris.facades.product.impl.DefaultOfferFacade;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class DefaultOfferFacadeTest {

  private static final String OFFER_ID = "offer-id";
  private static final String OFFER_CODE = "offer-code";
  private static final String PRODUCT_CODE = "product_code";
  private static final String UNKNOWN_OFFER_CODE = "Whatever..";

  @InjectMocks
  private DefaultOfferFacade testObj;

  @Mock
  private OfferCodeGenerationStrategy offerCodeGenerationStrategy;
  @Mock
  private Converter<OfferModel, OfferData> offerConverter;
  @Mock
  private OfferService offerService;
  @Mock
  private OfferData firstOfferData, secondOfferData;
  @Mock
  private OfferModel firstOffer, secondOffer, offer;

  @Before
  public void setUp() {
    when(offerService.getOfferForId(OFFER_ID)).thenReturn(offer);
    when(offerCodeGenerationStrategy.isOfferCode(OFFER_CODE)).thenReturn(true);
    when(offerCodeGenerationStrategy.translateCodeToId(OFFER_CODE)).thenReturn(OFFER_ID);
  }

  @Test
  public void getOffersForProductWhenOffersFound() {
    when(offerService.getSortedOffersForProductCode(PRODUCT_CODE)).thenReturn(asList(firstOffer, secondOffer));
    when(offerConverter.convertAll(asList(firstOffer, secondOffer)))
        .thenReturn(asList(firstOfferData, secondOfferData));

    List<OfferData> result = testObj.getOffersForProductCode(PRODUCT_CODE);

    assertThat(result).containsExactly(firstOfferData, secondOfferData);

    verify(offerService).getSortedOffersForProductCode(PRODUCT_CODE);
    verify(offerConverter).convertAll(asList(firstOffer, secondOffer));
  }

  @Test
  public void getOffersForProductWhenNoOffersFound() {
    when(offerService.getSortedOffersForProductCode(PRODUCT_CODE)).thenReturn(Collections.<OfferModel>emptyList());
    when(offerConverter.convertAll(Collections.<OfferModel>emptyList())).thenReturn(Collections.<OfferData>emptyList());

    List<OfferData> result = testObj.getOffersForProductCode(PRODUCT_CODE);

    assertThat(result).isEmpty();

    verify(offerService).getSortedOffersForProductCode(PRODUCT_CODE);
    verify(offerConverter).convertAll(Collections.<OfferModel>emptyList());
  }

  @Test
  public void getOfferForCode() {
    OfferModel offer = testObj.getOfferForCode(OFFER_CODE);

    assertThat(offer).isEqualTo(this.offer);
  }

  @Test(expected = UnknownIdentifierException.class)
  public void getOfferForUnknownCodeThrowsUnknownIdentifierException() {
    testObj.getOfferForCode(UNKNOWN_OFFER_CODE);
  }

}

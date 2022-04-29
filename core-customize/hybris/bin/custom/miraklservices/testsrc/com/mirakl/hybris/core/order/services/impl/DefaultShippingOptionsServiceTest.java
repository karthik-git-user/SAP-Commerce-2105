package com.mirakl.hybris.core.order.services.impl;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.base.Optional;
import com.mirakl.client.mmp.front.domain.shipping.MiraklOrderShippingFee;
import com.mirakl.client.mmp.front.domain.shipping.MiraklOrderShippingFeeError;
import com.mirakl.client.mmp.front.domain.shipping.MiraklOrderShippingFeeOffer;
import com.mirakl.client.mmp.front.domain.shipping.MiraklOrderShippingFees;
import com.mirakl.hybris.core.model.OfferModel;
import com.mirakl.hybris.core.order.services.ShippingFeeService;
import com.mirakl.hybris.core.product.services.OfferService;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.model.order.AbstractOrderEntryModel;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.user.AddressModel;
import de.hybris.platform.servicelayer.model.ModelService;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class DefaultShippingOptionsServiceTest {

  private static final String OFFER_1 = "offer1";
  private static final String OFFER_2 = "offer2";
  private static final String ERROR_OFFER = "errorOffer";
  private static final String SHIPPING_FEES_JSON = "shippingFeesJSON";
  private static final String ORIGINAL_SHIPPING_FEES_JSON = "originalShippingFeesJSON";
  private static final String ORIGINAL_SHIPPING_CODE_FOR_OFFER_1 = "originalShippingCodeForOffer1";
  private static final int LEAD_TIME_TO_SHIP = 3;
  private static final int FIRST_OFFER_QUANTITY = 2;
  private static final int SECOND_OFFER_QUANTITY = 1;
  private static final int ZERO_QUANTITY = 0;
  private static final Long LOWER_QUANTITY = 5L;
  private static final Long HIGHER_QUANTITY = 7L;
  private static final double LINE_SHIPPING_PRICE = 12.50;
  private static final String SELECTED_SHIPPING_OPTION_CODE = "selectedShippingOptionCode";
  private static final String SHOP_ID = "shopId";

  @InjectMocks
  private DefaultShippingOptionsService testObj = new DefaultShippingOptionsService();


  @Mock
  private OfferService offerServiceMock;
  @Mock
  private ShippingFeeService shippingFeeServiceMock;
  @Mock
  private ModelService modelServiceMock;
  @Mock
  private OfferModel firstOfferMock, secondOfferMock;
  @Mock
  private AbstractOrderModel orderMock;
  @Mock
  private AbstractOrderEntryModel firstCartEntryMock, secondCartEntryMock, entryWithoutOfferMock;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private AddressModel deliveryAddressMock;

  @Mock
  private MiraklOrderShippingFees shippingRatesMock, originalShippingRatesMock;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private MiraklOrderShippingFee shippingFeeMock, originalShippingFeeMock;
  @Mock
  private MiraklOrderShippingFeeOffer firstShippingOfferMock, secondShippingOfferMock;
  @Mock
  private MiraklOrderShippingFeeError shippingFeeErrorMock;

  @Captor
  private ArgumentCaptor<Collection<AbstractOrderEntryModel>> collectionArgumentCaptor;

  @Before
  public void setUp() {
    when(firstOfferMock.getId()).thenReturn(OFFER_1);
    when(firstOfferMock.getLeadTimeToShip()).thenReturn(LEAD_TIME_TO_SHIP);
    when(secondOfferMock.getId()).thenReturn(OFFER_2);
    when(secondOfferMock.getLeadTimeToShip()).thenReturn(null);

    when(shippingRatesMock.getOrders()).thenReturn(singletonList(shippingFeeMock));
    when(shippingFeeMock.getOffers()).thenReturn(singletonList(firstShippingOfferMock));
    when(firstShippingOfferMock.getLineShippingPrice()).thenReturn(BigDecimal.valueOf(LINE_SHIPPING_PRICE));

    when(orderMock.getMarketplaceEntries()).thenReturn(asList(firstCartEntryMock, secondCartEntryMock));
    when(firstCartEntryMock.getQuantity()).thenReturn(Long.valueOf(FIRST_OFFER_QUANTITY));
    when(secondCartEntryMock.getQuantity()).thenReturn(Long.valueOf(SECOND_OFFER_QUANTITY));
    when(firstCartEntryMock.getOfferId()).thenReturn(OFFER_1);
    when(secondCartEntryMock.getOfferId()).thenReturn(OFFER_2);
    when(firstCartEntryMock.getOrder()).thenReturn(orderMock);
    when(secondCartEntryMock.getOrder()).thenReturn(orderMock);

    when(offerServiceMock.getOfferForId(OFFER_1)).thenReturn(firstOfferMock);
    when(offerServiceMock.getOfferForId(OFFER_2)).thenReturn(secondOfferMock);

    when(shippingFeeServiceMock.getStoredShippingFees(orderMock)).thenReturn(originalShippingRatesMock);
    when(shippingFeeServiceMock.getShippingFeesAsJson(shippingRatesMock)).thenReturn(SHIPPING_FEES_JSON);
    when(shippingFeeServiceMock.getShippingFeesAsJson(originalShippingRatesMock)).thenReturn(ORIGINAL_SHIPPING_FEES_JSON);
    when(shippingFeeServiceMock.extractShippingFeeForShop(originalShippingRatesMock, SHOP_ID, LEAD_TIME_TO_SHIP))
        .thenReturn(Optional.of(shippingFeeMock));

    when(orderMock.getShippingFeesJSON()).thenReturn(ORIGINAL_SHIPPING_FEES_JSON);
    when(originalShippingRatesMock.getOrders()).thenReturn(singletonList(originalShippingFeeMock));
    when(originalShippingFeeMock.getOffers()).thenReturn(singletonList(firstShippingOfferMock));
    when(originalShippingFeeMock.getSelectedShippingType().getCode()).thenReturn(ORIGINAL_SHIPPING_CODE_FOR_OFFER_1);
    when(firstShippingOfferMock.getId()).thenReturn(OFFER_1);
    when(shippingFeeServiceMock.getShippingFees(orderMock)).thenReturn(shippingRatesMock);

  }

  @Test
  public void setsShippingOptions() {
    testObj.setShippingOptions(orderMock);

    verify(orderMock).setShippingFeesJSON(SHIPPING_FEES_JSON);
    verify(shippingFeeServiceMock).setLineShippingDetails(orderMock, shippingRatesMock);
    verify(modelServiceMock).save(orderMock);
  }

  @Test
  public void setShippingOptionsResetsJSONShippingFeesWhenNoMarketplaceEntries() {
    when(shippingFeeServiceMock.getShippingFees(orderMock)).thenReturn(null);

    testObj.setShippingOptions(orderMock);

    verify(orderMock).setShippingFeesJSON(null);
    verify(modelServiceMock).save(orderMock);
  }

  @Test(expected = IllegalArgumentException.class)
  public void setShippingOptionsThrowsIllegalArgumentExceptionIfCartIsNull() {
    testObj.setShippingOptions(null);
  }

  @Test
  public void setsSelectedShippingOption() {
    testObj.setSelectedShippingOption(orderMock, SELECTED_SHIPPING_OPTION_CODE, LEAD_TIME_TO_SHIP, SHOP_ID);

    verify(shippingFeeServiceMock).getStoredShippingFees(orderMock);
    verify(shippingFeeServiceMock).extractShippingFeeForShop(originalShippingRatesMock, SHOP_ID, LEAD_TIME_TO_SHIP);
    verify(shippingFeeServiceMock).updateSelectedShippingOption(shippingFeeMock, SELECTED_SHIPPING_OPTION_CODE);
    verify(shippingFeeServiceMock).getShippingFeesAsJson(originalShippingRatesMock);
    verify(orderMock).setShippingFeesJSON(ORIGINAL_SHIPPING_FEES_JSON);
    verify(modelServiceMock).save(orderMock);
  }

  @Test(expected = IllegalStateException.class)
  public void setSelectedShippingOptionThrowsIllegalStateExceptionIfNoShippingFeesFoundInOrder() {
    when(shippingFeeServiceMock.getStoredShippingFees(orderMock)).thenReturn(null);

    testObj.setSelectedShippingOption(orderMock, SELECTED_SHIPPING_OPTION_CODE, LEAD_TIME_TO_SHIP, SHOP_ID);
  }

  @Test(expected = IllegalStateException.class)
  public void setSelectedShippingOptionThrowsIllegalStateExceptionIfNoShippingFeeFoundForShop() {
    when(shippingFeeServiceMock.extractShippingFeeForShop(originalShippingRatesMock, SHOP_ID, LEAD_TIME_TO_SHIP))
        .thenReturn(Optional.<MiraklOrderShippingFee>absent());

    testObj.setSelectedShippingOption(orderMock, SELECTED_SHIPPING_OPTION_CODE, LEAD_TIME_TO_SHIP, SHOP_ID);
  }

  @Test(expected = IllegalArgumentException.class)
  public void setSelectedShippingOptionThrowsIllegalArgumentExceptionIfOrderIsNull() {
    testObj.setSelectedShippingOption(null, SELECTED_SHIPPING_OPTION_CODE, LEAD_TIME_TO_SHIP, SHOP_ID);
  }

  @Test(expected = IllegalArgumentException.class)
  public void setSelectedShippingOptionThrowsIllegalArgumentExceptionIfSelectedShippingOptionCodeIsNull() {
    testObj.setSelectedShippingOption(orderMock, null, LEAD_TIME_TO_SHIP, SHOP_ID);
  }

  @Test(expected = IllegalArgumentException.class)
  public void setSelectedShippingOptionThrowsIllegalArgumentExceptionIfLeadTimeToShipIsNull() {
    testObj.setSelectedShippingOption(orderMock, SELECTED_SHIPPING_OPTION_CODE, null, SHOP_ID);
  }

  @Test(expected = IllegalArgumentException.class)
  public void setSelectedShippingOptionThrowsIllegalArgumentExceptionIfShopIdIsNull() {
    testObj.setSelectedShippingOption(orderMock, SELECTED_SHIPPING_OPTION_CODE, LEAD_TIME_TO_SHIP, null);
  }

  @Test
  public void removesOfferEntriesWithError() {
    when(firstCartEntryMock.getOfferId()).thenReturn(ERROR_OFFER);
    when(shippingFeeErrorMock.getOfferId()).thenReturn(ERROR_OFFER);

    testObj.removeOfferEntriesWithError(orderMock, singletonList(shippingFeeErrorMock));

    verify(modelServiceMock).removeAll(collectionArgumentCaptor.capture());
    Collection<AbstractOrderEntryModel> removedEntries = collectionArgumentCaptor.getValue();
    assertThat(removedEntries).containsOnly(firstCartEntryMock);
  }

  @Test
  public void adjustsOfferQuantitiesWhenOfferQuantityIsLower() {
    when(firstCartEntryMock.getQuantity()).thenReturn(HIGHER_QUANTITY);
    when(firstShippingOfferMock.getQuantity()).thenReturn(LOWER_QUANTITY.intValue());

    testObj.adjustOfferQuantities(singletonList(firstCartEntryMock), singletonList(firstShippingOfferMock));

    verify(firstCartEntryMock).setQuantity(LOWER_QUANTITY);
    verify(modelServiceMock).saveAll(singletonList(firstCartEntryMock));
  }

  @Test
  public void adjustOfferQuantitiesDoesNothingWhenOfferQuantityIsHigher() {
    when(firstCartEntryMock.getQuantity()).thenReturn(LOWER_QUANTITY);
    when(firstShippingOfferMock.getQuantity()).thenReturn(HIGHER_QUANTITY.intValue());

    testObj.adjustOfferQuantities(singletonList(firstCartEntryMock), singletonList(firstShippingOfferMock));

    verify(firstCartEntryMock, never()).setQuantity(anyLong());
    verify(modelServiceMock).saveAll(Collections.emptyList());
  }

  @Test
  public void adjustOfferQuantitiesRemovesEntryIfOfferQuantityIsZero() {
    when(firstCartEntryMock.getQuantity()).thenReturn(HIGHER_QUANTITY);
    when(firstShippingOfferMock.getQuantity()).thenReturn(ZERO_QUANTITY);

    testObj.adjustOfferQuantities(singletonList(firstCartEntryMock), singletonList(firstShippingOfferMock));

    verify(modelServiceMock).remove(firstCartEntryMock);
  }
}

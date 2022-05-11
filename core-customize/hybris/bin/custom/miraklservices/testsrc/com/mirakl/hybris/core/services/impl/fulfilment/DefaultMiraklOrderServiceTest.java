package com.mirakl.hybris.core.services.impl.fulfilment;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.mirakl.client.mmp.domain.offer.MiraklOffer;
import com.mirakl.client.mmp.domain.offer.price.MiraklOfferPricing;
import com.mirakl.client.mmp.domain.offer.price.MiraklVolumePrice;
import com.mirakl.client.mmp.front.core.MiraklMarketplacePlatformFrontApi;
import com.mirakl.client.mmp.front.domain.order.create.MiraklCreateOrder;
import com.mirakl.client.mmp.front.domain.order.create.MiraklCreatedOrders;
import com.mirakl.client.mmp.front.domain.order.create.MiraklOfferNotShippable;
import com.mirakl.client.mmp.front.domain.shipping.MiraklOrderShippingFee;
import com.mirakl.client.mmp.front.domain.shipping.MiraklOrderShippingFeeOffer;
import com.mirakl.client.mmp.front.domain.shipping.MiraklOrderShippingFees;
import com.mirakl.client.mmp.front.request.offer.MiraklGetOfferRequest;
import com.mirakl.client.mmp.front.request.order.worflow.MiraklCreateOrderRequest;
import com.mirakl.client.mmp.front.request.order.worflow.MiraklValidOrderRequest;
import com.mirakl.client.mmp.request.order.evaluation.MiraklGetAssessmentsRequest;
import com.mirakl.hybris.core.model.OfferModel;
import com.mirakl.hybris.core.order.services.impl.DefaultMiraklOrderService;
import com.mirakl.hybris.core.product.services.MiraklPriceService;
import com.mirakl.hybris.core.product.services.OfferService;
import com.mirakl.hybris.core.util.services.JsonMarshallingService;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.core.model.order.AbstractOrderEntryModel;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import de.hybris.platform.servicelayer.model.ModelService;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class DefaultMiraklOrderServiceTest {

  private static final String JSON_STRING = "json-string";
  private static final String OFFER_1_ID = "1756";
  private static final String OFFER_2_ID = "47896";
  private static final BigDecimal OFFER_1_PRICE = BigDecimal.valueOf(45.2);
  private static final BigDecimal OFFER_2_PRICE = BigDecimal.valueOf(10.8);
  private static final BigDecimal OFFER_1_UPDATED_PRICE = OFFER_1_PRICE;
  private static final BigDecimal OFFER_2_UPDATED_PRICE = BigDecimal.valueOf(51.8);
  private static final String ORDER_CODE = "orderCode";
  private static final long SHIPPABLE_OFFER_QTY = 15L;
  private static final String ORDER_LINE_ID = "order-line-id";

  @InjectMocks
  @Spy
  private DefaultMiraklOrderService testObj;

  @Mock
  private MiraklMarketplacePlatformFrontApi miraklApi;
  @Mock
  private Converter<OrderModel, MiraklCreateOrder> miraklCreateOrderConverter;
  @Mock
  private ModelService modelService;
  @Mock
  private JsonMarshallingService jsonMarshallingService;
  @Mock
  private OrderModel order;
  @Mock
  private MiraklCreateOrder miraklCreateOrder;
  @Mock
  private MiraklCreatedOrders miraklCreatedOrders;
  @Mock
  private MiraklOfferNotShippable offerNotShippable;
  @Mock
  private AbstractOrderEntryModel orderEntry1, orderEntry2;
  @Mock
  private MiraklOrderShippingFees miraklOrderShippingFees;
  @Mock
  private MiraklOrderShippingFee miraklOrderShippingFee;
  @Mock
  private MiraklOrderShippingFeeOffer miraklOffer1, miraklOffer2;
  @Mock
  private OfferService offerService;
  @Mock
  private OfferModel offer1, offer2;
  @Mock
  private MiraklPriceService miraklPriceService;
  @Mock
  private MiraklVolumePrice miraklVolumePrice;
  @Mock
  private MiraklOffer synchronousOfferData;
  @Mock
  private MiraklOfferPricing miraklOfferPricing1, miraklOfferPricing2;
  @Mock
  private Converter<MiraklCreatedOrders, AbstractOrderModel> miraklOrderModelConverter;
  @Captor
  private ArgumentCaptor<MiraklCreateOrderRequest> createOrderRequestCaptor;
  @Captor
  private ArgumentCaptor<MiraklValidOrderRequest> validOrderRequestCaptor;
  @Captor
  private ArgumentCaptor<List<ItemModel>> itemModelsCaptor;

  @Before
  public void setUp() {
    when(miraklCreateOrderConverter.convert(order)).thenReturn(miraklCreateOrder);
    when(miraklApi.createOrder(createOrderRequestCaptor.capture())).thenReturn(miraklCreatedOrders);
    when(order.getMarketplaceEntries()).thenReturn(asList(orderEntry1, orderEntry2));
    when(order.getCode()).thenReturn(ORDER_CODE);
    when(offerNotShippable.getId()).thenReturn(OFFER_1_ID);
    when(orderEntry1.getOfferId()).thenReturn(OFFER_1_ID);
    when(orderEntry2.getOfferId()).thenReturn(OFFER_2_ID);
    when(jsonMarshallingService.toJson(miraklCreatedOrders)).thenReturn(JSON_STRING);
    when(jsonMarshallingService.fromJson(JSON_STRING, MiraklCreatedOrders.class)).thenReturn(miraklCreatedOrders);
    when(miraklOrderShippingFees.getOrders()).thenReturn(singletonList(miraklOrderShippingFee));
    when(miraklOrderShippingFee.getOffers()).thenReturn(asList(miraklOffer1, miraklOffer2));
    when(miraklOffer1.getId()).thenReturn(OFFER_1_ID);
    when(miraklOffer2.getId()).thenReturn(OFFER_2_ID);

    when(offerService.getOfferForId(OFFER_1_ID)).thenReturn(offer1);
    when(offerService.getOfferForId(OFFER_2_ID)).thenReturn(offer2);
    when(offer1.getId()).thenReturn(OFFER_1_ID);
    when(offer2.getId()).thenReturn(OFFER_2_ID);
  }

  @Test
  public void createsMarketplaceOrder() {
    MiraklCreatedOrders result = testObj.createMarketplaceOrders(order);

    assertThat(result).isSameAs(miraklCreatedOrders);

    verify(miraklCreateOrderConverter).convert(order);
    verify(miraklApi).createOrder(createOrderRequestCaptor.capture());

    MiraklCreateOrderRequest createOrderRequest = createOrderRequestCaptor.getValue();
    assertThat(createOrderRequest.getCreateOrder()).isSameAs(miraklCreateOrder);
  }

  @Test(expected = IllegalArgumentException.class)
  public void createMarketplaceOrderThrowsIllegalArgumentExceptionIfOrderIsNull() {
    testObj.createMarketplaceOrders(null);
  }

  @Test
  public void marksNotShippableEntries() {
    List<AbstractOrderEntryModel> result = testObj.extractNotShippableEntries(singletonList(offerNotShippable), order);

    assertThat(result).containsOnly(orderEntry1);

    verify(order).getMarketplaceEntries();
  }

  @Test(expected = IllegalArgumentException.class)
  public void markNotShippableEntriesThrowsIllegalArgumentExceptionIfOrderIsNull() {
    testObj.extractNotShippableEntries(Collections.emptyList(), null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void markNotShippableEntriesThrowsIllegalArgumentExceptionIfNotShippableOfferListIsNull() {
    testObj.extractNotShippableEntries(null, order);
  }

  @Test
  public void loadsCreatedOrders() {
    when(order.getCreatedOrdersJSON()).thenReturn(JSON_STRING);

    MiraklCreatedOrders createdOrders = testObj.loadCreatedOrders(order);

    assertThat(createdOrders).isEqualTo(miraklCreatedOrders);
  }

  @Test
  public void loadsNullForEmptyCreatedOrders() {
    when(order.getCreatedOrdersJSON()).thenReturn(null);

    MiraklCreatedOrders createdOrders = testObj.loadCreatedOrders(order);

    assertThat(createdOrders).isNull();
  }

  @Test
  public void validatesOrder() {
    testObj.validateOrder(order);

    verify(miraklApi).validOrder(validOrderRequestCaptor.capture());
    assertThat(validOrderRequestCaptor.getValue().getCommercialId()).isEqualTo(order.getCode());
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateOrderThrowsIllegalArgumentExceptionIfOrderNull() {
    testObj.validateOrder(null);
  }

  @Test
  public void getAssessments() {
    testObj.getAssessments();

    verify(miraklApi).getAssessments(any(MiraklGetAssessmentsRequest.class));
  }

  @Test
  public void shouldUpdateOfferAndCartEntryPrices() {
    when(order.getMarketplaceEntries()).thenReturn(asList(orderEntry2));
    when(miraklOrderShippingFee.getOffers()).thenReturn(asList(miraklOffer2));
    when(orderEntry2.getBasePrice()).thenReturn(OFFER_2_PRICE.doubleValue());
    when(orderEntry2.getQuantity()).thenReturn(SHIPPABLE_OFFER_QTY);
    when(miraklPriceService.getOfferUnitPriceForQuantity(offer2, SHIPPABLE_OFFER_QTY)).thenReturn(OFFER_2_PRICE);
    when(miraklOffer2.getPrice()).thenReturn(OFFER_2_UPDATED_PRICE);
    when(miraklApi.getOffer(any(MiraklGetOfferRequest.class))).thenReturn(synchronousOfferData);
    when(synchronousOfferData.getAllPrices()).thenReturn(asList(miraklOfferPricing2));

    testObj.updateOffersPrice(order, miraklOrderShippingFees);

    verify(orderEntry2).setBasePrice(OFFER_2_UPDATED_PRICE.doubleValue());
    verify(offerService).storeAllOfferPricings(asList(miraklOfferPricing2), offer2);
    verify(modelService).saveAll(itemModelsCaptor.capture());
    assertThat(itemModelsCaptor.getValue()).containsOnly(order, offer2, orderEntry2);
  }

  @Test
  public void shouldUpdateOnlyCartEntryPrice() {
    when(order.getMarketplaceEntries()).thenReturn(asList(orderEntry2));
    when(miraklOrderShippingFee.getOffers()).thenReturn(asList(miraklOffer2));
    when(orderEntry2.getBasePrice()).thenReturn(OFFER_2_PRICE.doubleValue());
    when(orderEntry2.getQuantity()).thenReturn(SHIPPABLE_OFFER_QTY);
    when(miraklPriceService.getOfferUnitPriceForQuantity(offer2, SHIPPABLE_OFFER_QTY)).thenReturn(OFFER_2_UPDATED_PRICE);
    when(miraklOffer2.getPrice()).thenReturn(OFFER_2_UPDATED_PRICE);
    testObj.updateOffersPrice(order, miraklOrderShippingFees);


    verify(orderEntry2).setBasePrice(OFFER_2_UPDATED_PRICE.doubleValue());
    verify(offerService, never()).storeAllOfferPricings(anyListOf(MiraklOfferPricing.class), any(OfferModel.class));
    verify(modelService).saveAll(itemModelsCaptor.capture());
    assertThat(itemModelsCaptor.getValue()).containsOnly(order, orderEntry2);
  }

  @Test
  public void shouldNotUpdatePricesIfNoChange() {
    when(order.getMarketplaceEntries()).thenReturn(asList(orderEntry2));
    when(miraklOrderShippingFee.getOffers()).thenReturn(asList(miraklOffer2));
    when(orderEntry2.getBasePrice()).thenReturn(OFFER_2_UPDATED_PRICE.doubleValue());
    when(orderEntry2.getQuantity()).thenReturn(SHIPPABLE_OFFER_QTY);
    when(miraklPriceService.getOfferUnitPriceForQuantity(offer2, SHIPPABLE_OFFER_QTY)).thenReturn(OFFER_2_UPDATED_PRICE);
    when(miraklOffer2.getPrice()).thenReturn(OFFER_2_UPDATED_PRICE);

    testObj.updateOffersPrice(order, miraklOrderShippingFees);

    verify(offerService, never()).storeAllOfferPricings(anyListOf(MiraklOfferPricing.class), any(OfferModel.class));
    verifyZeroInteractions(modelService);
  }

  @Test
  public void shouldStoreCreatedOrders() {

    testObj.storeCreatedOrders(order, miraklCreatedOrders);

    verify(miraklOrderModelConverter).convert(miraklCreatedOrders, order);
    verify(modelService).save(order);
  }

}

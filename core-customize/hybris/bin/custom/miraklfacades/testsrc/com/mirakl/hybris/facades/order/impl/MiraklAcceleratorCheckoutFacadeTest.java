package com.mirakl.hybris.facades.order.impl;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.commercefacades.order.CartFacade;
import de.hybris.platform.commerceservices.delivery.DeliveryService;
import de.hybris.platform.commerceservices.order.CommerceCheckoutService;
import de.hybris.platform.commerceservices.service.data.CommerceCheckoutParameter;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.delivery.DeliveryModeModel;
import de.hybris.platform.order.CartService;
import de.hybris.platform.order.exceptions.CalculationException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class MiraklAcceleratorCheckoutFacadeTest {

  public static final String MIRAKL_DEFAULT_SHIPPING = "mirakl-default-shipping";

  @Spy
  @InjectMocks
  private MiraklAcceleratorCheckoutFacade testObj = new MiraklAcceleratorCheckoutFacade();

  @Mock
  private DeliveryService deliveryServiceMock;
  @Mock
  private CartService cartServiceMock;
  @Mock
  private CommerceCheckoutService commerceCheckoutServiceMock;
  @Mock
  private CartFacade cartFacadeMock;

  @Mock
  private DeliveryModeModel deliveryModeMock;
  @Mock
  private CartModel cartMock;

  @Captor
  private ArgumentCaptor<CommerceCheckoutParameter> checkoutParameterArgumentCaptor;

  @Before
  public void setUp() {
    testObj.setDefaultFreeDeliveryModeCode(MIRAKL_DEFAULT_SHIPPING);
    when(cartFacadeMock.hasSessionCart()).thenReturn(true);
    when(cartServiceMock.getSessionCart()).thenReturn(cartMock);
    when(deliveryServiceMock.getDeliveryModeForCode(MIRAKL_DEFAULT_SHIPPING)).thenReturn(deliveryModeMock);
  }

  @Test
  public void setAvailableShippingOptionsSetsDefaultDeliveryModeIfCartIsMarketplaceOnly() throws CalculationException {
    when(cartMock.isMarketplaceOrder()).thenReturn(true);

    boolean result = testObj.setDeliveryModeIfAvailable();

    assertThat(result).isTrue();

    verify(cartMock).isMarketplaceOrder();

    verify(deliveryServiceMock).getDeliveryModeForCode(MIRAKL_DEFAULT_SHIPPING);
    verify(commerceCheckoutServiceMock).setDeliveryMode(checkoutParameterArgumentCaptor.capture());
    CommerceCheckoutParameter deliveryModeParameter = checkoutParameterArgumentCaptor.getValue();
    assertThat(deliveryModeParameter.getCart()).isEqualTo(cartMock);
    assertThat(deliveryModeParameter.isEnableHooks()).isTrue();
    assertThat(deliveryModeParameter.getDeliveryMode()).isSameAs(deliveryModeMock);

    verify(testObj, never()).superSetDeliveryModeIfAvailable();
  }

  @Test
  public void setAvailableShippingOptionsFallsBackToSuperIfCartIsNotMarketplaceOnly() throws CalculationException {
    when(cartMock.isMarketplaceOrder()).thenReturn(false);

    testObj.setDeliveryModeIfAvailable();

    verify(cartMock).isMarketplaceOrder();
    verify(testObj).superSetDeliveryModeIfAvailable();
    verify(deliveryServiceMock, never()).getDeliveryModeForCode(anyString());
    verify(commerceCheckoutServiceMock, never()).setDeliveryMode(any(CommerceCheckoutParameter.class));

  }

  @Test
  public void setAvailableShippingOptionsReturnsFalseIfCartIsNull() throws CalculationException {
    when(cartFacadeMock.hasSessionCart()).thenReturn(false);

    boolean result = testObj.setDeliveryModeIfAvailable();

    assertThat(result).isFalse();

    verify(testObj, never()).superSetDeliveryModeIfAvailable();
    verify(deliveryServiceMock, never()).getDeliveryModeForCode(anyString());
    verify(commerceCheckoutServiceMock, never()).setDeliveryMode(any(CommerceCheckoutParameter.class));

  }

  @Test(expected = IllegalStateException.class)
  public void setAvailableShippingOptionsThrowsIllegalStateArgumentIfCartIsMarketplaceOnlyAndNoFreeShippingDeliveryModeIsFound()
      throws CalculationException {
    when(cartMock.isMarketplaceOrder()).thenReturn(true);
    when(deliveryServiceMock.getDeliveryModeForCode(MIRAKL_DEFAULT_SHIPPING)).thenReturn(null);

    testObj.setDeliveryModeIfAvailable();
  }
}

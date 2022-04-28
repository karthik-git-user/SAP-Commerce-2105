package com.mirakl.hybris.mtc.populators;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.mirakl.client.mmp.front.request.shipping.MiraklCustomerShippingToAddress;
import com.mirakl.client.mmp.front.request.shipping.MiraklGetShippingRatesRequest;
import com.mirakl.hybris.mtc.strategies.MiraklTaxConnectorActivationStrategy;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.model.c2l.CountryModel;
import de.hybris.platform.core.model.order.AbstractOrderEntryModel;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.user.AddressModel;
import de.hybris.platform.servicelayer.dto.converter.Converter;


@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class MiraklTaxConnectorGetShippingRatesRequestPopulatorTest {

  private static final String OFFER_1 = "offer-1";
  private static final int FIRST_OFFER_QUANTITY = 2;

  @InjectMocks
  private MiraklTaxConnectorGetShippingRatesRequestPopulator testObj;
  @Mock
  private AbstractOrderModel order;
  @Mock
  private MiraklGetShippingRatesRequest request;
  @Mock
  protected MiraklTaxConnectorActivationStrategy miraklTaxConnectorActivationStrategy;
  @Mock
  protected Converter<AddressModel, MiraklCustomerShippingToAddress> miraklCustomerShippingToAddressConverter;
  @Mock
  protected AddressModel deliveryAddress;
  @Mock
  private CountryModel country;
  @Mock
  private AbstractOrderEntryModel firstOrderEntry;
  @Mock
  private MiraklCustomerShippingToAddress miraklCustomerShippingToAddress;

  @Before
  public void setUp() {
    when(miraklTaxConnectorActivationStrategy.isMiraklTaxConnectorComputation(order)).thenReturn(true);
    when(deliveryAddress.getCountry()).thenReturn(country);
    when(order.getDeliveryAddress()).thenReturn(deliveryAddress);
    when(order.getMarketplaceEntries()).thenReturn(asList(firstOrderEntry));
    when(firstOrderEntry.getQuantity()).thenReturn(Long.valueOf(FIRST_OFFER_QUANTITY));
    when(firstOrderEntry.getOfferId()).thenReturn(OFFER_1);

    when(miraklCustomerShippingToAddressConverter.convert(deliveryAddress)).thenReturn(miraklCustomerShippingToAddress);
  }

  @Test
  public void populateShouldEnableTaxCalculation() {
    testObj.populate(order, request);

    verify(request).setComputeOrderTaxes(true);
    verify(request).setCustomerShippingToAddress(miraklCustomerShippingToAddress);
  }

}

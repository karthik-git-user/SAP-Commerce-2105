package com.mirakl.hybris.mtc.populators;

import static com.mirakl.hybris.mtc.constants.MirakltaxconnectorConstants.ALLOWED_MIRAKL_TAX_CONNECTOR_CURRENCIES;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.mirakl.client.mmp.domain.common.currency.MiraklIsoCurrencyCode;
import com.mirakl.client.mmp.domain.order.tax.MiraklOrderTaxAmount;
import com.mirakl.client.mmp.front.domain.order.create.MiraklCreateOrderOffer;
import com.mirakl.client.mmp.front.domain.shipping.MiraklOrderShippingFee;
import com.mirakl.hybris.beans.MiraklTaxValuesData;
import com.mirakl.hybris.mtc.beans.MiraklTaxEstimation;
import com.mirakl.hybris.mtc.strategies.MiraklTaxConnectorEmptyTaxesStrategy;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.model.c2l.CurrencyModel;
import de.hybris.platform.core.model.order.AbstractOrderEntryModel;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import de.hybris.platform.util.TaxValue;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class MiraklCreateOrderWithMiraklTaxConnectorOfferPopulatorTest {

  private static final MiraklIsoCurrencyCode MIRAKL_ISO_CURRENCY_CODE = ALLOWED_MIRAKL_TAX_CONNECTOR_CURRENCIES.iterator().next();

  @InjectMocks
  private MiraklCreateOrderWithMiraklTaxConnectorOfferPopulator testObj;
  @Mock
  private MiraklCreateOrderOffer miraklCreateOrderOffer;
  @Mock
  private AbstractOrderEntryModel marketplaceEntry;
  @Mock
  private AbstractOrderModel order;
  @Mock
  private CurrencyModel currency;
  @Mock
  private MiraklOrderShippingFee shippingFee;
  @Mock
  private Converter<TaxValue, MiraklOrderTaxAmount> miraklOrderTaxAmountConverter;
  @Mock
  private Converter<MiraklTaxEstimation, List<TaxValue>> absoluteTaxValueConverter;
  @Mock
  private Converter<Pair<MiraklOrderShippingFee, AbstractOrderEntryModel>, MiraklTaxValuesData> miraklTaxConnectorTaxValuesDataConverter;
  @Mock
  private Converter<MiraklTaxValuesData, Pair<List<MiraklOrderTaxAmount>, List<MiraklOrderTaxAmount>>> miraklOrderTaxConnectorTaxAmountConverter;
  @Mock
  private MiraklTaxValuesData miraklTaxValuesData;
  @Mock
  private MiraklTaxConnectorEmptyTaxesStrategy miraklTaxConnectorEmptyTaxesStrategy;
  @Mock
  private TaxValue taxValue, shippingTax;
  @Mock
  private MiraklOrderTaxAmount miraklOrderTaxAmount, miraklOrderShippingTaxAmount;
  @Captor
  private ArgumentCaptor<List<MiraklOrderTaxAmount>> taxCaptor, shippingTaxCaptor;
  private List<TaxValue> taxes = new ArrayList<>(), shippingTaxes = new ArrayList<>();
  private List<MiraklOrderTaxAmount> miraklOrderTaxesAmounts = new ArrayList<>(), miraklOrderShippingTaxesAmounts = new ArrayList<>();

  @Before
  public void setUp() {
    taxes.add(taxValue);
    shippingTaxes.add(shippingTax);
    miraklOrderTaxesAmounts.add(miraklOrderTaxAmount);
    miraklOrderShippingTaxesAmounts.add(miraklOrderShippingTaxAmount);
    when(miraklCreateOrderOffer.getCurrencyIsoCode()).thenReturn(MIRAKL_ISO_CURRENCY_CODE);
    when(marketplaceEntry.getOrder()).thenReturn(order);
    when(order.getCurrency()).thenReturn(currency);
    when(currency.getIsocode()).thenReturn(MIRAKL_ISO_CURRENCY_CODE.name());

    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        Pair<List<MiraklOrderTaxAmount>, List<MiraklOrderTaxAmount>> pair = (Pair<List<MiraklOrderTaxAmount>, List<MiraklOrderTaxAmount>>) invocation.getArguments()[1];
        pair.getLeft().add(miraklOrderTaxAmount);
        pair.getRight().add(miraklOrderShippingTaxAmount);
        return null;
      }
    }).when(miraklOrderTaxConnectorTaxAmountConverter).convert(any(MiraklTaxValuesData.class), any());

    testObj.setMiraklTaxConnectorTaxValuesDataConverter(miraklTaxConnectorTaxValuesDataConverter);
  }

  @Test
  public void populateTaxesShouldHandleNullValues() {
    when(shippingFee.getOffers()).thenReturn(null);
    testObj.populateTaxes(miraklCreateOrderOffer, null, null);
    assertThat(miraklCreateOrderOffer.getShippingTaxes()).isEmpty();
    assertThat(miraklCreateOrderOffer.getTaxes()).isEmpty();
    testObj.populateTaxes(miraklCreateOrderOffer, marketplaceEntry, null);
    assertThat(miraklCreateOrderOffer.getShippingTaxes()).isEmpty();
    assertThat(miraklCreateOrderOffer.getTaxes()).isEmpty();
    testObj.populateTaxes(miraklCreateOrderOffer, null, shippingFee);
    assertThat(miraklCreateOrderOffer.getShippingTaxes()).isEmpty();
    assertThat(miraklCreateOrderOffer.getTaxes()).isEmpty();
  }

  @Test
  public void populateTaxesShouldPopulatesAllTaxesIfPresent() {
    when(miraklTaxConnectorTaxValuesDataConverter.convert(any())).thenReturn(miraklTaxValuesData);
    when(miraklTaxValuesData.getTaxValues()).thenReturn(taxes);
    when(miraklTaxValuesData.getShippingTaxValues()).thenReturn(shippingTaxes);
    when(miraklOrderTaxAmountConverter.convertAll(taxes)).thenReturn(miraklOrderTaxesAmounts);
    when(miraklOrderTaxAmountConverter.convertAll(shippingTaxes)).thenReturn(miraklOrderShippingTaxesAmounts);

    testObj.populateTaxes(miraklCreateOrderOffer, marketplaceEntry, shippingFee);
    verify(miraklCreateOrderOffer).setTaxes(taxCaptor.capture());
    verify(miraklCreateOrderOffer).setShippingTaxes(shippingTaxCaptor.capture());

    assertThat(taxCaptor.getValue()).isEqualTo(miraklOrderTaxesAmounts);
    assertThat(shippingTaxCaptor.getValue()).isEqualTo(miraklOrderShippingTaxesAmounts);
  }
}

package com.mirakl.hybris.core.payment.populators;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import com.mirakl.client.mmp.domain.common.currency.MiraklIsoCurrencyCode;
import com.mirakl.client.mmp.domain.payment.MiraklPaymentStatus;
import com.mirakl.client.mmp.domain.payment.refund.MiraklOrderLineRefund;

import de.hybris.bootstrap.annotations.UnitTest;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class MiraklOrderLineRefundPopulatorTest extends AbstractMiraklOrderLineRefundPopulatorTest {

  @InjectMocks
  private MiraklOrderLineRefundPopulator testObj;

  @Before
  public void setUp() throws Exception {
    super.setUp();
  }

  @Test
  public void populate() throws Exception {
    MiraklOrderLineRefund output = new MiraklOrderLineRefund();

    testObj.populate(refundEntry, output);

    assertThat(output.getRefundId()).isEqualTo(MIRAKL_REFUND_ID);
    assertThat(output.getAmount()).isEqualTo(REFUND_ENTRY_AMOUNT);
    assertThat(output.getCurrencyIsoCode()).isEqualTo(MiraklIsoCurrencyCode.valueOf(CURRENCY_ISO_CODE));
    assertThat(output.getPaymentStatus()).isNotEqualTo(MiraklPaymentStatus.OK);
    assertThat(output.getTransactionDate()).isEqualTo(PAYMENT_TRANSACTION_ENTRY_TIME);
    assertThat(output.getTransactionNumber()).isEqualTo(PAYMENT_TRANSACTION_ENTRY_CODE);
  }

}

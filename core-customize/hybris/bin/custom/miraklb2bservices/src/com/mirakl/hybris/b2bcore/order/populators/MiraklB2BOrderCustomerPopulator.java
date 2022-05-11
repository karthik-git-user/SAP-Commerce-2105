package com.mirakl.hybris.b2bcore.order.populators;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;

import com.mirakl.client.mmp.domain.order.MiraklOrderCustomer;
import com.mirakl.hybris.b2bcore.order.strategies.B2BUnitBillingAddressStrategy;
import com.mirakl.hybris.core.order.populators.MiraklOrderCustomerPopulator;

import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.order.payment.InvoicePaymentInfoModel;

public class MiraklB2BOrderCustomerPopulator extends MiraklOrderCustomerPopulator {

  private static final Logger LOG = Logger.getLogger(MiraklB2BOrderCustomerPopulator.class);

  protected B2BUnitBillingAddressStrategy b2bUnitBillingAddressStrategy;

  @Override
  protected void setCustomerAddresses(MiraklOrderCustomer miraklOrderCustomer, OrderModel order) {
    miraklOrderCustomer.setShippingAddress(miraklCustomerAddressConverter.convert(order.getDeliveryAddress()));
    if (order.getPaymentInfo() instanceof InvoicePaymentInfoModel) {
      if (LOG.isDebugEnabled()) {
        LOG.debug(String.format("Order [%s] with account payment: Using B2B Unit billing address as customer billing address.",
            order.getCode()));
      }
      miraklOrderCustomer.setBillingAddress(
          miraklCustomerAddressConverter.convert(b2bUnitBillingAddressStrategy.getBillingAddressForUnit(order.getUnit())));
    } else {
      miraklOrderCustomer.setBillingAddress(miraklCustomerAddressConverter.convert(order.getPaymentAddress()));
    }
  }

  @Required
  public void setB2bUnitBillingAddressStrategy(B2BUnitBillingAddressStrategy b2bUnitBillingAddressStrategy) {
    this.b2bUnitBillingAddressStrategy = b2bUnitBillingAddressStrategy;
  }
}

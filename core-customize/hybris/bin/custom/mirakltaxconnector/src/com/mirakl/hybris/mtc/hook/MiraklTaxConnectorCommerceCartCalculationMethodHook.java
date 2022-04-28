package com.mirakl.hybris.mtc.hook;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNullStandardMessage;

import org.springframework.beans.factory.annotation.Required;

import com.mirakl.hybris.core.order.hook.MiraklCommerceCartCalculationMethodHook;
import com.mirakl.hybris.mtc.strategies.MiraklTaxConnectorActivationStrategy;

import de.hybris.platform.commerceservices.order.hook.CommerceCartCalculationMethodHook;
import de.hybris.platform.commerceservices.service.data.CommerceCartParameter;

public class MiraklTaxConnectorCommerceCartCalculationMethodHook extends MiraklCommerceCartCalculationMethodHook
    implements CommerceCartCalculationMethodHook {

  protected MiraklTaxConnectorActivationStrategy miraklTaxConnectorActivationStrategy;

  @Override
  public void afterCalculate(CommerceCartParameter parameter) {
    // Nothing to do here
  }

  @Override
  public void beforeCalculate(CommerceCartParameter parameter) {
    validateParameterNotNullStandardMessage("CommerceCartParameter", parameter);
    validateParameterNotNullStandardMessage("CommerceCartParameter.cart", parameter.getCart());
    if ((miraklPromotionsActivationStrategy.isMiraklPromotionsEnabled()
        || miraklTaxConnectorActivationStrategy.isMiraklTaxConnectorComputation(parameter.getCart()))
        && !parameter.getCart().getMarketplaceEntries().isEmpty()) {
      updateCartCalculationJSON(parameter);
    }
  }

  @Required
  public void setMiraklTaxConnectorActivationStrategy(MiraklTaxConnectorActivationStrategy miraklTaxConnectorActivationStrategy) {
    this.miraklTaxConnectorActivationStrategy = miraklTaxConnectorActivationStrategy;
  }
}

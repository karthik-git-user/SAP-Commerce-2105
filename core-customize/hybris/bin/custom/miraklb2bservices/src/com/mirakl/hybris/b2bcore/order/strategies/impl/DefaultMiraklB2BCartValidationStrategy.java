package com.mirakl.hybris.b2bcore.order.strategies.impl;

import com.mirakl.hybris.core.order.strategies.impl.DefaultMiraklCartValidationStrategy;

import de.hybris.platform.b2bacceleratorservices.enums.CheckoutPaymentType;
import de.hybris.platform.core.model.order.CartModel;

/**
 * This class is a copy of de.hybris.platform.b2bacceleratorservices.strategies.impl.DefaultB2BCartValidationStrategy,
 * extending com.mirakl.hybris.core.order.strategies.impl.DefaultMiraklCartValidationStrategy rather than
 * de.hybris.platform.commerceservices.strategies.impl.DefaultCartValidationStrategy
 *
 * @since SAP Commerce 18.11
 */
public class DefaultMiraklB2BCartValidationStrategy extends DefaultMiraklCartValidationStrategy {

  @Override
  protected void validateDelivery(final CartModel cartModel) {
    final CheckoutPaymentType paymentType = cartModel.getPaymentType();

    if (paymentType == null || CheckoutPaymentType.CARD.equals(paymentType)) {
      super.validateDelivery(cartModel);
    }
  }
}

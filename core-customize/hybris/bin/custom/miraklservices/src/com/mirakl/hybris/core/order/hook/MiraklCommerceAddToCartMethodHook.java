package com.mirakl.hybris.core.order.hook;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNullStandardMessage;

import org.springframework.beans.factory.annotation.Required;

import com.mirakl.hybris.core.order.strategies.SynchronousCartUpdateActivationStrategy;
import com.mirakl.hybris.core.product.services.OfferService;

import de.hybris.platform.commerceservices.order.CommerceCartCalculationStrategy;
import de.hybris.platform.commerceservices.order.CommerceCartModification;
import de.hybris.platform.commerceservices.order.hook.CommerceAddToCartMethodHook;
import de.hybris.platform.commerceservices.service.data.CommerceCartParameter;
import de.hybris.platform.core.model.order.CartModel;


public class MiraklCommerceAddToCartMethodHook implements CommerceAddToCartMethodHook {

  protected OfferService offerService;
  protected CommerceCartCalculationStrategy commerceCartCalculationStrategy;
  protected SynchronousCartUpdateActivationStrategy synchronousCartUpdateActivationStrategy;

  @Override
  public void beforeAddToCart(final CommerceCartParameter parameters) {
    validateParameterNotNullStandardMessage("CommerceCartParameter", parameters);

    if (synchronousCartUpdateActivationStrategy.isSynchronousCartUpdateEnabled() && parameters.getOffer() != null
        && parameters.getOffer().getId() != null) {
      offerService.updateExistingOfferForId(parameters.getOffer().getId());
      CartModel cart = parameters.getCart();
      cart.setShippingFeesJSON(null);
    }
  }

  @Override
  public void afterAddToCart(final CommerceCartParameter parameters, final CommerceCartModification result) {
    // Nothing to do here
  }

  @Required
  public void setOfferService(OfferService offerService) {
    this.offerService = offerService;
  }

  @Required
  public void setCommerceCartCalculationStrategy(CommerceCartCalculationStrategy commerceCartCalculationStrategy) {
    this.commerceCartCalculationStrategy = commerceCartCalculationStrategy;
  }

  @Required
  public void setSynchronousCartUpdateActivationStrategy(
      SynchronousCartUpdateActivationStrategy synchronousCartUpdateActivationStrategy) {
    this.synchronousCartUpdateActivationStrategy = synchronousCartUpdateActivationStrategy;
  }
}

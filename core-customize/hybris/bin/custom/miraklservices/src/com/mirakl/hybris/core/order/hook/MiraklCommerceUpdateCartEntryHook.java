package com.mirakl.hybris.core.order.hook;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNullStandardMessage;

import org.springframework.beans.factory.annotation.Required;

import com.mirakl.hybris.core.order.strategies.SynchronousCartUpdateActivationStrategy;
import com.mirakl.hybris.core.product.services.OfferService;

import de.hybris.platform.commerceservices.order.CommerceCartCalculationStrategy;
import de.hybris.platform.commerceservices.order.CommerceCartModification;
import de.hybris.platform.commerceservices.order.hook.CommerceUpdateCartEntryHook;
import de.hybris.platform.commerceservices.service.data.CommerceCartParameter;
import de.hybris.platform.core.model.order.AbstractOrderEntryModel;
import de.hybris.platform.core.model.order.CartModel;

public class MiraklCommerceUpdateCartEntryHook implements CommerceUpdateCartEntryHook {

  protected OfferService offerService;
  protected CommerceCartCalculationStrategy commerceCartCalculationStrategy;
  protected SynchronousCartUpdateActivationStrategy synchronousCartUpdateActivationStrategy;

  @Override
  public void afterUpdateCartEntry(CommerceCartParameter parameter, CommerceCartModification result) {
    // Nothing to do here
  }

  @Override
  public void beforeUpdateCartEntry(CommerceCartParameter parameter) {
    validateParameterNotNullStandardMessage("CommerceCartParameter", parameter);

    if (synchronousCartUpdateActivationStrategy.isSynchronousCartUpdateEnabled() && parameter.getCart() != null
        && parameter.getCart().getMarketplaceEntries() != null) {
      for (AbstractOrderEntryModel marketplaceEntry : parameter.getCart().getMarketplaceEntries()) {
        if (marketplaceEntry != null && marketplaceEntry.getEntryNumber() == parameter.getEntryNumber()) {
          offerService.updateExistingOfferForId(marketplaceEntry.getOfferId());
        }
      }
      CartModel cart = parameter.getCart();
      cart.setShippingFeesJSON(null);
    }
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


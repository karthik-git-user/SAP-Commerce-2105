package com.mirakl.hybris.facades.order.impl;

import static java.lang.String.format;

import org.springframework.beans.factory.annotation.Required;

import de.hybris.platform.acceleratorfacades.order.impl.DefaultAcceleratorCheckoutFacade;
import de.hybris.platform.commerceservices.service.data.CommerceCheckoutParameter;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.delivery.DeliveryModeModel;

public class MiraklAcceleratorCheckoutFacade extends DefaultAcceleratorCheckoutFacade {

  protected String defaultFreeDeliveryModeCode;

  @Override
  public boolean setDeliveryModeIfAvailable() {
    CartModel cart = getCart();
    if (cart == null) {
      return false;
    }
    if (cart.isMarketplaceOrder()) {
      return setDefaultFreeDeliveryMode(cart);
    }
    return superSetDeliveryModeIfAvailable();
  }

  protected boolean setDefaultFreeDeliveryMode(CartModel sessionCart) {
    DeliveryModeModel defaultFreeDeliveryMode = getDeliveryService().getDeliveryModeForCode(defaultFreeDeliveryModeCode);
    if (defaultFreeDeliveryMode == null) {
      throw new IllegalStateException(
          format("No default free delivery mode [%s] found for the marketplace cart", defaultFreeDeliveryModeCode));
    }

    CommerceCheckoutParameter checkoutParameter = new CommerceCheckoutParameter();
    checkoutParameter.setEnableHooks(true);
    checkoutParameter.setCart(sessionCart);
    checkoutParameter.setDeliveryMode(defaultFreeDeliveryMode);
    getCommerceCheckoutService().setDeliveryMode(checkoutParameter);

    return true;
  }

  protected boolean superSetDeliveryModeIfAvailable() {
    return super.setDeliveryModeIfAvailable();
  }

  @Required
  //~ public void setDefaultFreeDeliveryModeCode(String defaultFreeDeliveryModeCode) {
  public void setDefaultFreeDeliveryModeCode() {
    this.defaultFreeDeliveryModeCode  = "FreeDeliveryModeCode";
  }
}

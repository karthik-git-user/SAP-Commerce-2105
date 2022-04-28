package com.mirakl.hybris.occ.controllers;

import static de.hybris.platform.webservicescommons.mapping.FieldSetLevelHelper.DEFAULT_LEVEL;

import javax.annotation.Resource;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.mirakl.hybris.facades.order.ShippingFacade;

import de.hybris.platform.commercefacades.order.CartFacade;
import de.hybris.platform.commerceservices.order.CommerceCartModificationException;
import de.hybris.platform.commerceservices.request.mapping.annotation.ApiVersion;
import de.hybris.platform.commercewebservicescommons.dto.order.CartWsDTO;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdUserIdAndCartIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;


/**
 * Web Services Controller to expose the functionality of the {@link ShippingFacade} and the {@link CartFacade} .
 */
@Controller(value = "miraklExtendedCartsController")
@RequestMapping(value = "/{baseSiteId}/users/{userId}/carts")
@ApiVersion("v2")
@Api(tags = "Mirakl Extended Carts")
public class MiraklExtendedCartsController extends MiraklBaseController {

  @Resource(name = "shippingFacade")
  private ShippingFacade shippingFacade;
  @Resource(name = "cartFacade")
  private CartFacade cartFacade;

  @RequestMapping(value = "/{cartId}/marketplacedeliverymodes", method = RequestMethod.GET)
  @ResponseBody
  @ApiOperation(nickname = "updateMarketplaceDeliveryModes", value = "Get cart with marketplace delivery options.",
      notes = "Returns the cart with updated marketplace delivery modes.")
  @ApiBaseSiteIdUserIdAndCartIdParam
  public CartWsDTO updateMarketplaceDeliveryModes(
      @ApiFieldsParam @RequestParam(defaultValue = DEFAULT_LEVEL) final String fields) {
    shippingFacade.updateAvailableShippingOptions();
    return getDataMapper().map(cartFacade.getSessionCart(), CartWsDTO.class, fields);
  }

  @RequestMapping(value = "/{cartId}/marketplacedeliverymodes", method = RequestMethod.PUT)
  @ResponseBody
  @ApiOperation(nickname = "setMarketplaceDeliveryOption",
      value = "Sets the marketplace delivery option for the given leadtime to ship and shop.",
      notes = "Returns the cart with updated marketplace delivery modes.")
  @ApiBaseSiteIdUserIdAndCartIdParam
  public CartWsDTO setMarketplaceDeliveryOption(
      @ApiParam(value = "The selected shipping option code",
          required = true) @RequestParam final String selectedShippingOptionCode,
      @ApiParam(value = "The leadtime to ship", required = true) @RequestParam final Integer leadTimeToShip,
      @ApiParam(value = "The shop identifier", required = true) @RequestParam final String shopId,
      @ApiFieldsParam @RequestParam(defaultValue = DEFAULT_LEVEL) final String fields) throws CommerceCartModificationException {
    shippingFacade.updateShippingOptions(selectedShippingOptionCode, leadTimeToShip, shopId);
    return getDataMapper().map(cartFacade.getSessionCart(), CartWsDTO.class, fields);
  }
}

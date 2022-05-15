/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package novalnet.controllers.v2;

import de.hybris.platform.acceleratorfacades.order.AcceleratorCheckoutFacade;
import de.hybris.platform.acceleratorfacades.payment.data.PaymentSubscriptionResultData;
import de.hybris.platform.acceleratorservices.payment.data.PaymentData;
import de.hybris.platform.acceleratorservices.payment.data.PaymentErrorField;
//~ import de.hybris.platform.acceleratorocc.exceptions.PaymentProviderException;
//~ import de.hybris.platform.acceleratorocc.dto.payment.PaymentRequestWsDTO;
//~ import de.hybris.platform.acceleratorocc.dto.payment.SopPaymentDetailsWsDTO;
//~ import de.hybris.platform.acceleratorocc.payment.facade.CommerceWebServicesPaymentFacade;
//~ import de.hybris.platform.acceleratorocc.validator.SopPaymentDetailsValidator;
import de.hybris.platform.commercefacades.order.CheckoutFacade;
import de.hybris.platform.commercefacades.order.data.CCPaymentInfoData;
import de.hybris.platform.commercefacades.storelocator.data.PointOfServiceDataList;
import de.hybris.platform.commercefacades.user.UserFacade;
import de.hybris.platform.commerceservices.order.CommerceCartModificationException;
import de.hybris.platform.commerceservices.request.mapping.annotation.ApiVersion;
//~ import de.hybris.platform.commercewebservicescommons.dto.order.CartModificationListWsDTO;
//~ import de.hybris.platform.commercewebservicescommons.dto.order.PaymentDetailsWsDTO;
//~ import de.hybris.platform.commercewebservicescommons.dto.store.PointOfServiceListWsDTO;
import de.hybris.platform.webservicescommons.errors.exceptions.WebserviceValidationException;
import de.hybris.platform.webservicescommons.mapping.DataMapper;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdUserIdAndCartIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;
import de.hybris.platform.commercefacades.order.data.CartModificationDataList;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import springfox.documentation.annotations.ApiIgnore;

import static de.hybris.platform.webservicescommons.mapping.FieldSetLevelHelper.DEFAULT_LEVEL;


@Controller
@RequestMapping(value = "/{baseSiteId}/users/{userId}/carts")
@ApiVersion("v2")
@Api(tags = "Extended Carts")
public class ExtendedCartsController
{
	private final static Logger LOG = Logger.getLogger(ExtendedCartsController.class);

	@RequestMapping(value = "/{cartId}/payment/sop/request", method = RequestMethod.GET)
	@ResponseBody
	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT" })
	@ApiOperation(nickname = "getSopPaymentRequestDetails1", value = "Get information needed for create subscription", notes =
			"Returns the necessary information for creating a subscription that contacts the "
					+ "payment provider directly. This information contains the payment provider URL and a list of parameters that are needed to create the subscription.")
	public void getSopPaymentRequestDetails()
	{
		LOG.debug("test1");
	}

	

	@RequestMapping(value = "/{cartId}/payment/sop/response", method = RequestMethod.POST)
	@ResponseBody
	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT" })
	@ApiOperation(nickname = "doHandleSopPaymentResponse1", value = "Handles response from payment provider and create payment details", notes =
			"Handles the response from the payment provider and creates payment details."
					+ "\n\nNote, the “Try it out” button is not enabled for this method (always returns an error) because the Extended Carts Controller handles parameters differently, depending "
					+ "on which payment provider is used. For more information about this controller, please refer to the “acceleratorocc AddOn” documentation on help.hybris.com.")
	@ApiBaseSiteIdUserIdAndCartIdParam
	public void doHandleSopPaymentResponse()
	{
		LOG.debug("test2");
	}

	

	@RequestMapping(value = "/{cartId}/payment/sop/response", method = RequestMethod.GET)
	@ResponseBody
	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT" })
	@ApiOperation(nickname = "getSopPaymentResponse1", value = "Get information about create subscription request results", notes =
			"Returns information related to creating subscription request results. "
					+ "If there is no response from the payment provider, a \"202 Accepted\" status is returned. If the subscription is created successfully, the payment details "
					+ "are returned. Otherwise, an error response is returned.\n\nNote, the “Try it out” button is not enabled for this method (always returns an error) because "
					+ "the Extended Carts Controller handles parameters differently, depending on which payment provider is used. For more information about this controller, please "
					+ "refer to the “acceleratorocc AddOn” documentation on help.hybris.com.")
	@ApiBaseSiteIdUserIdAndCartIdParam
	public void getSopPaymentResponse()
	{
		LOG.debug("test3");
	}

	@RequestMapping(value = "/{cartId}/payment/sop/response", method = RequestMethod.DELETE)
	@ResponseBody
	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT" })
	@ApiOperation(nickname = "removeSopPaymentResponse1", value = "Deletes payment provider response related to cart.", notes = "Deletes the payment provider response related to the specified cart.")
	@ApiBaseSiteIdUserIdAndCartIdParam
	public void removeSopPaymentResponse()
	{
		LOG.debug("test4");
	}

	
	
}

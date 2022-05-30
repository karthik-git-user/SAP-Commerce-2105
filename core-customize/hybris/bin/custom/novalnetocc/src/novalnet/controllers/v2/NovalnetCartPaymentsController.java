/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.commercewebservices.core.v2.controller;

import de.hybris.platform.commercefacades.order.data.CCPaymentInfoData;
import de.hybris.platform.commercewebservicescommons.dto.order.PaymentDetailsWsDTO;
import de.hybris.platform.webservicescommons.cache.CacheControl;
import de.hybris.platform.webservicescommons.cache.CacheControlDirective;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdUserIdAndCartIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;
import novalnet.controllers.InvalidPaymentInfoException;
import novalnet.controllers.NoCheckoutCartException;
import novalnet.controllers.UnsupportedRequestException;
//~ import de.hybris.platform.commercewebservices.core.request.support.impl.PaymentProviderRequestSupportedStrategy;
import de.hybris.platform.commerceservices.request.mapping.annotation.RequestMappingOverride;

import javax.annotation.Resource;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import de.hybris.novalnet.core.model.NovalnetPaymentInfoModel;


import de.hybris.platform.commercefacades.i18n.I18NFacade;
import de.hybris.platform.commercefacades.i18n.comparators.CountryComparator;
import de.hybris.platform.commercefacades.order.CheckoutFacade;
import de.hybris.platform.commercefacades.order.OrderFacade;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.commercefacades.user.data.CountryData;
import de.hybris.platform.commercefacades.user.data.RegionData;
import de.hybris.platform.commerceservices.strategies.CheckoutCustomerStrategy;
import de.hybris.platform.commercewebservicescommons.dto.order.PaymentDetailsListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.PaymentDetailsWsDTO;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.enums.OrderStatus;
import de.hybris.platform.core.model.c2l.CountryModel;
import de.hybris.platform.core.model.order.AbstractOrderEntryModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.order.payment.PaymentInfoModel;
import de.hybris.platform.core.model.user.AddressModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.core.model.user.TitleModel;
import de.hybris.platform.order.CalculationService;
import de.hybris.platform.order.CartFactory;
import de.hybris.platform.order.CartService;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.order.exceptions.CalculationException;
import de.hybris.platform.servicelayer.config.ConfigurationService;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import de.hybris.platform.servicelayer.i18n.CommonI18NService;
import de.hybris.platform.servicelayer.keygenerator.KeyGenerator;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.session.SessionService;
import de.hybris.platform.store.BaseStoreModel;
import de.hybris.platform.store.services.BaseStoreService;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.BindingResult;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Controller
@RequestMapping(value = "/{baseSiteId}/users/{userId}/carts")
@CacheControl(directive = CacheControlDirective.NO_CACHE)
@Api(tags = "Cart Payments")
public class NovalnetCartPaymentsController
{
	private final static Logger LOG = Logger.getLogger(NovalnetCartPaymentsController.class);
	
	private BaseStoreService baseStoreService;
    private SessionService sessionService;
    private CartService cartService;
    private OrderFacade orderFacade;
    private CheckoutFacade checkoutFacade;
    private CheckoutCustomerStrategy checkoutCustomerStrategy;
    private ModelService modelService;
    //~ private PaymentsResponseConverter paymentsResponseConverter;
    private PaymentsDetailsResponseConverter paymentsDetailsResponseConverter;
    private FlexibleSearchService flexibleSearchService;
    private Converter<AddressData, AddressModel> addressReverseConverter;
    private Converter<CountryModel, CountryData> countryConverter;
    private Converter<OrderModel, OrderData> orderConverter;
    private CartFactory cartFactory;
    private CalculationService calculationService;
    private Populator<AddressModel, AddressData> addressPopulator;
    private CommonI18NService commonI18NService;
    
    @Resource(name = "i18NFacade")
    private I18NFacade i18NFacade;
	
	private static final String PAYMENT_MAPPING = "accountHolderName,cardNumber,cardType,cardTypeData(code),expiryMonth,expiryYear,issueNumber,startMonth,startYear,subscriptionId,defaultPaymentInfo,saved,billingAddress(titleCode,firstName,lastName,line1,line2,town,postalCode,country(isocode),region(isocode),defaultAddress)";

	//~ @Resource(name = "paymentProviderRequestSupportedStrategy")
	//~ private PaymentProviderRequestSupportedStrategy paymentProviderRequestSupportedStrategy;

	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT" })
	@PostMapping(value = "/{cartId}/paymentdetails", consumes = { MediaType.APPLICATION_JSON_VALUE,
			MediaType.APPLICATION_XML_VALUE })
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMappingOverride
	@ResponseBody
	@ApiOperation(nickname = "createCartPaymentDetails", value = "Defines and assigns details of a new credit card payment to the cart.", notes = "Defines the details of a new credit card, and assigns this payment option to the cart.")
	@ApiBaseSiteIdUserIdAndCartIdParam
	public void createCartPaymentDetails(@ApiParam(value =
			"Request body parameter that contains details such as the name on the card (accountHolderName), the card number (cardNumber), the card type (cardType.code), "
					+ "the month of the expiry date (expiryMonth), the year of the expiry date (expiryYear), whether the payment details should be saved (saved), whether the payment details "
					+ "should be set as default (defaultPaymentInfo), and the billing address (billingAddress.firstName, billingAddress.lastName, billingAddress.titleCode, billingAddress.country.isocode, "
					+ "billingAddress.line1, billingAddress.line2, billingAddress.town, billingAddress.postalCode, billingAddress.region.isocode)\n\nThe DTO is in XML or .json format.", required = true) @RequestBody final PaymentDetailsWsDTO paymentDetails,
			@ApiFieldsParam @RequestParam(defaultValue = "Default") final String fields)
			throws InvalidPaymentInfoException, NoCheckoutCartException, UnsupportedRequestException
	{
		LOG.info("+++++++++91++++++++");
		//~ paymentProviderRequestSupportedStrategy.checkIfRequestSupported("addPaymentDetails");
		//~ validatePayment(paymentDetails);
		//~ CCPaymentInfoData paymentInfoData = getDataMapper().map(paymentDetails, CCPaymentInfoData.class, PAYMENT_MAPPING);
		//~ paymentInfoData = addPaymentDetailsInternal(paymentInfoData).getPaymentInfo();
		//~ return getDataMapper().map(paymentInfoData, PaymentDetailsWsDTO.class, fields);
		CartModel cartModel = cartService.getSessionCart();
		LOG.info("+++++++++98++++++++");
		final AddressModel billingAddress = createBillingAddress(paymentDetails);
		NovalnetPaymentInfoModel paymentInfo = createPaymentInfo(cartModel, paymentDetails);
		paymentInfo.setBillingAddress(billingAddress);
        billingAddress.setOwner(paymentInfo);
        modelService.save(paymentInfo);
        cartModel.setPaymentInfo(paymentInfo);
        modelService.save(cartModel);
        LOG.info("+++++++++106++++++++");
	}
	
	public NovalnetPaymentInfoModel createPaymentInfo(final CartModel cartModel, PaymentDetailsWsDTO paymentDetails) {
        NovalnetPaymentInfoModel paymentInfoModel = new NovalnetPaymentInfoModel();
        CustomerModel customerModel = getCheckoutCustomerStrategy().getCurrentUserForCheckout();
		paymentInfoModel.setPaymentEmailAddress("karthik_m@novalnetsolutions.com");
		paymentInfoModel.setDuplicate(Boolean.FALSE);
		paymentInfoModel.setSaved(Boolean.TRUE);
		paymentInfoModel.setUser(customerModel);
		paymentInfoModel.setPaymentInfo("notes");
		paymentInfoModel.setOrderHistoryNotes("notes");
		paymentInfoModel.setPaymentProvider("NovalnetCreditCard");
		paymentInfoModel.setPaymentGatewayStatus("pending");
        return paymentInfoModel;
    }
	
	private AddressModel createBillingAddress(PaymentDetailsWsDTO paymentDetails) {
        String titleCode = paymentDetails.getBillingAddress().getTitleCode();
        final AddressModel billingAddress = getModelService().create(AddressModel.class);
        if (StringUtils.isNotBlank(titleCode)) {
            final TitleModel title = new TitleModel();
            title.setCode(titleCode);
            billingAddress.setTitle(getFlexibleSearchService().getModelByExample(title));
        }
        billingAddress.setFirstname(paymentDetails.getBillingAddress().getFirstName());
        billingAddress.setLastname(paymentDetails.getBillingAddress().getLastName());
        billingAddress.setLine1(paymentDetails.getBillingAddress().getLine1());
        billingAddress.setLine2(paymentDetails.getBillingAddress().getLine2());
        billingAddress.setTown(paymentDetails.getBillingAddress().getTown());
        billingAddress.setPostalcode(paymentDetails.getBillingAddress().getPostalCode());
        billingAddress.setCountry(getCommonI18NService().getCountry(paymentDetails.getBillingAddress().getCountry().getIsocode()));

        final AddressData addressData = new AddressData();
        addressData.setTitleCode(paymentDetails.getBillingAddress().getTitleCode());
        addressData.setFirstName(billingAddress.getFirstname());
        addressData.setLastName(billingAddress.getLastname());
        addressData.setLine1(billingAddress.getLine1());
        addressData.setLine2(billingAddress.getLine2());
        addressData.setTown(billingAddress.getTown());
        addressData.setPostalCode(billingAddress.getPostalcode());
        addressData.setBillingAddress(true);

        if (paymentDetails.getBillingAddress().getCountry() != null) {
            final CountryData countryData = getI18NFacade().getCountryForIsocode(paymentDetails.getBillingAddress().getCountry().getIsocode());
            addressData.setCountry(countryData);
        }
        if (paymentDetails.getBillingAddress().getRegion().getIsocode() != null) {
            final RegionData regionData = getI18NFacade().getRegion(paymentDetails.getBillingAddress().getCountry().getIsocode(), paymentDetails.getBillingAddress().getRegion().getIsocode());
            addressData.setRegion(regionData);
        }

        getAddressReverseConverter().convert(addressData, billingAddress);

        return billingAddress;
    }
	
	public ModelService getModelService() {
        return modelService;
    }

    public void setModelService(ModelService modelService) {
        this.modelService = modelService;
    }
    
    public CommonI18NService getCommonI18NService() {
        return commonI18NService;
    }

    public void setCommonI18NService(CommonI18NService commonI18NService) {
        this.commonI18NService = commonI18NService;
    }
    
    public FlexibleSearchService getFlexibleSearchService() {
        return flexibleSearchService;
    }

    public void setFlexibleSearchService(FlexibleSearchService flexibleSearchService) {
        this.flexibleSearchService = flexibleSearchService;
    }
    
    public CheckoutCustomerStrategy getCheckoutCustomerStrategy() {
        return checkoutCustomerStrategy;
    }

    public void setCheckoutCustomerStrategy(CheckoutCustomerStrategy checkoutCustomerStrategy) {
        this.checkoutCustomerStrategy = checkoutCustomerStrategy;
    }
    
    public I18NFacade getI18NFacade() {
        return i18NFacade;
    }

    public void setI18NFacade(I18NFacade i18NFacade) {
        this.i18NFacade = i18NFacade;
    }
    
    public Converter<AddressData, AddressModel> getAddressReverseConverter() {
        return addressReverseConverter;
    }

    public void setAddressReverseConverter(Converter<AddressData, AddressModel> addressReverseConverter) {
        this.addressReverseConverter = addressReverseConverter;
    }
}

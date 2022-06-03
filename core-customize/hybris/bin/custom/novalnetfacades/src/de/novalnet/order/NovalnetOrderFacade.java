package de.novalnet.order;

import java.util.List;

import de.hybris.platform.commercefacades.order.data.CCPaymentInfoData;
import de.hybris.platform.commercewebservicescommons.dto.order.PaymentDetailsWsDTO;
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

import de.hybris.platform.commercefacades.order.CartFacade;
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
import de.hybris.platform.commerceservices.customer.CustomerAccountService;
import de.hybris.platform.commerceservices.order.CommerceCheckoutService;
import de.hybris.platform.commerceservices.service.data.CommerceCheckoutParameter;
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
/**
 * Facade for setting shipping options on marketplace order entries
 */
public class NovalnetOrderFacade {

	private final static Logger LOG = Logger.getLogger(NovalnetOrderFacade.class);
	
	private BaseStoreService baseStoreService;
    private SessionService sessionService;
    private CartService cartService;
    private OrderFacade orderFacade;
    private CartFacade cartFacade;
    private CheckoutFacade checkoutFacade;
    private CheckoutCustomerStrategy checkoutCustomerStrategy;
    private ModelService modelService;
    private FlexibleSearchService flexibleSearchService;
    private CommerceCheckoutService commerceCheckoutService;
    private Converter<AddressData, AddressModel> addressReverseConverter;
    private Converter<CountryModel, CountryData> countryConverter;
    private Converter<OrderModel, OrderData> orderConverter;
    private CartFactory cartFactory;
    private CalculationService calculationService;
    private Populator<AddressModel, AddressData> addressPopulator;
    private CommonI18NService commonI18NService;
    private CustomerAccountService customerAccountService;
    
    @Resource(name = "i18NFacade")
    private I18NFacade i18NFacade;
    
    
	
	public BaseStoreService getBaseStoreService() {
        return baseStoreService;
    }

    public void setBaseStoreService(BaseStoreService baseStoreService) {
        this.baseStoreService = baseStoreService;
    }

    public SessionService getSessionService() {
        return sessionService;
    }

    public void setSessionService(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    public CartService getCartService() {
        return cartService;
    }

    public void setCartService(CartService cartService) {
        this.cartService = cartService;
    }

    public OrderFacade getOrderFacade() {
        return orderFacade;
    }

    public void setOrderFacade(OrderFacade orderFacade) {
        this.orderFacade = orderFacade;
    }

    public CheckoutFacade getCheckoutFacade() {
        return checkoutFacade;
    }

    public void setCheckoutFacade(CheckoutFacade checkoutFacade) {
        this.checkoutFacade = checkoutFacade;
    }

    public CheckoutCustomerStrategy getCheckoutCustomerStrategy() {
        return checkoutCustomerStrategy;
    }

    public void setCheckoutCustomerStrategy(CheckoutCustomerStrategy checkoutCustomerStrategy) {
        this.checkoutCustomerStrategy = checkoutCustomerStrategy;
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

    public Converter<AddressData, AddressModel> getAddressReverseConverter() {
        return addressReverseConverter;
    }

    public void setAddressReverseConverter(Converter<AddressData, AddressModel> addressReverseConverter) {
        this.addressReverseConverter = addressReverseConverter;
    }

    public I18NFacade getI18NFacade() {
        return i18NFacade;
    }

    public void setI18NFacade(I18NFacade i18NFacade) {
        this.i18NFacade = i18NFacade;
    }

    protected Converter<CountryModel, CountryData> getCountryConverter() {
        return countryConverter;
    }

    @Required
    public void setCountryConverter(final Converter<CountryModel, CountryData> countryConverter) {
        this.countryConverter = countryConverter;
    }

    public Converter<OrderModel, OrderData> getOrderConverter() {
        return orderConverter;
    }

    public void setOrderConverter(Converter<OrderModel, OrderData> orderConverter) {
        this.orderConverter = orderConverter;
    }

    public CartFactory getCartFactory() {
        return cartFactory;
    }

    public void setCartFactory(CartFactory cartFactory) {
        this.cartFactory = cartFactory;
    }

    public CalculationService getCalculationService() {
        return calculationService;
    }

    public void setCalculationService(CalculationService calculationService) {
        this.calculationService = calculationService;
    }

    public Populator<AddressModel, AddressData> getAddressPopulator() {
        return addressPopulator;
    }

    public void setAddressPopulator(Populator<AddressModel, AddressData> addressPopulator) {
        this.addressPopulator = addressPopulator;
    }
    
    public CartData addPaymentDetailsInternal(final NovalnetPaymentInfoModel paymentInfo)
	{
		final CustomerModel currentCustomer = getCurrentUserForCheckout();
		getCustomerAccountService().setDefaultPaymentInfo(currentCustomer, paymentInfo);
		final CartModel cartModel = getCart();
		final CommerceCheckoutParameter parameter = createCommerceCheckoutParameter(cartModel, true);
		parameter.setPaymentInfo(paymentInfo);
		getCommerceCheckoutService().setPaymentInfo(parameter);
		return cartFacade.getSessionCart();
	}
	
	protected CustomerModel getCurrentUserForCheckout()
	{
		return getCheckoutCustomerStrategy().getCurrentUserForCheckout();
	}
	
	protected CommerceCheckoutParameter createCommerceCheckoutParameter(final CartModel cart, final boolean enableHooks)
	{
		final CommerceCheckoutParameter parameter = new CommerceCheckoutParameter();
		parameter.setEnableHooks(enableHooks);
		parameter.setCart(cart);
		return parameter;
	}
	
	protected CustomerAccountService getCustomerAccountService()
	{
		return customerAccountService;
	}

	@Required
	public void setCustomerAccountService(final CustomerAccountService customerAccountService)
	{
		this.customerAccountService = customerAccountService;
	}
	
	public boolean hasCheckoutCart()
	{
		return getCartFacade().hasSessionCart();
	}

	protected CartModel getCart()
	{
		return hasCheckoutCart() ? getCartService().getSessionCart() : null;
	}
	
	protected CartFacade getCartFacade()
	{
		return cartFacade;
	}

	protected void setCartFacade(final CartFacade cartFacade)
	{
		this.cartFacade = cartFacade;
	}
	
	protected CartData getSessionCart()
	{
		return cartFacade.getSessionCart();
	}
	
	protected CommerceCheckoutService getCommerceCheckoutService()
	{
		return commerceCheckoutService;
	}

	@Required
	public void setCommerceCheckoutService(final CommerceCheckoutService commerceCheckoutService)
	{
		this.commerceCheckoutService = commerceCheckoutService;
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
    
}

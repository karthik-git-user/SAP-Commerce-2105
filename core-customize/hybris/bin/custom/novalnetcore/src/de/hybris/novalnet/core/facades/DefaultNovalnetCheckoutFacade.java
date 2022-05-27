
package de.hybris.novalnet.core.facades;

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
import de.hybris.platform.core.model.user.UserModel;
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
import de.hybris.novalnet.core.model.NovalnetPaymentInfoModel;
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

//~ import static com.adyen.constants.ApiConstants.ThreeDS2Property.THREEDS2_CHALLENGE_TOKEN;
//~ import static com.adyen.constants.ApiConstants.ThreeDS2Property.THREEDS2_FINGERPRINT_TOKEN;
//~ import static com.adyen.constants.HPPConstants.Response.SHOPPER_LOCALE;
//~ import static com.adyen.v6.constants.Adyenv6coreConstants.ISSUER_PAYMENT_METHODS;
//~ import static com.adyen.v6.constants.Adyenv6coreConstants.KLARNA;
//~ import static com.adyen.v6.constants.Adyenv6coreConstants.OPENINVOICE_METHODS_ALLOW_SOCIAL_SECURITY_NUMBER;
//~ import static com.adyen.v6.constants.Adyenv6coreConstants.OPENINVOICE_METHODS_API;
//~ import static com.adyen.v6.constants.Adyenv6coreConstants.PAYBRIGHT;
//~ import static com.adyen.v6.constants.Adyenv6coreConstants.PAYMENT_METHOD;
//~ import static com.adyen.v6.constants.Adyenv6coreConstants.PAYMENT_METHODS_ALLOW_SOCIAL_SECURITY_NUMBER;
//~ import static com.adyen.v6.constants.Adyenv6coreConstants.PAYMENT_METHOD_AMAZONPAY;
//~ import static com.adyen.v6.constants.Adyenv6coreConstants.PAYMENT_METHOD_APPLEPAY;
//~ import static com.adyen.v6.constants.Adyenv6coreConstants.PAYMENT_METHOD_BOLETO;
//~ import static com.adyen.v6.constants.Adyenv6coreConstants.PAYMENT_METHOD_BOLETO_SANTANDER;
//~ import static com.adyen.v6.constants.Adyenv6coreConstants.PAYMENT_METHOD_MULTIBANCO;
//~ import static com.adyen.v6.constants.Adyenv6coreConstants.PAYMENT_METHOD_SCHEME;
//~ import static com.adyen.v6.constants.Adyenv6coreConstants.PAYMENT_METHOD_SEPA_DIRECTDEBIT;
//~ import static com.adyen.v6.constants.Adyenv6coreConstants.RATEPAY;
import static de.hybris.platform.order.impl.DefaultCartService.SESSION_CART_PARAMETER_NAME;

/**
 * Adyen Checkout Facade for initiating payments using CC or APM
 */
public class DefaultNovalnetCheckoutFacade implements NovalnetCheckoutFacade {

    public static final String DETAILS = "details";

    private BaseStoreService baseStoreService;
    private SessionService sessionService;
    private CartService cartService;
    private OrderFacade orderFacade;
    private CheckoutFacade checkoutFacade;
    //~ private AdyenTransactionService adyenTransactionService;
    //~ private OrderRepository orderRepository;
    private CheckoutCustomerStrategy checkoutCustomerStrategy;
    private ModelService modelService;
    private CommonI18NService commonI18NService;
    private KeyGenerator keyGenerator;
    //~ private PaymentsResponseConverter paymentsResponseConverter;
    //~ private PaymentsDetailsResponseConverter paymentsDetailsResponseConverter;
    private FlexibleSearchService flexibleSearchService;
    private Converter<AddressData, AddressModel> addressReverseConverter;
    private Converter<CountryModel, CountryData> countryConverter;
    private Converter<OrderModel, OrderData> orderConverter;
    private CartFactory cartFactory;
    private CalculationService calculationService;
    private Populator<AddressModel, AddressData> addressPopulator;


    @Resource(name = "i18NFacade")
    private I18NFacade i18NFacade;

    @Resource(name = "configurationService")
    private ConfigurationService configurationService;

    public static final Logger LOGGER = Logger.getLogger(DefaultNovalnetCheckoutFacade.class);

    public static final String SESSION_PENDING_ORDER_CODE = "adyen_pending_order_code";
    public static final String SESSION_CSE_TOKEN = "adyen_cse_token";
    public static final String SESSION_SF_CARD_NUMBER = "encryptedCardNumber";
    public static final String SESSION_SF_EXPIRY_MONTH = "encryptedExpiryMonth";
    public static final String SESSION_SF_EXPIRY_YEAR = "encryptedExpiryYear";
    public static final String SESSION_SF_SECURITY_CODE = "encryptedSecurityCode";
    public static final String SESSION_CARD_BRAND = "cardBrand";
    public static final String MODEL_SELECTED_PAYMENT_METHOD = "selectedPaymentMethod";
    public static final String MODEL_PAYMENT_METHODS = "paymentMethods";
    public static final String MODEL_CREDIT_CARD_LABEL = "creditCardLabel";
    public static final String MODEL_ALLOWED_CARDS = "allowedCards";
    public static final String MODEL_REMEMBER_DETAILS = "showRememberTheseDetails";
    public static final String MODEL_STORED_CARDS = "storedCards";
    public static final String MODEL_DF_URL = "dfUrl";
    public static final String MODEL_CLIENT_KEY = "clientKey";
    public static final String MODEL_MERCHANT_ACCOUNT = "merchantAccount";
    public static final String MODEL_CHECKOUT_SHOPPER_HOST = "checkoutShopperHost";
    public static final String DF_VALUE = "dfValue";
    public static final String MODEL_OPEN_INVOICE_METHODS = "openInvoiceMethods";
    public static final String MODEL_SHOW_SOCIAL_SECURITY_NUMBER = "showSocialSecurityNumber";
    public static final String MODEL_SHOW_BOLETO = "showBoleto";
    public static final String MODEL_SHOW_POS = "showPos";
    public static final String MODEL_SHOW_COMBO_CARD = "showComboCard";
    public static final String CHECKOUT_SHOPPER_HOST_TEST = "checkoutshopper-test.adyen.com";
    public static final String CHECKOUT_SHOPPER_HOST_LIVE = "checkoutshopper-live.adyen.com";
    public static final String MODEL_ISSUER_LISTS = "issuerLists";
    public static final String MODEL_CONNECTED_TERMINAL_LIST = "connectedTerminalList";
    public static final String MODEL_ENVIRONMENT_MODE = "environmentMode";
    public static final String MODEL_AMOUNT = "amount";
    public static final String MODEL_IMMEDIATE_CAPTURE = "immediateCapture";
    public static final String MODEL_PAYPAL_MERCHANT_ID = "paypalMerchantId";
    public static final String MODEL_COUNTRY_CODE = "countryCode";
    public static final String MODEL_APPLEPAY_MERCHANT_IDENTIFIER = "applePayMerchantIdentifier";
    public static final String MODEL_APPLEPAY_MERCHANT_NAME = "applePayMerchantName";
    public static final String MODEL_AMAZONPAY_CONFIGURATION = "amazonPayConfiguration";
    public static final String MODEL_DELIVERY_ADDRESS = "deliveryAddress";
    public static final String ECOMMERCE_SHOPPER_INTERACTION = "Ecommerce";
    public static final String MODEL_CARD_HOLDER_NAME_REQUIRED = "cardHolderNameRequired";
    public static final String IS_CARD_HOLDER_NAME_REQUIRED_PROPERTY = "isCardHolderNameRequired";

    public DefaultNovalnetCheckoutFacade() {
    }

    @Override
    public PaymentDetailsWsDTO addPaymentDetails(PaymentDetailsWsDTO paymentDetails) {
		LOGGER.info("++++++++++++++++++test==========================207");
        CartModel cartModel = cartService.getSessionCart();

        final AddressModel billingAddress = createBillingAddress(paymentDetails);

        NovalnetPaymentInfoModel paymentInfo = createPaymentInfo(cartModel, paymentDetails);
        paymentInfo.setBillingAddress(billingAddress);
        billingAddress.setOwner(paymentInfo);

        modelService.save(paymentInfo);

        cartModel.setPaymentInfo(paymentInfo);
        modelService.save(cartModel);

        return paymentDetails;
    }
    
    public PaymentInfoModel createPaymentInfo(final CartModel cartModel, PaymentDetailsWsDTO paymentDetails) {
        NovalnetPaymentInfoModel paymentInfoModel = new NovalnetPaymentInfoModel();
        final UserModel currentUser = getCurrentUserForCheckout();
		paymentInfoModel.setPaymentEmailAddress('karthik_m@novalnetsolutions.com');
		paymentInfoModel.setDuplicate(Boolean.FALSE);
		paymentInfoModel.setSaved(Boolean.TRUE);
		paymentInfoModel.setUser(currentUser);
		paymentInfoModel.setPaymentInfo('');
		paymentInfoModel.setOrderHistoryNotes('');
		paymentInfoModel.setPaymentProvider('NovalnetCreditCard');
		paymentInfoModel.setPaymentGatewayStatus('CO=nnfirmed');
        return paymentInfo;
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

   

    //~ @Override
    //~ public PaymentDetailsListWsDTO getPaymentDetails(String userId) throws IOException, ApiException {
        //~ CustomerModel customer = getCheckoutCustomerStrategy().getCurrentUserForCheckout();

        //~ List<RecurringDetail> recurringDetails = getAdyenPaymentService().getStoredCards(customer.getCustomerID());

        //~ PaymentDetailsListWsDTO paymentDetailsListWsDTO = new PaymentDetailsListWsDTO();
        //~ paymentDetailsListWsDTO.setPayments(toPaymentDetails(recurringDetails));

        //~ return paymentDetailsListWsDTO;
    //~ }

    
    protected String generateCcPaymentInfoCode(final CartModel cartModel) {
        return cartModel.getCode() + "_" + UUID.randomUUID();
    }

    


    private boolean hasUserContextChanged(OrderModel orderModel, CartModel cartModel) {
        return !orderModel.getUser().equals(cartModel.getUser())
                || !orderModel.getStore().equals(cartModel.getStore());
    }

    private boolean getHolderNameRequired() {
        boolean holderNameRequired = true;
        Configuration configuration = this.configurationService.getConfiguration();
        if (configuration != null && configuration.containsKey(IS_CARD_HOLDER_NAME_REQUIRED_PROPERTY)) {
            holderNameRequired = configuration.getBoolean(IS_CARD_HOLDER_NAME_REQUIRED_PROPERTY);
        }
        return holderNameRequired;
    }

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

    public KeyGenerator getKeyGenerator() {
        return keyGenerator;
    }

    public void setKeyGenerator(KeyGenerator keyGenerator) {
        this.keyGenerator = keyGenerator;
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

}

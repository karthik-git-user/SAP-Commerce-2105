package novalnet.controllers.v2;

import de.hybris.platform.acceleratorfacades.order.AcceleratorCheckoutFacade;
import de.hybris.platform.acceleratorfacades.payment.data.PaymentSubscriptionResultData;
import de.hybris.platform.acceleratorservices.payment.data.PaymentData;
import de.hybris.platform.acceleratorservices.payment.data.PaymentErrorField;
import de.hybris.platform.acceleratorocc.exceptions.PaymentProviderException;
import novalnet.controllers.InvalidPaymentInfoException;
import novalnet.controllers.NoCheckoutCartException;
import novalnet.controllers.UnsupportedRequestException;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.novalnetocc.dto.payment.PaymentRequestWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.OrderWsDTO;
import de.hybris.platform.acceleratorocc.dto.payment.SopPaymentDetailsWsDTO;
import de.hybris.platform.acceleratorocc.payment.facade.CommerceWebServicesPaymentFacade;
import de.hybris.platform.acceleratorocc.validator.SopPaymentDetailsValidator;
import de.hybris.platform.commercefacades.order.CheckoutFacade;
import de.hybris.platform.commercefacades.order.data.CCPaymentInfoData;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.commercefacades.storelocator.data.PointOfServiceDataList;
import de.hybris.platform.commercefacades.user.UserFacade;
import de.hybris.platform.commerceservices.order.CommerceCartModificationException;
import de.hybris.platform.commerceservices.request.mapping.annotation.RequestMappingOverride;
import de.hybris.platform.commerceservices.request.mapping.annotation.ApiVersion;
import de.hybris.platform.commercewebservicescommons.dto.order.CartModificationListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.PaymentDetailsWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.store.PointOfServiceListWsDTO;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.PaymentAuthorizationException;
import de.hybris.platform.commercewebservicescommons.annotation.SiteChannelRestriction;
//~ import de.hybris.platform.commercewebservices.core.request.support.impl.PaymentProviderRequestSupportedStrategy;
import de.hybris.platform.webservicescommons.errors.exceptions.WebserviceValidationException;
import de.hybris.platform.webservicescommons.mapping.DataMapper;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdUserIdAndCartIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdAndUserIdParam;
import de.hybris.platform.commercefacades.order.data.CartModificationDataList;
import de.hybris.platform.core.model.user.AddressModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.store.BaseStoreModel;
import de.hybris.platform.commercefacades.order.CartFacade;
import de.hybris.platform.commercewebservicescommons.strategies.CartLoaderStrategy;
//~ import novalnet.facades.NovalnetOccFacade;

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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import springfox.documentation.annotations.ApiIgnore;

import static de.hybris.platform.webservicescommons.mapping.FieldSetLevelHelper.DEFAULT_LEVEL;
import de.hybris.platform.webservicescommons.mapping.FieldSetLevelHelper;
import de.hybris.platform.store.services.BaseStoreService;

import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.io.ObjectOutputStream;
import java.net.URL;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;
import org.json.JSONObject;

import java.net.MalformedURLException;

import java.nio.charset.StandardCharsets;

import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.UnknownHostException;
import java.util.Base64;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Arrays;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Type;

import java.io.*;


@Controller
@RequestMapping(value = "/{baseSiteId}")
@ApiVersion("v2")
@Api(tags = "Novalnet Carts")
public class NovalnetCartsController
{
	private final static Logger LOG = Logger.getLogger(NovalnetCartsController.class);
	
	protected static final String DEFAULT_FIELD_SET = FieldSetLevelHelper.DEFAULT_LEVEL;

	@Resource(name = "acceleratorCheckoutFacade")
	private AcceleratorCheckoutFacade acceleratorCheckoutFacade;
	@Resource(name = "dataMapper")
	private DataMapper dataMapper;
	@Resource(name = "commerceWebServicesPaymentFacade")
	private CommerceWebServicesPaymentFacade commerceWebServicesPaymentFacade;
	@Resource(name = "userFacade")
	private UserFacade userFacade;
	@Resource(name = "checkoutFacade")
	private CheckoutFacade checkoutFacade;
	@Resource(name = "sopPaymentDetailsValidator")
	private SopPaymentDetailsValidator sopPaymentDetailsValidator;
	@Resource(name = "cartLoaderStrategy")
	private CartLoaderStrategy cartLoaderStrategy;
	private BaseStoreService baseStoreService;
	
	private static final String PAYMENT_MAPPING = "accountHolderName,cardNumber,cardType,cardTypeData(code),expiryMonth,expiryYear,issueNumber,startMonth,startYear,subscriptionId,defaultPaymentInfo,saved,billingAddress(titleCode,firstName,lastName,line1,line2,town,postalCode,country(isocode),region(isocode),defaultAddress)";
	protected static final String API_COMPATIBILITY_B2C_CHANNELS = "api.compatibility.b2c.channels";
	//~ @Resource(name = "novalnetOccFacade")
    //~ NovalnetOccFacade novalnetOccFacade;
    //~ @Resource(name = "paymentProviderRequestSupportedStrategy")
	//~ private PaymentProviderRequestSupportedStrategy paymentProviderRequestSupportedStrategy;
	

	@RequestMapping(value = "/{cartId}/payment/sop/request", method = RequestMethod.GET)
	@ResponseBody
	@RequestMappingOverride
	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT" })
	@ApiOperation(nickname = "getSopPaymentRequestDetails", value = "Get information needed for create subscription", notes =
			"Returns the necessary information for creating a subscription that contacts the "
					+ "payment provider directly. This information contains the payment provider URL and a list of parameters that are needed to create the subscription.")
	public PaymentRequestWsDTO getSopPaymentRequestDetails(@ApiParam(value =
			"The URL that the payment provider uses to return payment information. Possible values for responseUrl include the following: “orderPage_cancelResponseURL”, "
					+ "“orderPage_declineResponseURL”, and “orderPage_receiptResponseURL”.", required = true) @RequestParam final String responseUrl,
			@ApiParam(value = "Define which url should be returned") @RequestParam(defaultValue = "false") final boolean extendedMerchantCallback,
			@ApiParam(value = "Base site identifier", required = true) @PathVariable final String baseSiteId,
			@ApiParam(value = "User identifier or one of the literals : 'current' for currently authenticated user, 'anonymous' for anonymous user", required = true) @PathVariable final String userId,
			@ApiParam(value = "Cart identifier: cart code for logged in user, cart guid for anonymous user, 'current' for the last modified cart", required = true) @PathVariable final String cartId,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_LEVEL) final String fields)
	{
		LOG.info("=========10NovalnetOccFacade5=========");
		final PaymentData paymentData = commerceWebServicesPaymentFacade.beginSopCreateSubscription(responseUrl,
				buildMerchantCallbackUrl(extendedMerchantCallback, baseSiteId, userId, cartId));
				//~ paymentData.setPostUrl("https://paygate.novalnet.de/paygate.jsp?vendor=4&product=14&key=6&hfooter=0&lhide=1&shide=1&skip_cfm=1");
		LOG.info(paymentData.getPostUrl());	
		//~ LOG.info("=========108=========");
		//~ final BaseStoreModel baseStore = novalnetOccFacade.getBaseStoreModel();
		//~ LOG.info(baseStore.getNovalnetPaymentAccessKey().trim());	
			
		final PaymentRequestWsDTO result = dataMapper.map(paymentData, PaymentRequestWsDTO.class, fields);
		return result;
	}

	/**
	 * Method build merchant callback url for given parameters
	 *
	 * @param extendedMerchantCallback
	 * 		Define which url should be returned
	 * @param baseSiteId
	 * 		Base site identifier
	 * @param userId
	 * 		User identifier
	 * @param cartId
	 * 		Cart identifier
	 * @return merchant callback url
	 */
	protected String buildMerchantCallbackUrl(final boolean extendedMerchantCallback, final String baseSiteId, final String userId,
			final String cartId)
	{
		if (extendedMerchantCallback)
		{
			return "/v2/" + baseSiteId + "/integration/users/" + userId + "/carts/" + cartId + "/payment/sop/response";
		}
		else
		{
			return "/v2/" + baseSiteId + "/integration/merchant_callback";
		}
	}

	@RequestMapping(value = "/{cartId}/payment/sop/response", method = RequestMethod.POST)
	@ResponseBody
	@RequestMappingOverride
	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT" })
	@ApiOperation(nickname = "doHandleSopPaymentResponse", value = "Handles response from payment provider and create payment details", notes =
			"Handles the response from the payment provider and creates payment details."
					+ "\n\nNote, the “Try it out” button is not enabled for this method (always returns an error) because the Extended Carts Controller handles parameters differently, depending "
					+ "on which payment provider is used. For more information about this controller, please refer to the “acceleratorocc AddOn” documentation on help.hybris.com.")
	@ApiBaseSiteIdUserIdAndCartIdParam
	public PaymentDetailsWsDTO doHandleSopPaymentResponse(@ApiIgnore final HttpServletRequest request,
			@ApiIgnore final SopPaymentDetailsWsDTO sopPaymentDetails,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_LEVEL) final String fields)
	{
		LOG.info("-------------170==============");
		final Errors errors = validate(sopPaymentDetails, "SOP data", sopPaymentDetailsValidator);
		final PaymentSubscriptionResultData paymentSubscriptionResultData = commerceWebServicesPaymentFacade
				.completeSopCreateSubscription(getParameterMap(request), sopPaymentDetails.isSavePaymentInfo(),
						sopPaymentDetails.isDefaultPayment());

		final CCPaymentInfoData paymentInfoData = handlePaymentSubscriptionResultData(paymentSubscriptionResultData, errors);
		if (userFacade.getCCPaymentInfos(true).size() <= 1)
		{
			userFacade.setDefaultPaymentInfo(paymentInfoData);
		}
		checkoutFacade.setPaymentDetails(paymentInfoData.getId());

		return dataMapper.map(paymentInfoData, PaymentDetailsWsDTO.class, fields);
	}

	/**
	 * Method analyze payment subscription result data. If create subscription result is success it returns created payment
	 * info. Otherwise appropriate exception is thrown.
	 *
	 * @param paymentSubscriptionResultData
	 * 		Data to analyze
	 * @param errors
	 * 		Object storing validation errors. Can be null - then empty error object will be created
	 * @return payment info
	 */
	protected CCPaymentInfoData handlePaymentSubscriptionResultData(
			final PaymentSubscriptionResultData paymentSubscriptionResultData, Errors errors)
	{
		if (paymentSubscriptionResultData.isSuccess() && paymentSubscriptionResultData.getStoredCard() != null && StringUtils
				.isNotBlank(paymentSubscriptionResultData.getStoredCard().getSubscriptionId()))
		{
			return paymentSubscriptionResultData.getStoredCard();
		}
		else if (paymentSubscriptionResultData.getErrors() != null && !paymentSubscriptionResultData.getErrors().isEmpty())
		{
			SopPaymentDetailsWsDTO sopPaymentDetailsWsDTO = null;
			if (errors == null)
			{
				sopPaymentDetailsWsDTO = new SopPaymentDetailsWsDTO();
				errors = new BeanPropertyBindingResult(sopPaymentDetailsWsDTO, "SOP data");
			}

			for (final PaymentErrorField paymentErrorField : paymentSubscriptionResultData.getErrors().values())
			{
				if (paymentErrorField.isMissing())
				{
					LOG.error("Missing: " + paymentErrorField.getName());
					errors.rejectValue(paymentErrorField.getName(), "field.required", "Please enter a value for this field");
				}
				if (paymentErrorField.isInvalid())
				{
					try
					{
						if (sopPaymentDetailsWsDTO != null)
						{
							PropertyUtils.setProperty(sopPaymentDetailsWsDTO, paymentErrorField.getName(), "invalid");
						}
					}
					catch (final Exception e)
					{
						LOG.error(e.getMessage(), e);
					}
					LOG.error("Invalid: " + paymentErrorField.getName());
					errors.rejectValue(paymentErrorField.getName(), "field.invalid", new Object[] { paymentErrorField.getName() },
							"This value is invalid for this field");
				}
			}
			throw new WebserviceValidationException(errors);
		}
		else if (paymentSubscriptionResultData.getDecision() != null && "error"
				.equalsIgnoreCase(paymentSubscriptionResultData.getDecision()))
		{
			LOG.error("Failed to create subscription. Error occurred while contacting external payment services.");
			throw new PaymentProviderException(
					"Failed to create subscription. Decision :" + paymentSubscriptionResultData.getDecision(),
					paymentSubscriptionResultData.getResultCode());
		}
		throw new PaymentProviderException(
				"Failed to create payment details. Decision :" + paymentSubscriptionResultData.getDecision(),
				paymentSubscriptionResultData.getResultCode());
	}

	@RequestMapping(value = "/{cartId}/payment/sop/response", method = RequestMethod.GET)
	@ResponseBody
	@RequestMappingOverride
	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT" })
	@ApiOperation(nickname = "getSopPaymentResponse", value = "Get information about create subscription request results", notes =
			"Returns information related to creating subscription request results. "
					+ "If there is no response from the payment provider, a \"202 Accepted\" status is returned. If the subscription is created successfully, the payment details "
					+ "are returned. Otherwise, an error response is returned.\n\nNote, the “Try it out” button is not enabled for this method (always returns an error) because "
					+ "the Extended Carts Controller handles parameters differently, depending on which payment provider is used. For more information about this controller, please "
					+ "refer to the “acceleratorocc AddOn” documentation on help.hybris.com.")
	@ApiBaseSiteIdUserIdAndCartIdParam
	public PaymentDetailsWsDTO getSopPaymentResponse(@PathVariable final String cartId,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_LEVEL) final String fields,
			@ApiIgnore final HttpServletResponse response)
	{
		LOG.info("Base store 267");
		final PaymentSubscriptionResultData paymentSubscriptionResultData = commerceWebServicesPaymentFacade
				.getPaymentSubscriptionResult(cartId);
		if (paymentSubscriptionResultData == null) //still waiting for payment provider response
		{
			response.setStatus(HttpServletResponse.SC_ACCEPTED);
			return null;
		}

		final CCPaymentInfoData paymentInfoData = handlePaymentSubscriptionResultData(paymentSubscriptionResultData, null);
		return dataMapper.map(paymentInfoData, PaymentDetailsWsDTO.class, fields);
	}

	@RequestMapping(value = "/{cartId}/payment/sop/response", method = RequestMethod.DELETE)
	@ResponseBody
	@RequestMappingOverride
	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT" })
	@ApiOperation(nickname = "removeSopPaymentResponse", value = "Deletes payment provider response related to cart.", notes = "Deletes the payment provider response related to the specified cart.")
	@ApiBaseSiteIdUserIdAndCartIdParam
	public void removeSopPaymentResponse(@PathVariable final String cartId)
	{
		commerceWebServicesPaymentFacade.removePaymentSubscriptionResult(cartId);
	}

	protected Map<String, String> getParameterMap(final HttpServletRequest request)
	{
		final Map<String, String> map = new HashMap<>();
		final Enumeration myEnum = request.getParameterNames();
		while (myEnum.hasMoreElements())
		{
			final String paramName = (String) myEnum.nextElement();
			final String paramValue = request.getParameter(paramName);
			map.put(paramName, paramValue);
		}
		return map;
	}

	protected Errors validate(final Object object, final String objectName, final Validator validator)
	{
		final Errors errors = new BeanPropertyBindingResult(object, objectName);
		validator.validate(object, errors);
		if (errors.hasErrors())
		{
			throw new WebserviceValidationException(errors);
		}
		return errors;
	}
	
	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_CLIENT", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT" })
	@RequestMapping(value = "/users/{userId}/orders", method = RequestMethod.POST)
	@RequestMappingOverride
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	@SiteChannelRestriction(allowedSiteChannelsProperty = API_COMPATIBILITY_B2C_CHANNELS)
	@ApiOperation(nickname = "placeOrder", value = "Place a order.", notes = "Authorizes the cart and places the order. The response contains the new order data.")
	@ApiBaseSiteIdAndUserIdParam
	public void placeOrder(
			@ApiParam(value = "Cart code for logged in user, cart GUID for guest checkout", required = true) @RequestParam final String cartId,
			@ApiParam(value = "credit card hash", required = true) @RequestParam final String panHash,
			@ApiParam(value = "credit card hash", required = true) @RequestParam final String uniqId,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
			throws PaymentAuthorizationException, InvalidCartException, NoCheckoutCartException
	{
		LOG.info("placeOrder");
		LOG.info("+++++++++++++++++++335");
		LOG.info("+++++++++++++++++++335");
		LOG.info(panHash);
		LOG.info("+++++++++++++++++++335");
		
		BaseStoreModel baseStore = baseStoreService.getCurrentBaseStore();
		LOG.info(baseStore.getNovalnetPaymentAccessKey());
		LOG.info("+++++++++++++++++++349");
		
		
		
		
		
		final Map<String, Object> transactionParameters = new HashMap<String, Object>();
        final Map<String, Object> merchantParameters = new HashMap<String, Object>();
        final Map<String, Object> customerParameters = new HashMap<String, Object>();
        final Map<String, Object> billingParameters = new HashMap<String, Object>();
        final Map<String, Object> shippingParameters = new HashMap<String, Object>();
        final Map<String, Object> customParameters = new HashMap<String, Object>();
        final Map<String, Object> paymentParameters = new HashMap<String, Object>();
        final Map<String, Object> dataParameters = new HashMap<String, Object>();

        //~ final BaseStoreModel baseStore = this.getBaseStoreModel();

        //~ final Integer tariff = baseStore.getNovalnetTariffId();
        //~ final String apiKey = baseStore.getNovalnetAPIKey();
        //~ String token = "";

        //~ final CartData cartData = getCheckoutFacade().getCheckoutCart();

        //~ final String currency = cartData.getTotalPriceWithTax().getCurrencyIso();
        //~ final Map<String, Object> customerParameter = (Map<String, Object>) getSessionService().getAttribute("novalnetCustomerParams");
        //~ String customerNo = JaloSession.getCurrentSession().getUser().getPK().toString();
        //~ String currentPayment = getSessionService().getAttribute("selectedPaymentMethodId");
        //~ PaymentModeModel paymentModeModel = paymentModeService.getPaymentModeForCode(currentPayment);

        //~ String orderAmount = getSessionService().getAttribute("novalnetOrderAmount");
        //~ float floatAmount = Float.parseFloat(orderAmount);
        //~ BigDecimal orderAmountCents = BigDecimal.valueOf(floatAmount).multiply(BigDecimal.valueOf(CONVERT_TO_CENT_OR_SUCCESS_STATUS));
        //~ Integer orderAmountCent = orderAmountCents.intValue();
        //~ Integer testMode = 0;
        //~ boolean redirect = false;


        merchantParameters.put("signature", "n7ibc7ob5t|doU3HJVoym7MQ44qonbobljblnmdli0p|qJEH3gNbeWJfIHah||f7cpn7pc");
        merchantParameters.put("tariff", "30");

        customerParameters.put("first_name", "test");
        customerParameters.put("last_name", "user");
        customerParameters.put("email", "karthik_m@novalnetsolutions.com");
        //~ customerParameters.put("customer_ip", getRemoteIpAddr(request));
        customerParameters.put("customer_no", "2");
        customerParameters.put("gender", "u");


        billingParameters.put("street", "Feringastr. 4");
        billingParameters.put("city", "Unterföhring");
        billingParameters.put("zip","85774");
        billingParameters.put("country_code", "DE");
        
        shippingParameters.put("same_as_billing", "1");

        //~ String sameAsBilling = getSessionService().getAttribute("same_as_billing");
        //~ if ("1".equals(sameAsBilling)) {
            //~ shippingParameters.put("same_as_billing", sameAsBilling);
            //~ getSessionService().setAttribute("same_as_billing", null);
        //~ } else {
            //~ shippingParameters.put("street", customerParameter.get("shipping_street"));
            //~ shippingParameters.put("city", customerParameter.get("shipping_city"));
            //~ shippingParameters.put("zip", customerParameter.get("shipping_zip"));
            //~ shippingParameters.put("country_code", customerParameter.get("shipping_country"));
            //~ shippingParameters.put("first_name", customerParameter.get("shipping_first_name"));
            //~ shippingParameters.put("last_name", customerParameter.get("shipping_last_name"));
        //~ }
        

        customerParameters.put("billing", billingParameters);
        customerParameters.put("shipping", shippingParameters);

        transactionParameters.put("payment_type", "CREDITCARD");
        transactionParameters.put("currency", "EUR");
        transactionParameters.put("amount", "100");
        transactionParameters.put("system_name", "SAP Commerce Cloud");
        transactionParameters.put("system_version", "2105-NN1.0.1");
        
        //~ boolean verify_payment_data = false;

        //~ boolean oneClickShopping = false;
        //~ // Get shop current language
        //~ final Locale language = JaloSession.getCurrentSession().getSessionContext().getLocale();
        //~ final String languageCode = language.toString().toUpperCase();
        customParameters.put("lang", "EN");
		//~ Integer onholdOrderAmount = 0;
		 
        //~ if ("novalnetDirectDebitSepa".equals(currentPayment)) {

            //~ NovalnetDirectDebitSepaPaymentModeModel novalnetPaymentMethod = (NovalnetDirectDebitSepaPaymentModeModel) paymentModeModel;

            //~ if (novalnetPaymentMethod.getNovalnetTestMode()) {
                //~ testMode = 1;
            //~ }
            
            //~ // Form sepa duedate
            //~ Integer sepaDueDate = novalnetPaymentMethod.getNovalnetDueDate();
            //~ if (sepaDueDate != null && sepaDueDate > 2 && sepaDueDate < 14) {
                //~ transactionParameters.put("due_date", formatDate(sepaDueDate));
            //~ }
           
            //~ if(novalnetPaymentMethod != null) {
				//~ onholdOrderAmount = novalnetPaymentMethod.getNovalnetOnholdAmount();
				//~ if (onholdOrderAmount == null) { 
					 //~ onholdOrderAmount = 0;
				//~ }
			//~ } else {
				//~ LOGGER.error("onhold order amount is null");
			//~ }
				
            //~ if (PAYMENT_AUTHORIZE.equals(novalnetPaymentMethod.getNovalnetOnholdAction().toString()) && orderAmountCent >= onholdOrderAmount) {
                 //~ verify_payment_data = true;
            //~ }
            
            //~ boolean novalnetDirectDebitSepaStorePaymentData = getSessionService().getAttribute("novalnetDirectDebitSepaStorePaymentData");
				
				//~ if(getSessionService().getAttribute("novalnetDirectDebitSepatoken") != null) {
					//~ token =  getSessionService().getAttribute("novalnetDirectDebitSepatoken");
				//~ } else {
					//~ LOGGER.info("novalnetDirectDebitSepatoken is null");
					//~ token = "";
				//~ }
				
             
				 //~ if (Boolean.FALSE.equals(novalnetFacade.isGuestUser()) && Boolean.TRUE.equals(novalnetPaymentMethod.getNovalnetOneClickShopping()) && Boolean.TRUE.equals(novalnetDirectDebitSepaStorePaymentData)) {
					//~ transactionParameters.put("create_token", '1');
					//~ oneClickShopping = true;
				//~ }
				
				
				//~ if (Boolean.FALSE.equals(novalnetFacade.isGuestUser()) && Boolean.TRUE.equals(novalnetPaymentMethod.getNovalnetOneClickShopping()) && !"".equals(token)) {
					//~ paymentParameters.put("token", token);
					//~ getSessionService().setAttribute("novalnetDirectDebitSepatoken", null);
				//~ }

                //~ if("".equals(token)) {
					//~ String accountHolder = customerParameter.get("first_name").toString() + ' ' + customerParameter.get("last_name").toString();
					//~ paymentParameters.put("iban", getSessionService().getAttribute("novalnetDirectDebitSepaAccountIban").toString());
					//~ paymentParameters.put("bank_account_holder", accountHolder.replace("&", ""));
					//~ getSessionService().setAttribute("novalnetDirectDebitSepaAccountIban", null);
				//~ }
            

        //~ } else if ("novalnetGuaranteedDirectDebitSepa".equals(currentPayment)) {

            //~ NovalnetGuaranteedDirectDebitSepaPaymentModeModel novalnetPaymentMethod = (NovalnetGuaranteedDirectDebitSepaPaymentModeModel) paymentModeModel;

            //~ if (novalnetPaymentMethod.getNovalnetTestMode()) {
                //~ testMode = 1;
            //~ }
            
            //~ Integer guaranteedSepaDueDate = novalnetPaymentMethod.getNovalnetDueDate();
            //~ if (guaranteedSepaDueDate != null && guaranteedSepaDueDate > 2 && guaranteedSepaDueDate < 14) {
                //~ transactionParameters.put("due_date", formatDate(guaranteedSepaDueDate));
            //~ }
            
            //~ if(novalnetPaymentMethod != null) {
				//~ onholdOrderAmount = novalnetPaymentMethod.getNovalnetOnholdAmount();
				//~ if (onholdOrderAmount == null) { 
					 //~ onholdOrderAmount = 0;
				//~ }
			//~ } else {
				//~ LOGGER.error("onhold order amount is null");
			//~ }
			
			//~ boolean novalnetGuaranteedDirectDebitSepaStorePaymentData = getSessionService().getAttribute("novalnetGuaranteedDirectDebitSepaStorePaymentData");
				
				//~ if(getSessionService().getAttribute("novalnetDirectDebitSepatoken") != null) {
					//~ token =  getSessionService().getAttribute("novalnetDirectDebitSepatoken");
				//~ } else {
					//~ LOGGER.info("novalnetDirectDebitSepatoken is null");
					//~ token = "";
				//~ }
				
             
				 //~ if (Boolean.FALSE.equals(novalnetFacade.isGuestUser()) && Boolean.TRUE.equals(novalnetPaymentMethod.getNovalnetOneClickShopping()) && Boolean.TRUE.equals(novalnetGuaranteedDirectDebitSepaStorePaymentData)) {
					//~ transactionParameters.put("create_token", '1');
					//~ oneClickShopping = true;
				//~ }
				
				
				//~ if (Boolean.FALSE.equals(novalnetFacade.isGuestUser()) && Boolean.TRUE.equals(novalnetPaymentMethod.getNovalnetOneClickShopping()) && !"".equals(token)) {
					//~ paymentParameters.put("token", token);
					//~ getSessionService().setAttribute("novalnetDirectDebitSepatoken", null);
				//~ }

                //~ if("".equals(token)) {
					//~ String accountHolder = customerParameter.get("first_name").toString() + ' ' + customerParameter.get("last_name").toString();
					//~ paymentParameters.put("iban", getSessionService().getAttribute("novalnetGuaranteedDirectDebitSepaAccountIban").toString());
					//~ paymentParameters.put("bank_account_holder", accountHolder.replace("&", ""));
					//~ getSessionService().setAttribute("novalnetGuaranteedDirectDebitSepaAccountIban", null);
				//~ }
				
				//~ String dob = getSessionService().getAttribute("novalnetGuaranteedDirectDebitSepaDateOfBirth");
				//~ customerParameters.put("birth_date", dob);
            
            //~ if (PAYMENT_AUTHORIZE.equals(novalnetPaymentMethod.getNovalnetOnholdAction().toString()) && orderAmountCent >= onholdOrderAmount) {
                  //~ verify_payment_data = true;
            //~ }
        //~ } else if ("novalnetPayPal".equals(currentPayment)) {
            //~ redirect = true;
            //~ NovalnetPayPalPaymentModeModel novalnetPaymentMethod = (NovalnetPayPalPaymentModeModel) paymentModeModel;
            
            //~ if(getSessionService().getAttribute("novalnetPayPaltoken") != null) {
				//~ token =  getSessionService().getAttribute("novalnetPayPaltoken");
			//~ } else {
				//~ LOGGER.info("novalnetPayPaltoken is null");
				//~ token = "";
			//~ }
			
			//~ boolean novalnetPayPalStorePaymentData = getSessionService().getAttribute("novalnetPayPalStorePaymentData");
                         
            //~ if (!novalnetFacade.isGuestUser() && novalnetPaymentMethod.getNovalnetOneClickShopping() && Boolean.TRUE.equals(novalnetPayPalStorePaymentData)) {
                //~ transactionParameters.put("create_token", '1');
                //~ oneClickShopping = true;
            //~ }
            
            
            //~ if (!novalnetFacade.isGuestUser() && novalnetPaymentMethod.getNovalnetOneClickShopping() && !"".equals(token)) {
                //~ paymentParameters.put("token", token);
                //~ getSessionService().setAttribute("novalnetPayPaltoken", null);
            //~ }
             
            //~ if(novalnetPaymentMethod != null) {
				//~ onholdOrderAmount = novalnetPaymentMethod.getNovalnetOnholdAmount();
				//~ if (onholdOrderAmount == null) { 
					 //~ onholdOrderAmount = 0;
				//~ }
			//~ } else {
				//~ LOGGER.info("onhold order amount is null");
			//~ }
			
            //~ if (PAYMENT_AUTHORIZE.equals(novalnetPaymentMethod.getNovalnetOnholdAction().toString()) && orderAmountCent >= onholdOrderAmount) {
                 //~ verify_payment_data = true;
            //~ }

            //~ if (novalnetPaymentMethod.getNovalnetTestMode()) {
                //~ testMode = 1;
            //~ }
        //~ } else if ("novalnetCreditCard".equals(currentPayment)) {
            //~ NovalnetCreditCardPaymentModeModel novalnetPaymentMethod = (NovalnetCreditCardPaymentModeModel) paymentModeModel;
            
            //~ if(novalnetPaymentMethod != null) {
				//~ onholdOrderAmount = novalnetPaymentMethod.getNovalnetOnholdAmount();
				//~ if (onholdOrderAmount == null) { 
					 //~ onholdOrderAmount = 0;
				//~ }
			//~ } else {
				//~ LOGGER.info("onhold order amount is null");
			//~ }
            
            //~ if (PAYMENT_AUTHORIZE.equals(novalnetPaymentMethod.getNovalnetOnholdAction().toString()) && orderAmountCent >= onholdOrderAmount) {
                 //~ verify_payment_data = true;
            //~ }

            //~ if (novalnetPaymentMethod.getNovalnetTestMode()) {
                //~ testMode = 1;
            //~ }
            
            //~ if(getSessionService().getAttribute("novalnetCreditCardtoken") != null) {
				//~ token =  getSessionService().getAttribute("novalnetCreditCardtoken");
			//~ } else {
				//~ LOGGER.info("novalnetCreditCardtoken is null");
				//~ token = "";
			//~ }
            
            //~ if(getSessionService().getAttribute("novalnetCreditCardtoken") != null) {
				//~ token =  getSessionService().getAttribute("novalnetCreditCardtoken");
			//~ } else {
				//~ LOGGER.info("onhold order amount is null");
				 //~ onholdOrderAmount = 0;
			//~ }
			
			//~ boolean novalnetCreditCardStorePaymentData = getSessionService().getAttribute("novalnetCreditCardStorePaymentData");

            //~ if (!novalnetFacade.isGuestUser() && novalnetPaymentMethod.getNovalnetOneClickShopping() && Boolean.TRUE.equals(novalnetCreditCardStorePaymentData)) {
                //~ transactionParameters.put("create_token", '1');
                //~ oneClickShopping = true;
            //~ }

            //~ String referenceTid = getSessionService().getAttribute("novalnetCreditCardReferenceTid");
            //~ if (!novalnetFacade.isGuestUser() && novalnetPaymentMethod.getNovalnetOneClickShopping() && !"".equals(token)) {
                 //~ paymentParameters.put("token", token);
                //~ getSessionService().setAttribute("novalnetCreditCardtoken", null);
            //~ } else {

                paymentParameters.put("pan_hash", panHash);
                paymentParameters.put("unique_id", uniqId);
                //~ String do_redirect = getSessionService().getAttribute("do_redirect").toString();
                
                //~ if(!"".equals(do_redirect)) {
					 //~ redirect = true;
				//~ }
                
                //~ getSessionService().setAttribute("novalnetCreditCardPanHash", null);

            //~ }
        //~ } else if ("novalnetInvoice".equals(currentPayment)) {
            //~ NovalnetInvoicePaymentModeModel novalnetPaymentMethod = (NovalnetInvoicePaymentModeModel) paymentModeModel;
            //~ transactionParameters.put("invoice_type", "INVOICE");

            //~ // Form invoice duedate
            //~ Integer invoiceDueDate = novalnetPaymentMethod.getNovalnetDueDate();
            //~ if (invoiceDueDate != null && invoiceDueDate > 7) {
                //~ transactionParameters.put("due_date", formatDate(invoiceDueDate));
            //~ }
            
            //~ if(novalnetPaymentMethod != null) {
				//~ onholdOrderAmount = novalnetPaymentMethod.getNovalnetOnholdAmount();
				//~ if (onholdOrderAmount == null) { 
					 //~ onholdOrderAmount = 0;
				//~ }
			//~ } else {
				//~ LOGGER.info("onhold order amount is null");
			//~ }
			
            //~ if (PAYMENT_AUTHORIZE.equals(novalnetPaymentMethod.getNovalnetOnholdAction().toString()) && orderAmountCent >= onholdOrderAmount) {
                  //~ verify_payment_data = true;
            //~ }


            //~ // Check for test mode
            //~ if (novalnetPaymentMethod.getNovalnetTestMode()) {
                //~ testMode = 1;
            //~ }
        //~ } else if ("novalnetPrepayment".equals(currentPayment)) {
			//~ NovalnetPrepaymentPaymentModeModel novalnetPaymentMethod = (NovalnetPrepaymentPaymentModeModel) paymentModeModel;
			//~ Integer prepaymentDueDate = novalnetPaymentMethod.getNovalnetDueDate();
            //~ if (prepaymentDueDate != null && PREPAYMENT_FROM_DATE >= 7 && prepaymentDueDate <= PREPAYMENT_TILL_DATE) {
                //~ transactionParameters.put("due_date", formatDate(prepaymentDueDate));
            //~ }
            //~ transactionParameters.put("invoice_type", "PREPAYMENT");

            //~ // Check for test mode
            //~ if (novalnetPaymentMethod.getNovalnetTestMode()) {
                //~ testMode = 1;
            //~ }
        //~ } else if ("novalnetMultibanco".equals(currentPayment)) {
			//~ NovalnetMultibancoPaymentModeModel novalnetPaymentMethod = (NovalnetMultibancoPaymentModeModel) paymentModeModel;
			//~ if (novalnetPaymentMethod.getNovalnetTestMode()) {
                //~ testMode = 1;
            //~ }
        //~ } else if ("novalnetGuaranteedInvoice".equals(currentPayment)) {
            //~ NovalnetGuaranteedInvoicePaymentModeModel novalnetPaymentMethod = (NovalnetGuaranteedInvoicePaymentModeModel) paymentModeModel;
            
            //~ if(novalnetPaymentMethod != null) {
				//~ onholdOrderAmount = novalnetPaymentMethod.getNovalnetOnholdAmount();
				//~ if (onholdOrderAmount == null) { 
					 //~ onholdOrderAmount = 0;
				//~ }
			//~ } else {
				//~ LOGGER.info("onhold order amount is null");
			//~ }
            
            //~ if (PAYMENT_AUTHORIZE.equals(novalnetPaymentMethod.getNovalnetOnholdAction().toString()) && orderAmountCent >= onholdOrderAmount) {
                 //~ verify_payment_data = true;
            //~ }
            
            //~ String dob = getSessionService().getAttribute("novalnetGuaranteedInvoiceDateOfBirth");
            //~ customerParameters.put("birth_date", dob);

            //~ // Check for test mode
            //~ if (novalnetPaymentMethod.getNovalnetTestMode()) {
                //~ testMode = 1;
            //~ }
        //~ } else if ("novalnetBarzahlen".equals(currentPayment)) {
            //~ NovalnetBarzahlenPaymentModeModel novalnetPaymentMethod = (NovalnetBarzahlenPaymentModeModel) paymentModeModel;

            //~ // Form Barzahlen slip expiry date
            //~ Integer slipExpiryDate = novalnetPaymentMethod.getNovalnetBarzahlenslipExpiryDate();
            //~ if (slipExpiryDate != null) {
                //~ transactionParameters.put("due_date", formatDate(slipExpiryDate));
            //~ }
            //~ if (novalnetPaymentMethod.getNovalnetTestMode()) {
                //~ testMode = 1;
            //~ }
        //~ } else if ("novalnetInstantBankTransfer".equals(currentPayment)) {
            //~ NovalnetInstantBankTransferPaymentModeModel novalnetPaymentMethod = (NovalnetInstantBankTransferPaymentModeModel) paymentModeModel;

            //~ // Redirect Flag
            //~ redirect = true;

            //~ // Check for test mode
            //~ if (novalnetPaymentMethod.getNovalnetTestMode()) {
                //~ testMode = 1;
            //~ }
        //~ } else if ("novalnetBancontact".equals(currentPayment)) {
            //~ NovalnetBancontactPaymentModeModel novalnetPaymentMethod = (NovalnetBancontactPaymentModeModel) paymentModeModel;

            //~ // Redirect Flag
            //~ redirect = true;

            //~ // Check for test mode
            //~ if (novalnetPaymentMethod.getNovalnetTestMode()) {
                //~ testMode = 1;
            //~ }
        //~ } else if ("novalnetIdeal".equals(currentPayment)) {
            //~ NovalnetIdealPaymentModeModel novalnetPaymentMethod = (NovalnetIdealPaymentModeModel) paymentModeModel;

            //~ // Redirect Flag
            //~ redirect = true;

            //~ // Check for test mode
            //~ if (novalnetPaymentMethod.getNovalnetTestMode()) {
                //~ testMode = 1;
            //~ }
        //~ } else if ("novalnetEps".equals(currentPayment)) {
            //~ NovalnetEpsPaymentModeModel novalnetPaymentMethod = (NovalnetEpsPaymentModeModel) paymentModeModel;

            //~ // Redirect Flag
            //~ redirect = true;

            //~ // Check for test mode
            //~ if (novalnetPaymentMethod.getNovalnetTestMode()) {
                //~ testMode = 1;
            //~ }

        //~ } else if ("novalnetGiropay".equals(currentPayment)) {
            //~ NovalnetGiropayPaymentModeModel novalnetPaymentMethod = (NovalnetGiropayPaymentModeModel) paymentModeModel;

            //~ // Redirect Flag
            //~ redirect = true;

            //~ // Check for test mode
            //~ if (novalnetPaymentMethod.getNovalnetTestMode()) {
                //~ testMode = 1;
            //~ }
        //~ } else if ("novalnetPostFinance".equals(currentPayment)) {
            //~ NovalnetPostFinancePaymentModeModel novalnetPaymentMethod = (NovalnetPostFinancePaymentModeModel) paymentModeModel;

            //~ // Redirect Flag
            //~ redirect = true;

            //~ // Check for test mode
            //~ if (novalnetPaymentMethod.getNovalnetTestMode()) {
                //~ testMode = 1;
            //~ }
        //~ } else if ("novalnetPostFinanceCard".equals(currentPayment)) {
            //~ NovalnetPostFinanceCardPaymentModeModel novalnetPaymentMethod = (NovalnetPostFinanceCardPaymentModeModel) paymentModeModel;

            //~ // Redirect Flag
            //~ redirect = true;

            //~ // Check for test mode
            //~ if (novalnetPaymentMethod.getNovalnetTestMode()) {
                //~ testMode = 1;
            //~ }
        //~ } else if ("novalnetPrzelewy24".equals(currentPayment)) {
            //~ NovalnetPrzelewy24PaymentModeModel novalnetPaymentMethod = (NovalnetPrzelewy24PaymentModeModel) paymentModeModel;

            //~ // Redirect Flag
            //~ redirect = true;

            //~ // Check for test mode
            //~ if (novalnetPaymentMethod.getNovalnetTestMode()) {
                //~ testMode = 1;
            //~ }
        //~ }

        //~ transactionParameters.put("test_mode", testMode);

        //~ if (redirect == true) {
            //~ final String currentUrl = request.getRequestURL().toString();
            //~ String returnUrl = currentUrl.replace("summary/placeOrder", "hop-response");
            //~ transactionParameters.put("return_url", returnUrl);
            //~ transactionParameters.put("error_return_url", returnUrl);
            //~ transactionParameters.put("payment_data", paymentParameters);
        //~ } else {

            transactionParameters.put("payment_data", paymentParameters);
        //~ }
        dataParameters.put("merchant", merchantParameters);
        dataParameters.put("customer", customerParameters);
        dataParameters.put("transaction", transactionParameters);
        dataParameters.put("custom", customParameters);

        Gson gson = new GsonBuilder().create();
        String jsonString = gson.toJson(dataParameters);

        String password = "a87ff679a2f3e71d9181a67b7542122c";
        String url = "https://payport.novalnet.de/v2/payment";
        //~ if(verify_payment_data == true) {
			//~ url = "https://payport.novalnet.de/v2/authorize";
		//~ }
        StringBuilder response = sendRequest(url, jsonString);
        //~ JSONObject tomJsonObject = new JSONObject(response.toString());
        //~ JSONObject resultJsonObject = tomJsonObject.getJSONObject("result");
        //~ JSONObject transactionJsonObject = tomJsonObject.getJSONObject("transaction");
		
		
		
		
	}
	
	public StringBuilder sendRequest(String url, String jsonString) {
        final BaseStoreModel baseStore = this.getBaseStoreModel();
        String password = baseStore.getNovalnetPaymentAccessKey().trim();
        StringBuilder response = new StringBuilder();

        try {
            String urly = url;
            URL obj = new URL(urly);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            byte[] postData = jsonString.getBytes(StandardCharsets.UTF_8);
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Charset", "utf-8");
            con.setRequestProperty("Accept", "application/json");
            con.setRequestProperty("X-NN-Access-Key", Base64.getEncoder().encodeToString(password.getBytes()));

            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.write(postData);
            wr.flush();
            wr.close();

            int responseCode = con.getResponseCode();
            BufferedReader iny = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String output;


            while ((output = iny.readLine()) != null) {
                response.append(output);
            }
            iny.close();
        } catch (MalformedURLException ex) {
            LOGGER.error("MalformedURLException ", ex);
        } catch (IOException ex) {
            LOGGER.error("IOException ", ex);
        }

        return response;

    }
	
	public BaseStoreService getBaseStoreService() {
        return baseStoreService;
    }

    public void setBaseStoreService(BaseStoreService baseStoreService) {
        this.baseStoreService = baseStoreService;
    }

}

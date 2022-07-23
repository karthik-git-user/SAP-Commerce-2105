package novalnet.controllers.v2;

import novalnet.controllers.NoCheckoutCartException;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.commercewebservicescommons.dto.order.OrderWsDTO;
import de.hybris.platform.commercefacades.order.CheckoutFacade;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.commerceservices.order.CommerceCartModificationException;
import de.hybris.platform.commerceservices.request.mapping.annotation.RequestMappingOverride;
import de.hybris.platform.commerceservices.request.mapping.annotation.ApiVersion;
import de.hybris.platform.commercewebservicescommons.dto.order.PaymentDetailsWsDTO;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.PaymentAuthorizationException;
import de.hybris.platform.commercewebservicescommons.annotation.SiteChannelRestriction;
import de.hybris.platform.webservicescommons.mapping.DataMapper;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdAndUserIdParam;
import de.hybris.platform.core.model.user.AddressModel;
import de.hybris.platform.store.BaseStoreModel;
import javax.annotation.Resource;

import java.util.HashMap;
import java.util.Map;
import java.math.BigDecimal;

import org.apache.log4j.Logger;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.http.HttpStatus;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import static de.hybris.platform.webservicescommons.mapping.FieldSetLevelHelper.DEFAULT_LEVEL;
import de.hybris.platform.webservicescommons.mapping.FieldSetLevelHelper;
import de.hybris.platform.store.services.BaseStoreService;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.search.SearchResult;
import java.net.URL;

import org.json.JSONObject;
import java.net.MalformedURLException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

import java.util.Base64;
import java.util.Locale;
import javax.annotation.Resource;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;

import de.hybris.novalnet.core.model.NovalnetPaymentInfoModel;
import de.hybris.novalnet.core.model.NovalnetCreditCardPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetDirectDebitSepaPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetPayPalPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetCallbackInfoModel;
import de.hybris.platform.core.model.order.payment.PaymentModeModel;
import de.hybris.platform.payment.model.PaymentTransactionEntryModel;
import de.hybris.platform.orderhistory.model.OrderHistoryEntryModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.core.enums.OrderStatus;
import de.hybris.platform.core.enums.PaymentStatus;
import de.hybris.platform.payment.model.PaymentTransactionModel;
import de.hybris.platform.commerceservices.strategies.CheckoutCustomerStrategy;
import de.hybris.platform.order.PaymentModeService;
import de.hybris.platform.commercefacades.order.OrderFacade;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.commercefacades.user.data.CountryData;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.core.model.c2l.CountryModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.order.CartFactory;
import de.hybris.platform.order.CalculationService;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.servicelayer.i18n.CommonI18NService;
import de.hybris.platform.jalo.JaloSession;
import de.novalnet.order.NovalnetOrderFacade;
import de.novalnet.beans.NnResponseData;
import novalnet.dto.payment.NnResponseWsDTO;
import de.novalnet.beans.NnPaymentDetailsData;
import novalnet.dto.payment.NnPaymentDetailsWsDTO;
import novalnet.dto.payment.NnRequestWsDTO;
import de.novalnet.beans.NnCreditCardData;
import de.novalnet.beans.NnDirectDebitSepaData;
import de.novalnet.beans.NnPayPalData;
import de.novalnet.beans.NnPaymentData;
import de.novalnet.beans.NnConfigData;
import novalnet.dto.payment.NnConfigWsDTO;
import java.text.NumberFormat;
import java.text.DecimalFormat;

@Controller
@RequestMapping(value = "/{baseSiteId}")
@ApiVersion("v2")
@Api(tags = "Novalnet Carts")
public class NovalnetOrdersController 
{
	private final static Logger LOG = Logger.getLogger(NovalnetOrdersController.class);
	
	protected static final String DEFAULT_FIELD_SET = FieldSetLevelHelper.DEFAULT_LEVEL;
	
    private BaseStoreModel baseStore;
    private CartData cartData;
    private CartModel cartModel;
    private String password;
   
    private static final String PAYMENT_AUTHORIZE = "AUTHORIZE";

	@Resource(name = "novalnetOrderFacade")
    NovalnetOrderFacade novalnetOrderFacade;
    
    @Resource(name = "dataMapper")
	private DataMapper dataMapper;
    
    @Resource
    private PaymentModeService paymentModeService;

    private static final String REQUEST_MAPPING = "paymentType,action,cartId,billingAddress(titleCode,firstName,lastName,line1,line2,town,postalCode,country(isocode),region(isocode),paymentData(panHash, uniqId))";
	
	protected static final String API_COMPATIBILITY_B2C_CHANNELS = "api.compatibility.b2c.channels";
	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_CLIENT", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT" })
	@RequestMapping(value = "/users/{userId}/orders", method = RequestMethod.POST)
	@RequestMappingOverride
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	@SiteChannelRestriction(allowedSiteChannelsProperty = API_COMPATIBILITY_B2C_CHANNELS)
	@ApiOperation(nickname = "placeOrder", value = "Place a order.", notes = "Authorizes the cart and places the order. The response contains the new order data.")
	@ApiBaseSiteIdAndUserIdParam
	public OrderWsDTO placeOrder(
			@ApiParam(value =
			"Request body parameter that contains details such as the name on the card (accountHolderName), the card number (cardNumber), the card type (cardType.code), "
					+ "the month of the expiry date (expiryMonth), the year of the expiry date (expiryYear), whether the payment details should be saved (saved), whether the payment details "
					+ "should be set as default (defaultPaymentInfo), and the billing address (billingAddress.firstName, billingAddress.lastName, billingAddress.titleCode, billingAddress.country.isocode, "
					+ "billingAddress.line1, billingAddress.line2, billingAddress.town, billingAddress.postalCode, billingAddress.region.isocode)\n\nThe DTO is in XML or .json format.", required = true) @RequestBody final NnRequestWsDTO orderRequest,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
			throws PaymentAuthorizationException, InvalidCartException, NoCheckoutCartException
	{

		NnRequestData requestData = getDataMapper().map(orderRequest, NnRequestData.class, REQUEST_MAPPING);
		requestData.get("action");
		LOG.info("action recieved from request : " + requestData.get("action"));

		JSONObject requestObject = new JSONObject(reqJsonString.toString());
		cartData = novalnetOrderFacade.loadCart(requestObject.get("cartId").toString());
		cartModel = novalnetOrderFacade.getCart();
		baseStore = novalnetOrderFacade.getBaseStoreModel();

		String action 				= requestObject.get("action").toString();
		final String emailAddress 	= JaloSession.getCurrentSession().getUser().getLogin();
		String responseString = "";

		final UserModel currentUser = novalnetOrderFacade.getCurrentUserForCheckout();
		String totalAmount 			= formatAmount(String.valueOf(cartData.getTotalPriceWithTax().getValue()));
		DecimalFormat decimalFormat = new DecimalFormat("##.##");
		String orderAmount 			= decimalFormat.format(Float.parseFloat(totalAmount));
		float floatAmount 			= Float.parseFloat(orderAmount);
		BigDecimal orderAmountCents = BigDecimal.valueOf(floatAmount).multiply(BigDecimal.valueOf(100));
		Integer orderAmountCent 	= orderAmountCents.intValue();
		final String currency 	  	= cartData.getTotalPriceWithTax().getCurrencyIso();
		final Locale language	  	= JaloSession.getCurrentSession().getSessionContext().getLocale();
		final String languageCode 	= language.toString().toUpperCase();

		password = baseStore.getNovalnetPaymentAccessKey().trim();
			
		if("create_order".equals(action)) {
			Map<String, Object> requsetDeatils = formPaymentRequest(requestObject, action, emailAddress, orderAmountCent, currency, languageCode);
        	StringBuilder response = sendRequest(requsetDeatils.get("paygateURL").toString(), requsetDeatils.get("jsonString").toString());
			responseString = response.toString();
		} else {
			responseString = getTransactionDetails(requestObject, languageCode);
		}

		JSONObject tomJsonObject 	= new JSONObject(responseString);
		JSONObject resultJsonObject = tomJsonObject.getJSONObject("result");
        
        if(!String.valueOf("100").equals(resultJsonObject.get("status_code").toString())) {
			final String statMessage = resultJsonObject.get("status_text").toString() != null ? resultJsonObject.get("status_text").toString() : resultJsonObject.get("status_desc").toString();
			 LOG.info("Error message recieved from novalnet for cart id: " + requestObject.get("cartId").toString() + " " + statMessage);
			throw new PaymentAuthorizationException();
		}

		JSONObject transactionJsonObject = tomJsonObject.getJSONObject("transaction");
		String[] successStatus = {"CONFIRMED", "ON_HOLD", "PENDING"};

        if (Arrays.asList(successStatus).contains(transactionJsonObject.get("status").toString())) {
		
			JSONObject customerJsonObject = tomJsonObject.getJSONObject("customer");
			JSONObject billingJsonObject = customerJsonObject.getJSONObject("billing");
	        
	        final AddressModel billingAddress = novalnetOrderFacade.getModelService().create(AddressModel.class);
			
			billingAddress.setFirstname(customerJsonObject.get("first_name").toString());
			billingAddress.setLastname(customerJsonObject.get("last_name").toString());
			if (billingJsonObject.has("street")) {
				billingAddress.setLine1(billingJsonObject.get("street").toString());
			}
			billingAddress.setLine2("");
			billingAddress.setTown(billingJsonObject.get("city").toString());
			billingAddress.setPostalcode(billingJsonObject.get("zip").toString());
			billingAddress.setCountry(novalnetOrderFacade.getCommonI18NService().getCountry(billingJsonObject.get("country_code").toString()));
			billingAddress.setEmail(emailAddress);
			billingAddress.setOwner(cartModel);

			String payment = (transactionJsonObject.get("payment_type").toString()).equals("CREDITCARD") ? "novalnetCreditCard" : ((transactionJsonObject.get("payment_type").toString()).equals("DIRECT_DEBIT_SEPA") ? "novalnetDirectDebitSepa" :((transactionJsonObject.get("payment_type").toString()).equals("PAYPAL") ? "novalnetPayPal": ""));

			OrderData orderData = createOrder(transactionJsonObject, payment, billingAddress, emailAddress, currentUser, orderAmountCent, currency, languageCode);
	        return dataMapper.map(orderData, OrderWsDTO.class, fields);
	    } else {
	    	final String statMessage = resultJsonObject.get("status_text").toString() != null ? resultJsonObject.get("status_text").toString() : resultJsonObject.get("status_desc").toString();
			 LOG.info("Error message recieved from novalnet for cart id: " + requestObject.get("cartId").toString() + " " + statMessage);
			throw new PaymentAuthorizationException();
	    }
	}
	
	public List<OrderModel> getOrderInfoModel(String orderCode) {
        // Initialize StringBuilder
        StringBuilder query = new StringBuilder();

        // Select query for fetch OrderModel
        query.append("SELECT {pk} from {" + OrderModel._TYPECODE + "} where {" + OrderModel.CODE
                + "} = ?code");
        FlexibleSearchQuery executeQuery = new FlexibleSearchQuery(query.toString());

        // Add query parameter
        executeQuery.addQueryParameter("code", orderCode);

        // Execute query
        SearchResult<OrderModel> result = novalnetOrderFacade.getFlexibleSearchService().search(executeQuery);
        return result.getResult();
    }

    public Map<String, Object> formPaymentRequest(JSONObject requestObject, String action, String emailAddress, Integer orderAmountCent, String currency, String languageCode) {

    	final Map<String, Object> transactionParameters = new HashMap<String, Object>();
		final Map<String, Object> merchantParameters 	= new HashMap<String, Object>();
		final Map<String, Object> customerParameters 	= new HashMap<String, Object>();
		final Map<String, Object> billingParameters 	= new HashMap<String, Object>();
		final Map<String, Object> shippingParameters	= new HashMap<String, Object>();
		final Map<String, Object> customParameters 		= new HashMap<String, Object>();
		final Map<String, Object> paymentParameters 	= new HashMap<String, Object>();
		final Map<String, Object> dataParameters 		= new HashMap<String, Object>();
		final Map<String, Object> responeParameters     = new HashMap<String, Object>();

		final AddressModel deliveryAddress 	= cartModel.getDeliveryAddress();

		boolean verify_payment_data = false;

		Integer testMode 			= 0;
		Integer onholdOrderAmount   = 0;
		String customerNo 			= JaloSession.getCurrentSession().getUser().getPK().toString();
		String currentPayment 		= requestObject.get("paymentType").toString();

		String payment = currentPayment.equals("CREDITCARD") ? "novalnetCreditCard" : (currentPayment.equals("DIRECT_DEBIT_SEPA") ? "novalnetDirectDebitSepa" :(currentPayment.equals("PAYPAL") ? "novalnetPayPal": ""));
		PaymentModeModel paymentModeModel = paymentModeService.getPaymentModeForCode(payment);

    	JSONObject billingObject = new JSONObject(requestObject.getJSONObject("billingAddress").toString());
		JSONObject countryObject = new JSONObject(billingObject.getJSONObject("country").toString());
		JSONObject regionObject  = new JSONObject(billingObject.getJSONObject("region").toString());

		String firstName 	= billingObject.get("firstName").toString();
		String lastName 	= billingObject.get("lastName").toString();
		String street1 		= billingObject.get("line1").toString();
		String street2 		= billingObject.get("line2").toString();
		String town 		= billingObject.get("town").toString();
		String zip 			= billingObject.get("postalCode").toString();
		String countryCode 	= countryObject.get("isocode").toString();

		Gson gson = new GsonBuilder().create();

		merchantParameters.put("signature", baseStore.getNovalnetAPIKey());
		merchantParameters.put("tariff", baseStore.getNovalnetTariffId());

		customerParameters.put("first_name", firstName);
		customerParameters.put("last_name", lastName);
		customerParameters.put("email", emailAddress);
		customerParameters.put("customer_no", customerNo);
		customerParameters.put("gender", "u");

		billingParameters.put("street", street1 + " " + street2);
		billingParameters.put("city", town);
		billingParameters.put("zip", zip);
		billingParameters.put("country_code", countryCode);

		if(deliveryAddress.getLine1().toString().toLowerCase().equals(street1.toLowerCase()) && deliveryAddress.getLine2().toString().toLowerCase().equals(street2.toLowerCase()) && deliveryAddress.getTown().toString().toLowerCase().equals(town.toLowerCase()) &&  deliveryAddress.getPostalcode().toString().equals(zip) && deliveryAddress.getCountry().getIsocode().toString().equals(countryCode)) {
		    shippingParameters.put("same_as_billing", 1);
		    LOG.info("The billing address is same as shipping address for cart id " + requestObject.get("cartId").toString());
	    } else {
	        shippingParameters.put("street", deliveryAddress.getLine1() + " " + deliveryAddress.getLine2());
	        shippingParameters.put("city", deliveryAddress.getTown());
	        shippingParameters.put("zip", deliveryAddress.getPostalcode());
	        shippingParameters.put("country_code", deliveryAddress.getCountry().getIsocode());
	        shippingParameters.put("first_name", deliveryAddress.getFirstname());
	        shippingParameters.put("last_name", deliveryAddress.getLastname());
	        LOG.info("The billing address differs from shipping address for cart id " + requestObject.get("cartId").toString());
	    }
		
		customerParameters.put("billing", billingParameters);
		customerParameters.put("shipping", shippingParameters);
		customParameters.put("lang", languageCode);

		transactionParameters.put("payment_type", currentPayment);
		transactionParameters.put("currency", currency);
		transactionParameters.put("amount", orderAmountCent);
		// transactionParameters.put("create_token", 1);
		transactionParameters.put("system_name", "SAP Commerce Cloud");
		transactionParameters.put("system_version", "2105-NN1.0.1");
		
		if ("novalnetCreditCard".equals(payment)) {

			JSONObject paymentObject = new JSONObject(requestObject.getJSONObject("paymentData").toString());
			paymentParameters.put("pan_hash", paymentObject.get("panHash").toString());
			paymentParameters.put("unique_id", paymentObject.get("uniqId").toString());
			NovalnetCreditCardPaymentModeModel novalnetPaymentMethod = (NovalnetCreditCardPaymentModeModel) paymentModeModel;
			if (novalnetPaymentMethod.getNovalnetTestMode()) {
                testMode = 1;
            }

            onholdOrderAmount = (novalnetPaymentMethod.getNovalnetOnholdAmount() == null) ? 0 : novalnetPaymentMethod.getNovalnetOnholdAmount();

            if (PAYMENT_AUTHORIZE.equals(novalnetPaymentMethod.getNovalnetOnholdAction().toString()) && orderAmountCent >= onholdOrderAmount) {
                 verify_payment_data = true;
            }


		} else if ("novalnetPayPal".equals(payment)) {

			NovalnetPayPalPaymentModeModel novalnetPaymentMethod = (NovalnetPayPalPaymentModeModel) paymentModeModel;
			if (novalnetPaymentMethod.getNovalnetTestMode()) {
                testMode = 1;
            }

            onholdOrderAmount = (novalnetPaymentMethod.getNovalnetOnholdAmount() == null) ? 0 : novalnetPaymentMethod.getNovalnetOnholdAmount();

            if (PAYMENT_AUTHORIZE.equals(novalnetPaymentMethod.getNovalnetOnholdAction().toString()) && orderAmountCent >= onholdOrderAmount) {
                 verify_payment_data = true;
            }

		} else if ("novalnetDirectDebitSepa".equals(payment)) {

			JSONObject paymentObject = new JSONObject(requestObject.getJSONObject("paymentData").toString());
            NovalnetDirectDebitSepaPaymentModeModel novalnetPaymentMethod = (NovalnetDirectDebitSepaPaymentModeModel) paymentModeModel;
			String accountHolder = billingObject.get("firstName").toString() + ' ' + billingObject.get("lastName").toString();
			paymentParameters.put("iban", paymentObject.get("iban").toString());
			paymentParameters.put("bank_account_holder", accountHolder.replace("&", ""));
			if (novalnetPaymentMethod.getNovalnetTestMode()) {
                testMode = 1;
            }

            onholdOrderAmount = (novalnetPaymentMethod.getNovalnetOnholdAmount() == null) ? 0 : novalnetPaymentMethod.getNovalnetOnholdAmount();

            if (PAYMENT_AUTHORIZE.equals(novalnetPaymentMethod.getNovalnetOnholdAction().toString()) && orderAmountCent >= onholdOrderAmount) {
                 verify_payment_data = true;
            }

        } 

        if(action.equals("get_redirect_url")) {
			transactionParameters.put("return_url", requestObject.get("returnUrl").toString());
			transactionParameters.put("error_return_url", requestObject.get("returnUrl").toString());
        }

        transactionParameters.put("test_mode", testMode);

		transactionParameters.put("payment_data", paymentParameters);
		dataParameters.put("merchant", merchantParameters);
		dataParameters.put("customer", customerParameters);
		dataParameters.put("transaction", transactionParameters);
		dataParameters.put("custom", customParameters);
		
		String jsonString = gson.toJson(dataParameters);

		String url = "https://payport.novalnet.de/v2/payment";
        if(verify_payment_data == true) {
			url = "https://payport.novalnet.de/v2/authorize";
		}

		responeParameters.put("jsonString", jsonString);
		responeParameters.put("paygateURL", url);

		return responeParameters;
    }


    public String getTransactionDetails(JSONObject requestObject, String languageCode) {

    	Gson gson = new GsonBuilder().create();

    	final Map<String, Object> transactionParameters = new HashMap<String, Object>();
		final Map<String, Object> customParameters 		= new HashMap<String, Object>();
		final Map<String, Object> dataParameters 		= new HashMap<String, Object>();

		transactionParameters.put("tid", requestObject.get("tid").toString());
		customParameters.put("lang", languageCode);
		dataParameters.put("transaction", transactionParameters);
		dataParameters.put("custom", customParameters);

		String jsonString = gson.toJson(dataParameters);
		String url = "https://payport.novalnet.de/v2/transaction/details";
		StringBuilder response = sendRequest(url, jsonString);
		return response.toString();
    }


    public OrderData createOrder(JSONObject transactionJsonObject, String payment, AddressModel billingAddress, String emailAddress, UserModel currentUser, Integer orderAmountCent, String currency, String languageCode)
    throws InvalidCartException, NoCheckoutCartException {

    	PaymentModeModel paymentModeModel = paymentModeService.getPaymentModeForCode(payment);

        if ("novalnetCreditCard".equals(payment)) {
            NovalnetCreditCardPaymentModeModel novalnetPaymentMethod = (NovalnetCreditCardPaymentModeModel) paymentModeModel;
            cartModel.setPaymentMode(novalnetPaymentMethod);
        } else if ("novalnetDirectDebitSepa".equals(payment)) {
            NovalnetDirectDebitSepaPaymentModeModel novalnetPaymentMethod = (NovalnetDirectDebitSepaPaymentModeModel) paymentModeModel;
            cartModel.setPaymentMode(novalnetPaymentMethod);
        } else if ("novalnetPayPal".equals(payment)) {
            NovalnetPayPalPaymentModeModel novalnetPaymentMethod = (NovalnetPayPalPaymentModeModel) paymentModeModel;
            cartModel.setPaymentMode(novalnetPaymentMethod);
        }
		
		NovalnetPaymentInfoModel paymentInfoModel = new NovalnetPaymentInfoModel();
		paymentInfoModel.setBillingAddress(billingAddress);
		paymentInfoModel.setPaymentEmailAddress(emailAddress);
		paymentInfoModel.setDuplicate(Boolean.FALSE);
		paymentInfoModel.setSaved(Boolean.TRUE);
		paymentInfoModel.setUser(currentUser);
		paymentInfoModel.setPaymentInfo("Novalnet Transaction ID : "+ transactionJsonObject.get("tid").toString());
		paymentInfoModel.setOrderHistoryNotes("Novalnet Transaction ID : "+ transactionJsonObject.get("tid").toString());
		paymentInfoModel.setPaymentProvider(payment);
		paymentInfoModel.setPaymentGatewayStatus(transactionJsonObject.get("status").toString());
		cartModel.setPaymentInfo(paymentInfoModel);
		paymentInfoModel.setCode("");
		
		PaymentTransactionEntryModel orderTransactionEntry = null;
		final List<PaymentTransactionEntryModel> paymentTransactionEntries = new ArrayList<>();
		orderTransactionEntry = novalnetOrderFacade.createTransactionEntry(transactionJsonObject.get("tid").toString(),
											cartModel, orderAmountCent, "Novalnet Transaction ID : " + transactionJsonObject.get("tid").toString(), currency);
		paymentTransactionEntries.add(orderTransactionEntry);

		// Initiate/ Update PaymentTransactionModel
		PaymentTransactionModel paymentTransactionModel = new PaymentTransactionModel();
		paymentTransactionModel.setPaymentProvider(payment);
		paymentTransactionModel.setRequestId(transactionJsonObject.get("tid").toString());
		paymentTransactionModel.setEntries(paymentTransactionEntries);
		paymentTransactionModel.setOrder(cartModel);
		paymentTransactionModel.setInfo(paymentInfoModel);

		cartModel.setPaymentTransactions(Arrays.asList(paymentTransactionModel));

		novalnetOrderFacade.getModelService().saveAll(cartModel, billingAddress);

		final OrderData orderData = novalnetOrderFacade.getCheckoutFacade().placeOrder();
		String orderNumber = orderData.getCode();
		List<OrderModel> orderInfoModel = getOrderInfoModel(orderNumber);
        OrderModel orderModel = novalnetOrderFacade.getModelService().get(orderInfoModel.get(0).getPk());

        paymentInfoModel.setCode(orderNumber);

        novalnetOrderFacade.getModelService().save(paymentInfoModel);

        if ("novalnetCreditCard".equals(payment)) {
            NovalnetCreditCardPaymentModeModel novalnetPaymentMethod = (NovalnetCreditCardPaymentModeModel) paymentModeModel;
            orderModel.setPaymentMode(novalnetPaymentMethod);
        } else if ("novalnetDirectDebitSepa".equals(payment)) {
            NovalnetDirectDebitSepaPaymentModeModel novalnetPaymentMethod = (NovalnetDirectDebitSepaPaymentModeModel) paymentModeModel;
            orderModel.setPaymentMode(novalnetPaymentMethod);
        } else if ("novalnetPayPal".equals(payment)) {
            NovalnetPayPalPaymentModeModel novalnetPaymentMethod = (NovalnetPayPalPaymentModeModel) paymentModeModel;
            orderModel.setPaymentMode(novalnetPaymentMethod);
        }

        orderModel.setStatusInfo("Novalnet Transaction ID : " + transactionJsonObject.get("tid").toString());
		
        OrderHistoryEntryModel orderEntry = novalnetOrderFacade.getModelService().create(OrderHistoryEntryModel.class);
		orderEntry.setTimestamp(new Date());
		orderEntry.setOrder(orderModel);
		orderEntry.setDescription("Tid : " + transactionJsonObject.get("tid").toString());
		orderModel.setPaymentInfo(paymentInfoModel);
        novalnetOrderFacade.getModelService().saveAll(orderModel, orderEntry);
		updateOrderStatus(orderNumber, paymentInfoModel);
		createTransactionUpdate(transactionJsonObject.get("tid").toString(), orderNumber, languageCode);
		
        long callbackInfoTid = Long.parseLong(transactionJsonObject.get("tid").toString());
        int orderPaidAmount = orderAmountCent;

		NovalnetCallbackInfoModel novalnetCallbackInfo = new NovalnetCallbackInfoModel();
        novalnetCallbackInfo.setPaymentType(payment);
        novalnetCallbackInfo.setOrderAmount(orderAmountCent);
        novalnetCallbackInfo.setCallbackTid(callbackInfoTid);
        novalnetCallbackInfo.setOrginalTid(callbackInfoTid);
        novalnetCallbackInfo.setPaidAmount(orderPaidAmount);
        novalnetCallbackInfo.setOrderNo(orderNumber);
        novalnetOrderFacade.getModelService().save(novalnetCallbackInfo);

        return orderData;

    }

    public void createTransactionUpdate(String tid, String orderNumber, String languageCode) {

    	Gson gson = new GsonBuilder().create();

    	final Map<String, Object> transactionParameters = new HashMap<String, Object>();
		final Map<String, Object> customParameters 		= new HashMap<String, Object>();
		final Map<String, Object> dataParameters 		= new HashMap<String, Object>();

		transactionParameters.put("tid", tid);
		transactionParameters.put("order_no", orderNumber);
		customParameters.put("lang", languageCode);
		dataParameters.put("transaction", transactionParameters);
		dataParameters.put("custom", customParameters);
		String jsonString = gson.toJson(dataParameters);
		String url = "https://payport.novalnet.de/v2/transaction/update";
		StringBuilder response = sendRequest(url, jsonString);
    }


	/**
     * Update order status
     *
     * @param orderCode Order code of the order
     * @param paymentInfoModel payment configurations
     */
    public void updateOrderStatus(String orderCode, NovalnetPaymentInfoModel paymentInfoModel) {
        List<OrderModel> orderInfoModel = getOrderInfoModel(orderCode);

        OrderModel orderModel = novalnetOrderFacade.getModelService().get(orderInfoModel.get(0).getPk());
        orderModel.setStatus(OrderStatus.COMPLETED);
	
		// Update the payment status for completed payments
		orderModel.setPaymentStatus(PaymentStatus.PAID);
        novalnetOrderFacade.getModelService().save(orderModel);

    }
	
	public StringBuilder sendRequest(String url, String jsonString) {
        StringBuilder response = new StringBuilder();
        try {
        	LOG.info("request sent to novalnet");
			LOG.info(jsonString);
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
            LOG.error("MalformedURLException ", ex);
        } catch (IOException ex) {
            LOG.error("IOException ", ex);
        }

        LOG.info("response recieved from novalnet");
		LOG.info(response.toString());

        return response;

    }
    
    public static String formatAmount(String amount) {
        if (amount.contains(",")) {
            try {
                NumberFormat formattedAmount = NumberFormat.getNumberInstance(Locale.GERMANY);
                double formattedValue = formattedAmount.parse(amount).doubleValue();
                amount = Double.toString(formattedValue);
            } catch (Exception e) {
                amount = amount.replace(",", ".");
            }
        }
        return amount;
    }
    
    @Secured({ "ROLE_CUSTOMERGROUP", "ROLE_CLIENT", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT" })
	@RequestMapping(value = "/users/{userId}/novalnet/payment", method = RequestMethod.POST)
	@RequestMappingOverride
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	@SiteChannelRestriction(allowedSiteChannelsProperty = API_COMPATIBILITY_B2C_CHANNELS)
	@ApiOperation(nickname = "placeOrder", value = "Place a order.", notes = "Authorizes the cart and places the order. The response contains the new order data.")
	@ApiBaseSiteIdAndUserIdParam
	public NnResponseWsDTO getRedirectURL(
			@ApiParam(value = "billing details and payment details", required = true) @RequestParam final String reqJsonString,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
			throws PaymentAuthorizationException, InvalidCartException, NoCheckoutCartException
	{

		JSONObject requestObject = new JSONObject(reqJsonString.toString());

		cartData  = novalnetOrderFacade.loadCart(requestObject.get("cartId").toString());
		cartModel = novalnetOrderFacade.getCart();
		baseStore = novalnetOrderFacade.getBaseStoreModel();

		String action = "get_redirect_url";
		final String emailAddress 	= JaloSession.getCurrentSession().getUser().getLogin();
		String responseString = "";

		final UserModel currentUser = novalnetOrderFacade.getCurrentUserForCheckout();
		String totalAmount 			= formatAmount(String.valueOf(cartData.getTotalPriceWithTax().getValue()));
		DecimalFormat decimalFormat = new DecimalFormat("##.##");
		String orderAmount 			= decimalFormat.format(Float.parseFloat(totalAmount));
		float floatAmount 			= Float.parseFloat(orderAmount);
		BigDecimal orderAmountCents = BigDecimal.valueOf(floatAmount).multiply(BigDecimal.valueOf(100));
		Integer orderAmountCent 	= orderAmountCents.intValue();
		final String currency 	  	= cartData.getTotalPriceWithTax().getCurrencyIso();
		final Locale language	  	= JaloSession.getCurrentSession().getSessionContext().getLocale();
		final String languageCode 	= language.toString().toUpperCase();

		password = baseStore.getNovalnetPaymentAccessKey().trim();
	
		
		Map<String, Object> requsetDeatils = formPaymentRequest(requestObject, action, emailAddress, orderAmountCent, currency, languageCode);
        StringBuilder response = sendRequest(requsetDeatils.get("paygateURL").toString(), requsetDeatils.get("jsonString").toString());
        JSONObject tomJsonObject = new JSONObject(response.toString());
        JSONObject resultJsonObject = tomJsonObject.getJSONObject("result");
        JSONObject transactionJsonObject = tomJsonObject.getJSONObject("transaction");
        
        if(!String.valueOf("100").equals(resultJsonObject.get("status_code").toString())) {
			final String statMessage = resultJsonObject.get("status_text").toString() != null ? resultJsonObject.get("status_text").toString() : resultJsonObject.get("status_desc").toString();
			throw new PaymentAuthorizationException();
		}

		String redirectURL = resultJsonObject.get("redirect_url").toString();
		NnResponseData responseData = new NnResponseData();
		responseData.setRedirectURL(redirectURL);
		return dataMapper.map(responseData, NnResponseWsDTO.class, fields);
	}
    
    @Secured({ "ROLE_CUSTOMERGROUP", "ROLE_CLIENT", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT" })
	@RequestMapping(value = "/users/{userId}/novalnet/payment/config", method = RequestMethod.GET)
	@RequestMappingOverride
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	@SiteChannelRestriction(allowedSiteChannelsProperty = API_COMPATIBILITY_B2C_CHANNELS)
	@ApiOperation(nickname = "paymentConfig", value = "return payment configuration", notes = "return payment configuration stored in Backend")
	@ApiBaseSiteIdAndUserIdParam
	public NnConfigWsDTO getPaymentConfig(
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
			throws PaymentAuthorizationException, InvalidCartException, NoCheckoutCartException
	{
		final BaseStoreModel baseStore = novalnetOrderFacade.getBaseStoreModel();
		PaymentModeModel directDebitSepaPaymentModeModel = paymentModeService.getPaymentModeForCode("novalnetDirectDebitSepa");
		NovalnetDirectDebitSepaPaymentModeModel novalnetDirectDebitSepaPaymentMethod = (NovalnetDirectDebitSepaPaymentModeModel) directDebitSepaPaymentModeModel;
		PaymentModeModel payPalPaymentModeModel = paymentModeService.getPaymentModeForCode("novalnetPayPal");
		NovalnetPayPalPaymentModeModel novalnetPayPalPaymentMethod = (NovalnetPayPalPaymentModeModel) payPalPaymentModeModel;
		PaymentModeModel creditCardPaymentModeModel = paymentModeService.getPaymentModeForCode("novalnetCreditCard");
		NovalnetCreditCardPaymentModeModel novalnetCreditCardPaymentMethod = (NovalnetCreditCardPaymentModeModel) creditCardPaymentModeModel;

        NnCreditCardData creditCardData = new NnCreditCardData();
        creditCardData.setActive(novalnetCreditCardPaymentMethod.getActive());

        NnDirectDebitSepaData directDebitSepaData = new NnDirectDebitSepaData();
        directDebitSepaData.setActive(novalnetDirectDebitSepaPaymentMethod.getActive());

        NnPayPalData payPalData = new NnPayPalData();
        payPalData.setActive(novalnetPayPalPaymentMethod.getActive());

        NnPaymentData paymentData = new NnPaymentData();
        paymentData.setNovalnetCreditCard(creditCardData);
        paymentData.setNovalnetDirectDebitSepa(directDebitSepaData);
        paymentData.setNovalnetPayPal(payPalData);

        NnConfigData configData = new NnConfigData();
        configData.setPaymentinfo(paymentData);
        configData.setNovalnetClienKey(baseStore.getNovalnetClientKey());

		return dataMapper.map(configData, NnConfigWsDTO.class, fields);
	}


	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_CLIENT", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT" })
	@RequestMapping(value = "/users/{userId}/novalnet/paymentDetails", method = RequestMethod.POST)
	@RequestMappingOverride
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	@SiteChannelRestriction(allowedSiteChannelsProperty = API_COMPATIBILITY_B2C_CHANNELS)
	@ApiOperation(nickname = "placeOrder", value = "Place a order.", notes = "Authorizes the cart and places the order. The response contains the new order data.")
	@ApiBaseSiteIdAndUserIdParam
	public NnPaymentDetailsWsDTO getPaymentDetails(
			@ApiParam(value = "order no", required = true) @RequestParam final String orderno,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
			throws PaymentAuthorizationException, InvalidCartException, NoCheckoutCartException
	{
		final List<NovalnetPaymentInfoModel> paymentInfo = novalnetOrderFacade.getNovalnetPaymentInfo(orderno);
        NovalnetPaymentInfoModel paymentInfoModel = novalnetOrderFacade.getPaymentModel(paymentInfo);
		NnPaymentDetailsData paymentDetailsData = new NnPaymentDetailsData();
		paymentDetailsData.setStatus(paymentInfoModel.getPaymentGatewayStatus());
		paymentDetailsData.setComments(paymentInfoModel.getOrderHistoryNotes());
		return dataMapper.map(paymentDetailsData, NnPaymentDetailsWsDTO.class, fields);
	}
	
	public static String getPaymentType(String paymentName) {
        final Map<String, String> paymentType = new HashMap<String, String>();
        paymentType.put("novalnetCreditCard", "CREDITCARD");
        paymentType.put("novalnetDirectDebitSepa", "DIRECT_DEBIT_SEPA");
        paymentType.put("novalnetGuaranteedDirectDebitSepa", "GUARANTEED_DIRECT_DEBIT_SEPA");
        paymentType.put("novalnetInvoice", "INVOICE");
        paymentType.put("novalnetGuaranteedInvoice", "GUARANTEED_INVOICE");
        paymentType.put("novalnetPrepayment", "PREPAYMENT");
        paymentType.put("novalnetBarzahlen", "CASHPAYMENT");
        paymentType.put("novalnetPayPal", "PAYPAL");
        paymentType.put("novalnetInstantBankTransfer", "ONLINE_TRANSFER");
        paymentType.put("novalnetBancontact", "BANCONTACT");
        paymentType.put("novalnetMultibanco", "MULTIBANCO");
        paymentType.put("novalnetIdeal", "IDEAL");
        paymentType.put("novalnetEps", "EPS");
        paymentType.put("novalnetGiropay", "GIROPAY");
        paymentType.put("novalnetPrzelewy24", "PRZELEWY24");
        paymentType.put("novalnetPostFinanceCard", "POSTFINANCE_CARD");
        paymentType.put("novalnetPostFinance", "POSTFINANCE");
        return paymentType.get(paymentName);
    }

    
}

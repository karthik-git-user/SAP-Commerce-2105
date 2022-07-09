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
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.math.BigDecimal;

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
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.search.SearchResult;
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
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Type;

import java.io.*;

import de.hybris.novalnet.core.model.NovalnetPaymentInfoModel;
import de.hybris.novalnet.core.model.NovalnetCreditCardPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetDirectDebitSepaPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetPayPalPaymentModeModel;
import de.hybris.platform.core.model.order.payment.PaymentModeModel;
import de.hybris.platform.payment.model.PaymentTransactionEntryModel;
import de.hybris.platform.orderhistory.model.OrderHistoryEntryModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.core.enums.OrderStatus;
import de.hybris.platform.core.enums.PaymentStatus;
import de.hybris.platform.payment.model.PaymentTransactionModel;
import de.hybris.platform.commercefacades.i18n.I18NFacade;
import de.hybris.platform.commerceservices.strategies.CheckoutCustomerStrategy;
import de.hybris.platform.servicelayer.session.SessionService;
import de.hybris.platform.order.CartService;
import de.hybris.platform.order.PaymentModeService;
import de.hybris.platform.commercefacades.order.OrderFacade;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.commercefacades.user.data.CountryData;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.core.model.c2l.CountryModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.order.CartFactory;
import de.hybris.platform.order.CartService;
import de.hybris.platform.order.CalculationService;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.servicelayer.i18n.CommonI18NService;
import de.hybris.platform.jalo.JaloSession;
import org.springframework.beans.factory.annotation.Required;
import de.novalnet.order.NovalnetOrderFacade;

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
	
	private BaseStoreService baseStoreService;
    private SessionService sessionService;
    private CartService cartService;
    private OrderFacade orderFacade;
    private CheckoutFacade checkoutFacade;
    private CheckoutCustomerStrategy checkoutCustomerStrategy;
    private ModelService modelService;
    private FlexibleSearchService flexibleSearchService;
    private Converter<AddressData, AddressModel> addressReverseConverter;
    private Converter<CountryModel, CountryData> countryConverter;
    private Converter<OrderModel, OrderData> orderConverter;
    private CartFactory cartFactory;
    private CalculationService calculationService;
    private Populator<AddressModel, AddressData> addressPopulator;
    private CommonI18NService commonI18NService;
    

	@Resource(name = "novalnetOrderFacade")
    NovalnetOrderFacade novalnetOrderFacade;
    
    @Resource(name = "dataMapper")
	private DataMapper dataMapper;
    
    @Resource
    private PaymentModeService paymentModeService;
	
	private static final String PAYMENT_MAPPING = "accountHolderName,cardNumber,cardType,cardTypeData(code),expiryMonth,expiryYear,issueNumber,startMonth,startYear,subscriptionId,defaultPaymentInfo,saved,billingAddress(titleCode,firstName,lastName,line1,line2,town,postalCode,country(isocode),region(isocode),defaultAddress)";
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
			@ApiParam(value = "credit card hash", required = false) @RequestParam final String reqJsonString,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
			throws PaymentAuthorizationException, InvalidCartException, NoCheckoutCartException
	{

		JSONObject requestObject = new JSONObject();
		JSONObject billingObject = new JSONObject();
		JSONObject paymentObject = new JSONObject();
		JSONObject countryObject = new JSONObject();
		JSONObject regionObject = new JSONObject();

		requestObject = new JSONObject(reqJsonString.toString());
		
		// LOG.info(requestObject.get("tid").toString());
		// LOG.info(billingObject.get("postalCode").toString());

		String action = requestObject.get("action").toString();
		
		JSONObject tomJsonObject = new JSONObject();
		JSONObject resultJsonObject = new JSONObject();
		JSONObject transactionJsonObject = new JSONObject();
		JSONObject customerJsonObject = new JSONObject();
		JSONObject billingJsonObject = new JSONObject();
		final CartData cartData = novalnetOrderFacade.loadCart(requestObject.get("cartId").toString());
		final CartModel cartModel = novalnetOrderFacade.getCart();
		final UserModel currentUser = novalnetOrderFacade.getCurrentUserForCheckout();
		final String currency = cartData.getTotalPriceWithTax().getCurrencyIso();
		String totalAmount = formatAmount(String.valueOf(cartData.getTotalPriceWithTax().getValue()));
		DecimalFormat decimalFormat = new DecimalFormat("##.##");
		String orderAmount = decimalFormat.format(Float.parseFloat(totalAmount));
		float floatAmount = Float.parseFloat(orderAmount);
		BigDecimal orderAmountCents = BigDecimal.valueOf(floatAmount).multiply(BigDecimal.valueOf(100));
		Integer orderAmountCent = orderAmountCents.intValue();
		final Locale language = JaloSession.getCurrentSession().getSessionContext().getLocale();
		final String languageCode = language.toString().toUpperCase();
		final String emailAddress = JaloSession.getCurrentSession().getUser().getLogin();
		String customerNo = JaloSession.getCurrentSession().getUser().getPK().toString();
		
		final BaseStoreModel baseStore = novalnetOrderFacade.getBaseStoreModel();
		Gson gson = new GsonBuilder().create();
		final Map<String, Object> transactionParameters = new HashMap<String, Object>();
		final Map<String, Object> merchantParameters = new HashMap<String, Object>();
		final Map<String, Object> customerParameters = new HashMap<String, Object>();
		final Map<String, Object> billingParameters = new HashMap<String, Object>();
		final Map<String, Object> shippingParameters = new HashMap<String, Object>();
		final Map<String, Object> customParameters = new HashMap<String, Object>();
		final Map<String, Object> paymentParameters = new HashMap<String, Object>();
		final Map<String, Object> dataParameters = new HashMap<String, Object>();

		String firstName = "";
		String lastName = "";
		String street1 = "";
		String street2 = "";
		String town = "";
		String zip = "";
		String countryCode = "";
		
		if("create_order".equals(action)) {
			billingObject = requestObject.getJSONObject("billingAddress");
			paymentObject = requestObject.getJSONObject("paymentData");
			countryObject = billingObject.getJSONObject("country");
			regionObject = billingObject.getJSONObject("region");
			merchantParameters.put("signature", baseStore.getNovalnetAPIKey());
			merchantParameters.put("tariff", baseStore.getNovalnetTariffId());

			firstName = billingObject.get("firstName").toString();
			lastName = billingObject.get("lastName").toString();
			street1 = billingObject.get("line1").toString();
			street2 = billingObject.get("line2").toString();
			town = billingObject.get("town").toString();
			zip = billingObject.get("postalCode").toString();
			countryCode = countryObject.get("isocode").toString();

			customerParameters.put("first_name", firstName);
			customerParameters.put("last_name", lastName);
			customerParameters.put("email", emailAddress);
			customerParameters.put("customer_no", customerNo);
			customerParameters.put("gender", "u");

			billingParameters.put("street", street1 + " " + street2);
			billingParameters.put("city", town);
			billingParameters.put("zip", zip);
			billingParameters.put("country_code", countryCode);

			// AddressData deliveryAddress = cartData.getDeliveryAddress();
			// AddressData deliveryAddress1 = novalnetOrderFacade.getCheckoutFacade().getCheckoutCart().getDeliveryAddress();
			// LOG.info(deliveryAddress.getLine1());
			// LOG.info(deliveryAddress1.getLine1());

			// if(deliveryAddress.getLine1().equals(addressData.getLine1()) && deliveryAddress.getLine2().equals(addressData.getLine2()) && deliveryAddress.getTown().equals(addressData.getTown()) &&  deliveryAddress.getPostalCode().equals(addressData.getPostalCode()) && deliveryAddress.getCountry().getIsocode().equals(addressData.getCountry().getIsocode())) {
			    shippingParameters.put("same_as_billing", 1);
		    // } else {
		    //     shippingParameters.put("street", deliveryAddress.getLine1() + " " + deliveryAddress.getLine2());
		    //     shippingParameters.put("city", deliveryAddress.getTown());
		    //     shippingParameters.put("zip", deliveryAddress.getPostalCode());
		    //     shippingParameters.put("country_code", deliveryAddress.getCountry().getIsocode());
		    //     shippingParameters.put("first_name", deliveryAddress.getFirstName());
		    //     shippingParameters.put("last_name", deliveryAddress.getLastName());
		    // }
			
			customerParameters.put("billing", billingParameters);
			customerParameters.put("shipping", shippingParameters);

			transactionParameters.put("payment_type", requestObject.get("paymentType").toString());
			transactionParameters.put("currency", currency);
			transactionParameters.put("amount", 0);
			transactionParameters.put("create_token", 1);
			transactionParameters.put("system_name", "SAP Commerce Cloud");
			transactionParameters.put("system_version", "2105-NN1.0.1");
			customParameters.put("lang", languageCode);


			if ("novalnetDirectDebitSepa".equals(requestObject.get("paymentType").toString())) { 
				transactionParameters.put("currency", "EUR");
				String accountHolder = billingObject.get("firstName").toString() + ' ' + billingObject.get("lastName").toString();
				paymentParameters.put("iban", paymentObject.get("iban").toString());
				paymentParameters.put("bank_account_holder", accountHolder.replace("&", ""));
			} else if ("novalnetCreditCard".equals(requestObject.get("paymentType").toString())) {
				paymentParameters.put("pan_hash", paymentObject.get("panHash").toString());
				paymentParameters.put("unique_id", paymentObject.get("uniqId").toString());
			}

			transactionParameters.put("payment_data", paymentParameters);
			dataParameters.put("merchant", merchantParameters);
			dataParameters.put("customer", customerParameters);
			dataParameters.put("transaction", transactionParameters);
			dataParameters.put("custom", customParameters);

			
			String jsonString = gson.toJson(dataParameters);
LOG.info("request+");
LOG.info(jsonString);
			String password = baseStore.getNovalnetPaymentAccessKey().toString();
			String url = "https://payport.novalnet.de/v2/payment";
			StringBuilder response = sendRequest(url, jsonString);
			tomJsonObject = new JSONObject(response.toString());
					LOG.info("response+");
LOG.info(response.toString());
			resultJsonObject = tomJsonObject.getJSONObject("result");
			transactionJsonObject = tomJsonObject.getJSONObject("transaction");
		} else {
			transactionParameters.put("tid", requestObject.get("tid").toString());
			customParameters.put("lang", languageCode);

			dataParameters.put("transaction", transactionParameters);
			dataParameters.put("custom", customParameters);

			
			String jsonString = gson.toJson(dataParameters);

			LOG.info("request1111+");
LOG.info(jsonString);

			String url = "https://payport.novalnet.de/v2/transaction/details";
			StringBuilder response = sendRequest(url, jsonString);

			tomJsonObject = new JSONObject(response.toString());
			resultJsonObject = tomJsonObject.getJSONObject("result");
			//~ JSONObject customerJsonObject = tomJsonObject.getJSONObject("customer");
			transactionJsonObject = tomJsonObject.getJSONObject("transaction");
			customerJsonObject = tomJsonObject.getJSONObject("customer");
			billingJsonObject = tomJsonObject.getJSONObject("billing");
		LOG.info("response+");
LOG.info(response.toString());

			firstName = customerJsonObject.get("first_name").toString();
			lastName = customerJsonObject.get("last_name").toString();
			street1 = billingJsonObject.get("street").toString();
			town = billingJsonObject.get("town").toString();
			zip = billingJsonObject.get("postalCode").toString();
			countryCode = billingJsonObject.get("isocode").toString();
			
		}


        
        if(!String.valueOf("100").equals(resultJsonObject.get("status_code").toString())) {
			final String statMessage = resultJsonObject.get("status_text").toString() != null ? resultJsonObject.get("status_text").toString() : resultJsonObject.get("status_desc").toString();
			throw new PaymentAuthorizationException();
		}


        
        
        final AddressModel billingAddress = novalnetOrderFacade.getModelService().create(AddressModel.class);
		
		billingAddress.setFirstname(firstName);
		billingAddress.setLastname(lastName);
		billingAddress.setLine1(street1);
		billingAddress.setLine2(street2);
		billingAddress.setTown(town);
		billingAddress.setPostalcode(zip);
		billingAddress.setCountry(novalnetOrderFacade.getCommonI18NService().getCountry(countryCode));
		// billingAddress.setRegion(getCommonI18NService().getRegion(countryObject.get("isocode"), regionObject.get("isocode")));
		billingAddress.setEmail(emailAddress);

        // AddressModel billingAddress = novalnetOrderFacade.createBillingAddress(addressId);
		//~ billingAddress = addressReverseConverter.convert(addressData, billingAddress);
		billingAddress.setOwner(cartModel);

		String payment = (transactionJsonObject.get("payment_type").toString()).equals("CREDITCARD") ? "novalnetCreditCard" : ((transactionJsonObject.get("payment_type").toString()).equals("DIRECT_DEBIT_SEPA") ? "novalnetDirectDebitSepa" :((transactionJsonObject.get("payment_type").toString()).equals("PAYPAL") ? "novalnetPayPal": ""));
		
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
											cartModel, orderAmountCent, "tid: " + transactionJsonObject.get("tid").toString(), "EUR");
		paymentTransactionEntries.add(orderTransactionEntry);

		// Initiate/ Update PaymentTransactionModel
		PaymentTransactionModel paymentTransactionModel = new PaymentTransactionModel();
		paymentTransactionModel.setPaymentProvider(payment);
		paymentTransactionModel.setRequestId(transactionJsonObject.get("tid").toString());
		paymentTransactionModel.setEntries(paymentTransactionEntries);
		paymentTransactionModel.setOrder(cartModel);
		paymentTransactionModel.setInfo(paymentInfoModel);

		// Update the OrderModel
		cartModel.setPaymentTransactions(Arrays.asList(paymentTransactionModel));
		PaymentModeModel paymentModeModel = paymentModeService.getPaymentModeForCode("novalnetCreditCard");
		NovalnetCreditCardPaymentModeModel novalnetPaymentMethod = (NovalnetCreditCardPaymentModeModel) paymentModeModel;
        cartModel.setPaymentMode(novalnetPaymentMethod);
        novalnetOrderFacade.getModelService().saveAll(cartModel, billingAddress);
		final OrderData orderData = novalnetOrderFacade.getCheckoutFacade().placeOrder();
		String orderNumber = orderData.getCode();
		List<OrderModel> orderInfoModel = getOrderInfoModel(orderNumber);
        OrderModel orderModel = novalnetOrderFacade.getModelService().get(orderInfoModel.get(0).getPk());
		//~ return getDataMapper().map(orderData, OrderWsDTO.class, fields);
		paymentInfoModel.setCode(orderNumber);
        novalnetOrderFacade.getModelService().saveAll(paymentInfoModel);        
        OrderHistoryEntryModel orderEntry = novalnetOrderFacade.getModelService().create(OrderHistoryEntryModel.class);
		orderEntry.setTimestamp(new Date());
		orderEntry.setOrder(orderModel);
		orderEntry.setDescription("Tid : " + transactionJsonObject.get("tid").toString());
		orderModel.setPaymentInfo(paymentInfoModel);
        novalnetOrderFacade.getModelService().saveAll(orderModel, orderEntry);
		updateOrderStatus(orderNumber, paymentInfoModel);
		
		transactionParameters.clear();
        dataParameters.clear();
        customParameters.clear();
        
        transactionParameters.put("tid", transactionJsonObject.get("tid"));
		transactionParameters.put("order_no", orderData.getCode());
		customParameters.put("lang", languageCode);
		dataParameters.put("transaction", transactionParameters);
		dataParameters.put("custom", customParameters);

		String jsonString = gson.toJson(dataParameters);
		String url = "https://payport.novalnet.de/v2/transaction/update";
		StringBuilder responseString = sendRequest(url, jsonString);
        syncmirakl(tomJsonObject, orderData.getCode());
		return dataMapper.map(orderData, OrderWsDTO.class, fields);
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
        String password = "a87ff679a2f3e71d9181a67b7542122c";
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
            LOG.error("MalformedURLException ", ex);
        } catch (IOException ex) {
            LOG.error("IOException ", ex);
        }

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
	public String getRedirectURL(
			@ApiParam(value = "credit card hash", required = true) @RequestParam final String reqJsonString,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
			throws PaymentAuthorizationException, InvalidCartException, NoCheckoutCartException
	{
		JSONObject requestObject = new JSONObject();
		JSONObject billingObject = new JSONObject();
		JSONObject paymentObject = new JSONObject();
		JSONObject countryObject = new JSONObject();
		JSONObject regionObject = new JSONObject();

		requestObject = new JSONObject(reqJsonString.toString());

		// final AddressData addressData = novalnetOrderFacade.getAddressData(addressId);
		Integer testMode = 0;
		PaymentModeModel paymentModeModel = paymentModeService.getPaymentModeForCode(requestObject.get("paymentType").toString());
		
		final Locale language = JaloSession.getCurrentSession().getSessionContext().getLocale();
    	final String emailAddress = JaloSession.getCurrentSession().getUser().getLogin();
        final String languageCode = language.toString().toUpperCase();
		final CartData cartData = novalnetOrderFacade.loadCart(requestObject.get("cartId").toString());
		String totalAmount = formatAmount(String.valueOf(cartData.getTotalPriceWithTax().getValue()));
		DecimalFormat decimalFormat = new DecimalFormat("##.##");
		String orderAmount = decimalFormat.format(Float.parseFloat(totalAmount));
		float floatAmount = Float.parseFloat(orderAmount);
        BigDecimal orderAmountCents = BigDecimal.valueOf(floatAmount).multiply(BigDecimal.valueOf(100));
        Integer orderAmountCent = orderAmountCents.intValue();
		String customerNo = JaloSession.getCurrentSession().getUser().getPK().toString();

		final BaseStoreModel baseStore = novalnetOrderFacade.getBaseStoreModel();
		final CartModel cartModel = novalnetOrderFacade.getCart();
		final UserModel currentUser = novalnetOrderFacade.getCurrentUserForCheckout();
		final String currency = cartData.getTotalPriceWithTax().getCurrencyIso();
		
		final Map<String, Object> transactionParameters = new HashMap<String, Object>();
        final Map<String, Object> merchantParameters = new HashMap<String, Object>();
        final Map<String, Object> customerParameters = new HashMap<String, Object>();
        final Map<String, Object> billingParameters = new HashMap<String, Object>();
        final Map<String, Object> shippingParameters = new HashMap<String, Object>();
        final Map<String, Object> customParameters = new HashMap<String, Object>();
        final Map<String, Object> paymentParameters = new HashMap<String, Object>();
        final Map<String, Object> dataParameters = new HashMap<String, Object>();

        billingObject = requestObject.getJSONObject("billingAddress");
		paymentObject = requestObject.getJSONObject("paymentData");
		countryObject = billingObject.getJSONObject("country");
		regionObject = billingObject.getJSONObject("region");
		merchantParameters.put("signature", baseStore.getNovalnetAPIKey());
		merchantParameters.put("tariff", baseStore.getNovalnetTariffId());

		customerParameters.put("first_name", billingObject.get("firstName").toString());
		customerParameters.put("last_name", billingObject.get("lastName").toString());
		customerParameters.put("email", emailAddress);
		customerParameters.put("customer_no", customerNo);
		customerParameters.put("gender", "u");

		billingParameters.put("street", billingObject.get("line1").toString() + " " + billingObject.get("line2").toString());
		billingParameters.put("city", billingObject.get("town").toString());
		billingParameters.put("zip",billingObject.get("postalCode").toString());
		billingParameters.put("country_code", countryObject.get("isocode").toString());
        
        shippingParameters.put("same_as_billing", "1");

        transactionParameters.put("payment_type", getPaymentType(requestObject.get("paymentType").toString()));
        transactionParameters.put("currency", currency);
        transactionParameters.put("amount", 0);
        transactionParameters.put("system_name", "SAP Commerce Cloud");
        transactionParameters.put("create_token", 1);
        transactionParameters.put("system_version", "2105-NN1.0.1");
        customParameters.put("lang", languageCode);

        if ("novalnetCreditCard".equals(requestObject.get("paymentType").toString())) {
			paymentParameters.put("pan_hash", paymentObject.get("panHash").toString());
			paymentParameters.put("unique_id", paymentObject.get("uniqId").toString());
			transactionParameters.put("payment_data", paymentParameters);
			NovalnetCreditCardPaymentModeModel novalnetPaymentMethod = (NovalnetCreditCardPaymentModeModel) paymentModeModel;
			if (novalnetPaymentMethod.getNovalnetTestMode()) {
                testMode = 1;
            }
		}
		
		if ("novalnetPayPal".equals(requestObject.get("paymentType").toString())) {
			NovalnetPayPalPaymentModeModel novalnetPaymentMethod = (NovalnetPayPalPaymentModeModel) paymentModeModel;
			if (novalnetPaymentMethod.getNovalnetTestMode()) {
                testMode = 1;
            }
		}
		
		transactionParameters.put("test_mode", testMode);
		transactionParameters.put("return_url", requestObject.get("returnUrl").toString());
		transactionParameters.put("error_return_url", requestObject.get("returnUrl").toString());

        dataParameters.put("merchant", merchantParameters);
        dataParameters.put("customer", customerParameters);
        dataParameters.put("transaction", transactionParameters);
        dataParameters.put("custom", customParameters);
        

        Gson gson = new GsonBuilder().create();
        String jsonString = gson.toJson(dataParameters);

        String password = baseStore.getNovalnetPaymentAccessKey().toString();
        String url = "https://payport.novalnet.de/v2/payment";
        StringBuilder response = sendRequest(url, jsonString);
        JSONObject tomJsonObject = new JSONObject(response.toString());
        JSONObject resultJsonObject = tomJsonObject.getJSONObject("result");
        JSONObject transactionJsonObject = tomJsonObject.getJSONObject("transaction");
        
        if(!String.valueOf("100").equals(resultJsonObject.get("status_code").toString())) {
			final String statMessage = resultJsonObject.get("status_text").toString() != null ? resultJsonObject.get("status_text").toString() : resultJsonObject.get("status_desc").toString();
			throw new PaymentAuthorizationException();
		}

		final Map<String, Object> responseParameters = new HashMap<String, Object>();
		String redirectURL = resultJsonObject.get("redirect_url").toString();
		responseParameters.put("redirect_url", redirectURL);
		jsonString = gson.toJson(responseParameters);
		return jsonString;
	}
	
	
	
	/**
     * Sync data to mirakl
     *
     * @param novalnetJsonObject Order code of the order
     */
    public void syncmirakl(JSONObject tomJsonObject, String orderCode) {
        JSONObject resultJsonObject = tomJsonObject.getJSONObject("result");
		JSONObject customerJsonObject = tomJsonObject.getJSONObject("customer");
		JSONObject transactionJsonObject = tomJsonObject.getJSONObject("transaction");
		JSONObject billingJsonObject = customerJsonObject.getJSONObject("billing");
		JSONObject shippingJsonObject = customerJsonObject.getJSONObject("billing");
		if(customerJsonObject.has("shipping")) {
			shippingJsonObject = customerJsonObject.getJSONObject("shipping");
			if(shippingJsonObject.has("same_as_billing") && shippingJsonObject.get("same_as_billing").toString().equals("1") ){
				shippingJsonObject = customerJsonObject.getJSONObject("billing");
			}
		}
		
        final Map<String, Object> customerParameters = new HashMap<String, Object>();
        final Map<String, Object> billingParameters = new HashMap<String, Object>();
        final Map<String, Object> shippingParameters = new HashMap<String, Object>();
        final Map<String, Object> paymentinfoParameters= new HashMap<String, Object>();
        final Map<String, Object> dataParameters = new HashMap<String, Object>();
        
        dataParameters.put("commercial_id", orderCode);
        dataParameters.put("scored", true);
        dataParameters.put("shipping_zone_code", "testshippingzone");

        customerParameters.put("civility", "Dr");
        customerParameters.put("firstname", customerJsonObject.get("first_name"));
        customerParameters.put("lastname", customerJsonObject.get("last_name"));
        customerParameters.put("email", customerJsonObject.get("email"));
        customerParameters.put("customer_id", customerJsonObject.get("customer_no"));
        
        billingParameters.put("civility", "Dr");
        billingParameters.put("firstname", customerJsonObject.get("first_name"));
        billingParameters.put("lastname", customerJsonObject.get("last_name"));
        billingParameters.put("street_1", billingJsonObject.get("street"));
        billingParameters.put("city", billingJsonObject.get("city"));
        billingParameters.put("zip_code", billingJsonObject.get("zip"));
        billingParameters.put("country_iso_code", billingJsonObject.get("country_code"));
        billingParameters.put("country", "Germany");
        billingParameters.put("company", "Novalnet");
        billingParameters.put("state", "IDF");
        billingParameters.put("phone", "0619874662");
        billingParameters.put("phone_secondary", "0123456789");
        billingParameters.put("street_2", "Escalier A");
        
        shippingParameters.put("civility", "Dr");
        shippingParameters.put("firstname", customerJsonObject.get("first_name"));
        shippingParameters.put("lastname", customerJsonObject.get("last_name"));
        shippingParameters.put("street_1", shippingJsonObject.get("street"));
        shippingParameters.put("city", shippingJsonObject.get("city"));
        shippingParameters.put("zip_code", shippingJsonObject.get("zip"));
        shippingParameters.put("country_iso_code", shippingJsonObject.get("country_code"));
        shippingParameters.put("country", "Germany");
        shippingParameters.put("company", "Novalnet");
        shippingParameters.put("state", "IDF");
        shippingParameters.put("phone", "0619874662");
        shippingParameters.put("phone_secondary", "0123456789");
        shippingParameters.put("street_2", "Escalier A");
        
        paymentinfoParameters.put("payment_type", "NOVALNET_"+transactionJsonObject.get("payment_type"));
        paymentinfoParameters.put("imprint_number", transactionJsonObject.get("tid"));
        
        customerParameters.put("billing_address", billingParameters);
        customerParameters.put("shipping_address", shippingParameters);
        dataParameters.put("customer", customerParameters);
        dataParameters.put("payment_info", paymentinfoParameters);
        
        Gson gson = new GsonBuilder().create();
        String jsonString = gson.toJson(dataParameters);
        String url = "https://xtcommerce6.novalnet.de/mirakl_api_handler.php";        
        miraklSendRequest(url, jsonString);

    }
    
    
     public void miraklSendRequest(String url, String jsonString) {
        final BaseStoreModel baseStore = novalnetOrderFacade.getBaseStoreModel();
        String password = baseStore.getNovalnetPaymentAccessKey().trim();
        StringBuilder response = new StringBuilder();

        try {
            String urly = url;
            URL obj = new URL(urly);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            LOG.info("teststring");
            LOG.info(jsonString);
            byte[] postData = jsonString.getBytes(StandardCharsets.UTF_8);
            LOG.info(postData);
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Charset", "utf-8");
            con.setRequestProperty("Accept", "application/json");
            con.setRequestProperty("Authorization", "665177a1-78b9-44c9-8a93-a2c8dc11680c");

            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.write(postData);
            wr.flush();
            wr.close();
            int responseCode = con.getResponseCode();
            LOG.info("+++response1+++");
            LOG.info("+++response code+++"+responseCode);
            LOG.info("+++response+++");
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
    }
    
    
    @Secured({ "ROLE_CUSTOMERGROUP", "ROLE_CLIENT", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT" })
	@RequestMapping(value = "/users/{userId}/novalnet/payment/config", method = RequestMethod.POST)
	@RequestMappingOverride
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	@SiteChannelRestriction(allowedSiteChannelsProperty = API_COMPATIBILITY_B2C_CHANNELS)
	@ApiOperation(nickname = "placeOrder", value = "Place a order.", notes = "Authorizes the cart and places the order. The response contains the new order data.")
	@ApiBaseSiteIdAndUserIdParam
	public String getPaymentConfig(
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
		
		final Map<String, Object> paymentinfoParameters= new HashMap<String, Object>();
		final Map<String, Object> sepaPaymentinfoParameters= new HashMap<String, Object>();
		final Map<String, Object> paypalPaymentinfoParameters= new HashMap<String, Object>();
		final Map<String, Object> creditcardPaymentinfoParameters= new HashMap<String, Object>();
        final Map<String, Object> dataParameters = new HashMap<String, Object>();
        
        creditcardPaymentinfoParameters.put("active", novalnetCreditCardPaymentMethod.getActive());
        sepaPaymentinfoParameters.put("active", novalnetDirectDebitSepaPaymentMethod.getActive());
        paypalPaymentinfoParameters.put("active", novalnetPayPalPaymentMethod.getActive());
        paymentinfoParameters.put("novalnetCreditCard", creditcardPaymentinfoParameters);
        paymentinfoParameters.put("novalnetDirectDebitSepa", sepaPaymentinfoParameters);
        paymentinfoParameters.put("novalnetPayPal", paypalPaymentinfoParameters);
        
        dataParameters.put("novalnetActivationKey", baseStore.getNovalnetAPIKey());
        dataParameters.put("novalnetAccessKey", baseStore.getNovalnetPaymentAccessKey());
        dataParameters.put("novalnetClienKey", baseStore.getNovalnetClientKey());
        dataParameters.put("novalnetTariff", baseStore.getNovalnetTariffId());
        
        dataParameters.put("paymentinfo", paymentinfoParameters);
        
        Gson gson = new GsonBuilder().create();
        String jsonString = gson.toJson(dataParameters);
        return jsonString.toString();
	}


	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_CLIENT", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT" })
	@RequestMapping(value = "/users/{userId}/novalnet/paymentDetails", method = RequestMethod.POST)
	@RequestMappingOverride
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	@SiteChannelRestriction(allowedSiteChannelsProperty = API_COMPATIBILITY_B2C_CHANNELS)
	@ApiOperation(nickname = "placeOrder", value = "Place a order.", notes = "Authorizes the cart and places the order. The response contains the new order data.")
	@ApiBaseSiteIdAndUserIdParam
	public String getPaymentDetails(
			@ApiParam(value = "credit card hash", required = true) @RequestParam final String orderno,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
			throws PaymentAuthorizationException, InvalidCartException, NoCheckoutCartException
	{
		final List<NovalnetPaymentInfoModel> paymentInfo = novalnetOrderFacade.getNovalnetPaymentInfo(orderno);
        NovalnetPaymentInfoModel paymentInfoModel = novalnetOrderFacade.getPaymentModel(paymentInfo);
        final Map<String, Object> responseParameters = new HashMap<String, Object>();
		responseParameters.put("tid", paymentInfoModel.getOrderHistoryNotes());
		responseParameters.put("status", paymentInfoModel.getPaymentGatewayStatus());
		Gson gson = new GsonBuilder().create();
		String jsonString = gson.toJson(responseParameters);
		return jsonString;
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

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
	//~ @Resource(name = "novalnetOccFacade")
    //~ NovalnetOccFacade novalnetOccFacade;
    //~ @Resource(name = "paymentProviderRequestSupportedStrategy")
	//~ private PaymentProviderRequestSupportedStrategy paymentProviderRequestSupportedStrategy;
	
	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_CLIENT", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT" })
	@RequestMapping(value = "/users/{userId}/orders", method = RequestMethod.POST)
	@RequestMappingOverride
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	@SiteChannelRestriction(allowedSiteChannelsProperty = API_COMPATIBILITY_B2C_CHANNELS)
	@ApiOperation(nickname = "placeOrder", value = "Place a order.", notes = "Authorizes the cart and places the order. The response contains the new order data.")
	@ApiBaseSiteIdAndUserIdParam
	public OrderWsDTO placeOrder(
			@ApiParam(value = "Cart code for logged in user, cart GUID for guest checkout", required = true) @RequestParam final String cartId,
			@ApiParam(value = "credit card hash", required = false) @RequestParam final String panHash,
			@ApiParam(value = "credit card hash", required = false) @RequestParam final String uniqId,
			@ApiParam(value = "credit card hash", required = false) @RequestParam final String addressId,
			@ApiParam(value = "credit card hash", required = false) @RequestParam final String tid,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
			throws PaymentAuthorizationException, InvalidCartException, NoCheckoutCartException
	{
		
			JSONObject tomJsonObject = new JSONObject();
			JSONObject resultJsonObject = new JSONObject();
			JSONObject transactionJsonObject = new JSONObject();
			final CartData cartData = novalnetOrderFacade.loadCart(cartId);
			final CartModel cartModel = novalnetOrderFacade.getCart();
			final UserModel currentUser = novalnetOrderFacade.getCurrentUserForCheckout();
			final String currency = cartData.getTotalPriceWithTax().getCurrencyIso();
			String totalAmount = formatAmount(String.valueOf(cartData.getTotalPriceWithTax().getValue()));
			DecimalFormat decimalFormat = new DecimalFormat("##.##");
			String orderAmount = decimalFormat.format(Float.parseFloat(totalAmount));
			float floatAmount = Float.parseFloat(orderAmount);
			BigDecimal orderAmountCents = BigDecimal.valueOf(floatAmount).multiply(BigDecimal.valueOf(100));
			Integer orderAmountCent = orderAmountCents.intValue();
			LOG.info(totalAmount);
			LOG.info("+++++++++++++++++++205");
			LOG.info("+++++++++++++++++++205");
			final Locale language = JaloSession.getCurrentSession().getSessionContext().getLocale();
			final String languageCode = language.toString().toUpperCase();
			final String emailAddress = JaloSession.getCurrentSession().getUser().getLogin();
			
			final BaseStoreModel baseStore = novalnetOrderFacade.getBaseStoreModel();
			LOG.info(baseStore.getNovalnetPaymentAccessKey());
			LOG.info("+++++++++++++++++++206");
			Gson gson = new GsonBuilder().create();
			final Map<String, Object> transactionParameters = new HashMap<String, Object>();
			final Map<String, Object> merchantParameters = new HashMap<String, Object>();
			final Map<String, Object> customerParameters = new HashMap<String, Object>();
			final Map<String, Object> billingParameters = new HashMap<String, Object>();
			final Map<String, Object> shippingParameters = new HashMap<String, Object>();
			final Map<String, Object> customParameters = new HashMap<String, Object>();
			final Map<String, Object> paymentParameters = new HashMap<String, Object>();
			final Map<String, Object> dataParameters = new HashMap<String, Object>();
		
		if("".equals(tid) || tid == null) {
			final AddressData addressData = novalnetOrderFacade.getAddressData(addressId);
			LOG.info("+++++++++++++++++++210");
			LOG.info(addressData.getFirstName());
			
			
			LOG.info("+++++++++++++++++++210");
			LOG.info(emailAddress);
			LOG.info("+++++++++++++++++++210");
			LOG.info(languageCode);

			LOG.info("placeOrder");
			LOG.info("+++++++++++++++++++335");
			LOG.info("+++++++++++++++++++335");
			LOG.info(panHash);
			LOG.info("+++++++++++++++++++335");
			LOG.info("+++++++++++++++++++349");

			merchantParameters.put("signature", baseStore.getNovalnetAPIKey());
			merchantParameters.put("tariff", baseStore.getNovalnetTariffId());

			customerParameters.put("first_name", addressData.getFirstName());
			customerParameters.put("last_name", addressData.getLastName());
			customerParameters.put("email", emailAddress);
			customerParameters.put("customer_no", "2");
			customerParameters.put("gender", "u");

			billingParameters.put("street", addressData.getLine1() +" "+ addressData.getLine2());
			billingParameters.put("city", addressData.getTown());
			billingParameters.put("zip",addressData.getPostalCode());
			billingParameters.put("country_code", addressData.getCountry().getIsocode());
			
			shippingParameters.put("same_as_billing", "1");

			customerParameters.put("billing", billingParameters);
			customerParameters.put("shipping", shippingParameters);

			transactionParameters.put("payment_type", "CREDITCARD");
			transactionParameters.put("currency", currency);
			transactionParameters.put("amount", orderAmountCent);
			transactionParameters.put("system_name", "SAP Commerce Cloud");
			transactionParameters.put("system_version", "2105-NN1.0.1");
			customParameters.put("lang", languageCode);


			if ("novalnetDirectDebitSepa".equals(currentPayment)) {            
				String accountHolder = addressData.getFirstName() + ' ' + addressData.getLastName();
				paymentParameters.put("iban", panHash);
				paymentParameters.put("bank_account_holder", accountHolder.replace("&", ""));
			} else if ("novalnetCreditCard".equals(currentPayment)) {
				paymentParameters.put("pan_hash", panHash);
				paymentParameters.put("unique_id", uniqId);
			}

			transactionParameters.put("payment_data", paymentParameters);
			dataParameters.put("merchant", merchantParameters);
			dataParameters.put("customer", customerParameters);
			dataParameters.put("transaction", transactionParameters);
			dataParameters.put("custom", customParameters);

			
			String jsonString = gson.toJson(dataParameters);

			String password = baseStore.getNovalnetPaymentAccessKey().toString();
			String url = "https://payport.novalnet.de/v2/payment";
			StringBuilder response = sendRequest(url, jsonString);
			tomJsonObject = new JSONObject(response.toString());
			 resultJsonObject = tomJsonObject.getJSONObject("result");
			 transactionJsonObject = tomJsonObject.getJSONObject("transaction");
			LOG.info(response.toString());
		} else {
			LOG.info("+++++++++++++++++++++++++++++++320");
			transactionParameters.put("tid", tid);
			customParameters.put("lang", languageCode);

			dataParameters.put("transaction", transactionParameters);
			dataParameters.put("custom", customParameters);

			
			String jsonString = gson.toJson(dataParameters);

			String url = "https://payport.novalnet.de/v2/transaction/details";
			StringBuilder response = sendRequest(url, jsonString);

			 tomJsonObject = new JSONObject(response.toString());
			 resultJsonObject = tomJsonObject.getJSONObject("result");
			//~ JSONObject customerJsonObject = tomJsonObject.getJSONObject("customer");
			 transactionJsonObject = tomJsonObject.getJSONObject("transaction");
			
		}
        
        if(!String.valueOf("100").equals(resultJsonObject.get("status_code").toString())) {
			final String statMessage = resultJsonObject.get("status_text").toString() != null ? resultJsonObject.get("status_text").toString() : resultJsonObject.get("status_desc").toString();
			LOG.info(statMessage);
			LOG.info("+++++++++++++++++++306");
			throw new PaymentAuthorizationException();
		}
        
        
        
        AddressModel billingAddress = novalnetOrderFacade.createBillingAddress(addressId);
		//~ billingAddress = addressReverseConverter.convert(addressData, billingAddress);
		billingAddress.setEmail("karthik_m@novalnetsolutions,com");
		billingAddress.setOwner(cartModel);

		String payment = (transactionJsonObject.get("tid").toString()).equals("CREDITCARD") ? "novalnetCreditCard" : ((transactionJsonObject.get("tid").toString()).equals("DIRECT_DEBIT_SEPA") ? "novalnetDirectDebitSepa" :((transactionJsonObject.get("tid").toString()).equals("PAYPAL") ? "novalnetPayPal": ""));
		
		NovalnetPaymentInfoModel paymentInfoModel = new NovalnetPaymentInfoModel();
		paymentInfoModel.setBillingAddress(billingAddress);
		paymentInfoModel.setPaymentEmailAddress(emailAddress);
		paymentInfoModel.setDuplicate(Boolean.FALSE);
		paymentInfoModel.setSaved(Boolean.TRUE);
		paymentInfoModel.setUser(currentUser);
		paymentInfoModel.setPaymentInfo("TID: "+ transactionJsonObject.get("tid").toString());
		paymentInfoModel.setOrderHistoryNotes("TID: "+ transactionJsonObject.get("tid").toString());
		paymentInfoModel.setPaymentProvider(payment);
		paymentInfoModel.setPaymentGatewayStatus("SUCCESS");
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
		LOG.info("++++++++315");
		LOG.info(orderData.getCode());
		String orderNumber = orderData.getCode();
		LOG.info("++++++++316");
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
        //~ final BaseStoreModel baseStore = this.getBaseStoreModel();
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
			@ApiParam(value = "Cart code for logged in user, cart GUID for guest checkout", required = true) @RequestParam final String cartId,
			@ApiParam(value = "credit card hash", required = true) @RequestParam final String panHash,
			@ApiParam(value = "credit card hash", required = true) @RequestParam final String uniqId,
			@ApiParam(value = "credit card hash", required = true) @RequestParam final String addressId,
			@ApiParam(value = "credit card hash", required = true) @RequestParam final String returnUrl,
			@ApiParam(value = "credit card hash", required = true) @RequestParam final String currentPayment,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
			throws PaymentAuthorizationException, InvalidCartException, NoCheckoutCartException
	{
		final AddressData addressData = novalnetOrderFacade.getAddressData(addressId);
		LOG.info("+++++++++++++++++++210");
		LOG.info(addressData.getFirstName());
		
		final Locale language = JaloSession.getCurrentSession().getSessionContext().getLocale();
        final String languageCode = language.toString().toUpperCase();
		LOG.info("+++++++++++++++++++210");
		LOG.info(languageCode);

		LOG.info("placeOrder");
		LOG.info("+++++++++++++++++++335");
		LOG.info("+++++++++++++++++++335");
		LOG.info(panHash);
		LOG.info("+++++++++++++++++++335");
		LOG.info("+++++++++++++++++++349");
		
		final CartData cartData = novalnetOrderFacade.loadCart(cartId);
		String totalAmount = formatAmount(String.valueOf(cartData.getTotalPriceWithTax().getValue()));
		DecimalFormat decimalFormat = new DecimalFormat("##.##");
		String orderAmount = decimalFormat.format(Float.parseFloat(totalAmount));
		float floatAmount = Float.parseFloat(orderAmount);
        BigDecimal orderAmountCents = BigDecimal.valueOf(floatAmount).multiply(BigDecimal.valueOf(100));
        Integer orderAmountCent = orderAmountCents.intValue();
		LOG.info(totalAmount);
		LOG.info("+++++++++++++++++++205");
		LOG.info("+++++++++++++++++++205");
		
		final BaseStoreModel baseStore = novalnetOrderFacade.getBaseStoreModel();
		LOG.info(baseStore.getNovalnetPaymentAccessKey());
		LOG.info("+++++++++++++++++++206");
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

        merchantParameters.put("signature", baseStore.getNovalnetAPIKey());
        merchantParameters.put("tariff", baseStore.getNovalnetTariffId());

        customerParameters.put("first_name", addressData.getFirstName());
        customerParameters.put("last_name", addressData.getLastName());
        customerParameters.put("email", "karthik_m@novalnetsolutions.com");
        customerParameters.put("customer_no", "2");
        customerParameters.put("gender", "u");

        billingParameters.put("street", addressData.getLine1() +" "+ addressData.getLine2());
        billingParameters.put("city", addressData.getTown());
        billingParameters.put("zip",addressData.getPostalCode());
        billingParameters.put("country_code", addressData.getCountry().getIsocode());
        
        shippingParameters.put("same_as_billing", "1");

        customerParameters.put("billing", billingParameters);
        customerParameters.put("shipping", shippingParameters);

        transactionParameters.put("payment_type", getPaymentType(currentPayment));
        transactionParameters.put("currency", currency);
        transactionParameters.put("amount", orderAmountCent);
        transactionParameters.put("system_name", "SAP Commerce Cloud");
        transactionParameters.put("system_version", "2105-NN1.0.1");
        customParameters.put("lang", "EN");

        if ("novalnetCreditCard".equals(currentPayment)) {
			paymentParameters.put("pan_hash", panHash);
			paymentParameters.put("unique_id", uniqId);
			transactionParameters.put("payment_data", paymentParameters);
		}

        dataParameters.put("merchant", merchantParameters);
        dataParameters.put("customer", customerParameters);
        dataParameters.put("transaction", transactionParameters);
        dataParameters.put("custom", customParameters);
        
        
		transactionParameters.put("return_url", returnUrl);
		transactionParameters.put("error_return_url", returnUrl);
        

        Gson gson = new GsonBuilder().create();
        String jsonString = gson.toJson(dataParameters);

        String password = baseStore.getNovalnetPaymentAccessKey().toString();
        String url = "https://payport.novalnet.de/v2/payment";
        LOG.info("+++++++++++++++++++616++++++++request");
        LOG.info(jsonString);
        StringBuilder response = sendRequest(url, jsonString);
        JSONObject tomJsonObject = new JSONObject(response.toString());
        JSONObject resultJsonObject = tomJsonObject.getJSONObject("result");
        JSONObject transactionJsonObject = tomJsonObject.getJSONObject("transaction");
        LOG.info(response.toString());
        
        if(!String.valueOf("100").equals(resultJsonObject.get("status_code").toString())) {
			final String statMessage = resultJsonObject.get("status_text").toString() != null ? resultJsonObject.get("status_text").toString() : resultJsonObject.get("status_desc").toString();
			LOG.info(statMessage);
			LOG.info("+++++++++++++++++++306");
			throw new PaymentAuthorizationException();
		}

		final Map<String, Object> responseParameters = new HashMap<String, Object>();
		String redirectURL = resultJsonObject.get("redirect_url").toString();
		responseParameters.put("redirect_url", redirectURL);
		LOG.info(redirectURL);
		jsonString = gson.toJson(responseParameters);
		//~ System.out.println(jsonString);
		//~ JSONObject sendObject = new JSONObject(jsonString);
		LOG.info("+++++++++++++++++++592");
		LOG.info(jsonString);
		//~ LOG.info(sendObject);
		return jsonString.toString();
	}
	
	
	
	/**
     * Sync data to mirakl
     *
     * @param novalnetJsonObject Order code of the order
     */
    public void syncmirakl(JSONObject tomJsonObject, String orderCode) {
        LOG.info("test============221");  
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
        
        LOG.info("test============238");  
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
        //~ JSONObject requestJsonObject = new JSONObject(jsonString);
        
        //~ final Map<String, Object> offerParameters = new HashMap<String, Object>();
        //~ offerParameters.put("price", "57.00");
        //~ offerParameters.put("shipping_price", "17.00");
        //~ offerParameters.put("shipping_type_code", "testshipping1");
        //~ offerParameters.put("offer_id", "2005");
        //~ offerParameters.put("offer_price", "57.00");
        //~ String offerString = gson.toJson(offerParameters); 
         //~ LOGGER.info("test============300");        
         //~ LOGGER.info(offerString);        
        //~ JSONArray array = new JSONArray();
        //~ String str = "{\"shipping_price\":\"17.00\",\"price\":\"57.00\",\"shipping_type_code\":\"testshipping1\",\"offer_id\":\"2005\",\"offer_price\":\"57.00\"}";
        //~ String str1 = str.replaceAll("\\\\", "");
        //~ array.put(str1);
        //~ requestJsonObject.put("offers", array);

        String url = "https://xtcommerce6.novalnet.de/mirakl_api_handler.php";
        //~ LOGGER.info("test============290");  
        
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

        LOG.info("+++response+++");
        LOG.info(response);
        LOG.info("+++response+++");
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
        //~ JSONObject respondObject = new JSONObject(jsonString);
        return jsonString.toString();
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

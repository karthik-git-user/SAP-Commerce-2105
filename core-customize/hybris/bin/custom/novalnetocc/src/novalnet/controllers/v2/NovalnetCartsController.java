package novalnet.controllers.v2;

import de.hybris.platform.acceleratorfacades.order.AcceleratorCheckoutFacade;
import de.hybris.platform.acceleratorfacades.payment.data.PaymentSubscriptionResultData;
import de.hybris.platform.acceleratorservices.payment.data.PaymentData;
import de.hybris.platform.acceleratorservices.payment.data.PaymentErrorField;
import de.hybris.platform.acceleratorocc.exceptions.PaymentProviderException;
import novalnet.controllers.InvalidPaymentInfoException;
import novalnet.controllers.NoCheckoutCartException;
//~ import novalnet.controllers.v2.NovalnetBaseCommerceController;
import novalnet.controllers.UnsupportedRequestException;
import de.hybris.platform.order.InvalidCartException;
//~ import de.hybris.platform.novalnetocc.dto.payment.PaymentRequestWsDTO;
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
import de.hybris.platform.servicelayer.model.ModelService;

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
		cartLoaderStrategy.loadCart(cartId);
		LOG.info("placeOrder");
		LOG.info("+++++++++++++++++++335");
		LOG.info("+++++++++++++++++++335");
		LOG.info(panHash);
		LOG.info(getCheckoutFacade());
		LOG.info("+++++++++++++++++++335");
		LOG.info("+++++++++++++++++++349");
		
		
		
		
		
		final Map<String, Object> transactionParameters = new HashMap<String, Object>();
        final Map<String, Object> merchantParameters = new HashMap<String, Object>();
        final Map<String, Object> customerParameters = new HashMap<String, Object>();
        final Map<String, Object> billingParameters = new HashMap<String, Object>();
        final Map<String, Object> shippingParameters = new HashMap<String, Object>();
        final Map<String, Object> customParameters = new HashMap<String, Object>();
        final Map<String, Object> paymentParameters = new HashMap<String, Object>();
        final Map<String, Object> dataParameters = new HashMap<String, Object>();

        merchantParameters.put("signature", "n7ibc7ob5t|doU3HJVoym7MQ44qonbobljblnmdli0p|qJEH3gNbeWJfIHah||f7cpn7pc");
        merchantParameters.put("tariff", "30");

        customerParameters.put("first_name", "test");
        customerParameters.put("last_name", "user");
        customerParameters.put("email", "karthik_m@novalnetsolutions.com");
        customerParameters.put("customer_no", "2");
        customerParameters.put("gender", "u");


        billingParameters.put("street", "Feringastr. 4");
        billingParameters.put("city", "Unterföhring");
        billingParameters.put("zip","85774");
        billingParameters.put("country_code", "DE");
        
        shippingParameters.put("same_as_billing", "1");

        customerParameters.put("billing", billingParameters);
        customerParameters.put("shipping", shippingParameters);

        transactionParameters.put("payment_type", "CREDITCARD");
        transactionParameters.put("currency", "EUR");
        transactionParameters.put("amount", "100");
        transactionParameters.put("system_name", "SAP Commerce Cloud");
        transactionParameters.put("system_version", "2105-NN1.0.1");
        

        customParameters.put("lang", "EN");

                paymentParameters.put("pan_hash", panHash);
                paymentParameters.put("unique_id", uniqId);
            transactionParameters.put("payment_data", paymentParameters);

        dataParameters.put("merchant", merchantParameters);
        dataParameters.put("customer", customerParameters);
        dataParameters.put("transaction", transactionParameters);
        dataParameters.put("custom", customParameters);

        Gson gson = new GsonBuilder().create();
        String jsonString = gson.toJson(dataParameters);

        String password = "a87ff679a2f3e71d9181a67b7542122c";
        String url = "https://payport.novalnet.de/v2/payment";
        StringBuilder response = sendRequest(url, jsonString);
        LOG.info(response.toString());
        
        //~ NovalnetPaymentInfoModel paymentInfoModel = new NovalnetPaymentInfoModel();
		//~ paymentInfoModel.setBillingAddress(billingAddress);
		//~ paymentInfoModel.setPaymentEmailAddress(email);
		//~ paymentInfoModel.setDuplicate(Boolean.FALSE);
		//~ paymentInfoModel.setSaved(Boolean.TRUE);
		//~ paymentInfoModel.setUser(currentUser);
		//~ paymentInfoModel.setPaymentInfo(orderComments);
		//~ paymentInfoModel.setOrderHistoryNotes(bankDetails);
		//~ paymentInfoModel.setPaymentProvider(currentPayment);
		//~ paymentInfoModel.setPaymentGatewayStatus(transactionStatus);
		//~ cartModel.setPaymentInfo(paymentInfoModel);
        
        
        LOG.info("++++++++872");
		
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
    
    protected CheckoutFacade getCheckoutFacade()
	{
		return checkoutFacade;
	}

	protected void setCheckoutFacade(final CheckoutFacade checkoutFacade)
	{
		this.checkoutFacade = checkoutFacade;
	}
	
	public BaseStoreService getBaseStoreService() {
        return baseStoreService;
    }

    public void setBaseStoreService(BaseStoreService baseStoreService) {
        this.baseStoreService = baseStoreService;
    }
    
    //~ protected ModelService getModelService()
	//~ {
		//~ return modelService;
	//~ }

	//~ protected UserService getUserService()
	//~ {
		//~ return userService;
	//~ }

}

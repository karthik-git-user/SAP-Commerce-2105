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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import de.hybris.platform.orderprocessing.model.OrderProcessModel;
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
import novalnet.dto.payment.NnCallbackRequestWsDTO;
import novalnet.dto.payment.NnRequestWsDTO;
import de.novalnet.beans.NnCreditCardData;
import de.novalnet.beans.NnDirectDebitSepaData;
import de.novalnet.beans.NnPayPalData;
import de.novalnet.beans.NnRequestData;
import de.novalnet.beans.NnBillingData;
import de.novalnet.beans.NnCountryData;
import de.novalnet.beans.NnRegionData;
import de.novalnet.beans.NnPaymentData;
import de.novalnet.beans.NnPaymentsData;
import de.novalnet.beans.NnConfigData;
import de.novalnet.beans.NnCallbackEventData;
import de.novalnet.beans.NnCallbackRequestData;
import de.novalnet.beans.NnCallbackResponseData;
import novalnet.dto.payment.NnConfigWsDTO;
import novalnet.dto.payment.NnCallbackResponseWsDTO;

import java.text.NumberFormat;
import java.text.DecimalFormat;

import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping(value = "/{baseSiteId}/novalnet")
@ApiVersion("v2")
@Api(tags = "Novalnet Callback")
public class NovalnetCallbackController 
{
    private final static Logger LOG = Logger.getLogger(NovalnetCallbackController.class);
    
    protected static final String DEFAULT_FIELD_SET = FieldSetLevelHelper.DEFAULT_LEVEL;
    
    private BaseStoreModel baseStore;
    private CartData cartData;
    private CartModel cartModel;
    private String password;
   
    private static final String PAYMENT_AUTHORIZE = "AUTHORIZE";
    public static final int REQUEST_IP = 4;
    private boolean testMode = false;
    private boolean errorFlag = false;

    @Resource(name = "novalnetOrderFacade")
    NovalnetOrderFacade novalnetOrderFacade;
    
    @Resource(name = "dataMapper")
    private DataMapper dataMapper;
    
    @Resource
    private PaymentModeService paymentModeService;

    private static final String REQUEST_MAPPING = "paymentType,action,cartId,billingAddress(titleCode,firstName,lastName,line1,line2,town,postalCode,country(isocode),region(isocode)),paymentData(panHash,uniqId,iban),returnUrl,tid";
    

    @RequestMapping(value = "/callback", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    @ApiOperation(nickname = "callback", value = "handle callback request", notes = "keeps the transactions in sync between novalnet and the sap commerce")
    @ApiBaseSiteIdAndUserIdParam
    public NnCallbackResponseWsDTO handleCallback(
			@ApiParam(value =
    "Request body parameter that contains callback request", required = true) @RequestBody final NnCallbackRequestWsDTO callbackRequest,
            @ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields, final HttpServletRequest request)
            throws UnknownHostException
    {
		
		NnCallbackResponseData callbackResponseData = new NnCallbackResponseData();
        NnCallbackRequestData callbackRequestData = dataMapper.map(callbackRequest, NnCallbackRequestData.class, fields);
        
        String ipCheck = checkIP(request);
        
        if(errorFlag) {
			callbackResponseData.setMessage(ipCheck);
			return dataMapper.map(callbackResponseData, NnCallbackResponseWsDTO.class, fields);
		}
        
        callbackResponseData.setMessage("Callback recieved");
        return dataMapper.map(callbackResponseData, NnCallbackResponseWsDTO.class, fields);
        
    }
    
    public static String checkmandateParams(NnCallbackRequestData callbackRequestData) {
		Map<String, String[]> mandate = new HashMap<String, String[]>();
		String[] eventParams = {"type", "checksum", "tid"};
		String[] merchantParams = {"vendor", "project"};
		String[] transactionParams = {"tid", "payment_type", "status"};
		String[] resultParams = {"status"};
		paymentTypes.put("event", eventParams);
		paymentTypes.put("mercahnt", eventParams);
		paymentTypes.put("transaction", eventParams);
		paymentTypes.put("result", eventParams);
		
		return "";
	}
	
    public static String checkIP(HttpServletRequest request) {
		
        String vendorScriptHostIpAddress = "";
        final BaseStoreModel baseStore = novalnetOrderFacade.getBaseStoreModel();
        
        try {
			InetAddress address = InetAddress.getByName("pay-nn.de"); //Novalnet vendor script host
			vendorScriptHostIpAddress = address.getHostAddress();
		} catch (UnknownHostException e) {
			errorFlag = true;
			return "error while fetching novalnet IP address : " + e;
		}
		
		String callerIp = request.getHeader("HTTP_X_FORWARDED_FOR");

		if (callerIp == null || callerIp.split("[.]").length != REQUEST_IP) {
			callerIp = request.getRemoteAddr();
		}
		
		testMode = baseStore.getNovalnetVendorscriptTestMode();
		LOG.info("novalnet vecdor script test mode : " + testMode);
		
		if (!vendorScriptHostIpAddress.equals(callerIp) && !testMode) {
			errorFlag = true;
			return "Novalnet webhook received. Unauthorised access from the IP " + callerIp;
		}
		
		return "";
    }

    
}

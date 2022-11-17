package novalnet.controllers.v2;

import novalnet.controllers.NoCheckoutCartException;
import novalnet.controllers.NovalnetPaymentException;
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

import java.text.SimpleDateFormat;
import java.util.Calendar;

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

import de.hybris.novalnet.core.model.NovalnetDirectDebitSepaPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetGuaranteedDirectDebitSepaPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetGuaranteedInvoicePaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetPayPalPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetCreditCardPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetInvoicePaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetPrepaymentPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetBarzahlenPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetInstantBankTransferPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetOnlineBankTransferPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetBancontactPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetMultibancoPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetIdealPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetEpsPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetGiropayPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetPrzelewy24PaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetPostFinanceCardPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetPostFinancePaymentModeModel;

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
import novalnet.dto.payment.NnConfigWsDTO;
import java.text.NumberFormat;
import java.text.DecimalFormat;

// import com.mirakl.hybris.fulfilmentprocess.actions.order.CreateMarketplaceOrderAction;

@Controller
@RequestMapping(value = "/{baseSiteId}/novalnet/config")
@ApiVersion("v2")
@Api(tags = "Novalnet Carts")
public class NovalnetConfigController 
{
    private final static Logger LOG = Logger.getLogger(NovalnetConfigController.class);
    
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

    private static final String REQUEST_MAPPING = "paymentType,action,cartId,billingAddress(titleCode,firstName,lastName,line1,line2,town,postalCode,country(isocode),region(isocode)),paymentData(panHash,uniqId,iban),returnUrl,tid";
    
    protected static final String API_COMPATIBILITY_B2C_CHANNELS = "api.compatibility.b2c.channels";

    
    
    @Secured({ "ROLE_CUSTOMERGROUP", "ROLE_CLIENT", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT" })
    @RequestMapping(value = "/users/{userId}/novalnet/payment/config", method = RequestMethod.GET)
    @RequestMappingOverride
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    @SiteChannelRestriction(allowedSiteChannelsProperty = API_COMPATIBILITY_B2C_CHANNELS)
    @ApiOperation(nickname = "paymentConfig", value = "return payment configuration", notes = "return payment configuration stored in Backend")
    @ApiBaseSiteIdAndUserIdParam
    public String getPaymentConfig(
            @ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
            throws PaymentAuthorizationException, InvalidCartException, NoCheckoutCartException
    {
        // final BaseStoreModel baseStore = novalnetOrderFacade.getBaseStoreModel();
        // PaymentModeModel directDebitSepaPaymentModeModel = paymentModeService.getPaymentModeForCode("novalnetDirectDebitSepa");
        // NovalnetDirectDebitSepaPaymentModeModel novalnetDirectDebitSepaPaymentMethod = (NovalnetDirectDebitSepaPaymentModeModel) directDebitSepaPaymentModeModel;
        // PaymentModeModel payPalPaymentModeModel = paymentModeService.getPaymentModeForCode("novalnetPayPal");
        // NovalnetPayPalPaymentModeModel novalnetPayPalPaymentMethod = (NovalnetPayPalPaymentModeModel) payPalPaymentModeModel;
        // PaymentModeModel creditCardPaymentModeModel = paymentModeService.getPaymentModeForCode("novalnetCreditCard");
        // NovalnetCreditCardPaymentModeModel novalnetCreditCardPaymentMethod = (NovalnetCreditCardPaymentModeModel) creditCardPaymentModeModel;

        // NnCreditCardData creditCardData = new NnCreditCardData();
        // creditCardData.setActive(novalnetCreditCardPaymentMethod.getActive());

        // NnDirectDebitSepaData directDebitSepaData = new NnDirectDebitSepaData();
        // directDebitSepaData.setActive(novalnetDirectDebitSepaPaymentMethod.getActive());

        // NnPayPalData payPalData = new NnPayPalData();
        // payPalData.setActive(novalnetPayPalPaymentMethod.getActive());

        // NnPaymentData paymentData = new NnPaymentData();
        // paymentData.setNovalnetCreditCard(creditCardData);
        // paymentData.setNovalnetDirectDebitSepa(directDebitSepaData);
        // paymentData.setNovalnetPayPal(payPalData);

        
        
        NnConfigData configData = new NnConfigData();

        configData = novalnetOrderFacade.getPaymentConfiguration();
        // // configData.setPaymentinfo(paymentData);
        // // configData.setNovalnetClienKey(baseStore.getNovalnetClientKey());

        // // return dataMapper.map(configData, NnConfigWsDTO.class, fields);

        // Map<String, String> responseDeatils = new HashMap<String, String>();
        // Map<String, Object> dataParameters  = new HashMap<String, Object>();
        // Gson gson = new GsonBuilder().create();
        // String[] paymentTypes = {"novalnetCreditCard", "novalnetDirectDebitSepa", "novalnetGuaranteedDirectDebitSepa", "novalnetInvoice", "novalnetGuaranteedInvoice", "novalnetPrepayment", "novalnetMultibanco", "novalnetBarzahlen", "novalnetPayPal", "novalnetInstantBankTransfer", "novalnetOnlineBankTransfer", "novalnetBancontact", "novalnetPostFinanceCard", "novalnetPostFinance", "novalnetIdeal", "novalnetEps", "novalnetGiropay", "novalnetPrzelewy24"};

        // for (String payment : paymentTypes) {
        //     responseDeatils = novalnetOrderFacade.getPaymentConfiguration();
        //     dataParameters.put(payment, responseDeatils);
        // }
        
        // String jsonString = gson.toJson(dataParameters);

        return dataMapper.map(configData, NnConfigWsDTO.class, fields);
    }

}

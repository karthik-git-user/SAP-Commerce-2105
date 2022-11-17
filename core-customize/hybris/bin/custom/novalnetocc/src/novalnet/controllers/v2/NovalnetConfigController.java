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

import de.novalnet.beans.NnGuaranteedDirectDebitSepaData;
import de.novalnet.beans.NnGuaranteedInvoiceData;
import de.novalnet.beans.NnInvoiceData;
import de.novalnet.beans.NnPrepaymentData;
import de.novalnet.beans.NnBarzahlenData;
import de.novalnet.beans.NnInstantBankTransferData;
import de.novalnet.beans.NnOnlineBankTransferData;
import de.novalnet.beans.NnBancontactData;
import de.novalnet.beans.NnMultibancoData;
import de.novalnet.beans.NnIdealData;
import de.novalnet.beans.NnEpsData;
import de.novalnet.beans.NnGiropayData;
import de.novalnet.beans.NnPrzelewy24Data;
import de.novalnet.beans.NnPostFinanceCardData;
import de.novalnet.beans.NnPostFinanceData;

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
    public NnConfigWsDTO getPaymentConfig(
            @ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
            throws PaymentAuthorizationException, InvalidCartException, NoCheckoutCartException
    {
        final BaseStoreModel baseStore = novalnetOrderFacade.getBaseStoreModel();

        PaymentModeModel directDebitSepaPaymentModeModel            = paymentModeService.getPaymentModeForCode("novalnetDirectDebitSepa");
        PaymentModeModel payPalPaymentModeModel                     = paymentModeService.getPaymentModeForCode("novalnetPayPal");
        PaymentModeModel creditCardPaymentModeModel                 = paymentModeService.getPaymentModeForCode("novalnetCreditCard");
        PaymentModeModel invoicePaymentModeModel                    = paymentModeService.getPaymentModeForCode("novalnetInvoice");
        PaymentModeModel guaranteedInvoicePaymentModeModel          = paymentModeService.getPaymentModeForCode("novalnetGuaranteedInvoice");
        PaymentModeModel guaranteedDirectDebitSepaPaymentModeModel  = paymentModeService.getPaymentModeForCode("novalnetGuaranteedDirectDebitSepa");
        PaymentModeModel prepaymentPaymentModeModel                 = paymentModeService.getPaymentModeForCode("novalnetPrepayment");
        PaymentModeModel multibancoPaymentModeModel                 = paymentModeService.getPaymentModeForCode("novalnetMultibanco");
        PaymentModeModel barzahlenPaymentModeModel                  = paymentModeService.getPaymentModeForCode("novalnetBarzahlen");
        PaymentModeModel instantBankTransferPaymentModeModel        = paymentModeService.getPaymentModeForCode("novalnetInstantBankTransfer");
        PaymentModeModel onlineBankTransferPaymentModeModel         = paymentModeService.getPaymentModeForCode("novalnetOnlineBankTransfer");
        PaymentModeModel bancontactPaymentModeModel                 = paymentModeService.getPaymentModeForCode("novalnetBancontact");
        PaymentModeModel postFinanceCardPaymentModeModel            = paymentModeService.getPaymentModeForCode("novalnetPostFinanceCard");
        PaymentModeModel postFinancePaymentModeModel                = paymentModeService.getPaymentModeForCode("novalnetPostFinance");
        PaymentModeModel idealPaymentModeModel                      = paymentModeService.getPaymentModeForCode("novalnetIdeal");
        PaymentModeModel epsPaymentModeModel                        = paymentModeService.getPaymentModeForCode("novalnetEps");
        PaymentModeModel giropayPaymentModeModel                    = paymentModeService.getPaymentModeForCode("novalnetGiropay");
        PaymentModeModel przelewy24PaymentModeModel                 = paymentModeService.getPaymentModeForCode("novalnetPrzelewy24");

        NovalnetDirectDebitSepaPaymentModeModel novalnetDirectDebitSepaPaymentMethod    = (NovalnetDirectDebitSepaPaymentModeModel) directDebitSepaPaymentModeModel;  
        NovalnetPayPalPaymentModeModel novalnetPayPalPaymentMethod                      = (NovalnetPayPalPaymentModeModel) payPalPaymentModeModel;
        NovalnetCreditCardPaymentModeModel novalnetCreditCardPaymentMethod              = (NovalnetCreditCardPaymentModeModel) creditCardPaymentModeModel;
        NovalnetGuaranteedDirectDebitSepaPaymentModeModel novalnetGuaranteedDirectDebitSepaPaymentMethod = (NovalnetGuaranteedDirectDebitSepaPaymentModeModel) guaranteedDirectDebitSepaPaymentModeModel;
        NovalnetInvoicePaymentModeModel novalnetInvoicePaymentMethod                    = (NovalnetInvoicePaymentModeModel) invoicePaymentModeModel;
        NovalnetGuaranteedInvoicePaymentModeModel novalnetGuaranteedInvoicePaymentMethod = (NovalnetGuaranteedInvoicePaymentModeModel) guaranteedInvoicePaymentModeModel;
        NovalnetPrepaymentPaymentModeModel novalnetPrepaymentPaymentMethod              = (NovalnetPrepaymentPaymentModeModel) prepaymentPaymentModeModel;
        NovalnetMultibancoPaymentModeModel novalnetMultibancoPaymentMethod              = (NovalnetMultibancoPaymentModeModel) multibancoPaymentModeModel;
        NovalnetBarzahlenPaymentModeModel novalnetBarzahlenPaymentMethod                = (NovalnetBarzahlenPaymentModeModel) barzahlenPaymentModeModel;
        NovalnetInstantBankTransferPaymentModeModel novalnetInstantBankTransferPaymentMethod = (NovalnetInstantBankTransferPaymentModeModel) instantBankTransferPaymentModeModel;
        NovalnetOnlineBankTransferPaymentModeModel novalnetOnlineBankTransferPaymentMethod = (NovalnetOnlineBankTransferPaymentModeModel) onlineBankTransferPaymentModeModel;
        NovalnetBancontactPaymentModeModel novalnetBancontactPaymentMethod              = (NovalnetBancontactPaymentModeModel) bancontactPaymentModeModel;
        NovalnetPostFinanceCardPaymentModeModel novalnetPostFinanceCardPaymentMethod    = (NovalnetPostFinanceCardPaymentModeModel) postFinanceCardPaymentModeModel;
        NovalnetPostFinancePaymentModeModel nnovalnetPostFinancePaymentMethod           = (NovalnetPostFinancePaymentModeModel) postFinancePaymentModeModel;
        NovalnetIdealPaymentModeModel novalnetIdealPaymentMethod                        = (NovalnetIdealPaymentModeModel) idealPaymentModeModel;
        NovalnetEpsPaymentModeModel novalnetEpsPaymentMethod                            = (NovalnetEpsPaymentModeModel) epsPaymentModeModel;
        NovalnetGiropayPaymentModeModel novalnetGiropayPaymentMethod                    = (NovalnetGiropayPaymentModeModel) giropayPaymentModeModel;
        NovalnetPrzelewy24PaymentModeModel novalnetPrzelewy24PaymentMethod              = (NovalnetPrzelewy24PaymentModeModel) przelewy24PaymentModeModel;

        NnCreditCardData creditCardData                                 = new NnCreditCardData();
        NnDirectDebitSepaData directDebitSepaData                       = new NnDirectDebitSepaData();
        NnPayPalData payPalData                                         = new NnPayPalData();
        NnGuaranteedDirectDebitSepaData guaranteedDirectDebitSepaData   = new NnGuaranteedDirectDebitSepaData();
        NnInvoiceData invoiceData                                       = new NnInvoiceData();
        NnGuaranteedInvoiceData guaranteedInvoiceData                   = new NnGuaranteedInvoiceData();
        NnPrepaymentData prepaymentData                                 = new NnPrepaymentData();
        NnMultibancoData multibancoData                                 = new NnMultibancoData();
        NnBarzahlenData barzahlenData                                   = new NnBarzahlenData();
        NnInstantBankTransferData instantBankTransferData               = new NnInstantBankTransferData();
        NnOnlineBankTransferData onlineBankTransferData                 = new NnOnlineBankTransferData();
        NnBancontactData bancontactData                                 = new NnBancontactData();
        NnPostFinanceCardData postFinanceCardData                       = new NnPostFinanceCardData();
        NnPostFinanceData postFinanceData                               = new NnPostFinanceData();
        NnIdealData idealData                                           = new NnIdealData();
        NnEpsData epsData                                               = new NnEpsData();
        NnGiropayData giropayData                                       = new NnGiropayData();
        NnPrzelewy24Data przelewy24Data                                 = new NnPrzelewy24Data();     
        NnPaymentData paymentData                                       = new NnPaymentData();  
        NnConfigData configData                                         = new NnConfigData();


        creditCardData.setActive(novalnetCreditCardPaymentMethod.getActive());
        creditCardData.setTest_mode(novalnetCreditCardPaymentMethod.getNovalnetTestMode());
        creditCardData.setDescription(novalnetCreditCardPaymentMethod.getDescription());
        creditCardData.setOnhold_amount((novalnetCreditCardPaymentMethod.getNovalnetOnholdAmount().toString() == null) ? 0 : Integer.parseInt(novalnetCreditCardPaymentMethod.getNovalnetOnholdAmount().toString()));
        creditCardData.setOnhold_action(novalnetCreditCardPaymentMethod.getNovalnetOnholdAction().toString());
        creditCardData.setEnforce_3d(novalnetCreditCardPaymentMethod.getNovalnetEnforce3D());

        directDebitSepaData.setActive(novalnetDirectDebitSepaPaymentMethod.getActive());
        directDebitSepaData.setTest_mode(novalnetDirectDebitSepaPaymentMethod.getNovalnetTestMode());
        directDebitSepaData.setDescription(novalnetDirectDebitSepaPaymentMethod.getDescription().toString());
        directDebitSepaData.setOnhold_amount((novalnetDirectDebitSepaPaymentMethod.getNovalnetOnholdAmount().toString() == null) ? 0 : Integer.parseInt(novalnetDirectDebitSepaPaymentMethod.getNovalnetOnholdAmount().toString()));
        directDebitSepaData.setOnhold_action(novalnetDirectDebitSepaPaymentMethod.getNovalnetOnholdAction().toString());
        directDebitSepaData.setDue_date((novalnetDirectDebitSepaPaymentMethod.getNovalnetDueDate().toString() == null) ? 2 : Integer.parseInt(novalnetDirectDebitSepaPaymentMethod.getNovalnetDueDate().toString()));

        guaranteedDirectDebitSepaData.setActive(novalnetGuaranteedDirectDebitSepaPaymentMethod.getActive());
        guaranteedDirectDebitSepaData.setTest_mode(novalnetGuaranteedDirectDebitSepaPaymentMethod.getNovalnetTestMode());
        guaranteedDirectDebitSepaData.setDescription(novalnetGuaranteedDirectDebitSepaPaymentMethod.getDescription().toString());
        guaranteedDirectDebitSepaData.setOnhold_amount((novalnetGuaranteedDirectDebitSepaPaymentMethod.getNovalnetOnholdAmount().toString() == null) ? 0 : Integer.parseInt(novalnetGuaranteedDirectDebitSepaPaymentMethod.getNovalnetOnholdAmount().toString()));
        guaranteedDirectDebitSepaData.setOnhold_action(novalnetGuaranteedDirectDebitSepaPaymentMethod.getNovalnetOnholdAction().toString());
        guaranteedDirectDebitSepaData.setDue_date((novalnetGuaranteedDirectDebitSepaPaymentMethod.getNovalnetDueDate().toString() == null) ? 2 : Integer.parseInt(novalnetGuaranteedDirectDebitSepaPaymentMethod.getNovalnetDueDate().toString()));

        invoiceData.setActive(novalnetInvoicePaymentMethod.getActive());
        invoiceData.setTest_mode(novalnetInvoicePaymentMethod.getNovalnetTestMode());
        invoiceData.setDescription(novalnetInvoicePaymentMethod.getDescription().toString());
        invoiceData.setOnhold_amount((novalnetInvoicePaymentMethod.getNovalnetOnholdAmount().toString() == null) ? 0 : Integer.parseInt(novalnetInvoicePaymentMethod.getNovalnetOnholdAmount().toString()));
        invoiceData.setOnhold_action(novalnetInvoicePaymentMethod.getNovalnetOnholdAction().toString());
        invoiceData.setDue_date((novalnetInvoicePaymentMethod.getNovalnetDueDate().toString() == null) ? 2 : Integer.parseInt(novalnetInvoicePaymentMethod.getNovalnetDueDate().toString()));

        guaranteedInvoiceData.setActive(novalnetGuaranteedInvoicePaymentMethod.getActive());
        guaranteedInvoiceData.setTest_mode(novalnetGuaranteedInvoicePaymentMethod.getNovalnetTestMode());
        guaranteedInvoiceData.setDescription(novalnetGuaranteedInvoicePaymentMethod.getDescription().toString());
        guaranteedInvoiceData.setOnhold_amount((novalnetGuaranteedInvoicePaymentMethod.getNovalnetOnholdAmount().toString() == null) ? 0 : Integer.parseInt(novalnetGuaranteedInvoicePaymentMethod.getNovalnetOnholdAmount().toString()));
        guaranteedInvoiceData.setOnhold_action(novalnetGuaranteedInvoicePaymentMethod.getNovalnetOnholdAction().toString());

        prepaymentData.setActive(novalnetPrepaymentPaymentMethod.getActive());
        prepaymentData.setTest_mode(novalnetPrepaymentPaymentMethod.getNovalnetTestMode());
        prepaymentData.setDescription(novalnetPrepaymentPaymentMethod.getDescription().toString());
        prepaymentData.setDue_date((novalnetPrepaymentPaymentMethod.getNovalnetDueDate().toString() == null) ? 7 : Integer.parseInt(novalnetPrepaymentPaymentMethod.getNovalnetDueDate().toString()));

        multibancoData.setActive(novalnetMultibancoPaymentMethod.getActive());
        multibancoData.setTest_mode(novalnetMultibancoPaymentMethod.getNovalnetTestMode());
        multibancoData.setDescription(novalnetMultibancoPaymentMethod.getDescription().toString());

        barzahlenData.setActive(novalnetBarzahlenPaymentMethod.getActive());
        barzahlenData.setTest_mode(novalnetBarzahlenPaymentMethod.getNovalnetTestMode());
        barzahlenData.setDescription(novalnetBarzahlenPaymentMethod.getDescription().toString());
        barzahlenData.setDue_date((novalnetBarzahlenPaymentMethod.getNovalnetBarzahlenslipExpiryDate() == null) ? 14 :novalnetBarzahlenPaymentMethod.getNovalnetBarzahlenslipExpiryDate());

        payPalData.setActive(novalnetPayPalPaymentMethod.getActive());
        payPalData.setTest_mode(novalnetPayPalPaymentMethod.getNovalnetTestMode());
        payPalData.setDescription(novalnetPayPalPaymentMethod.getDescription().toString());
        payPalData.setOnhold_amount((novalnetPayPalPaymentMethod.getNovalnetOnholdAmount().toString() == null) ? 0 : Integer.parseInt(novalnetPayPalPaymentMethod.getNovalnetOnholdAmount().toString()));
        payPalData.setOnhold_action(novalnetPayPalPaymentMethod.getNovalnetOnholdAction().toString());

        instantBankTransferData.setActive(novalnetInstantBankTransferPaymentMethod.getActive());
        instantBankTransferData.setTest_mode(novalnetInstantBankTransferPaymentMethod.getNovalnetTestMode());
        instantBankTransferData.setDescription(novalnetInstantBankTransferPaymentMethod.getDescription().toString());

        onlineBankTransferData.setActive(novalnetOnlineBankTransferPaymentMethod.getActive());
        onlineBankTransferData.setTest_mode(novalnetOnlineBankTransferPaymentMethod.getNovalnetTestMode());
        onlineBankTransferData.setDescription(novalnetOnlineBankTransferPaymentMethod.getDescription().toString());

        bancontactData.setActive(novalnetBancontactPaymentMethod.getActive());
        bancontactData.setTest_mode(novalnetBancontactPaymentMethod.getNovalnetTestMode());
        bancontactData.setDescription(novalnetBancontactPaymentMethod.getDescription().toString());

        postFinanceCardData.setActive(novalnetPostFinanceCardPaymentMethod.getActive());
        postFinanceCardData.setTest_mode(novalnetPostFinanceCardPaymentMethod.getNovalnetTestMode());
        postFinanceCardData.setDescription(novalnetPostFinanceCardPaymentMethod.getDescription().toString());

        postFinanceData.setActive(nnovalnetPostFinancePaymentMethod.getActive());
        postFinanceData.setTest_mode(nnovalnetPostFinancePaymentMethod.getNovalnetTestMode());
        postFinanceData.setDescription(nnovalnetPostFinancePaymentMethod.getDescription().toString());

        idealData.setActive(novalnetIdealPaymentMethod.getActive());
        idealData.setTest_mode(novalnetIdealPaymentMethod.getNovalnetTestMode());
        idealData.setDescription(novalnetIdealPaymentMethod.getDescription().toString());

        epsData.setActive(novalnetEpsPaymentMethod.getActive());
        epsData.setTest_mode(novalnetEpsPaymentMethod.getNovalnetTestMode());
        epsData.setDescription(novalnetEpsPaymentMethod.getDescription().toString());

        giropayData.setActive(novalnetGiropayPaymentMethod.getActive());
        giropayData.setTest_mode(novalnetGiropayPaymentMethod.getNovalnetTestMode());
        giropayData.setDescription(novalnetGiropayPaymentMethod.getDescription().toString());

        przelewy24Data.setActive(novalnetPrzelewy24PaymentMethod.getActive());
        przelewy24Data.setTest_mode(novalnetPrzelewy24PaymentMethod.getNovalnetTestMode());
        przelewy24Data.setDescription(novalnetPrzelewy24PaymentMethod.getDescription().toString());


        paymentData.setNovalnetCreditCard(creditCardData);
        paymentData.setNovalnetDirectDebitSepa(directDebitSepaData);
        paymentData.setNovalnetGuaranteedDirectDebitSepa(guaranteedDirectDebitSepaData);
        paymentData.setNovalnetInvoice(invoiceData);
        paymentData.setNovalnetGuaranteedInvoice(guaranteedInvoiceData);
        paymentData.setNovalnetPrepayment(prepaymentData);
        paymentData.setNovalnetMultibanco(multibancoData);
        paymentData.setNovalnetBarzahlen(barzahlenData);
        paymentData.setNovalnetPayPal(payPalData);
        paymentData.setNovalnetInstantBankTransfer(instantBankTransferData);
        paymentData.setNovalnetOnlineBankTransfer(onlineBankTransferData);
        paymentData.setNovalnetBancontact(bancontactData);
        paymentData.setNovalnetPostFinanceCard(postFinanceCardData);
        paymentData.setNovalnetPostFinance(postFinanceData);
        paymentData.setNovalnetIdeal(idealData);
        paymentData.setNovalnetEps(epsData);
        paymentData.setNovalnetGiropay(giropayData);
        paymentData.setNovalnetPrzelewy24(przelewy24Data);

        configData.setPaymentinfo(paymentData);
        configData.setNovalnetClienKey(baseStore.getNovalnetClientKey());

        return dataMapper.map(configData, NnConfigWsDTO.class, fields);

        // creditCardData.setActive(novalnetCreditCardPaymentMethod.getActive());

        // NnDirectDebitSepaData directDebitSepaData = new NnDirectDebitSepaData();
        // directDebitSepaData.setActive(novalnetDirectDebitSepaPaymentMethod.getActive());

        // NnPayPalData payPalData = new NnPayPalData();
        // payPalData.setActive(novalnetPayPalPaymentMethod.getActive());

        // NnPaymentData paymentData = new NnPaymentData();
        // paymentData.setNovalnetCreditCard(creditCardData);
        // paymentData.setNovalnetDirectDebitSepa(directDebitSepaData);
        // paymentData.setNovalnetPayPal(payPalData);

        
        
        

        // configData = novalnetOrderFacade.getPaymentConfiguration();
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

        // return dataMapper.map(configData, NnConfigWsDTO.class, fields);
    }

}

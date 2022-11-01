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
import java.math.*;

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
import de.novalnet.beans.NnCallbackMerchantData;
import de.novalnet.beans.NnCallbackResultData;
import de.novalnet.beans.NnCallbackRequestData;
import de.novalnet.beans.NnCallbackResponseData;
import de.novalnet.beans.NnCallbackTransactionData;
import de.novalnet.beans.NnCallbackRefundData;
import novalnet.dto.payment.NnConfigWsDTO;
import novalnet.dto.payment.NnCallbackResponseWsDTO;

import java.text.NumberFormat;
import java.text.DecimalFormat;

import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.servlet.http.HttpServletRequest;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
    public String requestPaymentType = "";
    public String callbackComments;
    public String transactionStatus;
    public Date currentDate = new Date();
    public long amountToBeFormat = 0;
    public BigDecimal formattedAmount = new BigDecimal(0);
   
    private static final String PAYMENT_AUTHORIZE = "AUTHORIZE";
    public static final int REQUEST_IP = 4;
    private boolean testMode = false;
    private boolean errorFlag = false;

    public Map<String, String> capturePayments = new HashMap<String, String>();
    public Map<String, String> cancelPayments = new HashMap<String, String>();
    public Map<String, String> updatePayments = new HashMap<String, String>();
    public Map<String, String> refundPayments = new HashMap<String, String>();
    public Map<String, String> creditPayments = new HashMap<String, String>();
    public Map<String, String> initialPayments = new HashMap<String, String>();
    public Map<String, String> chargebackPayments = new HashMap<String, String>();
    public Map<String, String> collectionPayments = new HashMap<String, String>();
	public Map<String, String[]> paymentTypes = new HashMap<String, String[]>();

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
            throws UnknownHostException, NoSuchAlgorithmException
    {
		
		NnCallbackResponseData callbackResponseData = new NnCallbackResponseData();
        NnCallbackRequestData callbackRequestData = dataMapper.map(callbackRequest, NnCallbackRequestData.class, fields);
        String ipCheck = checkIP(request);

        if(errorFlag) {
			callbackResponseData.setMessage(ipCheck);
			return dataMapper.map(callbackResponseData, NnCallbackResponseWsDTO.class, fields);
		} else {
			LOG.info(ipCheck);
		}
		
		String mandateCheck = checkmandateParams(callbackRequestData);
		
		if(errorFlag) {
			callbackResponseData.setMessage(mandateCheck);
			return dataMapper.map(callbackResponseData, NnCallbackResponseWsDTO.class, fields);
		} else {
			LOG.info(mandateCheck);
		}
		
		String Checksum = validateChecksum(callbackRequestData);
		
		if(errorFlag) {
			callbackResponseData.setMessage(Checksum);
			return dataMapper.map(callbackResponseData, NnCallbackResponseWsDTO.class, fields);
		} else {
			LOG.info(Checksum);
		}
		
		capturePayments.put("CREDITCARD", "CREDITCARD");
		capturePayments.put("INVOICE", "INVOICE");
		capturePayments.put("GUARANTEED_INVOICE", "GUARANTEED_INVOICE");
		capturePayments.put("DIRECT_DEBIT_SEPA", "DIRECT_DEBIT_SEPA");
		capturePayments.put("GUARANTEED_DIRECT_DEBIT_SEPA", "GUARANTEED_DIRECT_DEBIT_SEPA");
		capturePayments.put("PAYPAL", "PAYPAL");

		cancelPayments.put("CREDITCARD", "CREDITCARD");
		cancelPayments.put("INVOICE", "INVOICE");
		cancelPayments.put("GUARANTEED_INVOICE", "GUARANTEED_INVOICE");
		cancelPayments.put("DIRECT_DEBIT_SEPA", "DIRECT_DEBIT_SEPA");
		cancelPayments.put("GUARANTEED_DIRECT_DEBIT_SEPA", "GUARANTEED_DIRECT_DEBIT_SEPA");
		cancelPayments.put("PAYPAL", "PAYPAL");
		cancelPayments.put("PRZELEWY24", "PRZELEWY24");

		updatePayments.put("CREDITCARD", "CREDITCARD");
		updatePayments.put("INVOICE_START", "INVOICE_START");
		updatePayments.put("PREPAYMENT", "PREPAYMENT");
		updatePayments.put("GUARANTEED_INVOICE", "GUARANTEED_INVOICE");
		updatePayments.put("DIRECT_DEBIT_SEPA", "DIRECT_DEBIT_SEPA");
		updatePayments.put("GUARANTEED_DIRECT_DEBIT_SEPA", "GUARANTEED_DIRECT_DEBIT_SEPA");
		updatePayments.put("PAYPAL", "PAYPAL");
		updatePayments.put("PRZELEWY24", "PRZELEWY24");
		updatePayments.put("CASHPAYMENT", "CASHPAYMENT");
		updatePayments.put("POSTFINANCE", "POSTFINANCE");
		updatePayments.put("POSTFINANCE_CARD", "POSTFINANCE_CARD");
		
		refundPayments.put("CREDITCARD_BOOKBACK", "CREDITCARD_BOOKBACK");
		refundPayments.put("REFUND_BY_BANK_TRANSFER_EU", "REFUND_BY_BANK_TRANSFER_EU");
		refundPayments.put("PAYPAL_BOOKBACK", "PAYPAL_BOOKBACK");
		refundPayments.put("PRZELEWY24_REFUND", "PRZELEWY24_REFUND");
		refundPayments.put("CASHPAYMENT_REFUND", "CASHPAYMENT_REFUND");
		refundPayments.put("POSTFINANCE_REFUND", "POSTFINANCE_REFUND");
		refundPayments.put("GUARANTEED_INVOICE_BOOKBACK", "GUARANTEED_INVOICE_BOOKBACK");
		refundPayments.put("GUARANTEED_SEPA_BOOKBACK", "GUARANTEED_SEPA_BOOKBACK");
		
		creditPayments.put("INVOICE_CREDIT", "INVOICE_CREDIT");
		creditPayments.put("CREDIT_ENTRY_CREDITCARD", "CREDIT_ENTRY_CREDITCARD");
		creditPayments.put("CREDIT_ENTRY_SEPA", "CREDIT_ENTRY_SEPA");
		creditPayments.put("DEBT_COLLECTION_SEPA", "DEBT_COLLECTION_SEPA");
		creditPayments.put("DEBT_COLLECTION_CREDITCARD", "DEBT_COLLECTION_CREDITCARD");
		creditPayments.put("GUARANTEED_DEBT_COLLECTION", "GUARANTEED_DEBT_COLLECTION");
		creditPayments.put("CASHPAYMENT_CREDIT", "CASHPAYMENT_CREDIT");
		creditPayments.put("ONLINE_TRANSFER_CREDIT", "ONLINE_TRANSFER_CREDIT");
		creditPayments.put("MULTIBANCO_CREDIT", "MULTIBANCO_CREDIT");
		creditPayments.put("CREDIT_ENTRY_DE", "CREDIT_ENTRY_DE");
		
		initialPayments.put("CREDITCARD", "CREDITCARD");
		initialPayments.put("INVOICE_START", "INVOICE_START");
		initialPayments.put("GUARANTEED_INVOICE", "GUARANTEED_INVOICE");
		initialPayments.put("DIRECT_DEBIT_SEPA", "DIRECT_DEBIT_SEPA");
		initialPayments.put("GUARANTEED_DIRECT_DEBIT_SEPA", "GUARANTEED_DIRECT_DEBIT_SEPA");
		initialPayments.put("GUARANTEED_INSTALLMENT_PAYMENT", "GUARANTEED_INSTALLMENT_PAYMENT");
		initialPayments.put("PAYPAL", "PAYPAL");
		initialPayments.put("ONLINE_TRANSFER", "ONLINE_TRANSFER");
		initialPayments.put("ONLINE_BANK_TRANSFER", "ONLINE_BANK_TRANSFER");
		initialPayments.put("IDEAL", "IDEAL");
		initialPayments.put("EPS", "EPS");
		initialPayments.put("PAYSAFECARD", "PAYSAFECARD");
		initialPayments.put("GIROPAY", "GIROPAY");
		initialPayments.put("PRZELEWY24", "PRZELEWY24");
		initialPayments.put("CASHPAYMENT", "CASHPAYMENT");
		initialPayments.put("POSTFINANCE", "POSTFINANCE");
		initialPayments.put("POSTFINANCE_CARD", "POSTFINANCE_CARD");
		
		chargebackPayments.put("RETURN_DEBIT_SEPA", "RETURN_DEBIT_SEPA");
		chargebackPayments.put("REVERSAL", "REVERSAL");
		chargebackPayments.put("CREDITCARD_CHARGEBACK", "CREDITCARD_CHARGEBACK");
		chargebackPayments.put("PAYPAL_CHARGEBACK", "PAYPAL_CHARGEBACK");
		
		collectionPayments.put("INVOICE_CREDIT", "INVOICE_CREDIT");
		collectionPayments.put("CREDIT_ENTRY_CREDITCARD", "CREDIT_ENTRY_CREDITCARD");
		collectionPayments.put("CREDIT_ENTRY_SEPA", "CREDIT_ENTRY_SEPA");
		collectionPayments.put("GUARANTEED_CREDIT_ENTRY_SEPA", "GUARANTEED_CREDIT_ENTRY_SEPA");
		collectionPayments.put("DEBT_COLLECTION_SEPA", "DEBT_COLLECTION_SEPA");
		collectionPayments.put("DEBT_COLLECTION_CREDITCARD", "DEBT_COLLECTION_CREDITCARD");
		collectionPayments.put("GUARANTEED_DEBT_COLLECTION", "GUARANTEED_DEBT_COLLECTION");
		collectionPayments.put("CASHPAYMENT_CREDIT", "CASHPAYMENT_CREDIT");
		collectionPayments.put("DEBT_COLLECTION_DE", "DEBT_COLLECTION_DE");

		// Payment types for each payment method
		String[] creditCardPaymentTypes = {"CREDITCARD", "CREDITCARD_CHARGEBACK", "CREDITCARD_BOOKBACK", "TRANSACTION_CANCELLATION", "CREDIT_ENTRY_CREDITCARD", "DEBT_COLLECTION_CREDITCARD"};
		String[] directDebitSepaPaymentTypes = {"DIRECT_DEBIT_SEPA", "RETURN_DEBIT_SEPA", "REFUND_BY_BANK_TRANSFER_EU", "TRANSACTION_CANCELLATION", "CREDIT_ENTRY_SEPA", "DEBT_COLLECTION_SEPA"};
		String[] invoicePaymentTypes = {"INVOICE_START", "INVOICE_CREDIT", "TRANSACTION_CANCELLATION", "REFUND_BY_BANK_TRANSFER_EU", "CREDIT_ENTRY_DE", "DEBT_COLLECTION_DE", "INVOICE"};
		String[] prepaymentPaymentTypes = {"PREPAYMENT", "INVOICE_CREDIT", "REFUND_BY_BANK_TRANSFER_EU", "CREDIT_ENTRY_DE", "DEBT_COLLECTION_DE"};
		String[] multibancoPaymentTypes = {"MULTIBANCO", "MULTIBANCO_CREDIT"};
		String[] payPalPaymentTypes = {"PAYPAL", "PAYPAL_BOOKBACK", "REFUND_BY_BANK_TRANSFER_EU"};
		String[] instantBankTransferPaymentTypes = {"ONLINE_TRANSFER", "REFUND_BY_BANK_TRANSFER_EU", "CREDIT_ENTRY_DE", "REVERSAL", "DEBT_COLLECTION_DE", "ONLINE_TRANSFER_CREDIT"};
		String[] onlineBankTransferPaymentTypes = {"ONLINE_BANK_TRANSFER", "REFUND_BY_BANK_TRANSFER_EU", "CREDIT_ENTRY_DE", "REVERSAL", "DEBT_COLLECTION_DE", "ONLINE_TRANSFER_CREDIT"};
		String[] bancontactPaymentTypes = {"BANCONTACT", "REFUND_BY_BANK_TRANSFER_EU"};
		String[] idealPaymentTypes = {"IDEAL", "REFUND_BY_BANK_TRANSFER_EU", "CREDIT_ENTRY_DE", "REVERSAL", "DEBT_COLLECTION_DE", "ONLINE_TRANSFER_CREDIT"};
		String[] epsPaymentTypes = {"EPS", "REFUND_BY_BANK_TRANSFER_EU", "CREDIT_ENTRY_DE", "REVERSAL", "DEBT_COLLECTION_DE", "ONLINE_TRANSFER_CREDIT"};
		String[] giropayPaymentTypes = {"GIROPAY", "REFUND_BY_BANK_TRANSFER_EU", "CREDIT_ENTRY_DE", "REVERSAL", "DEBT_COLLECTION_DE", "ONLINE_TRANSFER_CREDIT"};
		String[] przelewy24PaymentTypes = {"PRZELEWY24", "PRZELEWY24_REFUND"};
		String[] cashpaymentPaymentTypes = {"CASHPAYMENT", "CASHPAYMENT_REFUND", "CASHPAYMENT_CREDIT"};
		String[] postFinancePaymentTypes = {"POSTFINANCE", "POSTFINANCE_REFUND"};
		String[] postFinanceCardPaymentTypes = {"POSTFINANCE_CARD", "POSTFINANCE_REFUND"};
		String[] guaranteedInvoicePaymentTypes = {"GUARANTEED_INVOICE", "GUARANTEED_INVOICE_BOOKBACK"};
		String[] guaranteedDirectDebitSepaPaymentTypes = {"GUARANTEED_DIRECT_DEBIT_SEPA", "GUARANTEED_SEPA_BOOKBACK"};

		paymentTypes.put("novalnetCreditCard", creditCardPaymentTypes);
		paymentTypes.put("novalnetDirectDebitSepa", directDebitSepaPaymentTypes);
		paymentTypes.put("novalnetInvoice", invoicePaymentTypes);
		paymentTypes.put("novalnetPrepayment", prepaymentPaymentTypes);
		paymentTypes.put("novalnetPayPal", payPalPaymentTypes);
		paymentTypes.put("novalnetInstantBankTransfer", instantBankTransferPaymentTypes);
		paymentTypes.put("novalnetOnlineBankTransfer", onlineBankTransferPaymentTypes);
		paymentTypes.put("novalnetIdeal", idealPaymentTypes);
		paymentTypes.put("novalnetEps", epsPaymentTypes);
		paymentTypes.put("novalnetGiropay", giropayPaymentTypes);
		paymentTypes.put("novalnetPrzelewy24", przelewy24PaymentTypes);
		paymentTypes.put("novalnetBarzahlen", cashpaymentPaymentTypes);
		paymentTypes.put("novalnetPostFinance", postFinancePaymentTypes);
		paymentTypes.put("novalnetPostFinanceCard", postFinanceCardPaymentTypes);
		paymentTypes.put("novalnetGuaranteedDirectDebitSepa", guaranteedDirectDebitSepaPaymentTypes);
		paymentTypes.put("novalnetGuaranteedInvoice", guaranteedInvoicePaymentTypes);
		paymentTypes.put("novalnetMultibanco", multibancoPaymentTypes);
		paymentTypes.put("novalnetBancontact", bancontactPaymentTypes);
		
		NnCallbackEventData eventData =  callbackRequestData.getEvent();
        NnCallbackMerchantData merchantData =  callbackRequestData.getMerchant();
        NnCallbackTransactionData transactionData =  callbackRequestData.getTransaction();
        NnCallbackResultData resultData =  callbackRequestData.getResult();
		
		Map<String, String> capturedRequiredParams = new HashMap<String, String>();
		capturedRequiredParams.put("vendor", merchantData.getVendor());
		capturedRequiredParams.put("payment_type", transactionData.getPayment_type());
		capturedRequiredParams.put("tid", transactionData.getTid());
		capturedRequiredParams.put("status", transactionData.getStatus());
		
		String requestEventype = eventData.getType();
		String requestPaymentType = transactionData.getPayment_type();
		String response = "";
		String[] refundType = {"CHARGEBACK", "TRANSACTION_REFUND"};

		long amountToBeFormat = Integer.parseInt(transactionData.getAmount().toString());
        BigDecimal formattedAmount = new BigDecimal(amountToBeFormat).movePointLeft(2);

		transactionStatus = transactionData.getStatus();

		if (Arrays.asList(refundType).contains(eventData.getType())) {
			 response = performRefund(callbackRequestData);
		} else if ("CREDIT".equals(eventData.getType())) {
			 response = performCredit(callbackRequestData);
		}

        
        callbackResponseData.setMessage(response);
		return dataMapper.map(callbackResponseData, NnCallbackResponseWsDTO.class, fields);
        
    }


    public String performCredit(NnCallbackRequestData callbackRequestData) {

    	NnCallbackEventData eventData =  callbackRequestData.getEvent();
        NnCallbackMerchantData merchantData =  callbackRequestData.getMerchant();
        NnCallbackTransactionData transactionData =  callbackRequestData.getTransaction();
        NnCallbackResultData resultData =  callbackRequestData.getResult();
        NnCallbackRefundData refundData =  transactionData.getRefund();

        final List<NovalnetCallbackInfoModel> orderReference = novalnetOrderFacade.getCallbackInfo(eventData.getParent_tid());
    	String orderNo = orderReference.get(0).getOrderNo();

    	int amountInCents = Integer.parseInt(transactionData.getAmount().toString());

        int paidAmount = orderReference.get(0).getPaidAmount();

        int orderAmount = orderReference.get(0).getOrderAmount();

        int totalAmount = paidAmount + amountInCents;

        String paymentType = orderReference.get(0).getPaymentType();
        
        String notifyComments = "";

        long callbackTid = Long.parseLong(transactionData.getTid().toString());

    	String[] creditPayment = {"CREDIT_ENTRY_CREDITCARD", "CREDIT_ENTRY_SEPA", "DEBT_COLLECTION_SEPA", "DEBT_COLLECTION_CREDITCARD", "CREDIT_ENTRY_DE", "DEBT_COLLECTION_DE"};
        String[] creditPaymentType = {"INVOICE_CREDIT", "CASHPAYMENT_CREDIT", "MULTIBANCO_CREDIT"};

        if (Arrays.asList(creditPaymentType).contains(requestPaymentType)) {
            // if settlement of invoice OR Advance payment through Customer
            if (orderAmount > paidAmount) {
                // Form callback comments
                String notifyComments = callbackComments = "Credit has been successfully received for the TID: " + eventData.getParent_tid().toString() + " with amount: " + formattedAmount + " " + transactionData.getCurrency().toString() + " on " + currentDate.toString() + ". Please refer PAID order details in our Novalnet Admin Portal for the TID: " + transactionData.getTid().toString();

                // Update PART PAID payment status
                novalnetOrderFacade.updatePartPaidStatus(orderNo);

                // Update Callback info
                novalnetOrderFacade.updateCallbackInfo(callbackTid, orderReference, totalAmount);

                // Full amount paid by the customer
                if (totalAmount >= orderAmount) {
                    // Update Callback order status
                    novalnetOrderFacade.updateCallbackOrderStatus(orderNo, paymentType);

                    // Customer paid greater than the order amount
                    if (totalAmount > orderAmount) {
                        notifyComments += ". Customer paid amount is greater than order amount.";
                    }
                }

                // Update callback comments
                novalnetOrderFacade.updateCallbackComments(callbackComments, orderNo, transactionStatus);

                // Send notification email
                sendEmail(notifyComments, toEmailAddress);
                return false;
            }
        } else if (Arrays.asList(creditPayment).contains(requestPaymentType)) {
            callbackComments = "Credit has been successfully received for the TID: " + eventData.getParent_tid().toString() + " with amount: " + formattedAmount + " " + transactionData.getCurrency().toString() + " on " + currentDate.toString() + ". Please refer PAID order details in our Novalnet Admin Portal for the TID:" + transactionData.getTid().toString() + ".";
            novalnetFacade.updateCallbackInfo(callbackTid, orderReference, totalAmount);
            novalnetFacade.updateCallbackComments(callbackComments, orderNo, transactionStatus);

            // Send notification email
            sendEmail(callbackComments, toEmailAddress);
            return false;
        } 
    }

    public String performRefund(NnCallbackRequestData callbackRequestData) {

    	NnCallbackEventData eventData =  callbackRequestData.getEvent();
        NnCallbackMerchantData merchantData =  callbackRequestData.getMerchant();
        NnCallbackTransactionData transactionData =  callbackRequestData.getTransaction();
        NnCallbackResultData resultData =  callbackRequestData.getResult();
        NnCallbackRefundData refundData =  transactionData.getRefund();

		requestPaymentType = refundData.getPayment_type();
		final List<NovalnetCallbackInfoModel> orderReference = novalnetOrderFacade.getCallbackInfo(eventData.getParent_tid());
    	String orderNo = orderReference.get(0).getOrderNo();

    	if(!refundPayments.containsValue(requestPaymentType) ||  !chargebackPayments.containsValue(requestPaymentType)) {

	    	String[] chargeBackPaymentType = {"CREDITCARD_CHARGEBACK", "PAYPAL_CHARGEBACK", "RETURN_DEBIT_SEPA", "REVERSAL"};
	        BigDecimal refundFormattedAmount = new BigDecimal(0);

	        if(!Arrays.asList(chargeBackPaymentType).contains(requestPaymentType)) {
	            long refundAmountToBeFormat = Integer.parseInt(refundData.getAmount());

	        // Format the order amount to currency format
	            refundFormattedAmount = new BigDecimal(refundAmountToBeFormat).movePointLeft(2);
	        }

	        String stidMsg = ". The subsequent TID: ";

	        if(Arrays.asList(chargeBackPaymentType).contains(requestPaymentType)) {
	            callbackComments = "Chargeback executed successfully for the TID: " + eventData.getParent_tid().toString() + " amount: " + formattedAmount + " " + transactionData.getCurrency() + " on " + currentDate.toString() + stidMsg + transactionData.getTid().toString();
	        } else if("REVERSAL".equals(requestPaymentType)) {
	            callbackComments = "Chargeback executed for reversal of TID:" + eventData.getParent_tid().toString() + " with the amount  " + formattedAmount + " " + transactionData.getCurrency().toString() + " on " + currentDate.toString() + stidMsg + transactionData.getTid().toString();
	        } else if("RETURN_DEBIT_SEPA".equals(requestPaymentType)) {
	            callbackComments = "Chargeback executed for return debit of TID:" + eventData.getParent_tid().toString() + " with the amount  " + formattedAmount + " " + transactionData.getCurrency().toString() + " on " + currentDate.toString() + stidMsg + transactionData.getTid().toString();
	        } else {
	            callbackComments =  "Refund has been initiated for the TID " + eventData.getParent_tid().toString() + " with the amount : " + refundFormattedAmount + " " + transactionData.getCurrency().toString() + ". New TID: " + transactionData.getTid().toString();
	        }

	        // Update callback comments
	        novalnetOrderFacade.updateCallbackComments(callbackComments, orderNo, transactionStatus);

	        // Send notification email
	        // sendEmail(callbackComments, toEmailAddress);

	        return callbackComments;
	    } else {
	    	return "Payment type " + requestPaymentType + " is not supported for event type " + eventData.getType();
	    }

    }

    public String checkmandateParams(NnCallbackRequestData callbackRequestData) {
        
        NnCallbackEventData eventData =  callbackRequestData.getEvent();
        NnCallbackMerchantData merchantData =  callbackRequestData.getMerchant();
        NnCallbackTransactionData transactionData =  callbackRequestData.getTransaction();
        NnCallbackResultData resultData =  callbackRequestData.getResult();
        
        try {
			
			if(eventData.getType() != null && eventData.getChecksum() != null && eventData.getTid() != null && merchantData.getVendor() != null && merchantData.getProject() != null && transactionData.getTid() != null && transactionData.getPayment_type() != null && transactionData.getStatus() != null && resultData.getStatus() != null ) {
				if (!("".equals(transactionData.getTid())) && transactionData.getTid().toString().length() != 17) {
                    errorFlag = true;
					return "TID is not valid";
                }
				return "Mandatory params are recieved";
			} else {
				errorFlag = true;
				return "Mandatory params are empty in callback request";
			}
		} catch(RuntimeException ex) {
			errorFlag = true;
			return "Mandatory params validation failed " + ex;
		}
		
	}
	
    public String checkIP(HttpServletRequest request) {
		
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
		
		
		return "IP validation passed for Callback request";
    }
    
    public String validateChecksum(NnCallbackRequestData callbackRequestData) throws NoSuchAlgorithmException {
		
		final BaseStoreModel baseStore = novalnetOrderFacade.getBaseStoreModel();
        
        NnCallbackEventData eventData =  callbackRequestData.getEvent();
        NnCallbackMerchantData merchantData =  callbackRequestData.getMerchant();
        NnCallbackTransactionData transactionData =  callbackRequestData.getTransaction();
        NnCallbackResultData resultData =  callbackRequestData.getResult();

		String tokenString = eventData.getTid() + eventData.getType() + resultData.getStatus() + transactionData.getAmount() + transactionData.getCurrency();

		if (!"".equals(baseStore.getNovalnetPaymentAccessKey())) {
			tokenString += new StringBuilder(baseStore.getNovalnetPaymentAccessKey().trim()).reverse().toString();
		}

		String createdHash = "";
		
		try{
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(tokenString.getBytes(StandardCharsets.UTF_8));
			StringBuffer hexString = new StringBuffer();

			for (int i = 0; i < hash.length; i++) {
				String hex = Integer.toHexString(0xff & hash[i]);
				if(hex.length() == 1) {
					hexString.append('0');
				}
				hexString.append(hex);
			}

			createdHash =  hexString.toString();
		} catch(RuntimeException ex) {
			errorFlag = true;
			return "RuntimeException while generating checksum " + ex;
		}

		if ( !eventData.getChecksum().equals(createdHash) ) {
			errorFlag = true;
			return "While notifying some data has been changed. The hash check failed";
		} else {
			return "Chacksum validated for the callback request";
		}
	}

    
}

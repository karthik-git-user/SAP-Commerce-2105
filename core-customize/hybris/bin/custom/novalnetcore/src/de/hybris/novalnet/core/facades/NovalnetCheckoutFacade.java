
package de.hybris.novalnet.core.facades;

import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.commercefacades.user.data.CountryData;
import de.hybris.platform.commercewebservicescommons.dto.order.PaymentDetailsListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.PaymentDetailsWsDTO;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.payment.PaymentInfoModel;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.order.exceptions.CalculationException;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adyen Checkout Facade for initiating payments using CC or APM
 */
public interface NovalnetCheckoutFacade {

    /**
     * Add payment details to cart
     */
    PaymentDetailsWsDTO createCartPaymentDetails(PaymentDetailsWsDTO paymentDetails);

}

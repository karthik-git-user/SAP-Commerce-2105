package com.mirakl.hybris.occ.controllers;

import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.mirakl.hybris.beans.OfferData;
import com.mirakl.hybris.beans.OfferDataList;
import com.mirakl.hybris.core.util.PaginationUtils;
import com.mirakl.hybris.dto.offer.OfferListWsDTO;
import com.mirakl.hybris.facades.product.OfferFacade;

import de.hybris.platform.commerceservices.request.mapping.annotation.ApiVersion;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * Web Services Controller to expose the functionality of the {@link OfferFacade}.
 */
@Controller(value = "miraklOffersController")
@Api(tags = "Offers")
@ApiVersion("v2")
@RequestMapping(value = "/{baseSiteId}/offers")
public class MiraklOffersController extends MiraklBaseController {

  @Resource(name = "offerFacade")
  private OfferFacade offerFacade;

  @RequestMapping(method = RequestMethod.GET)
  @ResponseBody
  @ApiOperation(nickname = "getOffers", value = "Get a list of offers for a given product and additional data",
      notes = "Returns a list of offers for a given product and additional data, such as pagination options.")
  @ApiBaseSiteIdParam
  public OfferListWsDTO getOffers(
      @ApiParam(value = "The product code used to search offers", required = true) @RequestParam final String productCode,
      @ApiParam(value = "The current result page requested. Ignored if the pageSize attribute is empty") @RequestParam(required = false) final Integer currentPage,
      @ApiParam(value = "The number of results returned per page") @RequestParam(required = false) final Integer pageSize,
      @ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields, final HttpServletResponse response) {
    final OfferDataList offerDataList = new OfferDataList();
    final List<OfferData> offersForProductCode = offerFacade.getOffersForProductCode(productCode);
    if (currentPage != null && pageSize != null && currentPage >= 0 && pageSize > 0) {
      offerDataList.setOffers(PaginationUtils.getSafePage(currentPage, pageSize, offersForProductCode));
    } else {
      offerDataList.setOffers(offersForProductCode);
    }
    return getDataMapper().map(offerDataList, OfferListWsDTO.class, fields);
  }

}

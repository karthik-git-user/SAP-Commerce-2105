package com.mirakl.hybris.occ.controllers;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import com.mirakl.hybris.dto.evaluation.EvaluationPageWsDTO;
import com.mirakl.hybris.dto.shop.ShopWsDTO;
import com.mirakl.hybris.facades.shop.ShopFacade;

import de.hybris.platform.commerceservices.request.mapping.annotation.ApiVersion;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * Web Services Controller to expose the functionality of the {@link ShopFacade}.
 */
@Controller(value = "miraklShopsController")
@Api(tags = "Shops")
@ApiVersion("v2")
@RequestMapping(value = "/{baseSiteId}/shops")
public class MiraklShopsController extends MiraklBaseController {

  @Resource(name = "shopFacade")
  private ShopFacade shopFacade;

  @RequestMapping(value = "/{shopId}", method = RequestMethod.GET)
  @ResponseBody
  @ApiOperation(nickname = "getShop", value = "Get a shop by identifier", notes = "Returns a shop.")
  @ApiBaseSiteIdParam
  public ShopWsDTO getShop(@ApiParam(value = "The shop identifier", required = true) @PathVariable final String shopId,
      @ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields, final HttpServletResponse response) {
    return getDataMapper().map(shopFacade.getShopForId(shopId), ShopWsDTO.class, fields);
  }

  @RequestMapping(value = "/{shopId}/evaluations", method = RequestMethod.GET)
  @ResponseBody
  @ApiOperation(nickname = "getShopEvaluations", value = "Get the shop evaluations by shop identifier",
      notes = "Returns the evaluations of a shop.")
  @ApiBaseSiteIdParam
  public EvaluationPageWsDTO getShopEvaluations(
      @ApiParam(value = "The shop identifier", required = true) @PathVariable final String shopId,
      @ApiParam(value = "The current result page requested", required = true) @RequestParam final Integer currentPage,
      @ApiParam(value = "The number of results returned per page", required = true) @RequestParam final Integer pageSize,
      @ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields, final HttpServletResponse response) {
    return getDataMapper().map(shopFacade.getShopEvaluationPage(shopId, getPageableData(currentPage, pageSize)),
        EvaluationPageWsDTO.class, fields);
  }

}

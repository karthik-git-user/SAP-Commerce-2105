package com.mirakl.hybris.occ.controllers;

import static de.hybris.platform.webservicescommons.mapping.FieldSetLevelHelper.DEFAULT_LEVEL;

import javax.annotation.Resource;

import de.hybris.platform.commercewebservicescommons.dto.order.ConsignmentWsDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import com.mirakl.hybris.beans.AssessmentDataList;
import com.mirakl.hybris.beans.CreateThreadMessageData;
import com.mirakl.hybris.beans.EvaluationData;
import com.mirakl.hybris.dto.evaluation.AssessmentsWsDTO;
import com.mirakl.hybris.dto.evaluation.EvaluationWsDTO;
import com.mirakl.hybris.dto.message.CreateThreadMessageWsDTO;
import com.mirakl.hybris.facades.order.IncidentFacade;
import com.mirakl.hybris.facades.order.MarketplaceConsignmentFacade;
import com.mirakl.hybris.facades.order.MarketplaceOrderFacade;

import de.hybris.platform.commerceservices.request.mapping.annotation.ApiVersion;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.servicelayer.user.UserService;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdAndUserIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * Web Services Controller to expose the functionality of the {@link IncidentFacade}, {@link MarketplaceConsignmentFacade}, and
 * {@link MarketplaceOrderFacade}.
 */
@Controller(value = "miraklConsignmentController")
@RequestMapping(value = "/{baseSiteId}/users/{userId}/consignment")
@ApiVersion("v2")
@Api(tags = "Marketplace Consignments")
public class MiraklConsignmentsController extends MiraklBaseController {

  @Resource(name = "marketplaceOrderFacade")
  private MarketplaceOrderFacade marketplaceOrderFacade;
  @Resource(name = "marketplaceConsignmentFacade")
  private MarketplaceConsignmentFacade marketplaceConsignmentFacade;
  @Resource(name = "userService")
  private UserService userService;
  @Resource(name = "incidentFacade")
  private IncidentFacade incidentFacade;
  @Resource(name = "miraklCreateThreadMessageDataPopulator")
  private Populator<String, CreateThreadMessageData> createThreadMessageDataPopulator;

  @Secured({"ROLE_CUSTOMERGROUP", "ROLE_CLIENT", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT"})
  @RequestMapping(value = "/assessments", method = RequestMethod.GET)
  @ResponseBody
  @ApiOperation(nickname = "getOrderAssessments", value = "Get order assessments.", notes = "Returns the order assessments.")
  @ApiBaseSiteIdAndUserIdParam
  public AssessmentsWsDTO getOrderAssessments(@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_LEVEL) final String fields) {
    final AssessmentDataList assessmentDataList = new AssessmentDataList();
    assessmentDataList.setAssessments(marketplaceOrderFacade.getAssessments());
    return getDataMapper().map(assessmentDataList, AssessmentsWsDTO.class, fields);
  }

  @Secured({"ROLE_CUSTOMERGROUP", "ROLE_CLIENT", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT"})
  @RequestMapping(value = "/{consignmentCode}/evaluation", method = RequestMethod.POST,
      consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  @ApiOperation(nickname = "postMiraklOrderEvaluation", value = "Post a Mirakl order (consignment) evaluation.",
      notes = "Returns the Mirakl order (consignment) evaluation.")
  @ApiBaseSiteIdAndUserIdParam
  public AssessmentsWsDTO postMiraklOrderEvaluation(
      @ApiParam(value = "The consignment code", required = true) @PathVariable final String consignmentCode,
      @ApiParam(value = "The evaluation", required = true) @RequestBody final EvaluationWsDTO evaluationWsDTO,
      @ApiFieldsParam @RequestParam(defaultValue = DEFAULT_LEVEL) final String fields) {
    final EvaluationData evaluationData = getDataMapper().map(evaluationWsDTO, EvaluationData.class);
    return getDataMapper().map(
        marketplaceConsignmentFacade.postEvaluation(consignmentCode, evaluationData, userService.getCurrentUser()),
        AssessmentsWsDTO.class, fields);
  }

  @Secured({"ROLE_CUSTOMERGROUP", "ROLE_CLIENT", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT"})
  @RequestMapping(value = "/{consignmentCode}/consignmententry/{consignmentEntryCode}/incident/open",
      method = RequestMethod.POST, consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
  @ResponseStatus(HttpStatus.CREATED)
  @ApiOperation(nickname = "openIncident", value = "Open an order incident.")
  @ApiBaseSiteIdAndUserIdParam
  public void openIncident(
      @ApiParam(value = "The consignment code", required = true) @PathVariable final String consignmentCode,
      @ApiParam(value = "The consignment entry code (Mirakl order entry ID).",
          required = true) @PathVariable final String consignmentEntryCode,
      @ApiParam(value = "Create thread message DTO",
          required = true) @ModelAttribute CreateThreadMessageWsDTO createThreadMessageWsDTO,
      @ApiParam(value = "The reason code to open the incident", required = true) @RequestParam final String reasonCode) {
    final CreateThreadMessageData createThreadMessageData =
        getDataMapper().map(createThreadMessageWsDTO, CreateThreadMessageData.class);
    createThreadMessageDataPopulator.populate(consignmentCode, createThreadMessageData);
    incidentFacade.openIncident(consignmentEntryCode, reasonCode, createThreadMessageData);
  }

  @Secured({"ROLE_CUSTOMERGROUP", "ROLE_CLIENT", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT"})
  @RequestMapping(value = "/{consignmentCode}/consignmententry/{consignmentEntryCode}/incident/close",
      method = RequestMethod.POST, consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
  @ResponseStatus(HttpStatus.CREATED)
  @ApiOperation(nickname = "closeIncident", value = "Close an order incident.")
  @ApiBaseSiteIdAndUserIdParam
  public void closeIncident(
      @ApiParam(value = "The consignment code", required = true) @PathVariable final String consignmentCode,
      @ApiParam(value = "The consignment entry code (Mirakl order entry ID).",
          required = true) @PathVariable final String consignmentEntryCode,
      @ApiParam(value = "Create thread message DTO",
          required = true) @ModelAttribute CreateThreadMessageWsDTO createThreadMessageWsDTO,
      @ApiParam(value = "The reason code to close the incident", required = true) @RequestParam final String reasonCode) {
    final CreateThreadMessageData createThreadMessageData =
        getDataMapper().map(createThreadMessageWsDTO, CreateThreadMessageData.class);
    createThreadMessageDataPopulator.populate(consignmentCode, createThreadMessageData);
    incidentFacade.closeIncident(consignmentEntryCode, reasonCode, createThreadMessageData);
  }

  @Secured({"ROLE_CUSTOMERGROUP", "ROLE_CLIENT", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT"})
  @RequestMapping(value = "/{consignmentCode}/confirmreception", method = RequestMethod.POST)
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  @ApiOperation(nickname = "confirmConsignmentReception", value = "Confirm the consignment reception.")
  @ApiBaseSiteIdAndUserIdParam
  public ConsignmentWsDTO confirmConsignmentReception(
      @ApiParam(value = "The consignment code", required = true) @PathVariable final String consignmentCode,
      @ApiFieldsParam @RequestParam(defaultValue = DEFAULT_LEVEL) final String fields) {
     return getDataMapper().map(marketplaceConsignmentFacade.confirmConsignmentReceptionForCode(consignmentCode, userService.getCurrentUser()),
        ConsignmentWsDTO.class, fields);
  }

}

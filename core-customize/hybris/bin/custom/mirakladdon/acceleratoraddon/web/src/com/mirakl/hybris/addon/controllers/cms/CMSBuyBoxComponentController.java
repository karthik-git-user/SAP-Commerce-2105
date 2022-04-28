package com.mirakl.hybris.addon.controllers.cms;

import com.mirakl.hybris.addon.controllers.MirakladdonControllerConstants;
import com.mirakl.hybris.addon.model.CMSBuyBoxComponentModel;
import com.mirakl.hybris.beans.OfferData;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@RequestMapping(value = MirakladdonControllerConstants.Cms.CMSBuyBoxComponent)
public class CMSBuyBoxComponentController extends AbstractCMSOfferComponentController<CMSBuyBoxComponentModel> {

  @Override
  protected void fillModel(Model model, List<OfferData> offers) {
  }
}

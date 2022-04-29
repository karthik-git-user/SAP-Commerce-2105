package com.mirakl.hybris.core.product.populators;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNullStandardMessage;

import org.springframework.beans.factory.annotation.Required;

import com.mirakl.client.mmp.domain.offer.MiraklOffer;
import com.mirakl.hybris.core.model.OfferModel;
import com.mirakl.hybris.core.util.services.JsonMarshallingService;

import de.hybris.platform.converters.Populator;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;

public class MiraklOfferUpdatePopulator implements Populator<MiraklOffer, OfferModel> {
  protected JsonMarshallingService jsonMarshallingService;

  @Override
  public void populate(MiraklOffer miraklOffer, OfferModel offerModel) throws ConversionException {
    validateParameterNotNullStandardMessage("miraklOffer", miraklOffer);
    validateParameterNotNullStandardMessage("offerModel", offerModel);

    offerModel.setQuantity(miraklOffer.getQuantity());
    offerModel.setAllOfferPricingsJSON(jsonMarshallingService.toJson(miraklOffer.getAllPrices()));
    offerModel.setPrice(miraklOffer.getPrice());
    offerModel.setPriceAdditionalInfo(miraklOffer.getPriceAdditionalInfo());
    offerModel.setTotalPrice(miraklOffer.getTotalPrice());
  }

  @Required
  public void setJsonMarshallingService(JsonMarshallingService jsonMarshallingService) {
    this.jsonMarshallingService = jsonMarshallingService;
  }
}

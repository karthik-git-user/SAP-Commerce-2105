package com.mirakl.hybris.core.product.populators;

import com.mirakl.hybris.beans.ComparableOfferData;
import com.mirakl.hybris.core.model.OfferModel;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;

public class MiraklComparableOfferDataPopulator implements Populator<OfferModel, ComparableOfferData<OfferModel>> {

  @Override
  public void populate(OfferModel source, ComparableOfferData<OfferModel> target) throws ConversionException {
    target.setOffer(source);
    target.setTotalPrice(source.getEffectiveTotalPrice());
    target.setState(source.getState());
  }

}

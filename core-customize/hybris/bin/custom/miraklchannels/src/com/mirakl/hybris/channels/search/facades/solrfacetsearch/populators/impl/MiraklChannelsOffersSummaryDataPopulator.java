package com.mirakl.hybris.channels.search.facades.solrfacetsearch.populators.impl;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNullStandardMessage;

import java.util.List;

import com.mirakl.hybris.beans.OfferOverviewData;
import com.mirakl.hybris.beans.OffersSummaryData;

import de.hybris.platform.converters.Populator;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;

public class MiraklChannelsOffersSummaryDataPopulator implements Populator<List<OfferOverviewData>, OffersSummaryData> {

  @Override
  public void populate(List<OfferOverviewData> source, OffersSummaryData target) throws ConversionException {
    validateParameterNotNullStandardMessage("source", source);
    target.setAllOffers(source);
  }
}

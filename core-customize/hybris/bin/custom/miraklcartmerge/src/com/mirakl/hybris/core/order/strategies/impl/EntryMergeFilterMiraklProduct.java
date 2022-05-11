package com.mirakl.hybris.core.order.strategies.impl;

import javax.annotation.Nonnull;

import de.hybris.platform.commerceservices.order.EntryMergeFilter;
import de.hybris.platform.core.model.order.AbstractOrderEntryModel;

/**
 * Disable to merge between mirakl products and operator products.
 */
public class EntryMergeFilterMiraklProduct implements EntryMergeFilter {

  @Override
  public Boolean apply(@Nonnull AbstractOrderEntryModel candidate, @Nonnull AbstractOrderEntryModel target) {
    return !(candidate.getOfferId() != null && target.getOfferId() == null)
        && !(candidate.getOfferId() == null && target.getOfferId() != null);
  }

}

package com.mirakl.hybris.core.comparators;

import com.mirakl.client.mmp.domain.offer.price.MiraklVolumePrice;

import de.hybris.platform.commerceservices.util.AbstractComparator;

public class MiraklVolumePriceComparator extends AbstractComparator<MiraklVolumePrice> {

  public static final MiraklVolumePriceComparator INSTANCE = new MiraklVolumePriceComparator();

  @Override
  protected int compareInstances(final MiraklVolumePrice price1, final MiraklVolumePrice price2) {
    if (price1 == null || price1.getQuantityThreshold() == null) {
      return BEFORE;
    }
    if (price2 == null || price2.getQuantityThreshold() == null) {
      return AFTER;
    }

    return compareValues(price1.getQuantityThreshold().longValue(), price2.getQuantityThreshold().longValue());
  }

}

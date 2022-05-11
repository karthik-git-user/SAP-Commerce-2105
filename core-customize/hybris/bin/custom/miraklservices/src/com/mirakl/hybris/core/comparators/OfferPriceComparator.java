package com.mirakl.hybris.core.comparators;

import java.util.Comparator;

import com.mirakl.hybris.beans.ComparableOfferData;

public class OfferPriceComparator<T> implements Comparator<ComparableOfferData<T>> {

  @Override
  public int compare(ComparableOfferData<T> offer1, ComparableOfferData<T> offer2) {
    return offer1.getTotalPrice().compareTo(offer2.getTotalPrice());
  }
}

package com.mirakl.hybris.core.product.daos.impl;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableList;
import com.mirakl.hybris.core.model.OfferModel;
import com.mirakl.hybris.core.util.flexiblesearch.QueryDecorator;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.model.c2l.CurrencyModel;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.impl.SearchResultImpl;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class DefaultOfferDaoTest {

  private static final String OFFER_ID = "offerId";
  private static final String PRODUCT_ID = "productId";
  private static final int NUMBER_OF_OFFERS = 2;
  public static final String PRODUCT_CODE = "product_code";

  @InjectMocks
  private DefaultOfferDao offerDao = new DefaultOfferDao();

  @Mock
  private FlexibleSearchService flexibleSearchService;
  @Mock
  private OfferModel firstOfferModel, secondOfferModel;
  @Mock
  private CurrencyModel currency;
  @Mock
  private Date modificationDate;
  @Mock
  private QueryDecorator queryDecorator;
  private List<QueryDecorator> queryDecorators;

  @Before
  public void setUp() {
    queryDecorators = asList(queryDecorator);
    offerDao.setQueryDecorators(queryDecorators);
  }

  @Test
  public void findsOfferById() {
    when(flexibleSearchService.search(any(FlexibleSearchQuery.class)))
        .thenReturn(new SearchResultImpl<>(ImmutableList.<Object>of(firstOfferModel), 1, 0, 0));

    OfferModel result = offerDao.findOfferById(OFFER_ID);

    assertThat(result).isSameAs(firstOfferModel);
  }

  @Test
  public void findOfferByIdReturnsNullIfNoOfferFound() {
    when(flexibleSearchService.search(any(FlexibleSearchQuery.class))).thenReturn(new SearchResultImpl<>(emptyList(), 1, 0, 0));

    OfferModel result = offerDao.findOfferById(OFFER_ID);

    assertThat(result).isNull();
  }

  @Test
  public void findUndeletedOffersModifiedBeforeDate() {
    when(flexibleSearchService.search(any(FlexibleSearchQuery.class)))
        .thenReturn(new SearchResultImpl<>(ImmutableList.<Object>of(firstOfferModel), 1, 0, 0));

    List<OfferModel> result = offerDao.findUndeletedOffersModifiedBeforeDate(modificationDate);

    assertThat(result).containsExactly(firstOfferModel);
  }

  @Test
  public void findOffersForProductCode() {
    when(flexibleSearchService.search(any(FlexibleSearchQuery.class)))
        .thenReturn(new SearchResultImpl<>(ImmutableList.<Object>of(firstOfferModel, secondOfferModel), 2, 0, 0));

    List<OfferModel> result = offerDao.findOffersForProductCode(PRODUCT_ID);

    assertThat(result).containsExactly(firstOfferModel, secondOfferModel);
  }

  @Test
  public void countsOffersForProduct() {
    when(flexibleSearchService.search(any(FlexibleSearchQuery.class)))
        .thenReturn(new SearchResultImpl<>(ImmutableList.<Object>of(NUMBER_OF_OFFERS), 1, 0, 0));

    int result = offerDao.countOffersForProduct(PRODUCT_ID);

    assertThat(result).isEqualTo(NUMBER_OF_OFFERS);
  }

  @Test
  public void countsOffersForProductAndCurrency() {
    when(flexibleSearchService.search(any(FlexibleSearchQuery.class)))
        .thenReturn(new SearchResultImpl<>(ImmutableList.<Object>of(NUMBER_OF_OFFERS), 1, 0, 0));

    int result = offerDao.countOffersForProductAndCurrency(PRODUCT_ID, currency);

    assertThat(result).isEqualTo(NUMBER_OF_OFFERS);
  }

  @Test
  public void findLocalizedOffersForProductCode() {
    when(flexibleSearchService.search(any(FlexibleSearchQuery.class)))
        .thenReturn(new SearchResultImpl<>(ImmutableList.<Object>of(firstOfferModel, secondOfferModel), 1, 0, 0));

    List<OfferModel> result = offerDao.findOffersForProductCode(PRODUCT_CODE);

    assertThat(result).containsExactly(firstOfferModel, secondOfferModel);
  }

  @Test
  public void findLocalizedOffersForProductCodeWhenEmpty() {
    when(flexibleSearchService.search(any(FlexibleSearchQuery.class))).thenReturn(new SearchResultImpl<>(emptyList(), 0, 0, 0));

    List<OfferModel> result = offerDao.findOffersForProductCode(PRODUCT_CODE);

    assertThat(result).isEmpty();
  }
}

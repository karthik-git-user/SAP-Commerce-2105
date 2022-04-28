package com.mirakl.hybris.core.product.daos.impl;

import static com.google.common.collect.Lists.transform;
import static com.mirakl.hybris.core.util.QueryParamsBuilder.queryParams;
import static com.mirakl.hybris.core.util.flexiblesearch.impl.Condition.condition;
import static com.mirakl.hybris.core.util.flexiblesearch.impl.Condition.fieldEquals;
import static com.mirakl.hybris.core.util.flexiblesearch.impl.Field.field;
import static com.mirakl.hybris.core.util.flexiblesearch.impl.Item.item;
import static com.mirakl.hybris.core.util.flexiblesearch.impl.QueryBuilder.query;
import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNullStandardMessage;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Required;

import com.google.common.base.Function;
import com.mirakl.hybris.core.enums.OfferState;
import com.mirakl.hybris.core.model.OfferModel;
import com.mirakl.hybris.core.product.daos.OfferDao;
import com.mirakl.hybris.core.util.QueryParam;
import com.mirakl.hybris.core.util.flexiblesearch.QueryDecorator;
import com.mirakl.hybris.core.util.flexiblesearch.impl.QueryBuilder;

import de.hybris.platform.core.model.c2l.CurrencyModel;
import de.hybris.platform.servicelayer.internal.dao.DefaultGenericDao;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.SearchResult;

public class DefaultOfferDao extends DefaultGenericDao<OfferModel> implements OfferDao {

  protected List<QueryDecorator> queryDecorators;

  public DefaultOfferDao() {
    super(OfferModel._TYPECODE);
  }

  @Override
  public OfferModel findOfferById(String offerId) {
    return findOfferById(offerId,false);
  }

  @Override
  public OfferModel findOfferById(String offerId, boolean ignoreQueryDecorators) {
    validateParameterNotNullStandardMessage("offerId", offerId);

    List<OfferModel> offers = findOffersForAttributeValues(queryParams()//
        .withAttributeEquals(OfferModel.ID, offerId)//
        .build(), ignoreQueryDecorators);

    return isEmpty(offers) ? null : offers.get(0);
  }

  @Override
  public List<OfferModel> findUndeletedOffersModifiedBeforeDate(Date modificationDate) {
    validateParameterNotNullStandardMessage("modificationDate", modificationDate);

    return findOffersForAttributeValues(queryParams() //
        .withAttributeMatchingCondition(OfferModel.MODIFIEDTIME, "<", modificationDate) //
        .withAttributeEquals(OfferModel.DELETED, false) //
        .build());
  }

  @Override
  public List<OfferModel> findOffersForProductCode(String productCode) {
    validateParameterNotNullStandardMessage("product", productCode);

    return findOffersForAttributeValues(queryParams() //
        .withAttributeEquals(OfferModel.PRODUCTCODE, productCode) //
        .build());
  }

  @Override
  public List<OfferModel> findOffersForProductCodeAndCurrency(String productCode, CurrencyModel offerCurrency) {
    validateParameterNotNullStandardMessage("product", productCode);
    validateParameterNotNullStandardMessage("currency", offerCurrency);

    return findOffersForAttributeValues(queryParams() //
        .withAttributeEquals(OfferModel.PRODUCTCODE, productCode) //
        .withAttributeEquals(OfferModel.CURRENCY, offerCurrency) //
        .build());
  }

  @Override
  public List<Pair<OfferState, CurrencyModel>> findOfferStatesAndCurrencyForProductCode(String productCode) {
    validateParameterNotNullStandardMessage("product", productCode);

    QueryBuilder query = query(queryDecorators) //
        .select(field("o", OfferModel.STATE)) //
        .select(field("o", OfferModel.CURRENCY)) //
        .from(item(OfferModel._TYPECODE, "o")) //
        .where(fieldEquals(field("o", OfferModel.PRODUCTCODE), productCode)) //
        .groupBy(field("o", OfferModel.STATE)) //
        .groupBy(field("o", OfferModel.CURRENCY));

    FlexibleSearchQuery flexibleSearch = query.build();
    flexibleSearch.setResultClassList(asList(OfferState.class, CurrencyModel.class));
    SearchResult<List<Object>> result = getFlexibleSearchService().search(flexibleSearch);

    return transform(result.getResult(), new Function<List<Object>, Pair<OfferState, CurrencyModel>>() {

      @Override
      public Pair<OfferState, CurrencyModel> apply(List<Object> tuple) {
        return Pair.of((OfferState) tuple.get(0), (CurrencyModel) tuple.get(1));
      }
    });
  }

  @Override
  public int countOffersForProduct(String productCode) {
    validateParameterNotNullStandardMessage("product", productCode);

    return countOffersForAttributeValues(queryParams() //
        .withAttributeEquals(OfferModel.PRODUCTCODE, productCode) //
        .build());
  }

  @Override
  public int countOffersForProductAndCurrency(String productCode, CurrencyModel currency) {
    validateParameterNotNullStandardMessage("product", productCode);
    validateParameterNotNullStandardMessage("currency", currency);

    return countOffersForAttributeValues(queryParams() //
        .withAttributeEquals(OfferModel.PRODUCTCODE, productCode) //
        .withAttributeEquals(OfferModel.CURRENCY, currency) //
        .build());
  }

  protected int countOffersForAttributeValues(List<QueryParam> queryParams) {
    return countOffersForAttributeValues(queryParams, false);
  }

  protected int countOffersForAttributeValues(List<QueryParam> queryParams, boolean ignoreDecorators) {
    FlexibleSearchQuery flexibleSearchQuery = getFlexibleSearchQuery(queryParams, true, ignoreDecorators);
    flexibleSearchQuery.setResultClassList(singletonList(Integer.class));
    SearchResult<Integer> result = getFlexibleSearchService().search(flexibleSearchQuery);
    return result.getResult().get(0);
  }

  protected List<OfferModel> findOffersForAttributeValues(List<QueryParam> queryParams) {
    return findOffersForAttributeValues(queryParams, false);
  }

  protected List<OfferModel> findOffersForAttributeValues(List<QueryParam> queryParams, boolean ignoreDecorators) {
    FlexibleSearchQuery build = getFlexibleSearchQuery(queryParams, false, ignoreDecorators);
    SearchResult<OfferModel> result = getFlexibleSearchService().search(build);
    return result.getResult();
  }

  protected FlexibleSearchQuery getFlexibleSearchQuery(List<QueryParam> queryParams, boolean count) {
    return getFlexibleSearchQuery(queryParams, count, false);
  }

  protected FlexibleSearchQuery getFlexibleSearchQuery(List<QueryParam> queryParams, boolean count, boolean ignoreDecorators) {
    QueryBuilder query = (ignoreDecorators ? query() : query(queryDecorators)) //
        .select(field("o", OfferModel.PK), count) //
        .from(item(OfferModel._TYPECODE, "o"));
    for (QueryParam queryParam : queryParams) {
      query.and(condition(field("o", queryParam.getAttribute()), queryParam.getOperator(), queryParam.getValue()));
    }

    return query.build();
  }

  @Required
  public void setQueryDecorators(List<QueryDecorator> queryDecorators) {
    this.queryDecorators = queryDecorators;
  }

}

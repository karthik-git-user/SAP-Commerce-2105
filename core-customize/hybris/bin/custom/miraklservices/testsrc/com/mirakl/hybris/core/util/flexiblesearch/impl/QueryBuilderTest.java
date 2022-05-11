package com.mirakl.hybris.core.util.flexiblesearch.impl;

import static com.mirakl.hybris.core.util.flexiblesearch.impl.Condition.condition;
import static com.mirakl.hybris.core.util.flexiblesearch.impl.Condition.fieldEquals;
import static com.mirakl.hybris.core.util.flexiblesearch.impl.Field.field;
import static com.mirakl.hybris.core.util.flexiblesearch.impl.Item.item;
import static com.mirakl.hybris.core.util.flexiblesearch.impl.Join.entity;
import static com.mirakl.hybris.core.util.flexiblesearch.impl.QueryBuilder.query;
import static org.fest.assertions.Assertions.assertThat;

import java.util.Arrays;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;

import com.mirakl.hybris.core.model.OfferModel;
import com.mirakl.hybris.core.model.ShopModel;
import com.mirakl.hybris.core.util.flexiblesearch.QueryDecorator;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;

@UnitTest
public class QueryBuilderTest {

  @Before
  public void setUp() throws Exception {}

  @Test
  public void buildSimpleQuery() throws Exception {
    String expectedQueryString =
        "SELECT {o:pk} FROM {Offer AS o} WHERE {o:modifiedtime} < ?o_modifiedtime AND {o:deleted} = ?o_deleted";
    Date date = new Date();

    FlexibleSearchQuery flexibleSearch = query() //
        .select(field("o", OfferModel.PK)) //
        .from(item(OfferModel._TYPECODE, "o")) //
        .where(condition(field("o", OfferModel.MODIFIEDTIME), "<", date))//
        .and(condition(field("o", OfferModel.DELETED), "=", false))//
        .build();

    assertThat(flexibleSearch.getQuery()).isEqualTo(expectedQueryString);
    assertThat(flexibleSearch.getQueryParameters()).hasSize(2);
    assertThat(flexibleSearch.getQueryParameters().get("o_" + OfferModel.DELETED)).isEqualTo(false);
    assertThat(flexibleSearch.getQueryParameters().get("o_" + OfferModel.MODIFIEDTIME)).isEqualTo(date);
  }

  @Test
  public void buildQueryWithMultipleSelectsAndGroupByClause() throws Exception {
    String expectedQueryString =
        "SELECT {o:state}, {o:currency} FROM {Offer AS o} WHERE {o:productCode} = ?o_productCode GROUP BY {o:state},{o:currency}";
    String productCode = "12345678";

    FlexibleSearchQuery flexibleSearch = query()//
        .select(field("o", OfferModel.STATE)) //
        .select(field("o", OfferModel.CURRENCY)) //
        .from(item(OfferModel._TYPECODE, "o")) //
        .where(condition(field("o", OfferModel.PRODUCTCODE), "=", productCode)) //
        .groupBy(field("o", OfferModel.STATE))//
        .groupBy(field("o", OfferModel.CURRENCY))//
        .build();

    assertThat(flexibleSearch.getQuery()).isEqualTo(expectedQueryString);
    assertThat(flexibleSearch.getQueryParameters()).hasSize(1);
    assertThat(flexibleSearch.getQueryParameters().get("o_" + OfferModel.PRODUCTCODE)).isEqualTo(productCode);
  }

  @Test
  public void buildQueryWithJoinClauses() throws Exception {
    String expectedQueryString =
        "SELECT {o:pk} FROM {Offer AS o JOIN Shop2OffersRel AS rel ON {rel:target}={o:pk} JOIN Shop AS s ON {rel:source}={s:pk}} WHERE {s:id} = ?s_id";
    String shopId = "1234";

    FlexibleSearchQuery flexibleSearch = query()//
        .select(field("o", OfferModel.PK)) //
        .from(item(OfferModel._TYPECODE, "o")) //
        .join(entity(item(OfferModel._SHOP2OFFERSREL, "rel")).on(field("rel", "target"), field("o", OfferModel.PK))) //
        .join(entity(item(ShopModel._TYPECODE, "s")).on(field("rel", "source"), field("s", ShopModel.PK))) //
        .where(condition(field("s", ShopModel.ID), "=", shopId)) //
        .build();

    assertThat(flexibleSearch.getQuery()).isEqualTo(expectedQueryString);
    assertThat(flexibleSearch.getQueryParameters()).hasSize(1);
    assertThat(flexibleSearch.getQueryParameters().get("s_" + ShopModel.ID)).isEqualTo(shopId);
  }

  @Test
  public void buildQueryWithCount() throws Exception {
    String expectedQueryString = "SELECT COUNT({o:pk}) FROM {Offer AS o} WHERE {o:active} = ?o_active";

    FlexibleSearchQuery flexibleSearch = query()//
        .selectCount(field("o", OfferModel.PK)) //
        .from(item(OfferModel._TYPECODE, "o")) //
        .where(condition(field("o", OfferModel.ACTIVE), "=", true)) //
        .build();

    assertThat(flexibleSearch.getQuery()).isEqualTo(expectedQueryString);
    assertThat(flexibleSearch.getQueryParameters()).hasSize(1);
    assertThat(flexibleSearch.getQueryParameters().get("o_" + OfferModel.ACTIVE)).isEqualTo(true);
  }

  @Test
  public void idempotentToString() {
    QueryBuilder query = query(Arrays.asList(new QueryDecoratorExample()))//
        .selectCount(field("o", OfferModel.PK)) //
        .from(item(OfferModel._TYPECODE, "o")) //
        .where(condition(field("o", OfferModel.ACTIVE), "=", true));

    query.build();

    String firstCall = query.toString();
    String secondCall = query.toString();

    assertThat(firstCall).isEqualTo(secondCall);
    assertThat(query.whereClause.getConditions()).hasSize(2);
    assertThat(query.fromClause.getJoinedEntities()).hasSize(2);
  }

  private static class QueryDecoratorExample implements QueryDecorator {

    private static final String SHOP_ID = "9999";

    @Override
    public void decorate(QueryBuilder queryBuilder) {
      queryBuilder //
          .join(entity(item(OfferModel._SHOP2OFFERSREL, "rel")).on(field("rel", "target"), field("o", OfferModel.PK))) //
          .join(entity(item(ShopModel._TYPECODE, "s")).on(field("rel", "source"), field("s", ShopModel.PK))) //
          .and(fieldEquals(field("s", ShopModel.ID), SHOP_ID));
    }

  }

}

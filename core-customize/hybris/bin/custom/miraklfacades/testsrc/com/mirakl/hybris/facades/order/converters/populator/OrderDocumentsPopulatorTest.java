package com.mirakl.hybris.facades.order.converters.populator;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.fest.assertions.Assertions.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.util.collections.Sets;
import org.mockito.runners.MockitoJUnitRunner;

import com.mirakl.client.mmp.domain.order.document.MiraklOrderDocument;
import com.mirakl.hybris.beans.DocumentData;
import com.mirakl.hybris.core.model.MarketplaceConsignmentModel;
import com.mirakl.hybris.core.order.services.MiraklDocumentService;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.commercefacades.order.data.ConsignmentData;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.ordersplitting.model.ConsignmentModel;
import de.hybris.platform.servicelayer.dto.converter.Converter;

/**
 * Copyright (C) 2016 Mirakl. www.mirakl.com - info@mirakl.com All Rights Reserved. Tous droits réservés.
 */

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class OrderDocumentsPopulatorTest {

  private static final String MARKETPLACE_CONSIGNMENT_CODE_1 = "ce003377-4117-4517-a28e-3ee1bc7bf684-A";
  private static final String MARKETPLACE_CONSIGNMENT_CODE_2 = "ce003377-4117-4517-a28e-3ee1bc7bf684-B";
  private static final String VANILLA_CONSIGNMENT_CODE_1 = "ce003377-4117-4517-a28e-3ee1bc7bf684-C";
  private static final String VANILLA_CONSIGNMENT_CODE_2 = "ce003377-4117-4517-a28e-3ee1bc7bf684-D";

  @Mock
  private OrderModel orderModel;

  @Mock
  private OrderData orderData;

  @Mock
  private MarketplaceConsignmentModel marketplaceConsignment1, marketplaceConsignment2;

  @Mock
  private ConsignmentModel consignment1, consignment2;

  @Mock
  private ConsignmentData consignmentData1, consignmentData2, consignmentData3, consignmentData4;

  @Mock
  private MiraklOrderDocument document1_1, document1_2, document2_1;

  @Mock
  private DocumentData documentData1_1, documentData1_2, documentData2_1;

  @Mock
  private MiraklDocumentService documentService;

  @Mock
  private Converter<MiraklOrderDocument, DocumentData> marketplaceConsignmentDocumentConverter;

  @Captor
  private ArgumentCaptor<List<DocumentData>> documentDataListCaptor;

  @InjectMocks
  OrderDocumentsPopulator testObj;

  @Before
  public void setUp() throws Exception {
    when(orderModel.getConsignments())
        .thenReturn(Sets.newSet(marketplaceConsignment1, consignment1, marketplaceConsignment2, consignment2));
    when(orderData.getConsignments()).thenReturn(asList(consignmentData1, consignmentData2, consignmentData3, consignmentData4));
    when(marketplaceConsignment1.getCode()).thenReturn(MARKETPLACE_CONSIGNMENT_CODE_1);
    when(marketplaceConsignment2.getCode()).thenReturn(MARKETPLACE_CONSIGNMENT_CODE_2);
    when(consignment1.getCode()).thenReturn(VANILLA_CONSIGNMENT_CODE_1);
    when(consignment2.getCode()).thenReturn(VANILLA_CONSIGNMENT_CODE_2);
    when(document1_1.getOrderId()).thenReturn(MARKETPLACE_CONSIGNMENT_CODE_1);
    when(document1_2.getOrderId()).thenReturn(MARKETPLACE_CONSIGNMENT_CODE_1);
    when(document2_1.getOrderId()).thenReturn(MARKETPLACE_CONSIGNMENT_CODE_2);
    when(documentService
        .getDocumentsForMarketplaceConsignments(asList(MARKETPLACE_CONSIGNMENT_CODE_1, MARKETPLACE_CONSIGNMENT_CODE_2)))
            .thenReturn(asSet(document1_1, document1_2, document2_1));
    when(consignmentData1.getCode()).thenReturn(MARKETPLACE_CONSIGNMENT_CODE_1);
    when(consignmentData2.getCode()).thenReturn(MARKETPLACE_CONSIGNMENT_CODE_2);
    when(consignmentData3.getCode()).thenReturn(VANILLA_CONSIGNMENT_CODE_1);
    when(consignmentData4.getCode()).thenReturn(VANILLA_CONSIGNMENT_CODE_2);
    when(marketplaceConsignmentDocumentConverter
        .convertAll((Collection<MiraklOrderDocument>) argThat(hasItems(document1_1, document1_2))))
            .thenReturn(asList(documentData1_1, documentData1_2));
    when(marketplaceConsignmentDocumentConverter.convertAll((Collection<MiraklOrderDocument>) argThat(hasItems(document2_1))))
        .thenReturn(singletonList(documentData2_1));
  }

  @Test
  public void getMarketplaceConsignmentCodes() throws Exception {
    Collection<String> output = testObj.getMarketplaceConsignmentCodes(orderModel);

    assertThat(output).containsOnly(MARKETPLACE_CONSIGNMENT_CODE_1, MARKETPLACE_CONSIGNMENT_CODE_2);
  }

  @Test
  public void getDocumentsForConsignment() throws Exception {
    Collection<MiraklOrderDocument> output =
        testObj.getDocumentsForConsignment(MARKETPLACE_CONSIGNMENT_CODE_1, asSet(document1_1, document1_2, document2_1));

    assertThat(output).containsOnly(document1_1, document1_2);
  }

  @Test
  public void populate() throws Exception {
    ArgumentCaptor.forClass(List.class);

    testObj.populate(orderModel, orderData);

    verify(consignmentData1).setDocuments(documentDataListCaptor.capture());
    assertThat(documentDataListCaptor.getValue()).containsExactly(documentData1_1, documentData1_2);
    verify(consignmentData2).setDocuments(documentDataListCaptor.capture());
    assertThat(documentDataListCaptor.getValue()).containsExactly(documentData2_1);
  }

}

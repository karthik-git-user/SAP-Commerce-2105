package com.mirakl.hybris.fulfilmentprocess.jobs;

import static java.util.Arrays.asList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import com.mirakl.client.mmp.domain.order.MiraklOrder;
import com.mirakl.client.mmp.domain.order.MiraklOrders;
import com.mirakl.client.mmp.front.core.MiraklMarketplacePlatformFrontApi;
import com.mirakl.client.mmp.request.order.MiraklGetOrdersRequest;
import com.mirakl.hybris.core.model.MarketplaceConsignmentModel;
import com.mirakl.hybris.core.model.MiraklRetrieveConsignmentUpdatesCronJobModel;
import com.mirakl.hybris.core.ordersplitting.services.MarketplaceConsignmentService;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.ordersplitting.model.ConsignmentProcessModel;
import de.hybris.platform.servicelayer.model.ModelService;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class MiraklRetrieveConsignmentUpdatesJobTest {

  private static final Date LAST_EXECUTION_DATE = new Date();
  private static final int MAX_RESULTS_BY_PAGE = 10;
  private static final String CONSIGNMENT_PROCESS_CODE = "consignment-process-code";

  @Spy
  @InjectMocks
  private MiraklRetrieveConsignmentUpdatesJob job;

  @Mock
  private MarketplaceConsignmentService marketplaceConsignmentService;

  @Mock
  private ModelService modelService;

  @Mock
  private MiraklMarketplacePlatformFrontApi miraklFrontApi;

  @Mock
  private MiraklRetrieveConsignmentUpdatesCronJobModel cronJob;

  @Mock
  private MiraklOrders miraklOrders;

  @Mock
  private MarketplaceConsignmentModel consignment;

  @Mock
  private ConsignmentProcessModel consignmentProcess;

  @Mock
  private Date lastExecutionDate;

  @Captor
  private ArgumentCaptor<MiraklGetOrdersRequest> miraklGetOrdersRequestArgumentCaptor;

  @Before
  public void setUp() throws Exception {
    when(cronJob.getLastUpdateTime()).thenReturn(LAST_EXECUTION_DATE);
    when(marketplaceConsignmentService.receiveConsignmentUpdate(any(MiraklOrder.class))).thenReturn(consignment);
    when(consignment.getConsignmentProcesses()).thenReturn(asList(consignmentProcess));
    when(consignmentProcess.getCode()).thenReturn(CONSIGNMENT_PROCESS_CODE);
    doReturn(MAX_RESULTS_BY_PAGE).when(job).getMaxResultsByPage();
    doReturn(new Date()).when(job).getMostRecentUpdateTime(any(Date.class), any(Date.class));
  }

  @Test
  public void shouldRetrieveConsignmentUpdatesWithNoPagination() {
    when(miraklFrontApi.getOrders(miraklGetOrdersRequestArgumentCaptor.capture())).thenReturn(miraklOrders);
    when(miraklOrders.getOrders()).thenReturn(getOrderUpdateMocks(MAX_RESULTS_BY_PAGE));

    job.perform(cronJob);

    MiraklGetOrdersRequest orderUpdatesRequest = miraklGetOrdersRequestArgumentCaptor.getValue();
    assertThat(orderUpdatesRequest.getMax()).isEqualTo(MAX_RESULTS_BY_PAGE);
    assertThat(orderUpdatesRequest.getStartUpdateDate()).isEqualTo(cronJob.getLastUpdateTime());
    assertThat(orderUpdatesRequest.isPaginate()).isTrue();
    verify(miraklFrontApi).getOrders(orderUpdatesRequest);
    verify(marketplaceConsignmentService, times(miraklOrders.getOrders().size()))
        .receiveConsignmentUpdate(any(MiraklOrder.class));
  }

  @Test
  public void shouldRetrieveConsignmentUpdatesWhenTotalCountIsMultipleOfMaxPerPage() {
    int multiplicityFactor = 3;
    int orderUpdatesCount = multiplicityFactor * MAX_RESULTS_BY_PAGE;
    when(miraklOrders.getTotalCount()).thenReturn((long) orderUpdatesCount);
    when(miraklFrontApi.getOrders(miraklGetOrdersRequestArgumentCaptor.capture())).thenReturn(miraklOrders);
    when(miraklOrders.getOrders()).thenReturn(getOrderUpdateMocks(MAX_RESULTS_BY_PAGE));

    job.perform(cronJob);

    MiraklGetOrdersRequest orderUpdatesRequest = miraklGetOrdersRequestArgumentCaptor.getValue();
    assertThat(orderUpdatesRequest.getMax()).isEqualTo(MAX_RESULTS_BY_PAGE);
    assertThat(orderUpdatesRequest.getStartUpdateDate()).isEqualTo(cronJob.getLastUpdateTime());
    assertThat(orderUpdatesRequest.isPaginate()).isTrue();
    verify(miraklFrontApi, times(multiplicityFactor)).getOrders(any(MiraklGetOrdersRequest.class));
    verify(marketplaceConsignmentService, times(orderUpdatesCount)).receiveConsignmentUpdate(any(MiraklOrder.class));
  }

  @Test
  public void shouldRetrieveConsignmentUpdatesWhenTotalCountIsNotMultipleOfMaxPerPage() {
    int multiplicityFactor = 3;
    int orderUpdatesCount = multiplicityFactor * MAX_RESULTS_BY_PAGE + 1;
    when(miraklOrders.getTotalCount()).thenReturn((long) orderUpdatesCount);
    when(miraklFrontApi.getOrders(miraklGetOrdersRequestArgumentCaptor.capture())).thenReturn(miraklOrders);
    when(miraklOrders.getOrders()).thenReturn(getOrderUpdateMocks(MAX_RESULTS_BY_PAGE));

    job.perform(cronJob);

    MiraklGetOrdersRequest orderUpdatesRequest = miraklGetOrdersRequestArgumentCaptor.getValue();
    assertThat(orderUpdatesRequest.getMax()).isEqualTo(MAX_RESULTS_BY_PAGE);
    assertThat(orderUpdatesRequest.getStartUpdateDate()).isEqualTo(cronJob.getLastUpdateTime());
    assertThat(orderUpdatesRequest.isPaginate()).isTrue();
    verify(miraklFrontApi, times(multiplicityFactor + 1)).getOrders(any(MiraklGetOrdersRequest.class));
    verify(marketplaceConsignmentService, atLeast(orderUpdatesCount)).receiveConsignmentUpdate(any(MiraklOrder.class));
  }


  private List<MiraklOrder> getOrderUpdateMocks(int size) {
    List<MiraklOrder> mocks = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      mocks.add(mock(MiraklOrder.class));
    }
    return mocks;
  }
}

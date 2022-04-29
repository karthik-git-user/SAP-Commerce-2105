package com.mirakl.hybris.core.customfields.jobs;

import com.mirakl.hybris.core.customfields.services.CustomFieldImportService;
import com.mirakl.hybris.core.model.MiraklCustomFieldModel;
import com.mirakl.hybris.core.model.MiraklImportCustomFieldsCronJobModel;
import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.cronjob.enums.CronJobResult;
import de.hybris.platform.cronjob.enums.CronJobStatus;
import de.hybris.platform.servicelayer.cronjob.PerformResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class MiraklImportCustomFieldsJobTest {

  @InjectMocks
  private MiraklImportCustomFieldsJob testObj;

  @Mock
  private MiraklImportCustomFieldsCronJobModel cronJob;
  @Mock
  private CustomFieldImportService customFieldsImportService;
  @Mock
  private MiraklCustomFieldModel miraklCustomFieldModel;

  @Before
  public void setUp() {
    when(customFieldsImportService.importCustomFields(null)).thenReturn(Collections.singletonList(miraklCustomFieldModel));
  }

  @Test
  public void shouldImportAllCustomFieldsByDefault() {
    PerformResult result = testObj.perform(cronJob);
    assertThat(result.getResult()).isEqualTo(CronJobResult.SUCCESS);
    assertThat(result.getStatus()).isEqualTo(CronJobStatus.FINISHED);

    verify(customFieldsImportService).importCustomFields(Collections.emptySet());
  }

}

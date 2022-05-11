package com.mirakl.hybris.core.customfields.services.impl;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.Sets;
import com.mirakl.client.mmp.domain.additionalfield.FieldPermission;
import com.mirakl.client.mmp.domain.additionalfield.MiraklAdditionalFieldLinkedEntity;
import com.mirakl.client.mmp.domain.additionalfield.MiraklAdditionalFieldType;
import com.mirakl.client.mmp.domain.additionalfield.MiraklFrontOperatorAdditionalField;
import com.mirakl.client.mmp.front.core.MiraklMarketplacePlatformFrontApi;
import com.mirakl.client.mmp.front.request.additionalfield.MiraklGetAdditionalFieldRequest;
import com.mirakl.hybris.core.customfields.daos.MiraklCustomFieldDao;
import com.mirakl.hybris.core.enums.MiraklCustomFieldLinkedEntity;
import com.mirakl.hybris.core.enums.MiraklCustomFieldPermission;
import com.mirakl.hybris.core.enums.MiraklCustomFieldType;
import com.mirakl.hybris.core.model.MiraklCustomFieldModel;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import de.hybris.platform.servicelayer.exceptions.AmbiguousIdentifierException;
import de.hybris.platform.servicelayer.model.ModelService;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class DefaultCustomFieldImportServiceTest {
  private static final String CODE = "CODE";

  private static final MiraklCustomFieldLinkedEntity MIRAKL_CUSTOM_FIELD_LINKED_ENTITY = MiraklCustomFieldLinkedEntity.SHOP;
  private static final MiraklAdditionalFieldLinkedEntity MIRAKL_ADDITIONAL_FIELD_LINKED_ENTITY_UPDATED =
      MiraklAdditionalFieldLinkedEntity.SHOP;
  private static final MiraklCustomFieldPermission MIRAKL_CUSTOM_FIELD_SHOP_PERMISSION = MiraklCustomFieldPermission.READ_WRITE;
  private static final FieldPermission FIELD_PERMISSION_UPDATED = FieldPermission.READ_ONLY;
  private static final MiraklCustomFieldType MIRAKL_CUSTOM_FIELD_TYPE = MiraklCustomFieldType.LIST;
  private static final MiraklAdditionalFieldType MIRAKL_ADDITIONAL_FIELD_TYPE_UPDATED = MiraklAdditionalFieldType.REGEX;
  private static final String LABEL = "label", LABEL_UPDATED = "label-updated";
  private static final String ACCEPTED_VALUE = "ACCEPTED_VALUE", REGEX = "REGEX";

  @InjectMocks
  @Spy
  private DefaultCustomFieldImportService testObj;

  @Mock
  private ModelService modelService;
  @Mock
  private MiraklMarketplacePlatformFrontApi mmpFrontApi;
  @Mock
  private MiraklCustomFieldDao miraklCustomFieldsDao;
  @Mock
  private MiraklCustomFieldModel existingCustomField, existingCustomField2, newCustomField;
  @Mock
  private MiraklFrontOperatorAdditionalField apiCustomField, apiCustomField2;
  @Mock
  protected Converter<MiraklFrontOperatorAdditionalField, MiraklCustomFieldModel> miraklCustomFieldConverter;

  @Captor
  private ArgumentCaptor<MiraklGetAdditionalFieldRequest> requestArgumentCaptor;

  private List<MiraklFrontOperatorAdditionalField> apiCustomFields = new ArrayList<>();

  @Before
  public void setUp() throws IOException {
    apiCustomFields.add(apiCustomField);
    apiCustomFields.add(apiCustomField2);

    when(miraklCustomFieldsDao.findCustomFieldByCodeAndEntity(CODE, MIRAKL_CUSTOM_FIELD_LINKED_ENTITY))
        .thenReturn(existingCustomField);

    when(existingCustomField.getCode()).thenReturn(CODE);
    when(existingCustomField.getShopPermission()).thenReturn(MIRAKL_CUSTOM_FIELD_SHOP_PERMISSION);
    when(existingCustomField.getLabel()).thenReturn(LABEL);
    when(existingCustomField.getType()).thenReturn(MIRAKL_CUSTOM_FIELD_TYPE);
    when(existingCustomField.isRequired()).thenReturn(true);
    when(existingCustomField.getEntity()).thenReturn(MIRAKL_CUSTOM_FIELD_LINKED_ENTITY);
    when(existingCustomField.getAcceptedValues()).thenReturn(Collections.singletonList(ACCEPTED_VALUE));

    when(apiCustomField.getCode()).thenReturn(CODE);
    when(apiCustomField.getShopPermission()).thenReturn(FIELD_PERMISSION_UPDATED);
    when(apiCustomField.getLabel()).thenReturn(LABEL_UPDATED);
    when(apiCustomField.getType()).thenReturn(MIRAKL_ADDITIONAL_FIELD_TYPE_UPDATED);
    when(apiCustomField.getEntity()).thenReturn(MIRAKL_ADDITIONAL_FIELD_LINKED_ENTITY_UPDATED);
    when(apiCustomField.isRequired()).thenReturn(false);
    when(apiCustomField.getAcceptedValues()).thenReturn(Collections.emptyList());
    when(apiCustomField.getRegex()).thenReturn(REGEX);
    when(mmpFrontApi.getAdditionalFields(requestArgumentCaptor.capture())).thenReturn(Collections.singletonList(apiCustomField));

    when(miraklCustomFieldConverter.convert(apiCustomField)).thenReturn(newCustomField);
    when(miraklCustomFieldConverter.convert(apiCustomField, existingCustomField)).thenReturn(existingCustomField);
  }

  @Test
  public void importCustomFieldsWithDefaultRequestWithFallback() {

    Collection<MiraklCustomFieldModel> miraklCustomFieldModels = testObj.importAllCustomFields();

    MiraklGetAdditionalFieldRequest request = requestArgumentCaptor.getValue();
    assertThat(request.getQueryParams()).hasSize(0);
    assertThat(miraklCustomFieldModels).containsOnly(existingCustomField);
    verify(testObj).importCustomFields(Collections.emptySet());
  }

  @Test
  public void importCustomFieldsWithDefaultRequest() {

    Collection<MiraklCustomFieldModel> miraklCustomFieldModels = testObj.importAllCustomFields();

    MiraklGetAdditionalFieldRequest request = requestArgumentCaptor.getValue();
    assertThat(request.getQueryParams()).hasSize(0);
    assertThat(miraklCustomFieldModels).containsOnly(existingCustomField);
  }

  @Test
  public void importCustomFieldsWithSpecificEntityRequest() {

    testObj.importCustomFields(Sets.newHashSet(MiraklCustomFieldLinkedEntity.SHOP));

    MiraklGetAdditionalFieldRequest request = requestArgumentCaptor.getValue();
    Map<String, String> queryParams = request.getQueryParams();
    assertThat(queryParams).hasSize(1);
    assertThat(queryParams).hasSize(1);
    assertThat(queryParams.get("entities")).isEqualTo(MIRAKL_ADDITIONAL_FIELD_LINKED_ENTITY_UPDATED.toString());
  }

  @Test
  public void importApiCustomFieldsShouldImportAllCustomFields() {
    doReturn(existingCustomField).when(testObj).getCustomField(apiCustomField);
    doReturn(existingCustomField2).when(testObj).getCustomField(apiCustomField2);

    Collection<MiraklCustomFieldModel> miraklCustomFieldModels = testObj.importApiCustomFields(apiCustomFields);

    verify(testObj).getCustomField(apiCustomField);
    verify(testObj).getCustomField(apiCustomField2);

    assertThat(miraklCustomFieldModels).containsOnly(existingCustomField, existingCustomField2);
  }

  @Test
  public void updateCustomField() {
    MiraklCustomFieldModel customField = testObj.getCustomField(apiCustomField);

    assertThat(customField).isEqualTo(existingCustomField);
  }

  @Test
  public void createCustomField() {
    when(miraklCustomFieldsDao.findCustomFieldByCodeAndEntity(CODE, MIRAKL_CUSTOM_FIELD_LINKED_ENTITY)).thenReturn(null);
    MiraklCustomFieldModel customField = testObj.getCustomField(apiCustomField);

    assertThat(customField).isEqualTo(newCustomField);
  }

  @Test
  public void createCustomFieldForShouldNotThrowExceptionOnDuplicatedCustomField() {
    doThrow(AmbiguousIdentifierException.class).when(miraklCustomFieldsDao).findCustomFieldByCodeAndEntity(CODE,
        MIRAKL_CUSTOM_FIELD_LINKED_ENTITY);

    MiraklCustomFieldModel customField = testObj.getCustomField(apiCustomField);

    assertThat(customField).isNull();
  }

}

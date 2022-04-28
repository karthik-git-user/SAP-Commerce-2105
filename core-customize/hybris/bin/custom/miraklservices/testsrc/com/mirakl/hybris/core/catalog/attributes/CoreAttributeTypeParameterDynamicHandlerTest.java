package com.mirakl.hybris.core.catalog.attributes;

import static com.mirakl.hybris.core.constants.MiraklservicesConstants.CATALOG_EXPORT_DATE_FORMAT;
import static com.mirakl.hybris.core.constants.MiraklservicesConstants.CATALOG_EXPORT_DECIMAL_PRECISION;
import static com.mirakl.hybris.core.constants.MiraklservicesConstants.CATALOG_EXPORT_MEDIA_SIZE;
import static com.mirakl.hybris.core.enums.MiraklAttributeType.DATE;
import static com.mirakl.hybris.core.enums.MiraklAttributeType.DECIMAL;
import static com.mirakl.hybris.core.enums.MiraklAttributeType.LIST;
import static com.mirakl.hybris.core.enums.MiraklAttributeType.LONG_TEXT;
import static com.mirakl.hybris.core.enums.MiraklAttributeType.MEDIA;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.mirakl.hybris.core.catalog.strategies.ValueListNamingStrategy;
import com.mirakl.hybris.core.model.MiraklCoreAttributeModel;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.servicelayer.config.ConfigurationService;

/**
 * Copyright (C) 2017 Mirakl. www.mirakl.com - info@mirakl.com All Rights Reserved. Tous droits réservés.
 */
@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class CoreAttributeTypeParameterDynamicHandlerTest {

  private static final String DEFAULT_TYPE_PARAMETER = "default";
  private static final String CUSTOM_TYPE_PARAMETER = "custom";
  private static final String DEFAULT_DATE_FORMAT = "dd-MM-yyyy";
  private static final String DEFAULT_DECIMAL_PRECISION = "4";
  private static final String DEFAULT_MEDIA_SIZE = "10240";
  private static final String DEFAULT_VALUE_LIST_NAME = "value-list-name";
  private static final String USER_LONG_TEXT_PARAMETER = "long text parameter";

  @Mock
  private MiraklCoreAttributeModel coreAttribute;
  @Mock
  private ConfigurationService configurationService;
  @Mock
  private ValueListNamingStrategy valueListNamingStrategy;
  @Mock
  private Configuration configuration;

  @InjectMocks
  private CoreAttributeTypeParameterDynamicHandler testObj;

  @Before
  public void setUp() throws Exception {
    when(configurationService.getConfiguration()).thenReturn(configuration);
    when(valueListNamingStrategy.getCode(coreAttribute)).thenReturn(DEFAULT_VALUE_LIST_NAME);
    when(configuration.getString(CATALOG_EXPORT_DATE_FORMAT)).thenReturn(DEFAULT_DATE_FORMAT);
    when(configuration.getString(CATALOG_EXPORT_DECIMAL_PRECISION)).thenReturn(DEFAULT_DECIMAL_PRECISION);
    when(configuration.getString(CATALOG_EXPORT_MEDIA_SIZE)).thenReturn(DEFAULT_MEDIA_SIZE);
  }

  @Test
  public void get() throws Exception {
    when(coreAttribute.getType()).thenReturn(LONG_TEXT);
    when(coreAttribute.getTypeParameter()).thenReturn(USER_LONG_TEXT_PARAMETER);

    String output = testObj.get(coreAttribute);

    assertThat(output).isEqualTo(USER_LONG_TEXT_PARAMETER);
  }

  @Test
  public void getWhenNoParameter() throws Exception {
    when(coreAttribute.getType()).thenReturn(LONG_TEXT);

    String output = testObj.get(coreAttribute);

    assertThat(output).isNull();
  }


  @Test
  public void getDefaultDecimalValue() throws Exception {
    when(coreAttribute.getType()).thenReturn(DECIMAL);
    when(coreAttribute.getTypeParameter()).thenReturn("");

    String output = testObj.get(coreAttribute);

    assertThat(output).isEqualTo(DEFAULT_DECIMAL_PRECISION);
  }

  @Test
  public void getDefaultMediaValue() throws Exception {
    when(coreAttribute.getType()).thenReturn(MEDIA);

    String output = testObj.get(coreAttribute);

    assertThat(output).isEqualTo(DEFAULT_MEDIA_SIZE);
  }

  @Test
  public void getDefaultDateValue() throws Exception {
    when(coreAttribute.getType()).thenReturn(DATE);
    when(coreAttribute.getTypeParameter()).thenReturn("");

    String output = testObj.get(coreAttribute);

    assertThat(output).isEqualTo(DEFAULT_DATE_FORMAT);
  }

  @Test
  public void getDefaultListValue() throws Exception {
    when(coreAttribute.getType()).thenReturn(LIST);

    String output = testObj.get(coreAttribute);

    assertThat(output).isEqualTo(DEFAULT_VALUE_LIST_NAME);
  }

  @Test
  public void getTypeParameter() throws Exception {
    when(coreAttribute.getTypeParameter()).thenReturn(CUSTOM_TYPE_PARAMETER);
    when(coreAttribute.getType()).thenReturn(DATE);

    String output = testObj.get(coreAttribute);

    assertThat(output).isEqualTo(CUSTOM_TYPE_PARAMETER);
  }


}

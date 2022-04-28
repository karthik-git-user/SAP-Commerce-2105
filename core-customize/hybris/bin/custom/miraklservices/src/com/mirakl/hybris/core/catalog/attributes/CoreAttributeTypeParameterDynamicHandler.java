package com.mirakl.hybris.core.catalog.attributes;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.mirakl.hybris.core.constants.MiraklservicesConstants.BOOLEAN_VALUE_LIST_ID;
import static com.mirakl.hybris.core.constants.MiraklservicesConstants.CATALOG_EXPORT_DATE_FORMAT;
import static com.mirakl.hybris.core.constants.MiraklservicesConstants.CATALOG_EXPORT_DECIMAL_PRECISION;
import static com.mirakl.hybris.core.constants.MiraklservicesConstants.CATALOG_EXPORT_MEDIA_SIZE;

import org.springframework.beans.factory.annotation.Required;

import com.mirakl.hybris.core.catalog.strategies.ValueListNamingStrategy;
import com.mirakl.hybris.core.model.MiraklCoreAttributeModel;

import de.hybris.platform.servicelayer.config.ConfigurationService;
import de.hybris.platform.servicelayer.model.attribute.AbstractDynamicAttributeHandler;

/**
 * Copyright (C) 2017 Mirakl. www.mirakl.com - info@mirakl.com All Rights Reserved. Tous droits réservés.
 */
public class CoreAttributeTypeParameterDynamicHandler extends AbstractDynamicAttributeHandler<String, MiraklCoreAttributeModel> {

  protected ConfigurationService configurationService;
  protected ValueListNamingStrategy valueListNamingStrategy;

  @Override
  public String get(MiraklCoreAttributeModel coreAttribute) {
    String defaultValue = null;
    if (coreAttribute.getType() != null) {
      switch (coreAttribute.getType()) {
        case BOOLEAN:
          defaultValue = BOOLEAN_VALUE_LIST_ID;
          break;
        case DATE:
          defaultValue = configurationService.getConfiguration().getString(CATALOG_EXPORT_DATE_FORMAT);
          break;
        case DECIMAL:
          defaultValue = configurationService.getConfiguration().getString(CATALOG_EXPORT_DECIMAL_PRECISION);
          break;
        case MEDIA:
          defaultValue = configurationService.getConfiguration().getString(CATALOG_EXPORT_MEDIA_SIZE);
          break;
        case LIST:
        case LIST_MULTIPLE_VALUES:
          defaultValue = valueListNamingStrategy.getCode(coreAttribute);
          break;
        default:
          defaultValue = null;
      }
    }
    return isNullOrEmpty(coreAttribute.getTypeParameter()) ? defaultValue : coreAttribute.getTypeParameter();
  }

  @Required
  public void setConfigurationService(ConfigurationService configurationService) {
    this.configurationService = configurationService;
  }

  @Required
  public void setValueListNamingStrategy(ValueListNamingStrategy valueListNamingStrategy) {
    this.valueListNamingStrategy = valueListNamingStrategy;
  }
}

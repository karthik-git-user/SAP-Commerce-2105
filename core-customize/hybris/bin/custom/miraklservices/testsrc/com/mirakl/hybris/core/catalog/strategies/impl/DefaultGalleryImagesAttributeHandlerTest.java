package com.mirakl.hybris.core.catalog.strategies.impl;

import static java.lang.String.format;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.Lists;
import com.mirakl.hybris.beans.AttributeValueData;
import com.mirakl.hybris.beans.ProductImportData;
import com.mirakl.hybris.beans.ProductImportFileContextData;
import com.mirakl.hybris.beans.ProductImportGlobalContextData;
import com.mirakl.hybris.core.catalog.strategies.CoreAttributeOwnerStrategy;
import com.mirakl.hybris.core.model.MiraklCoreAttributeModel;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.PK;
import de.hybris.platform.core.model.media.MediaContainerModel;
import de.hybris.platform.core.model.media.MediaModel;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.servicelayer.media.MediaService;
import de.hybris.platform.servicelayer.model.ModelService;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class DefaultGalleryImagesAttributeHandlerTest {

  private static final String IMAGE_URL = "image-url";
  private static final String MEDIA_1_ATTRIBUTE = "media1";
  private static final PK PRODUCT_CATALOG_VERSION_PK = PK.fromLong(1L);

  @Mock
  private CoreAttributeOwnerStrategy coreAttributeOwnerStrategy;
  @Mock
  private MediaService mediaService;
  @Mock
  private ModelService modelService;
  @Mock
  private AttributeValueData attributeValue;
  @Mock
  private ProductImportData data;
  @Mock
  private ProductImportFileContextData context;
  @Mock
  private ProductImportGlobalContextData globalContext;
  @Mock
  private MiraklCoreAttributeModel coreAttribute;
  @Mock
  private MediaModel media;
  @Mock
  private MediaContainerModel mediaContainer, existingMediaContainer;
  private ProductModel product;

  @InjectMocks
  @Spy
  private DefaultGalleryImagesAttributeHandler defaultGalleryImagesValueHandler;

  @Before
  public void setUp() throws Exception {
    product = new ProductModel();
    when(coreAttribute.getCode()).thenReturn(MEDIA_1_ATTRIBUTE);
    when(attributeValue.getCode()).thenReturn(MEDIA_1_ATTRIBUTE);
    when(attributeValue.getValue()).thenReturn(IMAGE_URL);
    when(attributeValue.getCoreAttribute()).thenReturn(coreAttribute);
    when(coreAttributeOwnerStrategy.determineOwner(coreAttribute, data, context)).thenReturn(product);
    when(modelService.create(MediaModel.class)).thenReturn(media);
    when(context.getGlobalContext()).thenReturn(globalContext);
    when(globalContext.getProductCatalogVersion()).thenReturn(PRODUCT_CATALOG_VERSION_PK);
    when(modelService.create(MediaContainerModel.class)).thenReturn(mediaContainer);
    doNothing().when(defaultGalleryImagesValueHandler).downloadMedia(eq(IMAGE_URL), any(MediaModel.class), eq(data), eq(context));
  }

  @Test
  public void shouldCreateGalleryImage() throws Exception {
    defaultGalleryImagesValueHandler.setValue(attributeValue, data, context);

    assertThat(product.getGalleryImages()).isNotEmpty();
  }

  @Test
  public void shouldDoNothingWhenNoMediaUrl() throws Exception {
    when(attributeValue.getValue()).thenReturn(null);

    defaultGalleryImagesValueHandler.setValue(attributeValue, data, context);

    assertThat(product.getGalleryImages()).isNull();
  }

  @Test
  public void shouldReplaceExistingGalleryImage() throws Exception {
    when(existingMediaContainer.getQualifier()).thenReturn(format("%s_%s", product.getCode(), MEDIA_1_ATTRIBUTE));
    product.setGalleryImages(Lists.newArrayList(existingMediaContainer));

    defaultGalleryImagesValueHandler.setValue(attributeValue, data, context);

    assertThat(product.getGalleryImages()).containsOnly(mediaContainer);
  }


  @Test
  public void shouldNotDownloadMediaWhenSameMasterUrl() throws Exception {
    when(existingMediaContainer.getMasterUrl()).thenReturn(IMAGE_URL);
    product.setGalleryImages(Lists.newArrayList(existingMediaContainer));

    defaultGalleryImagesValueHandler.setValue(attributeValue, data, context);

    assertThat(product.getGalleryImages()).containsOnly(existingMediaContainer);
  }
}

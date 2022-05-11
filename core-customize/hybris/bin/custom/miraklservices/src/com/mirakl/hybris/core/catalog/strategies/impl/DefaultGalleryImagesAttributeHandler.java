package com.mirakl.hybris.core.catalog.strategies.impl;

import static com.google.common.collect.Sets.newHashSet;
import static com.mirakl.hybris.core.constants.MiraklservicesConstants.MEDIA_URL_SECURE;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections.MapUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.mirakl.hybris.beans.AttributeValueData;
import com.mirakl.hybris.beans.ProductImportData;
import com.mirakl.hybris.beans.ProductImportFileContextData;
import com.mirakl.hybris.core.constants.MiraklservicesConstants;
import com.mirakl.hybris.core.model.MiraklCoreAttributeModel;
import com.mirakl.hybris.core.product.exceptions.ProductImportException;

import de.hybris.platform.acceleratorservices.urlresolver.SiteBaseUrlResolutionService;
import de.hybris.platform.catalog.model.CatalogVersionModel;
import de.hybris.platform.core.model.media.MediaContainerModel;
import de.hybris.platform.core.model.media.MediaModel;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.servicelayer.config.ConfigurationService;
import de.hybris.platform.servicelayer.i18n.L10NService;
import de.hybris.platform.servicelayer.media.MediaService;

public class DefaultGalleryImagesAttributeHandler extends AbstractCoreAttributeHandler<MiraklCoreAttributeModel> {

  private static final Logger LOG = Logger.getLogger(DefaultGalleryImagesAttributeHandler.class);

  protected MediaService mediaService;
  protected L10NService l10nService;
  protected SiteBaseUrlResolutionService siteBaseUrlResolutionService;
  protected ConfigurationService configurationService;
  protected boolean secureMediaUrl;

  @Override
  public void setValue(AttributeValueData attributeValue, ProductImportData data, ProductImportFileContextData context)
      throws ProductImportException {
    MiraklCoreAttributeModel coreAttribute = attributeValue.getCoreAttribute();
    String mediaUrl = attributeValue.getValue();

    if (isBlank(mediaUrl)) {
      return;
    }

    ProductModel product = determineOwner(coreAttribute, data, context);
    markItemsToSave(data, product);

    List<MediaContainerModel> galleryImages = new ArrayList<>();
    if (product.getGalleryImages() != null) {
      galleryImages.addAll(product.getGalleryImages());
    }

    if (isAlreadyReceivedGalleryImage(galleryImages, mediaUrl, coreAttribute, context)) {
      if (LOG.isDebugEnabled()) {
        LOG.debug(format("Already received a gallery image with URL [%s] for product [%s]. Won't reimport it.", mediaUrl,
            product.getCode()));
      }
      return;
    }

    final String containerId = getMediaContainerQualifier(mediaUrl, product, coreAttribute, data, context);
    MediaContainerModel existingContainer = findExistingContainer(galleryImages, containerId);
    if (existingContainer != null) {
      removeMediaContainer(existingContainer, galleryImages);
    }

    CatalogVersionModel productCatalogVersion = modelService.get(context.getGlobalContext().getProductCatalogVersion());

    MediaModel media = modelService.create(MediaModel.class);
    media.setCode(getMediaCode(mediaUrl, product, coreAttribute, data, context));
    media.setCatalogVersion(productCatalogVersion);
    media.setURL(mediaUrl);
    modelService.save(media);
    downloadMedia(mediaUrl, media, data, context);

    MediaContainerModel container = modelService.create(MediaContainerModel.class);
    container.setQualifier(containerId);
    container.setCatalogVersion(productCatalogVersion);
    container.setMedias(newHashSet(media));
    container.setMasterUrl(mediaUrl);
    container.setFromMarketplace(true);

    galleryImages.add(container);
    modelService.save(container);

    product.setGalleryImages(galleryImages);
    markItemsToSave(data, product);
  }

  protected void downloadMedia(String mediaUrl, MediaModel media, ProductImportData data, ProductImportFileContextData context)
      throws ProductImportException {
    try {
      URLConnection urlConnection = new URL(mediaUrl).openConnection();
      configureRequestHeaders(urlConnection, context);
      mediaService.setStreamForMedia(media, urlConnection.getInputStream());
    } catch (Exception e) {
      LOG.error(format("Unable to set stream for media [%s] from url [%s]", media.getCode(), mediaUrl), e);
      throw new ProductImportException(data.getRawProduct(), l10nService
          .getLocalizedString(MiraklservicesConstants.PRODUCTS_IMPORT_MEDIA_DOWNLOAD_FAILURE_MESSAGE, new Object[] {mediaUrl}));
    }
  }

  protected void configureRequestHeaders(URLConnection urlConnection, ProductImportFileContextData context) {
    Map<String, String> mediaDownloadHttpHeaders = context.getGlobalContext().getMediaDownloadHttpHeaders();
    if (MapUtils.isNotEmpty(mediaDownloadHttpHeaders)) {
      for (Entry<String, String> entry : mediaDownloadHttpHeaders.entrySet()) {
        urlConnection.setRequestProperty(entry.getKey(), entry.getValue());
      }
    }
  }

  protected boolean isAlreadyReceivedGalleryImage(List<MediaContainerModel> galleryImages, final String mediaUrl,
      final MiraklCoreAttributeModel coreAttribute, final ProductImportFileContextData context) {
    return FluentIterable.from(galleryImages).anyMatch(new Predicate<MediaContainerModel>() {

      @Override
      public boolean apply(final MediaContainerModel mediaContainer) {
        return mediaUrl.equalsIgnoreCase(mediaContainer.getMasterUrl());
      }
    });
  }

  protected void removeMediaContainer(MediaContainerModel existingContainer, List<MediaContainerModel> galleryImages) {
    galleryImages.remove(existingContainer);
    modelService.removeAll(existingContainer.getMedias());
    modelService.remove(existingContainer);
  }

  protected MediaContainerModel findExistingContainer(List<MediaContainerModel> galleryImages, final String containerId) {
    return FluentIterable.from(galleryImages).firstMatch(new Predicate<MediaContainerModel>() {

      @Override
      public boolean apply(MediaContainerModel container) {
        return containerId.equals(container.getQualifier());
      }
    }).orNull();
  }

  protected String getMediaCode(String mediaUrl, ProductModel product, MiraklCoreAttributeModel coreAttribute,
      ProductImportData data, ProductImportFileContextData context) {
    return format("%s_%s", product.getCode(), coreAttribute.getCode());
  }

  protected String getMediaContainerQualifier(String mediaUrl, ProductModel product, MiraklCoreAttributeModel coreAttribute,
      ProductImportData data, ProductImportFileContextData context) {
    return format("%s_%s", product.getCode(), coreAttribute.getCode());
  }

  public boolean isSecureMediaUrl() {
    return configurationService.getConfiguration().getBoolean(MEDIA_URL_SECURE, true);
  }

  @Required
  public void setConfigurationService(ConfigurationService configurationService) {
    this.configurationService = configurationService;
  }

  @Required
  public void setMediaService(MediaService mediaService) {
    this.mediaService = mediaService;
  }

  @Required
  public void setL10nService(L10NService l10nService) {
    this.l10nService = l10nService;
  }

  @Required
  public void setSiteBaseUrlResolutionService(SiteBaseUrlResolutionService siteBaseUrlResolutionService) {
    this.siteBaseUrlResolutionService = siteBaseUrlResolutionService;
  }

}

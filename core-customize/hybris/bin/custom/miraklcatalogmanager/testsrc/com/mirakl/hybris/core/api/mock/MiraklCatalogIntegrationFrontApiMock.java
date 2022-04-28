package com.mirakl.hybris.core.api.mock;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.mirakl.client.core.security.MiraklCredential;
import com.mirakl.client.mci.domain.product.MiraklProductDataSheetSyncTracking;
import com.mirakl.client.mci.front.core.MiraklCatalogIntegrationFrontApiClient;
import com.mirakl.client.mci.request.product.MiraklProductDataSheetSyncRequest;


public class MiraklCatalogIntegrationFrontApiMock extends MiraklCatalogIntegrationFrontApiClient {

  protected List<File> files;

  public MiraklCatalogIntegrationFrontApiMock(String endpoint, MiraklCredential credential) {
    super(endpoint, credential);
    files = new ArrayList<>();
  }

  @Override
  public MiraklProductDataSheetSyncTracking synchronizeProductDataSheets(MiraklProductDataSheetSyncRequest request) {
    files.add(request.getFile());
    return new MiraklProductDataSheetSyncTracking();
  }

  public List<File> getFiles() {
    return files;
  }

  public void clearFiles() {
    files = new ArrayList<>();
  }

}

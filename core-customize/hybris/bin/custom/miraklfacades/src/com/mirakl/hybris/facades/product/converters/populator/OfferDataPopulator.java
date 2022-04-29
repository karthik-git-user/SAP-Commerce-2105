package com.mirakl.hybris.facades.product.converters.populator;

import static com.mirakl.hybris.core.util.OpenDateRange.dateRange;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Required;

import com.mirakl.client.mmp.domain.offer.price.MiraklVolumePrice;
import com.mirakl.hybris.beans.OfferData;
import com.mirakl.hybris.core.model.OfferModel;
import com.mirakl.hybris.core.product.services.MiraklPriceService;
import com.mirakl.hybris.core.product.strategies.OfferCodeGenerationStrategy;

import de.hybris.platform.commercefacades.product.PriceDataFactory;
import de.hybris.platform.commercefacades.product.data.PriceData;
import de.hybris.platform.commercefacades.product.data.PriceDataType;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.enumeration.EnumerationService;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;

public class OfferDataPopulator implements Populator<OfferModel, OfferData> {

  protected EnumerationService enumerationService;
  protected PriceDataFactory priceDataFactory;
  protected OfferCodeGenerationStrategy offerCodeGenerationStrategy;
  protected MiraklPriceService miraklPriceService;
  protected Comparator<PriceData> volumePriceComparator;

  @Override
  public void populate(OfferModel offerModel, OfferData offerData) throws ConversionException {

    BigDecimal effectiveDiscountPrice = offerModel.getEffectiveDiscountPrice();
    if (effectiveDiscountPrice != null) {
      offerData.setDiscountPrice(priceDataFactory.create(PriceDataType.BUY, effectiveDiscountPrice, offerModel.getCurrency()));
    }
    if (offerModel.getMinShippingPrice() != null) {
      offerData.setMinShippingPrice(
          priceDataFactory.create(PriceDataType.BUY, offerModel.getMinShippingPrice(), offerModel.getCurrency()));
    }
    if (offerModel.getMinShippingPriceAdditional() != null) {
      offerData.setMinShippingPriceAdditional(
          priceDataFactory.create(PriceDataType.BUY, offerModel.getMinShippingPriceAdditional(), offerModel.getCurrency()));
    }
    BigDecimal effectiveOriginPrice = offerModel.getEffectiveOriginPrice();
    if (effectiveOriginPrice != null) {
      offerData.setOriginPrice(priceDataFactory.create(PriceDataType.BUY, effectiveOriginPrice, offerModel.getCurrency()));
    }
    offerData.setPrice(priceDataFactory.create(PriceDataType.BUY, offerModel.getEffectiveBasePrice(), offerModel.getCurrency()));
    offerData.setTotalPrice(priceDataFactory.create(PriceDataType.BUY, offerModel.getTotalPrice(), offerModel.getCurrency()));
    offerData.setAvailableEndDate(offerModel.getAvailableEndDate());
    offerData.setAvailableStartDate(offerModel.getAvailableStartDate());
    offerData.setCode(offerCodeGenerationStrategy.generateCode(offerModel.getId()));
    offerData.setDescription(offerModel.getDescription());
    offerData.setDiscountEndDate(offerModel.getDiscountEndDate());
    offerData.setDiscountStartDate(offerModel.getDiscountStartDate());
    offerData.setId(offerModel.getId());
    offerData.setLeadTimeToShip(offerModel.getLeadTimeToShip());
    if (isInDiscount(offerModel)) {
      offerData.setPriceAdditionalInfo(offerModel.getPriceAdditionalInfo());
    }
    offerData.setProductCode(offerModel.getProductCode());
    offerData.setQuantity(offerModel.getQuantity());
    offerData.setShopCode(offerModel.getShop().getId());
    offerData.setShopName(offerModel.getShop().getName());
    offerData.setShopGrade(offerModel.getShop().getGrade());
    offerData.setShopEvaluationCount(offerModel.getShop().getEvaluationCount());
    offerData.setState(enumerationService.getEnumerationName(offerModel.getState()));
    offerData.setMaxOrderQuantity(offerModel.getMaxOrderQuantity());
    offerData.setMinOrderQuantity(offerModel.getMinOrderQuantity());
    offerData.setPackageQuantity(offerModel.getPackageQuantity());
    populateVolumePrices(offerModel, offerData);
  }

  protected void populateVolumePrices(OfferModel offerModel, OfferData offerData) {
    List<MiraklVolumePrice> volumePrices = miraklPriceService.getVolumePrices(offerModel);
    if (volumePrices.size() < 2) {
      return;
    }

    List<PriceData> volumePricesDatas = new ArrayList<PriceData>();
    List<PriceData> originVolumePricesDatas = new ArrayList<PriceData>();
    for (MiraklVolumePrice volumePrice : volumePrices) {
      PriceData price = priceDataFactory.create(PriceDataType.BUY, volumePrice.getPrice(), offerModel.getCurrency());
      price.setMinQuantity(volumePrice.getQuantityThreshold().longValue());
      volumePricesDatas.add(price);

      BigDecimal originPriceValue =
          volumePrice.getUnitOriginPrice() == null ? offerModel.getEffectiveOriginPrice() : volumePrice.getUnitOriginPrice();
      PriceData originPrice = priceDataFactory.create(PriceDataType.BUY, originPriceValue, offerModel.getCurrency());
      originPrice.setMinQuantity(volumePrice.getQuantityThreshold().longValue());
      originVolumePricesDatas.add(originPrice);
    }

    sortAndPopulateMaxQty(volumePricesDatas);
    sortAndPopulateMaxQty(originVolumePricesDatas);
    offerData.setVolumePrices(volumePricesDatas);
    offerData.setVolumeOriginPrices(originVolumePricesDatas);
  }

  protected void sortAndPopulateMaxQty(List<PriceData> volumePricesDatas) {
    Collections.sort(volumePricesDatas, volumePriceComparator);
    for (int i = 0; i < volumePricesDatas.size() - 1; i++) {
      volumePricesDatas.get(i).setMaxQuantity(Long.valueOf(volumePricesDatas.get(i + 1).getMinQuantity().longValue() - 1));

    }
  }

  protected boolean isInDiscount(OfferModel offer) {
    return dateRange(offer.getDiscountStartDate(), offer.getDiscountEndDate()).encloses(new Date());
  }

  @Required
  public void setEnumerationService(EnumerationService enumerationService) {
    this.enumerationService = enumerationService;
  }

  @Required
  public void setPriceDataFactory(PriceDataFactory priceDataFactory) {
    this.priceDataFactory = priceDataFactory;
  }

  @Required
  public void setOfferCodeGenerationStrategy(OfferCodeGenerationStrategy offerCodeGenerationStrategy) {
    this.offerCodeGenerationStrategy = offerCodeGenerationStrategy;
  }

  @Required
  public void setMiraklPriceService(MiraklPriceService miraklPriceService) {
    this.miraklPriceService = miraklPriceService;
  }

  @Required
  public void setVolumePriceComparator(Comparator<PriceData> volumePriceComparator) {
    this.volumePriceComparator = volumePriceComparator;
  }

}

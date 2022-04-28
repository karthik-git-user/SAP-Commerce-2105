package com.mirakl.hybris.channels.channel.services.impl;

import static com.mirakl.hybris.channels.constants.MiraklchannelsConstants.MIRAKL_CHANNELS_ENABLED;
import static com.mirakl.hybris.channels.constants.MiraklchannelsConstants.MIRAKL_CHANNEL_SESSION_ATTRIBUTE;
import static org.apache.commons.collections.CollectionUtils.isEmpty;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Required;

import com.mirakl.hybris.channels.channel.daos.MiraklChannelDao;
import com.mirakl.hybris.channels.channel.services.MiraklChannelService;
import com.mirakl.hybris.channels.model.MiraklChannelModel;

import de.hybris.platform.servicelayer.config.ConfigurationService;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.session.SessionService;

public class DefaultMiraklChannelService implements MiraklChannelService {

  protected ModelService modelService;
  protected SessionService sessionService;
  protected ConfigurationService configurationService;
  protected MiraklChannelDao miraklChannelDao;

  @Override
  public MiraklChannelModel getMiraklChannelForCode(String code) {
    List<MiraklChannelModel> channels = miraklChannelDao.find(Collections.singletonMap(MiraklChannelModel.CODE, code));

    if (isEmpty(channels)) {
      return null;
    }

    return channels.get(0);
  }

  @Override
  public MiraklChannelModel createMiraklChannel(String code, String label) {
    MiraklChannelModel channel = modelService.create(MiraklChannelModel.class);
    channel.setCode(code);
    channel.setLabel(label);
    modelService.save(channel);

    return channel;
  }

  @Override
  public MiraklChannelModel getCurrentMiraklChannel() {
    return (MiraklChannelModel) sessionService.getAttribute(MIRAKL_CHANNEL_SESSION_ATTRIBUTE);
  }

  @Override
  public void setCurrentMiraklChannel(MiraklChannelModel miraklChannel) {
    sessionService.setAttribute(MIRAKL_CHANNEL_SESSION_ATTRIBUTE, miraklChannel);
  }

  @Override
  public boolean isMiraklChannelsEnabled() {
    return configurationService.getConfiguration().getBoolean(MIRAKL_CHANNELS_ENABLED, false);
  }

  @Required
  public void setModelService(ModelService modelService) {
    this.modelService = modelService;
  }

  @Required
  public void setSessionService(SessionService sessionService) {
    this.sessionService = sessionService;
  }

  @Required
  public void setConfigurationService(ConfigurationService configurationService) {
    this.configurationService = configurationService;
  }

  @Required
  public void setMiraklChannelDao(MiraklChannelDao miraklChannelDao) {
    this.miraklChannelDao = miraklChannelDao;
  }

}

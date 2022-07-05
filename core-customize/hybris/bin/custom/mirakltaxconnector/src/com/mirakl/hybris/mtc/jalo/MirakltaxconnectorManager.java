package com.mirakl.hybris.mtc.jalo;

import org.apache.log4j.Logger;

import com.mirakl.hybris.mtc.constants.MirakltaxconnectorConstants;

import de.hybris.platform.jalo.JaloSession;
import de.hybris.platform.jalo.extension.ExtensionManager;

@SuppressWarnings("PMD")
public class MirakltaxconnectorManager extends GeneratedMirakltaxconnectorManager {
  @SuppressWarnings("unused")
  private static final Logger log = Logger.getLogger(MirakltaxconnectorManager.class.getName());

  public static final MirakltaxconnectorManager getInstance() {
    ExtensionManager em = JaloSession.getCurrentSession().getExtensionManager();
    return (MirakltaxconnectorManager) em.getExtension(MirakltaxconnectorConstants.EXTENSIONNAME);
  }

}

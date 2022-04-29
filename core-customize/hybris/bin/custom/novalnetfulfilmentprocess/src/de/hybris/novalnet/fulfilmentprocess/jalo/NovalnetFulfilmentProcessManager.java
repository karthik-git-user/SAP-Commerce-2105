/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.novalnet.fulfilmentprocess.jalo;

import de.hybris.platform.jalo.JaloSession;
import de.hybris.platform.jalo.extension.ExtensionManager;
import de.hybris.novalnet.fulfilmentprocess.constants.NovalnetFulfilmentProcessConstants;

public class NovalnetFulfilmentProcessManager extends GeneratedNovalnetFulfilmentProcessManager
{
	public static final NovalnetFulfilmentProcessManager getInstance()
	{
		ExtensionManager em = JaloSession.getCurrentSession().getExtensionManager();
		return (NovalnetFulfilmentProcessManager) em.getExtension(NovalnetFulfilmentProcessConstants.EXTENSIONNAME);
	}
	
}

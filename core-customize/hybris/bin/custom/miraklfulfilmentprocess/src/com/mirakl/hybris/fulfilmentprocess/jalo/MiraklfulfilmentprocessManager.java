/*
 * [y] hybris Platform
 *
 * Copyright (c) 2000-2016 hybris AG
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of hybris
 * ("Confidential Information"). You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms of the
 * license agreement you entered into with hybris.
 *
 *  
 */
package com.mirakl.hybris.fulfilmentprocess.jalo;

import com.mirakl.hybris.fulfilmentprocess.jalo.GeneratedMiraklfulfilmentprocessManager;
import com.mirakl.hybris.fulfilmentprocess.constants.MiraklfulfilmentprocessConstants;

import de.hybris.platform.jalo.JaloSession;
import de.hybris.platform.jalo.extension.ExtensionManager;

@SuppressWarnings("PMD")
public class MiraklfulfilmentprocessManager extends GeneratedMiraklfulfilmentprocessManager
{
	public static final MiraklfulfilmentprocessManager getInstance()
	{
		ExtensionManager em = JaloSession.getCurrentSession().getExtensionManager();
		return (MiraklfulfilmentprocessManager) em.getExtension(MiraklfulfilmentprocessConstants.EXTENSIONNAME);
	}
	
}

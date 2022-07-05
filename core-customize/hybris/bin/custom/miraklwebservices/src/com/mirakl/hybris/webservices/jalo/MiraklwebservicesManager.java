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
package com.mirakl.hybris.webservices.jalo;

import de.hybris.platform.jalo.JaloSession;
import de.hybris.platform.jalo.extension.ExtensionManager;
import org.apache.log4j.Logger;
import com.mirakl.hybris.webservices.constants.MiraklwebservicesConstants;

@SuppressWarnings("PMD")
public class MiraklwebservicesManager extends GeneratedMiraklwebservicesManager
{
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger( MiraklwebservicesManager.class.getName() );
	
	public static final MiraklwebservicesManager getInstance()
	{
		ExtensionManager em = JaloSession.getCurrentSession().getExtensionManager();
		return (MiraklwebservicesManager) em.getExtension(MiraklwebservicesConstants.EXTENSIONNAME);
	}
	
}

/*
 *  
 * Copyright (c) 2023 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.novalnet.core.jalo;

import de.hybris.novalnet.core.constants.NovalnetCoreConstants;
import de.hybris.platform.jalo.JaloSession;
import de.hybris.platform.jalo.extension.ExtensionManager;
import org.apache.log4j.Logger;

public class NovalnetCoreManager extends GeneratedNovalnetCoreManager
{
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger( NovalnetCoreManager.class.getName() );
	
	public static final NovalnetCoreManager getInstance()
	{
		ExtensionManager em = JaloSession.getCurrentSession().getExtensionManager();
		return (NovalnetCoreManager) em.getExtension(NovalnetCoreConstants.EXTENSIONNAME);
	}
	
}

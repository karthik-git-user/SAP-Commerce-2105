package com.mirakl.hybris.core.jalo;

import com.mirakl.hybris.core.constants.MiraklcartmergeConstants;
import de.hybris.platform.jalo.JaloSession;
import de.hybris.platform.jalo.extension.ExtensionManager;
import org.apache.log4j.Logger;

public class MiraklcartmergeManager extends GeneratedMiraklcartmergeManager
{
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger( MiraklcartmergeManager.class.getName() );
	
	public static final MiraklcartmergeManager getInstance()
	{
		ExtensionManager em = JaloSession.getCurrentSession().getExtensionManager();
		return (MiraklcartmergeManager) em.getExtension(MiraklcartmergeConstants.EXTENSIONNAME);
	}
	
}

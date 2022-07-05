package com.mirakl.hybris.channels.jalo;

import com.mirakl.hybris.channels.constants.MiraklchannelsConstants;
import de.hybris.platform.jalo.JaloSession;
import de.hybris.platform.jalo.extension.ExtensionManager;
import org.apache.log4j.Logger;

public class MiraklchannelsManager extends GeneratedMiraklchannelsManager
{
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger( MiraklchannelsManager.class.getName() );
	
	public static final MiraklchannelsManager getInstance()
	{
		ExtensionManager em = JaloSession.getCurrentSession().getExtensionManager();
		return (MiraklchannelsManager) em.getExtension(MiraklchannelsConstants.EXTENSIONNAME);
	}
	
}

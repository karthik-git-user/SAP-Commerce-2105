package com.mirakl.hybris.channelsaddon.jalo;

import com.mirakl.hybris.channelsaddon.constants.MiraklchannelsaddonConstants;
import de.hybris.platform.jalo.JaloSession;
import de.hybris.platform.jalo.extension.ExtensionManager;
import org.apache.log4j.Logger;

public class MiraklchannelsaddonManager extends GeneratedMiraklchannelsaddonManager
{
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger( MiraklchannelsaddonManager.class.getName() );
	
	public static final MiraklchannelsaddonManager getInstance()
	{
		ExtensionManager em = JaloSession.getCurrentSession().getExtensionManager();
		return (MiraklchannelsaddonManager) em.getExtension(MiraklchannelsaddonConstants.EXTENSIONNAME);
	}
	
}

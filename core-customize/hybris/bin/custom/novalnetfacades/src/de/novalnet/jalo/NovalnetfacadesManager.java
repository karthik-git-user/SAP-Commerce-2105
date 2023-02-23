package de.novalnet.jalo;

import de.hybris.platform.jalo.JaloSession;
import de.hybris.platform.jalo.extension.ExtensionManager;
import de.novalnet.constants.NovalnetfacadesConstants;
import org.apache.log4j.Logger;

public class NovalnetfacadesManager extends GeneratedNovalnetfacadesManager
{
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger( NovalnetfacadesManager.class.getName() );
	
	public static final NovalnetfacadesManager getInstance()
	{
		ExtensionManager em = JaloSession.getCurrentSession().getExtensionManager();
		return (NovalnetfacadesManager) em.getExtension(NovalnetfacadesConstants.EXTENSIONNAME);
	}
	
}

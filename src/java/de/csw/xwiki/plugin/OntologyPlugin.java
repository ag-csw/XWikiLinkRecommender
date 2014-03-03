/*******************************************************************************
 * This file is part of the Coporate Semantic Web Project.
 * 
 * This work has been partially supported by the ``InnoProfile-Corporate Semantic Web" project funded by the German Federal
 * Ministry of Education and Research (BMBF) and the BMBF Innovation Initiative for the New German Laender - Entrepreneurial Regions.
 * 
 * http://www.corporate-semantic-web.de/
 * 
 * Freie Universitaet Berlin
 * Copyright (c) 2007-2013
 * 
 * Institut fuer Informatik
 * Working Group Coporate Semantic Web
 * Koenigin-Luise-Strasse 24-26
 * 14195 Berlin
 * 
 * http://www.mi.fu-berlin.de/en/inf/groups/ag-csw/
 * 
 * This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA or see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package de.csw.xwiki.plugin;

import org.apache.log4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.notify.XWikiNotificationManager;
import com.xpn.xwiki.notify.XWikiNotificationRule;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;

import de.csw.ontology.OntologyIndex;
import de.csw.util.Config;

public class OntologyPlugin extends XWikiDefaultPlugin {
	private static Logger log = Logger.getLogger(OntologyPlugin.class); 

	final static String APP_PROP_FILE = "ontology_plugin.properties";

	private static final String ID = "ontology";

	/** if enabled == false no text enhancement is performed */
	boolean enabled = true;
	
	public OntologyPlugin(String name, String className, XWikiContext context) {
		super(name, className, context);
	}
	
	@Override
	public void init(XWikiContext context) {
		log.debug("Initializing annotator plugin");

		// load the plug-in configuration
		// this has to happen here, because we need a class loader with the context of the web application server
		ClassLoader cl = OntologyPlugin.class.getClassLoader();
		log.debug("Loading properties from file " + APP_PROP_FILE);
		Config.loadConfigFile(cl.getResourceAsStream(APP_PROP_FILE), false);
		log.debug("Loading properties from file " + APP_PROP_FILE + ".user");
		Config.loadConfigFile(cl.getResourceAsStream(APP_PROP_FILE + ".user"), true);
		
		// load the ontology
		log.debug("Loading ontology from file " + Config.getAppProperty(Config.ONTOLOGY_FILE));
		OntologyIndex.get().load(cl.getResourceAsStream(Config.getAppProperty(Config.ONTOLOGY_FILE)));
		log.debug("** " + OntologyIndex.get().getModel().size() + " statements loaded.");
		
		XWikiNotificationManager notificationManager = context.getWiki().getNotificationManager();
		
		XWikiNotificationRule rule = new OntologyNotificationRule(this);
		notificationManager.addGeneralRule(rule);
	}
	
	@Override
	public String getName() {
		return ID;
	}

	@Override
	public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context) {
		return new OntologyPluginAPI((OntologyPlugin) plugin, context);
	}

	@Override
	public void flushCache() {
		super.flushCache();
	}
	
	public boolean isEnabled() {
		log.debug("Changed ontology plug-in status to " + (enabled ? "active" : "inactive"));
		return enabled;
	}

	public void setEnabled(boolean b) {
		this.enabled = b;
	}
}

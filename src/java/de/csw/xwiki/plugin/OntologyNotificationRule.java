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

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.log4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.notify.XWikiNotificationRule;

import de.csw.linkgenerator.CSWLinksetRenderer;
import de.csw.ontology.XWikiTextEnhancer;

public class OntologyNotificationRule implements XWikiNotificationRule {
	static Logger log = Logger.getLogger(OntologyNotificationRule.class);
	
	OntologyPlugin plugin;

	static final String ACTION_VIEW = "view";
	static final String ACTION_EDIT = "edit";
	static final String ACTION_SAVE = "save";
	static final String ACTION_LOCK = "lock";
	
	private XWikiTextEnhancer textEnhancer;
	private CSWLinksetRenderer linksetRenderer;
	
	public OntologyNotificationRule(OntologyPlugin plugin) {
		textEnhancer = new XWikiTextEnhancer();
		this.linksetRenderer = new CSWLinksetRenderer();
		this.plugin = plugin;
	}

	public void verify(XWikiDocument doc, String action, XWikiContext context) {
		log.debug("VERIFY called");
	}

	public void verify(XWikiDocument newDoc, XWikiDocument oldDoc, XWikiContext context) {
	}

	/**
	 * This method gets called BEFORE rendering the document (in VIEW and EDIT
	 * mode). So this is the place to put all code that should intercept the
	 * rendering process.
	 */
	public void preverify(XWikiDocument doc, String action, XWikiContext context) {
		if (!plugin.isEnabled()) {
			log.error("Ontology plug-in is disabled.");
			return;
		}
		
		if (log.isDebugEnabled())
			log.debug("PREVERIFY: Received " + action + " action for doc: " + doc.getName());

		XWikiDocument orignialDoc = doc.getOriginalDocument();

		if (ACTION_VIEW.equals(action)) {
			
			String content = orignialDoc != null ? orignialDoc.getContent() : doc.getContent();
			String enhancedContent = textEnhancer.enhance(content);
			// enhancedContent = linksetRenderer.renderLinks(enhancedContent);
			
			doc.setContent(enhancedContent);
		} else if (ACTION_EDIT.equals(action)) {
			
			// The document can be a cached version, and it is possible that we
			// have altered its content before.
			// Since we do not want to show altered content, we reset its
			// content to its original state before rendering it for editing.
			if (orignialDoc != null)
				doc.setContent(orignialDoc.getContent());
		}
	}

	public void preverify(XWikiDocument newDoc, XWikiDocument oldDoc, XWikiContext context) {
		// nothing to do here
	}
}

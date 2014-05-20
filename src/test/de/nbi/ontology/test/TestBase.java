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
package de.nbi.ontology.test;


import java.io.InputStream;

import org.junit.Assert;

import org.apache.log4j.Logger;

import de.csw.ontology.OntologyIndex;
import de.csw.util.Config;

public class TestBase {
	final static Logger log = Logger.getLogger(TestBase.class); 
	final static String ENV_PROP_FILE = "testenv.properties";
	final static String APP_PROP_FILE = "jurawiki.properties";

	public TestBase() {
		ClassLoader cl = this.getClass().getClassLoader();
		log.debug("Loading properties from file " + APP_PROP_FILE);
		Config.loadConfigFile(cl.getResourceAsStream(APP_PROP_FILE), false);
		log.debug("Loading properties from file " + APP_PROP_FILE + ".user");
		Config.loadConfigFile(cl.getResourceAsStream(APP_PROP_FILE + ".user"), true);
		log.debug("Loading properties from file " + ENV_PROP_FILE);
		Config.loadConfigFile(cl.getResourceAsStream(ENV_PROP_FILE), false);
		log.debug("Loading properties from file " + ENV_PROP_FILE + ".user");
		Config.loadConfigFile(cl.getResourceAsStream(ENV_PROP_FILE + ".user"), true);
	}
	
	/**
	 * Reset the ontology index and load the deployed domain ontology into the
	 * index. The domain ontology has searched in the classpath.
	 */
	public void loadDomainOntology() {
		InputStream is = ClassLoader.getSystemResourceAsStream(Config.getAppProperty(Config.ONTOLOGY_FILE));
		Assert.assertNotNull(is);

		OntologyIndex index = OntologyIndex.get();
		index.reset();
		index.load(is);
	}
}

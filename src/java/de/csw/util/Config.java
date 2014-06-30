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
package de.csw.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

public class Config {
	protected static Logger log = Logger.getLogger(Config.class);

	protected static Properties app_properties;

	public static final String DIR_RESOURCES = "dir.resources";
	
	/** filename of the deployed domain ontology file */
	public static final String ONTOLOGY_FILE = "ontology.file";

	/** directory being the root of the test data */
	public static final String DIR_RESOURCES_TEST = "dir.resources.test";
	public static final String LUCENE_URL = "lucene.url";
	public static final String LUCENE_MAXSEARCHTERMS = "lucene.maxsearchterms";
	public static final String LUCENE_EDITPROPRESULTS = "lucene.editpanel.maxresults";
	
	public static final String LANGUAGES = "languages";

	/**
	 * Loads the properties from an input stream to the property hash table.
	 * 
	 * @param is
	 *            input stream to be loaded
	 * @param optional
	 *            if <code>true</code> a missing file will be reported as a
	 *            warning, otherwise it is reported as an error.
	 */
	public static void loadConfigFile(InputStream is, boolean optional) {
		final String notFoundMsg = "** NOT found.";
		if (is == null) {
			if (optional) 	log.warn(notFoundMsg);
			else 			log.error(notFoundMsg);
		}
		
		Properties tmpProp = new Properties();
		if (is != null) {
			try {
				tmpProp.load(is);
			} catch (IOException e) {
				log.error("ERROR loading application property file ", e);
			}
			getAppProperties().putAll(tmpProp);
			log.debug("** " + tmpProp.size() + " properties loaded.");
		}
	}

	/**
	 * Return the value of property <code>name</code>. If necessary the
	 * property file is read.
	 * 
	 * @param name
	 * @return value of property <code>name</code>
	 */
	public static String getAppProperty(String name) {
		if(app_properties == null) {
			log.error("No application properties.");
			return "";
		}
		String value = app_properties.getProperty(name);
		if(value == null) {
			log.error("Property not found: "+name);
		}
		return value;
	}

	/**
	 * @see #getAppProperty(String)
	 */
	public static int getIntAppProperty(String name) {
		String value = app_properties.getProperty(name);
		if(value == null) {
			log.error("Property not found: "+name);
		}
		return Integer.valueOf(value);
	}

	/**
	 * Discards all property definitions read from the property file of the
	 * application.
	 * 
	 */
	public static void discardAppProperties() {
		app_properties.clear();
		app_properties = null;
	}

	/**
	 * Return all application properties.
	 */
	public static Properties getAppProperties() {
		if (app_properties == null) {
			app_properties = new Properties();
		}
		return app_properties;
	}
	
	/**
	 * Initializes the application config. Called from the KGSContextListener
	 */
	public static void setAppProperties(Properties p) {
		app_properties = p;
	}

	public static boolean getBooleanAppProperty(String name) {
		String value = app_properties.getProperty(name);
		if(value == null) {
			log.error("Property not found: "+name);
		}
		return Boolean.valueOf(app_properties.getProperty(name));
	}

	public static float getFloatAppProperty(String name) {
		String value = app_properties.getProperty(name);
		if(value == null) {
			log.error("Property not found: "+name);
		}
		return Float.parseFloat(app_properties.getProperty(name));
	}
	
	/**
	 * Returns a property's value as a list, if the value represents more than
	 * one elements (seperated by commas or semicolons).
	 * @param name
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static List<String> getListProperty(String name) {
		String value = app_properties.getProperty(name);
		if(value == null) {
			log.error("Property not found: "+name);
			return Collections.EMPTY_LIST;
		}
		return Arrays.asList(value.split("[,;]"));
	}
}

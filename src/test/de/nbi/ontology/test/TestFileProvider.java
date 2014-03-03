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

import java.io.File;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.log4j.Logger;
import org.testng.annotations.DataProvider;

import de.csw.util.Config;

public class TestFileProvider {
	static final Logger logger = Logger.getLogger(TestFileProvider.class);

	/**
	 * Retrieve all files for the label test.
	 */
	@DataProvider(name="labelTestFiles")
	public static Object[][] getLabelTestFiles() {
		return getTestFiles("util", "label", "txt");
	}
	
	/**
	 * Retrieve all files for the indexing test.
	 */
	@DataProvider(name="indexTestFiles")
	public static Object[][] getIndexTestFiles() {
		return getTestFiles("ontology", "index", "owl");
	}

	/**
	 * Retrieve all files for the prefix test.
	 */
	@DataProvider(name="prefixTestFiles")
	public static Object[][] getPrefixTestFiles() {
		return getTestFiles("ontology", "prefix", "owl");
	}

	/**
	 * Retrieve all files for the exact match test.
	 */
	@DataProvider(name="exactMatchTestFiles")
	public static Object[][] getExactMatchTestFiles() {
		return getTestFiles("ontology", "exact", "txt");
	}

	/**
	 * Retrieve all files for the synonym test.
	 */
	@DataProvider(name="synonymTestFiles")
	public static Object[][] getSynonymTestFiles() {
		return getTestFiles("ontology", "synonym", "txt");
	}

	/**
	 * Retrieve all files for the parent test.
	 */
	@DataProvider(name="parentTestFiles")
	public static Object[][] getParentTestFiles() {
		return getTestFiles("ontology", "parent", "txt");
	}

	/**
	 * Retrieve all files for the children test.
	 */
	@DataProvider(name="childTestFiles")
	public static Object[][] getChildTestFiles() {
		return getTestFiles("ontology", "child", "txt");
	}

	/**
	 * Retrieve all files for the similar test.
	 */
	@DataProvider(name="similarTestFiles")
	public static Object[][] getSimilarTestFiles() {
		return getTestFiles("ontology", "similar", "txt");
	}

	/**
	 * Retrieve all files for the enhance test.
	 */
	@DataProvider(name="enhanceTestFiles")
	public static Object[][] getEnhanceTestFiles() {
		return getTestFiles("enhance", "text", "txt");
	}

	/**
	 * Helper class to retrieve files from the file system (relative to
	 * {@link Config#DIR_RESOURCES_TEST}). File names have the form
	 * <code>prefix + * "-\\d+\\." + ext</code>.
	 * 
	 * @param dir
	 *            directory to be searched for files
	 * @param prefix
	 *            prefix of the files
	 * @param ext
	 *            extension of the files
	 */
	@SuppressWarnings("unchecked")
	public static Object[][] getTestFiles(String dir, String prefix, String ext) {
		Object[][] data;
		
		File resourceDir = new File(Config.getAppProperty(Config.DIR_RESOURCES_TEST) + "/" + dir);
		
		Collection<File> testFiles = 
			FileUtils.listFiles(resourceDir, new RegexFileFilter(prefix + "-\\d+\\." + ext), null);
		data = new Object[testFiles.size()][1];

		int count = 0;
		for (File inputFile : testFiles) {
			data[count] = new Object[]{inputFile};
			count++;
		}
		
		return data;
	}
}

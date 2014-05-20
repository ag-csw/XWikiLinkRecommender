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
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.junit.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import de.csw.ontology.OntologyIndex;
import de.csw.ontology.XWikiTextEnhancer;

public class TextEnhancerTest extends TestBase {
	static Logger log = Logger.getLogger(TextEnhancerTest.class);
	static final String ONTO_FILENAME = "gewuerz.owl";

	OntologyIndex index = OntologyIndex.get();

	/**
	 * Load the domain ontology.
	 */
	@BeforeSuite
	public void loadOntology() throws FileNotFoundException {
		log.info("Loading " + ONTO_FILENAME);
		loadDomainOntology();
	}

	/**
	 * Test, if texts are enhances properly.
	 * 
	 * @param inFile
	 *            a text
	 * @throws IOException
	 */
	@Test(dataProviderClass = TestFileProvider.class, dataProvider = "enhanceTestFiles",
			groups = { "functest" })
	public void exactMatch(File inFile) throws IOException {
		log.info("Processing " + inFile.getName());
		String basename = FilenameUtils.removeExtension(inFile.getAbsolutePath());
		File outFile = new File(basename + ".out");
		File resFile = new File(basename + ".res");

		PrintWriter w = new PrintWriter(new FileWriter(outFile));

		XWikiTextEnhancer enhancer = new XWikiTextEnhancer();
		String text = FileUtils.readFileToString(inFile);
		String newText = enhancer.enhance(text);

		w.print(newText);
		w.flush(); w.close();

		Assert.assertTrue(FileUtils.contentEquals(outFile, resFile));
	}
}

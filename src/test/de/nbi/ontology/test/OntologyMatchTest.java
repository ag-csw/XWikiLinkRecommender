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
import java.util.List;

import org.junit.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.hp.hpl.jena.ontology.OntClass;

import de.csw.ontology.OntologyIndex;

public class OntologyMatchTest extends TestBase {
	static Logger log = Logger.getLogger(OntologyMatchTest.class);
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
	 * Test, if terms are properly match to concept labels. The a list of terms
	 * contains a term in each line.
	 * 
	 * @param inFile
	 *            a list of terms
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	@Test(dataProviderClass = TestFileProvider.class, dataProvider = "exactMatchTestFiles",
			groups = { "functest" })
	public void exactMatch(File inFile) throws IOException {
		log.info("Processing " + inFile.getName());
		String basename = FilenameUtils.removeExtension(inFile.getAbsolutePath());
		File outFile = new File(basename + ".out");
		File resFile = new File(basename + ".res");

		List<String> terms = FileUtils.readLines(inFile);
		PrintWriter w = new PrintWriter(new FileWriter(outFile));
		for (String term : terms) {
			log.trace("** matching " + term);
			w.println(index.getExactMatches(term));
		}
		w.flush();
		w.close();

		Assert.assertTrue(FileUtils.contentEquals(outFile, resFile));
	}

	/**
	 * Test, if terms are properly match to concept labels. The a list of terms
	 * contains a term in each line.
	 * 
	 * @param inFile
	 *            a list of terms
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	@Test(dataProviderClass = TestFileProvider.class, dataProvider = "synonymTestFiles",
			groups = { "functest" })
	public void synonyms(File inFile) throws IOException {
		log.info("Processing " + inFile.getName());
		String basename = FilenameUtils.removeExtension(inFile.getAbsolutePath());
		File outFile = new File(basename + ".out");
		File resFile = new File(basename + ".res");

		List<String> terms = FileUtils.readLines(inFile);
		PrintWriter w = new PrintWriter(new FileWriter(outFile));
		for (String term : terms) {
			OntClass clazz = index.getModel().getOntClass(term);
			log.trace("** matching " + term);
			w.println(index.getSynonyms(clazz));
		}
		w.flush();
		w.close();

		Assert.assertTrue(FileUtils.contentEquals(outFile, resFile));
	}

	/**
	 * Test, if terms are properly match to concept labels. The a list of terms
	 * contains a term in each line.
	 * 
	 * @param inFile
	 *            a list of terms
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	@Test(dataProviderClass = TestFileProvider.class, dataProvider = "parentTestFiles",
			groups = { "functest" })
	public void parents(File inFile) throws IOException {
		log.info("Processing " + inFile.getName());
		String basename = FilenameUtils.removeExtension(inFile.getAbsolutePath());
		File outFile = new File(basename + ".out");
		File resFile = new File(basename + ".res");

		List<String> terms = FileUtils.readLines(inFile);
		PrintWriter w = new PrintWriter(new FileWriter(outFile));
		for (String term : terms) {
			OntClass clazz = index.getModel().getOntClass(term);
			log.trace("** matching " + term);
			w.println(index.getParents(clazz));
		}
		w.flush();
		w.close();

		Assert.assertTrue(FileUtils.contentEquals(outFile, resFile));
	}

	/**
	 * Test, if terms are properly match to concept labels. The a list of terms
	 * contains a term in each line.
	 * 
	 * @param inFile
	 *            a list of terms
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	@Test(dataProviderClass = TestFileProvider.class, dataProvider = "childTestFiles",
			groups = { "functest" })
	public void children(File inFile) throws IOException {
		log.info("Processing " + inFile.getName());
		String basename = FilenameUtils.removeExtension(inFile.getAbsolutePath());
		File outFile = new File(basename + ".out");
		File resFile = new File(basename + ".res");

		List<String> terms = FileUtils.readLines(inFile);
		PrintWriter w = new PrintWriter(new FileWriter(outFile));
		for (String term : terms) {
			log.trace("** matching " + term);
			OntClass clazz = index.getModel().getOntClass(term);
			w.println(index.getChildren(clazz));
		}
		w.flush();
		w.close();

		Assert.assertTrue(FileUtils.contentEquals(outFile, resFile));
	}

	/**
	 * Test, if terms are properly match to concept labels. The a list of terms
	 * contains a term in each line.
	 * 
	 * @param inFile
	 *            a list of terms
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	@Test(dataProviderClass = TestFileProvider.class, dataProvider = "similarTestFiles",
			groups = { "functest" })
	public void similar(File inFile) throws IOException {
		log.info("Processing " + inFile.getName());
		String basename = FilenameUtils.removeExtension(inFile.getAbsolutePath());
		File outFile = new File(basename + ".out");
		File resFile = new File(basename + ".res");

		List<String> terms = FileUtils.readLines(inFile);
		PrintWriter w = new PrintWriter(new FileWriter(outFile));
		for (String term : terms) {
			log.trace("** matching " + term);
			w.println(index.getSimilarMatches(term));
		}
		w.flush();
		w.close();

		Assert.assertTrue(FileUtils.contentEquals(outFile, resFile));
	}
}

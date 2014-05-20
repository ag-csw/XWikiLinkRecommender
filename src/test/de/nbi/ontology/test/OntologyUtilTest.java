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
import de.csw.ontology.util.OntologyUtils;

public class OntologyUtilTest extends TestBase {
	static Logger log = Logger.getLogger(OntologyUtilTest.class);
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
	 * Test, if concept labels are created properly. The input file contains a
	 * URI in each line.
	 * 
	 * @param inFile
	 *            input file
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	@Test(dataProviderClass=TestFileProvider.class, dataProvider="labelTestFiles",
			groups = {"functest"})
	public void getLabel(File inFile) throws IOException {
		
		// TODO. With the recent change to the OntologyUtil class, labels are
		// not extracted from a concept's uri any more. Instead, the RDF Label
		// annotations are used. Thus, a concept can have more than one label now.
		// The problem is that no assumption about the order in which the labels
		// are returned can be made. We have to rewrite the test case such that
		// it does not make this assumption.
		
		log.info("Processing " + inFile.getName());
		String basename = FilenameUtils.removeExtension(inFile.getAbsolutePath());
		File outFile = new File(basename + ".out");
		File resFile = new File(basename + ".res");
		
		List<String> uris = FileUtils.readLines(inFile);
		PrintWriter w = new PrintWriter(new FileWriter(outFile));

		for (String u : uris) {
			OntClass clazz = index.getModel().getOntClass(u);
			List<String> labels = OntologyUtils.getLabels(clazz);
			for (String l : labels) {
				log.debug("** " + u + " => " + l);
				w.println(l);
			}
		}
		w.flush(); w.close();

		Assert.assertTrue(FileUtils.contentEquals(outFile, resFile));
	}
}

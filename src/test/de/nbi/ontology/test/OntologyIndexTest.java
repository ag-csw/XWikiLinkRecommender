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
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.testng.annotations.Test;

import com.hp.hpl.jena.ontology.OntClass;

import de.csw.ontology.OntologyIndex;

public class OntologyIndexTest extends TestBase {
	static Logger log = Logger.getLogger(OntologyIndexTest.class);

	OntologyIndex index = OntologyIndex.get();
	
	/**
	 * Test, if concept labels are created properly. The input file contains a
	 * URI in each line.
	 * 
	 * @param inFile
	 *            an ontology
	 * @throws IOException
	 */
//	@Test(dataProviderClass=OntologyDataProvider.class, dataProvider="indexTestFiles",
//			groups = {"functest"})
	public void createIndex(File inFile) throws IOException {
		log.info("Processing " + inFile.getName());
		String basename = FilenameUtils.removeExtension(inFile.getAbsolutePath());
		File outFile = new File(basename + ".out");
		File resFile = new File(basename + ".res");
		
		index.reset();
		index.load(new FileInputStream(inFile));
		
		PrintWriter w = new PrintWriter(new FileWriter(outFile));
		Map<String, OntClass[]> labelIdx = index.getLabelIndex();
		for (String k : labelIdx.keySet()) {
			w.println(k + " = > " + Arrays.toString(labelIdx.get(k)));
		}
		
		List<String> prefixIdx = new ArrayList<String>(index.getPrefixIndex());
		Collections.sort(prefixIdx);
		for (String p : prefixIdx) {
			w.println(p);
		}
		w.flush(); w.close();
		
		Assert.assertTrue(FileUtils.contentEquals(outFile, resFile));
	}
	
	/**
	 * Test, if isPrefix(*) works.
	 * 
	 * @param inFile
	 *            an ontology
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	@Test(dataProviderClass=TestFileProvider.class, dataProvider="prefixTestFiles",
			groups = {"functest"})
	public void isPrefix(File inFile) throws IOException {
		log.info("Processing " + inFile.getName());
		String basename = FilenameUtils.removeExtension(inFile.getAbsolutePath());
		File txtFile = new File(basename + ".txt");
		
		index.reset();
		index.load(new FileInputStream(inFile));
		
		List<String> prefixes = FileUtils.readLines(txtFile);
		for (String p : prefixes) {
			log.trace("** checking " + p);
			Assert.assertTrue(index.isPrefix(p));
			Assert.assertTrue(index.isPrefix(Arrays.asList(StringUtils.split(p))));
		}
	}
}

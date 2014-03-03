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
package de.csw.ontology.util;

import java.util.ArrayList;
import java.util.List;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

import de.csw.util.Config;

/**
 * Helper methods for working with ontologies.
 * 
 * @author rheese
 * 
 */
public class OntologyUtils {
	/**
	 * Generates a concept label from the local part of a given URI.
	 * 
	 * @param uri
	 *            a URI
	 * @return a label
	 */
	public static String _getLabel(String uri) {
		if (uri == null)
			return null;
		String label = uri.substring(uri.lastIndexOf('#') + 1);
		label = label.replace('_', ' ');
		return label;
	}

	/**
	 * Generates the concept labels for a list of URIs. The order is kept.
	 * 
	 * @param uris
	 *            a list of URIs
	 * @return a list of corresponding labels
	 */
	public static List<String> _getLabels(List<String> uris) {
		if (uris == null)
			return null;
		
		List<String> labels = new ArrayList<String>(uris.size());
		for (String u : uris)
			labels.add(_getLabel(u));

		return labels;
	}
	
	/**
	 * Returns a list containing all labels for the given class, in all
	 * languages specified in the global application configuration.
	 * 
	 * @param clazz
	 *            an OntClass
	 * @return a list of labels for the given class
	 */
	public static List<String> getLabels(OntClass clazz) {
		ArrayList<String> result = new ArrayList<String>();
		List<String> languages = Config.getListProperty(Config.LANGUAGES);
		for (String language : languages) {
			ExtendedIterator labelIter = clazz.listLabels(language);
			while (labelIter.hasNext()) {
				Literal label = (Literal)labelIter.next();
				result.add(label.getString());
			}
		}

        if (result.isEmpty()) {
           result.add(_getLabel(clazz.getURI()));
        }

		return result;
	}
	
	/**
	 * Returns a list containing all labels for the given classes, in all
	 * languages specified in the global application configuration.
	 * 
	 * @param classes
	 *            a list of OntClasses
	 * @return a list of labels for the given classes
	 */
	public static List<String> getLabels(List<OntClass> classes) {
		ArrayList<String> result = new ArrayList<String>();
		
		for (OntClass clazz : classes) {
			result.addAll(getLabels(clazz));
		}
		
		return result;
	}
}

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
package de.csw.ontology;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.de.CSWGermanAnalyzer;
import org.jfree.util.Log;

import de.csw.lucene.ConceptFilter;
import de.csw.util.Config;

/**
 * Uses background knowledge to enhance the text.
 * 
 * @author rheese
 * 
 */
public class XWikiTextEnhancer implements TextEnhancer {
	static final Logger log = Logger.getLogger(XWikiTextEnhancer.class);
	
	static final int MAX_SIMILAR_CONCEPTS = Config.getIntAppProperty(Config.LUCENE_MAXSEARCHTERMS);
	static final String LUCENE_URL = Config.getAppProperty(Config.LUCENE_URL);
	
	OntologyIndex index;
	
	/** index for storing the positions of links in a text (start position, end position) */
	TreeMap<Integer, Integer> linkIndex = new TreeMap<Integer, Integer>();
	
	public XWikiTextEnhancer() {
		index = OntologyIndex.get();
	}

	/**
	 * The enhanced text contains links to the Lucene search page of the xWiki
	 * system. The search terms are related to the annotated phrase.
	 */
	public String enhance(String text) {
		CSWGermanAnalyzer ga = new CSWGermanAnalyzer();
		TokenStream ts = null;
		StringBuilder result = new StringBuilder();
		
		initializeLinkIndex(text);
		
		try {
			Reader r = new BufferedReader(new StringReader(text));
			
			ts = ga.tokenStream("",	 r);
			
			
			Token token;
			String term;
			int lastEndIndex = 0;
			
			while(ts.incrementToken()) {
			
				token = ts.addAttribute(Token.class);
					
				result.append(text.substring(lastEndIndex, token.startOffset()));
				term = String.copyValueOf(token.buffer(), 0, token.length());
				
				if (token.type().equals(ConceptFilter.CONCEPT_TYPE) && isAnnotatable(token)) {
					log.debug("Annotating concept: " + term);
					annotateWithSearch(result, text.substring(token.startOffset(), token.endOffset()));
				} else {
					result.append(text.substring(token.startOffset(), token.endOffset()));
				}
					
				lastEndIndex = token.endOffset();
			}
			result.append(text.subSequence(lastEndIndex, text.length()));
		} catch (IOException e) {
			Log.error("Error while processing the page content", e);
		}
		
		ga.close();
		return result.toString();
	}

	/**
	 * Extract from text all phrases that are enclosed by '[' and ']' denoting
	 * an xWiki link.
	 * 
	 * @param text
	 *            text to parse
	 */
	protected void initializeLinkIndex(String text) {
		if (text == null)
			throw new NullPointerException("Parameter text must not be null");
		
		linkIndex.clear();

		if (text.isEmpty()) return;
		
		Pattern pattern = Pattern.compile("\\[[^\\]]*\\]");
		Matcher matcher = pattern.matcher(text);
		
		while (matcher.find()) {
			linkIndex.put(matcher.start(), matcher.end());
		}

		pattern = Pattern.compile("<csw:linkset.*?>.*?</csw:linkset>");
		matcher = pattern.matcher(text);
		
		while (matcher.find()) {
			linkIndex.put(matcher.start(), matcher.end());
		}
	}

	/**
	 * Test if a token can be annotated by the {@link TextEnhancer}, e.g., if it
	 * is not a wiki link.
	 * 
	 * @param token
	 *            a token
	 * @return true iff the token can be annotated
	 */
	protected boolean isAnnotatable(Token token) {
		int tokenStart = token.startOffset();
		Entry<Integer, Integer> floor = linkIndex.floorEntry(tokenStart);
		
		return floor == null || (floor.getValue() < tokenStart);
	}

	/**
	 * Annotates the term by linking <code>term</code> to the search page of the
	 * wiki.
	 * 
	 * @param term
	 *            a term
	 * @return annotated term
	 */
	protected String annotateWithSearch(StringBuilder sb, String term) {
		List<String> matches = index.getSimilarMatchLabels(term, MAX_SIMILAR_CONCEPTS);

		if (matches.isEmpty())
			return term;

		sb.append("<a class=\"similarconcept\" title=\"Suche nach den verwandten Begriffen: ");
		Iterator<String> it = matches.listIterator(1);
		sb.append(it.next());
		while (it.hasNext()) {
			sb.append(", " + it.next());
		}
		sb.append("\" href=\"" + getSearchURL(matches) + "\">");
		sb.append(term);
		sb.append("</a>");
		return sb.toString();
	}

	/**
	 * Creates a link to the search wiki page.
	 * 
	 * @param terms
	 *            a collection of search terms
	 * @return the link
	 */
	protected String getSearchURL(Collection<String> terms) {
		log.debug("** search terms: " + terms);
		return LUCENE_URL + "?text=" + StringUtils.join(terms, '+');
	}
}

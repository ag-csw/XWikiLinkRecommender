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
package de.csw.lucene;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;

import de.csw.ontology.OntologyIndex;

/**
 * A filter that detects concepts from an ontology in the token stream. A
 * concept token is assigned the type {@value #CONCEPT_TYPE}.
 * 
 * @author rheese
 * 
 */
public final class ConceptFilter extends TokenFilter {
	static final Logger log = Logger.getLogger(ConceptFilter.class);
	
	/** token type of a concept */
	public static final String CONCEPT_TYPE = "concept"; 

	OntologyIndex index;

	/** Tokens that have been read ahead */
	Queue<Token> queue = new LinkedList<Token>();

	/**
	 * Build an ConceptFilter that uses a given ontology index
	 * 
	 * @param in
	 *            a token stream
	 * @param oi
	 *            a ontology index
	 */
	public ConceptFilter(TokenStream in, OntologyIndex oi) {
		super(in);
		index = oi;
	}

	/**
	 * @return Returns the next token in the stream, or null at EOS
	 */
	public final Token next(final Token reusableToken) throws IOException {
		assert reusableToken != null;
		
		Token nextToken = queue.isEmpty() ? input.next(reusableToken) : queue.poll(); 
		
		if (nextToken == null)
			return null;
		
		// check if we got a prefix of a concept (searching the longest match)
		Token tmpToken;
		List<String> terms = new ArrayList<String>();
		terms.add(String.copyValueOf(nextToken.termBuffer(), 0, nextToken.termLength()));
		int bufferSize = nextToken.termLength();

		while (index.isPrefix(terms)) {
			tmpToken = input.next();
			// TODO maybe we spoil buffer space using termLength(); is endOffset the right one?
			bufferSize += tmpToken.termLength() + 1;
			queue.add(tmpToken);
			terms.add(String.copyValueOf(tmpToken.termBuffer(), 0, tmpToken.termLength()));
		}
		
		if (index.hasExactMatches(StringUtils.join(terms.toArray(), ' '))) {
			if (!queue.isEmpty()) {
				// we have to adjust the token to represent the complete concept
				nextToken.resizeTermBuffer(bufferSize);
				int destPos = nextToken.termLength();
				// TODO the first one could be ignored since it is nextToken instance
				for (Token t : queue) {
					nextToken.termBuffer()[destPos] = OntologyIndex.PREFIX_SEPARATOR;
					nextToken.setEndOffset(t.endOffset());
					System.arraycopy(t.termBuffer(), 0, nextToken.termBuffer(), destPos + 1, t.termLength());
					destPos += t.termLength() + 1;
				}
				nextToken.setTermLength(bufferSize);
				queue.clear();
			}
			
			nextToken.setType(CONCEPT_TYPE);
			log.trace("Concept token recognized: " + String.copyValueOf(nextToken.termBuffer(), 0, nextToken.termLength()));
		}
		
		return nextToken;
	}
}

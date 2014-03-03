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
package org.apache.lucene.analysis.de;

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
import java.util.Set;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;

/**
 * A filter that stems German words. It supports a table of words that should
 * not be stemmed at all. The stemmer used can be changed at runtime after the
 * filter object is created (as long as it is a GermanStemmer).
 * 
 * 
 * @version $Id$
 */
public final class GermanStemFilter extends TokenFilter {
	private GermanStemmer stemmer = null;
	private Set<String> exclusionSet = null;

	public GermanStemFilter(TokenStream in) {
		super(in);
		stemmer = new GermanStemmer();
	}

	/**
	 * Builds a GermanStemFilter that uses an exclusion table.
	 */
	public GermanStemFilter(TokenStream in, Set<String> exclusionSet) {
		this(in);
		this.exclusionSet = exclusionSet;
	}

	/**
	 * @return Returns the next token in the stream, or null at EOS
	 */
	public final Token next(final Token reusableToken) throws IOException {
		assert reusableToken != null;
		Token nextToken = input.next(reusableToken);

		if (nextToken == null)
			return null;

		String term = String.copyValueOf(nextToken.termBuffer(), 0, nextToken.termLength());
		// Check the exclusion table.
		if (exclusionSet == null || !exclusionSet.contains(term)) {
			String s = stemmer.stem(term);
			// If not stemmed, don't waste the time adjusting the token.
			if ((s != null) && !s.equals(term))
				nextToken.setTermText(s);
		}
		return nextToken;
	}

	/**
	 * Set a alternative/custom GermanStemmer for this filter.
	 */
	public void setStemmer(GermanStemmer stemmer) {
		if (stemmer != null) {
			this.stemmer = stemmer;
		}
	}

	/**
	 * Set an alternative exclusion list for this filter.
	 */
	public void setExclusionSet(Set<String> exclusionSet) {
		this.exclusionSet = exclusionSet;
	}
}

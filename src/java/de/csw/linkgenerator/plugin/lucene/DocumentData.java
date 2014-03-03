/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package de.csw.linkgenerator.plugin.lucene;

import java.util.List;

import org.apache.log4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.DocumentSection;
import com.xpn.xwiki.doc.XWikiDocument;

import de.csw.ontology.OntologyIndex;

/**
 * Holds all data but the content of a wiki page to be indexed. The content is retrieved at indexing
 * time, which should save us some memory especially when rebuilding an index for a big wiki.
 * 
 * @version $Id: $
 */
public class DocumentData extends IndexData
{
	// word delimiters
	private static final String WORD_DELIMITER_PATTERN_STRING = "[\\s\\p{Punct}\\+\\=]+";

//	private static final Pattern sectionTitlePattern = Pattern.compile("^\\d(\\.\\d)* (.*)$");
	private static final Logger log = Logger.getLogger(DocumentData.class);
	
    public DocumentData(final XWikiDocument doc, final XWikiContext context)
    {
        super(doc, context);

        setAuthor(doc.getAuthor());
        setCreator(doc.getCreator());
        setModificationDate(doc.getDate());
        setCreationDate(doc.getCreationDate());
    }

    /**
     * @see IndexData#getType()
     */
    public String getType()
    {
        return LucenePlugin.DOCTYPE_WIKIPAGE;
    }

    /**
     * @return a string containing the result of {@link IndexData#getFullText} plus the full text
     *         content of this document (in the given language)
     */
    public String getFullText(XWikiDocument doc, XWikiContext context)
    {
        StringBuffer text = new StringBuffer(super.getFullText(doc, context));
        text.append(" ");
        text.append(super.getDocumentTitle());
        text.append(" ");
        text.append(doc.getContent());

        return text.toString();
    }
    
    public String getRelevantData(XWikiDocument doc, XWikiContext context) {
        StringBuffer text = new StringBuffer();
        text.append(super.getDocumentTitle());
        text.append(" ");
        text.append(extractDocumentSectionTitles(doc));

        String[] terms = text.toString().split(WORD_DELIMITER_PATTERN_STRING);
        
        OntologyIndex ontologyIndex = OntologyIndex.get();

        for (int i = 0; i < terms.length; i++) {
			String term = terms[i];
			List<String> similarTerms = ontologyIndex.getSimilarMatchLabels(term, 5);
			for (String similarTerm : similarTerms) {
				text.append(' ');
				text.append(similarTerm);
			}
		}
        

        return text.toString();
    }
    
    private String extractDocumentSectionTitles(XWikiDocument doc) {
    	StringBuffer text = new StringBuffer();
//    	Matcher matcher = sectionTitlePattern.matcher(doc.getContent());
    	
    	try {
			List<DocumentSection> sections = doc.getSections();
			for (DocumentSection section : sections) {
				String sectionTitle = section.getSectionTitle();
				text.append(sectionTitle);
				log.debug("Found section with title '" + sectionTitle + "'");
				text.append(' ');
			}
		} catch (XWikiException e) {
			log.error("Could not retrieve sections of document " + getDocumentFullName());
		}
		
		int len = text.length();
		if (len == 0) {
			return text.toString();
		}
		return text.substring(0, text.length()-1);
    }
 }

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
package de.csw.linkgenerator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

public class CSWLinksetRenderer {
	
	private static final String FILENAME_CSWLINK_POPUP_HTML = "CSWLinkPopup.html";

	private static final Logger log = Logger.getLogger(CSWLinksetRenderer.class);
	
	private static final Pattern linksetPattern = Pattern.compile("<csw:linkset.*?>.*?</csw:linkset>");
	private static final Pattern linkPattern = Pattern.compile("<csw:link page=\"(.*?)\"");
	private static final Pattern textContentPattern = Pattern.compile(">(.*?)<");
	

	
	private String cswLinkPopupHTML;
	
	public CSWLinksetRenderer() {
		try {
            InputStream is = CSWLinksetRenderer.class.getResourceAsStream(FILENAME_CSWLINK_POPUP_HTML);
            if (is == null) {
                 log.error("Unable to read HTML template of the CSW popup");
                 return;
             }
			cswLinkPopupHTML = IOUtils.toString(is);
			
			// strip empty lines
			Pattern emptyLinePattern = Pattern.compile("$\n^\\s*$", Pattern.MULTILINE);
			cswLinkPopupHTML = emptyLinePattern.matcher(cswLinkPopupHTML).replaceAll("");
			cswLinkPopupHTML = cswLinkPopupHTML.replace("[", "&#91;");
			cswLinkPopupHTML = cswLinkPopupHTML.replace("]", "&#93;");
		} catch (IOException e) {
			log.error("Could not read " + FILENAME_CSWLINK_POPUP_HTML, e);
		}
	}
	
	public String renderLinks(String text) {
		Matcher linksetMatcher = linksetPattern.matcher(text);
		
		StringBuilder newText = new StringBuilder();
		
		int oldEnd = 0;
		
		while (linksetMatcher.find()) {
			
			String linkset = linksetMatcher.group();
			
			// extract the content of the text nodes
			StringBuilder textContent = new StringBuilder();
			Matcher textContentMatcher = textContentPattern.matcher(linkset);
			while (textContentMatcher.find()) {
				textContent.append(textContentMatcher.group(1));
			}
			
			int start = linksetMatcher.start();
			int end = linksetMatcher.end();
			
			newText.append(text.substring(oldEnd, start));
			newText.append("<a href=\"#\" onclick=\"showPopup(this, new Array(");
						
			Matcher linkMatcher = linkPattern.matcher(linkset);
			while(linkMatcher.find()) {
				String page = linkMatcher.group(1);
				newText.append('\'');
				newText.append(page);
				newText.append("',");
			}
			
			newText.setCharAt(newText.length()-1, ')');
			
			newText.append("); return false;\">");
			newText.append(textContent);
			newText.append("</a>");
			
			oldEnd = end;
		}
		
		// append rest of text
		newText.append(text.substring(oldEnd, text.length()));
		
		// end == 0 means that there are no csw:linkset elements, thus we do not need to include the popup html and javascript
		if (oldEnd != 0) {
			newText.append('\n');
			newText.append(cswLinkPopupHTML);
		}
		
		return newText.toString();
	}
}

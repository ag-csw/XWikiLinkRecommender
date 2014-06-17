/**
 * 
 */
package de.csw.linkgenerator.struts;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.web.XWikiAction;
import com.xpn.xwiki.web.XWikiRequest;

import org.apache.http.client.utils.URLEncodedUtils;

import de.csw.linkgenerator.plugin.lucene.LucenePluginApi;
import de.csw.linkgenerator.plugin.lucene.SearchResult;
import de.csw.linkgenerator.plugin.lucene.SearchResults;

/**
 * @author ralph
 *
 */
public class CSWLinkAction extends XWikiAction {

	@Override
	public boolean action(XWikiContext context) throws XWikiException {
		XWikiRequest request = context.getRequest();
		String query = request.get("text");
		if (query == null) {
			return false;
		}
		
		PrintWriter out = null;
		try {
			out = context.getResponse().getWriter();
			context.getResponse().setContentType("text/plain");
			
		} catch (IOException e) {
			return false;
		}

		LucenePluginApi lucene = (LucenePluginApi)context.getWiki().getPluginApi("csw.linkgenerator.lucene", context);
		SearchResults searchResults = lucene.getSearchResults(query, "de, en");
		
		Iterator<SearchResult> results = searchResults.getResults(1, 10).iterator();
		
		// optimized (only one hasNext() check per iteration)
		if (results.hasNext()) {
			for (;;) {
				SearchResult searchResult = results.next();
				out.write(urlEncode(searchResult.getSpace()));
				out.write('/');
				out.write(urlEncode(searchResult.getName()));
				if (results.hasNext()) {
					out.write('\n');
				} else {
					break;
				}
			}
		}
		
		return true;
	}

	private String urlEncode(String name) {
	    try {
		return URLEncoder.encode(name, "UTF-8");
	    } catch (UnsupportedEncodingException e) {
		throw new RuntimeException("UTF-8 not found: this should not happen");
	    }
	}
}

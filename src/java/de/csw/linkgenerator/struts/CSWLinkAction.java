/**
 * 
 */
package de.csw.linkgenerator.struts;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.web.XWikiAction;
import com.xpn.xwiki.web.XWikiRequest;

import de.csw.linkgenerator.plugin.lucene.LucenePluginApi;
import de.csw.linkgenerator.plugin.lucene.SearchResult;
import de.csw.linkgenerator.plugin.lucene.SearchResults;
import de.csw.util.URLEncoder;

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
				out.write(URLEncoder.encode(searchResult.getSpace()));
				out.write('/');
				out.write(URLEncoder.encode(searchResult.getName()));
				if (results.hasNext()) {
					out.write('\n');
				} else {
					break;
				}
			}
		}
		
		return true;
	}
}

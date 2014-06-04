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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.surround.parser.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.Version;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.notify.DocChangeRule;
import com.xpn.xwiki.notify.XWikiActionRule;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;

/**
 * A plugin offering support for advanced searches using Lucene, a high performance, open source
 * search engine. It uses an {@link IndexUpdater} to monitor and submit wiki pages for indexing to
 * the Lucene engine, and offers simple methods for searching documents, with the possiblity to sort
 * by one or several document fields (besides the default sort by relevance), filter by one or
 * several languages, and search in one, several or all virtual wikis.
 * 
 * @version $Id: $
 */
public class LucenePlugin extends XWikiDefaultPlugin implements XWikiPluginInterface
{
    public static final String DOCTYPE_WIKIPAGE = "wikipage";

    public static final String DOCTYPE_OBJECTS = "objects";

    public static final String DOCTYPE_ATTACHMENT = "attachment";

    public static final String PROP_INDEX_DIR = "de.csw.linkgenerator.plugin.lucene.indexdir";

    public static final String PROP_ANALYZER = "de.csw.linkgenerator.plugin.lucene.analyzer";

    public static final String PROP_INDEXING_INTERVAL = "de.csw.linkgenerator.plugin.lucene.indexinterval";

    public static final String PROP_MAX_QUEUE_SIZE = "de.csw.linkgenerator.plugin.lucene.maxQueueSize";

    private static final String DEFAULT_ANALYZER =
        "org.apache.lucene.analysis.standard.StandardAnalyzer";

    private static final Log LOG = LogFactory.getLog(LucenePlugin.class);

    /**
     * The Lucene text analyzer, can be configured in <tt>xwiki.cfg</tt> using the key
     * {@link #PROP_ANALYZER} (<tt>xwiki.plugins.lucene.analyzer</tt>).
     */
    private Analyzer analyzer;

    /**
     * Lucene index updater. Listens for changes and indexes wiki documents in a separate thread.
     */
    private IndexUpdater indexUpdater;

    /** The thread running the index updater. */
    private Thread indexUpdaterThread;

    protected Properties config;

    /**
     * List of Lucene indexes used for searching. By default there is only one such index for all
     * the wiki. One searches is created for each entry in {@link #indexDirs}.
     */
    private IndexSearcher[] searchers;

    /**
     * Comma separated list of directories holding Lucene index data. The first such directory is
     * used by the internal indexer. Can be configured in <tt>xwiki.cfg</tt> using the key
     * {@link #PROP_INDEX_DIR} (<tt>xwiki.plugins.lucene.indexdir</tt>). If no directory is
     * configured, then a subdirectory <tt>lucene</tt> in the application's work directory is
     * used.
     */
    private String indexDirs;

    private IndexRebuilder indexRebuilder;

    public DocChangeRule docChangeRule = null;

    public XWikiActionRule xwikiActionRule = null;

    public LucenePlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#finalize()
     */
    protected void finalize() throws Throwable
    {
        LOG.error("Lucene plugin will exit !");
        if (indexUpdater != null) {
            indexUpdater.doExit();
        }

        super.finalize();
    }

    public synchronized int rebuildIndex(XWikiContext context)
    {
        return indexRebuilder.startRebuildIndex(context);
    }

    /**
     * Allows to search special named lucene indexes without having to configure them in
     * <tt>xwiki.cfg</tt>. Slower than
     * {@link #getSearchResults(String, String, String, String, XWikiContext)} since new index
     * searcher instances are created for every query.
     * 
     * @param query The base query, using the query engine supported by Lucene.
     * @param myIndexDirs Comma separated list of directories containing the lucene indexes to
     *            search.
     * @param languages Comma separated list of language codes to search in, may be <tt>null</tt>
     *            or empty to search all languages.
     * @param context The context of the request.
     * @return The list of search results.
     * @throws Exception If the index directories cannot be read, or the query is invalid.
     */
    public SearchResults getSearchResultsFromIndexes(String query, String myIndexDirs,
        String languages, XWikiContext context) throws Exception
    {
        IndexSearcher[] mySearchers = createSearchers(myIndexDirs);
        SearchResults retval =
            search(query, (String) null, (String) null, languages, mySearchers, context);
//        closeSearchers(mySearchers);

        return retval;
    }

    /**
     * Allows to search special named lucene indexes without having to configure them in xwiki.cfg.
     * Slower than {@link #getSearchResults}since new index searcher instances are created for
     * every query.
     * 
     * @param query The base query, using the query engine supported by Lucene.
     * @param sortFields A list of fields to sort results by. For each field, if the name starts
     *            with '-', then that field (excluding the -) is used for reverse sorting. If
     *            <tt>null</tt> or empty, sort by hit score.
     * @param myIndexDirs Comma separated list of directories containing the lucene indexes to
     *            search.
     * @param languages Comma separated list of language codes to search in, may be <tt>null</tt>
     *            or empty to search all languages.
     * @param context The context of the request.
     * @return The list of search results.
     * @throws Exception If the index directories cannot be read, or the query is invalid.
     */
    public SearchResults getSearchResultsFromIndexes(String query, String[] sortFields,
        String myIndexDirs, String languages, XWikiContext context) throws Exception
    {
        IndexSearcher[] mySearchers = createSearchers(myIndexDirs);
        SearchResults retval = search(query, sortFields, null, languages, mySearchers, context);
//        closeSearchers(mySearchers);

        return retval;
    }

    /**
     * Allows to search special named lucene indexes without having to configure them in
     * <tt>xwiki.cfg</tt>. Slower than
     * {@link #getSearchResults(String, String, String, String, XWikiContext)} since new index
     * searcher instances are created for every query.
     * 
     * @param query The base query, using the query engine supported by Lucene.
     * @param sortField The name of a field to sort results by. If the name starts with '-', then
     *            the field (excluding the -) is used for reverse sorting. If <tt>null</tt> or
     *            empty, sort by hit score.
     * @param myIndexDirs Comma separated list of directories containing the lucene indexes to
     *            search.
     * @param languages Comma separated list of language codes to search in, may be <tt>null</tt>
     *            or empty to search all languages.
     * @param context The context of the request.
     * @return The list of search results.
     * @throws Exception If the index directories cannot be read, or the query is invalid.
     */
    public SearchResults getSearchResultsFromIndexes(String query, String sortField,
        String myIndexDirs, String languages, XWikiContext context) throws Exception
    {
        IndexSearcher[] mySearchers = createSearchers(myIndexDirs);
        SearchResults retval = search(query, sortField, null, languages, mySearchers, context);
//        closeSearchers(mySearchers);

        return retval;
    }

    /**
     * Searches all Indexes configured in <tt>xwiki.cfg</tt> (property
     * <code>xwiki.plugins.lucene.indexdir</code>).
     * 
     * @param query The base query, using the query engine supported by Lucene.
     * @param sortField The name of a field to sort results by. If the name starts with '-', then
     *            the field (excluding the -) is used for reverse sorting. If <tt>null</tt> or
     *            empty, sort by hit score.
     * @param virtualWikiNames Comma separated list of virtual wiki names to search in, may be
     *            <tt>null</tt> to search all virtual wikis.
     * @param languages Comma separated list of language codes to search in, may be <tt>null</tt>
     *            or empty to search all languages.
     * @return The list of search results.
     * @param context The context of the request.
     * @throws Exception If the index directories cannot be read, or the query is invalid.
     */
    public SearchResults getSearchResults(String query, String sortField,
        String virtualWikiNames, String languages, XWikiContext context) throws Exception
    {
        // TODO Why is this here? This is slow, as it closes and opens indexes for each query.
        // openSearchers();
        return search(query, sortField, virtualWikiNames, languages, this.searchers, context);
    }

    /**
     * Searches all Indexes configured in <tt>xwiki.cfg</tt> (property
     * <code>xwiki.plugins.lucene.indexdir</code>).
     * 
     * @param query The base query, using the query engine supported by Lucene.
     * @param sortField The name of a field to sort results by. If the name starts with '-', then
     *            the field (excluding the -) is used for reverse sorting. If <tt>null</tt> or
     *            empty, sort by hit score.
     * @param virtualWikiNames Comma separated list of virtual wiki names to search in, may be
     *            <tt>null</tt> to search all virtual wikis.
     * @param languages Comma separated list of language codes to search in, may be <tt>null</tt>
     *            or empty to search all languages.
     * @return The list of search results.
     * @param context The context of the request.
     * @throws Exception If the index directories cannot be read, or the query is invalid.
     */
    public SearchResults getSearchResults(String query, String[] sortField,
        String virtualWikiNames, String languages, XWikiContext context) throws Exception
    {
        return search(query, sortField, virtualWikiNames, languages, this.searchers, context);
    }

    /**
     * Creates and submits a query to the Lucene engine.
     * 
     * @param query The base query, using the query engine supported by Lucene.
     * @param sortField The name of a field to sort results by. If the name starts with '-', then
     *            the field (excluding the -) is used for reverse sorting. If <tt>null</tt> or
     *            empty, sort by hit score.
     * @param virtualWikiNames Comma separated list of virtual wiki names to search in, may be
     *            <tt>null</tt> to search all virtual wikis.
     * @param languages Comma separated list of language codes to search in, may be <tt>null</tt>
     *            or empty to search all languages.
     * @param indexes List of Lucene indexes (searchers) to search.
     * @param context The context of the request.
     * @return The list of search results.
     * @throws IOException If the Lucene searchers encounter a problem reading the indexes.
     * @throws ParseException If the query is not valid.
     */
    private SearchResults search(String query, String sortField, String virtualWikiNames,
        String languages, IndexSearcher[] indexes, XWikiContext context) throws IOException,
        org.apache.lucene.queryparser.classic.ParseException
    {
        SortField sort = getSortField(sortField);
        // Perform the actual search
        return search(query, (sort != null) ? new Sort(sort) : null, virtualWikiNames, languages,
            indexes, context);
    }

    /**
     * Creates and submits a query to the Lucene engine.
     * 
     * @param query The base query, using the query engine supported by Lucene.
     * @param sortFields A list of fields to sort results by. For each field, if the name starts
     *            with '-', then that field (excluding the -) is used for reverse sorting. If
     *            <tt>null</tt> or empty, sort by hit score.
     * @param virtualWikiNames Comma separated list of virtual wiki names to search in, may be
     *            <tt>null</tt> to search all virtual wikis.
     * @param languages Comma separated list of language codes to search in, may be <tt>null</tt>
     *            or empty to search all languages.
     * @param indexes List of Lucene indexes (searchers) to search.
     * @param context The context of the request.
     * @return The list of search results.
     * @throws IOException If the Lucene searchers encounter a problem reading the indexes.
     * @throws ParseException If the query is not valid.
     */
    private SearchResults search(String query, String[] sortFields, String virtualWikiNames,
        String languages, IndexSearcher[] indexes, XWikiContext context) throws IOException,
        org.apache.lucene.queryparser.classic.ParseException
    {
        // Turn the sorting field names into SortField objects.
        SortField[] sorts = null;
        if (sortFields != null && sortFields.length > 0) {
            sorts = new SortField[sortFields.length];
            for (int i = 0; i < sortFields.length; ++i) {
                sorts[i] = getSortField(sortFields[i]);
            }
            // Remove any null values from the list.
            int prevLength = -1;
            while (prevLength != sorts.length) {
                prevLength = sorts.length;
                sorts = (SortField[]) ArrayUtils.removeElement(sorts, null);
            }
        }
        // Perform the actual search
        return search(query, (sorts != null) ? new Sort(sorts) : null, virtualWikiNames,
            languages, indexes, context);
    }

    /**
     * Creates and submits a query to the Lucene engine.
     * 
     * @param query The base query, using the query engine supported by Lucene.
     * @param sort A Lucene sort object, can contain one or more sort criterias. If <tt>null</tt>,
     *            sort by hit score.
     * @param virtualWikiNames Comma separated list of virtual wiki names to search in, may be
     *            <tt>null</tt> to search all virtual wikis.
     * @param languages Comma separated list of language codes to search in, may be <tt>null</tt>
     *            or empty to search all languages.
     * @param indexes List of Lucene indexes (searchers) to search.
     * @param context The context of the request.
     * @return The list of search results.
     * @throws IOException If the Lucene searchers encounter a problem reading the indexes.
     * @throws ParseException If the query is not valid.
     */
    private SearchResults search(String query, Sort sort, String virtualWikiNames,
        String languages, IndexSearcher[] indexes, XWikiContext context) throws IOException,
        org.apache.lucene.queryparser.classic.ParseException
    {
//        MultiSearcher searcher = new MultiSearcher(indexes);
    	IndexReader[] readers = new IndexReader[indexes.length];
    	for (int i = 0; i < readers.length; i++)
		{
			readers[i] = indexes[i].getIndexReader();
		}
    	
        IndexSearcher searcher = new IndexSearcher(new MultiReader(readers));
        // Enhance the base query with wiki names and languages.
        Query q = buildQuery(query, virtualWikiNames, languages);

        TopDocsCollector< ? extends ScoreDoc> topDocs;
        if (sort != null) {
            topDocs = TopFieldCollector.create(sort, 5, true, true, false, false);
        } else {
            topDocs = TopScoreDocCollector.create(5, false);
        }
        
        // Perform the actual search
        searcher.search(q, topDocs);
        
        // Transform the raw Lucene search results into XWiki-aware results
        return new SearchResults(topDocs, searcher,
            new com.xpn.xwiki.api.XWiki(context.getWiki(), context),
            context);
    }

    /**
     * Create a {@link SortField} corresponding to the field name. If the field name starts with
     * '-', then the field (excluding the leading -) will be used for reverse sorting.
     * 
     * @param sortField The name of the field to sort by. If <tt>null</tt>, return a
     *            <tt>null</tt> SortField. If starts with '-', then return a SortField that does a
     *            reverse sort on the field.
     * @return A SortFiled that sorts on the given field, or <tt>null</tt>.
     */
    private SortField getSortField(String sortField)
    {
        SortField sort = null;
        if (!StringUtils.isEmpty(sortField)) {
            if (sortField.startsWith("-")) {
                sort = new SortField(sortField.substring(1), SortField.Type.STRING, true);
            } else {
                sort = new SortField(sortField, SortField.Type.STRING);
            }
        }
        return sort;
    }

    /**
     * @param query
     * @param virtualWikiNames comma separated list of virtual wiki names
     * @param languages comma separated list of language codes to search in, may be null to search
     *            all languages
     */
    private Query buildQuery(String query, String virtualWikiNames, String languages)
        throws org.apache.lucene.queryparser.classic.ParseException
    {
        // build a query like this: <user query string> AND <wikiNamesQuery> AND
        // <languageQuery>
        BooleanQuery bQuery = new BooleanQuery();
        Query parsedQuery = null;

        // for object search
        if (query.startsWith("PROP ")) {
            String property = query.substring(0, query.indexOf(":"));
            query = query.substring(query.indexOf(":") + 1, query.length());
            QueryParser qp = new QueryParser(Version.LUCENE_40, property, analyzer);
            parsedQuery = qp.parse(query);
            bQuery.add(parsedQuery, BooleanClause.Occur.MUST);
        } else if (query.startsWith("MULTI ")) {
            // for fulltext search
            List<String> fieldList = IndexUpdater.fields;
            String[] fields = fieldList.toArray(new String[fieldList.size()]);
            BooleanClause.Occur[] flags = new BooleanClause.Occur[fields.length];
            for (int i = 0; i < flags.length; i++) {
                flags[i] = BooleanClause.Occur.SHOULD;
            }
            parsedQuery = MultiFieldQueryParser.parse(Version.LUCENE_40, query, fields, flags, analyzer);
            bQuery.add(parsedQuery, BooleanClause.Occur.MUST);
        } else {
            QueryParser qp = new QueryParser(Version.LUCENE_40, "ft", analyzer);
            parsedQuery = qp.parse(query);
            bQuery.add(parsedQuery, BooleanClause.Occur.MUST);
        }

        if (virtualWikiNames != null && virtualWikiNames.length() > 0) {
            bQuery.add(buildOredTermQuery(virtualWikiNames, IndexFields.DOCUMENT_WIKI),
                BooleanClause.Occur.MUST);
        }
        if (languages != null && languages.length() > 0) {
            bQuery.add(buildOredTermQuery(languages, IndexFields.DOCUMENT_LANGUAGE),
                BooleanClause.Occur.SHOULD);
        }

        return bQuery;
    }

    /**
     * @param values comma separated list of values to look for
     * @return A query returning documents matching one of the given values in the given field
     */
    private Query buildOredTermQuery(final String values, final String fieldname)
    {
        String[] valueArray = values.split("\\,");
        if (valueArray.length > 1) {
            // build a query like this: <valueArray[0]> OR <valueArray[1]> OR ...
            BooleanQuery orQuery = new BooleanQuery();
            for (int i = 0; i < valueArray.length; i++) {
                orQuery.add(new TermQuery(new Term(fieldname, valueArray[i].trim())),
                    BooleanClause.Occur.SHOULD);
            }
            return orQuery;
        }

        // exactly one value, no OR'ed Terms necessary
        return new TermQuery(new Term(fieldname, valueArray[0]));
    }

    @SuppressWarnings("unchecked")
	public synchronized void init(XWikiContext context)
    {
        super.init(context);
        if (LOG.isDebugEnabled()) {
            LOG.debug("lucene plugin: in init");
        }
        config = context.getWiki().getConfig();
        try {
            Class<Analyzer> analyzerClass = (Class<Analyzer>) Class.forName(config.getProperty(PROP_ANALYZER, DEFAULT_ANALYZER));
            Constructor<Analyzer> constructor = analyzerClass.getConstructor(Version.class);
            analyzer = constructor.newInstance(Version.LUCENE_40);
        } catch (Exception e) {
            LOG.error("error instantiating analyzer : ", e);
            LOG.warn("using default analyzer class: " + DEFAULT_ANALYZER);
            try {
                Class<Analyzer> analyzerClass = (Class<Analyzer>) Class.forName(DEFAULT_ANALYZER);
                Constructor<Analyzer> constructor = analyzerClass.getConstructor(Version.class);
                analyzer = constructor.newInstance(Version.LUCENE_40);
            } catch (Exception e1) {
                throw new RuntimeException("instantiation of default analyzer "
                    + DEFAULT_ANALYZER + " failed", e1);
            }
        }
        this.indexDirs = config.getProperty(PROP_INDEX_DIR);
        if (indexDirs == null || indexDirs.equals("")) {
            File workDir = context.getWiki().getWorkSubdirectory("lucene_linkgenerator", context);
            indexDirs = workDir.getAbsolutePath();
        }
        indexUpdater = new IndexUpdater();
        indexUpdater.setAnalyzer(analyzer);
        try
		{
			indexUpdater.init(config, this, context);
		} catch (IOException e)
		{
			LOG.fatal(e.getMessage());
		}
        indexUpdaterThread = new Thread(indexUpdater, "Lucene Index Updater");
        indexUpdaterThread.start();
        indexRebuilder = new IndexRebuilder(indexUpdater, context);

        docChangeRule = new DocChangeRule(indexUpdater);
        xwikiActionRule = new XWikiActionRule(indexUpdater);

        openSearchers();

        context.getWiki().getNotificationManager().addGeneralRule(docChangeRule);
        context.getWiki().getNotificationManager().addGeneralRule(xwikiActionRule);
        LOG.info("lucene plugin initialized.");
    }

    public void flushCache(XWikiContext context)
    {
        context.getWiki().getNotificationManager().removeGeneralRule(xwikiActionRule);
        context.getWiki().getNotificationManager().removeGeneralRule(docChangeRule);

        indexRebuilder = null;

        indexUpdaterThread.stop();

//        try {
//            closeSearchers(this.searchers);
//        } catch (IOException e) {
//            LOG.warn("cannot close searchers");
//        }
        indexUpdater = null;
        analyzer = null;

        init(context);
    }

    public String getName()
    {
        return "csw.linkgenerator.lucene";
    }

    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context)
    {
        return new LucenePluginApi((LucenePlugin) plugin, context);
    }

    /**
     * Creates an array of Searchers for a number of lucene indexes.
     * 
     * @param indexDirs Comma separated list of Lucene index directories to create searchers for.
     * @return Array of searchers
     */
    public IndexSearcher[] createSearchers(String indexDirs) throws Exception
    {
        String[] dirPaths = StringUtils.split(indexDirs, ",");
        NIOFSDirectory[] dirs = new NIOFSDirectory[dirPaths.length];
        for (int i = 0; i < dirPaths.length; i++)
		{
			dirs[i] = new NIOFSDirectory(new File(dirPaths[i]));
		}
        List<IndexSearcher> searchersList = new ArrayList<IndexSearcher>();
        for (int i = 0; i < dirs.length; i++) {
            try {
                if (!DirectoryReader.indexExists(dirs[i])) {
                    // If there's no index there, create an empty one; otherwise the reader
                    // constructor will throw an exception and fail to initialize
                	IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_40, analyzer);
                    new IndexWriter(dirs[i], conf).close();
                }
                DirectoryReader reader = DirectoryReader.open(dirs[i]);
                searchersList.add(new IndexSearcher(reader));
            } catch (IOException e) {
                LOG.error("cannot open index " + dirs[i], e);
            }
        }

        return searchersList.toArray(new IndexSearcher[searchersList.size()]);
    }

    /**
     * Opens the searchers for the configured index Dirs after closing any already existing ones.
     */
    protected synchronized void openSearchers()
    {
        try {
//            closeSearchers(this.searchers);
            this.searchers = createSearchers(indexDirs);
        } catch (Exception e1) {
            LOG.error("error opening searchers for index dirs "
                + config.getProperty(PROP_INDEX_DIR), e1);
            throw new RuntimeException("error opening searchers for index dirs "
                + config.getProperty(PROP_INDEX_DIR), e1);
        }
    }

    /**
     * @throws IOException
     * @Deprecated see http://lucene.472066.n3.nabble.com/IndexSearcher-close-removed-in-4-0-td4041177.html
     */
    protected static void closeSearchers(IndexSearcher[] searchers) throws IOException
    {
//        if (searchers != null) {
//            for (int i = 0; i < searchers.length; i++) {
//                if (searchers[i] != null) {
//                    searchers[i].close();
//                }
//            }
//        }
    }

    public String getIndexDirs()
    {
        return indexDirs;
    }

    public long getQueueSize()
    {
        return indexUpdater.getQueueSize();
    }

    public void queueDocument(XWikiDocument doc, XWikiContext context)
    {
        indexUpdater.add(doc, context);
    }

    public void queueAttachment(XWikiDocument doc, XWikiAttachment attach, XWikiContext context)
    {
        indexUpdater.add(doc, attach, context);
    }

    public void queueAttachment(XWikiDocument doc, XWikiContext context)
    {
        indexUpdater.addAttachmentsOfDocument(doc, context);
    }

    /**
     * @return the number of documents Lucene index writer.
     */
    public long getLuceneDocCount()
    {
        return indexUpdater.getLuceneDocCount();
    }

    /**
     * @return the number of documents in the second queue gave to Lucene.
     */
    public long getActiveQueueSize()
    {
        return indexUpdater.getActiveQueueSize();
    }
}

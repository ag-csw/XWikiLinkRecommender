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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.MDC;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.LogDocMergePolicy;
import org.apache.lucene.index.LogMergePolicy;
import org.apache.lucene.index.MergePolicy;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.Version;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.notify.XWikiActionNotificationInterface;
import com.xpn.xwiki.notify.XWikiDocChangeNotificationInterface;
import com.xpn.xwiki.notify.XWikiNotificationRule;

import de.csw.linkgenerator.plugin.lucene.AbstractXWikiRunnable;
import de.csw.linkgenerator.plugin.lucene.AttachmentData;
import de.csw.linkgenerator.plugin.lucene.DocumentData;
import de.csw.linkgenerator.plugin.lucene.IndexData;
import de.csw.linkgenerator.plugin.lucene.IndexRebuilder;
import de.csw.linkgenerator.plugin.lucene.LucenePlugin;
import de.csw.linkgenerator.plugin.lucene.ObjectData;
import de.csw.linkgenerator.plugin.lucene.XWikiDocumentQueue;

/**
 * @version $Id: $
 */
public class IndexUpdater extends AbstractXWikiRunnable 
    implements XWikiDocChangeNotificationInterface, XWikiActionNotificationInterface
{
    /** Logging helper. */
    private static final Log LOG = LogFactory.getLog(IndexUpdater.class);

    /** Milliseconds of sleep between checks for changed documents. */
    private int indexingInterval = 30000;

    private boolean exit = false;

    private IndexWriter writer;

    private Directory indexDir;

    private XWikiDocumentQueue queue = new XWikiDocumentQueue();

    /**
     * Soft threshold after which no more documents will be added to the indexing queue. When the
     * queue size gets larger than this value, the index rebuilding thread will sleep chuks of
     * {@link IndexRebuilder#retryInterval} milliseconds until the queue size will get back bellow
     * this threshold. This does not affect normal indexing through wiki updates.
     */
    public int maxQueueSize = 1000;

    private Analyzer analyzer;

    private LucenePlugin plugin;

    private IndexSearcher searcher;

    private DirectoryReader reader;

    private XWikiContext context;

    private XWiki xwiki;

    private long activesIndexedDocs = 0;

    static List<String> fields = new ArrayList<String>();

    public boolean needInitialBuild = false;

    public void doExit()
    {
        exit = true;
    }

    /**
     * Main loop. Polls the queue for documents to be indexed.
     * 
     * @see java.lang.Runnable#run()
     */
    public void run()
    {
        MDC.put("url", "Lucene index updating thread");

        // Since this is where a new thread is created this is where we need to initialize the Container 
        // ThreadLocal variables and not in the init() method. Otherwise we would simply overwrite the
        // Container values for the main thread...
        try {
            initXWikiContainer(this.context);
            runMainLoop();
        } finally {
            // Cleanup Container component (it has ThreadLocal variables)
            cleanupXWikiContainer(this.context);
            this.xwiki.getStore().cleanUp(this.context);
            MDC.remove("url");
        }
    }

    /**
     * Main loop. Polls the queue for documents to be indexed.
     */
    private void runMainLoop()
    {
        while (!this.exit) {
            if (this.queue.isEmpty()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("IndexUpdater: queue empty, nothing to do");
                }
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("IndexUpdater: documents in queue, start indexing");
                }

                Map<String, IndexData> toIndex = new HashMap<String, IndexData>();
                List<Integer> toDelete = new ArrayList<Integer>();
                activesIndexedDocs = 0;

                try {
                    openSearcher();
                    while (!this.queue.isEmpty()) {
                        IndexData data = this.queue.remove();
                        List<Integer> oldDocs = getOldIndexDocIds(data);
                        if (oldDocs != null) {
                            for (Integer id : oldDocs) {
                                if (LOG.isDebugEnabled()) {
                                    LOG.debug("Adding " + id + " to remove list");
                                }

                                if (!toDelete.contains(id)) {
                                    toDelete.add(id);
                                } else {
                                    if (LOG.isDebugEnabled()) {
                                        LOG.debug("Found " + id
                                            + " already in list while adding it to remove list");
                                    }
                                }
                            }
                        }

                        String id = data.getId();
                        LOG.debug("Adding " + id + " to index list");
                        if (toIndex.containsKey(id)) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Found " + id
                                    + " already in list while adding it to index list");
                            }
                            toIndex.remove(id);
                        }
                        ++activesIndexedDocs;
                        toIndex.put(id, data);
                    }
                } catch (Exception e) {
                    LOG.error("error preparing index queue", e);
                } finally {
                    closeSearcher();
                }

                // Let's delete
                try {
                    openSearcher();
                    if (LOG.isInfoEnabled()) {
                        LOG.info("deleting " + toDelete.size() + " docs from lucene index");
                    }
                    int nb = deleteOldDocs(toDelete);
                    if (LOG.isInfoEnabled()) {
                        LOG.info("deleted " + nb + " docs from lucene index");
                    }
                } catch (Exception e) {
                    LOG.error("error deleting previous documents", e);
                } finally {
                    closeSearcher();
                }

                // Let's index
                try {
                    if (LOG.isInfoEnabled()) {
                        LOG.info("indexing " + toIndex.size() + " docs to lucene index");
                    }

                    XWikiContext context = (XWikiContext) this.context.clone();
                    context.getWiki().getStore().cleanUp(context);
                    openWriter(OpenMode.APPEND);

                    int nb = 0;
                    for (Map.Entry<String, IndexData> entry : toIndex.entrySet()) {
                        String id = entry.getKey();
                        IndexData data = entry.getValue();

                        try {
                            XWikiDocument doc =
                                this.xwiki.getDocument(data.getFullName(), context);

                            if (data.getLanguage() != null && !data.getLanguage().equals("")) {
//                                doc = doc.getTranslatedDocument(data.getLanguage(), context);
                                doc.getTranslatedDocument(new Locale(data.getLanguage()), context);
                            }

                            addToIndex(data, doc, context);
                            ++nb;
                            --activesIndexedDocs;
                        } catch (Exception e) {
                            LOG.error("error indexing document " + id, e);
                        }
                    }

                    if (LOG.isInfoEnabled()) {
                        LOG.info("indexed " + nb + " docs to lucene index");
                    }

                    writer.commit();
                } catch (Exception e) {
                    LOG.error("error indexing documents", e);
                } finally {
                    this.context.getWiki().getStore().cleanUp(this.context);
                    closeWriter();
                }

                plugin.openSearchers();
            }
            try {
                Thread.sleep(indexingInterval);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }        
    }

    private synchronized void closeSearcher()
    {
        try {
//            if (this.searcher != null) {
//                this.searcher.close();
//            }
            if (this.reader != null) {
                this.reader.close();
            }
        } catch (IOException e) {
            LOG.error("error closing index searcher", e);
        } finally {
            this.searcher = null;
            this.reader = null;
        }
    }

    /**
     * Opens the index reader and searcher used for finding and deleting old versions of indexed
     * documents.
     */
    private synchronized void openSearcher()
    {
        try {
            this.reader = DirectoryReader.open(this.indexDir);
            this.searcher = new IndexSearcher(this.reader);
        } catch (IOException e) {
            LOG.error("error opening index searcher", e);
        }
    }

    /**
     * Deletes the documents with the given ids from the index.
     */
    private int deleteOldDocs(List<Integer> oldDocs)
    {
        int nb = 0;

        if (writer == null) {
        	openWriter(OpenMode.APPEND);
        }
        
        for (Integer id : oldDocs) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("delete doc " + id);
            }

            try {
                boolean tryDeleteByIdResult = this.writer.tryDeleteDocument(reader, id);
                if (!tryDeleteByIdResult) {
                    final String documentId = this.reader.document(id).get(IndexFields.DOCUMENT_ID);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("could not delete document by id " + id + "; try term for "
                                + documentId + " instead");
                    }
                    if (documentId !=null) {
                        Term docTerm = new Term(IndexFields.DOCUMENT_ID, documentId);
                        writer.deleteDocuments(docTerm);
                    }
                }
                nb++;
            } catch (IOException e1) {
                LOG.error("error deleting doc " + id, e1);
            }
        }
        
        closeWriter();

        return nb;
    }

    private List<Integer> getOldIndexDocIds(IndexData data)
    {
        List<Integer> retval = new ArrayList<Integer>(3);
        Query query = data.buildQuery();
        try {
            TopDocs hits = this.searcher.search(query, Integer.MAX_VALUE);
            for (int i = 0; i < hits.totalHits; i++) {
//                retval.add(new Integer(hits.id(i)));
                retval.add(new Integer(hits.scoreDocs[i].doc));
            }
        } catch (Exception e) {
            LOG.error(String.format(
                "Error looking for old versions of document [%s] with query [%s]", data, query),
                e);
        }

        return retval;
    }

    private void openWriter(OpenMode openMode)
    {
        if (writer != null) {
            LOG.error("Writer already open and createWriter called");
            return;
        }

        try {
            // fix for windows by Daniel Cortes:
//            FSDirectory f = FSDirectory.getDirectory(indexDir);
        	IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_40, analyzer);
        	conf.setOpenMode(openMode);
        	
        	// Ralph: This is kind of guesswork
        	LogDocMergePolicy mergePolicy = new LogDocMergePolicy();
        	mergePolicy.setUseCompoundFile(true);
        	conf.setMergePolicy(mergePolicy);
            // writer = new IndexWriter (indexDir, analyzer, create);
            writer = new IndexWriter(indexDir, conf);
//            writer.setUseCompoundFile(true);

            if (LOG.isDebugEnabled()) {
                LOG.debug("successfully opened index writer : " + indexDir);
            }
        } catch (IOException e) {
            LOG.error("IOException when opening Lucene Index for writing at " + indexDir, e);
        }
    }

    private void closeWriter()
    {
        if (this.writer == null) {
            LOG.error("Writer not open and closeWriter called");
            return;
        }

        // Ralph: see http://blog.trifork.com/2011/11/21/simon-says-optimize-is-bad-for-you/
//        try {
//            this.writer.optimize();
//        } catch (IOException e1) {
//            LOG.error("Exception caught when optimizing Index", e1);
//        }

        try {
            this.writer.close();
        } catch (Exception e) {
            LOG.error("Exception caught when closing IndexWriter", e);
        }

        this.writer = null;

        if (LOG.isDebugEnabled()) {
            LOG.debug("closed writer.");
        }
    }

    private void addToIndex(IndexData data, XWikiDocument doc, XWikiContext context)
        throws IOException
    {
        if (LOG.isDebugEnabled()) {
            LOG.debug("addToIndex: " + data);
        }

        org.apache.lucene.document.Document luceneDoc = new org.apache.lucene.document.Document();
        data.addDataToLuceneDocument(luceneDoc, doc, context);
        IndexableField fld = null;

        // collecting all the fields for using up in search
        for (Iterator<IndexableField> it = luceneDoc.getFields().iterator(); it.hasNext();) {
            fld = it.next();
            if (!fields.contains(fld.name())) {
                fields.add(fld.name());
            }
        }

        this.writer.addDocument(luceneDoc);
    }

    /**
     * @param indexDir The indexDir to set.
     */
    public void setIndexDir(Directory indexDir)
    {
        this.indexDir = indexDir;
    }

    /**
     * @param analyzer The analyzer to set.
     */
    public void setAnalyzer(Analyzer analyzer)
    {
        this.analyzer = analyzer;
    }

    public synchronized void init(Properties config, LucenePlugin plugin, XWikiContext context) throws IOException
    {
        this.xwiki = context.getWiki();
        this.context = (XWikiContext) context.clone();
        this.context.setDatabase(this.context.getMainXWiki());
        this.plugin = plugin;
        // take the first configured index dir as the one for writing
        // String[] indexDirs =
        // StringUtils.split(config.getProperty(LucenePlugin.PROP_INDEX_DIR), "
        // ,");
        String[] dirPaths = StringUtils.split(plugin.getIndexDirs(), ",");

        if (dirPaths != null && dirPaths.length > 0) {
        	try {
        		this.indexDir = new NIOFSDirectory(new File(dirPaths[0]));
        	} catch (IOException e) {
        		IOException up = new IOException("Cannot create index in " + dirPaths[0], e);
        		throw up; // he he
        	}
            File f = new File(dirPaths[0]);
            if (!f.isDirectory()) {
                f.mkdirs();
                this.needInitialBuild = true;
            }
            if (!DirectoryReader.indexExists(indexDir)) {
                this.needInitialBuild = true;
            }
        }

        this.indexingInterval =
            1000 * Integer
                .parseInt(config.getProperty(LucenePlugin.PROP_INDEXING_INTERVAL, "30"));
        this.maxQueueSize =
            Integer.parseInt(config.getProperty(LucenePlugin.PROP_MAX_QUEUE_SIZE, "1000"));

        // Note: There's no need to open the Searcher here (with a call to
        // openSearcher()) as each task needing it will open it itself.
    }
    
    public void cleanIndex()
    {
        if (LOG.isInfoEnabled()) {
            LOG.info("trying to clear index for rebuilding");
        }

        while (writer != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("waiting for existing index writer to close");
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        synchronized (this) {
            openWriter(OpenMode.CREATE);
            closeWriter();
        }
    }

    public void add(XWikiDocument document, XWikiContext context)
    {
        this.queue.add(new DocumentData(document, context));

        if (document.hasElement(XWikiDocument.HAS_OBJECTS)) {
            addObject(document, context);
        }
    }

    public void addObject(XWikiDocument document, XWikiContext context)
    {
        this.queue.add(new ObjectData(document, context));
    }

    public void add(XWikiDocument document, XWikiAttachment attachment, XWikiContext context)
    {
        if (document != null && attachment != null && context != null) {
        	// TODO re-enable when attachment extractors are available
//            this.queue.add(new AttachmentData(document, attachment, context));
        } else {
            LOG.error("invalid parameters given to add: " + document + ", " + attachment + ", "
                + context);
        }
    }

    public int addAttachmentsOfDocument(XWikiDocument document, XWikiContext context)
    {
        int retval = 0;

        final List<XWikiAttachment> attachmentList = document.getAttachmentList();
        retval += attachmentList.size();
        for (XWikiAttachment attachment : attachmentList) {
            try {
                add(document, attachment, context);
            } catch (Exception e) {
                LOG.error("error retrieving attachment of document " + document.getFullName(), e);
            }
        }

        return retval;
    }

    /**
     * Notification of changes in document content
     * 
     * @see com.xpn.xwiki.notify.XWikiNotificationInterface#notify(com.xpn.xwiki.notify.XWikiNotificationRule,
     *      com.xpn.xwiki.doc.XWikiDocument,com.xpn.xwiki.doc.XWikiDocument,
     *      int,com.xpn.xwiki.XWikiContext)
     */
    public void notify(XWikiNotificationRule rule, XWikiDocument newDoc, XWikiDocument oldDoc,
        int event, XWikiContext context)
    {
        if (LOG.isDebugEnabled()) {
            LOG.debug("notify from XWikiDocChangeNotificationInterface, event=" + event
                + ", newDoc=" + newDoc + " oldDoc=" + oldDoc);
        }

        try {
            add(newDoc, context);
        } catch (Exception e) {
            LOG.error("error in notify", e);
        }
    }

    /**
     * Notification of attachment uploads.
     * 
     * @see com.xpn.xwiki.notify.XWikiActionNotificationInterface#notify(com.xpn.xwiki.notify.XWikiNotificationRule,
     *      com.xpn.xwiki.doc.XWikiDocument,java.lang.String,com.xpn.xwiki.XWikiContext)
     */
    public void notify(XWikiNotificationRule arg0, XWikiDocument doc, String action,
        XWikiContext context)
    {
        if ("upload".equals(action)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("upload action notification for doc " + doc.getName());
            }

            try {
                // Retrieve the latest version (with the file just attached)
                XWikiDocument basedoc = context.getWiki().getDocument(doc.getFullName(), context);
                List<XWikiAttachment> attachments = basedoc.getAttachmentList();
                // find out the most recently changed attachment
                XWikiAttachment newestAttachment = null;
                for (XWikiAttachment attachment : attachments) {
                    if ((newestAttachment == null)
                        || attachment.getDate().after(newestAttachment.getDate())) {
                        newestAttachment = attachment;
                    }
                }
                add(basedoc, newestAttachment, context);
            } catch (Exception e) {
                LOG.error("error in notify", e);
            }
        }
    }

    /**
     * @return the number of documents in the queue.
     */
    public long getQueueSize()
    {
        return this.queue.getSize();
    }

    /**
     * @return the number of documents Lucene index writer.
     */
    public long getLuceneDocCount()
    {
        if (writer != null)
            return writer.numDocs();

        return -1;
    }

    /**
     * @return the number of documents in the second queue gave to Lucene.
     */
    public long getActiveQueueSize()
    {
        return this.activesIndexedDocs;
    }
}

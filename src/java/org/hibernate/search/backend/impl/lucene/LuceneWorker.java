//$Id$
package org.hibernate.search.backend.impl.lucene;

import java.io.IOException;
import java.io.Serializable;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.search.SearchException;
import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.DeleteLuceneWork;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.backend.Workspace;
import org.hibernate.search.backend.PurgeAllLuceneWork;
import org.hibernate.search.engine.DocumentBuilder;
import org.hibernate.search.store.DirectoryProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stateless implementation that performs a unit of work.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author John Griffin
 */
public class LuceneWorker {
	private Workspace workspace;
	private static final Logger log = LoggerFactory.getLogger( LuceneWorker.class );

	public LuceneWorker(Workspace workspace) {
		this.workspace = workspace;
	}

	public void performWork(WorkWithPayload luceneWork) {
		Class workClass = luceneWork.getWork().getClass();
		if ( AddLuceneWork.class.isAssignableFrom( workClass ) ) {
			performWork( (AddLuceneWork) luceneWork.getWork(), luceneWork.getProvider() );
		}
		else if ( DeleteLuceneWork.class.isAssignableFrom( workClass ) ) {
			performWork( (DeleteLuceneWork) luceneWork.getWork(), luceneWork.getProvider() );
		}
		else if ( OptimizeLuceneWork.class.isAssignableFrom( workClass ) ) {
			performWork( (OptimizeLuceneWork) luceneWork.getWork(), luceneWork.getProvider() );
		}else if ( PurgeAllLuceneWork.class.isAssignableFrom( workClass ) ) {
			performWork( (PurgeAllLuceneWork) luceneWork.getWork(), luceneWork.getProvider() );
		}
		else {
			throw new AssertionFailure( "Unknown work type: " + workClass );
		}
	}

	public void performWork(AddLuceneWork work, DirectoryProvider provider) {
		Class entity = work.getEntityClass();
		Serializable id = work.getId();
		Document document = work.getDocument();
		add( entity, id, document, provider );
	}

	private void add(Class entity, Serializable id, Document document, DirectoryProvider provider) {
		log.trace( "add to Lucene index: {}#{}:{}", new Object[] { entity, id, document } );
		IndexWriter writer = workspace.getIndexWriter( provider, entity, true );
		try {
			writer.addDocument( document );
		}
		catch (IOException e) {
			throw new SearchException( "Unable to add to Lucene index: " + entity + "#" + id, e );
		}
	}

	public void performWork(DeleteLuceneWork work, DirectoryProvider provider) {
		Class entity = work.getEntityClass();
		Serializable id = work.getId();
		remove( entity, id, provider );
	}

	private void remove(Class entity, Serializable id, DirectoryProvider provider) {
		/**
		 * even with Lucene 2.1, use of indexWriter to delete is not an option
		 * We can only delete by term, and the index doesn't have a termt that
		 * uniquely identify the entry. See logic below
		 */
		log.trace( "remove from Lucene index: {}#{}", entity, id );
		DocumentBuilder builder = workspace.getDocumentBuilder( entity );
		Term term = builder.getTerm( id );
		IndexReader reader = workspace.getIndexReader( provider, entity );
		TermDocs termDocs = null;
		try {
			//TODO is there a faster way?
			//TODO include TermDocs into the workspace?
			termDocs = reader.termDocs( term );
			String entityName = entity.getName();
			while ( termDocs.next() ) {
				int docIndex = termDocs.doc();
				if ( entityName.equals( reader.document( docIndex ).get( DocumentBuilder.CLASS_FIELDNAME ) ) ) {
					//remove only the one of the right class
					//loop all to remove all the matches (defensive code)
					reader.deleteDocument( docIndex );
				}
			}
		}
		catch (Exception e) {
			throw new SearchException( "Unable to remove from Lucene index: " + entity + "#" + id, e );
		}
		finally {
			if ( termDocs != null ) try {
				termDocs.close();
			}
			catch (IOException e) {
				log.warn( "Unable to close termDocs properly", e );
			}
		}

	}

	public void performWork(OptimizeLuceneWork work, DirectoryProvider provider) {
		Class entity = work.getEntityClass();
		log.trace( "optimize Lucene index: {}", entity );
		IndexWriter writer = workspace.getIndexWriter( provider, entity, false );
		try {
			writer.optimize();
			workspace.optimize( provider );
		}
		catch (IOException e) {
			throw new SearchException( "Unable to optimize Lucene index: " + entity, e );
		}
	}

	public void performWork(PurgeAllLuceneWork work, DirectoryProvider provider) {
		Class entity = work.getEntityClass();
		log.trace( "purgeAll Lucene index: {}", entity );
		IndexReader reader = workspace.getIndexReader( provider, entity );
		try {
			Term term = new Term( DocumentBuilder.CLASS_FIELDNAME, entity.getName() );
			reader.deleteDocuments( term );
		}
		catch (Exception e) {
			throw new SearchException( "Unable to purge all from Lucene index: " + entity, e );
		}
	}

	public static class WorkWithPayload {
		private LuceneWork work;
		private DirectoryProvider provider;


		public WorkWithPayload(LuceneWork work, DirectoryProvider provider) {
			this.work = work;
			this.provider = provider;
		}


		public LuceneWork getWork() {
			return work;
		}

		public DirectoryProvider getProvider() {
			return provider;
		}
	}
}

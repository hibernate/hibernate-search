package org.hibernate.search.backend.impl.lucene.works;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.slf4j.Logger;

import org.hibernate.search.SearchException;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.Workspace;
import org.hibernate.search.backend.impl.lucene.IndexInteractionType;
import org.hibernate.search.engine.DocumentBuilder;
import org.hibernate.search.util.LoggerFactory;

/**
 * Stateless implementation that performs a <code>DeleteLuceneWork</code>.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author John Griffin
 * @author Sanne Grinovero
 * @see LuceneWorkVisitor
 * @see LuceneWorkDelegate
 */
class DeleteWorkDelegate implements LuceneWorkDelegate {

	private final Workspace workspace;
	private final Logger log = LoggerFactory.make();

	DeleteWorkDelegate(Workspace workspace) {
		this.workspace = workspace;
	}

	public IndexInteractionType getIndexInteractionType() {
		return IndexInteractionType.NEEDS_INDEXWRITER;
	}

	public void performWork(LuceneWork work, IndexWriter writer) {
		log.trace( "Removing {}#{} by query.", work.getEntityClass(), work.getId() );
		DocumentBuilder builder = workspace.getDocumentBuilder( work.getEntityClass() );

		BooleanQuery entityDeltionQuery = new BooleanQuery();

		TermQuery idQueryTerm = new TermQuery( builder.getTerm( work.getId() ) );
		entityDeltionQuery.add(idQueryTerm, BooleanClause.Occur.MUST);

		Term classNameQueryTerm =  new Term(DocumentBuilder.CLASS_FIELDNAME, work.getEntityClass().getName());
		TermQuery classNameQuery = new TermQuery( classNameQueryTerm);
		entityDeltionQuery.add(classNameQuery, BooleanClause.Occur.MUST);

		try {
			writer.deleteDocuments( entityDeltionQuery );
		}
		catch ( Exception e ) {
			String message = "Unable to remove " + work.getEntityClass() + "#" + work.getId() + " from index.";
			throw new SearchException( message, e );
		}
	}

	/*
	 * This method is obsolete and was used pre Lucene 2.4. Now we are using IndexWriter.deleteDocuments(Query) to
	 * delete index documents.
	 *
	 * This method might be deleted at some stage. (hardy)
	 */
	public void performWork(LuceneWork work, IndexReader reader) {
		/**
		 * even with Lucene 2.1, use of indexWriter to delete is not an option
		 * We can only delete by term, and the index doesn't have a term that
		 * uniquely identify the entry. See logic below
		 */
		log.trace( "Removing {}#{} from Lucene index.", work.getEntityClass(), work.getId() );
		DocumentBuilder builder = workspace.getDocumentBuilder( work.getEntityClass() );
		Term term = builder.getTerm( work.getId() );
		TermDocs termDocs = null;
		try {
			//TODO is there a faster way?
			//TODO include TermDocs into the workspace?
			termDocs = reader.termDocs( term );
			String entityName = work.getEntityClass().getName();
			while ( termDocs.next() ) {
				int docIndex = termDocs.doc();
				if ( entityName.equals( reader.document( docIndex ).get( DocumentBuilder.CLASS_FIELDNAME ) ) ) {
					//remove only the one of the right class
					//loop all to remove all the matches (defensive code)
					reader.deleteDocument( docIndex );
				}
			}
			//TODO shouldn't this use workspace.incrementModificationCounter( 1 ) ? 
		}
		catch ( Exception e ) {
			throw new SearchException(
					"Unable to remove from Lucene index: "
							+ work.getEntityClass() + "#" + work.getId(), e
			);
		}
		finally {
			if ( termDocs != null ) {
				try {
					termDocs.close();
				}
				catch ( IOException e ) {
					log.warn( "Unable to close termDocs properly", e );
				}
			}
		}
	}

}

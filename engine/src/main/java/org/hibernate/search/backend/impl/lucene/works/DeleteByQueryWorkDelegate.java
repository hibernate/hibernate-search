/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene.works;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.hibernate.search.backend.DeleteByQueryLuceneWork;
import org.hibernate.search.backend.DeleteByQuerySupport;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.DeletionQuery;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.store.Workspace;
import org.hibernate.search.util.impl.ScopedAnalyzer;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Stateless implementation that performs a <code>DeleteByQueryWork</code>.
 *
 * @author Martin Braun
 * @see LuceneWorkVisitor
 * @see LuceneWorkDelegate
 */
class DeleteByQueryWorkDelegate implements LuceneWorkDelegate {

	private static final Log log = LoggerFactory.make();
	protected final Workspace workspace;

	DeleteByQueryWorkDelegate(Workspace workspace) {
		this.workspace = workspace;
	}

	@Override
	public void performWork(LuceneWork work, IndexWriter writer, IndexingMonitor monitor) {
		DeleteByQueryLuceneWork deleteWork = (DeleteByQueryLuceneWork) work;

		final Class<?> entityType = work.getEntityClass();
		final DeletionQuery query = deleteWork.getDeletionQuery();

		log.tracef( "Removing all %s matching Query: %s", entityType.toString(), query.toString() );

		BooleanQuery entityDeletionQuery = new BooleanQuery();

		// TODO: we could let experts supply their own means to convert back to
		// Queries here by letting users specify a provider-service in the
		// hibernate-properties. However, this could be a risky thing to do for
		// users (users of this feature should definitely be warned then that
		// they will have to worry about serialization problems that might
		// occur!)

		// this could be done via a SerializableQuery type that contains a
		// String representation of a query
		// which could (not necessarily) be a LuceneQueryParser-Query (this
		// could be dangerous though)

		// but if users can supply their own deletion-serialization logic
		// they can also handle problems with different behaving versions
		// by hand by encoding the version of their code that should be in use
		// into the Query (or we could simply put that into our Wrapping
		// Query-Type)
		{
			DeleteByQuerySupport.ToLuceneQuery converter = DeleteByQuerySupport.TO_LUCENE_QUERY_CONVERTER.get( query.getQueryKey() );

			ScopedAnalyzer analyzer = this.workspace.getDocumentBuilder( entityType ).getAnalyzer();
			Query queryToDelete = converter.build( query, analyzer );

			entityDeletionQuery.add( queryToDelete, BooleanClause.Occur.MUST );
		}

		Term classNameQueryTerm = new Term( ProjectionConstants.OBJECT_CLASS, entityType.getName() );
		TermQuery classNameQuery = new TermQuery( classNameQueryTerm );
		entityDeletionQuery.add( classNameQuery, BooleanClause.Occur.MUST );

		try {
			writer.deleteDocuments( entityDeletionQuery );
		}
		catch (Exception e) {
			String message = String.format( "unable to remove all %s matching Query: %s", entityType.toString(), query.toString() );
			throw new SearchException( message, e );
		}
		this.workspace.notifyWorkApplied( work );

	}

}

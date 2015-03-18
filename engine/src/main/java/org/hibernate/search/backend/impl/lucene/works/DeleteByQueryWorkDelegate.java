/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene.works;

import java.io.IOException;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.hibernate.search.backend.DeleteByQueryLuceneWork;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.DeletionQuery;
import org.hibernate.search.backend.impl.DeleteByQuerySupport;
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

		if ( log.isTraceEnabled() ) {
			log.tracef( "Removing all %s matching Query: %s", entityType.toString(), query.toString() );
		}

		BooleanQuery entityDeletionQuery = new BooleanQuery();

		{
			DeleteByQuerySupport.ToLuceneQuery converter = DeleteByQuerySupport.getToLuceneQuery( query.getQueryKey() );

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
		catch (IOException e) {
			SearchException ex = log.unableToDeleteByQuery( entityType, query, e );
			throw ex;
		}
		this.workspace.notifyWorkApplied( work );

	}

}

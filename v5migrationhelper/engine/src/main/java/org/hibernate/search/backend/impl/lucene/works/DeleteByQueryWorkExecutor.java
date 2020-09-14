/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene.works;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.lucene.IndexWriterDelegate;
import org.hibernate.search.backend.spi.DeleteByQueryLuceneWork;
import org.hibernate.search.backend.spi.DeletionQuery;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.store.Workspace;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;

/**
 * Stateless implementation that performs a <code>DeleteByQueryWork</code>.
 *
 * @author Martin Braun
 * @see IndexUpdateVisitor
 * @see LuceneWorkExecutor
 */
class DeleteByQueryWorkExecutor implements LuceneWorkExecutor {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );
	protected final Workspace workspace;

	DeleteByQueryWorkExecutor(Workspace workspace) {
		this.workspace = workspace;
	}

	@Override
	public void performWork(LuceneWork work, IndexWriterDelegate delegate, IndexingMonitor monitor) {
		DeleteByQueryLuceneWork deleteWork = (DeleteByQueryLuceneWork) work;

		final IndexedTypeIdentifier entityType = work.getEntityType();
		final DeletionQuery query = deleteWork.getDeletionQuery();

		if ( log.isTraceEnabled() ) {
			log.tracef( "Removing all %s matching Query: %s", entityType.toString(), query.toString() );
		}

		BooleanQuery.Builder entityDeletionQueryBuilder = new BooleanQuery.Builder();

		{
			Query queryToDelete = query.toLuceneQuery( this.workspace.getDocumentBuilder( entityType ) );

			entityDeletionQueryBuilder.add( queryToDelete, BooleanClause.Occur.FILTER );
		}

		Term classNameQueryTerm = new Term( ProjectionConstants.OBJECT_CLASS, entityType.getName() );
		TermQuery classNameQuery = new TermQuery( classNameQueryTerm );
		entityDeletionQueryBuilder.add( classNameQuery, BooleanClause.Occur.FILTER );

		addTenantQueryTerm( work.getTenantId(), entityDeletionQueryBuilder );

		try {
			delegate.deleteDocuments( entityDeletionQueryBuilder.build() );
		}
		catch (IOException e) {
			SearchException ex = log.unableToDeleteByQuery( entityType, query, e );
			throw ex;
		}
		this.workspace.notifyWorkApplied( work );
	}

	private void addTenantQueryTerm(final String tenantId, BooleanQuery.Builder entityDeletionQueryBuilder) {

		if ( tenantId != null ) {
			Term tenantTerm = new Term( DocumentBuilderIndexedEntity.TENANT_ID_FIELDNAME, tenantId );
			entityDeletionQueryBuilder.add( new TermQuery( tenantTerm ), BooleanClause.Occur.FILTER );
		}
	}
}

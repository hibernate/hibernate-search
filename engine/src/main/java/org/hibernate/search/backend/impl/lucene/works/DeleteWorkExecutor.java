/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene.works;

import java.io.Serializable;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.builtin.NumericFieldBridge;
import org.hibernate.search.bridge.util.impl.NumericFieldUtils;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.store.Workspace;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.lucene.IndexWriterDelegate;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Stateless implementation that performs a <code>DeleteLuceneWork</code>.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author John Griffin
 * @author Sanne Grinovero
 * @see IndexUpdateVisitor
 * @see LuceneWorkExecutor
 */
class DeleteWorkExecutor implements LuceneWorkExecutor {

	private static final Log log = LoggerFactory.make();
	protected final Workspace workspace;

	DeleteWorkExecutor(Workspace workspace) {
		this.workspace = workspace;
	}

	@Override
	public void performWork(LuceneWork work, IndexWriterDelegate delegate, IndexingMonitor monitor) {
		final Class<?> entityType = work.getEntityClass();
		final Serializable id = work.getId();
		log.tracef( "Removing %s#%s by query.", entityType, id );
		DocumentBuilderIndexedEntity builder = workspace.getDocumentBuilder( entityType );

		BooleanQuery entityDeletionQuery = new BooleanQuery();

		Query idQueryTerm;
		if ( isIdNumeric( builder ) ) {
			idQueryTerm = NumericFieldUtils.createExactMatchQuery( builder.getIdKeywordName(), id );
		}
		else {
			Term idTerm = new Term( builder.getIdKeywordName(), work.getIdInString() );
			idQueryTerm = new TermQuery( idTerm );
		}
		entityDeletionQuery.add( idQueryTerm, BooleanClause.Occur.FILTER );

		Term classNameQueryTerm = new Term( ProjectionConstants.OBJECT_CLASS, entityType.getName() );
		TermQuery classNameQuery = new TermQuery( classNameQueryTerm );
		entityDeletionQuery.add( classNameQuery, BooleanClause.Occur.FILTER );

		addTenantQueryTerm( work.getTenantId(), entityDeletionQuery );

		try {
			delegate.deleteDocuments( entityDeletionQuery );
		}
		catch (Exception e) {
			String message = "Unable to remove " + entityType + "#" + id + " from index.";
			throw new SearchException( message, e );
		}
		workspace.notifyWorkApplied( work );
	}

	private void addTenantQueryTerm(final String tenantId, BooleanQuery entityDeletionQuery) {
		if ( tenantId != null ) {
			Term tenantTerm = new Term( DocumentBuilderIndexedEntity.TENANT_ID_FIELDNAME, tenantId );
			entityDeletionQuery.add( new TermQuery( tenantTerm ), BooleanClause.Occur.FILTER );
		}
	}

	protected static boolean isIdNumeric(DocumentBuilderIndexedEntity documentBuilder) {
		TwoWayFieldBridge idBridge = documentBuilder.getIdBridge();
		return idBridge instanceof NumericFieldBridge;
	}

}

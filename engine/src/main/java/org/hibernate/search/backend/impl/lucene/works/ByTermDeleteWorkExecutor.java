/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene.works;

import java.io.IOException;
import java.io.Serializable;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.lucene.IndexWriterDelegate;
import org.hibernate.search.bridge.util.impl.NumericFieldUtils;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.store.Workspace;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;
import org.hibernate.search.util.logging.impl.Log;

/**
 * Extension of {@link DeleteWorkExecutor}
 * that applies the index delete operation using the Lucene operation
 * {@link org.apache.lucene.index.IndexWriter#deleteDocuments(Term...)} when possible.
 * <p>
 * This is the most efficient way to delete from the index, but the underlying store
 * must guarantee that the term is unique across documents and entity types.
 *
 * @author gustavonalle
 * @see DeleteWorkExecutor
 */
public class ByTermDeleteWorkExecutor extends DeleteWorkExecutor {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	ByTermDeleteWorkExecutor(Workspace workspace) {
		super( workspace );
	}

	@Override
	public void performWork(LuceneWork work, IndexWriterDelegate delegate, IndexingMonitor monitor) {
		final IndexedTypeIdentifier managedType = work.getEntityType();
		DocumentBuilderIndexedEntity builder = workspace.getDocumentBuilder( managedType );
		doPerformWork( work, delegate, managedType, builder, isIdNumeric( builder ) );
	}
	protected void doPerformWork(LuceneWork work, IndexWriterDelegate delegate,
			IndexedTypeIdentifier managedType, DocumentBuilderIndexedEntity builder, boolean isIdNumeric) {
		final String tenantId = work.getTenantId();
		Serializable id = work.getId();
		log.tracef( "Removing %s#%s by id using an IndexWriter.", managedType, id );
		try {
			if ( tenantId == null ) {
				deleteWithoutTenant( work, delegate, builder, isIdNumeric, id );
			}
			else {
				deleteWithTenant( work, delegate, builder, isIdNumeric, tenantId, id );
			}
			workspace.notifyWorkApplied( work );
		}
		catch (Exception e) {
			String message = "Unable to remove " + managedType + "#" + id + " from index.";
			throw new SearchException( message, e );
		}
	}

	private void deleteWithTenant(LuceneWork work, IndexWriterDelegate delegate,
			DocumentBuilderIndexedEntity builder, boolean isIdNumeric,
			final String tenantId, Serializable id) throws IOException {
		BooleanQuery.Builder termDeleteQueryBuilder = new BooleanQuery.Builder();
		TermQuery tenantTermQuery = new TermQuery( new Term( DocumentBuilderIndexedEntity.TENANT_ID_FIELDNAME, tenantId ) );
		termDeleteQueryBuilder.add( tenantTermQuery, Occur.FILTER );
		if ( isIdNumeric ) {
			Query exactMatchQuery = NumericFieldUtils.createExactMatchQuery( builder.getIdFieldName(), id );
			termDeleteQueryBuilder.add( exactMatchQuery, Occur.FILTER );
		}
		else {
			Term idTerm = new Term( builder.getIdFieldName(), work.getIdInString() );
			termDeleteQueryBuilder.add( new TermQuery( idTerm ), Occur.FILTER );
		}
		BooleanQuery termDeleteQuery = termDeleteQueryBuilder.build();
		delegate.deleteDocuments( termDeleteQuery );
	}

	private void deleteWithoutTenant(LuceneWork work, IndexWriterDelegate delegate,
			DocumentBuilderIndexedEntity builder, boolean isIdNumeric,
			Serializable id) throws IOException {
		if ( isIdNumeric ) {
			delegate.deleteDocuments( NumericFieldUtils.createExactMatchQuery( builder.getIdFieldName(), id ) );
		}
		else {
			Term idTerm = new Term( builder.getIdFieldName(), work.getIdInString() );
			//The point to this class is to avoid using a Query to perform the delete operation!
			delegate.deleteDocuments( idTerm );
		}
	}

}

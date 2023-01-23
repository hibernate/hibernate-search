/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.backend.impl.lucene.works;

import static org.hibernate.search.backend.impl.lucene.works.DeleteWorkExecutor.isIdNumeric;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.analyzer.spi.ScopedAnalyzerReference;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.lucene.IndexWriterDelegate;
import org.hibernate.search.bridge.util.impl.NumericFieldUtils;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.store.Workspace;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;

/**
 * Extension of {@link UpdateWorkExecutor}
 * that applies the index update operation using the Lucene operation
 * {@link org.apache.lucene.index.IndexWriter#updateDocument} when possible.
 * <p>
 * This is the most efficient way to update the index, but the underlying store
 * must guarantee that the term is unique across documents and entity types.
 *
 * @author gustavonalle
 */
public class ByTermUpdateWorkExecutor extends UpdateWorkExecutor {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private final AddWorkExecutor addDelegate;
	private final Workspace workspace;

	ByTermUpdateWorkExecutor(Workspace workspace, AddWorkExecutor addDelegate) {
		super( null, null );
		this.workspace = workspace;
		this.addDelegate = addDelegate;
	}

	@Override
	public void performWork(LuceneWork work, IndexWriterDelegate delegate, IndexingMonitor monitor) {
		final IndexedTypeIdentifier managedType = work.getEntityType();
		DocumentBuilderIndexedEntity builder = workspace.getDocumentBuilder( managedType );
		doPerformWork( work, delegate, monitor, managedType, builder, isIdNumeric( builder ) );
	}

	protected void doPerformWork(LuceneWork work, IndexWriterDelegate delegate, IndexingMonitor monitor,
			IndexedTypeIdentifier managedType, DocumentBuilderIndexedEntity builder, boolean isIdNumeric) {
		final Serializable id = work.getId();
		final String tenantId = work.getTenantId();
		log.tracef( "Updating %s#%s by id using an IndexWriter.", managedType, id );
		try {
			if ( tenantId == null ) {
				updateWithoutTenant( work, delegate, monitor, builder, isIdNumeric, id );
			}
			else {
				updateWithTenant( work, delegate, monitor, builder, isIdNumeric, tenantId, id );
			}
			workspace.notifyWorkApplied( work );
		}
		catch (Exception e) {
			String message = "Unable to update " + managedType + "#" + id + " in index.";
			throw new SearchException( message, e );
		}
		if ( monitor != null ) {
			monitor.documentsAdded( 1l );
		}
	}

	private void updateWithTenant(LuceneWork work, IndexWriterDelegate delegate, IndexingMonitor monitor,
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
		this.addDelegate.performWork( work, delegate, monitor );
	}

	private void updateWithoutTenant(LuceneWork work, IndexWriterDelegate delegate, IndexingMonitor monitor,
			DocumentBuilderIndexedEntity builder, boolean isIdNumeric,
			Serializable id) throws IOException {
		if ( isIdNumeric ) {
			delegate.deleteDocuments( NumericFieldUtils.createExactMatchQuery( builder.getIdFieldName(), id ) );
			this.addDelegate.performWork( work, delegate, monitor );
		}
		else {
			Term idTerm = new Term( builder.getIdFieldName(), work.getIdInString() );
			Map<String, String> fieldToAnalyzerMap = work.getFieldToAnalyzerMap();
			ScopedAnalyzerReference analyzerReference = builder.getAnalyzerReference();
			analyzerReference = AddWorkExecutor.updateAnalyzerMappings( workspace, analyzerReference, fieldToAnalyzerMap );
			delegate.updateDocument( idTerm, work.getDocument(), analyzerReference );
		}
	}
}

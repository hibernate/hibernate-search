/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.backend.impl.lucene.works;

import java.io.Serializable;
import java.util.Map;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.lucene.IndexWriterDelegate;
import org.hibernate.search.bridge.util.impl.NumericFieldUtils;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.store.Workspace;
import org.hibernate.search.util.impl.ScopedAnalyzer;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * This applies the index update operation using the Lucene operation
 * {@link org.apache.lucene.index.IndexWriter#updateDocument}.
 * This is the most efficient way to update the index, but underlying store
 * must guarantee that the term is unique across documents and entity types.
 *
 * @author gustavonalle
 */
public final class ByTermUpdateWorkExecutor extends UpdateWorkExecutor {

	private static final Log log = LoggerFactory.make();

	private final AddWorkExecutor addDelegate;
	private final Workspace workspace;

	ByTermUpdateWorkExecutor(Workspace workspace, AddWorkExecutor addDelegate) {
		super( null, null );
		this.workspace = workspace;
		this.addDelegate = addDelegate;
	}

	@Override
	public void performWork(LuceneWork work, IndexWriterDelegate delegate, IndexingMonitor monitor) {
		final Serializable id = work.getId();
		final String tenantId = work.getTenantId();
		final Class<?> managedType = work.getEntityClass();
		DocumentBuilderIndexedEntity builder = workspace.getDocumentBuilder( managedType );
		try {
			if ( DeleteWorkExecutor.isIdNumeric( builder ) ) {
				log.tracef(
						"Deleting %s#%s by query using an IndexWriter#updateDocument as id is Numeric",
						managedType,
						id
				);
				Query exactMatchQuery = NumericFieldUtils.createExactMatchQuery( builder.getIdKeywordName(), id );
				BooleanQuery deleteDocumentsQuery = new BooleanQuery();
				deleteDocumentsQuery.add( exactMatchQuery, Occur.FILTER );
				if ( tenantId != null ) {
					TermQuery tenantTermQuery = new TermQuery( new Term( DocumentBuilderIndexedEntity.TENANT_ID_FIELDNAME, tenantId ) );
					deleteDocumentsQuery.add( tenantTermQuery, Occur.FILTER );
				}
				delegate.deleteDocuments( deleteDocumentsQuery );
				// no need to log the Add operation as we'll log in the delegate
				this.addDelegate.performWork( work, delegate, monitor );
			}
			else {
				log.tracef( "Updating %s#%s by id using an IndexWriter#updateDocument.", managedType, id );
				Term idTerm = new Term( builder.getIdKeywordName(), work.getIdInString() );
				Map<String, String> fieldToAnalyzerMap = work.getFieldToAnalyzerMap();
				ScopedAnalyzer analyzer = builder.getAnalyzer();
				analyzer = AddWorkExecutor.updateAnalyzerMappings( workspace, analyzer, fieldToAnalyzerMap );
				delegate.updateDocument( idTerm, work.getDocument(), analyzer );
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

}

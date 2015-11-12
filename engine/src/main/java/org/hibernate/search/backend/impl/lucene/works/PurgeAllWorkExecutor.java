/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene.works;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.store.Workspace;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.lucene.IndexWriterDelegate;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
* Stateless implementation that performs a PurgeAllLuceneWork.
* @see IndexUpdateVisitor
* @see LuceneWorkExecutor
* @author Emmanuel Bernard
* @author Hardy Ferentschik
* @author John Griffin
* @author Sanne Grinovero
*/
class PurgeAllWorkExecutor implements LuceneWorkExecutor {

	private static final Log log = LoggerFactory.make();
	protected final Workspace workspace;

	PurgeAllWorkExecutor(Workspace workspace) {
		this.workspace = workspace;
	}

	@Override
	public void performWork(LuceneWork work, IndexWriterDelegate delegate, IndexingMonitor monitor) {
		final Class<?> entityType = work.getEntityClass();
		final String tenantId = work.getTenantId();
		try {
			Term entityTypeTerm = new Term( ProjectionConstants.OBJECT_CLASS, entityType.getName() );
			if ( tenantId == null ) {
				log.tracef( "purgeAll Lucene index using IndexWriter for type: %s", entityType );
				delegate.deleteDocuments( entityTypeTerm );
			}
			else {
				log.tracef( "purgeAll Lucene index using IndexWriter for type $1%s and tenant $2%s", entityType, tenantId );
				Term tenantIdTerm = tenantId == null ? null : new Term( DocumentBuilderIndexedEntity.TENANT_ID_FIELDNAME, tenantId );
				BooleanQuery deleteDocumentsQuery = new BooleanQuery();
				deleteDocumentsQuery.add( new TermQuery( entityTypeTerm ), Occur.FILTER );
				deleteDocumentsQuery.add( new TermQuery( tenantIdTerm ), Occur.FILTER );
				delegate.deleteDocuments( deleteDocumentsQuery );
			}
		}
		catch (Exception e) {
			throw new SearchException( "Unable to purge all from Lucene index: " + entityType, e );
		}
		workspace.notifyWorkApplied( work );
	}
}

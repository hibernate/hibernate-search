/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene.works;

import java.io.IOException;
import java.io.Serializable;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.bridge.util.impl.NumericFieldUtils;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.store.Workspace;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.hibernate.search.util.logging.impl.Log;

/**
 * Extension of <code>DeleteLuceneWork</code> that will always perform the
 * delete LuceneWork in an optimal way, since the underlying data store guarantee
 * uniqueness of terms across different entity types.
 *
 * @author gustavonalle
 * @see DeleteWorkExecutor
 */
public final class ByTermDeleteWorkExecutor extends DeleteWorkExecutor {

	private static final Log log = LoggerFactory.make();

	ByTermDeleteWorkExecutor(Workspace workspace) {
		super( workspace );
	}

	@Override
	public void performWork(LuceneWork work, IndexWriter writer, IndexingMonitor monitor) {
		final Class<?> managedType = work.getEntityClass();
		final String tenantId = work.getTenantId();
		DocumentBuilderIndexedEntity builder = workspace.getDocumentBuilder( managedType );
		Serializable id = work.getId();
		log.tracef( "Removing %s#%s by id using an IndexWriter.", managedType, id );
		try {
			if ( tenantId == null ) {
				deleteWithoutTenant( work, writer, builder, id );
			}
			else {
				deleteWithTenant( work, writer, tenantId, builder, id );
			}
			workspace.notifyWorkApplied( work );
		}
		catch (Exception e) {
			String message = "Unable to remove " + managedType + "#" + id + " from index.";
			throw new SearchException( message, e );
		}
	}

	private void deleteWithTenant(LuceneWork work, IndexWriter writer, final String tenantId, DocumentBuilderIndexedEntity builder, Serializable id) throws IOException {
		BooleanQuery termDeleteQuery = new BooleanQuery();
		TermQuery tenantTermQuery = new TermQuery( new Term( DocumentBuilderIndexedEntity.TENANT_ID_FIELDNAME, tenantId ) );
		termDeleteQuery.add( tenantTermQuery, Occur.MUST );
		if ( isIdNumeric( builder ) ) {
			Query exactMatchQuery = NumericFieldUtils.createExactMatchQuery( builder.getIdKeywordName(), id );
			termDeleteQuery.add( exactMatchQuery, Occur.MUST );
		}
		else {
			Term idTerm = new Term( builder.getIdKeywordName(), work.getIdInString() );
			termDeleteQuery.add( new TermQuery( idTerm ), Occur.MUST );
		}
		writer.deleteDocuments( termDeleteQuery );
	}

	private void deleteWithoutTenant(LuceneWork work, IndexWriter writer, DocumentBuilderIndexedEntity builder, Serializable id) throws IOException {
		if ( isIdNumeric( builder ) ) {
			writer.deleteDocuments( NumericFieldUtils.createExactMatchQuery( builder.getIdKeywordName(), id ) );
		}
		else {
			Term idTerm = new Term( builder.getIdKeywordName(), work.getIdInString() );
			//The point to this class is to avoid using a Query to perform the delete operation!
			writer.deleteDocuments( idTerm );
		}
	}

}

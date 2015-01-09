/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene.works;

import java.io.Serializable;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;

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
 * @see DeleteWorkDelegate
 */
public final class ByTermDeleteWorkDelegate extends DeleteWorkDelegate {

	private static final Log log = LoggerFactory.make();

	ByTermDeleteWorkDelegate(Workspace workspace) {
		super( workspace );
	}

	@Override
	public void performWork(LuceneWork work, IndexWriter writer, IndexingMonitor monitor) {
		final Class<?> managedType = work.getEntityClass();
		DocumentBuilderIndexedEntity builder = workspace.getDocumentBuilder( managedType );
		Serializable id = work.getId();
		log.tracef( "Removing %s#%s by id using an IndexWriter.", managedType, id );
		try {
			if ( isIdNumeric( builder ) ) {
				writer.deleteDocuments( NumericFieldUtils.createExactMatchQuery( builder.getIdKeywordName(), id ) );
			}
			else {
				Term idTerm = new Term( builder.getIdKeywordName(), work.getIdInString() );
				writer.deleteDocuments( idTerm );
			}
			workspace.notifyWorkApplied( work );
		}
		catch (Exception e) {
			String message = "Unable to remove " + managedType + "#" + id + " from index.";
			throw new SearchException( message, e );
		}
	}


}

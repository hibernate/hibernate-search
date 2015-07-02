/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene.works;

import java.io.Serializable;

import org.apache.lucene.index.Term;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.lucene.IndexWriterDelegate;
import org.hibernate.search.bridge.util.impl.NumericFieldUtils;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.store.Workspace;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.hibernate.search.util.logging.impl.Log;

/**
 * Extension of <code>DeleteLuceneWork</code> bound to a single entity.
 * This allows to perform the delete LuceneWork in an optimal way in case
 * the index is NOT shared across different entities (which is the default).
 *
 * @author Sanne Grinovero
 * @see DeleteWorkExecutor
 */
public final class DeleteExtWorkExecutor extends DeleteWorkExecutor {

	private final Class<?> managedType;
	private final DocumentBuilderIndexedEntity builder;
	private static final Log log = LoggerFactory.make();
	private final boolean idIsNumeric;

	DeleteExtWorkExecutor(Workspace workspace) {
		super( workspace );
		managedType = workspace.getEntitiesInIndexManager().iterator().next();
		builder = workspace.getDocumentBuilder( managedType );
		idIsNumeric = isIdNumeric( builder );
	}

	@Override
	public void performWork(LuceneWork work, IndexWriterDelegate delegate, IndexingMonitor monitor) {
		checkType( work );
		Serializable id = work.getId();
		log.tracef( "Removing %s#%s by id using an IndexWriter.", managedType, id );
		try {
			if ( idIsNumeric ) {
				delegate.deleteDocuments( NumericFieldUtils.createExactMatchQuery( builder.getIdKeywordName(), id ) );
			}
			else {
				Term idTerm = new Term( builder.getIdKeywordName(), work.getIdInString() );
				delegate.deleteDocuments( idTerm );
			}
			workspace.notifyWorkApplied( work );
		}
		catch (Exception e) {
			String message = "Unable to remove " + managedType + "#" + id + " from index.";
			throw new SearchException( message, e );
		}
	}

	private void checkType(final LuceneWork work) {
		if ( work.getEntityClass() != managedType ) {
			throw new AssertionFailure( "Unexpected type" );
		}
	}

}

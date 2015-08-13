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
import org.hibernate.search.exception.AssertionFailure;
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
 * {@link org.apache.lucene.index.IndexWriter#updateDocument(Term, Iterable)}
 *
 * This is the most efficient way to update the index, but we can apply it only if the Document is uniquely identified
 * by a single term (so no index sharing across entities or Numeric ids).
 *
 * @author Sanne Grinovero (C) 2012 Red Hat Inc.
 */
public final class UpdateExtWorkExecutor extends UpdateWorkExecutor {

	private static final Log log = LoggerFactory.make();

	private final AddWorkExecutor addDelegate;
	private final Class<?> managedType;
	private final DocumentBuilderIndexedEntity builder;
	private final boolean idIsNumeric;
	private final Workspace workspace;

	UpdateExtWorkExecutor(Workspace workspace, AddWorkExecutor addDelegate) {
		super( null, null );
		this.workspace = workspace;
		this.addDelegate = addDelegate;
		this.managedType = workspace.getEntitiesInIndexManager().iterator().next();
		this.builder = workspace.getDocumentBuilder( managedType );
		this.idIsNumeric = DeleteWorkExecutor.isIdNumeric( builder );
	}

	@Override
	public void performWork(LuceneWork work, IndexWriterDelegate delegate, IndexingMonitor monitor) {
		checkType( work );
		final Serializable id = work.getId();
		try {
			if ( idIsNumeric ) {
				log.tracef( "Deleting %s#%s by query using an IndexWriter#updateDocument as id is Numeric", managedType, id );
				delegate.deleteDocuments( NumericFieldUtils.createExactMatchQuery( builder.getIdKeywordName(), id ) );
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

	private void checkType(final LuceneWork work) {
		if ( work.getEntityClass() != managedType ) {
			throw new AssertionFailure( "Unexpected type: " + work.getEntityClass() + " This workspace expects: " + managedType );
		}
	}

}

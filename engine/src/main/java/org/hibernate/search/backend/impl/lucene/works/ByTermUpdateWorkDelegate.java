/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.backend.impl.lucene.works;

import java.io.Serializable;
import java.util.Map;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;

import org.hibernate.search.exception.SearchException;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
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
public final class ByTermUpdateWorkDelegate extends UpdateWorkDelegate {

	private static final Log log = LoggerFactory.make();

	private final AddWorkDelegate addDelegate;
	private final Workspace workspace;

	ByTermUpdateWorkDelegate(Workspace workspace, AddWorkDelegate addDelegate) {
		super( null, null );
		this.workspace = workspace;
		this.addDelegate = addDelegate;
	}

	@Override
	public void performWork(LuceneWork work, IndexWriter writer, IndexingMonitor monitor) {
		final Serializable id = work.getId();
		final Class<?> managedType = work.getEntityClass();
		DocumentBuilderIndexedEntity builder = workspace.getDocumentBuilder( managedType );
		try {
			if ( DeleteWorkDelegate.isIdNumeric( builder ) ) {
				log.tracef(
						"Deleting %s#%s by query using an IndexWriter#updateDocument as id is Numeric",
						managedType,
						id
				);
				writer.deleteDocuments( NumericFieldUtils.createExactMatchQuery( builder.getIdKeywordName(), id ) );
				// no need to log the Add operation as we'll log in the delegate
				this.addDelegate.performWork( work, writer, monitor );
			}
			else {
				log.tracef( "Updating %s#%s by id using an IndexWriter#updateDocument.", managedType, id );
				Term idTerm = new Term( builder.getIdKeywordName(), work.getIdInString() );
				Map<String, String> fieldToAnalyzerMap = work.getFieldToAnalyzerMap();
				ScopedAnalyzer analyzer = builder.getAnalyzer();
				analyzer = AddWorkDelegate.updateAnalyzerMappings( workspace, analyzer, fieldToAnalyzerMap );
				writer.updateDocument( idTerm, work.getDocument(), analyzer );
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

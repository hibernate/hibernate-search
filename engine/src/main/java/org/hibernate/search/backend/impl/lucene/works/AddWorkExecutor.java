/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene.works;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;

import org.hibernate.search.exception.SearchException;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.lucene.IndexWriterDelegate;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.store.Workspace;
import org.hibernate.search.util.impl.ScopedAnalyzer;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Stateless implementation that performs an <code>AddLuceneWork</code>.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author John Griffin
 * @author Sanne Grinovero
 * @see IndexUpdateVisitor
 * @see LuceneWorkExecutor
 */
class AddWorkExecutor implements LuceneWorkExecutor {

	private static final Log log = LoggerFactory.make();
	protected final Workspace workspace;

	AddWorkExecutor(Workspace workspace) {
		this.workspace = workspace;
	}

	@Override
	public void performWork(LuceneWork work, IndexWriterDelegate delegate, IndexingMonitor monitor) {
		final Class<?> entityType = work.getEntityClass();
		DocumentBuilderIndexedEntity documentBuilder = workspace.getDocumentBuilder( entityType );
		Map<String, String> fieldToAnalyzerMap = work.getFieldToAnalyzerMap();
		ScopedAnalyzer analyzer = documentBuilder.getAnalyzer();
		analyzer = updateAnalyzerMappings( workspace, analyzer, fieldToAnalyzerMap );
		if ( log.isTraceEnabled() ) {
			log.trace( "add to Lucene index: " + entityType + "#" + work.getId() + ":" + work.getDocument() );
		}
		try {
			delegate.addDocument( work.getDocument(), analyzer );
			workspace.notifyWorkApplied( work );
		}
		catch (IOException e) {
			throw new SearchException(
					"Unable to add to Lucene index: "
							+ entityType + "#" + work.getId(), e
			);
		}
		if ( monitor != null ) {
			monitor.documentsAdded( 1l );
		}
	}

	/**
	 * Allows to override the otherwise static field to analyzer mapping in <code>scopedAnalyzer</code>.
	 *
	 * @param workspace The current work context
	 * @param scopedAnalyzer The scoped analyzer created at startup time.
	 * @param fieldToAnalyzerMap A map of <code>Document</code> field names for analyzer names. This map gets creates
	 * when the Lucene <code>Document</code> gets created and uses the state of the entity to index to determine analyzers
	 * dynamically at index time.
	 *
	 * @return <code>scopedAnalyzer</code> in case <code>fieldToAnalyzerMap</code> is <code>null</code> or empty. Otherwise
	 *         a clone of <code>scopedAnalyzer</code> is created where the analyzers get overriden according to <code>fieldToAnalyzerMap</code>.
	 */
	static ScopedAnalyzer updateAnalyzerMappings(Workspace workspace, ScopedAnalyzer scopedAnalyzer, Map<String, String> fieldToAnalyzerMap) {
		// for backwards compatibility
		if ( fieldToAnalyzerMap == null || fieldToAnalyzerMap.isEmpty() ) {
			return scopedAnalyzer;
		}

		ScopedAnalyzer analyzerClone = scopedAnalyzer.clone();
		for ( Map.Entry<String, String> entry : fieldToAnalyzerMap.entrySet() ) {
			Analyzer analyzer = workspace.getAnalyzer( entry.getValue() );
			if ( analyzer == null ) {
				log.unableToRetrieveNamedAnalyzer( entry.getValue() );
			}
			else {
				analyzerClone.addScopedAnalyzer( entry.getKey(), analyzer );
			}
		}
		return analyzerClone;
	}

}

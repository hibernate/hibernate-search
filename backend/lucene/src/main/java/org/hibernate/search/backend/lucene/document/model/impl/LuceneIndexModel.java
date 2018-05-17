/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.impl;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.util.impl.common.CollectionHelper;
import org.hibernate.search.util.impl.common.LoggerFactory;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;

/**
 * @author Guillaume Smet
 */
public class LuceneIndexModel implements AutoCloseable {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String indexName;

	private final Map<String, LuceneIndexSchemaObjectNode> objectNodes;

	private final Map<String, LuceneIndexSchemaFieldNode<?>> fieldNodes;

	private final ScopedAnalyzer scopedAnalyzer;

	public LuceneIndexModel(String indexName, LuceneRootIndexSchemaContributor contributor) {
		this.indexName = indexName;

		Map<String, LuceneIndexSchemaObjectNode> objectNodesBuilder = new HashMap<>();
		Map<String, LuceneIndexSchemaFieldNode<?>> fieldNodesBuilder = new HashMap<>();
		// TODO the default analyzer should be configurable, for now, we default to no analysis
		ScopedAnalyzer.Builder scopedAnalyzerBuilder = new ScopedAnalyzer.Builder( new KeywordAnalyzer() );
		contributor.contribute( new LuceneIndexSchemaNodeCollector() {
			@Override
			public void collectAnalyzer(String absoluteFieldPath, Analyzer analyzer) {
				scopedAnalyzerBuilder.setAnalyzer( absoluteFieldPath, analyzer );
			}

			@Override
			public void collectFieldNode(String absoluteFieldPath, LuceneIndexSchemaFieldNode<?> node) {
				fieldNodesBuilder.put( absoluteFieldPath, node );
			}

			@Override
			public void collectObjectNode(String absolutePath, LuceneIndexSchemaObjectNode node) {
				objectNodesBuilder.put( absolutePath, node );
			}
		} );

		objectNodes = CollectionHelper.toImmutableMap( objectNodesBuilder );
		fieldNodes = CollectionHelper.toImmutableMap( fieldNodesBuilder );
		scopedAnalyzer = scopedAnalyzerBuilder.build();
	}

	@Override
	public void close() {
		scopedAnalyzer.close();
	}

	public String getIndexName() {
		return indexName;
	}

	public LuceneIndexSchemaFieldNode<?> getFieldNode(String absoluteFieldPath) {
		return fieldNodes.get( absoluteFieldPath );
	}

	public LuceneIndexSchemaObjectNode getObjectNode(String absolutePath) {
		return objectNodes.get( absolutePath );
	}

	public ScopedAnalyzer getScopedAnalyzer() {
		return scopedAnalyzer;
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "indexName=" ).append( indexName )
				.append( "]" )
				.toString();
	}
}
